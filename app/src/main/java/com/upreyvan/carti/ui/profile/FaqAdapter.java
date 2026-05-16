package com.upreyvan.carti.ui.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemFaqBinding;
import com.upreyvan.carti.model.FaqItem;

import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private final List<FaqItem> items;

    public FaqAdapter(List<FaqItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFaqBinding binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FaqViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqItem item = items.get(position);
        holder.binding.tvQuestion.setText(item.getQuestion());
        holder.binding.tvAnswer.setText(item.getAnswer());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FaqViewHolder extends RecyclerView.ViewHolder {
        final ItemFaqBinding binding;

        FaqViewHolder(ItemFaqBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}