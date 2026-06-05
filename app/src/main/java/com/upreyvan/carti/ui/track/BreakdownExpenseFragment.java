package com.upreyvan.carti.ui.track;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_YEAR = "arg_year";

    private TransactionRepository transactionRepository;
    private GenericAdapter<TransactionWithUser, ItemCategoryBreakdownBinding> adapter;
    private int selectedMonth;
    private int selectedYear;

    public static BreakdownExpenseFragment newInstance(int month, int year) {
        BreakdownExpenseFragment fragment = new BreakdownExpenseFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedMonth = getArguments().getInt(ARG_MONTH);
            selectedYear = getArguments().getInt(ARG_YEAR);
        }
    }

    @Override
    protected FragmentBreakdownExpenseBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBreakdownExpenseBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof com.upreyvan.carti.MainActivity main) {
            main.setBottomNavVisibility(false);
        }
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
                    binding.tvDescription.setText(item.getNote());
                    binding.tvAmount.setText(Utils.formatCurrency(item.getAmount()));
                    binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
                    
                    Category cat = findCategory(item.getCategory());
                    if (cat != null) {
                        binding.ivIcon.setImageResource(cat.getIconRes());
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), cat.getIconColor())));
                        binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), cat.getBackgroundColor()));
                    } else {
                        binding.ivIcon.setImageResource(R.drawable.ic_sync); 
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray)));
                        binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant));
                    }

                    binding.getRoot().setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance("EXPENSE", item.getUserId(), itemWithUser.getUsername())));
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
        transactionRepository.getTransactionsByMonth(selectedMonth, selectedYear).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<TransactionWithUser> expenseList = new ArrayList<>();
                for (TransactionWithUser tWithU : transactions) {
                    if ("EXPENSE".equals(tWithU.getTransaction().getType())) {
                        expenseList.add(tWithU);
                    }
                }

                expenseList.sort((t1, t2) -> Long.compare(t2.getTransaction().getTimestampMillis(), t1.getTransaction().getTimestampMillis()));
                adapter.submitList(expenseList);
                
                boolean isEmpty = expenseList.isEmpty();
                getBinding().rvContributors.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }
}
