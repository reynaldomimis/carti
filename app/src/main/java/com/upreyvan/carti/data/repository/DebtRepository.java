package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.DebtDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class DebtRepository {
    private final DebtDao debtDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DebtRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        debtDao = db.debtDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
    }

    public LiveData<List<Debt>> getAllDebts() {
        return debtDao.getAllDebts(pref.getFamilyId());
    }

    public void syncDebtsIfNeeded() {
        executor.execute(() -> {
            List<Debt> local = debtDao.getAllDebtsList(pref.getFamilyId());
            if (local == null || local.isEmpty()) {
                // Reset sync time if local DB was wiped
                pref.setLastDebtSyncTime("1970-01-01T00:00:00.000Z");
            }
            refreshDebts();
        });
    }

    public void refreshDebts() {
        String familyId = pref.getFamilyId();
        String lastSync = pref.getLastDebtSyncTime();

        apiHelper.getDebtsSince(lastSync, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (result.getDocuments().isEmpty()) return;

                executor.execute(() -> {
                    List<Debt> debts = new ArrayList<>();
                    String latestTimestamp = lastSync;

                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        debts.add(mapToDebt(doc, familyId));
                        
                        if (doc.getCreatedAt().compareTo(latestTimestamp) > 0) {
                            latestTimestamp = doc.getCreatedAt();
                        }
                    }
                    
                    debtDao.insertAll(debts);
                    pref.setLastDebtSyncTime(latestTimestamp);
                });
            }

            @Override
            public void onError(Throwable error) {}
        });
    }

    public void saveLocally(Debt debt) {
        debt.setFamilyId(pref.getFamilyId());
        executor.execute(() -> debtDao.insert(debt));
    }

    public void deleteLocally(String id) {
        executor.execute(() -> debtDao.deleteById(id));
    }

    private Debt mapToDebt(Document<Map<String, Object>> doc, String familyId) {
        Map<String, Object> data = doc.getData();
        return new Debt(
            doc.getId(),
            familyId,
            String.valueOf(data.get("personName")),
            String.valueOf(data.get("description")),
            Utils.formatTimestamp(doc.getCreatedAt()),
            Utils.getDouble(data.get("amount")),
            Boolean.TRUE.equals(data.get("isPaid")),
            R.drawable.ic_person, // Default
            String.valueOf(data.get("notes"))
        );
    }
}