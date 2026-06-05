package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseMultiItem;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.*;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import java.util.List;
import java.util.Objects;

public interface HomeListItem extends BaseMultiItem {
    interface OnHomeInteractionListener {
        void onBudgetPromptClick();
        void onBillClick(Bill bill);
        void onActionClick(QuickLogItem item);
        void onQuickLogClick(QuickLogItem item);
        void onQuickLogLongClick(QuickLogItem item);
        void onTransactionClick(TransactionWithUser item);
        void onTransactionLike(TransactionWithUser item);
        void onTransactionReaction(TransactionWithUser item, String emoji);
        void onTransactionComment(TransactionWithUser item);
        void onViewLikes(TransactionWithUser item);
        void onSeeAllTransactions();
    }

    int TYPE_DASHBOARD = 1;
    int TYPE_BUDGET_PROMPT = 2;
    int TYPE_SECTION_HEADER = 3;
    int TYPE_BILL_CONTAINER = 4;
    int TYPE_QUICK_ACTIONS = 5;
    int TYPE_QUICK_LOG = 6;
    int TYPE_TRANSACTION = 7;
    int TYPE_EMPTY_STATE = 8;
    int TYPE_AI_INSIGHT = 9;

    @Override default int getViewType() { return 0; }
    @Override default String getItemUniqueId() { return ""; }

