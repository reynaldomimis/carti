package com.upreyvan.carti.ui.family;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.databinding.FragmentFamilyChatBinding;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.data.ai.GeminiManager;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class FamilyChatFragment extends BaseFragment<FragmentFamilyChatBinding> {

    private ChatAdapter chatAdapter;
    private ApiHelper apiHelper;
    private PreferenceManager pref;
    private RealtimeRepository realtimeRepo;
    private AiRepository aiRepository;
    private boolean isAiThinking = false;
    
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private long oldestTimestamp = Long.MAX_VALUE;
    private static final int PAGE_SIZE = 20;

    @Override
    protected FragmentFamilyChatBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentFamilyChatBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Prevent screenshots in chat room
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        Context context = getContext();
        if (context != null) {
            apiHelper = new ApiHelper(context);
            pref = new PreferenceManager(context);
            realtimeRepo = RealtimeRepository.getInstance(context);
            aiRepository = new AiRepository(context);
        }
        
        chatAdapter = new ChatAdapter();

        setupDynamicPadding();
        setupToolbar();
        setupChatList();
        setupInput();
        observeChatRealtime();
        loadChatHistory();
    }

    private void setupChatList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        getBinding().rvChat.setLayoutManager(layoutManager);
        getBinding().rvChat.setAdapter(chatAdapter);

        getBinding().rvChat.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0 && !recyclerView.canScrollVertically(-1)) {
                    if (!isLoading && !isLastPage) {
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
                
                for (int i = 0; i < currentList.size(); i++) {
                    ChatMessage m = currentList.get(i);
                    // 1. Exact ID match (Best case)
                    if (m.getId() != null && m.getId().equals(msg.getId())) {
                        exists = true;
                        break;
                    }
                    // 2. Optimistic match (No ID, but same content, sender and very recent)
                    if (m.getId() == null && m.isMe() && msg.isMe() 
                            && m.getMessage().equals(msg.getMessage())
                            && Math.abs(m.getTimestamp() - msg.getTimestamp()) < 30000) {
                        tempOptimisticIndex = i;
                        break;
                    }
                }
                
                if (exists) return;

                final int finalOptimisticIndex = tempOptimisticIndex;
                if (finalOptimisticIndex != -1) {
                    currentList.set(finalOptimisticIndex, msg);
                } else {
                    currentList.add(msg);
                }

                chatAdapter.submitList(currentList, () -> {
                    if (getBinding() == null) return;
                    if (finalOptimisticIndex == -1) {
                        getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });

                processAiForMessage(msg);
            }
        });
    }




    private void processAiForMessage(ChatMessage msg) {
        if (!msg.getSenderId().equals(Constants.Roles.AI_ID)) {
            String messageText = msg.getMessage().trim().toLowerCase();
            boolean isSender = msg.getSenderId().equals(pref.getUserId());

            boolean isExplicit = messageText.startsWith("@") || messageText.contains("@carti");
            
            boolean isAiActive = false;
            long SESSION_TIMEOUT_MS = 3 * 60 * 1000;
            List<ChatMessage> currentChat = chatAdapter.getCurrentList();
            
            if (!currentChat.isEmpty()) {
                int index = currentChat.size() - 1;
                while (index >= 0) {
                    ChatMessage m = currentChat.get(index);
                    if (m.getId() != null && !m.getId().equals(msg.getId())) {
                        boolean isAiAuthor = m.getSenderId().equals(Constants.Roles.AI_ID);
                        boolean isRecent = (System.currentTimeMillis() - m.getTimestamp()) < SESSION_TIMEOUT_MS;
                        isAiActive = isAiAuthor && isRecent;
                        break;
                    }
                    index--;
                }
            }

            if (aiRepository != null && (isExplicit || isAiActive)) {
                final ChatMessage[] typingRef = {null};
                if (isSender) {
                    isAiThinking = true;
                    updateInputState();
                    typingRef[0] = new ChatMessage(true, false);
                    List<ChatMessage> listWithTyping = new ArrayList<>(chatAdapter.getCurrentList());
                    listWithTyping.add(typingRef[0]);
                    chatAdapter.submitList(listWithTyping, () -> {
                        if (getBinding() != null) getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }

                aiRepository.processChat(msg.getMessage(), msg.getSenderName(), chatAdapter.getCurrentList(), isExplicit, new GeminiManager.AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (isAdded() && getActivity() != null && isSender) {
                            getActivity().runOnUiThread(() -> {
                                isAiThinking = false;
                                updateInputState();
                                if (typingRef[0] != null) {
                                    List<ChatMessage> listAfterResponse = new ArrayList<>(chatAdapter.getCurrentList());
                                    listAfterResponse.remove(typingRef[0]);
                                    chatAdapter.submitList(listAfterResponse);
                                }
                                sendAiResponse(response);
                            });
                        }
                    }

                    @Override
                    public void onActionDetected(org.json.JSONObject action) {
                        if (isSender) {
                            android.util.Log.d("FamilyChatFragment", "AI Action: " + action.optString("action"));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isAdded() && getActivity() != null && isSender) {
                            getActivity().runOnUiThread(() -> {
                                isAiThinking = false;
                                updateInputState();
                                if (typingRef[0] != null) {
                                    List<ChatMessage> listAfterError = new ArrayList<>(chatAdapter.getCurrentList());
                                    listAfterError.remove(typingRef[0]);
                                    chatAdapter.submitList(listAfterError);
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    private void updateInputState() {
        if (getBinding() == null) return;
        if (isAiThinking) {
            getBinding().layoutInput.btnSend.setImageResource(R.drawable.ic_close); // Change to cancel icon
        } else {
            getBinding().layoutInput.btnSend.setImageResource(R.drawable.ic_send); // Revert to send icon
        }
    }

    private void loadChatHistory() {
        loadChatHistory(false);
    }

    private void loadChatHistory(boolean loadMore) {
        if (isLoading || !isAdded()) return;
        
        Context context = getContext();
        if (context == null || pref == null) return;

        isLoading = true;
        if (getBinding() != null) getBinding().pbLoadingMore.show();

        List<String> queries = new ArrayList<>(Arrays.asList(
            Query.Companion.equal("familyId", pref.getFamilyId()),
            Query.Companion.orderDesc("timestamp"),
            Query.Companion.limit(PAGE_SIZE)
        ));

        if (loadMore) {
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

                        java.util.Collections.reverse(messages);

                        getActivity().runOnUiThread(() -> {
                            if (getBinding() == null) return;
                            getBinding().pbLoadingMore.hide();
                            List<ChatMessage> currentItems = new ArrayList<>(chatAdapter.getCurrentList());
                            List<ChatMessage> newList;
                            
                            if (loadMore) {
                                // Prepend older messages
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
                                if (getBinding() != null && !loadMore && !newList.isEmpty()) {
                                    getBinding().rvChat.scrollToPosition(newList.size() - 1);
                                }
                            });
                            isLoading = false;
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        isLoading = false;
                        if (!isAdded() || getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (getBinding() != null) getBinding().pbLoadingMore.hide();
                        });
                        if (getContext() != null) {
                            Utils.showToast(requireContext(), "Error loading messages");
                        }
                    }
                }
            );
    }

    private void sendAiResponse(String text) {
        if (text == null || text.trim().isEmpty() || !isAdded() || pref == null) return;

        Context context = getContext();
        if (context == null) return;

        android.util.Log.d("FamilyChatFragment", "Sending AI response to Appwrite: " + text);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("text", text);
        data.put("senderId", Constants.Roles.AI_ID);
        data.put("senderName", Constants.Roles.AI_NAME);
        data.put("familyId", pref.getFamilyId());
        data.put("timestamp", System.currentTimeMillis());

        List<String> permissions = new ArrayList<>();
        permissions.add(io.appwrite.Permission.Companion.read(io.appwrite.Role.Companion.users("")));

        AppwriteManager.getInstance(context).createDocument(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_MESSAGES,
                io.appwrite.ID.Companion.unique(0),
                data,
                permissions,
                new AppwriteCallback<Document<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(Document<Map<String, Object>> result) {
                        android.util.Log.d("FamilyChatFragment", "AI response successfully sent to Appwrite");
                    }

                    @Override
                    public void onError(Throwable error) {
                        android.util.Log.e("FamilyChatFragment", "Failed to send AI response to Appwrite", error);
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                String errorMsg = error.getMessage() != null ? error.getMessage() : "Unknown Appwrite Error";
                                Utils.showToast(getContext(), "DB Error: " + errorMsg);
                            });
                        }
                    }
                }
        );
    }

    private ChatMessage mapToChatMessage(Map<String, Object> map) {
        String id = String.valueOf(map.get("$id"));
        String senderId = String.valueOf(map.get("senderId"));
        String familyId = String.valueOf(map.get("familyId"));
        String senderName = String.valueOf(map.get("senderName"));
        String text = String.valueOf(map.get("text"));
        long timestamp = ((Number) map.get("timestamp")).longValue();
        boolean isMe = pref != null && senderId.equals(pref.getUserId());

        return new ChatMessage(id, senderId, familyId, senderName, text, timestamp, isMe);
    }

    private void setupInput() {
        if (getBinding() == null) return;

        getBinding().layoutInput.etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().rvChat.postDelayed(() -> {
                    if (isAdded() && getBinding() != null && chatAdapter.getItemCount() > 0) {
                        getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            if (isAiThinking) {
                isAiThinking = false;
                updateInputState();
                
                List<ChatMessage> currentList = new ArrayList<>(chatAdapter.getCurrentList());
                for (int i = currentList.size() - 1; i >= 0; i--) {
                    if (currentList.get(i).isShimmer()) {
                        currentList.remove(i);
                        break;
                    }
                }
                chatAdapter.submitList(currentList);
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

            // OPTIMISTIC UI: Show user message immediately
            ChatMessage localMsg = new ChatMessage(null, pref.getUserId(), pref.getFamilyId(), pref.getUsername(), text, System.currentTimeMillis(), true);
            List<ChatMessage> currentList = new ArrayList<>(chatAdapter.getCurrentList());
            currentList.add(localMsg);
            chatAdapter.submitList(currentList, () -> {
                getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            });

            getBinding().layoutInput.etInput.setText("");
            if (apiHelper != null) {
                apiHelper.sendMessage(text, new AppwriteCallback<Document<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(Document<Map<String, Object>> result) {
                        // Realtime will handle the replacement of the local message
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (isAdded()) {
                            // Remove optimistic message on error
                            List<ChatMessage> listOnError = new ArrayList<>(chatAdapter.getCurrentList());
                            listOnError.remove(localMsg);
                            chatAdapter.submitList(listOnError);
                            Utils.showToast(requireContext(), "Message failed: " + error.getMessage());
                        }
                    }
                });
            }
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            // Handle voice record
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void setupToolbar() {
        if (getBinding() == null) return;
        getBinding().layoutHeader.setElevation(4f);

        getBinding().backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().settingsButtonContainer.setOnClickListener(v -> showAutoDeleteDialog());
    }

    private void showAutoDeleteDialog() {
        if (pref == null) return;
        String[] options = {"7 Days (Default)", "15 Days", "1 Month"};
        int[] daysValues = {7, 15, 30};
        
        int currentDays = pref.getChatAutoDeleteDays();
        int checkedItem = 0;
        for (int i = 0; i < daysValues.length; i++) {
            if (daysValues[i] == currentDays) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Auto Delete Messages")
                .setCancelable(false)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    pref.setChatAutoDeleteDays(daysValues[which]);
                    Utils.showToast(requireContext(), "Auto delete set to " + options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDynamicPadding() {
        if (getBinding() == null) return;
        int originalHeaderBottom = getBinding().layoutHeader.getPaddingBottom();
        int originalInputBottom = getBinding().layoutInputContainer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutHeader, (v, insets) -> {
            if (getBinding() == null) return insets;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int adjustedTop = (int) (systemBars.top * 0.3f);

            v.setPadding(
                    v.getPaddingLeft(),
                     adjustedTop,
                    v.getPaddingRight(),
                    originalHeaderBottom
            );
            return insets;
        });


        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutInputContainer, (v, insets) -> {
            if (getBinding() == null) return insets;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int insetBottom = Math.max(systemBars.bottom, ime.bottom);

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    insetBottom + originalInputBottom
            );
            return insets;
        });
    }
}
