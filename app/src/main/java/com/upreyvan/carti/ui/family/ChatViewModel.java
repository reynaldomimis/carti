package com.upreyvan.carti.ui.family;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.ai.LocalIntentParser;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.data.repository.ChatRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.CategoryMapper;
import com.upreyvan.carti.data.ai.CategoryValidator;
import com.upreyvan.carti.util.MessageHelper;
import com.upreyvan.carti.util.Utils;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    
    private final MutableLiveData<Boolean> isAiThinking = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>(null);
    private final MutableLiveData<Long> sessionTimeRemaining = new MutableLiveData<>(0L);

    private final Map<String, ChatMessage> messageMap = new LinkedHashMap<>();
    private final MediatorLiveData<List<ChatMessage>> messagesLiveData = new MediatorLiveData<>();
    
    private long oldestTimestamp = Long.MAX_VALUE;
    private static final int PAGE_SIZE = 50;
    private JSONObject pendingTransaction = null;
    private boolean isClarifying = false;
    private CountDownTimer sessionTimer;
    private static final long SESSION_DURATION = 60000;
    private static final Pattern AI_TRIGGER_PATTERN = Pattern.compile("(?i)(@carti|/carti)");

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.pref = PreferenceManager.getInstance(application);
        this.chatRepo = ChatRepository.getInstance(application);
        this.aiRepo = AiRepository.getInstance(application);
        this.transRepo = TransactionRepository.getInstance(application);
        
        messagesLiveData.addSource(chatRepo.getChatStream(), payload -> {
            if (payload != null) {
                String id = (String) payload.get("$id");
                ChatMessage msg = MessageHelper.mapToChatMessage(payload, id, pref.getUserId(), pref.getUsername());
                if (msg != null) {
                    updateMessage(msg);
                }
            }
        });
        loadHistory(false);
    }

    private synchronized void updateMessage(ChatMessage msg) {
        messageMap.put(msg.getId(), msg);
        List<ChatMessage> sorted = new ArrayList<>(messageMap.values());
        sorted.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        messagesLiveData.postValue(sorted);
    }

    private synchronized void updateMessages(List<ChatMessage> batch) {
        for (ChatMessage msg : batch) {
            messageMap.put(msg.getId(), msg);
        }
        List<ChatMessage> sorted = new ArrayList<>(messageMap.values());
        sorted.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        messagesLiveData.postValue(sorted);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messagesLiveData; }
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
                    batch.add(MessageHelper.mapToChatMessage(doc.getData(), doc.getId(), pref.getUserId(), pref.getUsername()));
                }

                if (batch.isEmpty()) {
                    setLoading(false);
                    return;
                }

                ChatMessage oldestInBatch = batch.get(batch.size() - 1);
                oldestTimestamp = oldestInBatch.getTimestamp();
                updateMessages(batch);
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

        ChatMessage localMsg = new ChatMessage(clientSideId, pref.getUserId(), pref.getFamilyId(), pref.getUsername(), text, System.currentTimeMillis(), true);
        updateMessage(localMsg);
        
        chatRepo.sendMessage(clientSideId, text, new AppwriteCallback<>() {
            @Override public void onSuccess(Document<Map<String, Object>> result) {
                boolean isAiTriggered = AI_TRIGGER_PATTERN.matcher(text).find();
                boolean isSessionActive = sessionTimeRemaining.getValue() != null && sessionTimeRemaining.getValue() > 0;

                if (isAiTriggered) {
                    startSession();
                    String cleanText = text.trim().toLowerCase();
                    if ((cleanText.equals("@carti") || cleanText.equals("/carti")) && pref.isAiIntroDone()) {
                        sendAiResponse("Yes? How can I help you with your family budget today? 🐧");
                        return;
                    }
                }

                if (isAiTriggered || isSessionActive) {
                    if (isAiTriggered) startSession();
                    
                    if (pendingTransaction != null) {
                        handleClarificationReply(text);
                    } else {
                        JSONObject localResult = LocalIntentParser.parse(getApplication(), text);
                        String status = localResult != null ? localResult.optString("status") : "UNKNOWN";
                        
                        if ("LOG".equals(status)) {
                            isClarifying = false;
                            handleParsedIntent(localResult);
                        } else if ("PENDING".equals(status)) {
                            pendingTransaction = localResult;
                            sendAiResponse(localResult.optString("message"));
                        } else if ("BLOCK".equals(status)) {
                            sendAiResponse("I can only help with financial tracking.");
                        } else if (isAiTriggered) {
                            processAiChat(text);
                        }
                    }
                }
            }
            @Override public void onError(Throwable error) {
                errorMsg.postValue("Failed to send: " + error.getMessage());
            }
        });
    }

    private void processIntent(String text) {
        JSONObject freshParse = LocalIntentParser.parse(getApplication(), text);
        String status = freshParse != null ? freshParse.optString("status") : "UNKNOWN";
        
        switch (status) {
            case "LOG" -> {
                isClarifying = false;
                handleParsedIntent(freshParse);
            }
            case "BLOCK" -> {
                isClarifying = false;
                sendAiResponse("I can only help with financial tracking. Please send an expense, income, or transaction.");
            }
            case "PENDING" -> {
                pendingTransaction = freshParse;
                sendAiResponse(freshParse.optString("message"));
            }
            case "INSIGHT" -> {
                isClarifying = false;
                processAiChat("Task: Provide insight for " + freshParse.optString("type"));
            }
            default -> {
                if (!isClarifying) {
                    isClarifying = true;
                    sendAiResponse("I couldn't identify those words. Could you please clarify?");
                } else {
                    isClarifying = false;
                    processAiChat(text);
                }
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
                JSONObject p = LocalIntentParser.parse(getApplication(), reply);
                amount = (p != null) ? p.optDouble("amount", 0) : 0;
                if (amount <= 0) amount = extractAmountFromText(reply);
            }

            if (amount > 0 && !item.isEmpty() && item.length() >= 2) {
                final double finalAmt = amount;
                final String finalItem = item;
                CategoryMapper.MapResult mapping = CategoryMapper.mapDetailed(getApplication(), item);
                
                CategoryValidator.validate(getApplication(), item, mapping.category, validatedCategory -> {
                    try {
                        JSONObject finalized = new JSONObject();
                        finalized.put("type", "EXPENSE_LOG");
                        finalized.put("status", "LOG");
                        finalized.put("isTransaction", true);
                        finalized.put("amount", finalAmt);
                        JSONObject ext = new JSONObject();
                        ext.put("item", finalItem);
                        ext.put("category", validatedCategory);
                        ext.put("sub_category", mapping.subCategory);
                        finalized.put("extracted", ext);
                        logTransaction(finalized);
                    } catch (Exception e) {
                        Log.e("ChatViewModel", "Finalization error", e);
                    }
                });
                pendingTransaction = null;
            } else {
                pendingTransaction = null;
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
        String subCategory = data.optString("sub_category");
        String intentType = parse.optString("type");
        
        String type;
        if (Objects.equals(intentType, "INCOME_LOG") || Objects.equals(intentType, "ALLOCATION_LOG")) {
            type = "INCOME";
        } else {
            type = "EXPENSE";
        }

        final double amt = finalAmount;
        performAddTransaction(amt, type, Objects.requireNonNullElse(category, "Others"), subCategory, item);
    }

    private void performAddTransaction(double amt, String type, String category, String subCategory, String item) {
        Transaction t = new Transaction();
        t.setAmount(amt);
        t.setType(type);
        t.setCategory(category);
        t.setSubCategory(subCategory);
        t.setTitle(item);
        t.setNote("Chat log: " + item);

        transRepo.addTransaction(t, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> map) {
                String label = subCategory != null ? subCategory : category;
                sendAiResponse("Got it! Recorded " + item + " (" + Utils.formatCurrency(amt) + ") under " + label + ".");
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
        aiRepo.processChat(text, "", messagesLiveData.getValue(), true, new AiRepository.AiCallback() {
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (sessionTimer != null) sessionTimer.cancel();
    }
}
