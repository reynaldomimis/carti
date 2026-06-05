package com.upreyvan.carti.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.FragmentQuickLogsBottomSheetBinding;
import com.upreyvan.carti.databinding.LayoutExpenseFormBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.BudgetAllocationHelper;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.StringHelper;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.TransactionHandler;
import com.upreyvan.carti.util.Validator;

import java.util.ArrayList;
import java.util.List;

public class QuickLogsBottomSheetFragment extends BaseBottomSheetFragment<FragmentQuickLogsBottomSheetBinding> {

    public enum LogType {
        EXPENSE, DEBT, GOAL
    }

    private String selectedCategory;
    private LogType logType = LogType.EXPENSE;
    private MemberRepository memberRepository;

    public static QuickLogsBottomSheetFragment newInstance(String categoryName) {
        QuickLogsBottomSheetFragment fragment = new QuickLogsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("category_name", categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memberRepository = new MemberRepository(requireContext());
        if (getArguments() != null) {
            selectedCategory = getArguments().getString("category_name");
            determineLogType();
        }
    }

    private void determineLogType() {
        if (selectedCategory == null) {
            logType = LogType.EXPENSE;
            return;
        }

        String lowerCat = selectedCategory.toLowerCase();
        if (lowerCat.contains("debt") || lowerCat.contains("utang")) {
            logType = LogType.DEBT;
        } else if (lowerCat.contains("goal") || lowerCat.contains("savings") || lowerCat.contains("ipon")) {
            logType = LogType.GOAL;
        } else {
            logType = LogType.EXPENSE;
        }
    }

    @Override
    protected FragmentQuickLogsBottomSheetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentQuickLogsBottomSheetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDropdowns();
        setupClickListeners();
        updateUI();
    }

    private void updateUI() {
        String title = getString(R.string.quick_log_title);
        String btnText = getString(R.string.btn_save_expense);
        
        switch (logType) {
            case DEBT:
                title = getString(R.string.title_quick_debt_log);
                btnText = getString(R.string.btn_save_debt);
                getBinding().layoutForm.tilDescription.setHint(getString(R.string.label_reason_note_hint));
                getBinding().layoutForm.tilSource.setHint(getString(R.string.label_who_borrowed_hint));
                getBinding().layoutForm.cvBalanceInfo.setVisibility(View.GONE);
                break;
            case GOAL:
                title = getString(R.string.title_quick_goal_log);
                btnText = getString(R.string.btn_add_savings);
                getBinding().layoutForm.tilDescription.setHint(getString(R.string.label_note_optional));
                getBinding().layoutForm.tilSource.setVisibility(View.GONE);
                getBinding().layoutForm.allocatedHeader.setText(getString(R.string.label_goal_progress));
                break;
            case EXPENSE:
                title = getString(R.string.quick_log_title);
                btnText = getString(R.string.btn_save_expense);
                getBinding().layoutForm.tilDescription.setHint(getString(R.string.label_what_bought));
                getBinding().layoutForm.tilSource.setHint(getString(R.string.label_payment_source));
                
                if (selectedCategory != null) {
                    getBinding().layoutForm.allocatedHeader.setText(getString(R.string.category_expense_label, selectedCategory));
                    BudgetAllocationHelper.getRemainingBalance(requireContext(), selectedCategory, balance -> {
                        if (getBinding() != null && getBinding().layoutForm != null) {
                            getBinding().layoutForm.tvAllocatedBalance.setText(StringHelper.formatCurrency(balance));
                        }
                    });
                } else {
                    getBinding().layoutForm.tvAllocatedBalance.setText(StringHelper.formatCurrency(0.0));
                }
                break;
        }

        getBinding().tvCategoryLabel.setText(title);
        getBinding().btnSave.setText(btnText);
    }

    private void setupDropdowns() {
        if (logType == LogType.EXPENSE) {
            String[] sources = Constants.sourcesFund;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, sources);
            getBinding().layoutForm.actvSource.setAdapter(adapter);
            getBinding().layoutForm.actvSource.setText(sources[0], false);
        } else if (logType == LogType.DEBT) {
            memberRepository.getMembers().observe(getViewLifecycleOwner(), members -> {
                List<String> names = new ArrayList<>();
                for (Member m : members) names.add(m.getTitle());
                if (names.isEmpty()) names.add("Self");
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names);
                getBinding().layoutForm.actvSource.setAdapter(adapter);
                if (!names.isEmpty()) getBinding().layoutForm.actvSource.setText(names.get(0), false);
            });
        }
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            if (selectedCategory == null) {
                showToast(getString(R.string.err_no_category_selected), UiHelper.Status.WARNING);
                return;
            }

            if (Validator.isEmpty(getBinding().layoutForm.etAmount)) {
                showToast(getString(R.string.msg_fill_all_fields), UiHelper.Status.WARNING);
                return;
            }

            double amountVal = StringHelper.parseDouble(getBinding().layoutForm.etAmount.getText().toString());
            String description = getBinding().layoutForm.etDescription.getText().toString();
            String sourceOrPerson = getBinding().layoutForm.actvSource.getText().toString();

            if (amountVal <= 0) {
                showToast(getString(R.string.msg_invalid_amount), UiHelper.Status.WARNING);
                return;
            }

            handleSave(amountVal, description, sourceOrPerson);
        });
    }

    private void handleSave(double amount, String description, String extra) {
        TransactionHandler.TransactionCallback callback = new TransactionHandler.TransactionCallback() {
            @Override
            public void onLoading(boolean isLoading) {
                String msg = getString(R.string.msg_saving);
                if (logType == LogType.EXPENSE) msg = getString(R.string.msg_saving_expense);
                else if (logType == LogType.DEBT) msg = getString(R.string.msg_saving_debt);
                else if (logType == LogType.GOAL) msg = getString(R.string.msg_saving_goal);
                showLoading(isLoading, msg);
                if (getBinding() != null) {
                    getBinding().btnSave.setEnabled(!isLoading);
                }
            }

            @Override
            public void onSuccess(Transaction transaction) {
                String successMsg = getString(R.string.msg_save_success);
                if (logType == LogType.EXPENSE) successMsg = getString(R.string.msg_expense_saved);
                else if (logType == LogType.DEBT) successMsg = getString(R.string.msg_debt_saved_simple);
                else if (logType == LogType.GOAL) successMsg = getString(R.string.msg_goal_updated);
                
                showToast(successMsg, UiHelper.Status.SUCCESS);
                dismiss();
            }

            @Override
            public void onError(String message) {
                showToast(getString(R.string.err_failed_save, message), UiHelper.Status.ERROR);
            }
        };

        switch (logType) {
            case EXPENSE:
                TransactionHandler.saveTrack(requireContext(), amount, selectedCategory, description, extra, callback);
                break;
            case DEBT:
                TransactionHandler.saveDebt(requireContext(), amount, extra, description, callback);
                break;
            case GOAL:
                TransactionHandler.saveGoal(requireContext(), amount, selectedCategory, callback);
                break;
        }
    }
}

