package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.QuickLogItem;

import java.util.List;

public class QuickLogAdapter extends RecyclerView.Adapter<QuickLogAdapter.ViewHolder> {

    private final List<QuickLogItem> items;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(QuickLogItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public QuickLogAdapter(List<QuickLogItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuickLogBinding binding = ItemQuickLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickLogItem item = items.get(position);
        holder.binding.tvLabel.setText(item.getTitle());
        holder.binding.ivIcon.setImageResource(item.getIconRes());
        
        // Standardize icon background color to a light gray (IISA LANG)
        holder.binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_variant));
        
        // Keep unique icon colors (IBA IBA PER ITEM)
        holder.binding.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), item.getIconColor()));

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemQuickLogBinding binding;

        public ViewHolder(ItemQuickLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
