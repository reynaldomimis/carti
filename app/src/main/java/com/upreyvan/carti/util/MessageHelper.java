package com.upreyvan.carti.util;

import com.upreyvan.carti.model.ChatMessage;
import java.util.Map;
import java.util.UUID;

public class MessageHelper {

    public static ChatMessage mapToChatMessage(Map<String, Object> map, String docId, String currentUserId, String currentUsername) {
        String id = (docId == null || docId.equals("null")) ? "msg_" + UUID.randomUUID() : docId;
        String senderId = map.get("senderId") != null ? String.valueOf(map.get("senderId")).trim() : "";
        String senderName = map.get("senderName") != null ? String.valueOf(map.get("senderName")).trim() : "";
        String familyId = String.valueOf(map.get("familyId"));
        String text = String.valueOf(map.get("text"));
        long ts = 0;
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Number) ts = ((Number) tsObj).longValue();
        else if (tsObj instanceof String) {
            try { ts = Long.parseLong((String) tsObj); } catch (Exception ignored) {}
        }
        if (ts == 0) ts = System.currentTimeMillis();
        boolean isMe = senderId.equalsIgnoreCase(currentUserId) || senderName.equalsIgnoreCase(currentUsername);
        return new ChatMessage(id, senderId, familyId, senderName, text, ts, isMe);
    }
}
