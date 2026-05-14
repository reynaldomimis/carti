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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        
        ExpenseManager.getInstance().setOnExpenseChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadTransactions();
                    setupDailyBudgetCard();
                });
            }
        });

        fetchCloudData();
    }

    private void fetchCloudData() {
        new ApiHelper(requireContext()).sync("", "2000-01-01", "2099-12-31", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (!isAdded()) return;

                // Handle Summary (Salary/Budget)
                Map<String, Object> summary = (Map<String, Object>) result.get("summary");
                if (summary != null) {
                    Object balance = summary.get("balance");
                    if (balance != null) {
                        float amount = Float.parseFloat(String.valueOf(balance));
                        SalaryManager.getInstance(requireContext()).setSalaryAmount(amount);
                    }
                }

                // Handle Transactions
                List<Map<String, Object>> transactionsData = (List<Map<String, Object>>) result.get("transactions");
                if (transactionsData != null) {
                    List<Transaction> cloudTransactions = new ArrayList<>();
                    CategoryManager categoryManager = CategoryManager.getInstance(requireContext());
                    List<Category> categories = categoryManager.getCategories();

                    for (Map<String, Object> data : transactionsData) {
                        String categoryName = String.valueOf(data.get("category"));
                        double amount = Double.parseDouble(String.valueOf(data.get("amount")));
                        long timestamp = Long.parseLong(String.valueOf(data.get("timestamp")));

                        Category cat = findCategory(categories, categoryName);
                        int iconRes = (cat != null) ? cat.getIconRes() : R.drawable.ic_chart;
                        int iconColor = (cat != null) ? ContextCompat.getColor(requireContext(), cat.getIconColor()) : ContextCompat.getColor(requireContext(), R.color.icon_others);
                        int bgColor = (cat != null) ? ContextCompat.getColor(requireContext(), cat.getBackgroundColor()) : ColorUtils.setAlphaComponent(iconColor, 25);

                        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp);

                        cloudTransactions.add(new Transaction(
                                categoryName,
                                time,
                                "₱" + String.format(Locale.getDefault(), "%,.0f", amount),
                                iconRes,
                                bgColor,
                                iconColor,
                                timestamp
                        ));
                    }
                    ExpenseManager.getInstance().setTransactions(cloudTransactions);
                }

                // Handle Debts
                List<Map<String, Object>> debtsData = (List<Map<String, Object>>) result.get("debts");
                if (debtsData != null) {
                    List<Debt> cloudDebts = new ArrayList<>();
                    for (Map<String, Object> data : debtsData) {
                        String name = String.valueOf(data.get("personName"));
                        String description = String.valueOf(data.get("description"));
                        if (description == null || "null".equals(description)) description = "";
                        double amount = Double.parseDouble(String.valueOf(data.get("amount")));
                        boolean isPaid = false;
                        Object paid = data.get("isPaid");
                        if (paid instanceof Boolean) isPaid = (Boolean) paid;
                        
                        long timestamp = 0;
                        if (data.get("timestamp") != null) {
                            timestamp = Long.parseLong(String.valueOf(data.get("timestamp")));
                        }
                        String date = timestamp > 0 ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestamp) : "Today";
                        
                        cloudDebts.add(new Debt(name, description, date, amount, isPaid, R.drawable.ic_person, ""));
                    }
                    DebtManager.getInstance().setDebts(cloudDebts);
                }

                // Handle Goals
                List<Map<String, Object>> goalsData = (List<Map<String, Object>>) result.get("goals");
                if (goalsData != null) {
                    List<Goal> cloudGoals = new ArrayList<>();
                    int[] goalColors = {R.color.goal_card_1, R.color.goal_card_2, R.color.goal_card_3, R.color.goal_card_4, R.color.goal_card_5};
                    int colorIdx = 0;
                    
                    for (Map<String, Object> data : goalsData) {
                        String name = String.valueOf(data.get("name"));
                        double targetAmount = Double.parseDouble(String.valueOf(data.get("targetAmount")));
                        double currentAmount = 0;
                        if (data.get("currentAmount") != null) {
                            currentAmount = Double.parseDouble(String.valueOf(data.get("currentAmount")));
                        }
                        
                        int bgColor = ContextCompat.getColor(requireContext(), goalColors[colorIdx % goalColors.length]);
                        colorIdx++;
                        
                        cloudGoals.add(new Goal(name, currentAmount, targetAmount, "Target Date", R.drawable.test, bgColor));
                    }
                    GoalManager.getInstance().setGoals(cloudGoals);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Sync failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        PreferenceManager pref = new PreferenceManager(requireContext());
        String name = pref.getUserName();
        String greeting = Utils.getGreeting();
        getBinding().tvGreetingMain.setText(greeting + ",");
        getBinding().tvUsernameMain.setText(name + " 👋");

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
