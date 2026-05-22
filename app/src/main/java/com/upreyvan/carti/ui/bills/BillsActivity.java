package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.ActivityBillsBinding;
import com.upreyvan.carti.databinding.ItemCalendarDayBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.CalendarDay;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BillsActivity extends BaseActivity<ActivityBillsBinding> {

    private BillAdapter billAdapter;
    private GenericAdapter<CalendarDay, ItemCalendarDayBinding> calendarAdapter;
    private TransactionRepository transactionRepository;
    private RealtimeRepository realtimeRepository;
    private final Calendar currentDisplayMonth = Calendar.getInstance();
    private String selectedDate = "";
    private boolean showingAllBills = false;
    private static final int MAX_BILLS_DISPLAY = 5;

    @Override
    protected ActivityBillsBinding inflateBinding(LayoutInflater inflater) {
        return ActivityBillsBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(getApplication());
        realtimeRepository = RealtimeRepository.getInstance(getApplication());
        
        setupToolbar();
        setupDynamicPadding();
        setupCalendarRecyclerView();
        setupBillsRecyclerView();
        setupClickListeners();
        
        updateCalendarDisplay();
        observeBills();
        observeRealtimeUpdates();
    }

    private void observeRealtimeUpdates() {
        realtimeRepository.getTransactionStream().observe(this, payload -> {
            transactionRepository.refreshTransactions();
        });
    }

    private void observeBills() {
        transactionRepository.getTransactionsByType("EXPENSE").observe(this, transactions -> {
            List<Bill> bills = mapTransactionsToBills(transactions);
            updateBillList(bills);
            loadCalendarDays(bills);
        });
    }

    private List<Bill> mapTransactionsToBills(List<com.upreyvan.carti.model.TransactionWithUser> transactions) {
        List<Bill> bills = new ArrayList<>();
        if (transactions == null) return bills;
        for (com.upreyvan.carti.model.TransactionWithUser twu : transactions) {
            com.upreyvan.carti.model.Transaction t = twu.getTransaction();
            bills.add(new Bill(
                t.getId(),
                t.getFamilyId(),
                t.getTitle(),
                t.getCreatedAt(),
                "₱" + String.format(Locale.getDefault(), "%.2f", t.getAmount()),
                t.isPaid() ? "Paid" : "Unpaid",
                R.drawable.ic_calendar
            ));
        }
        return bills;
    }

    private void updateBillList(List<Bill> bills) {
        if (bills == null) return;
        if (!showingAllBills && bills.size() > MAX_BILLS_DISPLAY) {
            billAdapter.submitList(new ArrayList<>(bills.subList(0, MAX_BILLS_DISPLAY)));
            getBinding().btnViewAllBills.setVisibility(View.VISIBLE);
        } else {
            billAdapter.submitList(new ArrayList<>(bills));
            getBinding().btnViewAllBills.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.bills_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                null,
                1f,
                0
        );
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }

    private void setupCalendarRecyclerView() {
        calendarAdapter = new GenericAdapter<>(
                CalendarDay.DIFF_CALLBACK,
                (inflater, parent) -> ItemCalendarDayBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvDay.setText(item.getDay());
                    
                    if (item.isSelected()) {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_calendar_selected);
                        binding.tvDay.setTextColor(getColor(R.color.white));
                    } else if (item.isToday()) {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_circle_outline);
                        binding.tvDay.setTextColor(getColor(R.color.carti_primary_green));
                    } else {
                        binding.tvDay.setBackground(null);
                        binding.tvDay.setTextColor(getColor(R.color.text_primary));
                    }
                    
                    binding.viewDot.setVisibility(item.hasBill() ? View.VISIBLE : View.GONE);
                }
        );

        calendarAdapter.setOnItemClickListener(item -> {
            if (item.getDay().isEmpty()) return;

            selectedDate = item.getDay();
            updateSelection(item.getDay());
            showAddBillBottomSheet(item.getDay());
        });

        getBinding().rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        getBinding().rvCalendar.setAdapter(calendarAdapter);
    }

    private void showAddBillBottomSheet(String day) {
        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
        String formattedDate = Utils.formatDateFull(cal);
        
        AddBillBottomSheet bottomSheet = AddBillBottomSheet.newInstance(formattedDate);
        bottomSheet.show(getSupportFragmentManager(), "AddBillBottomSheet");
    }

    private void updateSelection(String dayToSelect) {
        List<CalendarDay> currentList = new ArrayList<>(calendarAdapter.getCurrentList());
        for (int i = 0; i < currentList.size(); i++) {
            CalendarDay day = currentList.get(i);
            boolean shouldBeSelected = day.getDay().equals(dayToSelect);
            if (day.isSelected() != shouldBeSelected) {
                currentList.set(i, new CalendarDay(day.getDay(), shouldBeSelected, day.isToday(), day.hasBill()));
            }
        }
        calendarAdapter.submitList(currentList);
    }

    private void setupBillsRecyclerView() {
        billAdapter = new BillAdapter();
        billAdapter.setOnItemClickListener(item -> {
            BillDetailsBottomSheet bottomSheet = BillDetailsBottomSheet.newInstance(
                    item.getId(),
                    item.getName(),
                    item.getAmount()
            );
            bottomSheet.show(getSupportFragmentManager(), "BillDetailsBottomSheet");
        });
        getBinding().rvBills.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvBills.setAdapter(billAdapter);
    }

    private void setupClickListeners() {
        getBinding().btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        getBinding().btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });
        
        getBinding().btnViewAllBills.setOnClickListener(v -> {
            showingAllBills = true;
            // Note: In real app we might want to refresh from repository here if not observing
        });
    }

    private void updateCalendarDisplay() {
        getBinding().tvMonthYear.setText(Utils.formatMonthYear(currentDisplayMonth));
        if (billAdapter.getCurrentList() != null) {
            loadCalendarDays(billAdapter.getCurrentList());
        } else {
            loadCalendarDays(new ArrayList<>());
        }
    }

    private void loadCalendarDays(List<Bill> bills) {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new CalendarDay("", false, false, false));
        }

        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = today.get(Calendar.MONTH) == currentDisplayMonth.get(Calendar.MONTH) &&
                today.get(Calendar.YEAR) == currentDisplayMonth.get(Calendar.YEAR);

        for (int i = 1; i <= daysInMonth; i++) {
            boolean isToday = isCurrentMonth && i == today.get(Calendar.DAY_OF_MONTH);
            boolean isSelected = selectedDate.equals(String.valueOf(i));
            
            boolean hasBill = false;
            String dayStr = String.valueOf(i);
            // Check if any bill is on this day
            for (Bill bill : bills) {
                // Extract day from date string "MMMM dd, yyyy"
                // This is a bit brittle, but works for the current format
                if (bill.getDate().contains(" " + (i < 10 ? "0" + i : i) + ",") || 
                    bill.getDate().contains(" " + i + ",")) {
                    hasBill = true;
                    break;
                }
            }
            
            days.add(new CalendarDay(dayStr, isSelected, isToday, hasBill));
        }

        calendarAdapter.submitList(days);
    }
}
