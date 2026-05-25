package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentCommentsBottomSheetBinding;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class CommentsBottomSheetFragment extends BaseBottomSheetFragment<FragmentCommentsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    
    private String transactionId;
    private String selectedParentId = null;
    private CommentAdapter adapter;
    private TransactionRepository repository;
    private ApiHelper apiHelper;
    private PreferenceManager pref;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = TransactionRepository.getInstance(requireContext());
        apiHelper = new ApiHelper(requireContext());
        pref = new PreferenceManager(requireContext());

        setupUI();
        loadComments();
        setupRealtime();
    }

    private void setupRealtime() {
        RealtimeRepository.getInstance(requireContext()).getCommentStream().observe(getViewLifecycleOwner(), payload -> {
            String txnId = String.valueOf(payload.get("transactionId"));
            if (transactionId.equals(txnId)) {
                loadComments(); // Refresh list on any change to comments for this transaction
            }
        });
    }

    private void setupUI() {
        adapter = new CommentAdapter(pref.getUserId(), pref.isAdmin());
        adapter.setOnCommentInteractionListener(new CommentAdapter.OnCommentInteractionListener() {
            @Override
            public void onCommentClicked(Comment comment, View view) {
                showCommentOptions(comment, view);
            }

            @Override
            public void onDeleteComment(Comment comment) {
                confirmDelete(comment);
            }
        });
        getBinding().rvComments.setAdapter(adapter);

        // Set current user avatar
        Glide.with(this)
                .load("https://cloud.appwrite.io/v1/avatars/initials?name=" + pref.getUsername() + "&project=carti")
                .placeholder(R.drawable.ai_holder)
                .into(getBinding().ivCurrentUserAvatar);

        getBinding().btnSend.setOnClickListener(v -> postComment());
    }

    private void showCommentOptions(Comment comment, View view) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        
        String currentUserId = pref.getUserId();
        String commentUserId = comment.getUserId();
        
        // Clean values for comparison
        String uid = (currentUserId != null) ? currentUserId.trim() : "";
        if (uid.equalsIgnoreCase("null")) uid = "";

        String itemUid = (commentUserId != null) ? commentUserId.trim() : "";
        if (itemUid.equalsIgnoreCase("null")) itemUid = "";
        
        boolean isMine = !uid.isEmpty() && uid.equalsIgnoreCase(itemUid);
        boolean isAdmin = pref.isAdmin();

        popup.getMenu().add(0, 1, 0, "Reply");
        if (isMine || isAdmin) {
            popup.getMenu().add(0, 2, 1, "Delete");
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                startReplyMode(comment);
            } else if (item.getItemId() == 2) {
                confirmDelete(comment);
            }
            return true;
        });
        popup.show();
    }

    private void startReplyMode(Comment comment) {
        selectedParentId = comment.getId();
        getBinding().etComment.setHint("Replying to @" + comment.getUsername() + "...");
        getBinding().etComment.requestFocus();
        com.upreyvan.carti.util.Utils.showKeyboard(requireContext(), getBinding().etComment);
    }

    private void confirmDelete(Comment comment) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.removeComment(comment.getId(), new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                        @Override
                        public void onSuccess(Map<String, Object> result) {
                            loadComments();
                        }

                        @Override
                        public void onError(Throwable error) {
                            showToast("Failed to delete comment", ToastHelper.Status.ERROR);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadComments() {
        getBinding().progressBar.setVisibility(View.VISIBLE);
        apiHelper.getComments(transactionId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().progressBar.setVisibility(View.GONE);
                    List<Comment> comments = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        comments.add(mapPayloadToComment(doc.getData()));
                    }
                    adapter.submitList(comments);
                    getBinding().tvEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().progressBar.setVisibility(View.GONE);
                    showToast("Failed to load comments", ToastHelper.Status.ERROR);
                });
            }
        });
    }

    private void postComment() {
        String text = getBinding().etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        getBinding().btnSend.setEnabled(false);
        repository.postComment(transactionId, text, selectedParentId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().etComment.setText("");
                    getBinding().btnSend.setEnabled(true);
                    selectedParentId = null;
                    getBinding().etComment.setHint("Write a comment...");
                    loadComments();
                });
            }

            @Override
            public void onError(Throwable error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().btnSend.setEnabled(true);
                    showToast("Failed to post comment", ToastHelper.Status.ERROR);
                });
            }
        });
    }

    private Comment mapPayloadToComment(Map<String, Object> data) {
        // Debug: I-print natin ang buong data para makita ang exact fields mula sa Appwrite
        android.util.Log.d("CommentDebug", "Appwrite Data: " + data.toString());

        return new Comment(
                safeString(data.get("$id")),
                safeString(data.get("transactionId")),
                safeString(data.get("userId")),
                data.get("username") != null && !String.valueOf(data.get("username")).equals("null") 
                        ? String.valueOf(data.get("username")) : "Anonymous",
                safeString(data.get("text")),
                data.get("parentId") != null && !String.valueOf(data.get("parentId")).equals("null") 
                        ? String.valueOf(data.get("parentId")) : null,
                safeString(data.get("$createdAt")),
                safeString(data.get("$updatedAt"))
        );
    }

    private String safeString(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj);
        return "null".equals(s) ? "" : s;
    }
}
