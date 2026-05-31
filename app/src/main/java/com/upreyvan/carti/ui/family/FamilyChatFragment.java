package com.upreyvan.carti.ui.family;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentFamilyChatBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.ui.ai.AiSuggestionAdapter;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.data.ai.VoiceToTextHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;

public class FamilyChatFragment extends BaseFragment<FragmentFamilyChatBinding> {

    private ChatAdapter chatAdapter;
    private AiSuggestionAdapter suggestionAdapter;
    private ApiHelper apiHelper;
    private PreferenceManager pref;
    private RealtimeRepository realtimeRepo;
    private AiRepository aiRepository;
    private VoiceToTextHelper voiceToTextHelper;
    private boolean isAiThinking = false;
    private CountDownTimer activeSessionTimer;
    private static final long SESSION_TIMEOUT_MS = 60000;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private long oldestTimestamp = Long.MAX_VALUE;
    private long sessionStartTime = System.currentTimeMillis();
    private static final int PAGE_SIZE = 20;

    @Override
    protected FragmentFamilyChatBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentFamilyChatBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        pref = new PreferenceManager(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        aiRepository = AiRepository.getInstance(requireContext());

        setupChatList();
        setupSuggestions();
        setupInput();
        setupVoiceInput();
        setupToolbar();
        setupDynamicPadding();

        checkConnectionAndLoad();
        observeChatRealtime();
    }

    private void checkConnectionAndLoad() {
        if (checkNetwork()) {
            getBinding().layoutNoInternet.setVisibility(View.GONE);
            toggleChatContentVisibility(true);
            loadChatHistory();
        } else {
            getBinding().layoutNoInternet.setVisibility(View.VISIBLE);
            toggleChatContentVisibility(false);
            getBinding().btnRetry.setOnClickListener(v -> checkConnectionAndLoad());
        }
    }

    private void toggleChatContentVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        getBinding().layoutHeader.setVisibility(visibility);
        getBinding().layoutInputContainer.setVisibility(visibility);

