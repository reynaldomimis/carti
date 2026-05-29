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
import com.upreyvan.carti.ui.home.ReactionsBottomSheetFragment;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.ui.home.CommentsBottomSheetFragment;
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
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
    private List<TransactionWithUser> displayList = new ArrayList<>();
    private String currentQuery = "";
    
    private int currentPage = 0;
    private static final int PAGE_SIZE = 12;
    private boolean isLoading = false;
    private boolean isLastPage = false;

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
        currentPage = 0;
        isLastPage = false;
        displayList.clear();
        applyFilters();
    }

    private void applyFilters() {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        for (TransactionWithUser t : fullList) {
            boolean matchesType = filterType == null || filterType.equals(t.getTransaction().getType());
            boolean matchesUser = filterUserId == null || filterUserId.equals(t.getTransaction().getUserId());

            String username = t.getUsername() != null ? t.getUsername().toLowerCase() : "";
            String category = t.getTransaction().getCategory() != null ? t.getTransaction().getCategory().toLowerCase() : "";
            String description = t.getTransaction().getDescription() != null ? t.getTransaction().getDescription().toLowerCase() : "";
            String amount = String.valueOf(t.getTransaction().getAmount());

            boolean matchesSearch = currentQuery.isEmpty() ||
                                   username.contains(currentQuery) ||
                                   category.contains(currentQuery) ||
                                   description.contains(currentQuery) ||
                                   amount.contains(currentQuery);

            if (matchesType && matchesUser && matchesSearch) {
                filteredList.add(t);
            }
        }

        if (filteredList.isEmpty()) {
            getBinding().rvAllTransactions.setVisibility(View.GONE);
            getBinding().layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            getBinding().layoutEmptyState.setVisibility(View.GONE);
            getBinding().rvAllTransactions.setVisibility(View.VISIBLE);
            loadNextPage(filteredList);
        }
    }

    private void loadNextPage(List<TransactionWithUser> sourceList) {
        if (isLoading || isLastPage) return;

        isLoading = true;
        getBinding().pbLoading.setVisibility(View.VISIBLE);
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded() || getBinding() == null) return;

            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, sourceList.size());

            if (start >= sourceList.size()) {
                isLastPage = true;
                isLoading = false;
                getBinding().pbLoading.setVisibility(View.GONE);
                return;
            }

            List<TransactionWithUser> pageItems = sourceList.subList(start, end);
            displayList.addAll(pageItems);
            adapter.submitList(new ArrayList<>(displayList));

            // Sync visibility after loading data
            getBinding().rvAllTransactions.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
            getBinding().layoutEmptyState.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);

            currentPage++;
            if (end >= sourceList.size()) {
                isLastPage = true;
            }
            isLoading = false;
            getBinding().pbLoading.setVisibility(View.GONE);
        }, 800);
    }

    private androidx.lifecycle.Observer<List<TransactionWithUser>> transactionObserver = transactions -> {
        if (transactions != null) {
            fullList = transactions;
            currentPage = 0;
            isLastPage = false;
            displayList.clear();
            applyFilters();
        }
    };
    private LiveData<List<TransactionWithUser>> currentLiveData;

    private void observeTransactions() {
        if (currentLiveData != null) {
            currentLiveData.removeObserver(transactionObserver);
        }
        
        currentLiveData = transactionRepository.getAllTransactions();
        currentLiveData.observe(getViewLifecycleOwner(), transactionObserver);

        transactionRepository.syncTransactionsIfNeeded();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.GONE);
        TextView title = getBinding().layoutToolbar.tvToolbarTitle;
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) title.getLayoutParams();
        lp.horizontalBias = 0.0f;
        lp.setMarginStart((int) getResources().getDimension(R.dimen.spacing_xs));
        title.setLayoutParams(lp);

        if (filterUserName != null) {
            title.setText(getString(R.string.label_user_expenses, filterUserName));
        } else if ("EXPENSE".equals(filterType)) {
            title.setText(R.string.expenses_title);
        } else {
            title.setText(R.string.all_transactions_title);
        }
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        // ... (existing adapter listener code remains same)
        adapter.setOnTransactionInteractionListener(new TransactionAdapter.OnTransactionInteractionListener() {
            @Override
            public void onLikeClick(Transaction transaction) {
                transactionRepository.likeTransaction(transaction.getId(), "👍", new AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                    }

                    @Override
                    public void onError(Throwable error) {
                        showToast("Failed to like", Status.ERROR);
                    }
                });
            }

            @Override
            public void onReactionClick(Transaction transaction, String emoji) {
                transactionRepository.likeTransaction(transaction.getId(), emoji, new AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                    }

                    @Override
                    public void onError(Throwable error) {
                        showToast("Failed to react", Status.ERROR);
                    }
                });
            }

            @Override
            public void onCommentClick(Transaction transaction) {
                CommentsBottomSheetFragment fragment = CommentsBottomSheetFragment.newInstance(transaction.getId());
                fragment.show(getChildFragmentManager(), "CommentsBottomSheet");
            }

            @Override
            public void onViewLikesClick(Transaction transaction, String reactorNames) {
                ReactionsBottomSheetFragment fragment = ReactionsBottomSheetFragment.newInstance(transaction.getId());
                fragment.show(getChildFragmentManager(), "ReactionsBottomSheet");
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        getBinding().rvAllTransactions.setLayoutManager(layoutManager);
        getBinding().rvAllTransactions.setAdapter(adapter);

        getBinding().rvAllTransactions.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_SIZE) {
                            
                            // Re-filter to get the list to slice from
                            List<TransactionWithUser> filteredList = new ArrayList<>();
                            for (TransactionWithUser t : fullList) {
                                boolean matchesType = filterType == null || filterType.equals(t.getTransaction().getType());
                                boolean matchesUser = filterUserId == null || filterUserId.equals(t.getTransaction().getUserId());

                                String username = t.getUsername() != null ? t.getUsername().toLowerCase() : "";
                                String category = t.getTransaction().getCategory() != null ? t.getTransaction().getCategory().toLowerCase() : "";
                                String description = t.getTransaction().getDescription() != null ? t.getTransaction().getDescription().toLowerCase() : "";
                                String amount = String.valueOf(t.getTransaction().getAmount());

                                boolean matchesSearch = currentQuery.isEmpty() ||
                                                       username.contains(currentQuery) ||
                                                       category.contains(currentQuery) ||
                                                       description.contains(currentQuery) ||
                                                       amount.contains(currentQuery);

                                if (matchesType && matchesUser && matchesSearch) {
                                    filteredList.add(t);
                                }
                            }
                            loadNextPage(filteredList);
                        }
                    }
                }
            }
        });

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
    }
}
