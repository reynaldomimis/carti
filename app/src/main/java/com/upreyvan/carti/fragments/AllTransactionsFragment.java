package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.adapters.TransactionAdapter;
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
        populateMockData();
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

    private void populateMockData() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(getString(R.string.label_food), "Today • 12:30 PM", "₱250", android.R.drawable.ic_menu_gallery, ContextCompat.getColor(requireContext(), R.color.log_food)));
        transactions.add(new Transaction(getString(R.string.label_fare), "Today • 8:15 AM", "₱45", android.R.drawable.ic_dialog_map, ContextCompat.getColor(requireContext(), R.color.log_fare)));
        transactions.add(new Transaction(getString(R.string.label_store), "Yesterday • 6:15 PM", "₱120", android.R.drawable.ic_input_add, ContextCompat.getColor(requireContext(), R.color.log_store)));
        transactions.add(new Transaction(getString(R.string.label_electricity), "May 10 • 2:00 PM", "₱1,500", android.R.drawable.ic_lock_power_off, ContextCompat.getColor(requireContext(), R.color.log_electricity)));
        transactions.add(new Transaction(getString(R.string.label_water), "May 08 • 10:00 AM", "₱450", android.R.drawable.ic_menu_compass, ContextCompat.getColor(requireContext(), R.color.log_water)));
        transactions.add(new Transaction(getString(R.string.label_others), "May 05 • 4:30 PM", "₱300", android.R.drawable.ic_menu_more, ContextCompat.getColor(requireContext(), R.color.log_others)));
        
        adapter.submitList(transactions);
    }
}