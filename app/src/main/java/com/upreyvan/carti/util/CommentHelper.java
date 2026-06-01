package com.upreyvan.carti.util;

import com.upreyvan.carti.model.Comment;
import java.util.Map;

public class CommentHelper {

    public static Comment parse(Map<String, Object> data) {
        String username = data.get("username") != null && !"null".equals(String.valueOf(data.get("username")))
                ? String.valueOf(data.get("username")) : "Anonymous";
        
        return new Comment(
                safeString(data.get("$id")),
                safeString(data.get("transactionId")),
                safeString(data.get("userId")),
                username,
                safeString(data.get("text")),
                data.get("parentId") != null && !"null".equals(String.valueOf(data.get("parentId")))
                        ? String.valueOf(data.get("parentId")) : null,
                safeString(data.get("$createdAt")),
                safeString(data.get("$updatedAt"))
        );
    }

    private static String safeString(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj);
        return "null".equals(s) ? "" : s;
    }
}
