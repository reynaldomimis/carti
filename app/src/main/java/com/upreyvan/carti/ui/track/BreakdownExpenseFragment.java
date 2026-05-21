package com.upreyvan.carti.ui.track;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentBreakdownExpenseBinding;
import com.upreyvan.carti.databinding.ItemCategoryBreakdownBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class BreakdownExpenseFragment extends BaseFragment<FragmentBreakdownExpenseBinding> {

    private static final String ARG_START_MILLIS = "start_millis";
    private static final String ARG_END_MILLIS = "end_millis";

    private TransactionRepository transactionRepository;
    private GenericAdapter<TransactionWithUser, ItemCategoryBreakdownBinding> adapter;
    private long startMillis;
    private long endMillis;

    public static BreakdownExpenseFragment newInstance(long startMillis, long endMillis) {
        BreakdownExpenseFragment fragment = new BreakdownExpenseFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_START_MILLIS, startMillis);
        args.putLong(ARG_END_MILLIS, endMillis);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            startMillis = getArguments().getLong(ARG_START_MILLIS);
            endMillis = getArguments().getLong(ARG_END_MILLIS);
        }
    }

    @Override
    protected FragmentBreakdownExpenseBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBreakdownExpenseBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        
        setupToolbar(getBinding().layoutToolbar, "Expense Breakdown");
        setupRecyclerView();
        observeBreakdown();
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                TransactionWithUser.DIFF_CALLBACK,
                (inflater, parent) -> ItemCategoryBreakdownBinding.inflate(inflater, parent, false),
                (binding, itemWithUser) -> {
                    Transaction item = itemWithUser.getTransaction();
                    binding.tvCategory.setText(item.getCategory());
                    binding.tvDescription.setText(item.getDescription());
                    binding.tvAmount.setText(Utils.formatCurrency(item.getAmount()));
                    binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
                    
                    // Icon logic using CategoryManager
                    Category cat = findCategory(item.getCategory());
                    if (cat != null) {
                        binding.ivIcon.setImageResource(cat.getIconRes());
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(cat.getIconColor(), null)));
                        binding.cvIcon.setCardBackgroundColor(getResources().getColor(cat.getBackgroundColor(), null));
                    } else {
                        binding.ivIcon.setImageResource(R.drawable.ic_sync); 
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray, null)));
                        binding.cvIcon.setCardBackgroundColor(getResources().getColor(R.color.surface_variant, null));
                    }

                    binding.getRoot().setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance("EXPENSE", item.getUserId(), itemWithUser.getUserName())));
                }
        );
        getBinding().rvContributors.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvContributors.setAdapter(adapter);
    }

    private Category findCategory(String name) {
        List<Category> categories = CategoryManager.getInstance(requireContext()).getCategories();
        for (Category c : categories) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    private void observeBreakdown() {
        transactionRepository.getTransactionsInRange(startMillis, endMillis).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<TransactionWithUser> expenseList = new ArrayList<>();
                for (TransactionWithUser tWithU : transactions) {
                    if ("EXPENSE".equals(tWithU.getTransaction().getType())) {
                        expenseList.add(tWithU);
                    }
                }

                // Sorting by timestamp descending (newest first)
                expenseList.sort((t1, t2) -> Long.compare(t2.getTransaction().getTimestampMillis(), t1.getTransaction().getTimestampMillis()));
                
                adapter.submitList(expenseList);
                
                boolean isEmpty = expenseList.isEmpty();
                getBinding().rvContributors.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }
}