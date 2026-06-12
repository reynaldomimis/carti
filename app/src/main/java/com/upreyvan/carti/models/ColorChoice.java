package com.upreyvan.carti.models;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class ColorChoice {
    private final @ColorRes int colorRes;
    private final @ColorRes int bgColorRes;

    public ColorChoice(int colorRes, int bgColorRes) {
        this.colorRes = colorRes;
        this.bgColorRes = bgColorRes;
    }

    public int getColorRes() { return colorRes; }
    public int getBgColorRes() { return bgColorRes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorChoice that = (ColorChoice) o;
        return colorRes == that.colorRes && bgColorRes == that.bgColorRes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorRes, bgColorRes);
    }

    public static final DiffUtil.ItemCallback<ColorChoice> DIFF_CALLBACK = new DiffUtil.ItemCallback<ColorChoice>() {
        @Override
        public boolean areItemsTheSame(@NonNull ColorChoice oldItem, @NonNull ColorChoice newItem) {
            return oldItem.colorRes == newItem.colorRes;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ColorChoice oldItem, @NonNull ColorChoice newItem) {
            return oldItem.equals(newItem);
        }
    };
}
