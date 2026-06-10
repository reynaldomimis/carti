package com.upreyvan.carti.ui.home;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemReactorBinding;
import com.upreyvan.carti.models.Reactor;
import com.upreyvan.carti.utils.AvatarHelper;

public class ReactorAdapter extends BaseAdapter<Reactor, ItemReactorBinding> {

    public ReactorAdapter() {
        super(Reactor.DIFF_CALLBACK,
                (inflater, parent) -> ItemReactorBinding.inflate(inflater, parent, false),
                (binding, item, position, count) -> {
                    binding.tvUsername.setText(item.getUsername());
                    binding.tvEmoji.setText(item.getEmoji());

                    AvatarHelper.loadUserAvatar(binding.getRoot().getContext(), binding.ivAvatar, item.getUsername(), item.getAvatarUrl());
                });
    }
}
