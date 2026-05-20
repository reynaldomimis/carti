package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAllTransactionsBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.ToastHelper.Status;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AllTransactionsFragment extends BaseFragment<FragmentAllTransactionsBinding> {

    private TransactionAdapter adapter;
    private TransactionRepository transactionRepository;
    private Calendar currentDisplayDate;

    @Override
    protected FragmentAllTransactionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllTransactionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = new TransactionRepository(requireContext());
        currentDisplayDate = Calendar.getInstance();
        
        setupToolbar();
        setupMonthNavigation();
        setupRecyclerView();
        observeTransactions();
    }

    private void setupMonthNavigation() {
        updateMonthDisplay();
        
        getBinding().btnPrevMonth.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.MONTH, -1);
            updateMonthAndRefresh();
        });

        getBinding().btnNextMonth.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.MONTH, 1);
            updateMonthAndRefresh();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        getBinding().tvCurrentMonth.setText(sdf.format(currentDisplayDate.getTime()));
    }

    private void updateMonthAndRefresh() {
        updateMonthDisplay();
        observeTransactions();
    }

    private void observeTransactions() {
        transactionRepository.getTransactionsByMonth(
                currentDisplayDate.get(Calendar.MONTH),
                currentDisplayDate.get(Calendar.YEAR)
        ).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                adapter.submitList(transactions);
                
                getBinding().rvAllTransactions.setVisibility(transactions.isEmpty() ? View.GONE : View.VISIBLE);
                getBinding().layoutEmptyState.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
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

        adapter.setOnItemClickListener(itemWithUser -> {
            Transaction item = itemWithUser.getTransaction();
            DialogHelper.showConfirmation(
                    requireContext(),
                    "Delete Transaction?",
                    "Are you sure you want to delete this " + item.getName() + "?",
                    "Delete",
                    () -> {
                        ApiHelper apiHelper = new ApiHelper(requireContext());
                        apiHelper.deleteTransaction(item.getId(), new AppwriteCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                requireActivity().runOnUiThread(() -> {
                                    transactionRepository.deleteLocally(item.getId());
                                    showToast(getString(R.string.msg_deleted_balance_updated), Status.SUCCESS);
                                });
                            }

                            @Override
                            public void onError(Throwable error) {
                                requireActivity().runOnUiThread(() -> {
                                    showToast(getString(R.string.err_generic, error.getMessage()), Status.ERROR);
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