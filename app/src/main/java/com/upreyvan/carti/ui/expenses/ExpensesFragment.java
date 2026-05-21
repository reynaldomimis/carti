package com.upreyvan.carti.ui.expenses;

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
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentExpensesBinding;
import com.upreyvan.carti.databinding.ItemLegendExpenseBinding;
import com.upreyvan.carti.model.ExpenseCategory;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensesFragment extends BaseFragment<FragmentExpensesBinding> {

    private GenericAdapter<ExpenseCategory, ItemLegendExpenseBinding> legendAdapter;
    private TransactionAdapter transactionAdapter;
    private Calendar currentCalendar = Calendar.getInstance();
    private TransactionRepository transactionRepository;
    private List<TransactionWithUser> fullExpenseList = new ArrayList<>();
    private String currentSearchQuery = "";

    @Override
    protected FragmentExpensesBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpensesBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().scrollView, 0.3f);
        setupMonthPicker();
        setupSearchBar();
        setupChart();
        setupLegend();
        setupTransactions();
        updateMonthDisplay();
        observeTransactions();
    }

    private void setupSearchBar() {
        getBinding().btnSearchToggle.setOnClickListener(v -> {
            showSearch();
        });

        getBinding().btnClearSearch.setOnClickListener(v -> {
            if (getBinding().etSearch.getText().toString().isEmpty()) {
                hideSearch();
            } else {
                getBinding().etSearch.setText("");
            }
        });

        getBinding().etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showSearch() {
        getBinding().tvHeaderTitle.setVisibility(View.GONE);
        getBinding().btnSearchToggle.setVisibility(View.GONE);
        getBinding().layoutSearchContainer.setVisibility(View.VISIBLE);
        getBinding().etSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(getBinding().etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearch() {
        getBinding().etSearch.setText("");
        getBinding().layoutSearchContainer.setVisibility(View.GONE);
        getBinding().tvHeaderTitle.setVisibility(View.VISIBLE);
        getBinding().btnSearchToggle.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getBinding().etSearch.getWindowToken(), 0);
    }

    private void applyFilters() {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        for (TransactionWithUser t : fullExpenseList) {
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (t.getTransaction().getTitle() != null && t.getTransaction().getTitle().toLowerCase().contains(currentSearchQuery)) ||
                    (t.getTransaction().getCategory() != null && t.getTransaction().getCategory().toLowerCase().contains(currentSearchQuery));

            if (matchesSearch) {
                filteredList.add(t);
            }
        }

        transactionAdapter.submitList(filteredList);
        boolean isEmpty = filteredList.isEmpty();
        getBinding().cardTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void observeTransactions() {
        updateDataForCurrentMonth();
        transactionRepository.syncTransactionsIfNeeded();
    }

    private void updateDataForCurrentMonth() {
        Calendar start = (Calendar) currentCalendar.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        Calendar end = (Calendar) currentCalendar.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);

        transactionRepository.getTransactionsInRange(start.getTimeInMillis(), end.getTimeInMillis())
                .observe(getViewLifecycleOwner(), transactions -> {
                    if (transactions != null) {
                        fullExpenseList.clear();
                        for (TransactionWithUser t : transactions) {
                            if ("EXPENSE".equals(t.getTransaction().getType())) {
                                fullExpenseList.add(t);
                            }
                        }
                        processTransactions(fullExpenseList);
                        applyFilters();
                    }
                });
    }

    private void processTransactions(List<TransactionWithUser> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            getBinding().pieChart.clear();
            legendAdapter.submitList(new ArrayList<>());
            return;
        }

        Map<String, Double> categoryTotals = new HashMap<>();
        for (TransactionWithUser tWithU : transactions) {
            Transaction t = tWithU.getTransaction();
            if ("EXPENSE".equals(t.getType())) {
                String name = ValueHelper.toStr(t.getName());
                categoryTotals.put(name, categoryTotals.getOrDefault(name, 0.0) + t.getAmount());
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<ExpenseCategory> legendCategories = new ArrayList<>();
        
        double total = 0;
        for (double val : categoryTotals.values()) total += val;

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            float percentage = total > 0 ? (float) (entry.getValue() / total * 100) : 0;
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            
            int color = ContextCompat.getColor(requireContext(), R.color.carti_primary_blue);
            colors.add(color);
            legendCategories.add(new ExpenseCategory(entry.getKey(), entry.getValue(), percentage, color));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        getBinding().pieChart.setData(new PieData(dataSet));
        getBinding().pieChart.invalidate();
        legendAdapter.submitList(legendCategories);
    }

    private void setupMonthPicker() {
        getBinding().btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            updateDataForCurrentMonth();
        });

        getBinding().btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            updateDataForCurrentMonth();
        });
    }

    private void updateMonthDisplay() {
        String monthName = currentCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("tl", "PH"));
        int year = currentCalendar.get(Calendar.YEAR);
        getBinding().tvCurrentMonth.setText(String.format(new Locale("tl", "PH"), "%s %d", monthName, year));
    }

    private void setupChart() {
        PieChart pieChart = getBinding().pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);

        pieChart.setDrawCenterText(false);
        pieChart.getLegend().setEnabled(false);
    }

    private void setupLegend() {
        legendAdapter = new GenericAdapter<>(
                ExpenseCategory.DIFF_CALLBACK,
                (inflater, parent) -> ItemLegendExpenseBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.viewColor.setBackgroundTintList(ColorStateList.valueOf(item.getColor()));
                    binding.tvCategory.setText(ValueHelper.toStr(item.getName()));
                    
                    String amountFormatted = Utils.formatCurrency(item.getAmount());
                    String text = String.format(Locale.getDefault(), "%s (%.0f%%)", amountFormatted, item.getPercentage());
                    binding.tvAmountPercent.setText(text);
                }
        );
        getBinding().rvLegend.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvLegend.setAdapter(legendAdapter);
    }

    private void setupTransactions() {
        getBinding().viewHeaderTransactions.tvSectionTitle.setText(R.string.recent_activity);
        getBinding().viewHeaderTransactions.btnSectionAction.setText(R.string.see_all);
        getBinding().viewHeaderTransactions.btnSectionAction.setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance("EXPENSE")));

        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionRepository != null) {
            transactionRepository.onDestroy();
        }
    }
}
