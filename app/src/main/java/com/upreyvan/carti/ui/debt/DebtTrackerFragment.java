package com.upreyvan.carti.ui.debt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.DialogDebtDetailBinding;
import com.upreyvan.carti.databinding.FragmentDebtTrackerBinding;
import com.upreyvan.carti.databinding.ItemDebtBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebtTrackerFragment extends BaseFragment<FragmentDebtTrackerBinding> {

    private GenericAdapter<TransactionWithUser, ItemDebtBinding> adapter;
    private List<TransactionWithUser> allDebts = new ArrayList<>();
    private ApiHelper apiHelper;
    private TransactionRepository transactionRepository;
    private RealtimeRepository realtimeRepo;
    private boolean isLoading = true;

    @Override
    protected FragmentDebtTrackerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDebtTrackerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        transactionRepository = TransactionRepository.getInstance(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        setupDynamicPadding();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        observeDebts();
        observeRealtimeChanges();
    }

    /**
     * Senior Optimization: Listen to the central hub for debt changes.
     */
    private void observeRealtimeChanges() {
        realtimeRepo.getDebtStream().observe(getViewLifecycleOwner(), payload -> {
            // Trigger local refresh when websocket reports a change
            transactionRepository.refreshTransactions();
        });
        
        // Also listen to transaction stream as debts are technically transactions
        realtimeRepo.getTransactionStream().observe(getViewLifecycleOwner(), payload -> {
            transactionRepository.refreshTransactions();
        });
    }

    private void setupDynamicPadding() {
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().rvDebts, 0.3f);
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                getBinding().btnAddDebtFloating,
                getBinding().btnAddDebtFloating,
                0f,
                0
        );
    }

    private void observeDebts() {
        transactionRepository.getTransactionsByType("DEBT").observe(getViewLifecycleOwner(), debts -> {
            if (debts != null) {
                isLoading = false;
                allDebts = debts;
                filterDebts(getBinding().tabLayout.getSelectedTabPosition());
            }
        });
        transactionRepository.syncTransactionsIfNeeded();
    }

    private void setupToolbar() {
        setupToolbar(getBinding().layoutToolbar, R.string.debt_tracker_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        
        getBinding().btnAddDebtFloating.setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireContext(), AddDebtActivity.class));
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterDebts(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterDebts(int position) {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        String emptyTitle;
        String emptyDesc;

        switch (position) {
            case 0: // Overview / All
                filteredList.addAll(allDebts);
                emptyTitle = getString(R.string.no_debts_title);
                emptyDesc = getString(R.string.no_debts_desc);
                break;
            case 1: // My Debts (Unpaid)
                for (TransactionWithUser debt : allDebts) {
                    if (!debt.getTransaction().isPaid()) filteredList.add(debt);
                }
                emptyTitle = getString(R.string.no_my_debts_title);
                emptyDesc = getString(R.string.no_my_debts_desc);
                break;
            case 2: // Settled (Paid)
                for (TransactionWithUser debt : allDebts) {
                    if (debt.getTransaction().isPaid()) filteredList.add(debt);
                }
                emptyTitle = getString(R.string.no_settled_debts_title);
                emptyDesc = getString(R.string.no_settled_debts_desc);
                break;
            default:
                emptyTitle = getString(R.string.no_debts_title);
                emptyDesc = getString(R.string.no_debts_desc);
                break;
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
                        
                        binding.tvPersonName.setText(debt.getName());
                        binding.tvDescription.setText(debt.getNote());
                        binding.tvAmount.setText(Utils.formatCurrency(debt.getAmount()));
                        binding.tvDate.setText(Utils.getTimeAgo(debt.getTimestampMillis()));
                        binding.ivAvatar.setImageResource(debt.getIconRes() != 0 ? debt.getIconRes() : R.drawable.ic_person);
                        
                        binding.tvStatus.setText(debt.isPaid() ? R.string.status_paid : R.string.status_not_paid);
                        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), 
                                debt.isPaid() ? R.color.status_green : R.color.status_red));
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

        dialogBinding.tvDetailName.setText(debt.getName());
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
                apiHelper.markDebtPaid(debt.getId(), new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        requireActivity().runOnUiThread(() -> {
                            transactionRepository.refreshTransactions();
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        requireActivity().runOnUiThread(() -> {
                            dialogBinding.btnMarkAsPaid.setEnabled(true);
                            Utils.showToast(requireContext(), "Error: " + error.getMessage());
                        });
                    }
                });
            });
        }

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionRepository != null) {
            transactionRepository.onDestroy();
        }
    }
}
