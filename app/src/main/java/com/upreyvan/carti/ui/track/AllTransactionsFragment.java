package com.upreyvan.carti.ui.track;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAllTransactionsBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.ui.home.CommentsBottomSheetFragment;
import com.upreyvan.carti.ui.home.ReactionsBottomSheetFragment;
import com.upreyvan.carti.ui.goals.UpdateGoalBottomSheetFragment;
import com.upreyvan.carti.utils.DialogHelper;
import com.upreyvan.carti.utils.UiHelper;

import java.util.ArrayList;

public class AllTransactionsFragment extends BaseFragment<FragmentAllTransactionsBinding> {

    private static final String ARG_TYPE = "transaction_type";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_CATEGORY = "category_name";

    private TransactionAdapter adapter;
    private AllTransactionsViewModel viewModel;
    
    private String filterType;
    private String filterUserId;
    private String filterUserName;
    private String filterCategory;

    public static AllTransactionsFragment newInstance(String type) {
        return newInstance(type, null, null);
    }

    public static AllTransactionsFragment newInstance(String type, String userId, String userName) {
        return newInstance(type, userId, userName, null);
    }

    public static AllTransactionsFragment newInstance(String type, String userId, String userName, String category) {
        AllTransactionsFragment fragment = new AllTransactionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_CATEGORY, category);
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
            filterCategory = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Override
    protected FragmentAllTransactionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllTransactionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AllTransactionsViewModel.class);
        
        setupToolbar();
        setupSearchBar();
        setupRecyclerView();
        observeViewModel();
        
        // Initialize filters in ViewModel
        viewModel.setFilters(filterType, filterUserId, filterCategory, "");
    }

    private void setupSearchBar() {
        getBinding().layoutToolbar.btnSearchToggle.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnSearchToggle.setOnClickListener(v -> showSearch());
        getBinding().layoutToolbar.btnClearSearch.setOnClickListener(v -> {
            if (getBinding().layoutToolbar.etSearch.getText().toString().isEmpty()) hideSearch();
            else getBinding().layoutToolbar.etSearch.setText("");
        });
        
        getBinding().layoutToolbar.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                viewModel.setSearchQuery(s.toString()); 
            }
            @Override public void afterTextChanged(Editable s) {}
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

    private void observeViewModel() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), list -> {
            getBinding().pbLoading.setVisibility(View.GONE);
            if (list == null || list.isEmpty()) {
                getBinding().rvAllTransactions.setVisibility(View.GONE);
                getBinding().layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                getBinding().layoutEmptyState.setVisibility(View.GONE);
                getBinding().rvAllTransactions.setVisibility(View.VISIBLE);
                adapter.submitList(new ArrayList<>(list));
            }
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> 
            getBinding().pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        TextView title = getBinding().layoutToolbar.tvToolbarTitle;
        if (filterUserName != null) {
            title.setText(getString(R.string.label_user_expenses, filterUserName));
        } else if (filterCategory != null) {
            title.setText(filterCategory);
        } else if ("EXPENSE".equals(filterType)) {
            title.setText(R.string.expenses_title);
        } else {
            title.setText(R.string.all_transactions_title);
        }
    }

    private void setupRecyclerView() {
        String currentUserId = com.upreyvan.carti.managers.PreferenceManager.getInstance(requireContext()).getUserId();
        adapter = new TransactionAdapter(currentUserId);
        adapter.setOnTransactionInteractionListener(new TransactionAdapter.OnTransactionInteractionListener() {
            @Override public void onLikeClick(TransactionWithUser item) { viewModel.toggleLike(item); }
            @Override public void onReactionClick(TransactionWithUser item, String emoji) { viewModel.toggleReaction(item, emoji); }
            @Override public void onCommentClick(Transaction transaction) { CommentsBottomSheetFragment.newInstance(transaction.getId()).show(getChildFragmentManager(), "CommentsBottomSheet"); }
            @Override public void onViewLikesClick(Transaction transaction, String reactorNames) { ReactionsBottomSheetFragment.newInstance(transaction.getId()).show(getChildFragmentManager(), "ReactionsBottomSheet"); }

            @Override
            public void onEditClick(Transaction transaction) {
                String type = transaction.getType();
                if ("INCOME".equals(type)) {
                    IncomeEditBottomSheet.newInstance(transaction).show(getChildFragmentManager(), "EditIncome");
                } else if ("GOAL".equals(type)) {
                    UpdateGoalBottomSheetFragment.newInstance(transaction.getId()).show(getChildFragmentManager(), "EditGoal");
                } else if ("EXPENSE".equals(type) || "BILL".equals(type) || "DEBT".equals(type)) {
                    ExpenseEditBottomSheet.newInstance(transaction).show(getChildFragmentManager(), "EditExpense");
                } else {
                    showToast("Edit for " + type + " coming soon", UiHelper.Status.INFO);
                }
            }

            @Override
            public void onDeleteClick(Transaction transaction) {
                DialogHelper.showConfirmation(requireContext(), "Delete Transaction?", 
                    "Are you sure you want to delete this " + (transaction.getTitle() != null ? transaction.getTitle() : "transaction") + "?", 
                    "Delete", () -> {
                    viewModel.deleteTransaction(transaction, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Object>() {
                        @Override public void onSuccess(Object result) {
                            showToast(getString(R.string.msg_deleted_balance_updated), UiHelper.Status.SUCCESS);
                        }
                        @Override public void onError(Throwable error) { 
                            showToast(getString(R.string.err_generic, error.getMessage()), UiHelper.Status.ERROR);
                        }
                    });
                });
            }
        });

        getBinding().rvAllTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvAllTransactions.setAdapter(adapter);
    }

    @Override public void onDestroyView() { super.onDestroyView(); }
}
