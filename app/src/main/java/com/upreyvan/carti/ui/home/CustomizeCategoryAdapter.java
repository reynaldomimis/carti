package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemCustomizeCategoryBinding;
import com.upreyvan.carti.model.Category;

import java.util.Collections;
import java.util.List;

public class CustomizeCategoryAdapter extends RecyclerView.Adapter<CustomizeCategoryAdapter.ViewHolder> {

    private final List<Category> categories;

    public CustomizeCategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    public List<Category> getCategories() {
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
        Category category = categories.get(position);
        holder.binding.tvCategoryName.setText(category.getName());
        holder.binding.ivIcon.setImageResource(category.getIconRes());
        holder.binding.cardIcon.setCardBackgroundColor(holder.itemView.getContext().getColor(category.getBackgroundColor()));
        holder.binding.ivIcon.setColorFilter(holder.itemView.getContext().getColor(category.getIconColor()));
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
