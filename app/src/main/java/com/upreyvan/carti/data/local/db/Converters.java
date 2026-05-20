package com.upreyvan.carti.data.local.db;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    @TypeConverter
    public static List<String> fromString(String value) {
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> list = new Gson().fromJson(value, listType);
        return list != null ? list : new ArrayList<>();
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}