    record DashboardItem(HomeViewModel.DashboardState state) implements HomeListItem {
        @Override public int getViewType() { return TYPE_DASHBOARD; }
        @Override public String getItemUniqueId() { return "DASHBOARD"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewHomeDashboardBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewHomeDashboardBinding b = (ViewHomeDashboardBinding) binding;
            b.tvBalanceAmount.setText(Utils.formatCurrency(state.balance()));
            b.tvIncomeAmount.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyIncome()));
            b.tvExpensesAmount.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyExpense()));
            b.tvTotalSavings.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlySavings()));
        }

        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            if (newItem instanceof DashboardItem other) {
                Bundle diff = new Bundle();
                if (state.balance() != other.state.balance()) diff.putDouble("balance", other.state.balance());
                if (state.monthlyIncome() != other.state.monthlyIncome()) diff.putDouble("income", other.state.monthlyIncome());
                if (state.monthlyExpense() != other.state.monthlyExpense()) diff.putDouble("expense", other.state.monthlyExpense());
                if (state.monthlySavings() != other.state.monthlySavings()) diff.putDouble("savings", other.state.monthlySavings());
                return !diff.isEmpty() ? diff : null;
            }
            return null;
        }

        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            for (Object p : payloads) {
                if (p instanceof Bundle b) {
                    ViewHomeDashboardBinding vb = (ViewHomeDashboardBinding) binding;
                    if (b.containsKey("balance")) vb.tvBalanceAmount.setText(Utils.formatCurrency(b.getDouble("balance")));
                    if (b.containsKey("income")) vb.tvIncomeAmount.setText(vb.getRoot().getContext().getString(R.string.format_currency_no_decimal, b.getDouble("income")));
                    if (b.containsKey("expense")) vb.tvExpensesAmount.setText(vb.getRoot().getContext().getString(R.string.format_currency_no_decimal, b.getDouble("expense")));
                    if (b.containsKey("savings")) vb.tvTotalSavings.setText(vb.getRoot().getContext().getString(R.string.format_currency_no_decimal, b.getDouble("savings")));
                }
            }
        }
    }

    record AIInsightItem(String message) implements HomeListItem {
        @Override public int getViewType() { return TYPE_AI_INSIGHT; }
        @Override public String getItemUniqueId() { return "AI_INSIGHT"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewAiInsightBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ((ViewAiInsightBinding) binding).tvInsightMessage.setText(message);
        }
    }

    record BudgetPromptItem(Runnable onClick) implements HomeListItem {
        @Override public int getViewType() { return TYPE_BUDGET_PROMPT; }
        @Override public String getItemUniqueId() { return "BUDGET_PROMPT"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewBudgetPlanPromptBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ((ViewBudgetPlanPromptBinding) binding).btnSetNow.setOnClickListener(v -> onClick.run());
        }
    }

    record SectionHeaderItem(String title, String subtitle, boolean showAction, String actionText, Runnable onAction) implements HomeListItem {
        @Override public int getViewType() { return TYPE_SECTION_HEADER; }
        @Override public String getItemUniqueId() { return "HEADER_" + title; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewSectionHeaderBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewSectionHeaderBinding b = (ViewSectionHeaderBinding) binding;
            b.tvSectionTitle.setText(title);
            b.tvSectionSubTitle.setVisibility(subtitle != null ? View.VISIBLE : View.GONE);
            if (subtitle != null) b.tvSectionSubTitle.setText(subtitle);
            b.btnSectionAction.setVisibility(showAction ? View.VISIBLE : View.GONE);
            if (actionText != null) b.btnSectionAction.setText(actionText);
            b.btnSectionAction.setOnClickListener(v -> { if (onAction != null) onAction.run(); });
        }
    }

    record BillContainerItem(List<Bill> bills, OnHomeInteractionListener listener, RecyclerView.RecycledViewPool pool) implements HomeListItem {
        @Override public int getViewType() { return TYPE_BILL_CONTAINER; }
        @Override public String getItemUniqueId() { return "BILL_CONTAINER"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewHomeNestedRvBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewHomeNestedRvBinding b = (ViewHomeNestedRvBinding) binding;
            RecyclerView rv = b.nestedRecyclerView;
            if (rv.getLayoutManager() == null) {
                LinearLayoutManager lm = new LinearLayoutManager(rv.getContext(), LinearLayoutManager.HORIZONTAL, false);
                lm.setInitialPrefetchItemCount(3);
                rv.setLayoutManager(lm);
                rv.setHasFixedSize(true);
                rv.setRecycledViewPool(pool);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<Bill, ItemBillDueCardBinding> existingAdapter = (GenericAdapter<Bill, ItemBillDueCardBinding>) rv.getAdapter();
            if (existingAdapter == null) {
                GenericAdapter<Bill, ItemBillDueCardBinding> newAdapter = new GenericAdapter<>(Bill.DIFF_CALLBACK, (i, p) -> ItemBillDueCardBinding.inflate(i, p, false), (bind, bill, p, c) -> {
                    bind.tvBillName.setText(bill.getName());
                    String dateText = bill.getDate();
                    if (dateText != null && !dateText.isEmpty()) {
                        bind.tvDueDate.setText(bind.getRoot().getContext().getString(R.string.add_bill_at_date, dateText).replace("Add Bill for ", "Due "));
                    } else {
                        bind.tvDueDate.setText(bill.getStatus());
                    }
                    bind.tvStatus.setText(Utils.formatCurrency(bill.getAmount()));
                    bind.getRoot().setOnClickListener(v -> listener.onBillClick(bill));
                });
                rv.setAdapter(newAdapter);
                newAdapter.submitList(bills);
            } else if (!Objects.equals(existingAdapter.getCurrentList(), bills)) {
                existingAdapter.submitList(bills);
            }
        }
    }

    record QuickActionsItem(List<QuickLogItem> actions, OnHomeInteractionListener listener, RecyclerView.RecycledViewPool pool) implements HomeListItem {
        @Override public int getViewType() { return TYPE_QUICK_ACTIONS; }
        @Override public String getItemUniqueId() { return "QUICK_ACTIONS"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewHomeNestedRvBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewHomeNestedRvBinding b = (ViewHomeNestedRvBinding) binding;
            RecyclerView rv = b.nestedRecyclerView;
            if (rv.getLayoutManager() == null) {
                LinearLayoutManager lm = new LinearLayoutManager(rv.getContext(), LinearLayoutManager.HORIZONTAL, false);
                lm.setInitialPrefetchItemCount(4);
                rv.setLayoutManager(lm);
                rv.setHasFixedSize(true);
                rv.setRecycledViewPool(pool);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<QuickLogItem, ItemQuickActionBinding> existingAdapter = (GenericAdapter<QuickLogItem, ItemQuickActionBinding>) rv.getAdapter();
            if (existingAdapter == null) {
                GenericAdapter<QuickLogItem, ItemQuickActionBinding> newAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickActionBinding.inflate(i, p, false), (bind, item, p, c) -> {
                    bind.tvLabel.setText(item.getTitle()); bind.ivIcon.setImageResource(item.getIconRes());
                    bind.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(bind.getRoot().getContext(), item.getBgColor()));
                    bind.ivIcon.setColorFilter(ContextCompat.getColor(bind.getRoot().getContext(), item.getIconColor()));
                });
                newAdapter.setOnItemClickListener(listener::onActionClick);
                rv.setAdapter(newAdapter);
                newAdapter.submitList(actions);
            } else if (!Objects.equals(existingAdapter.getCurrentList(), actions)) {
                existingAdapter.submitList(actions);
            }
        }
    }

    record QuickLogItemContainer(List<QuickLogItem> logs, OnHomeInteractionListener listener, RecyclerView.RecycledViewPool pool) implements HomeListItem {
        @Override public int getViewType() { return TYPE_QUICK_LOG; }
        @Override public String getItemUniqueId() { return "QUICK_LOG"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewHomeNestedRvBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewHomeNestedRvBinding b = (ViewHomeNestedRvBinding) binding;
            RecyclerView rv = b.nestedRecyclerView;
            if (rv.getLayoutManager() == null) {
                GridLayoutManager gm = new GridLayoutManager(rv.getContext(), 4);
                gm.setInitialPrefetchItemCount(8);
                rv.setLayoutManager(gm);
                rv.setHasFixedSize(true);
                rv.setRecycledViewPool(pool);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<QuickLogItem, ItemQuickLogBinding> existingAdapter = (GenericAdapter<QuickLogItem, ItemQuickLogBinding>) rv.getAdapter();
            if (existingAdapter == null) {
                GenericAdapter<QuickLogItem, ItemQuickLogBinding> newAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickLogBinding.inflate(i, p, false), (bind, item, p, c) -> {
                    bind.tvLabel.setText(item.getTitle()); bind.ivIcon.setImageResource(item.getIconRes());
                    int color = ContextCompat.getColor(bind.getRoot().getContext(), item.getIconColor());
                    bind.cvIconBg.setCardBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(color, 25));
                    bind.ivIcon.setColorFilter(color);
                });
                newAdapter.setOnItemClickListener(listener::onQuickLogClick);
                newAdapter.setOnItemLongClickListener(log -> { listener.onQuickLogLongClick(log); return true; });
                rv.setAdapter(newAdapter);
                newAdapter.submitList(logs);
            } else if (!Objects.equals(existingAdapter.getCurrentList(), logs)) {
                existingAdapter.submitList(logs);
            }
        }
    }

    record TransactionItem(TransactionWithUser transaction, OnHomeInteractionListener listener) implements HomeListItem {
        @Override public int getViewType() { return TYPE_TRANSACTION; }
        @Override public String getItemUniqueId() { return "TRANS_" + transaction.getTransaction().getId(); }
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

            if (transaction.getUserAvatarUrl() != null && !transaction.getUserAvatarUrl().isEmpty()) {
                Glide.with(b.getRoot().getContext()).load(transaction.getUserAvatarUrl()).placeholder(R.drawable.ai_holder).into(b.ivAvatar);
            } else if (transaction.getUserAvatarRes() != 0) {
                b.ivAvatar.setImageResource(transaction.getUserAvatarRes());
            } else {
                b.ivAvatar.setImageResource(R.drawable.ai_holder);
            }

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
                b.layoutReactionsSummary.setVisibility(View.VISIBLE);
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
                b.layoutReactionsSummary.setVisibility(View.GONE);
                b.divider.setVisibility(View.GONE);
            }

            b.btnLike.setOnClickListener(v -> listener.onTransactionLike(transaction));
            b.btnLike.setOnLongClickListener(v -> {
                com.upreyvan.carti.util.DialogHelper.showEmojiPicker(b.btnLike, emoji ->
                    listener.onTransactionReaction(transaction, emoji));
                return true;
            });
            
            b.layoutReactionsSummary.setOnClickListener(v -> listener.onViewLikes(transaction));
            b.tvLikesCount.setOnClickListener(v -> listener.onViewLikes(transaction));
            b.tvCommentsCountSummary.setOnClickListener(v -> listener.onTransactionComment(transaction));

            b.btnComment.setOnClickListener(v -> listener.onTransactionComment(transaction));
            b.getRoot().setOnClickListener(v -> listener.onTransactionClick(transaction));
        }

        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            if (newItem instanceof TransactionItem other) {
                Bundle diff = new Bundle();
                Transaction t = transaction.getTransaction();
                Transaction ot = other.transaction.getTransaction();
                if (t.getLikesCount() != ot.getLikesCount()) diff.putInt("likes", ot.getLikesCount());
                if (t.getCommentCount() != ot.getCommentCount()) diff.putInt("comments", ot.getCommentCount());
                if (!Objects.equals(transaction.getMyReaction(), other.transaction.getMyReaction())) diff.putString("myReaction", other.transaction.getMyReaction());
                return !diff.isEmpty() ? diff : null;
            }
            return null;
        }

        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            for (Object p : payloads) {
                if (p instanceof Bundle b) {
                    ItemTransactionBinding vb = (ItemTransactionBinding) binding;
                    if (b.containsKey("likes")) {
                        int likes = b.getInt("likes");
                        vb.tvLikesCount.setText(String.valueOf(likes));
                        vb.tvLikesCount.setVisibility(likes > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (b.containsKey("myReaction")) {
                        String reaction = b.getString("myReaction");
                        vb.tvBtnLikeIcon.setText(reaction != null && !reaction.isEmpty() ? reaction : "👍");
                        vb.tvBtnLikeText.setTextColor(ContextCompat.getColor(vb.getRoot().getContext(), 
                            reaction != null && !reaction.isEmpty() ? R.color.carti_primary_green : R.color.text_secondary));
                    }
                }
            }
        }
    }

    record EmptyStateItem(String message) implements HomeListItem {
        @Override public int getViewType() { return TYPE_EMPTY_STATE; }
        @Override public String getItemUniqueId() { return "EMPTY_" + message; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewSectionHeaderBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewSectionHeaderBinding b = (ViewSectionHeaderBinding) binding;
            b.tvSectionTitle.setText(message);
            b.tvSectionTitle.setGravity(android.view.Gravity.CENTER);
            b.btnSectionAction.setVisibility(View.GONE);
        }
    }
}
