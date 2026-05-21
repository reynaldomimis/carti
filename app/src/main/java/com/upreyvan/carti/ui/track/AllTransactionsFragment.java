package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAllTransactionsBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.ToastHelper.Status;
import com.upreyvan.carti.util.Utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AllTransactionsFragment extends BaseFragment<FragmentAllTransactionsBinding> {

    private static final String ARG_TYPE = "transaction_type";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_NAME = "user_name";
    private TransactionAdapter adapter;
    private TransactionRepository transactionRepository;
    private Calendar currentDisplayDate;
    private String filterType;
    private String filterUserId;
    private String filterUserName;
    private List<TransactionWithUser> fullList = new ArrayList<>();
    private String currentQuery = "";

    public static AllTransactionsFragment newInstance(String type) {
        return newInstance(type, null, null);
    }

    public static AllTransactionsFragment newInstance(String type, String userId, String userName) {
        AllTransactionsFragment fragment = new AllTransactionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterType = getArguments().getString(ARG_TYPE);
            filterUserId = getArguments().getString(ARG_USER_ID);
            filterUserName = getArguments().getString(ARG_USER_NAME);
        }
    }

    @Override
    protected FragmentAllTransactionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllTransactionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        currentDisplayDate = Calendar.getInstance();
        
        setupToolbar();
        setupSearchBar();
        setupMonthNavigation();
        setupRecyclerView();
        observeTransactions();
    }

    private void setupSearchBar() {
        getBinding().layoutToolbar.btnSearchToggle.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnSearchToggle.setOnClickListener(v -> {
            showSearch();
        });

        getBinding().layoutToolbar.btnClearSearch.setOnClickListener(v -> {
            if (getBinding().layoutToolbar.etSearch.getText().toString().isEmpty()) {
                hideSearch();
            } else {
                getBinding().layoutToolbar.etSearch.setText("");
            }
        });

        getBinding().layoutToolbar.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showSearch() {
        getBinding().layoutToolbar.tvToolbarTitle.setVisibility(View.GONE);
        getBinding().layoutToolbar.btnSearchToggle.setVisibility(View.GONE);
        getBinding().layoutToolbar.layoutSearchContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.etSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(getBinding().layoutToolbar.etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearch() {
        getBinding().layoutToolbar.etSearch.setText("");
        getBinding().layoutToolbar.layoutSearchContainer.setVisibility(View.GONE);
        getBinding().layoutToolbar.tvToolbarTitle.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnSearchToggle.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getBinding().layoutToolbar.etSearch.getWindowToken(), 0);
    }

    private void filterBySearch(String query) {
        currentQuery = query.toLowerCase().trim();
        applyFilters();
    }

    private void applyFilters() {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        for (TransactionWithUser t : fullList) {
            boolean matchesType = filterType == null || filterType.equals(t.getTransaction().getType());
            boolean matchesUser = filterUserId == null || filterUserId.equals(t.getTransaction().getUserId());
            boolean matchesSearch = currentQuery.isEmpty() || 
                                   (t.getTransaction().getTitle() != null && t.getTransaction().getTitle().toLowerCase().contains(currentQuery)) ||
                                   (t.getTransaction().getCategory() != null && t.getTransaction().getCategory().toLowerCase().contains(currentQuery));
            
            if (matchesType && matchesUser && matchesSearch) {
                filteredList.add(t);
            }
        }
        adapter.submitList(filteredList);
        getBinding().rvAllTransactions.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
        getBinding().layoutEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
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
        getBinding().tvCurrentMonth.setText(Utils.formatMonthYear(currentDisplayDate));
    }

    private void updateMonthAndRefresh() {
        updateMonthDisplay();
        observeTransactions();
    }

    private androidx.lifecycle.Observer<List<TransactionWithUser>> transactionObserver = transactions -> {
        if (transactions != null) {
            fullList = transactions;
            applyFilters();
        }
    };
    private LiveData<List<TransactionWithUser>> currentLiveData;

    private void observeTransactions() {
        if (currentLiveData != null) {
            currentLiveData.removeObserver(transactionObserver);
        }
        
        currentLiveData = transactionRepository.getTransactionsByMonth(
                currentDisplayDate.get(Calendar.MONTH),
                currentDisplayDate.get(Calendar.YEAR)
        );
        currentLiveData.observe(getViewLifecycleOwner(), transactionObserver);

        transactionRepository.syncTransactionsIfNeeded();
    }

    private void setupToolbar() {
        if (filterUserName != null) {
            getBinding().layoutToolbar.tvToolbarTitle.setText(getString(R.string.label_user_expenses, filterUserName));
        } else if ("EXPENSE".equals(filterType)) {
            getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.nav_track);
        } else {
            getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.all_transactions_title);
        }
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
