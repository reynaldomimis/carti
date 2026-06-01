package com.upreyvan.carti.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

public class UiHelper {

    public static void applySystemBarInsets(View topView, View bottomView, float topMultiplier, int customBarHeight) {
        int originalTopPadding = topView.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(topView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int adjustedTop = (int) (systemBars.top * topMultiplier);
            v.setPadding(v.getPaddingLeft(), originalTopPadding + adjustedTop, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        if (bottomView != null) {
            final int originalBottomPadding = bottomView.getPaddingBottom();
            final ViewGroup.LayoutParams layoutParams = bottomView.getLayoutParams();
            final int originalBottomMargin = (layoutParams instanceof ViewGroup.MarginLayoutParams) ?
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin : 0;

            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
                int systemNavHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                if (v instanceof NestedScrollView || v instanceof RecyclerView) {
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), originalBottomPadding + systemNavHeight + customBarHeight);
                } else if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    lp.bottomMargin = originalBottomMargin + systemNavHeight + customBarHeight;
                    v.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }

    public static void showKeyboard(Context context, View view) {
        if (context != null && view != null) {
            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void hideKeyboard(Context context, View view) {
        if (context != null && view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void setOptionalText(TextView textView, String text) {
        if (text == null || text.trim().isEmpty()) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
