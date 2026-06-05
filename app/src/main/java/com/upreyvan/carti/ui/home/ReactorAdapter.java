package com.upreyvan.carti.ui.home;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemReactorBinding;
import com.upreyvan.carti.model.Reactor;

public class ReactorAdapter extends BaseAdapter<Reactor, ItemReactorBinding> {

    public ReactorAdapter() {
        super(Reactor.DIFF_CALLBACK,
                (inflater, parent) -> ItemReactorBinding.inflate(inflater, parent, false),
                (binding, item, position, count) -> {
                    binding.tvUsername.setText(item.getUsername());
                    binding.tvEmoji.setText(item.getEmoji());

                    if (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) {
                        Glide.with(binding.getRoot().getContext())
                                .load(item.getAvatarUrl())
                                .placeholder(R.drawable.ai_holder)
                                .error(R.drawable.ai_holder)
                                .into(binding.ivAvatar);
                    } else if (item.getAvatarRes() != 0) {
                        binding.ivAvatar.setImageResource(item.getAvatarRes());
                    } else {
                        binding.ivAvatar.setImageResource(R.drawable.ai_holder);
                    }
                });
    }
}
