package com.upreyvan.carti.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.ExpenseManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.ui.expenses.AllTransactionsFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private QuickLogAdapter quickLogAdapter;
    private TransactionAdapter transactionAdapter;
    private boolean isExpanded = false;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupHeaders();
        setupQuickLog();
        setupRecentTransactions();
        setupNotifications();
        updateNotificationBadge(true);
        
        // Populate mock data into ExpenseManager if it's empty
        if (ExpenseManager.getInstance().getTransactions().isEmpty()) {
            populateInitialData();
        }
        
        loadTransactions();
        setupDailyBudgetCard();
        
        ExpenseManager.getInstance().setOnExpenseChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadTransactions();
                    setupDailyBudgetCard();
                });
            }
        });
    }

    private void populateInitialData() {
        CategoryManager categoryManager = CategoryManager.getInstance(requireContext());
        List<Category> categories = categoryManager.getCategories();
        List<Transaction> transactions = new ArrayList<>();
        
        // Find categories to match mock data
        Category foodCat = findCategory(categories, "Food");
        Category fareCat = findCategory(categories, "Fare");
        Category storeCat = findCategory(categories, "Sari-sari Store");
        
        if (foodCat != null) {
            transactions.add(new Transaction(foodCat.getName(), getString(R.string.mock_time_1), getString(R.string.mock_amount_120), 
                foodCat.getIconRes(), ContextCompat.getColor(requireContext(), foodCat.getBackgroundColor()), ContextCompat.getColor(requireContext(), foodCat.getIconColor())));
        }
        if (fareCat != null) {
            transactions.add(new Transaction(fareCat.getName(), getString(R.string.mock_time_2), getString(R.string.mock_amount_15), 
                fareCat.getIconRes(), ContextCompat.getColor(requireContext(), fareCat.getBackgroundColor()), ContextCompat.getColor(requireContext(), fareCat.getIconColor())));
        }
        if (storeCat != null) {
            transactions.add(new Transaction(storeCat.getName(), getString(R.string.mock_time_3), getString(R.string.mock_amount_85), 
                storeCat.getIconRes(), ContextCompat.getColor(requireContext(), storeCat.getBackgroundColor()), ContextCompat.getColor(requireContext(), storeCat.getIconColor())));
        }
        
        for (Transaction t : transactions) {
            ExpenseManager.getInstance().addTransaction(t);
        }
    }

    private Category findCategory(List<Category> categories, String name) {
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(name)) return cat;
        }
        return null;
    }

    private void setupDailyBudgetCard() {
        SalaryManager salaryManager = SalaryManager.getInstance(requireContext());
        double dailyBudget = salaryManager.getDailyBudget();
        int daysLeft = salaryManager.getDaysUntilNextPayday();
        double todaySpent = ExpenseManager.getInstance().getTodayTotalSpent();
        double remaining = dailyBudget - todaySpent;
        
        getBinding().cardBudget.tvAmount.setText(String.format(Locale.getDefault(), "₱%,.0f", dailyBudget));
        getBinding().cardBudget.tvSalaryInfo.setText(getString(R.string.salary_info_format, daysLeft));
        
        int progress = (dailyBudget > 0) ? (int) ((todaySpent / dailyBudget) * 100) : 0;
        getBinding().cardBudget.progressDaily.setProgress(Math.min(progress, 100));
        
        getBinding().cardBudget.tvStatus.setText(String.format(Locale.getDefault(), "₱%,.0f left", Math.max(0, remaining)));
    }

    private void loadTransactions() {
        List<Transaction> transactions = ExpenseManager.getInstance().getRecentTransactions(5);
        if (transactionAdapter != null) {
            transactionAdapter.submitList(transactions);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupQuickLog();
    }

    private void updateNotificationBadge(boolean hasNotifications) {
        if (hasNotifications) {
            getBinding().notifBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.carti_primary_green));
        } else {
            getBinding().notifBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
        }
    }

    private void setupNotifications() {
        getBinding().btnNotif.setOnClickListener(v -> navigateTo(new NotificationsFragment()));
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupHeaders() {
        // Quick Log Header
        getBinding().headerQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().headerQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().headerQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        
        // Palitan ang See All ng Customize (Text lang, action is in setupQuickLog for the adapter's See More item)
        getBinding().headerQuickLog.btnSectionAction.setText(R.string.customize);
        getBinding().headerQuickLog.btnSectionAction.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class));
        });

        // Recent Transactions Header
        getBinding().headerRecent.tvSectionTitle.setText(R.string.recent_transactions);
        getBinding().headerRecent.btnSectionAction.setText(R.string.see_all);
        getBinding().headerRecent.btnSectionAction.setOnClickListener(v -> navigateTo(new AllTransactionsFragment()));
    }

    private void setupQuickLog() {
        List<Category> categories = CategoryManager.getInstance(requireContext()).getCategories();
        List<QuickLogItem> items = new ArrayList<>();
        
        int limit = isExpanded ? categories.size() : 7;
        for (int i = 0; i < Math.min(categories.size(), limit); i++) {
            Category cat = categories.get(i);
            items.add(new QuickLogItem(cat.getName(), cat.getIconRes(), cat.getBackgroundColor(), cat.getIconColor()));
        }

        String othersLabel = getString(R.string.label_others);
        String seeLessLabel = getString(R.string.see_less);

        // See More / Others Item (Pang 8th or last item)
        if (categories.size() > 7 && !isExpanded) {
            items.add(new QuickLogItem(othersLabel, android.R.drawable.ic_menu_more, R.color.log_others, R.color.icon_others));
        } else if (isExpanded) {
            items.add(new QuickLogItem(seeLessLabel, android.R.drawable.ic_menu_close_clear_cancel, R.color.log_others, R.color.icon_others));
        }

        quickLogAdapter = new QuickLogAdapter(items);
        quickLogAdapter.setOnItemClickListener(item -> {
            if (item.getTitle().equals(othersLabel)) {
                isExpanded = true;
                setupQuickLog();
            } else if (item.getTitle().equals(seeLessLabel)) {
                isExpanded = false;
                setupQuickLog();
            } else {
                showQuickLogDialog(item);
            }
        });

        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
    }

    private void showQuickLogDialog(QuickLogItem item) {
        QuickLogDialog dialog = QuickLogDialog.newInstance(item);
        dialog.setListener((loggedItem, amount) -> {
            Toast.makeText(requireContext(), 
                "Logged ₱" + String.format("%.2f", amount) + " for " + loggedItem.getTitle(), 
                Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "QUICK_LOG_DIALOG");
    }

    private void setupRecentTransactions() {
        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }


    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutHeader, getBinding().home, 0.3f, getResources().getDimensionPixelSize(R.dimen.bottom_nav_medium));
    }
}
