package com.upreyvan.carti.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.upreyvan.carti.R;

public class RootLayout extends ConstraintLayout {

    private boolean useBottomNavPadding = true;
    private boolean useImePadding = false;
    private Integer originalFirstChildTopPadding = null;
    private Integer originalBottomPadding = null;

    public RootLayout(@NonNull Context context) {
        this(context, null);
    }

    public RootLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RootLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RootLayout);
            useBottomNavPadding = a.getBoolean(R.styleable.RootLayout_useBottomNavPadding, true);
            useImePadding = a.getBoolean(R.styleable.RootLayout_useImePadding, false);
            a.recycle();
        }
        init();
    }

    private void init() {
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            if (originalBottomPadding == null) {
                originalBottomPadding = getPaddingBottom();
            }

            if (getChildCount() > 0) {
                View firstChild = getChildAt(0);
                if (originalFirstChildTopPadding == null) {
                    originalFirstChildTopPadding = firstChild.getPaddingTop();
                }

                int topGap = getResources().getDimensionPixelSize(R.dimen.spacing_xs);
                firstChild.setPadding(
                    firstChild.getPaddingLeft(),
                    systemBars.top + topGap,
                    firstChild.getPaddingRight(),
                    firstChild.getPaddingBottom()
                );
            }

            int bottomInset = systemBars.bottom;
            int imeInset = useImePadding ? ime.bottom : 0;

            int navPadding = (useBottomNavPadding && imeInset == 0) ? getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding) : 0;
            
            setPadding(
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingRight(),
                originalBottomPadding + bottomInset + imeInset + navPadding
            );

            return WindowInsetsCompat.CONSUMED;
        });
    }

    public void setUseBottomNavPadding(boolean use) {
        this.useBottomNavPadding = use;
        ViewCompat.requestApplyInsets(this);
    }

    public void setUseImePadding(boolean use) {
        this.useImePadding = use;
        ViewCompat.requestApplyInsets(this);
    }
}
