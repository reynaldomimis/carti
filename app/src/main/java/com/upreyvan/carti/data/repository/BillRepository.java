package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.BillDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import io.appwrite.models.RealtimeSubscription;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class BillRepository {
    private final BillDao billDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final RealtimeHelper realtimeHelper;
    private RealtimeSubscription realtimeSubscription;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BillRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        billDao = db.billDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
        realtimeHelper = new RealtimeHelper(context);
    }

    public LiveData<List<Bill>> getAllBills() {
        return billDao.getAllBills(pref.getFamilyId());
    }

    public void saveLocally(Bill bill) {
        executor.execute(() -> billDao.insert(bill));
    }

    public void deleteLocally(String id) {
        executor.execute(() -> billDao.deleteById(id));
    }

    public void updateStatus(String id, String status) {
        executor.execute(() -> billDao.updateStatus(id, status));
    }
}
