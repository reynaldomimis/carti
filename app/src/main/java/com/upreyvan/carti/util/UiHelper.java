package com.upreyvan.carti.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;
import com.upreyvan.carti.R;

public class UiHelper {

    public enum Status {
        SUCCESS, ERROR, INFO, WARNING
    }

    public static void showSnackbar(@Nullable View view, @Nullable String message, @NonNull Status status) {
        if (view == null || message == null || message.trim().isEmpty()) return;

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        int colorRes = switch (status) {
            case SUCCESS -> R.color.status_green;
            case ERROR -> R.color.status_red;
            case WARNING -> R.color.dash_orange;
            default -> R.color.carti_primary_blue;
        };
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), colorRes));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    public static void showSnackbar(@Nullable View view, @StringRes int messageRes, @NonNull Status status) {
        if (view == null) return;
        showSnackbar(view, view.getContext().getString(messageRes), status);
    }

    public static void showSnackbar(@Nullable Activity activity, @Nullable String message, @NonNull Status status) {
        if (activity == null) return;
        View rootView = activity.findViewById(android.R.id.content);
        showSnackbar(rootView, message, status);
    }

    public static void applySystemBarInsets(View topView, View bottomView, float marginMultiplier, int extraBottomPadding) {
        if (topView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), (int) (systemBars.top * marginMultiplier), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (bottomView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), (int) (systemBars.bottom * marginMultiplier) + extraBottomPadding);
                return insets;
            });
        }
    }

    public static void showKeyboard(Context context, View view) {
        if (context == null || view == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            view.requestFocus();
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void hideKeyboard(Context context, View view) {
        if (context == null || view == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void setOptionalText(TextView textView, String text) {
        if (textView == null) return;
        if (text == null || text.trim().isEmpty()) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
