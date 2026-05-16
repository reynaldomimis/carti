package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.databinding.ItemQuickLogShimmerBinding;
import com.upreyvan.carti.model.QuickLogItem;

import java.util.List;

public class QuickLogAdapter extends RecyclerView.Adapter<QuickLogAdapter.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_SHIMMER = 2;
    private final List<QuickLogItem> items;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(QuickLogItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(QuickLogItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public QuickLogAdapter(List<QuickLogItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isShimmer() ? VIEW_TYPE_SHIMMER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SHIMMER) {
            ItemQuickLogShimmerBinding shimmerBinding = ItemQuickLogShimmerBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(shimmerBinding);
        }
        ItemQuickLogBinding binding = ItemQuickLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickLogItem item = items.get(position);
        
        if (item.isShimmer()) {
            if (holder.shimmerBinding != null) {
                holder.shimmerBinding.getRoot().startShimmer();
            }
            return;
        }

        if (holder.binding == null) return;

        holder.binding.tvLabel.setText(item.getTitle());
        holder.binding.ivIcon.setImageResource(item.getIconRes());

        int iconColor = ContextCompat.getColor(holder.itemView.getContext(), item.getIconColor());
        int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
        holder.binding.cvIconBg.setCardBackgroundColor(bgColor);
        holder.binding.ivIcon.setColorFilter(iconColor);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemQuickLogBinding binding;
        ItemQuickLogShimmerBinding shimmerBinding;

        public ViewHolder(ItemQuickLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewHolder(ItemQuickLogShimmerBinding shimmerBinding) {
            super(shimmerBinding.getRoot());
            this.shimmerBinding = shimmerBinding;
        }
    }
}
