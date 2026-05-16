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
import com.upreyvan.carti.data.local.PreferenceManager;
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
import com.upreyvan.carti.data.local.DebtManager;
import com.upreyvan.carti.data.local.GoalManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.Utils;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Senior Developer Refactored: HomeFragment.
 * It manages recent transactions and daily budget overview.
 */
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
        
        loadTransactions();
        setupDailyBudgetCard();
        
        // Listener for local changes
        ExpenseManager.getInstance().setOnExpenseChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadTransactions();
                    setupDailyBudgetCard();
                });
            }
        });
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
        PreferenceManager pref = new PreferenceManager(requireContext());
        String name = pref.getUserName();
        String greeting = Utils.getGreeting();
        getBinding().tvGreetingMain.setText(greeting + ",");
        getBinding().tvUsernameMain.setText(name + " 👋");

        getBinding().headerQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().headerQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().headerQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        
        getBinding().headerQuickLog.btnSectionAction.setText(R.string.customize);
        getBinding().headerQuickLog.btnSectionAction.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class));
        });

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

        quickLogAdapter.setOnItemLongClickListener(item -> {
            if (!item.getTitle().equals(othersLabel) && !item.getTitle().equals(seeLessLabel)) {
                showDeleteCategoryDialog(item);
            }
        });

        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
    }

    private void showDeleteCategoryDialog(QuickLogItem item) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete \"" + item.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCategory(item.getTitle());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCategory(String categoryName) {
        CategoryManager manager = CategoryManager.getInstance(requireContext());
        List<Category> categories = manager.getCategories();
        Category toRemove = null;
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(categoryName)) {
                toRemove = cat;
                break;
            }
        }
        if (toRemove != null) {
            categories.remove(toRemove);
            manager.updateCategories(categories);
            setupQuickLog(); // Refresh UI
            Toast.makeText(requireContext(), categoryName + " deleted", Toast.LENGTH_SHORT).show();
        }
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
