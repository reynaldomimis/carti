package com.upreyvan.carti.model;

public class FaqItem {
    private String question;
    private String answer;

    public FaqItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaqItem faqItem = (FaqItem) o;
        return java.util.Objects.equals(question, faqItem.question) &&
                java.util.Objects.equals(answer, faqItem.answer);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(question, answer);
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<FaqItem> DIFF_CALLBACK =
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<FaqItem>() {
                @Override
                public boolean areItemsTheSame(@androidx.annotation.NonNull FaqItem oldItem, @androidx.annotation.NonNull FaqItem newItem) {
                    return oldItem.question.equals(newItem.question);
                }

                @Override
                public boolean areContentsTheSame(@androidx.annotation.NonNull FaqItem oldItem, @androidx.annotation.NonNull FaqItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
