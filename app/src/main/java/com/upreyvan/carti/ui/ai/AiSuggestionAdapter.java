package com.upreyvan.carti.ui.ai;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemAiSuggestionBinding;
import com.upreyvan.carti.model.AiSuggestion;

public class AiSuggestionAdapter extends BaseAdapter<AiSuggestion, ItemAiSuggestionBinding> {

    public AiSuggestionAdapter() {
        super(new DiffUtil.ItemCallback<AiSuggestion>() {
            @Override
            public boolean areItemsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getDescription().equals(newItem.getDescription());
            }
        },
        (inflater, parent) -> ItemAiSuggestionBinding.inflate(inflater, parent, false),
        (binding, item) -> {
            binding.tvSuggestionTitle.setText(item.getTitle());
            binding.tvSuggestion.setText(item.getDescription());
            binding.ivIcon.setImageResource(item.getIconResId());
            
            int color = ContextCompat.getColor(binding.getRoot().getContext(), item.getThemeColor());
            binding.ivIcon.setColorFilter(color);
            binding.cvIcon.setCardBackgroundColor(color);
            binding.cvIcon.setCardForegroundColor(null); // Clear any foreground if needed
        });
    }
}
