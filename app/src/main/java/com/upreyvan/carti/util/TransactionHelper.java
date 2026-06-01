package com.upreyvan.carti.util;

import com.upreyvan.carti.model.Transaction;
import java.util.Map;

public class TransactionHelper {

    public static Transaction parse(Map<String, Object> data, String id, String createdAt, String updatedAt) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAmount(CurrencyHelper.parse(data.get("amount")));
        t.setType((String) data.get("type"));
        t.setCategory((String) data.get("category"));
        t.setDescription((String) data.get("description"));
        t.setFamilyId((String) data.get("familyId"));
        t.setUserId((String) data.get("userId"));
        t.setUsername((String) data.get("username"));
        t.setCreatedAt(createdAt);
        t.setUpdatedAt(updatedAt);
        
        try {
            if (createdAt != null) {
                t.setTimestampMillis(DateHelper.getMillisFromIso(createdAt));
            }
        } catch (Exception ignored) {}
        
        return t;
    }
}
