package com.upreyvan.carti.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private QuickLogAdapter quickLogAdapter;
    private TransactionAdapter transactionAdapter;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        setupHeaders();
        setupQuickLog();
        setupRecentTransactions();
        populateMockData();
    }


    private void setupDynamicPadding() {

        ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().layoutHeader.getRoot(),
                (v, insets) -> {

                    int statusBarHeight =
                            insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

                    int topPadding = Math.max(
                            statusBarHeight / 2,
                            Utils.dpToPx(requireContext(), 10)
                    );

                    int bottomPadding =
                            Utils.dpToPx(requireContext(), 4);

                    v.setPadding(
                            v.getPaddingLeft(),
                            topPadding,
                            v.getPaddingRight(),
                            bottomPadding
                    );

                    return insets;
                });

        ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().scrollView,
                (v, insets) -> {

                    int systemBarsBottom =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

                    int customBottomNavHeight =
                            getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

                    int extraBuffer =
                            Utils.dpToPx(requireContext(), 80);

                    v.setPadding(
                            v.getPaddingLeft(),
                            v.getPaddingTop(),
                            v.getPaddingRight(),
                            systemBarsBottom + customBottomNavHeight + extraBuffer
                    );

                    return insets;
                });
    }

    private void setupHeaders() {
        // Quick Log Header
        getBinding().headerQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().headerQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().headerQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        getBinding().headerQuickLog.btnSectionAction.setText(R.string.customize);

        // Recent Transactions Header
        getBinding().headerRecent.tvSectionTitle.setText(R.string.recent_transactions);
        getBinding().headerRecent.btnSectionAction.setText(R.string.see_all);
    }

    private void setupQuickLog() {
        List<QuickLogItem> items = new ArrayList<>();
        items.add(new QuickLogItem(getString(R.string.label_food), android.R.drawable.ic_menu_gallery, R.color.log_food, R.color.icon_food));
        items.add(new QuickLogItem(getString(R.string.label_fare), android.R.drawable.ic_dialog_map, R.color.log_fare, R.color.icon_fare));
        items.add(new QuickLogItem(getString(R.string.label_load), android.R.drawable.ic_menu_call, R.color.log_load, R.color.icon_load));
        items.add(new QuickLogItem(getString(R.string.label_store), android.R.drawable.ic_input_add, R.color.log_store, R.color.icon_store));
        items.add(new QuickLogItem(getString(R.string.label_electricity), android.R.drawable.ic_lock_power_off, R.color.log_electricity, R.color.icon_electricity));
        items.add(new QuickLogItem(getString(R.string.label_water), android.R.drawable.ic_menu_compass, R.color.log_water, R.color.icon_water));
        items.add(new QuickLogItem(getString(R.string.label_debt), android.R.drawable.ic_menu_save, R.color.log_debt, R.color.icon_debt));
        items.add(new QuickLogItem(getString(R.string.label_others), android.R.drawable.ic_menu_more, R.color.log_others, R.color.icon_others));

        quickLogAdapter = new QuickLogAdapter(items);
        getBinding().rvQuickLog.setAdapter(quickLogAdapter);
    }

    private void setupRecentTransactions() {
        transactionAdapter = new TransactionAdapter();
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

    private void populateMockData() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(
                getString(R.string.label_food),
                getString(R.string.mock_time_1),
                getString(R.string.mock_amount_120),
                android.R.drawable.ic_menu_gallery,
                ContextCompat.getColor(requireContext(), R.color.log_food)
        ));
        transactions.add(new Transaction(
                getString(R.string.label_fare),
                getString(R.string.mock_time_2),
                getString(R.string.mock_amount_15),
                android.R.drawable.ic_dialog_map,
                ContextCompat.getColor(requireContext(), R.color.log_fare)
        ));
        transactions.add(new Transaction(
                getString(R.string.label_store),
                "Kahapon • 6:15 PM",
                "₱85",
                android.R.drawable.ic_input_add,
                ContextCompat.getColor(requireContext(), R.color.log_store)
        ));

        transactionAdapter.submitList(transactions);
    }
}
