package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.BillRepository;
import com.upreyvan.carti.databinding.ActivityBillsBinding;
import com.upreyvan.carti.databinding.ItemCalendarDayBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.CalendarDay;
import com.upreyvan.carti.util.Utils;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BillsActivity extends BaseActivity<ActivityBillsBinding> {

    private BillAdapter billAdapter;
    private GenericAdapter<CalendarDay, ItemCalendarDayBinding> calendarAdapter;
    private BillRepository billRepository;
    private Calendar currentDisplayMonth = Calendar.getInstance();
    private String selectedDate = "";

    @Override
    protected ActivityBillsBinding inflateBinding(LayoutInflater inflater) {
        return ActivityBillsBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billRepository = new BillRepository(this);
        setupToolbar();
        setupStatusBar();
        setupCalendarRecyclerView();
        setupBillsRecyclerView();
        setupClickListeners();
        updateCalendarDisplay();
        observeBills();
    }

    private void observeBills() {
        billRepository.getAllBills().observe(this, bills -> {
            billAdapter.submitList(bills);
        });
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.bills_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.label_add);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            startActivity(new Intent(this, AddBillActivity.class));
        });
    }

    private void setupStatusBar() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().scrollView,
                1f,
                20
        );
    }

    private void setupCalendarRecyclerView() {
        calendarAdapter = new GenericAdapter<>(
                CalendarDay.DIFF_CALLBACK,
                (inflater, parent) -> ItemCalendarDayBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvDay.setText(item.getDay());
                    
                    // Styling base on state
                    if (item.isSelected()) {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_calendar_selected);
                        binding.tvDay.setTextColor(getColor(R.color.white));
                    } else if (item.isToday()) {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_circle);
                        binding.tvDay.setTextColor(getColor(R.color.carti_primary_green));
                    } else {
                        binding.tvDay.setBackground(null);
                        binding.tvDay.setTextColor(getColor(R.color.text_primary));
                    }
                    
                    binding.viewDot.setVisibility(item.hasBill() ? View.VISIBLE : View.GONE);

                    binding.getRoot().setOnClickListener(v -> {
                        if (item.getDay().isEmpty()) return;
                        
                        selectedDate = item.getDay();
                        updateSelection(item.getDay());
                        filterBillsForDate(item.getDay());
                    });
                }
        );

        getBinding().rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        getBinding().rvCalendar.setAdapter(calendarAdapter);
    }

    private void updateSelection(String dayToSelect) {
        List<CalendarDay> currentList = new ArrayList<>(calendarAdapter.getCurrentList());
        for (int i = 0; i < currentList.size(); i++) {
            CalendarDay day = currentList.get(i);
            boolean shouldBeSelected = day.getDay().equals(dayToSelect);
            currentList.set(i, new CalendarDay(day.getDay(), shouldBeSelected, day.isToday(), day.hasBill()));
        }
        calendarAdapter.submitList(currentList);
    }

    private void setupBillsRecyclerView() {
        billAdapter = new BillAdapter();
        getBinding().rvBills.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvBills.setAdapter(billAdapter);
    }

    private void setupClickListeners() {
        getBinding().btnSelectMonth.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_month)
                    .setSelection(currentDisplayMonth.getTimeInMillis())
                    .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                currentDisplayMonth.setTimeInMillis(selection);
                updateCalendarDisplay();
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        // Prev Month Arrow
        getBinding().btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        // Next Month Arrow
        getBinding().btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });

        // Prev Year Arrow
        getBinding().btnPrevYear.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.YEAR, -1);
            updateCalendarDisplay();
        });

        // Next Year Arrow
        getBinding().btnNextYear.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.YEAR, 1);
            updateCalendarDisplay();
        });

        getBinding().fabAddBill.setOnClickListener(v -> {
            startActivity(new Intent(this, AddBillActivity.class));
        });
    }

    private void updateCalendarDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        getBinding().tvMonthYear.setText(sdf.format(currentDisplayMonth.getTime()));
        loadCalendarDays();
    }

    private void loadCalendarDays() {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; 
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new CalendarDay("", false, false, false));
        }

        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = today.get(Calendar.MONTH) == currentDisplayMonth.get(Calendar.MONTH) &&
                today.get(Calendar.YEAR) == currentDisplayMonth.get(Calendar.YEAR);

        for (int i = 1; i <= maxDay; i++) {
            boolean isToday = isCurrentMonth && (i == today.get(Calendar.DAY_OF_MONTH));
            boolean hasBill = (i == 12 || i == 15 || i == 20 || i == 28); // Dummy markers
            days.add(new CalendarDay(String.valueOf(i), false, isToday, hasBill));
        }
        
        calendarAdapter.submitList(days);
        filterBillsForDate("");
    }

    private void filterBillsForDate(String day) {
        List<Bill> bills = new ArrayList<>();
        String monthName = new SimpleDateFormat("MMM", Locale.getDefault()).format(currentDisplayMonth.getTime());
        String displayDay = day.isEmpty() ? "Select a date" : monthName + " " + day + ", " + currentDisplayMonth.get(Calendar.YEAR);
        
        if (day.equals("12") || day.equals("15") || day.equals("20") || day.equals("28") || day.isEmpty()) {
            bills.add(new Bill("1", "", "Maynilad Water Bill", displayDay, "₱1,200", "Unpaid", R.drawable.ic_calendar));
            bills.add(new Bill("2", "", "PLDT Internet", displayDay, "₱1,699", "Unpaid", R.drawable.ic_calendar));
        }

        billAdapter.submitList(bills);
    }
}
