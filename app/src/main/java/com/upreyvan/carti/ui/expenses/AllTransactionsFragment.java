package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAllTransactionsBinding;
import com.upreyvan.carti.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class AllTransactionsFragment extends BaseFragment<FragmentAllTransactionsBinding> {

    private TransactionAdapter adapter;

    @Override
    protected FragmentAllTransactionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllTransactionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        loadTransactions();
        
        com.upreyvan.carti.data.local.ExpenseManager.getInstance().setOnExpenseChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::loadTransactions);
            }
        });
    }

    private void loadTransactions() {
        List<Transaction> transactions = com.upreyvan.carti.data.local.ExpenseManager.getInstance().getTransactions();
        adapter.submitList(transactions);
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.all_transactions_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        getBinding().rvAllTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvAllTransactions.setAdapter(adapter);

        com.upreyvan.carti.data.local.PreferenceManager pref = new com.upreyvan.carti.data.local.PreferenceManager(requireContext());

        adapter.setOnItemClickListener(item -> {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Transaction?")
                    .setMessage("Are you sure you want to delete this " + item.getTitle() + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        com.upreyvan.carti.data.remote.ApiHelper apiHelper = new com.upreyvan.carti.data.remote.ApiHelper(requireContext());
                        apiHelper.deleteTransaction(item.getId(), new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                // PURE CRUD: Calculate new totals in Java
                                double amount = item.getAmountDouble();
                                double currentBalance = pref.getBalance();
                                double currentIncome = pref.getTotalIncome();
                                double currentExpense = pref.getTotalExpense();

                                if ("INCOME".equals(item.getType())) {
                                    currentIncome -= amount;
                                } else {
                                    currentExpense -= amount;
                                }
                                currentBalance = currentIncome - currentExpense;

                                final double finalBalance = currentBalance;
                                final double finalIncome = currentIncome;
                                final double finalExpense = currentExpense;

                                // Update the new totals to Appwrite
                                apiHelper.updateFamilyTotals(finalBalance, finalIncome, finalExpense, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
                                    @Override
                                    public void onSuccess(java.util.Map<String, Object> result) {
                                        requireActivity().runOnUiThread(() -> {
                                            pref.saveFamilySummary(finalBalance, finalIncome, finalExpense);
                                            com.upreyvan.carti.data.local.ExpenseManager.getInstance().getTransactions().remove(item);
                                            com.upreyvan.carti.util.Utils.showToast(requireContext(), "Deleted and balance updated");
                                            loadTransactions();
                                        });
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        requireActivity().runOnUiThread(() -> loadTransactions());
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable error) {
                                requireActivity().runOnUiThread(() -> {
                                    com.upreyvan.carti.util.Utils.showToast(requireContext(), "Error: " + error.getMessage());
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
        });
    }

}