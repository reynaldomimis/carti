package com.upreyvan.carti.util;

import com.upreyvan.carti.model.Transaction;
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
        t.setTargetAmount(CurrencyHelper.parse(data.get("targetAmount")));
        t.setTargetDate((String) data.get("targetDate"));
        t.setAllocatedTo((String) data.get("allocatedTo"));
        t.setAllocationMonth((String) data.get("allocationMonth"));
        t.setIconUrl((String) data.get("iconUrl"));
        
        if (data.get("iconRes") != null) {
            t.setIconRes(((Number) data.get("iconRes")).intValue());
        }

        if (data.get("members") instanceof List) {
            t.setMembers((List<String>) data.get("members"));
        }
        
        if (data.get("likesCount") != null) t.setLikesCount(((Number) data.get("likesCount")).intValue());
        if (data.get("commentCount") != null) t.setCommentCount(((Number) data.get("commentCount")).intValue());

        try {
            if (createdAt != null) {
                t.setTimestampMillis(DateHelper.getMillisFromIso(createdAt));
            }
        } catch (Exception ignored) {}
        
        return t;
    }
}
