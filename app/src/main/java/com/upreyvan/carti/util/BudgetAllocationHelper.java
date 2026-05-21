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
        
        repository.getAllTransactions().observeForever(transactions -> {
            if (transactions == null) {
                callback.onBalanceLoaded(0.0);
                return;
            }

            double allocated = 5000.0; // Simulated allocation
            double spent = transactions.stream()
                    .map(TransactionWithUser::getTransaction)
                    .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()) && categoryName.equalsIgnoreCase(t.getCategory()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            callback.onBalanceLoaded(allocated - spent);
        });
    }
}
