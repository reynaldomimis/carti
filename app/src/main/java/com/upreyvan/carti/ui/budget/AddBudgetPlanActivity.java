package com.upreyvan.carti.ui.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ActivityAddBudgetPlanBinding;
import com.upreyvan.carti.databinding.ItemBudgetCategoryBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.Utils;
import androidx.core.util.Pair;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        loadDummyData();
    }

    private void setupListeners() {
        getBinding().btnAddCategory.setOnClickListener(v -> {
            AddBudgetCategoryBottomSheet bottomSheet = new AddBudgetCategoryBottomSheet();
            bottomSheet.setListener((name, amount) -> {
                List<BudgetCategoryItem> currentList = new ArrayList<>(adapter.getCurrentList());
                // Set default progress to 10% for visualization
                currentList.add(new BudgetCategoryItem(name, R.drawable.ic_chart, R.color.mint_green, R.color.mint_green_alpha, amount, 10));
                adapter.submitList(currentList);
            });
            bottomSheet.show(getSupportFragmentManager(), "ADD_CATEGORY_BOTTOM_SHEET");
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnCreateBudgetPlan,
                1f,
                20
        );
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
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.select_month)
                        .setTheme(R.style.CartiDatePicker)
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                String startDate = sdf.format(new Date(selection.first));
                String endDate = sdf.format(new Date(selection.second));
                String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date(selection.second));
                getBinding().etBudgetPeriod.setText(String.format("%s - %s, %s", startDate, endDate, year));
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
                    binding.tvAmount.setText(String.format(Locale.getDefault(), "PHP %,.0f", item.getAmount()));
                    binding.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", item.getPercentage()));
                    binding.pbBudget.setProgress(item.getPercentage());
                    binding.ivIcon.setImageResource(item.getIconRes());
                    binding.cvIcon.setCardBackgroundColor(ContextCompat.getColor(this, item.getBgColor()));
                    binding.ivIcon.setColorFilter(ContextCompat.getColor(this, item.getIconColor()));
                }
        );

        getBinding().rvCategories.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvCategories.setAdapter(adapter);
    }

    private void loadDummyData() {
        List<BudgetCategoryItem> items = new ArrayList<>();
        items.add(new BudgetCategoryItem("Bills & Utilities", R.drawable.ic_calendar, R.color.icon_water, R.color.log_water, 8000, 80));
        items.add(new BudgetCategoryItem("Grocery", R.drawable.ic_chart, R.color.icon_food, R.color.log_food, 12000, 60));
        items.add(new BudgetCategoryItem("Transportation", R.drawable.ic_chart, R.color.icon_fare, R.color.log_fare, 4000, 60));
        items.add(new BudgetCategoryItem("Education", R.drawable.ic_chart, R.color.icon_load, R.color.log_load, 6000, 50));
        items.add(new BudgetCategoryItem("Emergency Fund", R.drawable.ic_chart, R.color.icon_others, R.color.log_others, 5000, 40));
        adapter.submitList(items);
    }
}
