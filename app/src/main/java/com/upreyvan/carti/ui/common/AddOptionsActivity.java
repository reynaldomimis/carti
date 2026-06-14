package com.upreyvan.carti.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ActivityAddOptionsBinding;
import com.upreyvan.carti.databinding.ItemAddOptionBinding;
import com.upreyvan.carti.models.AddOption;
import com.upreyvan.carti.ui.bills.BillsActivity;
import com.upreyvan.carti.ui.debt.AddDebtActivity;
import com.upreyvan.carti.ui.family.InviteFamilyActivity;
import com.upreyvan.carti.ui.goals.AddGoalActivity;
import com.upreyvan.carti.ui.notifications.SendNotificationBottomSheetFragment;
import com.upreyvan.carti.ui.track.AddCategoryActivity;
import com.upreyvan.carti.ui.track.AddTrackActivity;
import com.upreyvan.carti.utils.Utils;
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
        setupBackPress();
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToHome();
            }
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().rvOptions, 1f, 20);
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_options_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> goBackToHome());
    }

    private void goBackToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("show_home", true);
        startActivity(intent);
        finish();
    }

    private void goBackToPlan() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("show_plan", true);
        startActivity(intent);
        finish();
    }


    private void setupRecyclerView() {
        List<AddOption> options = getAddOptions();
        GenericAdapter<AddOption, ItemAddOptionBinding> adapter = createAdapter();
        
        adapter.submitList(options);
        getBinding().rvOptions.setLayoutManager(new GridLayoutManager(this, 2));
        getBinding().rvOptions.setAdapter(adapter);
    }

    private List<AddOption> getAddOptions() {
        List<AddOption> options = new ArrayList<>();
        options.add(new AddOption(R.drawable.ic_chart, R.color.icon_food, R.color.log_food, R.string.add_options_expense, R.string.add_options_expense_desc, null));
        options.add(new AddOption(R.drawable.ic_add, R.color.mint_green, R.color.tonal_button_bg, R.string.add_options_goal, R.string.add_options_goal_desc, null));
        options.add(new AddOption(R.drawable.ic_person, R.color.icon_fare, R.color.log_fare, R.string.add_options_invite, R.string.add_options_invite_desc, null));
        options.add(new AddOption(R.drawable.ic_calendar, R.color.carti_primary_blue, R.color.log_water, R.string.add_options_bills, R.string.add_options_bills_desc, null));
        options.add(new AddOption(R.drawable.ic_chart, R.color.status_green, R.color.tonal_button_bg, R.string.add_options_budget_plan, R.string.add_options_budget_plan_desc, null));
        options.add(new AddOption(R.drawable.ic_add, R.color.icon_others, R.color.log_others, R.string.add_options_category, R.string.add_options_category_desc, null));
        
        if (PreferenceManager.getInstance(this).isAdmin()) {
            options.add(new AddOption(R.drawable.ic_bell, R.color.icon_debt, R.color.log_debt, R.string.add_options_announcement, R.string.add_options_announcement_desc, null));
        }
        return options;
    }

    private GenericAdapter<AddOption, ItemAddOptionBinding> createAdapter() {
        GenericAdapter<AddOption, ItemAddOptionBinding> adapter = new GenericAdapter<>(
                AddOption.DIFF_CALLBACK,
                (inflater, parent) -> ItemAddOptionBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.ivOptionIcon.setImageResource(item.getIconResId());
                    binding.ivOptionIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(binding.getRoot().getContext(), item.getIconTintResId())));
                    binding.viewBgTint.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(binding.getRoot().getContext(), item.getBgTintResId())));
                    binding.tvOptionTitle.setText(item.getTitleResId());
                    binding.tvOptionDesc.setText(item.getDescResId());
                }
        );

        adapter.setOnItemClickListener(item -> {
            Intent intent = null;
            int titleId = item.getTitleResId();
            if (titleId == R.string.add_options_expense) {
                intent = new Intent(this, AddTrackActivity.class);
            } else if (titleId == R.string.add_options_debt) {
                intent = new Intent(this, AddDebtActivity.class);
            } else if (titleId == R.string.add_options_goal) {
                intent = new Intent(this, AddGoalActivity.class);
            } else if (titleId == R.string.add_options_invite) {
                intent = new Intent(this, InviteFamilyActivity.class);
            } else if (titleId == R.string.add_options_category) {
                intent = new Intent(this, AddCategoryActivity.class);
            } else if (titleId == R.string.add_options_bills) {
                intent = new Intent(this, BillsActivity.class);
            } else if (titleId == R.string.add_options_budget_plan) {
                goBackToPlan();
            } else if (titleId == R.string.add_options_announcement) {
                new SendNotificationBottomSheetFragment().show(getSupportFragmentManager(), "SendNotificationBottomSheet");
            }
            if (intent != null) startActivity(intent);
        });
        return adapter;
    }
}
