package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAllTransactionsBinding;

public class AllTransactionsFragment extends BaseFragment<FragmentAllTransactionsBinding> {

    private TransactionAdapter adapter;
    private TransactionRepository transactionRepository;

    @Override
    protected FragmentAllTransactionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllTransactionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = new TransactionRepository(requireContext());
        setupToolbar();
        setupRecyclerView();
        observeTransactions();
    }

    private void observeTransactions() {
        transactionRepository.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                adapter.submitList(transactions);
            }
        });
        transactionRepository.syncTransactionsIfNeeded();
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

        adapter.setOnItemClickListener(itemWithUser -> {
            com.upreyvan.carti.model.Transaction item = itemWithUser.getTransaction();
            com.upreyvan.carti.util.DialogHelper.showConfirmation(
                    requireContext(),
                    "Delete Transaction?",
                    "Are you sure you want to delete this " + item.getTitle() + "?",
                    "Delete",
                    () -> {
                        com.upreyvan.carti.data.remote.ApiHelper apiHelper = new com.upreyvan.carti.data.remote.ApiHelper(requireContext());
                        apiHelper.deleteTransaction(item.getId(), new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                double amount = item.getAmount();
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

                                apiHelper.updateFamilyTotals(finalBalance, finalIncome, finalExpense, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
                                    @Override
                                    public void onSuccess(java.util.Map<String, Object> result) {
                                        requireActivity().runOnUiThread(() -> {
                                            pref.saveFamilySummary(finalBalance, finalIncome, finalExpense);
                                            transactionRepository.deleteLocally(item.getId());
                                            showToast(getString(R.string.msg_deleted_balance_updated), com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                                        });
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        requireActivity().runOnUiThread(() -> observeTransactions());
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable error) {
                                requireActivity().runOnUiThread(() -> {
                                    showToast(getString(R.string.err_generic, error.getMessage()), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                                });
                            }
                        });
                    }
            );
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionRepository != null) {
            transactionRepository.onDestroy();
        }
    }
}
