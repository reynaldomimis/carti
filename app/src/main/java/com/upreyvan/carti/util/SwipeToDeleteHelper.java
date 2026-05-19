package com.upreyvan.carti.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.upreyvan.carti.R;

public class SwipeToDeleteHelper {

    public interface OnSwipeListener {
        void onSwiped(int position);
    }

    public static void attach(RecyclerView recyclerView, OnSwipeListener listener) {
        attach(recyclerView, R.color.status_red, R.drawable.ic_close, listener);
    }

    public static void attach(RecyclerView recyclerView, int backgroundColorRes, int iconRes, OnSwipeListener listener) {
        new ItemTouchHelper(new SwipeCallback(recyclerView.getContext(), backgroundColorRes, iconRes, listener)).attachToRecyclerView(recyclerView);
    }

    public static void showUndoSnackbar(@NonNull View view, String message, Runnable onUndo, Runnable onDelete) {
        Snackbar snackbar = Snackbar.make(view, message, 3000); // 3 seconds
        snackbar.setAction("UNDO", v -> {
            if (onUndo != null) onUndo.run();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    if (onDelete != null) onDelete.run();
                }
            }
        });
        snackbar.show();
    }

    private static class SwipeCallback extends ItemTouchHelper.SimpleCallback {
        private final OnSwipeListener listener;
        private final Drawable deleteIcon;
        private final int intrinsicWidth;
        private final int intrinsicHeight;
        private final ColorDrawable background;
        private final int backgroundColor;
        private final Paint clearPaint;

        SwipeCallback(Context context, int backgroundColorRes, int iconRes, OnSwipeListener listener) {
            super(0, ItemTouchHelper.LEFT);
            this.listener = listener;
            this.background = new ColorDrawable();
            this.backgroundColor = ContextCompat.getColor(context, backgroundColorRes);
            this.clearPaint = new Paint();
            this.clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            this.deleteIcon = ContextCompat.getDrawable(context, iconRes);
            if (this.deleteIcon != null) {
                this.intrinsicWidth = this.deleteIcon.getIntrinsicWidth();
                this.intrinsicHeight = this.deleteIcon.getIntrinsicHeight();
            } else {
                this.intrinsicWidth = 0;
                this.intrinsicHeight = 0;
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            listener.onSwiped(viewHolder.getBindingAdapterPosition());
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getBottom() - itemView.getTop();
            boolean isCanceled = dX == 0f && !isCurrentlyActive;

            if (isCanceled) {
                clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }

            // Draw red background
            background.setColor(backgroundColor);
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            // Draw icon
            if (deleteIcon != null) {
                int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
                int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
                int deleteIconRight = itemView.getRight() - deleteIconMargin;
                int deleteIconBottom = deleteIconTop + intrinsicHeight;

                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                deleteIcon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
            c.drawRect(left, top, right, bottom, clearPaint);
        }
    }
}
