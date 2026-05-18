package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.IncomeDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.model.Income;
import io.appwrite.models.RealtimeSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class IncomeRepository {
    private final IncomeDao incomeDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final RealtimeHelper realtimeHelper;
    private RealtimeSubscription realtimeSubscription;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public IncomeRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        incomeDao = db.incomeDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
        realtimeHelper = new RealtimeHelper(context);
        initRealtime();
    }

    private void initRealtime() {
        if (realtimeSubscription != null) return;
        realtimeSubscription = realtimeHelper.subscribeToCollection(
                Constants.Appwrite.COL_INCOMES,
                event -> refreshIncomes()
        );
    }

    public void onDestroy() {
        if (realtimeSubscription != null) {
            realtimeSubscription.close();
            realtimeSubscription = null;
        }
    }

    public LiveData<List<Income>> getUserIncomes() {
        return incomeDao.getIncomesByUserId(pref.getUserId());
    }

    public LiveData<Double> getTotalIncome() {
        return incomeDao.getTotalIncomeByFamilyId(pref.getFamilyId());
    }

    public void addIncome(String source, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.addIncome(source, amount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                refreshIncomes();
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void updateIncome(String incomeId, String source, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.updateIncome(incomeId, source, amount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                refreshIncomes();
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void deleteIncome(String incomeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.deleteIncome(incomeId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                refreshIncomes();
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void refreshIncomes() {
        apiHelper.getIncomes(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<Income> incomes = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        
                        double amount = 0;
                        if (data.get("amount") != null) {
                            try {
                                amount = Double.parseDouble(String.valueOf(data.get("amount")));
                            } catch (Exception e) {
                                amount = 0;
                            }
                        }

                        // Parse Appwrite date: 2023-10-25T12:34:56.789Z
                        long ts = System.currentTimeMillis();
                        try {
                            String createdAt = String.valueOf(data.get("$createdAt"));
                            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(createdAt);
                            ts = odt.toInstant().toEpochMilli();
                        } catch (Exception e) {
                            // Fallback to current time
                        }

                        incomes.add(new Income(
                            doc.getId(),
                            String.valueOf(data.get("userId")),
                            String.valueOf(data.get("familyId")),
                            String.valueOf(data.get("source")),
                            amount,
                            String.valueOf(data.get("$createdAt"))
                        ));
                    }
                    incomeDao.insertAll(incomes);
                });
            }

            @Override
            public void onError(Throwable error) {
            }
        });
    }
}