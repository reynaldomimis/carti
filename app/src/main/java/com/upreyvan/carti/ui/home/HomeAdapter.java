package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemBillDueCardBinding;
import com.upreyvan.carti.databinding.ItemQuickActionBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.databinding.ViewAiInsightBinding;
import com.upreyvan.carti.databinding.ViewBudgetPlanPromptBinding;
import com.upreyvan.carti.databinding.ViewHomeDashboardBinding;
import com.upreyvan.carti.databinding.ViewSectionHeaderBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import java.util.Locale;
import java.util.Objects;

public class HomeAdapter extends ListAdapter<HomeListItem, RecyclerView.ViewHolder> {

    private final OnHomeInteractionListener listener;

    public interface OnHomeInteractionListener {
        void onBudgetPromptClick();
        void onBillClick(Bill bill);
        void onActionClick(QuickLogItem item);
        void onQuickLogClick(QuickLogItem item);
        void onQuickLogLongClick(QuickLogItem item);
        void onTransactionClick(TransactionWithUser item);
        void onTransactionLike(TransactionWithUser item);
        void onTransactionComment(TransactionWithUser item);
        void onSeeAllTransactions();
    }

    public HomeAdapter(OnHomeInteractionListener listener) {
        super(new DiffUtil.ItemCallback<HomeListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull HomeListItem oldItem, @NonNull HomeListItem newItem) {
                return oldItem.getViewType() == newItem.getViewType() && 
                       Objects.equals(oldItem.getItemId(), newItem.getItemId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull HomeListItem oldItem, @NonNull HomeListItem newItem) {
                return Objects.equals(oldItem, newItem);
            }
        });
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case HomeListItem.TYPE_DASHBOARD:
                ViewHomeDashboardBinding dashBinding = ViewHomeDashboardBinding.inflate(inflater, parent, false);
                return new DashboardViewHolder(dashBinding);
            case HomeListItem.TYPE_BUDGET_PROMPT:
                ViewBudgetPlanPromptBinding budgetBinding = ViewBudgetPlanPromptBinding.inflate(inflater, parent, false);
                return new BudgetPromptViewHolder(budgetBinding);
            case HomeListItem.TYPE_SECTION_HEADER:
                ViewSectionHeaderBinding headerBinding = ViewSectionHeaderBinding.inflate(inflater, parent, false);
                return new HeaderViewHolder(headerBinding);
            case HomeListItem.TYPE_BILL:
                return new BillViewHolder(ItemBillDueCardBinding.inflate(inflater, parent, false));
            case HomeListItem.TYPE_BILL_CONTAINER:
                return new BillContainerViewHolder(inflater.inflate(R.layout.view_home_nested_rv, parent, false), listener);
            case HomeListItem.TYPE_QUICK_ACTIONS:
                return new QuickActionsViewHolder(inflater.inflate(R.layout.view_home_nested_rv, parent, false), listener);
            case HomeListItem.TYPE_QUICK_LOG:
                return new QuickLogViewHolder(inflater.inflate(R.layout.view_home_nested_rv, parent, false), listener);
            case HomeListItem.TYPE_TRANSACTION:
                return new TransactionViewHolder(ItemTransactionBinding.inflate(inflater, parent, false));
            case HomeListItem.TYPE_EMPTY_STATE:
                return new EmptyStateViewHolder(inflater.inflate(android.R.layout.simple_list_item_1, parent, false));
            case HomeListItem.TYPE_AI_INSIGHT:
                ViewAiInsightBinding aiBinding = ViewAiInsightBinding.inflate(inflater, parent, false);
                return new AIInsightViewHolder(aiBinding);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeListItem item = getItem(position);
        if (holder instanceof DashboardViewHolder) {
            ((DashboardViewHolder) holder).bind((HomeListItem.DashboardItem) item);
        } else if (holder instanceof BudgetPromptViewHolder) {
            ((BudgetPromptViewHolder) holder).bind(listener);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HomeListItem.SectionHeaderItem) item, listener);
        } else if (holder instanceof BillViewHolder) {
            ((BillViewHolder) holder).bind((HomeListItem.BillItem) item, listener);
        } else if (holder instanceof BillContainerViewHolder) {
            ((BillContainerViewHolder) holder).bind((HomeListItem.BillContainerItem) item);
        } else if (holder instanceof QuickActionsViewHolder) {
            ((QuickActionsViewHolder) holder).bind((HomeListItem.QuickActionsItem) item);
        } else if (holder instanceof QuickLogViewHolder) {
            ((QuickLogViewHolder) holder).bind((HomeListItem.QuickLogItemContainer) item);
        } else if (holder instanceof TransactionViewHolder) {
            ((TransactionViewHolder) holder).bind((HomeListItem.TransactionItem) item, listener);
        } else if (holder instanceof AIInsightViewHolder) {
            ((AIInsightViewHolder) holder).bind((HomeListItem.AIInsightItem) item);
        }
    }

    static class DashboardViewHolder extends RecyclerView.ViewHolder {
        private final ViewHomeDashboardBinding binding;
        DashboardViewHolder(ViewHomeDashboardBinding b) { 
            super(b.getRoot()); 
            this.binding = b; 
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) b.getRoot().getLayoutParams();
            if (lp == null) {
                lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.setMargins(0, 0, 0, 0);
            b.getRoot().setLayoutParams(lp);
        }
        void bind(HomeListItem.DashboardItem item) {
            HomeViewModel.DashboardState state = item.state();
            binding.tvBalanceAmount.setText(Utils.formatCurrency(state.balance()));
            binding.tvIncomeAmount.setText(binding.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyIncome()));
            binding.tvExpensesAmount.setText(binding.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlyExpense()));
            binding.tvTotalSavings.setText(binding.getRoot().getContext().getString(R.string.format_currency_no_decimal, state.monthlySavings()));
            
            updateTrend(binding.tvIncomeTrend, state.incomeTrend(), false);
            updateTrend(binding.tvExpensesTrend, state.expenseTrend(), true);
            updateTrend(binding.tvSavingsTrend, state.savingsTrend(), false);
        }

        private void updateTrend(android.widget.TextView textView, double percentage, boolean isExpense) {
            textView.setText(String.format(java.util.Locale.getDefault(), "%s%.1f%%", percentage >= 0 ? "+" : "", percentage));
            int colorRes;
            if (percentage == 0) {
                colorRes = R.color.text_secondary;
            } else if (percentage >= 0) {
                colorRes = isExpense ? R.color.status_red : R.color.status_green;
            } else {
                colorRes = isExpense ? R.color.status_green : R.color.status_red;
            }
            textView.setTextColor(androidx.core.content.ContextCompat.getColor(textView.getContext(), colorRes));
        }
    }

    static class BudgetPromptViewHolder extends RecyclerView.ViewHolder {
        private final ViewBudgetPlanPromptBinding binding;
        BudgetPromptViewHolder(ViewBudgetPlanPromptBinding b) { 
            super(b.getRoot()); 
            this.binding = b; 
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) b.getRoot().getLayoutParams();
            if (lp == null) {
                lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.setMargins(0, Utils.dpToPx(b.getRoot().getContext(), 12), 0, 0);
            b.getRoot().setLayoutParams(lp);
        }
        void bind(OnHomeInteractionListener l) {
            binding.btnSetNow.setOnClickListener(v -> l.onBudgetPromptClick());
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ViewSectionHeaderBinding binding;
        HeaderViewHolder(ViewSectionHeaderBinding b) { 
            super(b.getRoot()); 
            this.binding = b; 
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) b.getRoot().getLayoutParams();
            if (lp == null) {
                lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.setMargins(0, Utils.dpToPx(b.getRoot().getContext(), 16), 0, 0);
            b.getRoot().setLayoutParams(lp);
        }
        void bind(HomeListItem.SectionHeaderItem item, OnHomeInteractionListener l) {
            binding.tvSectionTitle.setText(item.title());
            if (item.subtitle() != null) {
                binding.tvSectionSubTitle.setVisibility(View.VISIBLE);
                binding.tvSectionSubTitle.setText(item.subtitle());
            } else {
                binding.tvSectionSubTitle.setVisibility(View.GONE);
            }
            binding.btnSectionAction.setVisibility(item.showAction() ? View.VISIBLE : View.GONE);
            if (item.actionText() != null) binding.btnSectionAction.setText(item.actionText());
            binding.btnSectionAction.setOnClickListener(v -> l.onSeeAllTransactions());
        }
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        private final ItemBillDueCardBinding binding;
        BillViewHolder(ItemBillDueCardBinding b) { super(b.getRoot()); this.binding = b; }
        void bind(HomeListItem.BillItem item, OnHomeInteractionListener l) {
            Bill bill = item.bill();
            binding.tvBillName.setText(bill.getName());
            binding.tvDueDate.setText(bill.getDate());
            binding.tvStatus.setText(bill.getStatus());
            binding.getRoot().setOnClickListener(v -> l.onBillClick(bill));
        }
    }

    static class BillContainerViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView rv;
        private final OnHomeInteractionListener listener;
        private final GenericAdapter<Bill, ItemBillDueCardBinding> adapter;

        BillContainerViewHolder(View v, OnHomeInteractionListener l) {
            super(v);
            this.rv = v.findViewById(R.id.nestedRecyclerView);
            this.listener = l;
            rv.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rv.setPadding(Utils.dpToPx(v.getContext(), 4), 0, Utils.dpToPx(v.getContext(), 4), 0);
            rv.setClipToPadding(false);
            
            adapter = new GenericAdapter<>(Bill.DIFF_CALLBACK, (i, p) -> ItemBillDueCardBinding.inflate(i, p, false), (b, bill) -> {
                b.tvBillName.setText(bill.getName());
                b.tvDueDate.setText(bill.getDate());
                b.tvStatus.setText(bill.getStatus());
                b.getRoot().setOnClickListener(v1 -> listener.onBillClick(bill));
            });
            rv.setAdapter(adapter);
        }

        void bind(HomeListItem.BillContainerItem item) {
            adapter.submitList(item.bills());
        }
    }

    static class QuickActionsViewHolder extends RecyclerView.ViewHolder {
        private final OnHomeInteractionListener listener;
        private final GenericAdapter<QuickLogItem, ItemQuickActionBinding> adapter;

        QuickActionsViewHolder(View v, OnHomeInteractionListener l) {
            super(v);
            RecyclerView rv = v.findViewById(R.id.nestedRecyclerView);
            this.listener = l;
            rv.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rv.setPadding(Utils.dpToPx(v.getContext(), 4), 0, Utils.dpToPx(v.getContext(), 4), 0);
            rv.setClipToPadding(false);
            
            adapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickActionBinding.inflate(i, p, false), (b, action) -> {
                b.tvLabel.setText(action.getTitle());
                b.ivIcon.setImageResource(action.getIconRes());
                b.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(b.getRoot().getContext(), action.getBgColor()));
                b.ivIcon.setColorFilter(ContextCompat.getColor(b.getRoot().getContext(), action.getIconColor()));
            });
            adapter.setOnItemClickListener(listener::onActionClick);
            rv.setAdapter(adapter);
        }

        void bind(HomeListItem.QuickActionsItem item) {
            adapter.submitList(item.actions());
        }
    }

    static class QuickLogViewHolder extends RecyclerView.ViewHolder {
        private final OnHomeInteractionListener listener;
        private final GenericAdapter<QuickLogItem, ItemQuickLogBinding> adapter;

        QuickLogViewHolder(View v, OnHomeInteractionListener l) {
            super(v);
            RecyclerView rv = v.findViewById(R.id.nestedRecyclerView);
            this.listener = l;
            rv.setLayoutManager(new GridLayoutManager(v.getContext(), 4));
            
            adapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickLogBinding.inflate(i, p, false), (b, log) -> {
                b.tvLabel.setText(log.getTitle());
                b.ivIcon.setImageResource(log.getIconRes());
                int color = ContextCompat.getColor(b.getRoot().getContext(), log.getIconColor());
                b.cvIconBg.setCardBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(color, 25));
                b.ivIcon.setColorFilter(color);
            });
            adapter.setOnItemClickListener(listener::onQuickLogClick);
            adapter.setOnItemLongClickListener(log -> { listener.onQuickLogLongClick(log); return true; });
            rv.setAdapter(adapter);
        }

        void bind(HomeListItem.QuickLogItemContainer item) {
            adapter.submitList(item.logs());
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;
        TransactionViewHolder(ItemTransactionBinding b) { super(b.getRoot()); this.binding = b; }
        
        void bind(HomeListItem.TransactionItem itemContainer, OnHomeInteractionListener l) {
            TransactionWithUser itemWithUser = itemContainer.transaction();
            Transaction item = itemWithUser.getTransaction();
            
            String username = itemWithUser.getUsername() != null && !itemWithUser.getUsername().isEmpty() 
                    ? itemWithUser.getUsername() : "Someone";
            
            String type = item.getType() != null ? item.getType().toUpperCase() : "EXPENSE";
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

            binding.tvUserAction.setText(username);
            binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
            binding.tvTitle.setText(item.getCategory()); 

            String note = item.getNote();
            if (note != null && !note.isEmpty()) {
                String filteredNote = note.replace("Logged by Carti AI", "").replace("Logged by AI", "").trim();
                binding.tvDescription.setText(filteredNote.isEmpty() ? actionLabel : String.format("%s: %s", actionLabel, filteredNote));
            } else {
                binding.tvDescription.setText(actionLabel);
            }
            
            String formattedAmount = Utils.formatCurrency(item.getAmount());
            binding.tvAmount.setText(binding.getRoot().getContext().getString(amountFormatRes, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), amountColor));

            if (itemWithUser.getUserAvatarUrl() != null && !itemWithUser.getUserAvatarUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(itemWithUser.getUserAvatarUrl())
                        .placeholder(R.drawable.ai_holder)
                        .error(R.drawable.ai_holder)
                        .into(binding.ivAvatar);
            } else if (itemWithUser.getUserAvatarRes() != 0) {
                binding.ivAvatar.setImageResource(itemWithUser.getUserAvatarRes());
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ai_holder);
            }

            String myReaction = itemWithUser.getMyReaction();
            String lastEmoji = item.getLastEmoji();

            if (myReaction != null && !myReaction.isEmpty()) {
                binding.tvBtnLikeIcon.setText(myReaction);
                binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.carti_primary_green));
            } else {
                binding.tvBtnLikeIcon.setText("👍");
                binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
            }

            binding.tvReactionEmoji.setText(myReaction != null && !myReaction.isEmpty() ? myReaction : (lastEmoji != null && !lastEmoji.isEmpty() ? lastEmoji : "👍"));
            
            int likesCount = item.getLikesCount();
            int commentCount = item.getCommentCount();
            String rNames = itemWithUser.getReactorNames();

            if (likesCount > 0 || commentCount > 0) {
                binding.layoutReactionsSummary.setVisibility(android.view.View.VISIBLE);
                if (likesCount > 0) {
                    binding.tvReactionEmoji.setVisibility(android.view.View.VISIBLE);
                    binding.tvLikesCount.setVisibility(android.view.View.VISIBLE);
                    binding.tvLikesCount.setText(rNames != null && !rNames.isEmpty() ? rNames : String.valueOf(likesCount));
                } else {
                    binding.tvReactionEmoji.setVisibility(android.view.View.GONE);
                    binding.tvLikesCount.setVisibility(android.view.View.GONE);
                }

                if (commentCount > 0) {
                    binding.tvCommentsCountSummary.setText(String.format(Locale.getDefault(), "%d comments", commentCount));
                    binding.tvCommentsCountSummary.setVisibility(android.view.View.VISIBLE);
                } else {
                    binding.tvCommentsCountSummary.setVisibility(android.view.View.GONE);
                }
            } else {
                binding.layoutReactionsSummary.setVisibility(android.view.View.GONE);
            }

            binding.btnLike.setOnClickListener(v -> l.onTransactionLike(itemWithUser));
            binding.tvLikesCount.setOnClickListener(v -> l.onSeeAllTransactions());
            binding.btnComment.setOnClickListener(v -> l.onTransactionComment(itemWithUser));
            binding.getRoot().setOnClickListener(v -> l.onTransactionClick(itemWithUser));
        }
    }

    static class AIInsightViewHolder extends RecyclerView.ViewHolder {
        private final ViewAiInsightBinding binding;
        AIInsightViewHolder(ViewAiInsightBinding b) { 
            super(b.getRoot()); 
            this.binding = b; 
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) b.getRoot().getLayoutParams();
            if (lp == null) {
                lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.setMargins(0, Utils.dpToPx(b.getRoot().getContext(), 12), 0, 0);
            b.getRoot().setLayoutParams(lp);
        }
        void bind(HomeListItem.AIInsightItem item) {
            binding.tvInsightMessage.setText(item.message());
        }
    }

    static class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        EmptyStateViewHolder(View v) { super(v); }
    }
}
