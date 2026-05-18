package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class FaqItem {
    private String question;
    private String answer;
    private boolean isExpanded;

    public FaqItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.isExpanded = false;
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaqItem faqItem = (FaqItem) o;
        return isExpanded == faqItem.isExpanded &&
                Objects.equals(question, faqItem.question) &&
                Objects.equals(answer, faqItem.answer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, answer, isExpanded);
    }

    public static final DiffUtil.ItemCallback<FaqItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<FaqItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FaqItem oldItem, @NonNull FaqItem newItem) {
            return oldItem.question.equals(newItem.question);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FaqItem oldItem, @NonNull FaqItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
