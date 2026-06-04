package com.upreyvan.carti.ui.allocate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemAllocationChildBinding;
import com.upreyvan.carti.databinding.ItemAllocationParentBinding;
import com.upreyvan.carti.model.BudgetAllocation;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.StringHelper;
import com.upreyvan.carti.util.Utils;

public class AllocateAdapter extends ListAdapter<BudgetAllocation, AllocateAdapter.ParentViewHolder> {
    private boolean isTrackMode = false;

    public AllocateAdapter() { super(BudgetAllocation.DIFF_CALLBACK); }
    public void setTrackMode(boolean track) { this.isTrackMode = track; notifyDataSetChanged(); }

    @NonNull @Override public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ParentViewHolder(ItemAllocationParentBinding.inflate(LayoutInflater.from(p.getContext()), p, false)); }
    @Override public void onBindViewHolder(@NonNull ParentViewHolder h, int pos) { h.bind(getItem(pos)); }

    class ParentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAllocationParentBinding b;
        ParentViewHolder(ItemAllocationParentBinding b) { super(b.getRoot()); this.b = b; }

        void bind(BudgetAllocation item) {
            b.tvTitle.setText(item.getTitle());
            b.ivIcon.setImageResource(item.getIconRes());
            b.cvIcon.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), item.getThemeColor()));
            b.tvAmountLabel.setText(String.format("%s / %s", StringHelper.formatCompactCurrency(item.getCurrentSpent()), StringHelper.formatCompactCurrency(item.getAllocatedAmount())));
            b.pbAllocation.setProgress((int) ((item.getCurrentSpent() / item.getAllocatedAmount()) * 100));

            boolean expandable = isTrackMode ? item.hasExpenses() : item.hasSubAllocations();
            b.ivArrow.setVisibility(expandable ? View.VISIBLE : View.GONE);
            if (expandable) {
                b.ivArrow.setRotation(item.isExpanded() ? 180f : 0f);
                b.rvSubAllocations.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
                if (item.isExpanded()) {
                    if (isTrackMode) setupExpenseChildren(item); else setupSubChildren(item);
                }
            } else b.rvSubAllocations.setVisibility(View.GONE);

            b.cardParent.setOnClickListener(v -> { if (expandable) { item.setExpanded(!item.isExpanded()); notifyItemChanged(getAdapterPosition()); } });
        }

        private void setupSubChildren(BudgetAllocation p) {
            GenericAdapter<BudgetAllocation, ItemAllocationChildBinding> adapter = new GenericAdapter<>(BudgetAllocation.DIFF_CALLBACK, (i, c) -> ItemAllocationChildBinding.inflate(i, c, false), (bi, child) -> {
                bi.tvTitle.setText(child.getTitle());
                bi.tvAmount.setText(Utils.formatCurrency(child.getAllocatedAmount()));
                bi.pbAllocation.setProgress((int) ((child.getCurrentSpent() / child.getAllocatedAmount()) * 100));
            });
            b.rvSubAllocations.setLayoutManager(new LinearLayoutManager(itemView.getContext())); b.rvSubAllocations.setAdapter(adapter);
            adapter.submitList(p.getSubAllocations());
        }

        private void setupExpenseChildren(BudgetAllocation p) {
            GenericAdapter<Transaction, ItemAllocationChildBinding> adapter = new GenericAdapter<>(Transaction.DIFF_CALLBACK, (i, c) -> ItemAllocationChildBinding.inflate(i, c, false), (bi, tx) -> {
                bi.tvTitle.setText(tx.getNote());
                bi.tvAmount.setText(Utils.formatCurrency(tx.getAmount()));
                bi.pbAllocation.setVisibility(View.GONE);
            });
            b.rvSubAllocations.setLayoutManager(new LinearLayoutManager(itemView.getContext())); b.rvSubAllocations.setAdapter(adapter);
            adapter.submitList(p.getExpenses());
        }
    }
}
