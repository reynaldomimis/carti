package com.upreyvan.carti.ui.debt;

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
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.ui.debt.DebtAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.DebtManager;
import com.upreyvan.carti.databinding.DialogDebtDetailBinding;
import com.upreyvan.carti.databinding.FragmentDebtTrackerBinding;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.data.remote.AppwriteManager;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.widget.Toast;
import java.text.NumberFormat;
import java.util.Locale;

public class DebtTrackerFragment extends BaseFragment<FragmentDebtTrackerBinding> {

    private DebtAdapter adapter;
    private List<Debt> allDebts = new ArrayList<>();
    private ApiHelper apiHelper;

    @Override
    protected FragmentDebtTrackerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDebtTrackerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        loadDebts();

        DebtManager.getInstance().setOnDebtChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::loadDebts);
            }
        });
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

    private void loadDebts() {
        // Load local data first
        allDebts = DebtManager.getInstance().getDebts();
        filterDebts(getBinding().tabLayout.getSelectedTabPosition());
        updateOverallDebt(allDebts);

        // Fetch fresh data from Cloud
        apiHelper.getDebts(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;

                List<Debt> cloudDebts = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    cloudDebts.add(mapToDebt(doc.getData(), doc.getId()));
                }

                // Update Local Cache
                DebtManager.getInstance().setDebts(cloudDebts);

                requireActivity().runOnUiThread(() -> {
                    allDebts = cloudDebts;
                    filterDebts(getBinding().tabLayout.getSelectedTabPosition());
                    updateOverallDebt(allDebts);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error fetching debts: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private Debt mapToDebt(Map<String, Object> data, String id) {
        String name = String.valueOf(data.get("personName"));
        double amount = 0;
        Object amt = data.get("amount");
        if (amt instanceof Number) amount = ((Number) amt).doubleValue();

        boolean isPaid = Boolean.TRUE.equals(data.get("isPaid"));
        String date = "Upcoming";

        return new Debt(id, name, "", date, amount, isPaid, R.drawable.ic_person, "");
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.debt_tracker_title);
        getBinding().layoutToolbar.btnBack.setVisibility(View.VISIBLE);
        if (getBinding().layoutToolbar.backButtonContainer != null) {
            getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        }
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_debt);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), AddDebtActivity.class);
            startActivity(intent);
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

    private void filterDebts(int tabIndex) {
        List<Debt> filtered = new ArrayList<>();
        if (tabIndex == 0) { // All or Owed to me
            for (Debt d : allDebts) {
                if (!d.isPaid()) filtered.add(d);
            }
        } else { // History / Paid
            for (Debt d : allDebts) {
                if (d.isPaid()) filtered.add(d);
            }
        }
        adapter.setDebts(filtered);
    }

    private void showDebtDetail(Debt debt) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme);
        DialogDebtDetailBinding dialogBinding = DialogDebtDetailBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

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
                dialogBinding.btnMarkAsPaid.setEnabled(false);
                apiHelper.markDebtPaid(debt.getId(), new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
                    @Override
                    public void onSuccess(java.util.Map<String, Object> result) {
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
                            com.upreyvan.carti.util.Utils.showToast(requireContext(), "Error: " + error.getMessage());
                        });
                    }
                });
            });
        }

        dialog.show();
    }
}
