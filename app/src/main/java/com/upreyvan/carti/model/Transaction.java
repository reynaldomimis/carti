package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.upreyvan.carti.data.local.db.Converters;

import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.upreyvan.carti.R;
import androidx.core.content.ContextCompat;
import android.content.Context;

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
    private String username;
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
    private String lastEmoji;
    private String allocatedTo;
    private String allocationMonth;

    @Ignore
    private String reactorNames;

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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public String getLastEmoji() { return lastEmoji; }
    public void setLastEmoji(String lastEmoji) { this.lastEmoji = lastEmoji; }

    public String getAllocatedTo() { return allocatedTo; }
    public void setAllocatedTo(String allocatedTo) { this.allocatedTo = allocatedTo; }

    public String getAllocationMonth() { return allocationMonth; }
    public void setAllocationMonth(String allocationMonth) { this.allocationMonth = allocationMonth; }

    public String getReactorNames() { return reactorNames; }
    public void setReactorNames(String reactorNames) { this.reactorNames = reactorNames; }

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
                Objects.equals(description, that.description) &&
                Objects.equals(category, that.category) &&
                Objects.equals(status, that.status) &&
                Objects.equals(lastEmoji, that.lastEmoji) &&
                Objects.equals(reactorNames, that.reactorNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, amount, title, description, category, status, timestampMillis, likesCount, commentCount, lastEmoji, reactorNames);
    }

    public static Transaction fromPayload(Map<String, Object> data, String familyId, Context context, String currentUserId) {
        try {
            String id = String.valueOf(data.get("$id"));
            String createdAt = data.containsKey("startDate") ? String.valueOf(data.get("startDate")) : String.valueOf(data.get("$createdAt"));
            String updatedAt = String.valueOf(data.get("$updatedAt"));
            
            String type = "EXPENSE";
            if (data.containsKey("type")) type = String.valueOf(data.get("type")).toUpperCase();
            else if (data.containsKey("source")) type = "INCOME";
            else if (data.containsKey("targetAmount")) type = "GOAL";
            else if (data.containsKey("personName")) type = "DEBT";

            double amount = Utils.getDouble(data.get("amount"));
            double targetAmount = Utils.getDouble(data.get("targetAmount"));
            String categoryName = data.containsKey("category") ? String.valueOf(data.get("category")) : type;
            
            String userId = data.containsKey("userId") ? String.valueOf(data.get("userId")) : currentUserId;
            String username = data.containsKey("username") ? String.valueOf(data.get("username")) : 
                            (data.containsKey("userName") ? String.valueOf(data.get("userName")) : "someone");
            if (username != null) username = username.toLowerCase();
            
            String title = data.containsKey("title") ? String.valueOf(data.get("title")) : 
                         (data.containsKey("source") ? String.valueOf(data.get("source")) : categoryName);
            
            String description = data.containsKey("description") ? String.valueOf(data.get("description")) : 
                               (data.containsKey("note") ? String.valueOf(data.get("note")) : "");
            
            boolean isPaid = false;
            if (data.containsKey("isPaid")) isPaid = (Boolean) data.get("isPaid");

            List<String> members = new ArrayList<>();
            if (data.get("members") instanceof List) {
                for (Object item : (List<?>) data.get("members")) members.add(String.valueOf(item));
            }

            int likesCount = data.containsKey("likesCount") ? ((Number) data.get("likesCount")).intValue() : 0;
            int commentCount = data.containsKey("commentCount") ? ((Number) data.get("commentCount")).intValue() : 0;
            String lastEmoji = data.containsKey("lastEmoji") ? String.valueOf(data.get("lastEmoji")) : null;
            
            int iconRes = R.drawable.ic_person;
            int iconColor = ContextCompat.getColor(context, R.color.carti_primary_green);

            if ("INCOME".equals(type)) { iconRes = R.drawable.ic_arrow_up; iconColor = ContextCompat.getColor(context, R.color.dash_green); }
            else if ("GOAL".equals(type)) { iconRes = R.drawable.ic_trophy; iconColor = ContextCompat.getColor(context, R.color.mint_green); }
            else if ("DEBT".equals(type)) { iconRes = R.drawable.ic_lock; iconColor = ContextCompat.getColor(context, R.color.status_red); }

            int bgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(iconColor, 25);
            
            Transaction transaction = new Transaction(
                id, type, amount, title, description, categoryName, familyId, userId,
                createdAt, updatedAt, targetAmount, null, "completed", isPaid, members,
                null, iconRes, bgColor, iconColor, Utils.getMillisFromIso(createdAt),
                likesCount, commentCount
            );
            transaction.setUsername(username);
            transaction.setLastEmoji(lastEmoji);
            return transaction;
        } catch (Exception e) {
            return null;
        }
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
