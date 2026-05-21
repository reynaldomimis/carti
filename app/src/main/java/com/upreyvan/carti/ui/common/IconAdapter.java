package com.upreyvan.carti.ui.common;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemIconChoiceBinding;
import com.upreyvan.carti.model.IconChoice;
import java.util.List;

public class IconAdapter extends BaseAdapter<IconChoice, ItemIconChoiceBinding> {

    public IconAdapter(OnItemClickListener<IconChoice> listener) {
        super(IconChoice.DIFF_CALLBACK,
              (inflater, parent) -> ItemIconChoiceBinding.inflate(inflater, parent, false),
              (binding, icon) -> binding.ivIcon.setImageResource(icon.getIconRes()));
        setOnItemClickListener(listener);
    }

    public void updateList(List<IconChoice> newList) {
        submitList(newList);
    }
}