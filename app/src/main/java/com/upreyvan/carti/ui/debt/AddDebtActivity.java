package com.upreyvan.carti.ui.debt;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.DebtRepository;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityAddDebtBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.ui.goals.MemberPickerBottomSheet;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddDebtActivity extends BaseActivity<ActivityAddDebtBinding> {

    private DebtRepository debtRepository;
    private String selectedMemberId;
    private String selectedMemberName;
    private String selectedCategoryName = "";
    private String selectedType = "OWE";
    private Calendar selectedDueDate = Calendar.getInstance();

    @Override
    protected ActivityAddDebtBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddDebtBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debtRepository = new DebtRepository(this);
        setupDynamicPadding();
        setupToolbar();
        setupTabs();
        setupAmountField();
        setupCategories();
        setupDefaults();
        setupClickListeners();
    }

    private void setupDefaults() {
        updateDateText();
        getBinding().tvSelectedCategory.setText(R.string.hint_select_category);
    }

    private void setupTabs() {
        getBinding().tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getPosition() == 0 ? "OWE" : "OWED_TO_ME";
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAmountField() {
        getBinding().etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && !s.toString().startsWith("₱")) {
                    // This might be tricky with numeric input type, 
                    // usually better to just show currency in a separate TextView
                }
            }
        });
    }

    private void setupCategories() {
        List<Category> categories = CategoryManager.getInstance(this).getCategories();
        List<QuickLogItem> items = new ArrayList<>();
        for (Category cat : categories) {
            items.add(new QuickLogItem(cat.getName(), cat.getIconRes(), cat.getBackgroundColor(), cat.getIconColor()));
        }

        GenericAdapter<QuickLogItem, ItemQuickLogBinding> adapter = new GenericAdapter<>(
                QuickLogItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemQuickLogBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvLabel.setText(item.getTitle());
                    binding.ivIcon.setImageResource(item.getIconRes());
                    binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(this, item.getBgColor()));
                    binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, item.getIconColor())));
                    
                    boolean isSelected = item.getTitle().equals(selectedCategoryName);
                    binding.getRoot().setStrokeWidth(isSelected ? Utils.dpToPx(this, 2) : 0);
                    binding.getRoot().setStrokeColor(ContextCompat.getColor(this, R.color.carti_primary_green));
                }
        );

        adapter.setOnItemClickListener(item -> {
            selectedCategoryName = item.getTitle();
            getBinding().tvSelectedCategory.setText(selectedCategoryName);
            adapter.notifyDataSetChanged();
        });

        getBinding().rvCategories.setAdapter(adapter);
        adapter.submitList(items);
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSave,
                1f,
                0
        );
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_debt_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnSelectMember.setOnClickListener(v -> {
            MemberPickerBottomSheet bottomSheet = MemberPickerBottomSheet.newInstance(new HashSet<>(), false);
            bottomSheet.setListener(members -> {
                if (!members.isEmpty()) {
                    selectedMemberId = members.get(0).getId();
                    selectedMemberName = members.get(0).getTitle();
                    getBinding().tvSelectedMember.setText(selectedMemberName);
                }
            });
            bottomSheet.show(getSupportFragmentManager(), "MEMBER_PICKER");
        });

        getBinding().btnPlus100.setOnClickListener(v -> addAmount(100));
        getBinding().btnPlus500.setOnClickListener(v -> addAmount(500));
        getBinding().btnPlus1000.setOnClickListener(v -> addAmount(1000));
        getBinding().btnPlus5000.setOnClickListener(v -> addAmount(5000));

        getBinding().btnReminder.setOnClickListener(v -> {
            com.google.android.material.timepicker.MaterialTimePicker timePicker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText(R.string.label_remind_me)
                    .build();

            timePicker.addOnPositiveButtonClickListener(v2 -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d %s",
                        (timePicker.getHour() == 0 || timePicker.getHour() == 12) ? 12 : timePicker.getHour() % 12,
                        timePicker.getMinute(),
                        timePicker.getHour() < 12 ? "AM" : "PM");
                getBinding().tvReminder.setText(time);
            });

            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });

        getBinding().btnDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.label_due_date)
                    .setSelection(selectedDueDate.getTimeInMillis())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDueDate.setTimeInMillis(selection);
                updateDateText();
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> saveDebt());
    }

    private void addAmount(int amount) {
        String current = getBinding().etAmount.getText().toString();
        double val = current.isEmpty() ? 0 : Double.parseDouble(current);
        getBinding().etAmount.setText(String.valueOf(val + amount));
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvDueDate.setText(sdf.format(selectedDueDate.getTime()));
    }

    private void saveDebt() {
        if (!checkNetwork()) return;

        if (selectedMemberName == null || Validator.isEmpty(getBinding().etAmount)) {
            showToast(getString(R.string.msg_fill_name_amount), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
            return;
        }

        double amount = Double.parseDouble(getBinding().etAmount.getText().toString());
        String purpose = getBinding().etPurpose.getText().toString();
        String notes = getBinding().etNotes.getText().toString();
        String dueDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDueDate.getTime());
        String reminder = getBinding().tvReminder.getText().toString();

        showLoading(true, getString(R.string.msg_saving_debt));

        new ApiHelper(this).addDebt(selectedMemberName, amount, selectedType, selectedCategoryName, dueDateStr, reminder, notes, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                String id = String.valueOf(result.get("$id"));
                debtRepository.saveLocally(
                        new com.upreyvan.carti.model.Debt(
                                id, 
                                new PreferenceManager(AddDebtActivity.this).getFamilyId(), 
                                selectedMemberName, 
                                purpose, 
                                Utils.getCurrentTimestamp(), 
                                amount, 
                                false, 
                                R.drawable.ic_person, 
                                notes,
                                selectedType,
                                selectedCategoryName,
                                dueDateStr,
                                reminder
                        )
                );

                showLoading(false);
                showToast(getString(R.string.msg_debt_saved), com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                
                Intent intent = new Intent(AddDebtActivity.this, com.upreyvan.carti.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast(getString(R.string.err_generic, error.getMessage()), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }
}