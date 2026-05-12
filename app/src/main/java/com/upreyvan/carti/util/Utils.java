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
            final int originalBottomPadding = bottomView.getPaddingBottom();
            final ViewGroup.LayoutParams layoutParams = bottomView.getLayoutParams();
            final int originalBottomMargin = (layoutParams instanceof ViewGroup.MarginLayoutParams) ?
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin : 0;

            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int systemNavHeight = systemBars.bottom;

                if (v instanceof androidx.core.widget.NestedScrollView || v instanceof androidx.recyclerview.widget.RecyclerView) {
                    v.setPadding(
                            v.getPaddingLeft(),
                            v.getPaddingTop(),
                            v.getPaddingRight(),
                            originalBottomPadding + systemNavHeight + customBarHeight
                    );
                } else {
                    if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        lp.bottomMargin = originalBottomMargin + systemNavHeight + customBarHeight;
                        v.setLayoutParams(lp);
                    }
                }

                return insets;
            });
        }
    }
}