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
    }

}