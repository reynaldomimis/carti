package com.upreyvan.carti.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.upreyvan.carti.R;

public class RootLayout extends ConstraintLayout {

    private boolean useImePadding = false;
    private boolean useBottomNavPadding = false;
    private int imeGap = 0;

    public RootLayout(@NonNull Context context) {
        this(context, null);
    }

    public RootLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RootLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boolean useStatusBarPadding = false;
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RootLayout, defStyleAttr, 0)) {
                useImePadding = a.getBoolean(R.styleable.RootLayout_useImePadding, false);
                useBottomNavPadding = a.getBoolean(R.styleable.RootLayout_useBottomNavPadding, false);
                useStatusBarPadding = a.getBoolean(R.styleable.RootLayout_useStatusBarPadding, false);
                imeGap = a.getDimensionPixelSize(R.styleable.RootLayout_imeGap, 0);
            }
        }

        if (useStatusBarPadding) {
            setPadding(getPaddingLeft(), getStatusBarHeight(), getPaddingRight(), getPaddingBottom());
        }

        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            
            int bottomPadding = 0;
            if (useImePadding && imeInsets.bottom > 0) {
                bottomPadding = imeInsets.bottom + imeGap;
            } else if (useBottomNavPadding) {
                bottomPadding = navInsets.bottom;
            }

            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);
            return insets;
        });
    }

    @android.annotation.SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
