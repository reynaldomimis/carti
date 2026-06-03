package com.upreyvan.carti.ui.family;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.ai.LocalIntentParser;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.data.repository.ChatRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.data.ai.CategoryMapper;
import com.upreyvan.carti.data.ai.CategoryValidator;
import com.upreyvan.carti.util.Utils;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.CountDownTimer;

public class ChatViewModel extends BaseViewModel {
    private final ChatRepository chatRepo;
    private final AiRepository aiRepo;
    private final TransactionRepository transRepo;
    private final PreferenceManager pref;
    
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAiThinking = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>(null);
    private final MutableLiveData<Long> sessionTimeRemaining = new MutableLiveData<>(0L);
    
    private long oldestTimestamp = Long.MAX_VALUE;
    private static final int PAGE_SIZE = 20;
    private JSONObject pendingTransaction = null;
    private boolean isClarifying = false;
    private CountDownTimer sessionTimer;
    private static final long SESSION_DURATION = 60000;
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?i)@\\s*\\w+");

    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepo = ChatRepository.getInstance(application);
        aiRepo = AiRepository.getInstance(application);
        transRepo = TransactionRepository.getInstance(application);
        pref = PreferenceManager.getInstance(application);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getIsAiThinking() { return isAiThinking; }
    public LiveData<String> getError() { return errorMsg; }
    public LiveData<Long> getSessionTimeRemaining() { return sessionTimeRemaining; }
    public void clearError() { errorMsg.setValue(null); }

    public void loadHistory(boolean loadMore) {
        if (Objects.equals(isLoading.getValue(), true)) return;
        setLoading(true);
        if (!loadMore) oldestTimestamp = Long.MAX_VALUE;

        chatRepo.loadHistory(PAGE_SIZE, oldestTimestamp, new AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                List<ChatMessage> batch = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    batch.add(mapToChatMessage(doc.getData(), doc.getId()));
                }

                if (batch.isEmpty()) {
                    setLoading(false);
                    return;
                }

                ChatMessage oldestInBatch = batch.get(batch.size() - 1);
                oldestTimestamp = oldestInBatch.getTimestamp();
                Collections.reverse(batch);

                List<ChatMessage> currentMessages = new ArrayList<>(Objects.requireNonNullElse(messages.getValue(), new ArrayList<>()));
                if (loadMore) {
                    List<ChatMessage> newList = new ArrayList<>(batch);
                    newList.addAll(currentMessages);
                    messages.postValue(newList);
                } else {
                    messages.postValue(batch);
                }
                setLoading(false);
            }
            @Override public void onError(Throwable error) {
                errorMsg.postValue(error.getMessage());
                setLoading(false);
            }
        });
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        
        String uuid = UUID.randomUUID().toString().replace("-", ""); 
        String clientSideId = "msg_" + uuid;

        addMessage(new ChatMessage(clientSideId, pref.getUserId(), pref.getFamilyId(), pref.getUsername(), text, System.currentTimeMillis(), true));
        
        chatRepo.sendMessage(clientSideId, text, new AppwriteCallback<>() {
            @Override public void onSuccess(Document<Map<String, Object>> result) {
                boolean hasMention = MENTION_PATTERN.matcher(text).find();
                boolean isSessionActive = sessionTimeRemaining.getValue() != null && sessionTimeRemaining.getValue() > 0;

                if (hasMention) {
                    startSession();
                    if (text.trim().replaceAll("\\s+", "").equalsIgnoreCase("@carti") && pref.isAiIntroDone()) {
                        sendAiResponse("Yes?");
                        return;
                    }
                }

                if (hasMention || isSessionActive) {
                    startSession();
                    if (pendingTransaction != null) {
                        handleClarificationReply(text);
                    } else {
                        processIntent(text);
                    }
                }
            }
            @Override public void onError(Throwable error) {
                errorMsg.postValue("Failed to send: " + error.getMessage());
            }
        });
    }

    private void processIntent(String text) {
        JSONObject freshParse = LocalIntentParser.parse(text);
        String status = freshParse != null ? freshParse.optString("status") : "UNKNOWN";
        
        if (status.equals("LOG")) {
            isClarifying = false;
            handleParsedIntent(freshParse);
        } else if (status.equals("BLOCK")) {
            isClarifying = false;
            sendAiResponse("I can only help with financial tracking. Please send an expense, income, or transaction.");
        } else if (status.equals("PENDING")) {
            pendingTransaction = freshParse;
            sendAiResponse(freshParse.optString("message"));
        } else if (status.equals("INSIGHT")) {
            isClarifying = false;
            processAiChat("Task: Provide insight for " + freshParse.optString("type"));
        } else {
            if (!isClarifying) {
                isClarifying = true;
                sendAiResponse("I couldn't identify those words. Could you please clarify?");
            } else {
                isClarifying = false;
                processAiChat(text); 
            }
        }
    }

    private void startSession() {
        if (sessionTimer != null) sessionTimer.cancel();
        sessionTimer = new CountDownTimer(SESSION_DURATION, 1000) {
            @Override public void onTick(long millisUntilFinished) { sessionTimeRemaining.postValue(millisUntilFinished); }
            @Override public void onFinish() { sessionTimeRemaining.postValue(0L); }
        }.start();
    }

    private void handleClarificationReply(String reply) {
        try {
            String state = pendingTransaction.optString("state");
            double amount = pendingTransaction.optDouble("amount");
            String item = pendingTransaction.optString("item");

            if (Objects.equals(state, "NEED_ITEM")) {
                item = reply;
            } else if (Objects.equals(state, "NEED_AMOUNT")) {
                JSONObject p = LocalIntentParser.parse(reply);
                amount = (p != null) ? p.optDouble("amount", 0) : 0;
                if (amount <= 0) amount = extractAmountFromText(reply);
            }

            if (amount > 0 && !item.isEmpty() && item.length() >= 2) {
                final double finalAmt = amount;
                final String finalItem = item;
                String initialCategory = CategoryMapper.getCategory(item, null);
                
                CategoryValidator.validate(getApplication(), item, initialCategory, validatedCategory -> {
                    try {
                        JSONObject finalized = new JSONObject();
                        finalized.put("type", "EXPENSE_LOG");
                        finalized.put("status", "LOG");
                        finalized.put("isTransaction", true);
                        finalized.put("amount", finalAmt);
                        JSONObject ext = new JSONObject();
                        ext.put("item", finalItem);
                        ext.put("category", validatedCategory);
                        finalized.put("extracted", ext);
                        logTransaction(finalized);
                    } catch (Exception e) {
                        Log.e("ChatViewModel", "Finalization error", e);
                    }
                });
                pendingTransaction = null;
            } else {
                pendingTransaction = null;
                // Important: Don't call sendMessage(reply) again to avoid recursion/duplication.
                // Just process the existing reply as a new intent.
                processIntent(reply); 
            }
        } catch (Exception e) {
            pendingTransaction = null;
        }
    }

    private void handleParsedIntent(JSONObject parse) {
        if ("LOG".equals(parse.optString("status"))) {
            logTransaction(parse);
        }
    }

    private void logTransaction(JSONObject parse) {
        JSONObject data = parse.optJSONObject("extracted");
        if (data == null) return;

        double finalAmount = parse.optDouble("amount", 0);
        if (finalAmount <= 0) finalAmount = extractAmountFromText(data.optString("item")); 

        String item = data.optString("item");
        String category = data.optString("category");
        String intentType = parse.optString("type");
        
        String type;
        if (Objects.equals(intentType, "INCOME_LOG") || Objects.equals(intentType, "ALLOCATION_LOG")) {
            type = "INCOME";
        } else {
            type = "EXPENSE";
        }

        final double amt = finalAmount;
        performAddTransaction(amt, type, category != null ? category : "Others", item);
    }

    private void performAddTransaction(double amt, String type, String category, String item) {
        transRepo.addTransaction(amt, type, category, item, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> map) {
                sendAiResponse("Got it! Recorded " + item + " (" + Utils.formatCurrency(amt) + ") under " + category + ".");
            }
            @Override public void onError(Throwable error) {
                Log.e("ChatViewModel", "Transaction error", error);
            }
        });
    }

    private double extractAmountFromText(String text) {
        try {
             Pattern p = Pattern.compile("\\d+");
             Matcher m = p.matcher(text);
             if (m.find()) return Double.parseDouble(m.group());
        } catch (Exception ignored) {}
        return 0;
    }

    private void processAiChat(String text) {
        isAiThinking.postValue(true);
        aiRepo.processChat(text, "", messages.getValue(), true, new AiRepository.AiCallback() {
            @Override public void onSuccess(String response) {
                sendAiResponse(response);
                isAiThinking.postValue(false);
            }
            @Override public void onError(Throwable t) { 
                isAiThinking.postValue(false);
                errorMsg.postValue("AI Error: " + t.getMessage()); 
            }
            @Override public void onActionDetected(JSONObject action) {
                isAiThinking.postValue(false);
                String actionType = action.optString("type");
                if (!actionType.contains("TRANSACTION") && !actionType.contains("LOG")) {
                    aiRepo.executeAction(action);
                }
            }
        });
    }

    private void sendAiResponse(String text) {
        chatRepo.sendAiMessage(text, new AppwriteCallback<>() {
            @Override public void onSuccess(Document<Map<String, Object>> result) { isAiThinking.postValue(false); }
            @Override public void onError(Throwable e) {
                isAiThinking.postValue(false);
                Log.e("ChatViewModel", "AI response failed", e);
            }
        });
    }

    public void handleIncomingMessage(Map<String, Object> payload) {
        ChatMessage msg = mapToChatMessage(payload, String.valueOf(payload.get("$id")));
        List<ChatMessage> currentList = new ArrayList<>(Objects.requireNonNullElse(messages.getValue(), new ArrayList<>()));
        
        int index = -1;
        for (int i = 0; i < currentList.size(); i++) {
            if (Objects.equals(currentList.get(i).getId(), msg.getId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            currentList.set(index, msg);
        } else {
            currentList.add(msg);
        }
        messages.postValue(currentList);
    }

    private void addMessage(ChatMessage msg) {
        List<ChatMessage> list = new ArrayList<>(Objects.requireNonNullElse(messages.getValue(), new ArrayList<>()));
        list.add(msg);
        messages.setValue(list);
    }

    private ChatMessage mapToChatMessage(Map<String, Object> map, String docId) {
        String id = (docId == null || docId.equals("null")) ? "msg_" + UUID.randomUUID() : docId;
        String senderId = map.get("senderId") != null ? String.valueOf(map.get("senderId")).trim() : "";
        String senderName = map.get("senderName") != null ? String.valueOf(map.get("senderName")).trim() : "";
        String familyId = String.valueOf(map.get("familyId"));
        String text = String.valueOf(map.get("text"));
        long ts = 0;
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Number n) ts = n.longValue();
        else if (tsObj instanceof String s) {
            try { ts = Long.parseLong(s); } catch (Exception ignored) {}
        }
        if (ts == 0) ts = System.currentTimeMillis();
        boolean isMe = senderId.equalsIgnoreCase(pref.getUserId()) || senderName.equalsIgnoreCase(pref.getUsername());
        return new ChatMessage(id, senderId, familyId, senderName, text, ts, isMe);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (sessionTimer != null) sessionTimer.cancel();
    }
}
