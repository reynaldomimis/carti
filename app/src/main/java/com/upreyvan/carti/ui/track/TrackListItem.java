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
import com.upreyvan.carti.util.AvatarHelper;
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public interface TrackListItem extends BaseMultiItem {
    interface OnTrackInteractionListener {
        void onToggleAllocation();
        void onSeeAllTransactions();
        void onCategoryClick(String categoryName);
        void onTransactionLike(TransactionWithUser item);
        void onTransactionReaction(TransactionWithUser item, String emoji);
        void onTransactionComment(TransactionWithUser item);
        void onViewLikes(TransactionWithUser item);
        void onTransactionClick(TransactionWithUser item);
        void onTransactionEdit(TransactionWithUser item);
        void onTransactionDelete(TransactionWithUser item);
        String getCurrentUserId();
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
                    bind.tvCategoryName.setText(it.getCategoryName());
                    
                    String spentStr = Utils.formatCompactCurrency(it.getCurrentSpent());
                    String budgetStr = Utils.formatCompactCurrency(it.getAmount());
                    bind.tvAmount.setText(String.format("%s / %s", spentStr, budgetStr));
                    
                    int percent = it.getAmount() > 0 ? (int) ((it.getCurrentSpent() / it.getAmount()) * 100) : 0;
                    bind.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
                    bind.pbBudget.setProgress(Math.min(100, percent));

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

                    bind.getRoot().setOnClickListener(v -> listener.onCategoryClick(it.getCategoryName()));
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
            String username = transaction.getUsername() != null && !transaction.getUsername().isEmpty() ? transaction.getUsername() : "Someone";
            String type = t.getType() != null ? t.getType().toUpperCase() : "EXPENSE";
            
            int amountColor;
            int amountFormatRes;
            String actionLabel;

            switch (type) {
                case "INCOME" -> {
                    actionLabel = "Received income";
                    amountColor = R.color.green_primary;
                    amountFormatRes = R.string.format_income;
                }
                case "DEBT" -> {
                    actionLabel = "Recorded a debt";
                    amountColor = R.color.status_red;
                    amountFormatRes = R.string.format_expense;
                }
                case "GOAL", "GOAL_FUNDS" -> {
                    actionLabel = type.equals("GOAL") ? "Started a goal" : "Added funds to goal";
                    amountColor = R.color.carti_primary_green;
                    amountFormatRes = R.string.format_income;
                }
                case "BILL" -> {
                    actionLabel = "Settled a bill";
                    amountColor = R.color.status_red;
                    amountFormatRes = R.string.format_expense;
                }
                case "ALLOCATION" -> {
                    actionLabel = "Planned budget set";
                    amountColor = R.color.carti_primary_green;
                    amountFormatRes = R.string.format_income;
                }
                default -> {
                    actionLabel = "Added an expense";
                    amountColor = R.color.status_red;
                    amountFormatRes = R.string.format_expense;
                }
            }

            b.tvUserAction.setText(username);
            b.tvTimestamp.setText(Utils.getTimeAgo(t.getTimestampMillis()));
            b.tvTitle.setText(t.getTitle() != null ? t.getTitle() : t.getCategory());

            String note = t.getNote();
            if (note != null && !note.isEmpty()) {
                String filteredNote = note.replace("Logged by Carti AI", "").replace("Logged by AI", "").trim();
                b.tvDescription.setText(filteredNote.isEmpty() ? actionLabel : String.format("%s: %s", actionLabel, filteredNote));
                b.tvDescription.setVisibility(View.VISIBLE);
            } else {
                b.tvDescription.setText(actionLabel);
                b.tvDescription.setVisibility(View.VISIBLE);
            }
            
            String formattedAmount = Utils.formatCurrency(t.getAmount());
            b.tvAmount.setText(b.getRoot().getContext().getString(amountFormatRes, formattedAmount));
            b.tvAmount.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), amountColor));

            AvatarHelper.loadUserAvatar(b.getRoot().getContext(), b.ivAvatar, username, transaction.getUserAvatarUrl());

            String myReaction = transaction.getMyReaction();
            if (myReaction != null && !myReaction.isEmpty()) {
                b.tvBtnLikeIcon.setText(myReaction);
                b.tvBtnLikeText.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.carti_primary_green));
            } else {
                b.tvBtnLikeIcon.setText("👍");
                b.tvBtnLikeText.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.text_secondary));
            }

            int likesCount = t.getLikesCount();
            int commentCount = t.getCommentCount();
            String rNames = transaction.getReactorNames();

            if (likesCount > 0 || commentCount > 0) {
                b.layoutLikesSummaryClickable.setVisibility(View.VISIBLE);
                b.divider.setVisibility(View.VISIBLE);
                if (likesCount > 0) {
                    b.tvReactionEmoji.setVisibility(View.VISIBLE);
                    b.tvLikesCount.setVisibility(View.VISIBLE);
                    b.tvReactionEmoji.setText(myReaction != null && !myReaction.isEmpty() ? myReaction : "👍");
                    b.tvLikesCount.setText(rNames != null && !rNames.isEmpty() ? rNames : String.valueOf(likesCount));
                } else {
                    b.tvReactionEmoji.setVisibility(View.GONE);
                    b.tvLikesCount.setVisibility(View.GONE);
                }

                if (commentCount > 0) {
                    String commentText = commentCount == 1 ? "1 comment" : commentCount + " comments";
                    b.tvCommentsCountSummary.setText(commentText);
                    b.tvCommentsCountSummary.setVisibility(View.VISIBLE);
                } else {
                    b.tvCommentsCountSummary.setVisibility(View.GONE);
                }
            } else {
                b.layoutLikesSummaryClickable.setVisibility(View.GONE);
                b.divider.setVisibility(View.GONE);
            }

            b.btnLike.setOnClickListener(v -> listener.onTransactionLike(transaction));
            b.btnLike.setOnLongClickListener(v -> {
                DialogHelper.showEmojiPicker(b.btnLike, emoji ->
                    listener.onTransactionReaction(transaction, emoji));
                return true;
            });

            b.layoutLikesSummaryClickable.setOnClickListener(v -> listener.onViewLikes(transaction));
            b.tvLikesCount.setOnClickListener(v -> listener.onViewLikes(transaction));
            b.tvCommentsCountSummary.setOnClickListener(v -> listener.onTransactionComment(transaction));

            b.btnComment.setOnClickListener(v -> listener.onTransactionComment(transaction));
            b.getRoot().setOnClickListener(v -> listener.onTransactionClick(transaction));

            boolean isOwner = Objects.equals(t.getUserId(), listener.getCurrentUserId());
            b.btnOptions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            if (isOwner) {
                b.btnOptions.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(b.getRoot().getContext(), v);
                    popup.getMenuInflater().inflate(R.menu.menu_transaction_options, popup.getMenu());
                    popup.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_edit) {
                            listener.onTransactionEdit(transaction);
                            return true;
                        } else if (id == R.id.action_delete) {
                            DialogHelper.showConfirmation(
                                    b.getRoot().getContext(),
                                    "Delete Transaction?",
                                    "Are you sure you want to delete this transaction?",
                                    "Delete",
                                    () -> listener.onTransactionDelete(transaction)
                            );
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                });
            }
        }
    }
}
