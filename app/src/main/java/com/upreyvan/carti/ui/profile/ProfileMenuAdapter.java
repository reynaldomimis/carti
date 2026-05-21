package com.upreyvan.carti.ui.profile;

import android.view.View;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemProfileMenuBinding;
import com.upreyvan.carti.model.ProfileMenuItem;

public class ProfileMenuAdapter extends BaseAdapter<ProfileMenuItem, ItemProfileMenuBinding> {

    public ProfileMenuAdapter(OnItemClickListener<ProfileMenuItem> listener) {
        super(ProfileMenuItem.DIFF_CALLBACK,
              (inflater, parent) -> ItemProfileMenuBinding.inflate(inflater, parent, false),
              (binding, item) -> {
                  binding.ivMenuIcon.setImageResource(item.getIconResId());
                  binding.tvMenuTitle.setText(item.getTitleResId());
                  binding.tvMenuSubTitle.setText(item.getSubTitle());
                  binding.divider.setVisibility(item.isShowDivider() ? View.VISIBLE : View.GONE);
              });
        setOnItemClickListener(listener);
    }
}