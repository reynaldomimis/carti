package com.upreyvan.carti.ui.debt;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.databinding.ActivityAddDebtBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.ui.goals.MemberPickerBottomSheet;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ToastHelper.Status;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddDebtActivity extends BaseActivity<ActivityAddDebtBinding> {
    private TransactionRepository transactionRepository;
    private String selectedMemberId;
    private String selectedMemberName;
    private String selectedCategoryName = "";
    private Calendar selectedDueDate = Calendar.getInstance();

    @Override
    protected ActivityAddDebtBinding inflateBinding(LayoutInflater inflater) { return ActivityAddDebtBinding.inflate(inflater); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(this);
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
            @Override public void onTabSelected(TabLayout.Tab tab) {}
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAmountField() {
        getBinding().etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        List<Category> categories = CategoryManager.getInstance(this).getCategories();
        GenericAdapter<QuickLogItem, ItemQuickLogBinding> adapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (inflater, parent) -> ItemQuickLogBinding.inflate(inflater, parent, false), (binding, item) -> {
            binding.tvLabel.setText(item.getTitle());
            binding.ivIcon.setImageResource(item.getIconRes());
            binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(this, item.getBgColor()));
            binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, item.getIconColor())));
            binding.getRoot().setStrokeWidth(item.isSelected() ? Utils.dpToPx(this, 2) : 0);
            binding.getRoot().setStrokeColor(ContextCompat.getColor(this, R.color.carti_primary_green));
        });
        adapter.setOnItemClickListener(item -> {
            selectedCategoryName = item.getTitle();
            getBinding().tvSelectedCategory.setText(selectedCategoryName);
            List<QuickLogItem> newList = new ArrayList<>();
            for (QuickLogItem i : adapter.getCurrentList()) newList.add(new QuickLogItem(i.getTitle(), i.getIconRes(), i.getBgColor(), i.getIconColor(), i.getTitle().equals(selectedCategoryName)));
            adapter.submitList(newList);
        });
        getBinding().rvCategories.setAdapter(adapter);
        List<QuickLogItem> items = new ArrayList<>();
        for (Category cat : categories) items.add(new QuickLogItem(cat.getName(), cat.getIconRes(), cat.getBackgroundColor(), cat.getIconColor(), cat.getName().equals(selectedCategoryName)));
        adapter.submitList(items);
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 0);
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
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(12).setMinute(0).setTitleText(R.string.label_remind_me).build();
            timePicker.addOnPositiveButtonClickListener(v2 -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d %s", (timePicker.getHour() == 0 || timePicker.getHour() == 12) ? 12 : timePicker.getHour() % 12, timePicker.getMinute(), timePicker.getHour() < 12 ? "AM" : "PM");
                getBinding().tvReminder.setText(time);
            });
            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });
        getBinding().btnDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.label_due_date).setSelection(selectedDueDate.getTimeInMillis()).build();
            datePicker.addOnPositiveButtonClickListener(selection -> { selectedDueDate.setTimeInMillis(selection); updateDateText(); });
            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
        getBinding().btnSave.setOnClickListener(v -> saveDebt());
    }

    private void addAmount(int amount) {
        String current = getBinding().etAmount.getText().toString();
        double val = current.isEmpty() ? 0 : Double.parseDouble(current);
        getBinding().etAmount.setText(String.valueOf(val + amount));
    }

    private void updateDateText() { getBinding().tvDueDate.setText(Utils.formatDateShort(selectedDueDate)); }

    private void saveDebt() {
        if (!checkNetwork()) return;
        if (selectedMemberName == null || Validator.isEmpty(getBinding().etAmount)) {
            showToast(getString(R.string.msg_fill_name_amount), Status.WARNING);
            return;
        }
        double amount = Double.parseDouble(getBinding().etAmount.getText().toString());
        String purpose = getBinding().etPurpose.getText().toString();
        String notes = getBinding().etNotes.getText().toString();
        String dueDateStr = Utils.formatDateQuery(selectedDueDate);
        String reminder = getBinding().tvReminder.getText().toString();
        showLoading(true, getString(R.string.msg_saving_debt));
        Transaction transaction = new Transaction();
        transaction.setType("DEBT");
        transaction.setAmount(amount);
        transaction.setTitle(selectedMemberName);
        transaction.setDescription(purpose + (notes.isEmpty() ? "" : ": " + notes));
        transaction.setCategory(selectedCategoryName);
        transaction.setDueDate(dueDateStr);
        transaction.setReminder(reminder);
        transaction.setMembers(selectedMemberId != null ? List.of(selectedMemberId) : new ArrayList<>());
        PreferenceManager pref = new PreferenceManager(this);
        transaction.setUserId(pref.getUserId());
        transaction.setFamilyId(pref.getFamilyId());
        transactionRepository.addTransaction(transaction, new AppwriteCallback<>() {
            @Override public void onSuccess(Map<String, Object> result) { showLoading(false); finish(); }
            @Override public void onError(Throwable error) { showLoading(false); showToast(getString(R.string.err_generic, error.getMessage()), Status.ERROR); }
        });
    }
}
