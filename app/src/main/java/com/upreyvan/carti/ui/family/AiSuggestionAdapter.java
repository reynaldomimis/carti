package com.upreyvan.carti.ui.family;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemAiSuggestionCardBinding;
import com.upreyvan.carti.model.AiSuggestion;

public class AiSuggestionAdapter extends BaseAdapter<AiSuggestion, ItemAiSuggestionCardBinding> {

    public AiSuggestionAdapter() {
        super(AiSuggestion.DIFF_CALLBACK,
                (inflater, parent) -> ItemAiSuggestionCardBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvSuggestionTitle.setText(item.getTitle());
                    binding.ivIcon.setImageResource(item.getIconResId());
                });
    }
}
