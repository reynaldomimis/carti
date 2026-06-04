package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentBillsBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.allocate.PlanViewModel;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BillsFragment extends BaseFragment<FragmentBillsBinding> {

    private BillAdapter billAdapter;
    private PlanViewModel viewModel;
    private boolean showingAllBills = false;
    private static final int MAX_BILLS_DISPLAY = 5;

    @Override
    protected FragmentBillsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBillsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        
        setupUI();
        setupBillsRecyclerView();
        observeData();
    }

    private void setupUI() {
        getBinding().layoutHeader.tvHeaderTitle.setText(R.string.label_setup_bills_debts);
        String monthYear = Utils.formatMonthYear(Calendar.getInstance());
        getBinding().layoutHeader.tvHeaderSubtitle.setText(getString(R.string.for_the_month_of, monthYear));
        getBinding().layoutHeader.btnHeaderAction.setText(R.string.label_add_bill_plus);
        getBinding().layoutHeader.btnHeaderAction.setOnClickListener(v -> showAddBillBottomSheet());
        
        getBinding().layoutUpcomingHeader.tvSectionTitle.setText(R.string.label_upcoming_bills);
        getBinding().layoutUpcomingHeader.btnSectionAction.setVisibility(View.GONE);

        getBinding().btnViewAllBills.setVisibility(View.GONE);
    }

    private void observeData() {
        getBinding().pbBills.setVisibility(View.VISIBLE);
        getBinding().layoutBillsContent.setVisibility(View.GONE);
        getBinding().layoutEmptyBills.setVisibility(View.GONE);

        viewModel.getBills().observe(getViewLifecycleOwner(), bills -> {
            updateSummary(bills, viewModel.getDebts().getValue());
            getBinding().pbBills.setVisibility(View.GONE);
            if (bills == null || bills.isEmpty()) {
                getBinding().layoutBillsContent.setVisibility(View.GONE);
                getBinding().layoutEmptyBills.setVisibility(View.VISIBLE);
            } else {
                getBinding().layoutBillsContent.setVisibility(View.VISIBLE);
                getBinding().layoutEmptyBills.setVisibility(View.GONE);
                updateBillList(bills);
            }
        });

        viewModel.getDebts().observe(getViewLifecycleOwner(), debts -> 
                updateSummary(viewModel.getBills().getValue(), debts));
    }

    private void updateSummary(List<Bill> bills, List<TransactionWithUser> debts) {
        int unpaid = 0, paid = 0, overdue = 0;
        double unpaidAmt = 0, paidAmt = 0, overdueAmt = 0, debtAmt = 0;

        if (bills != null) {
            for (Bill b : bills) {
                if ("PAID".equalsIgnoreCase(b.getStatus())) {
                    paid++;
                    paidAmt += b.getAmount();
                } else if ("OVERDUE".equalsIgnoreCase(b.getStatus())) {
                    overdue++;
                    overdueAmt += b.getAmount();
                } else {
                    unpaid++;
                    unpaidAmt += b.getAmount();
                }
            }
        }
        
        if (debts != null) {
            for (TransactionWithUser d : debts) {
                debtAmt += d.getTransaction().getAmount();
            }
        }

        getBinding().layoutSummary.tvUnpaidCount.setText(String.valueOf(unpaid));
        getBinding().layoutSummary.tvPaidCount.setText(String.valueOf(paid));
        getBinding().layoutSummary.tvOverdueCount.setText(String.valueOf(overdue));
        getBinding().layoutSummary.tvDebtsCount.setText(String.valueOf(debts != null ? debts.size() : 0));

        getBinding().layoutSummary.tvUnpaidAmount.setText(Utils.formatCurrency(unpaidAmt)); 
        getBinding().layoutSummary.tvPaidAmount.setText(Utils.formatCurrency(paidAmt));
        getBinding().layoutSummary.tvOverdueAmount.setText(Utils.formatCurrency(overdueAmt));
        getBinding().layoutSummary.tvDebtsAmount.setText(Utils.formatCurrency(debtAmt));
    }

    private void updateBillList(List<Bill> bills) {
        if (bills == null) return;
        if (!showingAllBills && bills.size() > MAX_BILLS_DISPLAY) {
            billAdapter.submitList(new ArrayList<>(bills.subList(0, MAX_BILLS_DISPLAY)));
            getBinding().btnViewAllBills.setVisibility(View.VISIBLE);
        } else {
            billAdapter.submitList(new ArrayList<>(bills));
            getBinding().btnViewAllBills.setVisibility(View.GONE);
        }
    }

    private void setupBillsRecyclerView() {
        billAdapter = new BillAdapter();
        billAdapter.setOnItemClickListener(item -> BillDetailsBottomSheet.newInstance(item.getId(), item.getName()).show(getChildFragmentManager(), "BillDetailsBottomSheet"));
        getBinding().rvBills.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvBills.setAdapter(billAdapter);
    }

    private void showAddBillBottomSheet() {
        AddBillBottomSheet.newInstance(Utils.formatDateFull(Calendar.getInstance())).show(getChildFragmentManager(), "AddBillBottomSheet");
    }
}
