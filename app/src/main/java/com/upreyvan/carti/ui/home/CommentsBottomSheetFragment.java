package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentCommentsBottomSheetBinding;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommentsBottomSheetFragment extends BaseBottomSheetFragment<FragmentCommentsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    
    private String transactionId;
    private String selectedParentId = null;
    private CommentAdapter adapter;
    private TransactionRepository repository;
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
        pref = PreferenceManager.getInstance(requireContext());

        setupUI();
        loadComments();
        setupRealtime();
    }

    private void setupRealtime() {
        RealtimeRepository.getInstance(requireContext()).getCommentStream().observe(getViewLifecycleOwner(), payload -> {
            String txnId = String.valueOf(payload.get("transactionId"));
            if (Objects.equals(transactionId, txnId)) {
                loadComments();
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

        Glide.with(this)
                .load("https://cloud.appwrite.io/v1/avatars/initials?name=" + pref.getUsername() + "&project=carti")
                .placeholder(R.drawable.ai_holder)
                .into(getBinding().ivCurrentUserAvatar);

        getBinding().btnSend.setOnClickListener(v -> postComment());
    }

    private void showCommentOptions(Comment comment, View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        
        String currentUserId = Objects.requireNonNullElse(pref.getUserId(), "").trim();
        if (currentUserId.equalsIgnoreCase("null")) currentUserId = "";

        String commentUserId = Objects.requireNonNullElse(comment.getUserId(), "").trim();
        if (commentUserId.equalsIgnoreCase("null")) commentUserId = "";
        
        boolean isMine = !currentUserId.isEmpty() && currentUserId.equalsIgnoreCase(commentUserId);
        boolean isAdmin = pref.isAdmin();

        popup.getMenu().add(0, 1, 0, "Reply");
        if (isMine || isAdmin) {
            popup.getMenu().add(0, 2, 1, "Delete");
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1 -> startReplyMode(comment);
                case 2 -> confirmDelete(comment);
            }
            return true;
        });
        popup.show();
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
                .setPositiveButton("Delete", (dialog, which) -> repository.removeComment(comment.getId(), new AppwriteCallback<>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        loadComments();
                    }

                    @Override
                    public void onError(Throwable error) {
                        showToast("Failed to delete comment", ToastHelper.Status.ERROR);
                    }
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadComments() {
        getBinding().progressBar.setVisibility(View.VISIBLE);
        repository.getComments(transactionId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(List<Comment> result) {
                getBinding().progressBar.setVisibility(View.GONE);
                adapter.submitList(result);
                getBinding().tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Throwable error) {
                getBinding().progressBar.setVisibility(View.GONE);
                showToast("Failed to load comments", ToastHelper.Status.ERROR);
            }
        });
    }

    private void postComment() {
        String text = getBinding().etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        getBinding().btnSend.setEnabled(false);
        repository.postComment(transactionId, text, selectedParentId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                getBinding().etComment.setText("");
                getBinding().btnSend.setEnabled(true);
                selectedParentId = null;
                getBinding().etComment.setHint("Write a comment...");
                loadComments();
            }

            @Override
            public void onError(Throwable error) {
                getBinding().btnSend.setEnabled(true);
                showToast("Failed to post comment", ToastHelper.Status.ERROR);
            }
        });
    }
}
