package com.upreyvan.carti.util;

import android.content.Context;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;

public class BudgetAllocationHelper {

    public interface AllocationCallback {
        void onBalanceLoaded(double remainingBalance);
    }

    public static void getRemainingBalance(Context context, String categoryName, AllocationCallback callback) {
        TransactionRepository repository = TransactionRepository.getInstance(context);
        
        androidx.lifecycle.LiveData<com.upreyvan.carti.model.TransactionWithUser> dummy = new androidx.lifecycle.MutableLiveData<>(); // Just to get access to observer
        repository.getAllTransactions().observeForever(new androidx.lifecycle.Observer<java.util.List<com.upreyvan.carti.model.TransactionWithUser>>() {
            @Override
            public void onChanged(java.util.List<com.upreyvan.carti.model.TransactionWithUser> transactions) {
                repository.getAllTransactions().removeObserver(this);
                if (transactions == null) {
                    callback.onBalanceLoaded(0.0);
                    return;
                }

                double allocated = 5000.0;
                double spent = 0;
                for (com.upreyvan.carti.model.TransactionWithUser tWithU : transactions) {
                    com.upreyvan.carti.model.Transaction t = tWithU.getTransaction();
                    if ("EXPENSE".equalsIgnoreCase(t.getType()) && categoryName.equalsIgnoreCase(t.getCategory())) {
                        spent += t.getAmount();
                    }
                }

                callback.onBalanceLoaded(allocated - spent);
            }
        });
    }
}
