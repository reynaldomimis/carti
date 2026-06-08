package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemCustomizeCategoryBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;

import java.util.Collections;
import java.util.List;

public class CustomizeCategoryAdapter extends RecyclerView.Adapter<CustomizeCategoryAdapter.ViewHolder> {

    private final List<BudgetCategoryItem> categories;

    public CustomizeCategoryAdapter(List<BudgetCategoryItem> categories) {
        this.categories = categories;
    }

    public List<BudgetCategoryItem> getCategories() {
        return categories;
    }

    public void moveItem(int fromPosition, int toPosition) {
        Collections.swap(categories, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCustomizeCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetCategoryItem category = categories.get(position);
        holder.binding.tvCategoryName.setText(category.getCategoryName());
        holder.binding.ivIcon.setImageResource(category.getIconRes() != 0 ? category.getIconRes() : R.drawable.ic_chart);
        
        int iconColorRes = category.getIconColor() != 0 ? category.getIconColor() : R.color.carti_primary_green;
        int iconColor = holder.itemView.getContext().getColor(iconColorRes);
        int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
        
        holder.binding.cardIcon.setCardBackgroundColor(bgColor);
        holder.binding.ivIcon.setColorFilter(iconColor);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCustomizeCategoryBinding binding;
        ViewHolder(ItemCustomizeCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
