package com.upreyvan.carti.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.upreyvan.carti.R;
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.models.Category;

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

    public static void applyCategoryStyle(View container, View iconBg, ImageView icon, String categoryName, boolean applyToContainer) {
        if (container == null && iconBg == null && icon == null) return;
        Context context = (container != null) ? container.getContext() : (iconBg != null ? iconBg.getContext() : icon.getContext());
        Category cat = CategoryManager.getInstance(context).getCategoryByName(categoryName);
        
        int iconColorRes = R.color.carti_primary_green;
        int bgColorRes = R.color.log_others;

        if (cat != null) {
            if (cat.getIconColor() != 0) iconColorRes = cat.getIconColor();
            if (cat.getBackgroundColor() != 0) bgColorRes = cat.getBackgroundColor();
        }

        int iconColor = ContextCompat.getColor(context, iconColorRes);
        int bgColor = ContextCompat.getColor(context, bgColorRes);

        if (icon != null) icon.setColorFilter(iconColor);
        
        if (iconBg != null) {
            if (applyToContainer && container != null) {
                int glassColor = androidx.core.graphics.ColorUtils.setAlphaComponent(0, 8);
                if (iconBg instanceof MaterialCardView mcv) mcv.setCardBackgroundColor(glassColor);
                else iconBg.setBackgroundColor(glassColor);
            } else {
                if (iconBg instanceof MaterialCardView mcv) mcv.setCardBackgroundColor(bgColor);
                else iconBg.setBackgroundColor(bgColor);
            }
        }

        if (container != null) {
            if (applyToContainer) {
                if (container instanceof MaterialCardView mcv) {
                    mcv.setCardBackgroundColor(bgColor);
                    mcv.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(iconColor, 60))); // ~25% alpha
                    mcv.setStrokeWidth(3); 
                } else {
                    container.setBackgroundColor(bgColor);
                }
            } else {
                int defaultColor = ContextCompat.getColor(context, R.color.white);
                if (container instanceof MaterialCardView mcv) {
                    mcv.setCardBackgroundColor(defaultColor);
                    mcv.setStrokeWidth(0);
                } else {
                    container.setBackgroundColor(defaultColor);
                }
            }
        }
    }

    public static void applyCategoryStyle(View container, View iconBg, ImageView icon, String categoryName) {
        applyCategoryStyle(container, iconBg, icon, categoryName, true);
    }

    public static void showPopupMenuWithIcons(androidx.appcompat.widget.PopupMenu popup) {
        try {
            java.lang.reflect.Field[] fields = popup.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuHelper.getClass().getName());
                    java.lang.reflect.Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        popup.show();
    }
}
