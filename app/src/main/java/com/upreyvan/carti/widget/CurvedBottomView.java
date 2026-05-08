package com.upreyvan.carti.widget;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.upreyvan.carti.R;

public class CurvedBottomView extends View {
    private final Path mPath = new Path();
    private final Path mTopPath = new Path();
    private final Paint mPaint = new Paint();
    private final Paint mStrokePaint = new Paint();

    private float bezierX = 0;
    private final int CURVE_CIRCLE_RADIUS = 110;

    public CurvedBottomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);

        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(context.getColor(R.color.border_light));
        mStrokePaint.setStrokeWidth(2f);
        mStrokePaint.setAntiAlias(true);

        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bezierX == 0) bezierX = w / 2f;
    }

    public void animateCurveTo(float targetX) {
        ValueAnimator animator = ValueAnimator.ofFloat(bezierX, targetX);
        animator.setDuration(450);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            bezierX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        mTopPath.reset();
        mTopPath.moveTo(0, 0);

        float controlDistance = CURVE_CIRCLE_RADIUS * 1.5f;
        mTopPath.lineTo(bezierX - controlDistance, 0);

        mTopPath.cubicTo(
                bezierX - CURVE_CIRCLE_RADIUS, 0,
                bezierX - CURVE_CIRCLE_RADIUS, CURVE_CIRCLE_RADIUS * 0.95f,
                bezierX, CURVE_CIRCLE_RADIUS * 0.95f
        );

        mTopPath.cubicTo(
                bezierX + CURVE_CIRCLE_RADIUS, CURVE_CIRCLE_RADIUS * 0.95f,
                bezierX + CURVE_CIRCLE_RADIUS, 0,
                bezierX + controlDistance, 0
        );

        mTopPath.lineTo(w, 0);


        mPath.reset();
        mPath.addPath(mTopPath);
        mPath.lineTo(w, h);
        mPath.lineTo(0, h);
        mPath.close();

        canvas.drawPath(mPath, mPaint);
        canvas.drawPath(mTopPath, mStrokePaint);
    }
}
