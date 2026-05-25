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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentTrackBinding;
import com.upreyvan.carti.databinding.ItemBudgetCategoryBinding;
import com.upreyvan.carti.databinding.ItemLegendTrackBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.TrackCategory;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackFragment extends BaseFragment<FragmentTrackBinding> {

    private GenericAdapter<TrackCategory, ItemLegendTrackBinding> legendAdapter;
    private GenericAdapter<BudgetCategoryItem, ItemBudgetCategoryBinding> allocationAdapter;
    private TransactionAdapter transactionAdapter;
    private TransactionRepository transactionRepository;
    private PreferenceManager preferenceManager;
    private List<TransactionWithUser> fullTrackList = new ArrayList<>();
    private final List<BudgetCategoryItem> allAllocations = new ArrayList<>();
    private boolean isAllocationExpanded = false;
    private Calendar currentDisplayDate;

    @Override
    protected FragmentTrackBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentTrackBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        preferenceManager = new PreferenceManager(requireContext());
        currentDisplayDate = Calendar.getInstance();
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().scrollView, 0.3f);
        setupSummaryCards();
        setupChart();
        setupLegend();
        setupBudgetAllocation();
        setupTransactions();
        
        loadCurrentMonthData();
        transactionRepository.syncTransactionsIfNeeded();
    }

    private void loadCurrentMonthData() {
        transactionRepository.getTransactionsByMonth(
                currentDisplayDate.get(Calendar.MONTH),
                currentDisplayDate.get(Calendar.YEAR)
        ).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                updateSummaryData(transactions);
                updateIncomeVsExpense(transactions);
                fullTrackList.clear();
                for (TransactionWithUser t : transactions) {
                    if ("EXPENSE".equals(t.getTransaction().getType())) {
                        fullTrackList.add(t);
                    }
                }
                processTransactions(fullTrackList);
                applyFilters();
            }
        });
    }

    private void setupSummaryCards() {
        // Initial state
        getBinding().layoutSummary.tvTotalBalance.setText(Utils.formatCurrency(0));
        getBinding().layoutSummary.tvSummaryIncome.setText(getString(R.string.format_currency_no_decimal_simple, 0.0));
        getBinding().layoutSummary.tvSummaryExpense.setText(getString(R.string.format_currency_no_decimal_simple, 0.0));
        
        getBinding().layoutSummary.tvSavingsAmount.setText(String.format(Locale.getDefault(), "%s / %s", Utils.formatCurrency(0), Utils.formatCurrency(0)));
        getBinding().layoutSummary.tvSavingsPercent.setText(getString(R.string.zero_percent));
        getBinding().layoutSummary.progressSavings.setProgress(0);

        // Savings Progress remains tied to goals
        transactionRepository.getGoals().observe(getViewLifecycleOwner(), goals -> {
            double savedAmount = 0;
            double targetAmount = 0;
            
            if (goals != null && !goals.isEmpty()) {
                for (TransactionWithUser tWithU : goals) {
                    savedAmount += tWithU.getTransaction().getAmount();
                    targetAmount += tWithU.getTransaction().getTargetAmount();
                }
            }
            
            int progress = targetAmount > 0 ? (int) ((savedAmount / targetAmount) * 100) : 0;
            String progressText = String.format(Locale.getDefault(), "%s / %s", Utils.formatCurrency(savedAmount), Utils.formatCurrency(targetAmount));
            
            getBinding().layoutSummary.tvSavingsAmount.setText(progressText);
            getBinding().layoutSummary.tvSavingsPercent.setText(getString(R.string.percentage_format, progress));
            getBinding().layoutSummary.progressSavings.setProgress(progress);
        });

        getBinding().layoutSummary.btnViewDetails.setOnClickListener(v -> {
            navigateTo(IncomeContributorsFragment.newInstance(
                    currentDisplayDate.get(Calendar.MONTH),
                    currentDisplayDate.get(Calendar.YEAR)
            ));
        });
        getBinding().layoutSummary.cardTotalBalance.setOnClickListener(v -> {
            navigateTo(IncomeContributorsFragment.newInstance(
                    currentDisplayDate.get(Calendar.MONTH),
                    currentDisplayDate.get(Calendar.YEAR)
            ));
        });
    }

    private void updateSummaryData(List<TransactionWithUser> transactions) {
        double income = 0;
        double expense = 0;
        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            if ("INCOME".equals(t.getType())) {
                income += t.getAmount();
            } else if ("EXPENSE".equals(t.getType())) {
                expense += t.getAmount();
            }
        }
        double balance = income - expense;
        getBinding().layoutSummary.tvTotalBalance.setText(Utils.formatCurrency(balance));
        getBinding().layoutSummary.tvSummaryIncome.setText(getString(R.string.format_currency_no_decimal_simple, income));
        getBinding().layoutSummary.tvSummaryExpense.setText(getString(R.string.format_currency_no_decimal_simple, expense));
    }

    private void setupBudgetAllocation() {
        allocationAdapter = new GenericAdapter<>(
                BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCategoryBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.tvAmount.setText(Utils.formatCurrency(item.getAmount()));
                    binding.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", item.getPercentage()));
                    binding.pbBudget.setProgress(item.getPercentage());

                    Category cat = findCategoryByName(item.getCategoryName());
                    if (cat != null) {
                        binding.ivIcon.setImageResource(cat.getIconRes());
                        binding.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), cat.getIconColor()));
                        binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), cat.getBackgroundColor()));
                    } else {
                        binding.ivIcon.setImageResource(item.getIconRes());
                        binding.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), item.getIconColor()));
                        binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), item.getBgColor()));
                    }
                }
        );
        getBinding().rvBudgetAllocation.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvBudgetAllocation.setAdapter(allocationAdapter);

        getBinding().btnViewAllAllocation.setOnClickListener(v -> {
            isAllocationExpanded = !isAllocationExpanded;
            updateAllocationList();
        });
    }

    private Category findCategoryByName(String name) {
        List<Category> categories = CategoryManager.getInstance(requireContext()).getCategories();
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(name)) return cat;
        }
        return null;
    }

    private void updateAllocationList() {
        if (allAllocations.isEmpty()) {
            getBinding().cardBudgetAllocation.setVisibility(View.GONE);
            return;
        }
        getBinding().cardBudgetAllocation.setVisibility(View.VISIBLE);

        List<BudgetCategoryItem> sortedList = new ArrayList<>(allAllocations);
        Collections.sort(sortedList, (a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        List<BudgetCategoryItem> displayList;
        if (isAllocationExpanded) {
            displayList = sortedList.subList(0, Math.min(sortedList.size(), 10));
            getBinding().btnViewAllAllocation.setText(R.string.see_less);
        } else {
            displayList = sortedList.subList(0, Math.min(sortedList.size(), 3));
            getBinding().btnViewAllAllocation.setText(R.string.see_all);
        }
        
        getBinding().btnViewAllAllocation.setVisibility(sortedList.size() > 3 ? View.VISIBLE : View.GONE);
        allocationAdapter.submitList(new ArrayList<>(displayList));
    }

    private void applyFilters() {
        transactionAdapter.submitList(new ArrayList<>(fullTrackList));
        boolean isEmpty = fullTrackList.isEmpty();
        getBinding().rvTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        getBinding().tvNoTransactions.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        getBinding().viewHeaderTransactions.btnSectionAction.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        calculateBudgetProgress(fullTrackList);
    }

    private void calculateBudgetProgress(List<TransactionWithUser> transactions) {
        List<BudgetCategoryItem> budgetPlan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        
        if (budgetPlan.isEmpty()) {
            populateWithActualSpending(transactions);
            return;
        }

        Map<String, Double> spendingMap = new HashMap<>();
        double totalBudget = 0;
        for (BudgetCategoryItem item : budgetPlan) {
            totalBudget += item.getAmount();
        }

        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            String catName = ValueHelper.toStr(t.getCategory());
            double current = spendingMap.get(catName) != null ? spendingMap.get(catName) : 0.0;
            spendingMap.put(catName, current + t.getAmount());
        }

        allAllocations.clear();
        for (BudgetCategoryItem planItem : budgetPlan) {
            double spent = spendingMap.get(planItem.getCategoryName()) != null ? spendingMap.get(planItem.getCategoryName()) : 0.0;
            int progress = planItem.getAmount() > 0 ? (int) ((spent / planItem.getAmount()) * 100) : 0;

            allAllocations.add(new BudgetCategoryItem(
                    planItem.getCategoryName(),
                    planItem.getIconRes(),
                    planItem.getIconColor(),
                    planItem.getBgColor(),
                    planItem.getAmount(),
                    progress
            ));
        }

        getBinding().tvTotalAllocation.setText(getString(R.string.label_total_allocation, Utils.formatCurrency(totalBudget)));
        updateAllocationList();
    }

    private void populateWithActualSpending(List<TransactionWithUser> transactions) {
        Map<String, Double> categoryTotals = new HashMap<>();
        double total = 0;
        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            String catName = ValueHelper.toStr(t.getCategory());
            double current = categoryTotals.get(catName) != null ? categoryTotals.get(catName) : 0.0;
            categoryTotals.put(catName, current + t.getAmount());
            total += t.getAmount();
        }

        allAllocations.clear();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            int progress = total > 0 ? (int) (entry.getValue() / total * 100) : 0;
            allAllocations.add(new BudgetCategoryItem(
                    entry.getKey(),
                    R.drawable.ic_chart,
                    R.color.carti_primary_green,
                    R.color.tonal_button_bg,
                    entry.getValue(),
                    progress
            ));
        }
        getBinding().tvTotalAllocation.setText(getString(R.string.label_total_allocation, Utils.formatCurrency(total)));
        updateAllocationList();
    }

    private void processTransactions(List<TransactionWithUser> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            getBinding().pieChart.clear();
            legendAdapter.submitList(new ArrayList<>());
            allAllocations.clear();
            updateAllocationList();
            updateChartCenterText(0);
            return;
        }

        Map<String, Double> categoryTotals = new HashMap<>();
        double total = 0;
        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            if ("EXPENSE".equals(t.getType())) {
                String name = ValueHelper.toStr(t.getCategory());
                categoryTotals.put(name, categoryTotals.getOrDefault(name, 0.0) + t.getAmount());
                total += t.getAmount();
            }
        }

        updateChartCenterText(total);
        getBinding().tvTotalAllocation.setText(getString(R.string.label_total_allocation, Utils.formatCurrency(total)));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<TrackCategory> legendCategories = new ArrayList<>();
        
        int[] colorRes = {
            R.color.status_red,
            R.color.dash_orange,
            R.color.icon_electricity,
            R.color.icon_water,
            R.color.carti_primary_blue,
            R.color.purple
        };

        int colorIndex = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            float percentage = total > 0 ? (float) (entry.getValue() / total * 100) : 0;
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            
            int color = ContextCompat.getColor(requireContext(), colorRes[colorIndex % colorRes.length]);
            colors.add(color);
            TrackCategory trackCategory = new TrackCategory(entry.getKey(), entry.getValue(), percentage, color);
            legendCategories.add(trackCategory);
            colorIndex++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        getBinding().pieChart.setData(new PieData(dataSet));
        getBinding().pieChart.invalidate();
        legendAdapter.submitList(legendCategories);
    }

    private void updateChartCenterText(double total) {
        String centerText = "Total\n" + Utils.formatCurrency(total);
        getBinding().pieChart.setCenterText(centerText);
        getBinding().pieChart.setCenterTextSize(14f);
        getBinding().pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        getBinding().pieChart.setCenterTextTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
    }

    private void updateIncomeVsExpense(List<TransactionWithUser> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            if ("INCOME".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else if ("EXPENSE".equals(t.getType())) {
                totalExpense += t.getAmount();
            }
        }

        getBinding().tvIncomeAmountComp.setText(Utils.formatCurrency(totalIncome));
        getBinding().tvExpenseAmountComp.setText(Utils.formatCurrency(totalExpense));

        float max = (float) Math.max(totalIncome, totalExpense);
        if (max == 0) max = 1;

        int maxHeightPx = Utils.dpToPx(requireContext(), 120);
        
        ViewGroup.LayoutParams incomeParams = getBinding().barIncome.getLayoutParams();
        incomeParams.height = (int) ((totalIncome / max) * maxHeightPx);
        getBinding().barIncome.setLayoutParams(incomeParams);

        ViewGroup.LayoutParams expenseParams = getBinding().barExpense.getLayoutParams();
        expenseParams.height = (int) ((totalExpense / max) * maxHeightPx);
        getBinding().barExpense.setLayoutParams(expenseParams);
    }

    private void setupChart() {
        PieChart pieChart = getBinding().pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(85f);

        pieChart.setDrawCenterText(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
    }

    private void setupLegend() {
        legendAdapter = new GenericAdapter<>(
                TrackCategory.DIFF_CALLBACK,
                (inflater, parent) -> ItemLegendTrackBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.viewColor.setBackgroundTintList(ColorStateList.valueOf(item.getColor()));
                    binding.tvCategory.setText(ValueHelper.toStr(item.getName()));
                    binding.tvPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", item.getPercentage()));
                }
        );
        getBinding().rvLegend.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvLegend.setAdapter(legendAdapter);
    }

    private void setupTransactions() {
        getBinding().viewHeaderTransactions.tvSectionTitle.setText(R.string.recent_transactions);
        getBinding().viewHeaderTransactions.btnSectionAction.setText(R.string.see_all);
        getBinding().viewHeaderTransactions.btnSectionAction.setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance("EXPENSE")));

        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
