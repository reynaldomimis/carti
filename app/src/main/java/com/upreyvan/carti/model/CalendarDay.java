package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class CalendarDay {
    private final String id;
    private final String day;
    private final boolean isSelected;
    private final boolean isToday;
    private final boolean hasBill;

    public CalendarDay(String day, boolean isSelected, boolean isToday, boolean hasBill) {
        this.id = day.isEmpty() ? "pad_" + Math.random() : day;
        this.day = day;
        this.isSelected = isSelected;
        this.isToday = isToday;
        this.hasBill = hasBill;
    }

    private CalendarDay(String id, String day, boolean isSelected, boolean isToday, boolean hasBill) {
        this.id = id;
        this.day = day;
        this.isSelected = isSelected;
        this.isToday = isToday;
        this.hasBill = hasBill;
    }

    public String getId() { return id; }
    public String getDay() { return day; }
    public boolean isSelected() { return isSelected; }
    public boolean isToday() { return isToday; }
    public boolean hasBill() { return hasBill; }

    public CalendarDay withSelected(boolean selected) {
        return new CalendarDay(this.id, this.day, selected, this.isToday, this.hasBill);
    }

    public static final DiffUtil.ItemCallback<CalendarDay> DIFF_CALLBACK = new DiffUtil.ItemCallback<CalendarDay>() {
        @Override
        public boolean areItemsTheSame(@NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
            return oldItem.isSelected == newItem.isSelected &&
                    oldItem.isToday == newItem.isToday &&
                    oldItem.hasBill == newItem.hasBill;
        }
    };
}
