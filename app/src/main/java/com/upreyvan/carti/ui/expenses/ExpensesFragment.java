package com.upreyvan.carti.ui.expenses;

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
import com.upreyvan.carti.data.local.ExpenseManager;
import com.upreyvan.carti.ui.common.LegendAdapter;
import com.upreyvan.carti.ui.home.TransactionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentExpensesBinding;
import com.upreyvan.carti.model.ExpenseCategory;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends BaseFragment<FragmentExpensesBinding> {

    private LegendAdapter legendAdapter;
    private TransactionAdapter transactionAdapter;
    private Calendar currentCalendar = Calendar.getInstance();

    @Override
    protected FragmentExpensesBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpensesBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupMonthPicker();
        setupChart();
        setupLegend();
        setupTransactions();
        updateMonthDisplay();
        loadData();

        // Listen for global updates
        ExpenseManager.getInstance().setOnExpenseChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::loadData);
            }
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutHeader,
                getBinding().scrollView,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_medium)
        );
    }

    private void loadData() {
        populateMockData(); // Still needed for Chart/Legend for now
        List<Transaction> transactions = ExpenseManager.getInstance().getRecentTransactions(5);
        if (transactionAdapter != null) {
            transactionAdapter.submitList(transactions);
        }
    }

    private void setupMonthPicker() {
        getBinding().btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            populateMockData();
        });

        getBinding().btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            populateMockData();
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
        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);

        pieChart.setDrawCenterText(false);
        pieChart.getLegend().setEnabled(false);
    }

    private void setupLegend() {
        legendAdapter = new LegendAdapter();
        getBinding().rvLegend.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvLegend.setAdapter(legendAdapter);
    }

    private void setupTransactions() {
        getBinding().headerTransactions.tvSectionTitle.setText(R.string.recent_transactions);
        getBinding().headerTransactions.btnSectionAction.setText(R.string.see_all);
        getBinding().headerTransactions.btnSectionAction.setOnClickListener(v -> navigateTo(new AllTransactionsFragment()));

        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void populateMockData() {
        // Legend and Chart Data
        List<ExpenseCategory> categories = new ArrayList<>();
        categories.add(new ExpenseCategory(getString(R.string.label_food), 2250, 36f, ContextCompat.getColor(requireContext(), R.color.icon_food)));
        categories.add(new ExpenseCategory(getString(R.string.label_fare), 1200, 19f, ContextCompat.getColor(requireContext(), R.color.icon_fare)));
        categories.add(new ExpenseCategory(getString(R.string.label_store), 950, 15f, ContextCompat.getColor(requireContext(), R.color.icon_store)));
        categories.add(new ExpenseCategory(getString(R.string.label_load), 800, 13f, ContextCompat.getColor(requireContext(), R.color.icon_load)));
        categories.add(new ExpenseCategory(getString(R.string.label_others), 1050, 17f, ContextCompat.getColor(requireContext(), R.color.icon_others)));

        legendAdapter.submitList(categories);

        // Chart Data
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (ExpenseCategory cat : categories) {
            entries.add(new PieEntry((float) cat.getAmount(), cat.getName()));
            colors.add(cat.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        getBinding().pieChart.setData(new PieData(dataSet));
        getBinding().pieChart.invalidate();

        // Transaction Data
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(getString(R.string.label_food), "Ngayon • 9:35 AM", "₱120", android.R.drawable.ic_menu_gallery, ContextCompat.getColor(requireContext(), R.color.log_food)));
        transactions.add(new Transaction("Pamasahe", "Ngayon • 8:20 AM", "₱15", android.R.drawable.ic_dialog_map, ContextCompat.getColor(requireContext(), R.color.log_fare)));
        transactionAdapter.submitList(transactions);

        // Summary Text
        String comparisonText = getString(R.string.vs_last_month) + " " +
                getString(R.string.percentage_decrease_format, "1,250", "16.7");
        getBinding().tvComparison.setText(comparisonText);
    }
}