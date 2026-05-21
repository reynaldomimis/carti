package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentIncomeContributorsBinding;
import com.upreyvan.carti.databinding.ItemMemberContributionBinding;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IncomeContributorsFragment extends BaseFragment<FragmentIncomeContributorsBinding> {

    private static final String ARG_START_MILLIS = "start_millis";
    private static final String ARG_END_MILLIS = "end_millis";

    private TransactionRepository transactionRepository;
    private GenericAdapter<Contributor, ItemMemberContributionBinding> adapter;
    private long startMillis;
    private long endMillis;

    public static IncomeContributorsFragment newInstance(long startMillis, long endMillis) {
        IncomeContributorsFragment fragment = new IncomeContributorsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_START_MILLIS, startMillis);
        args.putLong(ARG_END_MILLIS, endMillis);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            startMillis = getArguments().getLong(ARG_START_MILLIS);
            endMillis = getArguments().getLong(ARG_END_MILLIS);
        }
    }

    @Override
    protected FragmentIncomeContributorsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentIncomeContributorsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        
        setupToolbar(getBinding().layoutToolbar, "Expenses Contributors");
        setupRecyclerView();
        observeContributions();
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Contributor.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberContributionBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvName.setText(item.name);
                    binding.tvContributionLabel.setText(String.format("Profile Contribution: %s", Utils.formatCurrency(item.amount)));

                    if (item.avatarUrl != null && !item.avatarUrl.isEmpty()) {
                        com.bumptech.glide.Glide.with(requireContext())
                                .load(item.avatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(binding.ivAvatar);
                    } else {
                        binding.ivAvatar.setImageResource(R.drawable.ic_person);
                    }

                    binding.tvBtnViewExpenses.setOnClickListener(v -> 
                        navigateTo(BreakdownExpenseFragment.newInstance(startMillis, endMillis))
                    );
                    
                    binding.getRoot().setOnClickListener(v -> 
                        navigateTo(AllTransactionsFragment.newInstance("INCOME", item.userId, item.name))
                    );
                }
        );
        getBinding().rvContributors.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvContributors.setAdapter(adapter);
    }

    private void observeContributions() {
        transactionRepository.getTransactionsInRange(startMillis, endMillis).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                Map<String, Contributor> contributorMap = new HashMap<>();
                double totalIncome = 0;
                double totalExpense = 0;
                
                for (TransactionWithUser tWithU : transactions) {
                    double amount = tWithU.getTransaction().getAmount();
                    if ("INCOME".equals(tWithU.getTransaction().getType())) {
                        totalIncome += amount;
                        
                        String userId = tWithU.getTransaction().getUserId();
                        String userName = ValueHelper.toStr(tWithU.getUserName() != null ? tWithU.getUserName() : tWithU.getTransaction().getTitle());
                        String avatarUrl = tWithU.getUserAvatarUrl();

                        if (contributorMap.containsKey(userId)) {
                            contributorMap.get(userId).amount += amount;
                        } else {
                            contributorMap.put(userId, new Contributor(userId, userName, avatarUrl, amount));
                        }
                    } else if ("EXPENSE".equals(tWithU.getTransaction().getType())) {
                        totalExpense += amount;
                    }
                }

                getBinding().tvTotalBalanceAmount.setText(Utils.formatCurrency(totalIncome - totalExpense));

                List<Contributor> contributors = new ArrayList<>(contributorMap.values());
                contributors.sort((c1, c2) -> Double.compare(c2.amount, c1.amount));
                adapter.submitList(contributors);
                
                boolean isEmpty = contributors.isEmpty();
                getBinding().rvContributors.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private static class Contributor {
        String userId;
        String name;
        String avatarUrl;
        double amount;

        Contributor(String userId, String name, String avatarUrl, double amount) {
            this.userId = userId;
            this.name = name;
            this.avatarUrl = avatarUrl;
            this.amount = amount;
        }

        static final androidx.recyclerview.widget.DiffUtil.ItemCallback<Contributor> DIFF_CALLBACK = 
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<Contributor>() {
                @Override
                public boolean areItemsTheSame(@NonNull Contributor oldItem, @NonNull Contributor newItem) {
                    return oldItem.userId.equals(newItem.userId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Contributor oldItem, @NonNull Contributor newItem) {
                    return oldItem.name.equals(newItem.name) && oldItem.amount == newItem.amount;
                }
            };
    }
}