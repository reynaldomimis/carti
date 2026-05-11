package com.upreyvan.carti.fragments;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.adapters.DebtAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.DialogDebtDetailBinding;
import com.upreyvan.carti.databinding.FragmentDebtTrackerBinding;
import com.upreyvan.carti.model.Debt;

import java.util.ArrayList;
import java.util.List;

public class DebtTrackerFragment extends BaseFragment<FragmentDebtTrackerBinding> {

    private DebtAdapter adapter;
    private List<Debt> allDebts = new ArrayList<>();

    @Override
    protected FragmentDebtTrackerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDebtTrackerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        loadDummyData();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.debt_tracker_title);
        getBinding().layoutToolbar.btnBack.setVisibility(View.GONE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_debt);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            // Handle add debt
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterDebts(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new DebtAdapter();
        getBinding().rvDebts.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvDebts.setAdapter(adapter);

        adapter.setOnDebtClickListener(this::showDebtDetail);
    }

    private void loadDummyData() {
        allDebts.add(new Debt("Ate Liza", "Utang para sa groceries", "May 25, 2024", 500.0, false, R.drawable.ic_person, "Binili sa SM Supermarket"));
        allDebts.add(new Debt("Tindahan ni Aling Nena", "Utang sa tingi", "May 20, 2024", 120.0, false, R.drawable.ic_person, "Kape at asukal"));
        allDebts.add(new Debt("Kuya Mario", "Utang na cash", "May 18, 2024", 300.0, true, R.drawable.ic_person, "Pambayad sa kuryente"));

        filterDebts(0);
    }

    private void filterDebts(int tabIndex) {
        if (tabIndex == 0) {
            adapter.setDebts(allDebts);
        } else {
            List<Debt> filtered = new ArrayList<>();
            for (Debt d : allDebts) {
                if (d.getAmount() > 200) filtered.add(d);
            }
            adapter.setDebts(filtered);
        }
    }

    private void showDebtDetail(Debt debt) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme);
        DialogDebtDetailBinding dialogBinding = DialogDebtDetailBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.tvDetailName.setText(debt.getPersonName());
        dialogBinding.tvDetailDesc.setText(debt.getDescription());
        dialogBinding.tvDetailAmount.setText(String.format("₱%.0f", debt.getAmount()));
        dialogBinding.tvDetailDate.setText(debt.getDate());
        dialogBinding.tvNotes.setText(debt.getNotes());
        dialogBinding.ivDetailAvatar.setImageResource(debt.getAvatarResId());

        if (debt.isPaid()) {
            dialogBinding.btnMarkAsPaid.setEnabled(false);
            dialogBinding.btnMarkAsPaid.setText(R.string.status_paid);
            dialogBinding.tvPaymentHistory.setText("Paid on " + debt.getDate());
        } else {
            dialogBinding.btnMarkAsPaid.setOnClickListener(v -> {
                debt.setPaid(true);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}