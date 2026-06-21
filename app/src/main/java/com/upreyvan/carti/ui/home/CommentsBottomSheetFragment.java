package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.Window;
import android.view.WindowManager;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.core.view.WindowCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentCommentsBottomSheetBinding;
import com.upreyvan.carti.utils.AvatarHelper;
import com.upreyvan.carti.models.Comment;
import com.upreyvan.carti.utils.CommentHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommentsBottomSheetFragment extends BaseBottomSheetFragment<FragmentCommentsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    
    private String transactionId;
    private String selectedParentId = null;
    private String lastPostedParentId = null;
    private CommentAdapter adapter;
    private TransactionRepository repository;
    private PreferenceManager pref;
    private boolean shouldScrollToBottom = false;
    private final List<Comment> currentComments = new ArrayList<>();

    public static CommentsBottomSheetFragment newInstance(String transactionId) {
        CommentsBottomSheetFragment fragment = new CommentsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRANSACTION_ID, transactionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentCommentsBottomSheetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentCommentsBottomSheetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transactionId = getArguments().getString(ARG_TRANSACTION_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog dialog) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = TransactionRepository.getInstance(requireContext());
        pref = PreferenceManager.getInstance(requireContext());

        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setupUI();
        loadComments();
        setupRealtime();
    }

    private void setupRealtime() {
        RealtimeRepository.getInstance(requireContext()).getCommentStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                String tid = String.valueOf(payload.get("transactionId"));
                if (transactionId.equals(tid)) {
                    patchRealtimeComment(payload);
                }
            }
        });
    }

    private void setupUI() {
        adapter = new CommentAdapter(pref.getUserId(), pref.isAdmin());
        adapter.setOnCommentInteractionListener(new CommentAdapter.OnCommentInteractionListener() {
            @Override
            public void onReplyComment(Comment comment) {
                startReplyMode(comment);
            }

            @Override
            public void onDeleteComment(Comment comment) {
                confirmDelete(comment);
            }

            @Override
            public void onRetryComment(Comment comment) {
                retryComment(comment);
            }
        });
        getBinding().rvComments.setAdapter(adapter);

        AvatarHelper.loadUserAvatar(requireContext(), getBinding().ivCurrentUserAvatar, pref.getUsername());

        getBinding().cardInput.setOnClickListener(v -> {
            getBinding().etComment.requestFocus();
            UiHelper.showKeyboard(requireContext(), getBinding().etComment);
        });

        ViewCompat.setOnApplyWindowInsetsListener(getBinding().getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(systemBars.bottom, ime.bottom);

            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int extraGap = isKeyboardVisible ? Utils.dpToPx(requireContext(), 20) : Utils.dpToPx(requireContext(), 16);
            
            if (v.getParent() instanceof ViewGroup parent) {
                ChangeBounds transition = new ChangeBounds();
                transition.setDuration(400);
                TransitionManager.beginDelayedTransition(parent, transition);
            }

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = bottom + extraGap;
            lp.leftMargin = Utils.dpToPx(requireContext(), 12);
            lp.rightMargin = Utils.dpToPx(requireContext(), 12);
            v.setLayoutParams(lp);

            if (isKeyboardVisible && adapter != null && adapter.getItemCount() > 0) {
                getBinding().rvComments.postDelayed(() -> 
                    getBinding().rvComments.smoothScrollToPosition(adapter.getItemCount() - 1), 450);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        getBinding().btnSend.setOnClickListener(v -> postComment());
        getBinding().btnClose.setOnClickListener(v -> dismiss());

        // Disable send button by default and add text listener to avoid spamming
        getBinding().btnSend.setEnabled(false);
        getBinding().btnSend.setAlpha(0.5f);
        getBinding().etComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && !s.toString().trim().isEmpty();
                getBinding().btnSend.setEnabled(hasText);
                getBinding().btnSend.setAlpha(hasText ? 1.0f : 0.5f);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        getBinding().etComment.requestFocus();
        Utils.showKeyboard(requireContext(), getBinding().etComment);
    }

    private void startReplyMode(Comment comment) {
        selectedParentId = comment.getId();
        getBinding().etComment.setHint(String.format("Replying to @%s...", comment.getUsername()));
        getBinding().etComment.requestFocus();
        Utils.showKeyboard(requireContext(), getBinding().etComment);
    }

    private void confirmDelete(Comment comment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setCancelable(false)
                .setPositiveButton("Delete", (dialog, which) -> {
                    int removedIndex = findCommentIndex(comment.getId());
                    Comment removed = removedIndex >= 0 ? currentComments.remove(removedIndex) : null;
                    renderComments();
                    repository.removeComment(comment.getId(), new AppwriteCallback<>() {
                        @Override public void onSuccess(Map<String, Object> result) {
                            // Realtime delete can arrive later; the local patch already removed it.
                        }
                        @Override public void onError(Throwable error) {
                            if (removed != null) {
                                currentComments.add(Math.min(removedIndex, currentComments.size()), removed);
                                renderComments();
                            }
                            showToast("Failed to delete comment", UiHelper.Status.ERROR);}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadComments() {
        loadComments(true);
    }

    private void loadComments(boolean showShimmer) {
        if (showShimmer) {
            getBinding().layoutShimmer.setVisibility(View.VISIBLE);
            getBinding().rvComments.setVisibility(View.GONE);
        }

        repository.getComments(transactionId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(List<Comment> result) {
                getBinding().layoutShimmer.setVisibility(View.GONE);
                getBinding().rvComments.setVisibility(View.VISIBLE);
                currentComments.clear();
                currentComments.addAll(result);
                renderComments();


                if (shouldScrollToBottom && !result.isEmpty()) {
                    getBinding().rvComments.post(() -> {
                        if (!isAdded() || getBinding() == null) return;
                        if (lastPostedParentId == null) {
                            int count = adapter.getItemCount();
                            if (count > 0) {
                                getBinding().rvComments.smoothScrollToPosition(count - 1);
                            }
                        } else {
                            List<Comment> current = adapter.getCurrentList();
                            if (current != null) {
                                for (int i = 0; i < current.size(); i++) {
                                    if (current.get(i).getId().equals(lastPostedParentId)) {
                                        getBinding().rvComments.smoothScrollToPosition(i);
                                        break;
                                    }
                                }
                            }
                        }
                        shouldScrollToBottom = false;
                        lastPostedParentId = null;
                    });
                }
            }

            @Override
            public void onError(Throwable error) {
                getBinding().layoutShimmer.setVisibility(View.GONE);
                getBinding().rvComments.setVisibility(View.VISIBLE);
                showToast("Failed to load comments", UiHelper.Status.ERROR);
            }
        });
    }

    private void postComment() {
        String text = getBinding().etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        getBinding().btnSend.setEnabled(false);
        getBinding().etComment.setText("");
        shouldScrollToBottom = true;
        lastPostedParentId = selectedParentId;
        String parentId = selectedParentId;
        selectedParentId = null;
        getBinding().etComment.setHint("Write a comment...");

        String localId = "local_" + UUID.randomUUID().toString().replace("-", "");
        Comment local = new Comment(
                localId,
                transactionId,
                pref.getUserId(),
                pref.getUsername(),
                text,
                parentId,
                Utils.getCurrentTimestamp(),
                Utils.getCurrentTimestamp()
        );
        local.setStatus(Comment.Status.SENDING);
        currentComments.add(local);
        if (parentId != null) adapter.forceExpand(parentId);
        renderComments();
        scrollAfterPatch();

        sendComment(localId, text, parentId);
    }

    private void retryComment(Comment comment) {
        int idx = findCommentIndex(comment.getId());
        if (idx < 0) return;

        Comment retry = new Comment(
                comment.getId(),
                comment.getTransactionId(),
                comment.getUserId(),
                comment.getUsername(),
                comment.getText(),
                comment.getParentId(),
                comment.getCreatedAt(),
                Utils.getCurrentTimestamp()
        );
        retry.setStatus(Comment.Status.SENDING);
        currentComments.set(idx, retry);
        renderComments();
        sendComment(retry.getId(), retry.getText(), retry.getParentId());
    }

    private void sendComment(String localId, String text, String parentId) {
        repository.postComment(transactionId, text, parentId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (getBinding() == null) return;
                getBinding().btnSend.setEnabled(true);

                Comment server = CommentHelper.parse(result);
                server.setStatus(Comment.Status.SENT);
                replaceLocalComment(localId, server);

                if (parentId != null) {
                    adapter.forceExpand(parentId);
                }
                renderComments();
                scrollAfterPatch();
            }

            @Override
            public void onError(Throwable error) {
                if (getBinding() == null) return;
                getBinding().btnSend.setEnabled(true);
                markLocalCommentFailed(localId);
                showToast("Failed to post comment", UiHelper.Status.ERROR);
            }
        });
    }

    private void patchRealtimeComment(Map<String, Object> payload) {
        String id = String.valueOf(payload.get("$id"));
        if (id == null || id.isEmpty() || "null".equalsIgnoreCase(id)) return;

        boolean isDelete = Boolean.TRUE.equals(payload.get("__isDelete"));
        if (isDelete) {
            int idx = findCommentIndex(id);
            if (idx >= 0) {
                currentComments.remove(idx);
                renderComments();
            }
            return;
        }

        Comment incoming = CommentHelper.parse(payload);
        incoming.setStatus(Comment.Status.SENT);

        int existingIdx = findCommentIndex(incoming.getId());
        if (existingIdx >= 0) {
            Comment existing = currentComments.get(existingIdx);
            if (!existing.equals(incoming)) {
                currentComments.set(existingIdx, incoming);
                renderComments();
            }
            return;
        }

        int pendingIdx = findMatchingPendingComment(incoming);
        if (pendingIdx >= 0) {
            currentComments.set(pendingIdx, incoming);
        } else {
            currentComments.add(incoming);
        }
        renderComments();
        scrollAfterPatch();
    }

    private void renderComments() {
        adapter.setAllComments(new ArrayList<>(currentComments));
        getBinding().tvEmpty.setVisibility(currentComments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int findCommentIndex(String id) {
        for (int i = 0; i < currentComments.size(); i++) {
            if (currentComments.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private int findMatchingPendingComment(Comment incoming) {
        for (int i = 0; i < currentComments.size(); i++) {
            Comment c = currentComments.get(i);
            boolean sameUser = c.getUserId().equals(incoming.getUserId());
            boolean sameText = c.getText().equals(incoming.getText());
            boolean sameParent = java.util.Objects.equals(c.getParentId(), incoming.getParentId());
            if (c.getStatus() == Comment.Status.SENDING && sameUser && sameText && sameParent) return i;
        }
        return -1;
    }

    private void replaceLocalComment(String localId, Comment server) {
        int localIdx = findCommentIndex(localId);
        int serverIdx = findCommentIndex(server.getId());
        if (serverIdx >= 0 && serverIdx != localIdx) {
            currentComments.remove(serverIdx);
            if (localIdx > serverIdx) localIdx--;
        }
        if (localIdx >= 0) currentComments.set(localIdx, server);
        else if (findCommentIndex(server.getId()) < 0) currentComments.add(server);
    }

    private void markLocalCommentFailed(String localId) {
        int idx = findCommentIndex(localId);
        if (idx < 0) return;
        Comment current = currentComments.get(idx);
        Comment failed = new Comment(
                current.getId(),
                current.getTransactionId(),
                current.getUserId(),
                current.getUsername(),
                current.getText(),
                current.getParentId(),
                current.getCreatedAt(),
                Utils.getCurrentTimestamp()
        );
        failed.setStatus(Comment.Status.FAILED);
        currentComments.set(idx, failed);
        renderComments();
    }

    private void scrollAfterPatch() {
        if (adapter.getItemCount() > 0) {
            getBinding().rvComments.post(() -> getBinding().rvComments.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }
}
