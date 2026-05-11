package com.upreyvan.carti.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Utils {

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static void applySystemBarInsets(
            View topView,
            View bottomView,
            float topMultiplier,
            int customBarHeight
    ) {

        int originalTopPadding = topView.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(topView, (v, insets) -> {

            Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int statusBarHeight = systemBars.top;

            int adjustedTop =
                    (int) (statusBarHeight * topMultiplier);

            v.setPadding(
                    v.getPaddingLeft(),
                    originalTopPadding + adjustedTop,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return insets;
        });

        if (bottomView != null) {

            int originalBottomPadding = bottomView.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {

                Insets systemBars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());

                int systemNavHeight = systemBars.bottom;

                int imeHeight =
                        insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

                if (v.getLayoutParams()
                        instanceof ViewGroup.MarginLayoutParams) {

                    ViewGroup.MarginLayoutParams params =
                            (ViewGroup.MarginLayoutParams)
                                    v.getLayoutParams();

                    params.bottomMargin =
                            Math.max(systemNavHeight, imeHeight);

                    v.setLayoutParams(params);
                }

                int extraBuffer =
                        Utils.dpToPx(v.getContext(), 16);

                int totalBottomPadding =
                        originalBottomPadding
                                + customBarHeight
                                + extraBuffer;

                if (imeHeight > 0) {
                    totalBottomPadding =
                            originalBottomPadding
                                    + Utils.dpToPx(v.getContext(), 12);
                }

                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        totalBottomPadding
                );

                return insets;
            });
        }
    }
}