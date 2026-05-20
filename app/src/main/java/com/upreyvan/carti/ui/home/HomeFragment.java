package com.upreyvan.carti.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemAiSuggestionCardBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.ui.expenses.AllTransactionsFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import androidx.recyclerview.widget.DiffUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import com.upreyvan.carti.ui.bills.BillsActivity;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.DialogHelper;
import androidx.core.graphics.ColorUtils;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private GenericAdapter<QuickLogItem, ItemQuickLogBinding> quickLogAdapter;
    private GenericAdapter<AiSuggestion, ItemAiSuggestionCardBinding> aiSuggestionsAdapter;
    private TransactionAdapter transactionAdapter;
    private boolean isExpanded = false;
    private RealtimeSubscription userSubscription;
    private RealtimeSubscription familySubscription;
    private RealtimeSubscription transactionSubscription;
    private RealtimeSubscription memberSubscription;
    private TransactionRepository transactionRepository;
    private MemberRepository memberRepository;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = new TransactionRepository(requireContext());
        memberRepository = new MemberRepository(requireContext());
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().home, 0.3f);
        setupHeaders();
        setupQuickLog();
        setupRecentTransactions();
        setupNotifications();
        initRealtime();
        
        observeTransactions();
        setupDashboard();
        setupRecurringBills();
        setupAiInsightCard();
        setupAiSuggestions();
        fetchFamilyData();
    }

    private void setupAiSuggestions() {
        DiffUtil.ItemCallback<AiSuggestion> diffCallback = new DiffUtil.ItemCallback<AiSuggestion>() {
            @Override
            public boolean areItemsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getDescription().equals(newItem.getDescription());
            }
        };

        aiSuggestionsAdapter = new GenericAdapter<>(
                diffCallback,
                (inflater, parent) -> ItemAiSuggestionCardBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvTitle.setText(item.getTitle());
                    binding.tvDescription.setText(item.getDescription());
                    binding.ivIcon.setImageResource(item.getIconResId());
                    binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), item.getThemeColor()));
                    binding.btnAction.setText(item.getActionText());
                    binding.btnAction.setTextColor(ContextCompat.getColor(requireContext(), item.getThemeColor()));

                    binding.btnClose.setOnClickListener(v -> {
                        List<AiSuggestion> currentList = new ArrayList<>(aiSuggestionsAdapter.getCurrentList());
                        currentList.remove(item);
                        aiSuggestionsAdapter.submitList(currentList);
                    });

                    binding.getRoot().setOnClickListener(v -> {
                        // Handle card click
                    });
                }
        );

        getBinding().layoutAiSuggestions.rvAiSuggestions.setAdapter(aiSuggestionsAdapter);
        getBinding().layoutAiSuggestions.btnCloseContainer.setOnClickListener(v -> 
            getBinding().layoutAiSuggestions.getRoot().setVisibility(View.GONE)
        );

        loadAiSuggestions();
    }

    private void loadAiSuggestions() {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                getString(R.string.suggestion_predictive_alert),
                getString(R.string.desc_predictive_alert),
                R.drawable.ic_bell,
                R.color.status_red,
                getString(R.string.label_view)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.suggestion_savings_tip),
                getString(R.string.desc_savings_tip),
                R.drawable.ic_trophy,
                R.color.status_green,
                getString(R.string.label_apply)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.suggestion_bill_reminder),
                getString(R.string.desc_bill_reminder),
                R.drawable.ic_calendar,
                R.color.carti_primary_blue,
                getString(R.string.label_pay_now)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.suggestion_goal_progress),
                getString(R.string.desc_goal_progress),
                R.drawable.ic_chart,
                R.color.mint_green,
                getString(R.string.label_view)
        ));
        aiSuggestionsAdapter.submitList(suggestions);
    }

    private void setupAiInsightCard() {
        getBinding().layoutAiInsight.btnAskAi.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(6);
            }
        });

        getBinding().layoutAiInsight.btnViewReport.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(2);
            }
        });
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
        Realtime realtime = new Realtime(AppwriteManager.getInstance(requireContext()).getClient());
        
        String familyId = pref.getFamilyId();
        
        String userChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_USERS + ".documents";
        userSubscription = realtime.subscribe(new String[]{userChannel}, event -> {
            checkNotifications(new ApiHelper(requireContext()), pref);
            return null;
        });

        String transactionChannel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_TRANSACTIONS + ".documents";
        transactionSubscription = realtime.subscribe(new String[]{transactionChannel}, event -> {
            if (transactionRepository != null) {
                transactionRepository.refreshTransactions();
            }
            return null;
        });

        memberSubscription = realtime.subscribe(new String[]{userChannel}, event -> {
            if (memberRepository != null) {
                memberRepository.refreshMembers();
            }
            return null;
        });

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
                        requireActivity().runOnUiThread(() -> {
                            setupHeaders();
                            setupDashboard();
                        });
                    }
                }
                return null;
            });
        }
    }

    private void fetchFamilyData() {
        ApiHelper apiHelper = new ApiHelper(requireContext());
        PreferenceManager pref = new PreferenceManager(requireContext());

        apiHelper.getFamilySummary(new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Document<Map<String, Object>> result) {
                if (!isAdded()) return;
                Map<String, Object> data = result.getData();
                double balance = Utils.getDouble(data.get("balance"));
                double income = Utils.getDouble(data.get("totalIncome"));
                double expense = Utils.getDouble(data.get("totalExpense"));
                
                pref.saveFamilySummary(balance, income, expense);
                requireActivity().runOnUiThread(() -> {
                    setupHeaders();
                    setupDashboard();
                });
            }

            @Override
            public void onError(Throwable error) {
            }
        });

        checkNotifications(apiHelper, pref);
    }

    private void checkNotifications(ApiHelper apiHelper, PreferenceManager pref) {
        boolean isAdmin = false;
        String role = pref.getUserRole();
        for (String r : Constants.Roles.PARENTS) {
            if (r.equalsIgnoreCase(role)) {
                isAdmin = true;
                break;
            }
        }

        if (isAdmin) {
            apiHelper.getMembers(new AppwriteManager.AppwriteCallback<>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    if (!isAdded()) return;
                    boolean hasPending = false;
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Object status = doc.getData().get("status");
                        if (Objects.equals("pending", status)) {
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

    private void setupDashboard() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        
        double totalBalance = pref.getTotalIncome() - pref.getTotalExpense();
        double totalIncome = pref.getTotalIncome();
        double totalExpense = pref.getTotalExpense();
        
        // Monthly Income
        getBinding().layoutDashboard.tvIncomeAmount.setText(getString(R.string.format_currency_no_decimal, totalIncome));
        getBinding().layoutDashboard.tvIncomeTrend.setText("8.5%");
        
        // Monthly Expenses
        getBinding().layoutDashboard.tvExpensesAmount.setText(getString(R.string.format_currency_no_decimal, totalExpense));
        getBinding().layoutDashboard.tvExpensesTrend.setText("3.2%");
        
        // Total Savings
        getBinding().layoutDashboard.tvTotalSavings.setText(getString(R.string.format_currency, totalBalance));
        getBinding().layoutDashboard.tvSavingsTrend.setText("12.5%");

        String currentMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new java.util.Date());
        getBinding().layoutDashboard.tvOverviewDate.setText(currentMonth);
    }

    private void setupRecurringBills() {
        getBinding().layoutDashboard.btnViewAllBills.setOnClickListener(v -> 
            startActivity(new Intent(requireContext(), BillsActivity.class))
        );
    }

    @Override
    public void onDestroyView() {
        if (userSubscription != null) userSubscription.close();
        if (familySubscription != null) familySubscription.close();
        if (transactionSubscription != null) transactionSubscription.close();
        if (memberSubscription != null) memberSubscription.close();
        
        if (transactionRepository != null) transactionRepository.onDestroy();
        if (memberRepository != null) memberRepository.onDestroy();
        
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupQuickLog();
        setupDashboard();
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
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, greeting));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, name));

        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        double net = income - expense;
        getBinding().tvGreetingSub.setText(getString(R.string.family_label, String.format(Locale.getDefault(), "₱%,.0f", net)));

        getBinding().headerQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().headerQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().headerQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        
        getBinding().headerQuickLog.btnSectionAction.setText(R.string.customize);
        getBinding().headerQuickLog.btnSectionAction.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)));

        getBinding().headerRecent.tvSectionTitle.setText(R.string.recent_transactions);
        getBinding().headerRecent.btnSectionAction.setText(R.string.see_all);
        getBinding().headerRecent.btnSectionAction.setOnClickListener(v -> navigateTo(new AllTransactionsFragment()));
    }

    private void setupQuickLog() {
        List<Category> categories = CategoryManager.getInstance(requireContext()).getCategories();
        List<QuickLogItem> items = new ArrayList<>();
        
        int limit = isExpanded ? categories.size() : 7;
        int i = 0;
        while (i < Math.min(categories.size(), limit)) {
            Category cat = categories.get(i);
            items.add(new QuickLogItem(cat.getName(), cat.getIconRes(), cat.getBackgroundColor(), cat.getIconColor()));
            i++;
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
                    int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
                    binding.cvIconBg.setCardBackgroundColor(bgColor);
                    binding.ivIcon.setColorFilter(iconColor);
                }
        );
        
        quickLogAdapter.setOnItemClickListener(item -> {
            if (Objects.equals(item.getTitle(), othersLabel)) {
                isExpanded = true;
                setupQuickLog();
            } else if (Objects.equals(item.getTitle(), seeLessLabel)) {
                isExpanded = false;
                setupQuickLog();
            } else {
                showQuickLogDialog(item);
            }
        });

        quickLogAdapter.setOnItemLongClickListener(item -> {
            if (!Objects.equals(item.getTitle(), othersLabel) && !Objects.equals(item.getTitle(), seeLessLabel)) {
                showDeleteCategoryDialog(item);
                return true;
            }
            return false;
        });

        quickLogAdapter.submitList(items);
        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
    }

    private void showDeleteCategoryDialog(QuickLogItem item) {
        DialogHelper.showConfirmation(
                requireContext(),
                getString(R.string.add_options_category),
                getString(R.string.btn_delete_account) + " \"" + item.getTitle() + "?\"",
                getString(R.string.btn_delete_account),
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
            showToast(categoryName + " deleted", ToastHelper.Status.SUCCESS);
        }
    }

    private void showQuickLogDialog(QuickLogItem item) {
        QuickLogDialog dialog = QuickLogDialog.newInstance(item);
        dialog.setListener((loggedItem, amount) -> showToast(getString(R.string.msg_logged_success, String.format(Locale.getDefault(), "%.2f", amount), loggedItem.getTitle()), 
                ToastHelper.Status.SUCCESS));
        dialog.show(getChildFragmentManager(), "QUICK_LOG_DIALOG");
    }

    private void setupRecentTransactions() {
        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

}
