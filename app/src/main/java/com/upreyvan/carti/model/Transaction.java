package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.upreyvan.carti.data.local.db.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "transactions")
@TypeConverters(Converters.class)
public class Transaction {
    @PrimaryKey
    @NonNull
    private String id;
    private String type;
    private double amount;
    private String title;
    private String description;
    private String category;
    private String familyId;
    private String userId;
    private String createdAt;
    private String updatedAt;


    private double targetAmount;
    private String dueDate;
    private String status;
    private boolean isPaid;
    private List<String> members;
    private String reminder;

    private int iconRes;
    private int iconBgColor;
    private int iconColor;
    private long timestampMillis;

    private int likesCount;
    private int commentCount;

    @Ignore
    public Transaction() {
        this.id = "";
        this.type = "EXPENSE";
        this.category = "General";
        this.status = "active";
        this.members = new ArrayList<>();
    }

    public Transaction(@NonNull String id, String type, double amount, String title, String description, String category, String familyId, String userId, String createdAt, String updatedAt, double targetAmount, String dueDate, String status, boolean isPaid, List<String> members, String reminder, int iconRes, int iconBgColor, int iconColor, long timestampMillis, int likesCount, int commentCount) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.title = title;
        this.description = description;
        this.category = category;
        this.familyId = familyId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.targetAmount = targetAmount;
        this.dueDate = dueDate;
        this.status = status;
        this.isPaid = isPaid;
        this.members = members != null ? members : new ArrayList<>();
        this.reminder = reminder;
        this.iconRes = iconRes;
        this.iconBgColor = iconBgColor;
        this.iconColor = iconColor;
        this.timestampMillis = timestampMillis;
        this.likesCount = likesCount;
        this.commentCount = commentCount;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    @Ignore
    public String getName() { return title; }
    @Ignore
    public void setName(String name) { this.title = name; }

    @Ignore
    public String getTargetDate() { return dueDate; }
    @Ignore
    public void setTargetDate(String targetDate) { this.dueDate = targetDate; }

    @Ignore
    public String getNote() { return description; }
    @Ignore
    public void setNote(String note) { this.description = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public int getIconBgColor() { return iconBgColor; }
    public void setIconBgColor(int iconBgColor) { this.iconBgColor = iconBgColor; }

    public int getIconColor() { return iconColor; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    @Ignore
    public int getProgress() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, (amount / targetAmount) * 100);
    }

    @Ignore
    public boolean isCompleted() {
        return amount >= targetAmount && targetAmount > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Double.compare(that.amount, amount) == 0 &&
                Double.compare(that.targetAmount, targetAmount) == 0 &&
                isPaid == that.isPaid &&
                timestampMillis == that.timestampMillis &&
                likesCount == that.likesCount &&
                commentCount == that.commentCount &&
                Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(category, that.category) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, amount, title, category, status, timestampMillis);
    }

    public static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.equals(newItem);
        }
    };
}
