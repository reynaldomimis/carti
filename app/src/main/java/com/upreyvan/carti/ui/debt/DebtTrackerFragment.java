package com.upreyvan.carti.ui.debt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.DialogDebtDetailBinding;
import com.upreyvan.carti.databinding.FragmentDebtTrackerBinding;
import com.upreyvan.carti.databinding.ItemDebtBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.Utils;
import java.util.ArrayList;
import java.util.List;

public class DebtTrackerFragment extends BaseFragment<FragmentDebtTrackerBinding> {

    private GenericAdapter<TransactionWithUser, ItemDebtBinding> adapter;
    private List<TransactionWithUser> allDebts = new ArrayList<>();
    private DebtViewModel viewModel;
    private boolean isLoading = true;

    @Override
    protected FragmentDebtTrackerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDebtTrackerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DebtViewModel.class);
        
        setupDynamicPadding();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        observeViewModel();
        observeRealtimeChanges();
    }

    private void observeRealtimeChanges() {
        RealtimeRepository.getInstance(requireContext()).getDebtStream().observe(getViewLifecycleOwner(), payload -> viewModel.refresh());
        RealtimeRepository.getInstance(requireContext()).getTransactionStream().observe(getViewLifecycleOwner(), payload -> viewModel.refresh());
    }

    private void setupDynamicPadding() {
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().rvDebts, 0.3f);
        Utils.applySystemBarInsets(getBinding().btnAddDebtFloating, getBinding().btnAddDebtFloating, 0f, 0);
    }

    private void observeViewModel() {
        viewModel.getDebts().observe(getViewLifecycleOwner(), debts -> {
            if (debts != null) {
                isLoading = false;
                allDebts = debts;
                filterDebts(getBinding().tabLayout.getSelectedTabPosition());
            }
        });
        
        viewModel.getMarkPaidSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                // Handled via ViewModel.refresh() and LiveData observation
            }
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Utils.showToast(requireContext(), "Error: " + error);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            // Show loading if needed
        });
        
        viewModel.refresh();
    }

    private void setupToolbar() {
        setupToolbar(getBinding().layoutToolbar, R.string.debt_tracker_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        getBinding().btnAddDebtFloating.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddDebtActivity.class)));
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterDebts(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterDebts(int position) {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        String emptyTitle;
        String emptyDesc;
        switch (position) {
            case 1 -> {
                for (TransactionWithUser debt : allDebts) {
                    if (!debt.getTransaction().isPaid()) filteredList.add(debt);
                }
                emptyTitle = getString(R.string.no_my_debts_title);
                emptyDesc = getString(R.string.no_my_debts_desc);
            }
            case 2 -> {
                for (TransactionWithUser debt : allDebts) {
                    if (debt.getTransaction().isPaid()) filteredList.add(debt);
                }
                emptyTitle = getString(R.string.no_settled_debts_title);
                emptyDesc = getString(R.string.no_settled_debts_desc);
            }
            default -> {
                filteredList.addAll(allDebts);
                emptyTitle = getString(R.string.no_debts_title);
                emptyDesc = getString(R.string.no_debts_desc);
            }
        }

        if (filteredList.isEmpty() && !isLoading) {
            getBinding().rvDebts.setVisibility(View.GONE);
            getBinding().layoutEmptyState.setVisibility(View.VISIBLE);
            getBinding().tvEmptyTitle.setText(emptyTitle);
            getBinding().tvEmptyDesc.setText(emptyDesc);
        } else {
            getBinding().rvDebts.setVisibility(View.VISIBLE);
            getBinding().layoutEmptyState.setVisibility(View.GONE);
        }
        adapter.submitList(filteredList);
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                TransactionWithUser.DIFF_CALLBACK,
                (inflater, parent) -> ItemDebtBinding.inflate(inflater, parent, false),
                (binding, itemWithUser) -> {
                    Transaction debt = itemWithUser.getTransaction();
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        binding.tvPersonName.setText(debt.getTitle());
                        binding.tvDescription.setText(debt.getNote());
                        binding.tvAmount.setText(Utils.formatCurrency(debt.getAmount()));
                        binding.tvDate.setText(Utils.getTimeAgo(debt.getTimestampMillis()));
                        binding.ivAvatar.setImageResource(debt.getIconRes() != 0 ? debt.getIconRes() : R.drawable.ic_person);
                        binding.tvStatus.setText(debt.isPaid() ? R.string.status_paid : R.string.status_not_paid);
                        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), debt.isPaid() ? R.color.status_green : R.color.status_red));
                    }
                }
        );
        getBinding().rvDebts.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvDebts.setAdapter(adapter);
        adapter.setOnItemClickListener(this::showDebtDetail);
    }

    private void showDebtDetail(TransactionWithUser itemWithUser) {
        Transaction debt = itemWithUser.getTransaction();
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme);
        DialogDebtDetailBinding dialogBinding = DialogDebtDetailBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.tvDetailName.setText(debt.getTitle());
        dialogBinding.tvDetailDesc.setText(debt.getNote());
        dialogBinding.tvDetailAmount.setText(Utils.formatCurrency(debt.getAmount()));
        dialogBinding.tvDetailDate.setText(Utils.getTimeAgo(debt.getTimestampMillis()));
        dialogBinding.tvNotes.setText(debt.getNote());
        dialogBinding.ivDetailAvatar.setImageResource(debt.getIconRes() != 0 ? debt.getIconRes() : R.drawable.ic_person);

        if (debt.isPaid()) {
            dialogBinding.btnMarkAsPaid.setEnabled(false);
            dialogBinding.btnMarkAsPaid.setText(R.string.status_paid);
            dialogBinding.tvPaymentHistory.setText(getString(R.string.label_paid_on, Utils.getTimeAgo(debt.getTimestampMillis())));
        } else {
            dialogBinding.btnMarkAsPaid.setOnClickListener(v -> {
                dialogBinding.btnMarkAsPaid.setEnabled(false);
                viewModel.markDebtPaid(debt.getId());
                dialog.dismiss();
            });
        }
        dialog.show();
    }
}
