package com.upreyvan.carti.utils;

import androidx.annotation.ColorRes;
import com.upreyvan.carti.R;
import java.util.HashMap;
import java.util.Map;

public class ReactionHelper {

    public static final String REAC_LIKE = "👍";
    public static final String REAC_LOVE = "❤️";
    public static final String REAC_HAHA = "😂";
    public static final String REAC_WOW = "😮";
    public static final String REAC_SAD = "😢";
    public static final String REAC_ANGRY = "😡";

    private static final Map<String, String> LABEL_MAP = new HashMap<>();
    private static final Map<String, Integer> COLOR_MAP = new HashMap<>();

    static {
        LABEL_MAP.put(REAC_LIKE, "Like");
        LABEL_MAP.put(REAC_LOVE, "Love");
        LABEL_MAP.put(REAC_HAHA, "Haha");
        LABEL_MAP.put(REAC_WOW, "Wow");
        LABEL_MAP.put(REAC_SAD, "Sad");
        LABEL_MAP.put(REAC_ANGRY, "Angry");

        COLOR_MAP.put(REAC_LIKE, R.color.carti_primary_blue);
        COLOR_MAP.put(REAC_LOVE, R.color.status_red);
        COLOR_MAP.put(REAC_HAHA, R.color.icon_electricity);
        COLOR_MAP.put(REAC_WOW, R.color.icon_electricity);
        COLOR_MAP.put(REAC_SAD, R.color.icon_fare);
        COLOR_MAP.put(REAC_ANGRY, R.color.status_red);
    }

    public static String getLabel(String emoji) {
        return LABEL_MAP.getOrDefault(emoji, "Like");
    }

    @ColorRes
    public static int getColor(String emoji) {
        return COLOR_MAP.getOrDefault(emoji, R.color.icon_electricity);
    }

    public static String getEmoji(int viewId) {
        if (viewId == R.id.reac_love) return REAC_LOVE;
        if (viewId == R.id.reac_haha) return REAC_HAHA;
        if (viewId == R.id.reac_wow) return REAC_WOW;
        if (viewId == R.id.reac_sad) return REAC_SAD;
        if (viewId == R.id.reac_angry) return REAC_ANGRY;
        return REAC_LIKE;
    }
}
