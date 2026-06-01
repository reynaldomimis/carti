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
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.databinding.ActivityBillsBinding;
import com.upreyvan.carti.databinding.ItemCalendarDayBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.CalendarDay;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BillsActivity extends BaseActivity<ActivityBillsBinding> {

    private BillAdapter billAdapter;
    private GenericAdapter<CalendarDay, ItemCalendarDayBinding> calendarAdapter;
    private NotificationRepository notificationRepository;
    private PreferenceManager pref;
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
        notificationRepository = NotificationRepository.getInstance(getApplication());
        pref = new PreferenceManager(this);
        
        setupToolbar();
        setupDynamicPadding();
        setupCalendarRecyclerView();
        setupBillsRecyclerView();
        setupClickListeners();
        
        updateCalendarDisplay();
        observeBills();
    }

    private void observeBills() {
        getBinding().pbBills.setVisibility(View.VISIBLE);
        getBinding().layoutBillsContent.setVisibility(View.GONE);
        getBinding().layoutEmptyBills.setVisibility(View.GONE);

        notificationRepository.getBills(pref.getFamilyId()).observe(this, bills -> {
            getBinding().pbBills.setVisibility(View.GONE);
            if (bills == null || bills.isEmpty()) {
                getBinding().layoutBillsContent.setVisibility(View.GONE);
                getBinding().layoutEmptyBills.setVisibility(View.VISIBLE);
            } else {
                getBinding().layoutBillsContent.setVisibility(View.VISIBLE);
                getBinding().layoutEmptyBills.setVisibility(View.GONE);
                updateBillList(bills);
            }
            loadCalendarDays(bills != null ? bills : new ArrayList<>());
        });
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
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), null, 1f, 0);
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
        AddBillBottomSheet.newInstance(formattedDate).show(getSupportFragmentManager(), "AddBillBottomSheet");
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
        billAdapter.setOnItemClickListener(item -> BillDetailsBottomSheet.newInstance(item.getId(), item.getName()).show(getSupportFragmentManager(), "BillDetailsBottomSheet"));
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
            List<Bill> currentBills = notificationRepository.getBills(pref.getFamilyId()).getValue();
            if (currentBills != null) updateBillList(currentBills);
        });
    }

    private void updateCalendarDisplay() {
        getBinding().tvMonthYear.setText(Utils.formatMonthYear(currentDisplayMonth));
        if (billAdapter.getCurrentList() != null) loadCalendarDays(billAdapter.getCurrentList());
        else loadCalendarDays(new ArrayList<>());
    }

    private void loadCalendarDays(List<Bill> bills) {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < firstDayOfWeek; i++) days.add(new CalendarDay("", false, false, false));

        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = today.get(Calendar.MONTH) == currentDisplayMonth.get(Calendar.MONTH) &&
                today.get(Calendar.YEAR) == currentDisplayMonth.get(Calendar.YEAR);

        for (int i = 1; i <= daysInMonth; i++) {
            boolean isToday = isCurrentMonth && i == today.get(Calendar.DAY_OF_MONTH);
            boolean isSelected = selectedDate.equals(String.valueOf(i));
            boolean hasBill = false;
            String dayStr = String.valueOf(i);
            String daySearchStr = " " + (i < 10 ? "0" + i : i) + ",";
            String daySearchStr2 = " " + i + ",";

            for (Bill bill : bills) {
                if (bill.getDate().contains(daySearchStr) || bill.getDate().contains(daySearchStr2)) {
                    hasBill = true;
                    break;
                }
            }
            days.add(new CalendarDay(dayStr, isSelected, isToday, hasBill));
        }
        calendarAdapter.submitList(days);
    }
}
