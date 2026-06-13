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
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.QuickLogItem;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.AvatarHelper;
import com.upreyvan.carti.utils.Utils;
import java.util.List;
import java.util.Locale;
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
        void onTransactionEdit(TransactionWithUser item);
        void onTransactionDelete(TransactionWithUser item);
        String getCurrentUserId();
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
    int TYPE_SHIMMER = 10;

    @Override default int getViewType() { return 0; }
    @Override default String getItemUniqueId() { return ""; }

    record ShimmerItem(String id) implements HomeListItem {
        @Override public int getViewType() { return TYPE_SHIMMER; }
        @Override public String getItemUniqueId() { return "SHIMMER_" + id; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
            return ItemTransactionShimmerBinding.inflate(inflater, parent, false);
        }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ((ItemTransactionShimmerBinding) binding).shimmerViewContainer.startShimmer();
        }
    }

    record DashboardItem(HomeViewModel.DashboardState state) implements HomeListItem {
        @Override public int getViewType() { return TYPE_DASHBOARD; }
        @Override public String getItemUniqueId() { return "DASHBOARD"; }
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewHomeDashboardBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewHomeDashboardBinding b = (ViewHomeDashboardBinding) binding;
            b.tvBalanceAmount.setText(Utils.formatCurrency(state.balance()));
            b.tvTodayExpenseAmount.setText(Utils.formatCurrency(state.todayExpense()));
            
            // Monthly Budget
            b.tvMonthlyBudgetAmount.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyBudget()));
            b.cardMonthlyBudget.setVisibility(View.VISIBLE);
            
            // Monthly Income
            b.tvIncomeAmount.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyIncome()));
            b.cardMonthlyIncome.setVisibility(state.monthlyIncome() > 0 ? View.VISIBLE : View.GONE);
            
            // Monthly Expenses
            b.tvExpensesAmount.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyExpense()));
            b.cardMonthlyExpenses.setVisibility(View.VISIBLE);
            
            // Net Savings
            b.tvTotalSavings.setText(b.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlySavings()));
            b.cardNetSavings.setVisibility(View.VISIBLE);
        }

        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            if (newItem instanceof DashboardItem other) {
                // Use a key-based payload to signal that the data has changed
                // This prevents the RecyclerView from fully re-binding and flickering
                return "update_dashboard"; 
            }
            return null;
        }

        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                // Perform a surgical update of the UI values without re-inflating
                bind(binding, pos, count);
            }
        }
    }

    record AIInsightItem(String message, String type) implements HomeListItem {
        @Override public int getViewType() { return TYPE_AI_INSIGHT; }
        @Override public String getItemUniqueId() { return "AI_INSIGHT"; } // Use fixed ID to stabilize position
        @NonNull @Override public ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) { return ViewAiInsightBinding.inflate(inflater, parent, false); }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count) {
            ViewAiInsightBinding b = (ViewAiInsightBinding) binding;
            b.tvInsightMessage.setText(message);
            b.tvInsightTitle.setVisibility(View.GONE); // Hide "AI Insight" label as requested

            if ("OVERDUE_BILL".equals(type) || "BUDGET_WARNING".equals(type)) {
                b.ivCarti.setColorFilter(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
            } else {
                b.ivCarti.setColorFilter(null);
            }
        }
        
        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            return "update_insight";
        }
        
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) bind(binding, pos, count);
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
                rv.setRecycledViewPool(pool);
                rv.setItemAnimator(null);
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
                    String status = bill.getStatus();
                    
                    if ("Overdue".equalsIgnoreCase(status) || "Due Today".equalsIgnoreCase(status) || "Due Tomorrow".equalsIgnoreCase(status)) {
                        int labelRes = "Overdue".equalsIgnoreCase(status) ? R.string.label_overdue : 
                                     ("Due Today".equalsIgnoreCase(status) ? R.string.label_due_today : R.string.label_due_tomorrow);
                        
                        bind.tvDueDate.setText(labelRes);
                        bind.tvDueDate.setTextColor(ContextCompat.getColor(bind.getRoot().getContext(), R.color.status_red));
                        bind.tvStatus.setBackgroundResource(R.drawable.bg_status_unpaid);
                        bind.tvStatus.setTextColor(ContextCompat.getColor(bind.getRoot().getContext(), R.color.status_red));
                    } else if (dateText != null && !dateText.isEmpty()) {
                        bind.tvDueDate.setText(bind.getRoot().getContext().getString(R.string.add_bill_at_date, dateText).replace("Add Bill for ", "Due "));
                        bind.tvDueDate.setTextColor(ContextCompat.getColor(bind.getRoot().getContext(), R.color.text_secondary));
                        
                        if (bind.getRoot().getContext().getString(R.string.status_paid_label).equalsIgnoreCase(status)) {
                            bind.tvStatus.setBackgroundResource(R.drawable.bg_status_paid);
                            bind.tvStatus.setTextColor(ContextCompat.getColor(bind.getRoot().getContext(), R.color.green_primary));
                        } else {
                            bind.tvStatus.setBackgroundResource(R.drawable.bg_status_unpaid);
                            bind.tvStatus.setTextColor(ContextCompat.getColor(bind.getRoot().getContext(), R.color.status_red));
                        }
                    } else {
                        bind.tvDueDate.setText(status);
                    }
                    bind.tvStatus.setText(Utils.formatCurrency(bill.getAmount()));
                    bind.getRoot().setOnClickListener(v -> listener.onBillClick(bill));
                });
                rv.setAdapter(newAdapter);
                newAdapter.submitList(bills);
            } else {
                existingAdapter.submitList(bills);
            }
        }
        
        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) { return "update_bills"; }
        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) bind(binding, pos, count);
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
                rv.setRecycledViewPool(pool);
                rv.setItemAnimator(null);
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
            } else {
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
                rv.setRecycledViewPool(pool);
                rv.setItemAnimator(null);
            }
            setupAdapter(rv);
        }

        private void setupAdapter(RecyclerView rv) {
            @SuppressWarnings("unchecked")
            GenericAdapter<QuickLogItem, ItemQuickLogBinding> existingAdapter = (GenericAdapter<QuickLogItem, ItemQuickLogBinding>) rv.getAdapter();
            if (existingAdapter == null) {
                GenericAdapter<QuickLogItem, ItemQuickLogBinding> newAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickLogBinding.inflate(i, p, false), (bind, item, p, c) -> {
                    bind.tvLabel.setText(item.getTitle()); 
                    bind.ivIcon.setImageResource(item.getIconRes());
                    com.upreyvan.carti.utils.UiHelper.applyCategoryStyle(bind.getRoot(), bind.cvIconBg, bind.ivIcon, item.getTitle());
                });
                newAdapter.setOnItemClickListener(listener::onQuickLogClick);
                newAdapter.setOnItemLongClickListener(log -> { listener.onQuickLogLongClick(log); return true; });
                rv.setAdapter(newAdapter);
                newAdapter.submitList(logs);
            } else {
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
            
            String status = t.getStatus();
            if (Transaction.STATUS_PENDING.equals(status) || Transaction.STATUS_SYNCING.equals(status)) {
                b.getRoot().setAlpha(0.6f);
                b.tvTimestamp.setText("Sending...");
            } else {
                b.getRoot().setAlpha(1.0f);
                b.tvTimestamp.setText(Utils.getTimeAgo(t.getTimestampMillis()));
            }

            String category = t.getCategory() != null ? t.getCategory() : "Expense";
            String subCategory = t.getSubCategory();
            if (subCategory != null && !subCategory.isEmpty()) {
                b.tvTitle.setText(String.format("%s (%s)", category, subCategory));
            } else {
                b.tvTitle.setText(category);
            }

            String note = t.getNote();
            String filteredNote = "";
            if (note != null && !note.isEmpty()) {
                filteredNote = note.replace("Logged by Carti AI", "").replace("Logged by AI", "").trim();
            }
            b.tvDescription.setText(!filteredNote.isEmpty() ? filteredNote : actionLabel);
            b.tvDescription.setVisibility(View.VISIBLE);

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
                    
                    if (likesCount == 1 && rNames != null && !rNames.isEmpty()) {
                        b.tvLikesCount.setText(rNames);
                    } else {
                        b.tvLikesCount.setText(String.valueOf(likesCount));
                    }
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
                com.upreyvan.carti.utils.DialogHelper.showEmojiPicker(b.btnLike, emoji ->
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
                            com.upreyvan.carti.utils.DialogHelper.showConfirmation(
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

        @Override public Object getChangePayload(@NonNull BaseMultiItem newItem) {
            if (newItem instanceof TransactionItem other) {
                Bundle diff = new Bundle();
                Transaction t = transaction.getTransaction();
                Transaction ot = other.transaction.getTransaction();
                if (t.getLikesCount() != ot.getLikesCount()) diff.putInt("likes", ot.getLikesCount());
                if (t.getCommentCount() != ot.getCommentCount()) diff.putInt("comments", ot.getCommentCount());
                if (!Objects.equals(transaction.getMyReaction(), other.transaction.getMyReaction())) diff.putString("myReaction", other.transaction.getMyReaction());
                if (!Objects.equals(transaction.getReactorNames(), other.transaction.getReactorNames())) diff.putString("reactorNames", other.transaction.getReactorNames());
                return !diff.isEmpty() ? diff : null;
            }
            return null;
        }

        @Override public void bind(@NonNull ViewBinding binding, int pos, int count, @NonNull List<Object> payloads) {
            for (Object p : payloads) {
                if (p instanceof Bundle b) {
                    ItemTransactionBinding vb = (ItemTransactionBinding) binding;
                    Transaction t = transaction.getTransaction();
                    
                    if (b.containsKey("likes") || b.containsKey("reactorNames")) {
                        int likes = b.containsKey("likes") ? b.getInt("likes") : t.getLikesCount();
                        String names = b.containsKey("reactorNames") ? b.getString("reactorNames") : transaction.getReactorNames();
                        
                        if (likes == 1 && names != null && !names.isEmpty()) {
                            vb.tvLikesCount.setText(names);
                        } else {
                            vb.tvLikesCount.setText(String.valueOf(likes));
                        }
                        
                        vb.tvLikesCount.setVisibility(likes > 0 ? View.VISIBLE : View.GONE);
                        vb.tvReactionEmoji.setVisibility(likes > 0 ? View.VISIBLE : View.GONE);
                        vb.layoutLikesSummaryClickable.setVisibility(likes > 0 || t.getCommentCount() > 0 ? View.VISIBLE : View.GONE);
                        vb.divider.setVisibility(vb.layoutLikesSummaryClickable.getVisibility());
                    }
                    
                    if (b.containsKey("myReaction")) {
                        String reaction = b.getString("myReaction");
                        boolean hasReac = reaction != null && !reaction.isEmpty();

                        vb.tvBtnLikeIcon.setText(hasReac ? reaction : "👍");
                        vb.tvBtnLikeText.setTextColor(ContextCompat.getColor(vb.getRoot().getContext(), 
                            hasReac ? R.color.carti_primary_green : R.color.text_secondary));
                        
                        vb.tvReactionEmoji.setText(hasReac ? reaction : "👍");
                        vb.tvReactionEmoji.setVisibility(t.getLikesCount() > 0 ? View.VISIBLE : View.GONE);
                    }

                    if (b.containsKey("comments")) {
                        int comments = b.getInt("comments");
                        String commentText = comments == 1 ? "1 comment" : comments + " comments";
                        vb.tvCommentsCountSummary.setText(commentText);
                        vb.tvCommentsCountSummary.setVisibility(comments > 0 ? View.VISIBLE : View.GONE);
                        vb.layoutLikesSummaryClickable.setVisibility(comments > 0 || t.getLikesCount() > 0 ? View.VISIBLE : View.GONE);
                        vb.divider.setVisibility(vb.layoutLikesSummaryClickable.getVisibility());
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
