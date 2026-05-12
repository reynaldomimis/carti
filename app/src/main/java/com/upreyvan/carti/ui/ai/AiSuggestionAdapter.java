package com.upreyvan.carti.ui.ai;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemAiSuggestionBinding;
import com.upreyvan.carti.model.AiSuggestion;

public class AiSuggestionAdapter extends BaseAdapter<AiSuggestion, ItemAiSuggestionBinding> {

    public AiSuggestionAdapter() {
        super(new DiffUtil.ItemCallback<AiSuggestion>() {
            @Override
            public boolean areItemsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getText().equals(newItem.getText());
            }

            @Override
            public boolean areContentsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
                return oldItem.getText().equals(newItem.getText());
            }
        });
    }

    @Override
    protected ItemAiSuggestionBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemAiSuggestionBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(ItemAiSuggestionBinding binding, AiSuggestion item) {
        binding.tvSuggestion.setText(item.getText());
        binding.ivIcon.setImageResource(item.getIconResId());
        binding.ivIcon.setColorFilter(item.getIconColor());
        binding.cvIcon.setCardBackgroundColor(item.getBgColor());
    }
}