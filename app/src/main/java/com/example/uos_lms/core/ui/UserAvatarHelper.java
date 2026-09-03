package com.example.uos_lms.core.ui;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.uos_lms.R;

/** Java port of the Compose UserAvatar - circular profile photo with a person-icon fallback. */
public final class UserAvatarHelper {

    private UserAvatarHelper() {
    }

    public static void bind(ImageView imageView, String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            imageView.setImageResource(R.drawable.ic_person);
            return;
        }
        Glide.with(imageView)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}
