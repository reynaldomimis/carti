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
import com.upreyvan.carti.data.repository.DebtRepository;
import com.upreyvan.carti.databinding.DialogDebtDetailBinding;
import com.upreyvan.carti.databinding.FragmentDebtTrackerBinding;
import com.upreyvan.carti.databinding.ItemDebtBinding;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DebtTrackerFragment extends BaseFragment<FragmentDebtTrackerBinding> {

    private GenericAdapter<Debt, ItemDebtBinding> adapter;
    private List<Debt> allDebts = new ArrayList<>();
    private ApiHelper apiHelper;
    private DebtRepository debtRepository;
    private boolean isLoading = true;

    @Override
    protected FragmentDebtTrackerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDebtTrackerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        debtRepository = new DebtRepository(requireContext());
        
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        observeDebts();
    }

    private void observeDebts() {
        debtRepository.getAllDebts().observe(getViewLifecycleOwner(), debts -> {
            if (debts != null) {
                isLoading = debts.isEmpty();
                allDebts = debts;
                if (isLoading) {
                    List<Debt> placeholders = new ArrayList<>();
                    for (int i = 0; i < 3; i++) placeholders.add(new Debt());
                    adapter.submitList(placeholders);
                } else {
                    filterDebts(getBinding().tabLayout.getSelectedTabPosition());
                    updateOverallDebt(allDebts);
                }
            }
        });
        debtRepository.syncDebtsIfNeeded();
    }

    private void updateOverallDebt(List<Debt> debts) {
        double totalOwed = 0;
        double totalPaid = 0;

        for (Debt debt : debts) {
            if (debt.isPaid()) {
                totalPaid += debt.getAmount();
            } else {
                totalOwed += debt.getAmount();
            }
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        getBinding().tvTotalAmount.setText(currencyFormat.format(totalOwed));

        double total = totalOwed + totalPaid;
        if (total > 0) {
            int progress = (int) ((totalPaid / total) * 100);
            getBinding().tvOverallPercentage.setText(getString(R.string.overall_debt_percentage_format, progress));
        } else {
            getBinding().tvOverallPercentage.setText(getString(R.string.zero_percent));
        }
    }

    private void setupToolbar() {
        setupToolbar(getBinding().layoutToolbar, R.string.debt_tracker_title);
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_debt);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
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

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Debt.DIFF_CALLBACK,
                (inflater, parent) -> ItemDebtBinding.inflate(inflater, parent, false),
                (binding, debt) -> {
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        
                        binding.tvPersonName.setText(debt.getTitle());
                        binding.tvDescription.setText(debt.getDescription());
                        binding.tvAmount.setText(Utils.formatCurrency(debt.getAmount()));
                        binding.tvDate.setText(debt.getTimestamp());
                        binding.ivAvatar.setImageResource(debt.getAvatarResId());
                        
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

    private void filterDebts(int tabIndex) {
        List<Debt> filtered = new ArrayList<>();
        if (tabIndex == 0) {
            for (Debt d : allDebts) {
                if (!d.isPaid()) filtered.add(d);
            }
        } else {
            for (Debt d : allDebts) {
                if (d.isPaid()) filtered.add(d);
            }
        }
        adapter.submitList(filtered);
    }

    private void showDebtDetail(Debt debt) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme);
        DialogDebtDetailBinding dialogBinding = DialogDebtDetailBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.tvDetailName.setText(debt.getTitle());
        dialogBinding.tvDetailDesc.setText(debt.getDescription());
        dialogBinding.tvDetailAmount.setText(Utils.formatCurrency(debt.getAmount()));
        dialogBinding.tvDetailDate.setText(debt.getTimestamp());
        dialogBinding.tvNotes.setText(debt.getNotes());
        dialogBinding.ivDetailAvatar.setImageResource(debt.getAvatarResId());

        if (debt.isPaid()) {
            dialogBinding.btnMarkAsPaid.setEnabled(false);
            dialogBinding.btnMarkAsPaid.setText(R.string.status_paid);
            dialogBinding.tvPaymentHistory.setText("Paid on " + debt.getTimestamp());
        } else {
            dialogBinding.btnMarkAsPaid.setOnClickListener(v -> {
                dialogBinding.btnMarkAsPaid.setEnabled(false);
                apiHelper.markDebtPaid(debt.getId(), new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        requireActivity().runOnUiThread(() -> {
                            debt.setPaid(true);
                            adapter.notifyDataSetChanged();
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
}
