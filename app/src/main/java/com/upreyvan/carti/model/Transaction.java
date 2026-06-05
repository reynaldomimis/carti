package com.upreyvan.carti.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.db.Converters;
import com.upreyvan.carti.util.TransactionHelper;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity(tableName = "transactions", indices = {
        @Index("familyId"),
        @Index("userId"),
        @Index("type")
})
@TypeConverters(Converters.class)
public class Transaction {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String type;
    private String note;
    private String familyId;
    private String username;
    private String startDate;
    private String targetDate;
    private boolean isPaid;
    private double targetAmount;
    private String status;
    private List<String> members;
    private int likesCount;
    private int commentCount;
    private String allocatedTo;
    private String allocationMonth;
    private String iconUrl;
    private int iconRes;
    private String title;
    
    private String createdAt;
    private String updatedAt;

    // Local-only UI fields
    private int iconBgColor;
    private int iconColor;
    private long timestampMillis;
    private String reminder;
    private String lastEmoji;

    @Ignore
    private String reactorNames;

    public Transaction() {
        this.id = "";
        this.type = "EXPENSE";
        this.category = "General";
        this.status = "active";
        this.members = new ArrayList<>();
        this.isPaid = false;
        this.startDate = Utils.getCurrentTimestamp();
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public String getAllocatedTo() { return allocatedTo; }
    public void setAllocatedTo(String allocatedTo) { this.allocatedTo = allocatedTo; }

    public String getAllocationMonth() { return allocationMonth; }
    public void setAllocationMonth(String allocationMonth) { this.allocationMonth = allocationMonth; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // UI and Legacy helpers
    public int getIconBgColor() { return iconBgColor; }
    public void setIconBgColor(int iconBgColor) { this.iconBgColor = iconBgColor; }

    public int getIconColor() { return iconColor; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public String getLastEmoji() { return lastEmoji; }
    public void setLastEmoji(String lastEmoji) { this.lastEmoji = lastEmoji; }

    public String getReactorNames() { return reactorNames; }
    public void setReactorNames(String reactorNames) { this.reactorNames = reactorNames; }

    public static Transaction fromPayload(Map<String, Object> payload, String familyId, Context context, String userId) {
        if (payload == null) return null;

        String id = String.valueOf(payload.get("$id"));
        String createdAt = String.valueOf(payload.get("$createdAt"));
        String updatedAt = String.valueOf(payload.get("$updatedAt"));

        Transaction t = TransactionHelper.parse(payload, id, createdAt, updatedAt);

        if (t.getFamilyId() == null || t.getFamilyId().isEmpty()) t.setFamilyId(familyId);
        if (t.getUserId() == null || t.getUserId().isEmpty()) t.setUserId(userId);

        if (context != null) {
            t.setIconBgColor(Utils.getCategoryColor(context, t.getCategory()));
            t.setIconColor(context.getColor(R.color.white));
        }

        return t;
    }

    @Ignore
    public int getProgress() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, (amount / targetAmount) * 100);
    }

    @Ignore
    public boolean isCompleted() {
        if ("GOAL".equals(type)) return amount >= targetAmount && targetAmount > 0;
        return isPaid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Double.compare(that.amount, amount) == 0 &&
                Double.compare(that.targetAmount, targetAmount) == 0 &&
                isPaid == that.isPaid &&
                likesCount == that.likesCount &&
                commentCount == that.commentCount &&
                Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(note, that.note) &&
                Objects.equals(category, that.category) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, amount, title, note, category, status, likesCount, commentCount);
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
