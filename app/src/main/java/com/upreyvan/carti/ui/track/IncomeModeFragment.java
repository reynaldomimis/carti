package com.upreyvan.carti.ui.track;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentIncomeModeBinding;
import com.upreyvan.carti.managers.SalaryManager;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.models.TransactionWithUser;
import java.util.ArrayList;
import java.util.List;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemIncomeBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.utils.Utils;
import com.upreyvan.carti.utils.ValueHelper;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class IncomeModeFragment extends BaseFragment<FragmentIncomeModeBinding> {

    private GenericAdapter<Transaction, ItemIncomeBinding> incomeAdapter;
    private TransactionRepository transactionRepository;
    private RealtimeRepository realtimeRepo;

    @Override
    protected FragmentIncomeModeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentIncomeModeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        transactionRepository = TransactionRepository.getInstance(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        setupToolbar(getBinding().layoutToolbar, "Allocation Mode");
        setupRecyclerView();
        observeIncomes();
        observeRealtime();
        updateUI();
        setupHeaderActions();
        setupListeners();
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), null, 0.3f);
    }

    private void observeRealtime() {
        realtimeRepo.getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> updateUI());
    }

    private void setupRecyclerView() {
        if (incomeAdapter == null) {
            incomeAdapter = new GenericAdapter<>(
                    Transaction.DIFF_CALLBACK,
                    (inflater, parent) -> ItemIncomeBinding.inflate(inflater, parent, false),
                    (binding, income, position, count) -> {
                        binding.tvSource.setText(income.getTitle());
                        binding.tvAmount.setText(String.format(Locale.getDefault(), "+₱%,.2f", income.getAmount()));
                        binding.tvDate.setText(Utils.getTimeAgo(income.getTimestampMillis()));
                        
                        if (ValueHelper.toStr(income.getTitle()).toLowerCase().contains("salary")) {
                            binding.ivIcon.setImageResource(R.drawable.ic_calendar);
                            binding.viewIconBg.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.log_food));
                        } else {
                            binding.ivIcon.setImageResource(R.drawable.ic_chart);
                            binding.viewIconBg.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.log_fare));
                        }
                    }
            );
            incomeAdapter.setOnItemLongClickListener(income -> {
                IncomeEditBottomSheet bottomSheet = IncomeEditBottomSheet.newInstance(income);
                bottomSheet.setListener(this::updateUI);
                bottomSheet.show(getChildFragmentManager(), "IncomeEditBottomSheet");
                return true;
            });
        }
        getBinding().rvIncomeHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvIncomeHistory.setAdapter(incomeAdapter);
    }

    private void observeIncomes() {
        String myUserId = com.upreyvan.carti.managers.PreferenceManager.getInstance(requireContext()).getUserId();
        transactionRepository.getIncome().observe(getViewLifecycleOwner(), incomesWithUser -> {
            List<Transaction> incomes = new ArrayList<>();
            if (incomesWithUser != null) {
                for (TransactionWithUser t : incomesWithUser) {
                    if (Objects.equals(t.getTransaction().getUserId(), myUserId)) {
                        incomes.add(t.getTransaction());
                    }
                }
            }
            boolean hasIncomes = !incomes.isEmpty();
            getBinding().tvIncomeHistoryLabel.setText("Contribution History");
            getBinding().tvIncomeHistoryLabel.setVisibility(hasIncomes ? View.VISIBLE : View.GONE);
            getBinding().cardIncomeHistory.setVisibility(hasIncomes ? View.VISIBLE : View.GONE);

            incomeAdapter.submitList(incomes);
        });

        transactionRepository.getTotalIncome().observe(getViewLifecycleOwner(), totalIncome -> 
                updateTotalBudget(Objects.requireNonNullElse(totalIncome, 0.0)));
    }

    private void updateTotalBudget(double totalBudget) {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        double dailyBudget = manager.getDailyBudget(totalBudget);

        getBinding().tvSalaryAmount.setText(Utils.formatCurrency(totalBudget));
        getBinding().tvDailyBudget.setText(getString(R.string.format_currency_with_unit, Utils.formatCurrency(dailyBudget)));
    }
    

    private void setupHeaderActions() {
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.label_add);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> showAddIncomeBottomSheet());
    }

    private void updateUI() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        
        int daysLeft = manager.getDaysUntilNextPayday();
        getBinding().tvDaysLeft.setText(String.format(Locale.getDefault(), "%d", daysLeft));
        
        Calendar nextPayday = manager.getNextPayday();
        getBinding().tvTargetDate.setText(Utils.formatDateShort(nextPayday));
        
        transactionRepository.refreshTransactions();
    }

    private void setupListeners() {
        getBinding().cardNextPayday.setOnClickListener(v -> showDatePicker());
        getBinding().cardNextPayday.setOnLongClickListener(v -> {
            showPaydaySettings();
            return true;
        });
    }

    private void showPaydaySettings() {
        PaydayEditBottomSheet bottomSheet = PaydayEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "PaydayEditBottomSheet");
    }

    private void showDatePicker() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        Calendar nextPayday = manager.getNextPayday();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    if (manager.isMonthly()) {
                        manager.setFirstPayday(dayOfMonth);
                    } else {
                        int currentNextDay = nextPayday.get(Calendar.DAY_OF_MONTH);
                        int first = manager.getFirstPayday();
                        int second = manager.getSecondPayday();
                        
                        if (Math.abs(currentNextDay - first) <= Math.abs(currentNextDay - second)) {
                            manager.setFirstPayday(dayOfMonth);
                        } else {
                            manager.setSecondPayday(dayOfMonth);
                        }
                    }
                    updateUI();
                },
                nextPayday.get(Calendar.YEAR),
                nextPayday.get(Calendar.MONTH),
                nextPayday.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showAddIncomeBottomSheet() {
        IncomeEditBottomSheet bottomSheet = IncomeEditBottomSheet.newInstance(IncomeEditBottomSheet.Mode.ADD_INCOME);
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "IncomeAddBottomSheet");
    }
}
