package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.databinding.ActivityAddBudgetPlanBinding;
import com.upreyvan.carti.databinding.ItemBudgetCategoryBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.StringHelper;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddBudgetPlanActivity extends BaseActivity<ActivityAddBudgetPlanBinding> {

    private GenericAdapter<BudgetCategoryItem, ItemBudgetCategoryBinding> adapter;

    @Override
    protected ActivityAddBudgetPlanBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddBudgetPlanBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupBudgetPeriod();
        setupRecyclerView();
        setupListeners();
        loadExistingPlan();
    }

    private void setupListeners() {
        getBinding().btnAddCategory.setOnClickListener(v -> {
            AddBudgetCategoryBottomSheet bottomSheet = new AddBudgetCategoryBottomSheet();
            bottomSheet.setListener((name, amount) -> {
                List<BudgetCategoryItem> currentList = new ArrayList<>(adapter.getCurrentList());
                currentList.add(new BudgetCategoryItem(name, R.drawable.ic_chart, R.color.mint_green, R.color.mint_green_alpha, amount, 0));
                adapter.submitList(currentList);
            });
            bottomSheet.show(getSupportFragmentManager(), "ADD_CATEGORY_BOTTOM_SHEET");
        });

        getBinding().btnCreateBudgetPlan.setOnClickListener(v -> {
            List<BudgetCategoryItem> items = adapter.getCurrentList();
            if (items.isEmpty()) {
                ToastHelper.show(this, R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
                return;
            }
            BudgetManager.getInstance(this).saveBudgetPlan(items);
            ToastHelper.show(this, R.string.msg_goal_saved_success, ToastHelper.Status.SUCCESS);
            finish();
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnCreateBudgetPlan, 1f, 20);
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_budget_plan_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupBudgetPeriod() {
        getBinding().etBudgetPeriod.setOnClickListener(v -> showDateRangePicker());
        getBinding().tilBudgetPeriod.setEndIconOnClickListener(v -> showDateRangePicker());
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.select_month)
                .setTheme(R.style.CartiDatePicker)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                getBinding().etBudgetPeriod.setText(Utils.formatDateRange(selection.first, selection.second));
            }
        });
        picker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCategoryBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    String spent = StringHelper.formatCompactCurrency(item.getCurrentSpent());
                    String total = StringHelper.formatCompactCurrency(item.getAmount());
                    binding.tvAmount.setText(String.format(Locale.getDefault(), "%s / %s", spent, total));
                    int progress = item.getAmount() > 0 ? (int)((item.getCurrentSpent() / item.getAmount()) * 100) : 0;
                    binding.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", progress));
                    binding.pbBudget.setProgress(progress);
                    binding.ivIcon.setImageResource(item.getIconRes());
                    binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(this, item.getBgColor()));
                    binding.ivIcon.setColorFilter(ContextCompat.getColor(this, item.getIconColor()));
                }
        );
        getBinding().rvCategories.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvCategories.setAdapter(adapter);
    }

    private void loadExistingPlan() {
        List<BudgetCategoryItem> items = BudgetManager.getInstance(this).getBudgetPlan();
        if (items.isEmpty()) {
            items.add(new BudgetCategoryItem("Bills & Utilities", R.drawable.ic_calendar, R.color.icon_water, R.color.log_water, 8000, 0));
            items.add(new BudgetCategoryItem("Grocery", R.drawable.ic_chart, R.color.icon_food, R.color.log_food, 12000, 0));
        }
        adapter.submitList(items);
    }
}
