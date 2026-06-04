package com.upreyvan.carti.ui.track;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseMultiItem;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.*;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.TrackCategory;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public interface TrackListItem extends BaseMultiItem {
    interface OnTrackInteractionListener {
        void onViewDetails();
        void onTotalBalanceClick();
        void onToggleAllocation();
        void onSeeAllTransactions();
        void onTransactionLike(TransactionWithUser item);
        void onTransactionComment(TransactionWithUser item);
        void onTransactionClick(TransactionWithUser item);
        Category findCategory(String name);
        List<BudgetCategoryItem> getAllocations();
    }

    int TYPE_SUMMARY = 0;
    int TYPE_CHART = 1;
    int TYPE_COMPARISON = 2;
    int TYPE_ALLOCATION_HEADER = 3;
    int TYPE_SECTION_HEADER = 5;
    int TYPE_TRANSACTION = 6;

    @Override default int getViewType() { return 0; }
    @Override default String getItemUniqueId() { return ""; }

    record SummaryItem(double balance, double income, double expense, double saved, double target, int progress, OnTrackInteractionListener listener) implements TrackListItem {
        @Override public int getViewType() { return TYPE_SUMMARY; }
        @Override public String getItemUniqueId() { return "summary"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewTrackSummaryBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewTrackSummaryBinding b = (ViewTrackSummaryBinding) binding;

            if (Objects.equals(b.getRoot().getTag(R.id.item_tag_id), this)) return;
            b.getRoot().setTag(R.id.item_tag_id, this);

            b.tvTotalBalance.setText(Utils.formatCurrency(balance));
            b.tvSummaryIncome.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal_simple, income));
            b.tvSummaryExpense.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal_simple, expense));
            b.tvSavingsAmount.setText(String.format(Locale.getDefault(), "%s / %s", Utils.formatCurrency(saved), Utils.formatCurrency(target)));
            b.tvSavingsPercent.setText(b.getRoot().getContext().getString(R.string.percentage_format, progress));
            b.progressSavings.setProgress(progress);
            b.btnViewDetails.setOnClickListener(v -> listener.onViewDetails());
            b.cardTotalBalance.setOnClickListener(v -> listener.onTotalBalanceClick());
        }

        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            if (newItem instanceof SummaryItem other) {
                Bundle diff = new Bundle();
                if (balance != other.balance) diff.putDouble("balance", other.balance);
                if (income != other.income) diff.putDouble("income", other.income);
                if (expense != other.expense) diff.putDouble("expense", other.expense);
                if (saved != other.saved) diff.putDouble("saved", other.saved);
                if (target != other.target) diff.putDouble("target", other.target);
                if (progress != other.progress) diff.putInt("progress", other.progress);
                return !diff.isEmpty() ? diff : null;
            }
            return null;
        }

        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            for (Object p : payloads) {
                if (p instanceof Bundle b) {
                    ViewTrackSummaryBinding vb = (ViewTrackSummaryBinding) binding;
                    if (b.containsKey("balance")) vb.tvTotalBalance.setText(Utils.formatCurrency(b.getDouble("balance")));
                    if (b.containsKey("income")) vb.tvSummaryIncome.setText(vb.getRoot().getContext().getString(R.string.format_currency_no_decimal_simple, b.getDouble("income")));
                    if (b.containsKey("expense")) vb.tvSummaryExpense.setText(vb.getRoot().getContext().getString(R.string.format_currency_no_decimal_simple, b.getDouble("expense")));
                    if (b.containsKey("saved") || b.containsKey("target")) {
                        double s = b.containsKey("saved") ? b.getDouble("saved") : saved;
                        double t = b.containsKey("target") ? b.getDouble("target") : target;
                        vb.tvSavingsAmount.setText(String.format(Locale.getDefault(), "%s / %s", Utils.formatCurrency(s), Utils.formatCurrency(t)));
                    }
                    if (b.containsKey("progress")) {
                        int pr = b.getInt("progress");
                        vb.tvSavingsPercent.setText(vb.getRoot().getContext().getString(R.string.percentage_format, pr));
                        vb.progressSavings.setProgress(pr);
                    }
                }
            }
        }
    }

    record ChartItem(List<PieEntryData> entries, List<Integer> colors, List<TrackCategory> legend, double total) implements TrackListItem {
        @Override public int getViewType() { return TYPE_CHART; }
        @Override public String getItemUniqueId() { return "chart"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewTrackChartBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewTrackChartBinding b = (ViewTrackChartBinding) binding;
            PieChart pc = b.pieChart;
            
            if (Objects.equals(pc.getTag(R.id.item_tag_id), this)) return;
            pc.setTag(R.id.item_tag_id, this);

            if (pc.getData() == null) {
                pc.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                pc.setUsePercentValues(true); pc.getDescription().setEnabled(false); pc.setDrawEntryLabels(false);
                pc.setDrawHoleEnabled(true); pc.setHoleColor(ContextCompat.getColor(pc.getContext(), android.R.color.transparent));
                pc.setTransparentCircleRadius(61f); pc.setHoleRadius(85f); pc.setDrawCenterText(true);
                pc.getLegend().setEnabled(false); pc.setRotationEnabled(true); pc.setHighlightPerTapEnabled(true);
            }
            
            List<PieEntry> pieEntries = new ArrayList<>();
            for (PieEntryData d : entries) pieEntries.add(new PieEntry(d.value, d.label));
            PieDataSet ds = new PieDataSet(pieEntries, "");
            ds.setColors(colors); ds.setDrawValues(false); ds.setSliceSpace(2f);
            pc.setData(new PieData(ds));
            pc.setCenterText("Total\n" + Utils.formatCurrency(total));
            pc.setCenterTextSize(14f); pc.setCenterTextColor(ContextCompat.getColor(pc.getContext(), R.color.text_primary));
            pc.setCenterTextTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            pc.invalidate();

            RecyclerView rv = b.rvLegend;
            if (rv.getLayoutManager() == null) {
                LinearLayoutManager lm = new LinearLayoutManager(rv.getContext());
                lm.setInitialPrefetchItemCount(4);
                rv.setLayoutManager(lm);
                rv.setHasFixedSize(true);
                rv.setNestedScrollingEnabled(false);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<TrackCategory, ItemLegendTrackBinding> adapter = (GenericAdapter<TrackCategory, ItemLegendTrackBinding>) rv.getAdapter();
            if (adapter == null) {
                adapter = new GenericAdapter<>(TrackCategory.DIFF_CALLBACK, (i, p) -> ItemLegendTrackBinding.inflate(i, p, false), (bind, it, p, c) -> {
                    bind.viewColor.setBackgroundTintList(ColorStateList.valueOf(it.getColor()));
                    bind.tvCategory.setText(ValueHelper.toStr(it.getName()));
                    bind.tvPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", it.getPercentage()));
                });
                rv.setAdapter(adapter);
            }
            if (!Objects.equals(adapter.getCurrentList(), legend)) {
                adapter.submitList(legend);
            }
        }
    }

    record PieEntryData(float value, String label) {}

    record ComparisonItem(double income, double expense) implements TrackListItem {
        @Override public int getViewType() { return TYPE_COMPARISON; }
        @Override public String getItemUniqueId() { return "comparison"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewTrackComparisonBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewTrackComparisonBinding b = (ViewTrackComparisonBinding) binding;
            b.tvIncomeAmountComp.setText(Utils.formatCurrency(income));
            b.tvExpenseAmountComp.setText(Utils.formatCurrency(expense));
            float totalMax = Math.max(1.0f, (float) Math.max(income, expense));
            int maxH = Utils.dpToPx(b.getRoot().getContext(), 120);
            ViewGroup.LayoutParams lpI = b.barIncome.getLayoutParams();
            lpI.height = (int) ((income / totalMax) * maxH); b.barIncome.setLayoutParams(lpI);
            ViewGroup.LayoutParams lpE = b.barExpense.getLayoutParams();
            lpE.height = (int) ((expense / totalMax) * maxH); b.barExpense.setLayoutParams(lpE);
        }
    }

    record AllocationHeaderItem(double totalAllocation, boolean isExpanded, int listSize, OnTrackInteractionListener listener, RecyclerView.RecycledViewPool pool) implements TrackListItem {
        @Override public int getViewType() { return TYPE_ALLOCATION_HEADER; }
        @Override public String getItemUniqueId() { return "allocation_header"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewTrackAllocationCardBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewTrackAllocationCardBinding b = (ViewTrackAllocationCardBinding) binding;
            
            if (Objects.equals(b.getRoot().getTag(R.id.item_tag_id), this)) return;
            b.getRoot().setTag(R.id.item_tag_id, this);

            b.tvTotalAllocation.setText(b.getRoot().getContext().getString(R.string.label_total_allocation, Utils.formatCurrency(totalAllocation)));
            b.tvViewAllAllocationLabel.setText(isExpanded ? R.string.see_less : R.string.see_all);
            
            float targetRot = isExpanded ? 90f : 0f;
            if (b.ivAllocationArrow.getRotation() != targetRot) {
                b.ivAllocationArrow.animate().rotation(targetRot).setDuration(200).start();
            }

            b.btnViewAllAllocation.setOnClickListener(v -> listener.onToggleAllocation());

            RecyclerView rv = b.rvBudgetAllocation;
            if (rv.getLayoutManager() == null) {
                LinearLayoutManager lm = new LinearLayoutManager(rv.getContext());
                lm.setInitialPrefetchItemCount(3);
                rv.setLayoutManager(lm);
                rv.setHasFixedSize(true);
                rv.setNestedScrollingEnabled(false);
                rv.setRecycledViewPool(pool);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<BudgetCategoryItem, ItemBudgetCategoryBinding> adapter = (GenericAdapter<BudgetCategoryItem, ItemBudgetCategoryBinding>) rv.getAdapter();
            List<BudgetCategoryItem> allocations = listener.getAllocations();
            if (adapter == null) {
                adapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK, (i, p) -> ItemBudgetCategoryBinding.inflate(i, p, false), (bind, it, p, c) -> {
                    bind.tvCategoryName.setText(it.getCategoryName()); bind.tvAmount.setText(Utils.formatCurrency(it.getAmount()));
                    bind.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", it.getPercentage()));
                    bind.pbBudget.setProgress(it.getPercentage());
                    Category cat = listener.findCategory(it.getCategoryName());
                    if (cat != null) {
                        bind.ivIcon.setImageResource(cat.getIconRes());
                        bind.ivIcon.setColorFilter(ContextCompat.getColor(bind.getRoot().getContext(), cat.getIconColor()));
                        bind.cvIcon.setCardBackgroundColor(ContextCompat.getColor(bind.getRoot().getContext(), cat.getBackgroundColor()));
                    } else {
                        bind.ivIcon.setImageResource(it.getIconRes());
                        bind.ivIcon.setColorFilter(ContextCompat.getColor(bind.getRoot().getContext(), it.getIconColor()));
                        bind.cvIcon.setCardBackgroundColor(ContextCompat.getColor(bind.getRoot().getContext(), it.getBgColor()));
                    }
                });
                rv.setAdapter(adapter);
            }
            if (adapter.getCurrentList() != allocations) {
                adapter.submitList(new ArrayList<>(allocations));
            }
        }
    }

    record SectionHeaderItem(String title, String actionText, OnTrackInteractionListener listener) implements TrackListItem {
        @Override public int getViewType() { return TYPE_SECTION_HEADER; }
        @Override public String getItemUniqueId() { return "header_" + title; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewSectionHeaderBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewSectionHeaderBinding b = (ViewSectionHeaderBinding) binding;
            b.tvSectionTitle.setText(title);
            b.btnSectionAction.setText(actionText);
            b.btnSectionAction.setVisibility(View.VISIBLE);
            b.btnSectionAction.setOnClickListener(v -> listener.onSeeAllTransactions());
        }
    }

    record TransactionItem(TransactionWithUser transaction, OnTrackInteractionListener listener) implements TrackListItem {
        @Override public int getViewType() { return TYPE_TRANSACTION; }
        @Override public String getItemUniqueId() { return transaction.getTransaction().getId(); }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ItemTransactionBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ItemTransactionBinding b = (ItemTransactionBinding) binding;

            if (Objects.equals(b.getRoot().getTag(R.id.item_tag_id), this)) return;
            b.getRoot().setTag(R.id.item_tag_id, this);

            Transaction t = transaction.getTransaction();
            b.tvUserAction.setText(transaction.getUsername());
            b.tvTimestamp.setText(Utils.getTimeAgo(t.getTimestampMillis()));
            b.tvTitle.setText(t.getCategory()); 
            
            String amountStr = Utils.formatCurrency(t.getAmount());
            if ("INCOME".equals(t.getType())) {
                b.tvAmount.setText(b.getRoot().getContext().getString(R.string.format_income, amountStr));
                b.tvAmount.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.green_primary));
            } else {
                b.tvAmount.setText(b.getRoot().getContext().getString(R.string.format_expense, amountStr));
                b.tvAmount.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
            }

            String note = t.getNote();
            if (note != null && !note.isEmpty()) {
                b.tvDescription.setText(note.replace("Logged by Carti AI", "").trim());
                b.tvDescription.setVisibility(View.VISIBLE);
            } else {
                b.tvDescription.setVisibility(View.GONE);
            }
            
            if (transaction.getUserAvatarUrl() != null && !transaction.getUserAvatarUrl().isEmpty()) {
                Glide.with(b.getRoot().getContext()).load(transaction.getUserAvatarUrl()).placeholder(R.drawable.ai_holder).error(R.drawable.ai_holder).into(b.ivAvatar);
            } else if (transaction.getUserAvatarRes() != 0) {
                b.ivAvatar.setImageResource(transaction.getUserAvatarRes());
            } else {
                b.ivAvatar.setImageResource(R.drawable.ai_holder);
            }

            b.btnLike.setOnClickListener(v -> listener.onTransactionLike(transaction));
            b.btnComment.setOnClickListener(v -> listener.onTransactionComment(transaction));
            b.getRoot().setOnClickListener(v -> listener.onTransactionClick(transaction));
        }
    }
}
