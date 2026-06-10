package com.upreyvan.carti.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.utils.Constants;
import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static ChatRepository instance;
    private final ApiHelper apiHelper;
    private final RealtimeRepository realtimeRepo;
    private final PreferenceManager pref;
    private final AppwriteManager appwriteManager;

    private ChatRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.realtimeRepo = RealtimeRepository.getInstance(context);
        this.pref = PreferenceManager.getInstance(context);
        this.appwriteManager = AppwriteManager.getInstance(context);
    }

    public static synchronized ChatRepository getInstance(Context context) {
        if (instance == null) instance = new ChatRepository(context);
        return instance;
    }

    public void sendMessage(String id, String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        apiHelper.sendMessageWithId(id, text, callback);
    }

    public void sendAiMessage(String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        apiHelper.sendAiMessage(text, callback);
    }

    public LiveData<Map<String, Object>> getChatStream() {
        return realtimeRepo.getChatStream();
    }

    public void loadHistory(int pageSize, long oldestTimestamp, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) {
            callback.onError(new Exception("No family session found."));
            return;
        }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.orderDesc("timestamp"));
        queries.add(Query.Companion.limit(pageSize));

        if (oldestTimestamp != Long.MAX_VALUE) {
            queries.add(Query.Companion.lessThan("timestamp", oldestTimestamp));
        }

        appwriteManager.listDocuments(
            Constants.Appwrite.DATABASE_ID,
            Constants.Appwrite.COL_MESSAGES,
            queries,
            callback
        );
    }
}
