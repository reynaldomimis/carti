package com.upreyvan.carti.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.BottomSheetQuickAddBinding;
import com.upreyvan.carti.databinding.ItemAddOptionHorizontalBinding;
import com.upreyvan.carti.model.AddOption;
import com.upreyvan.carti.ui.bills.AddBillBottomSheet;
import com.upreyvan.carti.ui.debt.AddDebtActivity;
import com.upreyvan.carti.ui.goals.AddGoalBottomSheetFragment;
import com.upreyvan.carti.ui.track.AddTrackActivity;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class QuickAddBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetQuickAddBinding> {

    public static QuickAddBottomSheetFragment newInstance() {
        return new QuickAddBottomSheetFragment();
    }

    @Override
    protected BottomSheetQuickAddBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetQuickAddBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        getBinding().btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        List<AddOption> options = new ArrayList<>();
        options.add(new AddOption(R.drawable.ic_arrow_up, R.color.status_red, R.color.status_red_tonal, R.string.add_options_expense, R.string.add_options_expense_desc, null));
        options.add(new AddOption(R.drawable.ic_arrow_down, R.color.green_primary, R.color.mint_green_alpha, R.string.action_add_income, R.string.income_mode_title, null));
        options.add(new AddOption(R.drawable.ic_calendar, R.color.dash_orange, R.color.dash_orange_alpha, R.string.add_options_bills, R.string.add_options_bills_desc, null));
        options.add(new AddOption(R.drawable.ic_person, R.color.icon_debt, R.color.log_debt, R.string.add_options_debt, R.string.add_options_debt_desc, null));
        options.add(new AddOption(R.drawable.ic_trophy, R.color.carti_primary_green, R.color.mint_green_alpha, R.string.add_options_goal, R.string.add_options_goal_desc, null));

        GenericAdapter<AddOption, ItemAddOptionHorizontalBinding> adapter = new GenericAdapter<>(
                AddOption.DIFF_CALLBACK,
                (inflater, parent) -> ItemAddOptionHorizontalBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.ivIcon.setImageResource(item.getIconResId());
                    binding.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(item.getIconTintResId())));
                    binding.cardIcon.setCardBackgroundColor(requireContext().getColor(item.getBgTintResId()));
                    binding.tvTitle.setText(item.getTitleResId());
                    binding.tvDesc.setText(item.getDescResId());
                }
        );

        adapter.setOnItemClickListener(item -> {
            int titleId = item.getTitleResId();
            if (titleId == R.string.add_options_expense) {
                QuickLogsBottomSheetFragment.newInstance(null).show(getParentFragmentManager(), "QUICK_LOG");
                dismiss();
            } else if (titleId == R.string.action_add_income) {
                if (getActivity() instanceof com.upreyvan.carti.MainActivity) {
                    ((MainActivity) getActivity()).navigateTo(Constants.Navigation.INCOME);
                }
                dismiss();
            } else if (titleId == R.string.add_options_bills) {
                AddBillBottomSheet.newInstance(Utils.formatDateFull(Calendar.getInstance())).show(getParentFragmentManager(), "AddBill");
                dismiss();
            } else if (titleId == R.string.add_options_debt) {
                startActivity(new Intent(requireContext(), AddDebtActivity.class));
                dismiss();
            } else if (titleId == R.string.add_options_goal) {
                AddGoalBottomSheetFragment.newInstance().show(getParentFragmentManager(), "AddGoal");
                dismiss();
            }
        });

        getBinding().rvOptions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvOptions.setAdapter(adapter);
        adapter.submitList(options);
    }
}
