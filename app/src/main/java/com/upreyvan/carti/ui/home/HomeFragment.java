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
import com.upreyvan.carti.data.ai.GeminiManager;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemAiSuggestionCardBinding;
import com.upreyvan.carti.databinding.ItemQuickActionBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.bills.BillDetailsBottomSheet;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.ui.budget.AddBudgetPlanActivity;
import com.upreyvan.carti.util.ToastHelper;
import androidx.core.graphics.ColorUtils;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private GenericAdapter<QuickLogItem, ItemQuickLogBinding> quickLogAdapter;
    private GenericAdapter<QuickLogItem, ItemQuickActionBinding> quickActionsAdapter;
    private GenericAdapter<AiSuggestion, ItemAiSuggestionCardBinding> aiSuggestionsAdapter;
    private TransactionAdapter transactionAdapter;
    private boolean isExpanded = false;
    private RealtimeRepository realtimeRepo;
    private TransactionRepository transactionRepository;
    private MemberRepository memberRepository;
    private AiRepository aiRepository;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        memberRepository = new MemberRepository(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().home, 0.3f);
        initAdapters();
        setupHeaders();
        setupQuickActions();
        setupQuickLog();
        setupRecentTransactions();
        setupNotifications();
        
        observeTransactions();
        observeRealtimeData();
        setupDashboard();
        setupPaydayCard();
        setupBudgetPlanPrompt();
        setupAiInsightCard();
        setupAiSuggestions();
        fetchFamilyData();
        loadHomeAiInsights();

        BudgetManager.getInstance(requireContext()).addListener(items -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::updateQuickLogData);
            }
        });
    }


    private void observeRealtimeData() {
        realtimeRepo.getTransactionStream().observe(getViewLifecycleOwner(), payload -> {
            if (transactionRepository != null) {
                transactionRepository.refreshTransactions();
            }
        });

        // 2. User/Member Updates
        realtimeRepo.getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
            checkNotifications(new ApiHelper(requireContext()), new PreferenceManager(requireContext()), null);
            if (memberRepository != null) {
                memberRepository.refreshMembers();
            }
        });

        // 3. Family Balance/Summary Updates
        realtimeRepo.getFamilyStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                double balance = Utils.getDouble(payload.get("balance"));
                double income = Utils.getDouble(payload.get("totalIncome"));
                double expense = Utils.getDouble(payload.get("totalExpense"));
                
                PreferenceManager pref = new PreferenceManager(requireContext());
                pref.saveFamilySummary(balance, income, expense);
                
                requireActivity().runOnUiThread(() -> {
                    setupHeaders();
                    setupDashboard();
                });
            }
        });

        // 4. Realtime Notification Badge
        realtimeRepo.getNotificationStream().observe(getViewLifecycleOwner(), payload -> {
            checkNotifications(new ApiHelper(requireContext()), new PreferenceManager(requireContext()), null);
            checkBillNotifications();
        });

        // 5. Realtime Likes (Emoji Updates)
        realtimeRepo.getLikeStream().observe(getViewLifecycleOwner(), payload -> {
            if (transactionRepository != null) {
                transactionRepository.refreshTransactions();
            }
        });

        checkBillNotifications();
    }

    private void checkBillNotifications() {
        new ApiHelper(requireContext()).getNotifications(new PreferenceManager(requireContext()).getFamilyId(), new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;
                
                Document<Map<String, Object>> latestBill = null;
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    String title = (String) doc.getData().get("title");
                    if (title != null && title.startsWith("BILL: ")) {
                        latestBill = doc;
                        break;
                    }
                }

                if (latestBill != null) {
                    Document<Map<String, Object>> finalBill = latestBill;
                    requireActivity().runOnUiThread(() -> {
                        getBinding().viewBillNotification.cardNotification.setVisibility(View.VISIBLE);
                        getBinding().viewBillNotification.tvTitle.setText((String) finalBill.getData().get("title"));
                        getBinding().viewBillNotification.tvContent.setText((String) finalBill.getData().get("content"));
                        getBinding().viewBillNotification.btnClose.setOnClickListener(v -> 
                            getBinding().viewBillNotification.cardNotification.setVisibility(View.GONE)
                        );
                        getBinding().viewBillNotification.getRoot().setOnClickListener(v -> {
                            String title = (String) finalBill.getData().get("title");
                            String billName = title.substring(6);
                            BillDetailsBottomSheet bottomSheet = BillDetailsBottomSheet.newInstance(finalBill.getId(), billName);
                            bottomSheet.show(getChildFragmentManager(), "BillDetailsBottomSheet");
                        });
                    });
                } else {
                    requireActivity().runOnUiThread(() -> 
                        getBinding().viewBillNotification.cardNotification.setVisibility(View.GONE)
                    );
                }
            }

            @Override
            public void onError(Throwable error) {}
        });
    }

    private void updateQuickLogData() {
        List<com.upreyvan.carti.model.BudgetCategoryItem> budgetPlan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        List<QuickLogItem> items = new ArrayList<>();

        for (com.upreyvan.carti.model.BudgetCategoryItem item : budgetPlan) {
            items.add(new QuickLogItem(item.getCategoryName(), item.getIconRes(), item.getBgColor(), item.getIconColor()));
        }

        boolean isEmpty = items.isEmpty();

        getBinding().viewHeaderQuickLog.getRoot().setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        getBinding().rvQuickLog.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        quickLogAdapter.submitList(items);
    }

    private void initAdapters() {
        if (quickLogAdapter != null) return;

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
            showQuickLogDialog(item);
        });
        quickLogAdapter.setOnItemLongClickListener(item -> {
            // Long click can be used for secondary actions or customization shortcut
            startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class));
            return true;
        });

        quickActionsAdapter = new GenericAdapter<>(
                QuickLogItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemQuickActionBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvLabel.setText(item.getTitle());
                    binding.ivIcon.setImageResource(item.getIconRes());
                    int iconColor = ContextCompat.getColor(requireContext(), item.getIconColor());
                    int bgColor = ContextCompat.getColor(requireContext(), item.getBgColor());
                    binding.cvIconBg.setCardBackgroundColor(bgColor);
                    binding.ivIcon.setColorFilter(iconColor);
                }
        );
        quickActionsAdapter.setOnItemClickListener(item -> {
            if (item.getTitle().equals(getString(R.string.add_options_expense))) {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(7);
            } else if (item.getTitle().equals(getString(R.string.action_add_income))) {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(2); 
            } else if (item.getTitle().equals(getString(R.string.action_family_chat))) {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(5);
            } else if (item.getTitle().equals(getString(R.string.action_manage_goals))) {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(6);
            }
        });

        transactionAdapter = new TransactionAdapter();
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

        getBinding().viewAiSuggestions.rvAiSuggestions.setAdapter(aiSuggestionsAdapter);
        getBinding().viewAiSuggestions.btnCloseContainer.setOnClickListener(v -> 
            getBinding().viewAiSuggestions.getRoot().setVisibility(View.GONE)
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
        getBinding().viewAiInsight.btnAskAi.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(6);
            }
        });

        getBinding().viewAiInsight.btnViewReport.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(2);
            }
        });
    }

    private void loadHomeAiInsights() {
        if (aiRepository == null) {
            aiRepository = new AiRepository(requireContext());
        }

        PreferenceManager pref = new PreferenceManager(requireContext());
        String cachedInsight = pref.getDailyAiInsightText();
        String cachedDate = pref.getDailyAiInsightDate();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());

        if (today.equals(cachedDate) && !cachedInsight.isEmpty()) {
            getBinding().viewAiInsight.tvAiMessage.setText(cachedInsight);
            getBinding().viewAiInsight.tvAiMessage.setAlpha(1.0f);
            return;
        }

        // Only show loading if we don't have a cached one from today
        getBinding().viewAiInsight.tvAiMessage.setText(R.string.ai_insight_loading);
        getBinding().viewAiInsight.tvAiMessage.setAlpha(0.6f);

        aiRepository.getDailyInsights(new GeminiManager.AiCallback() {
            @Override
            public void onSuccess(String response) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        getBinding().viewAiInsight.tvAiMessage.setText(response);
                        getBinding().viewAiInsight.tvAiMessage.setAlpha(1.0f);
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (getBinding().viewAiInsight.tvAiMessage.getText().toString().equals(getString(R.string.ai_insight_loading))) {
                            getBinding().viewAiInsight.tvAiMessage.setText(R.string.ai_insight_error);
                        }
                    });
                }
            }
        });
    }

    private void observeTransactions() {
        transactionRepository.getRecentTransactions(5).observe(getViewLifecycleOwner(), transactions -> {
            if (transactionAdapter != null) {
                transactionAdapter.setLoading(false);
                transactionAdapter.submitList(transactions);
                
                boolean isEmpty = transactions == null || transactions.isEmpty();
                getBinding().viewHeaderRecent.getRoot().setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().cardRecentTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().rvTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().tvNoTransactions.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                getBinding().viewHeaderRecent.btnSectionAction.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        transactionRepository.syncTransactionsIfNeeded();
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
                
                String adminId = String.valueOf(data.get("adminId"));
                pref.setAdminId(adminId);
                checkNotifications(apiHelper, pref, adminId);

                requireActivity().runOnUiThread(() -> {
                    setupHeaders();
                    setupDashboard();
                });
            }

            @Override
            public void onError(Throwable error) {
                checkNotifications(apiHelper, pref, null);
            }
        });
    }

    private void checkNotifications(ApiHelper apiHelper, PreferenceManager pref, String adminId) {
        boolean isAdmin = false;
        String currentUserId = pref.getUserId();

        if (adminId != null && !adminId.isEmpty() && !"null".equals(adminId)) {
            isAdmin = currentUserId.equals(adminId);
        } else {
            String role = pref.getUserRole();
            for (String r : Constants.Roles.PARENTS) {
                if (r.equalsIgnoreCase(role)) {
                    isAdmin = true;
                    break;
                }
            }
        }

        if (isAdmin) {
            apiHelper.getMembers(new AppwriteManager.AppwriteCallback<>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    if (!isAdded()) return;
                    int pendingCount = 0;
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Object status = doc.getData().get("status");
                        if (Objects.equals("pending", status)) {
                            pendingCount++;
                        }
                    }
                    
                    final int finalCount = pendingCount;
                    requireActivity().runOnUiThread(() -> {
                        updateNotificationBadge(finalCount > 0);
                        if (finalCount > 0) {
                            showFamilyNotificationCard(finalCount);
                        } else {
                            getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    requireActivity().runOnUiThread(() -> updateNotificationBadge(pref.hasNotifications()));
                }
            });
        } else {
            updateNotificationBadge(false);
            requireActivity().runOnUiThread(() -> getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE));
        }
    }

    private void showFamilyNotificationCard(int count) {
        getBinding().viewFamilyNotification.cardNotification.setVisibility(View.VISIBLE);
        getBinding().viewFamilyNotification.tvTitle.setText(getString(R.string.pending_requests_title, count));
        getBinding().viewFamilyNotification.tvContent.setText(R.string.pending_requests_desc);
        getBinding().viewFamilyNotification.ivIcon.setImageResource(R.drawable.ic_person);
        getBinding().viewFamilyNotification.getRoot().setOnClickListener(v -> {
             if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(4); // Profile/Members
        });
        getBinding().viewFamilyNotification.btnClose.setOnClickListener(v -> 
            getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE)
        );
    }

    private void setupDashboard() {
        Calendar current = Calendar.getInstance();
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, -1);

        long currentStart = Utils.getMonthStartMillis(current);
        long currentEnd = Utils.getMonthEndMillis(current);
        long lastStart = Utils.getMonthStartMillis(lastMonth);
        long lastEnd = Utils.getMonthEndMillis(lastMonth);

        // Current Month Data
        transactionRepository.getTotalIncomeInRange(currentStart, currentEnd).observe(getViewLifecycleOwner(), income -> {
            double currentIncome = income != null ? income : 0.0;
            getBinding().viewHomeDashboard.tvIncomeAmount.setText(getString(R.string.format_currency_no_decimal, currentIncome));
            
            transactionRepository.getTotalIncomeInRange(lastStart, lastEnd).observe(getViewLifecycleOwner(), lastIncome -> {
                updateTrend(getBinding().viewHomeDashboard.tvIncomeTrend, currentIncome, lastIncome != null ? lastIncome : 0.0);
            });
        });

        transactionRepository.getTotalExpenseInRange(currentStart, currentEnd).observe(getViewLifecycleOwner(), expense -> {
            double currentExpense = expense != null ? expense : 0.0;
            getBinding().viewHomeDashboard.tvExpensesAmount.setText(getString(R.string.format_currency_no_decimal, currentExpense));
            
            transactionRepository.getTotalExpenseInRange(lastStart, lastEnd).observe(getViewLifecycleOwner(), lastExpense -> {
                updateTrend(getBinding().viewHomeDashboard.tvExpensesTrend, currentExpense, lastExpense != null ? lastExpense : 0.0);
            });
        });

        transactionRepository.getTotalIncomeInRange(currentStart, currentEnd).observe(getViewLifecycleOwner(), income -> {
            transactionRepository.getTotalExpenseInRange(currentStart, currentEnd).observe(getViewLifecycleOwner(), expense -> {
                double currentIncome = income != null ? income : 0.0;
                double currentExpense = expense != null ? expense : 0.0;
                double savings = currentIncome - currentExpense;
                getBinding().viewHomeDashboard.tvTotalSavings.setText(getString(R.string.format_currency, savings));

                transactionRepository.getTotalIncomeInRange(lastStart, lastEnd).observe(getViewLifecycleOwner(), lastIncome -> {
                    transactionRepository.getTotalExpenseInRange(lastStart, lastEnd).observe(getViewLifecycleOwner(), lastExpense -> {
                        double prevSavings = (lastIncome != null ? lastIncome : 0.0) - (lastExpense != null ? lastExpense : 0.0);
                        updateTrend(getBinding().viewHomeDashboard.tvSavingsTrend, savings, prevSavings);
                    });
                });
            });
        });

        getBinding().viewHomeDashboard.tvOverviewDate.setText(Utils.formatMonthYear(current));
    }

    private void updateTrend(android.widget.TextView textView, double current, double previous) {
        if (previous == 0) {
            textView.setText("0%");
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            return;
        }
        double percentage = ((current - previous) / previous) * 100;
        String sign = percentage >= 0 ? "+" : "";
        textView.setText(String.format(Locale.getDefault(), "%s%.1f%%", sign, percentage));
        
        int color = percentage >= 0 ? R.color.status_green : R.color.status_red;
        // Logic inversion for expenses: +% is bad (red), -% is good (green)
        if (textView.getId() == getBinding().viewHomeDashboard.tvExpensesTrend.getId()) {
            color = percentage <= 0 ? R.color.status_green : R.color.status_red;
        }
        
        textView.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void setupPaydayCard() {
        SalaryManager salaryManager = SalaryManager.getInstance(requireContext());
        PreferenceManager pref = new PreferenceManager(requireContext());

        int daysLeft = salaryManager.getDaysUntilNextPayday();
         Calendar nextPayday = salaryManager.getNextPayday();

        String name = pref.getUsername();
        getBinding().viewHomeDashboard.tvDaysRemaining.setText(getString(R.string.days_to_go, daysLeft));
        getBinding().viewHomeDashboard.tvPaydayFor.setText(getString(R.string.next_payday_for, name));

        getBinding().viewHomeDashboard.tvPaydayDate.setText(Utils.formatDateFull(nextPayday));
    }

    private void setupBudgetPlanPrompt() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String currentMonth = Utils.formatMonthQuery(Calendar.getInstance());
        String dismissedMonth = pref.getBudgetPlanDismissedMonth();

        if (currentMonth.equals(dismissedMonth)) {
            getBinding().viewHomeDashboard.cardBudgetPlan.setVisibility(View.GONE);
            return;
        }

        getBinding().viewHomeDashboard.cardBudgetPlan.setVisibility(View.VISIBLE);
        
        getBinding().viewHomeDashboard.btnCloseBudgetPlan.setOnClickListener(v -> {
            getBinding().viewHomeDashboard.cardBudgetPlan.setVisibility(View.GONE);
            pref.setBudgetPlanDismissedMonth(currentMonth);
        });

        getBinding().viewHomeDashboard.btnSetNow.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddBudgetPlanActivity.class));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupQuickLog();
        setupDashboard();
        setupPaydayCard();
        setupBudgetPlanPrompt();
        updateNotificationBadge(new PreferenceManager(requireContext()).hasNotifications());
    }

    private void updateNotificationBadge(boolean hasNotifications) {
        if (hasNotifications) {
            getBinding().notifBadge.setVisibility(View.VISIBLE);
            getBinding().notifBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.mint_green));
        } else {
            getBinding().notifBadge.setVisibility(View.GONE);
        }
    }

    private void setupNotifications() {
        getBinding().btnNotif.setOnClickListener(v -> navigateTo(new NotificationsFragment()));
        getBinding().btnAiTest.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.upreyvan.carti.data.ai.AiTestActivity.class));
        });
    }

    private void setupHeaders() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String name = pref.getUsername();
        String greeting = Utils.getGreeting();
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, greeting));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, name));

        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        double net = income - expense;
        getBinding().tvGreetingSub.setText(getString(R.string.family_label, String.format(Locale.getDefault(), "₱%,.0f", net)));

        getBinding().viewHeaderQuickActions.tvSectionTitle.setText(R.string.quick_actions_title);
        getBinding().viewHeaderQuickActions.btnSectionAction.setVisibility(View.GONE);

        getBinding().viewHeaderQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().viewHeaderQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().viewHeaderQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        
        getBinding().viewHeaderQuickLog.getRoot().setOnClickListener(v -> {
            QuickLogsBottomSheetFragment fragment =  QuickLogsBottomSheetFragment.newInstance(null);
            fragment.show(getChildFragmentManager(), "QUICK_LOG_BOTTOM_SHEET");
        });

        getBinding().viewHeaderQuickLog.btnSectionAction.setText(R.string.customize);
        getBinding().viewHeaderQuickLog.btnSectionAction.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)));

        getBinding().viewHeaderRecent.tvSectionTitle.setText(R.string.recent_activity);
        getBinding().viewHeaderRecent.btnSectionAction.setText(R.string.see_all);
        getBinding().viewHeaderRecent.btnSectionAction.setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance(null)));
    }

    private void setupQuickLog() {
        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
        updateQuickLogData();
    }

    private void setupQuickActions() {
        List<QuickLogItem> actions = new ArrayList<>();
        actions.add(new QuickLogItem(getString(R.string.add_options_expense), R.drawable.ic_add, R.color.status_red_tonal, R.color.status_red));
        actions.add(new QuickLogItem(getString(R.string.action_add_income), R.drawable.ic_arrow_up, R.color.dash_green_alpha, R.color.dash_green));
        actions.add(new QuickLogItem(getString(R.string.action_family_chat), R.drawable.ic_sync, R.color.log_fare, R.color.carti_primary_blue));
        actions.add(new QuickLogItem(getString(R.string.action_manage_goals), R.drawable.ic_trophy, R.color.mint_green_alpha, R.color.mint_green));
        getBinding().rvQuickActions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvQuickActions.setAdapter(quickActionsAdapter);
        quickActionsAdapter.submitList(actions);
    }

    private void showQuickLogDialog(QuickLogItem item) {
        com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment fragment = 
                com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment.newInstance(item.getTitle());
        fragment.show(getChildFragmentManager(), "QUICK_LOG_BOTTOM_SHEET");
    }

    private void setupRecentTransactions() {
        if (transactionAdapter == null) {
            transactionAdapter = new TransactionAdapter();
        }
        transactionAdapter.setOnTransactionInteractionListener(new TransactionAdapter.OnTransactionInteractionListener() {
            @Override
            public void onLikeClick(com.upreyvan.carti.model.Transaction transaction) {
                transactionRepository.likeTransaction(transaction.getId(), "👍", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        // Success handled by repo sync
                    }

                    @Override
                    public void onError(Throwable error) {
                        ToastHelper.show(requireContext(), "Failed to like", ToastHelper.Status.ERROR);
                    }
                });
            }

            @Override
            public void onReactionClick(com.upreyvan.carti.model.Transaction transaction, String emoji) {
                transactionRepository.likeTransaction(transaction.getId(), emoji, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                    }

                    @Override
                    public void onError(Throwable error) {
                        ToastHelper.show(requireContext(), "Failed to react", ToastHelper.Status.ERROR);
                    }
                });
            }

            @Override
            public void onCommentClick(com.upreyvan.carti.model.Transaction transaction) {
                CommentsBottomSheetFragment fragment = CommentsBottomSheetFragment.newInstance(transaction.getId());
                fragment.show(getChildFragmentManager(), "CommentsBottomSheet");
            }

            @Override
            public void onViewLikesClick(com.upreyvan.carti.model.Transaction transaction, String reactorNames) {
                ReactionsBottomSheetFragment fragment = ReactionsBottomSheetFragment.newInstance(transaction.getId());
                fragment.show(getChildFragmentManager(), "ReactionsBottomSheet");
            }
        });
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

}
