package com.upreyvan.carti.utils;

import com.upreyvan.carti.CartiApplication;
import com.upreyvan.carti.R;
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.models.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionHelper {

    public static Transaction parse(Map<String, Object> data, String id, String createdAt, String updatedAt) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAmount(CurrencyHelper.parse(data.get("amount")));
        t.setType((String) data.get("type"));
        t.setCategory((String) data.get("category"));
        String note = data.containsKey("note") ? (String) data.get("note") : (String) data.get("description");
        t.setNote(note);
        t.setFamilyId((String) data.get("familyId"));
        t.setUserId((String) data.get("userId"));
        t.setUsername((String) data.get("username"));
        t.setTitle((String) data.get("title"));
        t.setStartDate((String) data.get("startDate"));
        t.setCreatedAt(createdAt);
        t.setUpdatedAt(updatedAt);
        
        String status = (String) data.get("status");
        if (status == null || "PENDING".equalsIgnoreCase(status) || "SYNCING".equalsIgnoreCase(status)) {
            String type = (String) data.get("type");
            status = "DEBT".equalsIgnoreCase(type) ? "unpaid" : "active";
        }
        t.setStatus(status);

        t.setPaid(Boolean.TRUE.equals(data.get("isPaid")));
        t.setRecurring(Boolean.TRUE.equals(data.get("isRecurring")));
        t.setTargetAmount(CurrencyHelper.parse(data.get("targetAmount")));
        t.setTargetDate((String) data.get("targetDate"));
        t.setAllocatedTo((String) data.get("allocatedTo"));
        t.setAllocationMonth((String) data.get("allocationMonth"));
        t.setSubCategory((String) data.get("sub_category"));
        t.setIconUrl((String) data.get("iconUrl"));
        
        Object iconResObj = data.get("iconRes");
        if (iconResObj instanceof Number) {
            t.setIconRes(((Number) iconResObj).intValue());
        }

        // Centralized style hydration
        hydrateStyle(t);

        Object membersObj = data.get("members");
        if (membersObj instanceof List<?>) {
            List<String> members = new ArrayList<>();
            for (Object item : (List<?>) membersObj) {
                if (item instanceof String) {
                    members.add((String) item);
                }
            }
            t.setMembers(members);
        }
        
        Object likesCountObj = data.get("likesCount");
        if (likesCountObj instanceof Number) {
            t.setLikesCount(((Number) likesCountObj).intValue());
        }

        Object commentCountObj = data.get("commentCount");
        if (commentCountObj instanceof Number) {
            t.setCommentCount(((Number) commentCountObj).intValue());
        }

        try {
            if (createdAt != null) {
                t.setTimestampMillis(DateHelper.getMillisFromIso(createdAt));
            }
        } catch (Exception ignored) {}
        
        return t;
    }

    public static void hydrateStyle(Transaction t) {
        if (t == null) return;
        
        CategoryManager cm = CategoryManager.getInstance(CartiApplication.getAppContext());
        
        // Use sub-category first for more specific styling, then category
        String catName = (t.getSubCategory() != null && !t.getSubCategory().isEmpty()) ? t.getSubCategory() : t.getCategory();
        Category c = cm.getCategoryByName(catName);
        
        // Special handling for goal analysis contributions
        if ("allocated".equalsIgnoreCase(t.getCategory()) && "GOAL".equalsIgnoreCase(t.getType())) {
            t.setIconColor(R.color.carti_primary_green);
            t.setIconBgColor(R.color.mint_green_alpha);
            if (t.getIconRes() == 0) t.setIconRes(R.drawable.ic_chart);
            return;
        }

        if (c != null) {
            t.setIconColor(c.getIconColor());
            t.setIconBgColor(c.getBackgroundColor());
            if (t.getIconRes() == 0 && (t.getIconUrl() == null || t.getIconUrl().isEmpty())) {
                t.setIconRes(c.getIconRes());
            }
        } else {
            // Default styles based on transaction type if category not found
            String type = t.getType() != null ? t.getType().toUpperCase() : "EXPENSE";
            switch (type) {
                case "GOAL", "ALLOCATION"-> {
                    t.setIconColor(R.color.carti_primary_green);
                    t.setIconBgColor(R.color.mint_green_alpha);
                    if (t.getIconRes() == 0) t.setIconRes(R.drawable.ic_chart);
                }
                case "DEBT" -> {
                    t.setIconColor(R.color.status_red);
                    t.setIconBgColor(R.color.status_red_tonal);
                    if (t.getIconRes() == 0) t.setIconRes(R.drawable.ic_chart);
                }
                default -> {
                    t.setIconColor(R.color.white);
                    t.setIconBgColor(R.color.gray);
                    if (t.getIconRes() == 0) t.setIconRes(R.drawable.ic_chart);
                }
            }
        }
    }
}
