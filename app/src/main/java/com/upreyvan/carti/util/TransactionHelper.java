package com.upreyvan.carti.util;

import com.upreyvan.carti.model.Transaction;
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
        t.setStatus((String) data.get("status"));
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
}