        if (!visible) {
            getBinding().rvChat.setVisibility(View.GONE);
            getBinding().layoutEmpty.setVisibility(View.GONE);
            getBinding().layoutShimmer.setVisibility(View.GONE);
        }
    }

    private void setupSuggestions() {
        suggestionAdapter = new AiSuggestionAdapter();
        getBinding().rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvSuggestions.setAdapter(suggestionAdapter);

        suggestionAdapter.setOnItemClickListener(suggestion -> {
            getBinding().layoutInput.etInput.setText(suggestion.getDescription());
        });

        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion("Budget", "How much is our family budget?", R.drawable.ic_chart, R.color.carti_primary_green));
        suggestions.add(new AiSuggestion("Spending", "Who spent the most this week?", R.drawable.ic_person, R.color.carti_primary_green));
        suggestions.add(new AiSuggestion("Advice", "Give us a saving tip for today.", R.drawable.ai_holder, R.color.carti_primary_green));
        suggestionAdapter.submitList(suggestions);
    }

    private void setupChatList() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        getBinding().rvChat.setLayoutManager(layoutManager);
        getBinding().rvChat.setAdapter(chatAdapter);

        chatAdapter.setOnCancelListener((message, position) -> {
            // Optional: handle action cancellation
        });

        getBinding().rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0 && !isLoading && !isLastPage) {
                    if (layoutManager.findFirstVisibleItemPosition() <= 5) {
                        loadChatHistory(true);
                    }
                }
            }
        });
    }

    private void observeChatRealtime() {
        if (realtimeRepo == null) return;
        realtimeRepo.getChatStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null && isAdded()) {
                ChatMessage msg = mapToChatMessage(payload);
                List<ChatMessage> currentList = new ArrayList<>(chatAdapter.getCurrentList());

                boolean exists = false;
                int tempOptimisticIndex = -1;
                int shimmerIndex = -1;
                
                for (int i = 0; i < currentList.size(); i++) {
                    ChatMessage m = currentList.get(i);

                    if (m.getId() != null && m.getId().equals(msg.getId())) {
                        exists = true;
                        break;
                    }

                    boolean isSameSender = false;
                    if (m.getSenderId() != null && msg.getSenderId() != null) {
                        isSameSender = m.getSenderId().equals(msg.getSenderId());
                    } else if (m.isMe() && msg.isMe()) {
                        isSameSender = true;
                    }

                    if (m.getId() == null && !m.isShimmer() && isSameSender 
                            && m.getMessage().equals(msg.getMessage())
                            && Math.abs(m.getTimestamp() - msg.getTimestamp()) < 30000) {
                        tempOptimisticIndex = i;
                        break;
                    }
                    
                    if (m.isShimmer()) {
                        shimmerIndex = i;
                    }
                }
                
                if (exists) return;


                if (msg.getSenderId().equals(Constants.Roles.AI_ID) && shimmerIndex != -1) {
                    currentList.remove(shimmerIndex);
                    android.util.Log.d("FamilyChatFragment", "Removed AI shimmer for incoming AI message");
                }

                final int finalOptimisticIndex = tempOptimisticIndex;
                if (finalOptimisticIndex != -1) {
                    ChatMessage existing = currentList.get(finalOptimisticIndex);
                    msg.setIntent(existing.getIntent());
                    msg.setPendingAction(existing.getPendingAction());
                    msg.setCanceled(existing.isCanceled());
                    currentList.set(finalOptimisticIndex, msg);
                } else {
                    currentList.add(msg);
                }

                chatAdapter.submitList(currentList, () -> {
                    if (getBinding() == null) return;
                    if (finalOptimisticIndex == -1) {
                        getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                    processAiForMessage(msg);
                });
            }
        });
    }

    private void processAiForMessage(ChatMessage msg) {
        if (msg.isMe()) {
            if (msg.getTimestamp() < sessionStartTime) return;

            boolean isAddressedToAi = msg.getMessage().toLowerCase().contains("@carti");
            
            if (!isAddressedToAi && activeSessionTimer != null) {
                isAddressedToAi = true;
            }

            if (isAddressedToAi) {
                isAiThinking = true;
                updateInputState();
                
                ChatMessage shimmer = new ChatMessage(true, false);
                List<ChatMessage> list = new ArrayList<>(chatAdapter.getCurrentList());
                list.add(shimmer);
                chatAdapter.submitList(list, () -> getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1));

                aiRepository.processChat(msg.getMessage(), pref.getUsername(), list, true, new AiManager.AiCallback() {
                    @Override public void onSuccess(String response) { 
                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            sendAiResponse(response);
                            startActiveSessionTimer();
                        }); 
                    }
                    @Override public void onError(Throwable t) { 
                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            removeShimmerLocally(); 
                            isAiThinking = false; 
                            updateInputState(); 
                            android.util.Log.e("FamilyChatFragment", "AI Error", t);
                            String debugError = "Error: " + t.getMessage();
                            if (t.getCause() != null) debugError += " | Cause: " + t.getCause().getMessage();
                            sendAiResponse("DEBUG INFO: " + debugError + "\n\nI'm sorry, I'm having trouble connecting to my brain.");
                        });
                    }
                    @Override public void onActionDetected(org.json.JSONObject action) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                aiRepository.executeAction(action);
                            });
                        }
                    }
                });
            }
        }
    }

    private void removeShimmerLocally() {
        if (getBinding() == null) return;
        List<ChatMessage> list = new ArrayList<>(chatAdapter.getCurrentList());
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).isShimmer()) {
                list.remove(i);
                chatAdapter.submitList(list);
                break;
            }
        }
    }

    private void updateInputState() {
        if (getBinding() == null) return;
        getBinding().layoutInput.btnSend.setImageResource(isAiThinking ? R.drawable.ic_close : R.drawable.ic_send);
        boolean hasInputText = !getBinding().layoutInput.etInput.getText().toString().trim().isEmpty();
        getBinding().layoutInput.btnSend.setVisibility(isAiThinking || hasInputText ? View.VISIBLE : View.GONE);
        getBinding().layoutInput.btnEmojiLike.setVisibility(isAiThinking || hasInputText ? View.GONE : View.VISIBLE);
        
        if (isAiThinking) {
            getBinding().layoutInput.etInput.setHint("Carti is thinking...");
        } else if (activeSessionTimer == null) {
            getBinding().layoutInput.etInput.setHint(getString(R.string.hint_ask_me));
        }
    }

    private void startActiveSessionTimer() {
        if (activeSessionTimer != null) activeSessionTimer.cancel();
        
        if (getBinding() != null) {
            getBinding().tvSessionStatus.setVisibility(View.VISIBLE);
            getBinding().tvSessionStatus.setTextColor(getResources().getColor(R.color.carti_primary_green));
        }

        activeSessionTimer = new android.os.CountDownTimer(SESSION_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (getBinding() != null) {
                    String status = "Carti Ai Online " + (millisUntilFinished / 1000) + "s";
                    getBinding().tvSessionStatus.setText(status);
                    
                    if (!isAiThinking) {
                        getBinding().layoutInput.etInput.setHint("Carti is listening...");
                    }
                }
            }

            @Override
            public void onFinish() {
                activeSessionTimer = null;
                if (getBinding() != null) {
                    getBinding().tvSessionStatus.setText("Carti Ai Offline");
                    getBinding().tvSessionStatus.setTextColor(getResources().getColor(R.color.text_tertiary));
                    
                    if (!isAiThinking) {
                        getBinding().layoutInput.etInput.setHint(getString(R.string.hint_ask_me));
                    }
                }
            }
        }.start();
    }

    private void loadChatHistory() {
        loadChatHistory(false);
    }

    private void loadChatHistory(boolean loadMore) {
        Context context = getContext();
        if (context == null || isLoading) return;
        isLoading = true;

        if (!loadMore) {
            getBinding().layoutShimmer.setVisibility(View.VISIBLE);
            getBinding().layoutShimmer.startShimmer();
            getBinding().rvChat.setVisibility(View.GONE);
            getBinding().layoutEmpty.setVisibility(View.GONE);
        } else {
            getBinding().pbLoadingMore.setVisibility(View.VISIBLE);
        }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", pref.getFamilyId()));
        queries.add(Query.Companion.orderDesc("timestamp"));
        queries.add(Query.Companion.limit(PAGE_SIZE));

        if (loadMore && oldestTimestamp != Long.MAX_VALUE) {
            queries.add(Query.Companion.lessThan("timestamp", oldestTimestamp));
        }

        AppwriteManager.getInstance(context)
            .listDocuments(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_MESSAGES,
                queries,
                new AppwriteCallback<DocumentList<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(DocumentList<Map<String, Object>> result) {
                        if (!isAdded() || getActivity() == null) {
                            isLoading = false;
                            return;
                        }
                        
                        List<ChatMessage> messages = new ArrayList<>();
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            messages.add(mapToChatMessage(doc.getData()));
                        }

                        Collections.reverse(messages);

                        getActivity().runOnUiThread(() -> {
                            if (getBinding() == null) return;
                            getBinding().pbLoadingMore.hide();
                            getBinding().layoutShimmer.stopShimmer();
                            getBinding().layoutShimmer.setVisibility(View.GONE);

                            List<ChatMessage> currentItems = new ArrayList<>(chatAdapter.getCurrentList());
                            List<ChatMessage> newList;
                            
                            if (loadMore) {
                                newList = new ArrayList<>(messages);
                                newList.addAll(currentItems);
                            } else {
                                newList = messages;
                            }

                            if (!messages.isEmpty()) {
                                oldestTimestamp = messages.get(0).getTimestamp();
                            }

                            isLastPage = result.getDocuments().size() < PAGE_SIZE;
                            
                            chatAdapter.submitList(newList, () -> {
                                if (getBinding() != null) {
                                    if (!loadMore && !newList.isEmpty()) {
                                        getBinding().rvChat.scrollToPosition(newList.size() - 1);
                                    }
                                    
                                    if (newList.isEmpty()) {
                                        getBinding().layoutEmpty.setVisibility(View.VISIBLE);
                                        getBinding().rvChat.setVisibility(View.GONE);
                                    } else {
                                        getBinding().layoutEmpty.setVisibility(View.GONE);
                                        getBinding().rvChat.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                            isLoading = false;
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isLoading = false;
                            if (getBinding() != null) {
                                getBinding().pbLoadingMore.hide();
                                getBinding().layoutShimmer.stopShimmer();
                                getBinding().layoutShimmer.setVisibility(View.GONE);
                                showError(error);
                            }
                        });
                    }
                }
            );
    }

    private void sendAiResponse(String text) {
        if (apiHelper != null) {
            apiHelper.sendAiMessage(text, new AppwriteCallback<Document<Map<String, Object>>>() {
                @Override
                public void onSuccess(Document<Map<String, Object>> result) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        removeShimmerLocally();
                        isAiThinking = false;
                        updateInputState();
                    });
                }

                @Override
                public void onError(Throwable error) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        removeShimmerLocally();
                        isAiThinking = false;
                        updateInputState();
                        String errorMsg = error.getMessage() != null ? error.getMessage() : "Unknown Appwrite Error";
                        Utils.showToast(getContext(), "DB Error: " + errorMsg);
                    });
                }
            });
        }
    }

    private ChatMessage mapToChatMessage(Map<String, Object> map) {
        String id = String.valueOf(map.get("$id"));
        String senderId = map.get("senderId") != null ? String.valueOf(map.get("senderId")).trim() : "";
        String senderName = map.get("senderName") != null ? String.valueOf(map.get("senderName")).trim() : "";
        String familyId = String.valueOf(map.get("familyId"));
        String text = String.valueOf(map.get("text"));
        long timestamp = 0;
        if (map.get("timestamp") != null) {
            timestamp = ((Number) map.get("timestamp")).longValue();
        }
        
        String myUserId = pref != null ? pref.getUserId().trim() : "";
        String myUsername = pref != null ? pref.getUsername().trim() : "";

        boolean isMe = false;
        if (!senderId.isEmpty() && !senderId.equals("null")) {
            isMe = senderId.equalsIgnoreCase(myUserId);
        } else if (!senderName.isEmpty()) {
            isMe = senderName.equalsIgnoreCase(myUsername);
        }

        return new ChatMessage(id, senderId, familyId, senderName, text, timestamp, isMe);
    }

    private void setupVoiceInput() {
        voiceToTextHelper = VoiceToTextHelper.getInstance(requireContext());
        voiceToTextHelper.setCallback(new VoiceToTextHelper.VoiceCallback() {
            @Override public void onReadyForSpeech() { getBinding().layoutInput.btnVoice.setAlpha(1.0f); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { resetVoiceUi(); }
            @Override public void onError(String error) { Utils.showToast(requireContext(), error); resetVoiceUi(); }
            @Override public void onResults(String text) { getBinding().layoutInput.etInput.setText(text); resetVoiceUi(); }
            @Override public void onPartialResults(String partialText) { getBinding().layoutInput.etInput.setHint(partialText); }
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            if (voiceToTextHelper.isListening()) voiceToTextHelper.stopListening();
            else startVoiceRecognition();
        });
    }

    private void resetVoiceUi() {
        if (getBinding() == null) return;
        getBinding().layoutInput.btnVoice.setAlpha(1.0f);
        getBinding().layoutInput.etInput.setHint(getString(R.string.hint_ask_me));
    }

    private void startVoiceRecognition() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
        } else {
            getBinding().layoutInput.btnVoice.setAlpha(0.5f);
            getBinding().layoutInput.etInput.setHint("Listening...");
            voiceToTextHelper.startListening();
        }
    }

    private void setupInput() {
        getBinding().layoutInput.etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                getBinding().layoutInput.btnSend.setVisibility(hasText || isAiThinking ? View.VISIBLE : View.GONE);
                getBinding().layoutInput.btnEmojiLike.setVisibility(hasText || isAiThinking ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        View.OnClickListener emojiClickListener = v -> {
            v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            ).start();
            if (v instanceof android.widget.TextView) {
                String emoji = ((android.widget.TextView) v).getText().toString();
                sendDirectMessage(emoji);
            }
        };

        getBinding().layoutInput.btnEmojiLike.setOnClickListener(emojiClickListener);

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            if (isAiThinking) {
                isAiThinking = false;
                updateInputState();
                removeShimmerLocally();
                Utils.showToast(requireContext(), "Thinking canceled.");
                return;
            }

            if (!checkNetwork()) return;

            String text = getBinding().layoutInput.etInput.getText().toString().trim();
            if (text.isEmpty()) return;

            if (Utils.containsSensitiveInfo(text)) {
                Utils.showToast(requireContext(), "Security Alert: Sending sensitive information is not allowed.");
                return;
            }

            sendDirectMessage(text);
        });
    }

    private void sendDirectMessage(String text) {
        if (!checkNetwork()) return;

        ChatMessage localMsg = new ChatMessage(null, pref.getUserId(), pref.getFamilyId(), pref.getUsername(), text, System.currentTimeMillis(), true);
        List<ChatMessage> currentList = new ArrayList<>(chatAdapter.getCurrentList());
        currentList.add(localMsg);
        chatAdapter.submitList(currentList, () -> getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1));

        getBinding().layoutInput.etInput.setText("");
        if (apiHelper != null) {
            apiHelper.sendMessage(text, new AppwriteCallback<Document<Map<String, Object>>>() {
                @Override public void onSuccess(Document<Map<String, Object>> result) {}
                @Override public void onError(Throwable e) { showError(e); }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (voiceToTextHelper != null) voiceToTextHelper.destroy();
        super.onDestroyView();
    }

    private void setupToolbar() {
        getBinding().btnSettings.setOnClickListener(v -> showAutoDeleteDialog());
    }

    private void showAutoDeleteDialog() {
        String[] options = {"3 Days", "7 Days", "30 Days", "Never"};
        int[] values = {3, 7, 30, 0};
        int current = 1; 

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Auto-delete Messages")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    pref.setChatAutoDeleteDays(values[which]);
                    Utils.showToast(requireContext(), "Updated to " + options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDynamicPadding() {
        if (getBinding() == null) return;
        int bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height) + getResources().getDimensionPixelSize(R.dimen.spacing_medium);

        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutInputContainer, (v, insets) -> {
            if (getBinding() == null) return insets;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            
            // If keyboard is visible, we don't need extra padding because adjustResize handles it.
            // If not, we add padding to clear the system navigation bar and bottom nav.
            int paddingBottom = isKeyboardVisible ? 0 : (systemBars.bottom + bottomNavHeight);

            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), paddingBottom);
            
            // Try to hide the MainActivity's bottom nav if keyboard is visible
            if (getActivity() instanceof com.upreyvan.carti.MainActivity) {
                View bottomNav = getActivity().findViewById(R.id.bottom_nav_container);
                if (bottomNav != null) {
                    bottomNav.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
                }
            }

            return insets;
        });
    }
}
