package com.upreyvan.carti.ui.family;

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
import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class FamilyChatFragment extends BaseFragment<FragmentFamilyChatBinding> {

    private ChatAdapter chatAdapter;
    private ApiHelper apiHelper;
    private PreferenceManager pref;
    private Realtime realtime;
    private RealtimeSubscription subscription;
    
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

        apiHelper = new ApiHelper(requireContext());
        pref = new PreferenceManager(requireContext());

        setupDynamicPadding();
        setupToolbar();
        setupChatList();
        setupInput();
        initRealtime();
        loadChatHistory();
    }

    private void setupChatList() {
        chatAdapter = new ChatAdapter();
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

    private void initRealtime() {
        realtime = new Realtime(AppwriteManager.getInstance(requireContext()).getClient());
        String familyId = pref.getFamilyId();
        
        // Subscribe to the messages collection
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_MESSAGES + ".documents";
        
        subscription = realtime.subscribe(new String[]{channel}, event -> {
            if (event.getEvents().contains("databases.*.collections.*.documents.*.create")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) event.getPayload();
                String msgFamilyId = String.valueOf(payload.get("familyId"));
                
                if (familyId.equals(msgFamilyId)) {
                    ChatMessage msg = mapToChatMessage(payload);
                    requireActivity().runOnUiThread(() -> {
                        List<ChatMessage> currentList = new ArrayList<>(chatAdapter.getCurrentList());
                        boolean exists = false;
                        for (ChatMessage m : currentList) {
                            if (m.getId() != null && m.getId().equals(msg.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            currentList.add(msg);
                            chatAdapter.submitList(currentList, () -> {
                                LinearLayoutManager lm = (LinearLayoutManager) getBinding().rvChat.getLayoutManager();
                                if (lm != null) {
                                    int lastVisible = lm.findLastVisibleItemPosition();
                                    int totalItems = chatAdapter.getItemCount();
                                    if (lastVisible >= totalItems - 3) {
                                        getBinding().rvChat.scrollToPosition(totalItems - 1);
                                    }
                                }
                            });
                        }
                    });
                }
            }
            return null;
        });
    }

    private void loadChatHistory() {
        loadChatHistory(false);
    }

    private void loadChatHistory(boolean loadMore) {
        if (isLoading) return;
        isLoading = true;

        getBinding().pbLoadingMore.show();

        List<String> queries = new ArrayList<>(Arrays.asList(
            Query.Companion.equal("familyId", pref.getFamilyId()),
            Query.Companion.orderDesc("timestamp"),
            Query.Companion.limit(PAGE_SIZE)
        ));

        if (loadMore) {
            queries.add(Query.Companion.lessThan("timestamp", oldestTimestamp));
        }

        AppwriteManager.getInstance(requireContext())
            .listDocuments(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_MESSAGES,
                queries,
                new AppwriteCallback<DocumentList<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(DocumentList<Map<String, Object>> result) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            messages.add(mapToChatMessage(doc.getData()));
                        }

                        // Order for UI: ascending timestamp
                        java.util.Collections.reverse(messages);

                        requireActivity().runOnUiThread(() -> {
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
                            
                            // DiffUtil handles the scroll position retention automatically
                            chatAdapter.submitList(newList, () -> {
                                if (!loadMore && !newList.isEmpty()) {
                                    getBinding().rvChat.scrollToPosition(newList.size() - 1);
                                }
                            });
                            isLoading = false;
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        isLoading = false;
                        requireActivity().runOnUiThread(() -> getBinding().pbLoadingMore.hide());
                        if (getContext() != null) {
                            Utils.showToast(requireContext(), "Error loading messages");
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
        boolean isMe = senderId.equals(pref.getUserId());

        return new ChatMessage(id, senderId, familyId, senderName, text, timestamp, isMe);
    }

    private void setupInput() {
        getBinding().layoutInput.etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().rvChat.postDelayed(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            String text = getBinding().layoutInput.etInput.getText().toString().trim();
            if (text.isEmpty()) return;

            if (Utils.containsSensitiveInfo(text)) {
                Utils.showToast(requireContext(), "Security Alert: Sending sensitive information is not allowed.");
                return;
            }

            getBinding().layoutInput.etInput.setText("");
            apiHelper.sendMessage(text, new AppwriteCallback<Document<Map<String, Object>>>() {
                @Override
                public void onSuccess(Document<Map<String, Object>> result) {
                    // Handled by Realtime subscription
                }

                @Override
                public void onError(Throwable error) {
                    Utils.showToast(requireContext(), "Message failed: " + error.getMessage());
                }
            });
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            // Handle voice record
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear secure flag when leaving chat
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (subscription != null) {
            subscription.close();
        }
    }

    private void setupToolbar() {
        getBinding().layoutHeader.setElevation(4f);

        getBinding().backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().settingsButtonContainer.setOnClickListener(v -> showAutoDeleteDialog());
    }

    private void showAutoDeleteDialog() {
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
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    pref.setChatAutoDeleteDays(daysValues[which]);
                    Utils.showToast(requireContext(), "Auto delete set to " + options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDynamicPadding() {
        int originalHeaderBottom = getBinding().layoutHeader.getPaddingBottom();
        int originalInputBottom = getBinding().layoutInputContainer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutHeader, (v, insets) -> {
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
