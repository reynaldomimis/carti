package com.upreyvan.carti.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.LayoutReactionSelectorBinding;

public class DialogHelper {

    public interface DialogCallback {
        void onConfirm();
    }

    public interface EmojiCallback {
        void onEmojiSelected(String emoji);
    }

    public static void showConfirmation(Context context, String title, String message, String positiveButton, DialogCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveButton, (dialog, which) -> callback.onConfirm())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showNoInternetDialog(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.title_no_internet))
                .setMessage(context.getString(R.string.msg_no_internet_transaction))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.btn_ok), null)
                .setIcon(R.drawable.ic_chart)
                .show();
    }

    public static void showEmojiPicker(View anchor, EmojiCallback callback) {
        Context context = anchor.getContext();
        LayoutReactionSelectorBinding binding = LayoutReactionSelectorBinding.inflate(LayoutInflater.from(context));
        
        PopupWindow popup = new PopupWindow(binding.getRoot(), 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(16f);

        View.OnClickListener listener = v -> {
            String selectedEmoji = ReactionHelper.getEmoji(v.getId());
            if (callback != null) callback.onEmojiSelected(selectedEmoji);
            popup.dismiss();
        };

        binding.reacLike.setOnClickListener(listener);
        binding.reacLove.setOnClickListener(listener);
        binding.reacHaha.setOnClickListener(listener);
        binding.reacWow.setOnClickListener(listener);
        binding.reacSad.setOnClickListener(listener);
        binding.reacAngry.setOnClickListener(listener);

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        
        binding.getRoot().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = binding.getRoot().getMeasuredHeight();
        
        popup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, 
                location[0], location[1] - popupHeight - 16);
    }
}
