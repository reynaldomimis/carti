package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentPlanBinding;
import com.upreyvan.carti.ui.bills.BillsFragment;
import com.upreyvan.carti.ui.goals.GoalFragment;

public class PlanFragment extends BaseFragment<FragmentPlanBinding> {
    private PlanViewModel viewModel;

    public static PlanFragment newInstance(boolean track) {
        return new PlanFragment();
    }

    @Override
    protected FragmentPlanBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPlanBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        setupDynamicPadding(getBinding().headerContainer, null);
        setupViewPager();
        viewModel.loadData();
    }

    private void setupViewPager() {
        getBinding().viewPager.setAdapter(new PlanPagerAdapter(this));
        new TabLayoutMediator(getBinding().tabLayout, getBinding().viewPager, (tab, position) -> {
            switch (position) {
                case 0 -> {
                    tab.setText(R.string.label_budget);
                    tab.setIcon(R.drawable.ic_chart);
                }
                case 1 -> {
                    tab.setText(R.string.bills_title);
                    tab.setIcon(R.drawable.ic_calendar);
                }
                case 2 -> {
                    tab.setText(R.string.goal_title);
                    tab.setIcon(R.drawable.ic_trophy);
                }
            }
        }).attach();
        
        getBinding().viewPager.setOffscreenPageLimit(2);
    }

    private static class PlanPagerAdapter extends FragmentStateAdapter {
        public PlanPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> new BudgetFragment();
                case 1 -> new BillsFragment();
                case 2 -> new GoalFragment();
                default -> throw new IllegalArgumentException("Invalid position: " + position);
            };
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
