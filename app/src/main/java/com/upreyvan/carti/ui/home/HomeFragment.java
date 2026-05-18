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
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.DebtRepository;
import com.upreyvan.carti.data.repository.GoalRepository;
import com.upreyvan.carti.data.repository.IncomeRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.ui.expenses.AllTransactionsFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

/**
 * Senior Developer Refactored: HomeFragment.
 * It manages recent transactions and daily budget overview.
 */
public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private GenericAdapter<QuickLogItem, ItemQuickLogBinding> quickLogAdapter;
    private TransactionAdapter transactionAdapter;
    private boolean isExpanded = false;
    private Realtime realtime;
    private RealtimeSubscription userSubscription;
    private RealtimeSubscription familySubscription;
    private RealtimeSubscription transactionSubscription;
    private RealtimeSubscription goalSubscription;
    private RealtimeSubscription debtSubscription;
    private RealtimeSubscription memberSubscription;
    private TransactionRepository transactionRepository;
    private GoalRepository goalRepository;
    private DebtRepository debtRepository;
    private MemberRepository memberRepository;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = new TransactionRepository(requireContext());
        goalRepository = new GoalRepository(requireContext());
        debtRepository = new DebtRepository(requireContext());
        memberRepository = new MemberRepository(requireContext());
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().home, 0.3f);
        setupHeaders();
        setupQuickLog();
        setupRecentTransactions();
        setupNotifications();
        initRealtime();
        
        observeTransactions();
        setupDailyBudgetCard();
        fetchFamilyData();
    }

    private void observeTransactions() {
        transactionRepository.getRecentTransactions(5).observe(getViewLifecycleOwner(), transactions -> {
            if (transactionAdapter != null) {
                transactionAdapter.setLoading(false);
                transactionAdapter.submitList(transactions);
            }
        });

        transactionRepository.syncTransactionsIfNeeded();
    }

    private void initRealtime() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        ApiHelper apiHelper = new ApiHelper(requireContext());
        realtime = new Realtime(AppwriteManager.getInstance(requireContext()).getClient());
        
        String familyId = pref.getFamilyId();
        
        // Listen for user changes (for notification badge/join requests)
        String userChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_USERS + ".documents";
        userSubscription = realtime.subscribe(new String[]{userChannel}, event -> {
            checkNotifications(apiHelper, pref);
            return null;
        });

        // Listen for new transactions
        String transactionChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_TRANSACTIONS + ".documents";
        transactionSubscription = realtime.subscribe(new String[]{transactionChannel}, event -> {
            if (transactionRepository != null) {
                transactionRepository.refreshTransactions();
            }
            return null;
        });

        // Listen for goal changes
        String goalChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_GOALS + ".documents";
        goalSubscription = realtime.subscribe(new String[]{goalChannel}, event -> {
            if (goalRepository != null) {
                goalRepository.refreshGoals();
            }
            return null;
        });

        // Listen for debt changes
        String debtChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_DEBTS + ".documents";
        debtSubscription = realtime.subscribe(new String[]{debtChannel}, event -> {
            if (debtRepository != null) {
                debtRepository.refreshDebts();
            }
            return null;
        });

        // Listen for member changes
        memberSubscription = realtime.subscribe(new String[]{userChannel}, event -> {
            if (memberRepository != null) {
                memberRepository.refreshMembers();
            }
            return null;
        });

        // Listen for family document changes (for net balance)
        if (familyId != null && !familyId.isEmpty()) {
            String familyChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_FAMILIES + ".documents." + familyId;
            familySubscription = realtime.subscribe(new String[]{familyChannel}, event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) event.getPayload();
                if (data != null) {
                    double balance = Utils.getDouble(data.get("balance"));
                    double income = Utils.getDouble(data.get("totalIncome"));
                    double expense = Utils.getDouble(data.get("totalExpense"));
                    
                    pref.saveFamilySummary(balance, income, expense);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(this::setupHeaders);
                    }
                }
                return null;
            });
        }
    }

    private void fetchFamilyData() {
        ApiHelper apiHelper = new ApiHelper(requireContext());
        PreferenceManager pref = new PreferenceManager(requireContext());

        // Fetch Family Summary for Net Income
        apiHelper.getFamilySummary(new AppwriteManager.AppwriteCallback<io.appwrite.models.Document<Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.Document<Map<String, Object>> result) {
                if (!isAdded()) return;
                Map<String, Object> data = result.getData();
                double balance = Utils.getDouble(data.get("balance"));
                double income = Utils.getDouble(data.get("totalIncome"));
                double expense = Utils.getDouble(data.get("totalExpense"));
                
                pref.saveFamilySummary(balance, income, expense);
                requireActivity().runOnUiThread(() -> setupHeaders());
            }

            @Override
            public void onError(Throwable error) {
                // Silently fail or use local data
            }
        });

        // Check for notifications/join requests if admin
        checkNotifications(apiHelper, pref);
    }

    private void checkNotifications(ApiHelper apiHelper, PreferenceManager pref) {
        boolean isAdmin = false;
        String role = pref.getUserRole();
        for (String r : com.upreyvan.carti.util.Constants.Roles.PARENTS) {
            if (r.equalsIgnoreCase(role)) {
                isAdmin = true;
                break;
            }
        }

        if (isAdmin) {
            apiHelper.getMembers(new AppwriteManager.AppwriteCallback<io.appwrite.models.DocumentList<Map<String, Object>>>() {
                @Override
                public void onSuccess(io.appwrite.models.DocumentList<Map<String, Object>> result) {
                    if (!isAdded()) return;
                    boolean hasPending = false;
                    for (io.appwrite.models.Document<Map<String, Object>> doc : result.getDocuments()) {
                        Object status = doc.getData().get("status");
                        if ("pending".equals(status)) {
                            hasPending = true;
                            break;
                        }
                    }
                    final boolean finalHasPending = hasPending;
                    requireActivity().runOnUiThread(() -> updateNotificationBadge(finalHasPending));
                }

                @Override
                public void onError(Throwable error) {
                    requireActivity().runOnUiThread(() -> updateNotificationBadge(false));
                }
            });
        } else {
            updateNotificationBadge(false);
        }
    }

    private void setupDailyBudgetCard() {
        IncomeRepository incomeRepository = new IncomeRepository(requireContext());
        SalaryManager salaryManager = SalaryManager.getInstance(requireContext());
        
        incomeRepository.getTotalIncome().observe(getViewLifecycleOwner(), totalIncome -> {
            double amount = totalIncome != null ? totalIncome : 0.0;
            double dailyBudget = salaryManager.getDailyBudget(amount);
            int daysLeft = salaryManager.getDaysUntilNextPayday();

            getBinding().cardBudget.tvAmount.setText(String.format(Locale.getDefault(), "₱%,.0f", dailyBudget));
            getBinding().cardBudget.tvSalaryInfo.setText(getString(R.string.income_info_format, daysLeft));

            transactionRepository.getTodayTotalSpent().observe(getViewLifecycleOwner(), todaySpent -> {
                updateBudgetCard(dailyBudget, todaySpent != null ? todaySpent : 0.0);
            });
        });
        incomeRepository.refreshIncomes();
    }

    private void updateBudgetCard(double dailyBudget, double todaySpent) {
        double remaining = dailyBudget - todaySpent;
        
        int progress = (dailyBudget > 0) ? (int) ((todaySpent / dailyBudget) * 100) : 0;
        getBinding().cardBudget.progressDaily.setProgress(Math.min(progress, 100));
        
        getBinding().cardBudget.tvStatus.setText(String.format(Locale.getDefault(), "₱%,.0f left", Math.max(0, remaining)));
    }

    private void loadTransactions() {
        // Deprecated: now using observeTransactions()
    }

    @Override
    public void onDestroyView() {
        if (userSubscription != null) userSubscription.close();
        if (familySubscription != null) familySubscription.close();
        if (transactionSubscription != null) transactionSubscription.close();
        if (goalSubscription != null) goalSubscription.close();
        if (debtSubscription != null) debtSubscription.close();
        if (memberSubscription != null) memberSubscription.close();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupQuickLog();
        setupDailyBudgetCard();
    }

    private void updateNotificationBadge(boolean hasNotifications) {
        if (hasNotifications) {
            getBinding().notifBadge.setVisibility(View.VISIBLE);
            getBinding().notifBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.carti_primary_green));
        } else {
            getBinding().notifBadge.setVisibility(View.GONE);
        }
    }

    private void setupNotifications() {
        getBinding().btnNotif.setOnClickListener(v -> navigateTo(new NotificationsFragment()));
    }

    private void setupHeaders() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String name = pref.getUserName();
        String greeting = Utils.getGreeting();
        getBinding().tvGreetingMain.setText(greeting + ",");
        getBinding().tvUsernameMain.setText(name + " 👋");

        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        double net = income - expense;
        getBinding().tvGreetingSub.setText(getString(R.string.family_label, String.format(Locale.getDefault(), "₱%,.0f", net)));

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

        quickLogAdapter = new GenericAdapter<>(
                QuickLogItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemQuickLogBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvLabel.setText(item.getTitle());
                    binding.ivIcon.setImageResource(item.getIconRes());

                    int iconColor = ContextCompat.getColor(requireContext(), item.getIconColor());
                    int bgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(iconColor, 25);
                    binding.cvIconBg.setCardBackgroundColor(bgColor);
                    binding.ivIcon.setColorFilter(iconColor);
                }
        );
        
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
                return true;
            }
            return false;
        });

        quickLogAdapter.submitList(items);
        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
    }

    private void showDeleteCategoryDialog(QuickLogItem item) {
        com.upreyvan.carti.util.DialogHelper.showConfirmation(
                requireContext(),
                "Delete Category",
                "Are you sure you want to delete \"" + item.getTitle() + "\"?",
                "Delete",
                () -> deleteCategory(item.getTitle())
        );
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
            setupQuickLog();
            showToast(categoryName + " deleted", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
        }
    }

    private void showQuickLogDialog(QuickLogItem item) {
        QuickLogDialog dialog = QuickLogDialog.newInstance(item);
        dialog.setListener((loggedItem, amount) -> {
            showToast("Logged ₱" + String.format("%.2f", amount) + " for " + loggedItem.getTitle(), 
                    com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
        });
        dialog.show(getChildFragmentManager(), "QUICK_LOG_DIALOG");
    }

    private void setupRecentTransactions() {
        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

}
