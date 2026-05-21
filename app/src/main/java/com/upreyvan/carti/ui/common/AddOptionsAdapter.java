package com.upreyvan.carti.ui.common;

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemAddOptionBinding;
import com.upreyvan.carti.model.AddOption;

public class AddOptionsAdapter extends BaseAdapter<AddOption, ItemAddOptionBinding> {

    public AddOptionsAdapter(OnItemClickListener<AddOption> listener) {
        super(AddOption.DIFF_CALLBACK,
              (inflater, parent) -> ItemAddOptionBinding.inflate(inflater, parent, false),
              (binding, item) -> {
                  binding.ivOptionIcon.setImageResource(item.getIconResId());
                  binding.ivOptionIcon.setImageTintList(ColorStateList.valueOf(
                          ContextCompat.getColor(binding.getRoot().getContext(), item.getIconTintResId())));
                  
                  binding.viewBgTint.setBackgroundTintList(ColorStateList.valueOf(
                          ContextCompat.getColor(binding.getRoot().getContext(), item.getBgTintResId())));
                  
                  binding.tvOptionTitle.setText(item.getTitleResId());
                  binding.tvOptionDesc.setText(item.getDescResId());
              });
        setOnItemClickListener(listener);
    }
}