package com.upreyvan.carti.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.common.AddOptionsAdapter;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddOptionsBinding;
import com.upreyvan.carti.ui.debt.AddDebtActivity;
import com.upreyvan.carti.ui.track.AddCategoryActivity;
import com.upreyvan.carti.ui.track.AddTrackActivity;
import com.upreyvan.carti.ui.family.InviteFamilyActivity;
import com.upreyvan.carti.ui.goals.AddGoalActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.model.AddOption;
import com.upreyvan.carti.ui.notifications.SendNotificationBottomSheetFragment;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AddOptionsActivity extends BaseActivity<ActivityAddOptionsBinding> {

    @Override
    protected ActivityAddOptionsBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddOptionsBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupRecyclerView();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().rvOptions,
                1f,
                20
        );
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_options_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> goBackToHome());
    }

    private void goBackToHome() {
        Intent intent = new Intent(this, com.upreyvan.carti.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("show_home", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goBackToHome();
    }

    private void setupRecyclerView() {
        List<AddOption> options = new ArrayList<>();
        options.add(new AddOption(R.drawable.ic_chart, R.color.icon_food, R.color.log_food, R.string.add_options_expense, R.string.add_options_expense_desc, null));
        options.add(new AddOption(R.drawable.ic_calendar, R.color.icon_debt, R.color.log_debt, R.string.add_options_debt, R.string.add_options_debt_desc, null));
        options.add(new AddOption(R.drawable.ic_add, R.color.mint_green, R.color.tonal_button_bg, R.string.add_options_goal, R.string.add_options_goal_desc, null));
        options.add(new AddOption(R.drawable.ic_person, R.color.icon_fare, R.color.log_fare, R.string.add_options_invite, R.string.add_options_invite_desc, null));
        options.add(new AddOption(R.drawable.ic_calendar, R.color.carti_primary_blue, R.color.log_water, R.string.add_options_bills, R.string.add_options_bills_desc, null));
        options.add(new AddOption(R.drawable.ic_chart, R.color.status_green, R.color.tonal_button_bg, R.string.add_options_budget_plan, R.string.add_options_budget_plan_desc, null));
        options.add(new AddOption(R.drawable.ic_add, R.color.icon_others, R.color.log_others, R.string.add_options_category, R.string.add_options_category_desc, null));
        
        PreferenceManager pref = new PreferenceManager(this);
        if (pref.isAdmin()) {
            options.add(new AddOption(R.drawable.ic_bell, R.color.icon_debt, R.color.log_debt, R.string.add_options_announcement, R.string.add_options_announcement_desc, null));
        }

        AddOptionsAdapter adapter = new AddOptionsAdapter(item -> {
            Intent intent = null;
            if (item.getTitleResId() == R.string.add_options_expense) {
                intent = new Intent(this, AddTrackActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_debt) {
                intent = new Intent(this, AddDebtActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_goal) {
                intent = new Intent(this, AddGoalActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_invite) {
                intent = new Intent(this, InviteFamilyActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_category) {
                intent = new Intent(this, AddCategoryActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_bills) {
                intent = new Intent(this, com.upreyvan.carti.ui.bills.BillsActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_budget_plan) {
                intent = new Intent(this, com.upreyvan.carti.ui.budget.AddBudgetPlanActivity.class);
            } else if (item.getTitleResId() == R.string.add_options_announcement) {
                SendNotificationBottomSheetFragment bottomSheet = new SendNotificationBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(), "SendNotificationBottomSheet");
            }

            if (intent != null) {
                startActivity(intent);
            }
        });
        adapter.submitList(options);
        
        getBinding().rvOptions.setLayoutManager(new GridLayoutManager(this, 2));
        getBinding().rvOptions.setAdapter(adapter);
    }
}
