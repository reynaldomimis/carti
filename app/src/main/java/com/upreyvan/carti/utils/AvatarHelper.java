package com.upreyvan.carti.utils;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.upreyvan.carti.BuildConfig;
import com.upreyvan.carti.R;

public class AvatarHelper {

    public static void loadUserAvatar(Context context, ImageView imageView, String username, String avatarUrl) {
        String name = (username != null && !username.isEmpty()) ? username : "Someone";

        String initialsUrl = "https://cloud.appwrite.io/v1/avatars/initials?name=" + name + "&project=" + BuildConfig.APPWRITE_PROJECT_ID;

        Object loadSource = (avatarUrl != null && !avatarUrl.isEmpty()) ? avatarUrl : initialsUrl;

        Glide.with(context)
                .load(loadSource)
                .transition(DrawableTransitionOptions.withCrossFade())
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(initialsUrl)
                .into(imageView);
    }

    /**
     * Loads a user avatar using only the username (generates initials).
     */
    public static void loadUserAvatar(Context context, ImageView imageView, String username) {
        loadUserAvatar(context, imageView, username, null);
    }
}
