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
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.FragmentCommentsBottomSheetBinding;
import com.upreyvan.carti.util.AvatarHelper;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentsBottomSheetFragment extends BaseBottomSheetFragment<FragmentCommentsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    
    private String transactionId;
    private String selectedParentId = null;
    private String lastPostedParentId = null;
    private CommentAdapter adapter;
    private TransactionRepository repository;
    private PreferenceManager pref;
    private boolean shouldScrollToBottom = false;

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
                    loadComments(false);
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
                    repository.removeComment(comment.getId(), new AppwriteCallback<>() {
                        @Override public void onSuccess(Map<String, Object> result) {
                            loadComments();
                        }
                        @Override public void onError(Throwable error) {
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
                adapter.setAllComments(result);
                getBinding().tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);


                if (shouldScrollToBottom && !result.isEmpty()) {
                    getBinding().rvComments.post(() -> {
                        if (lastPostedParentId == null) {
                            getBinding().rvComments.smoothScrollToPosition(adapter.getItemCount() - 1);
                        } else {
                            List<Comment> current = adapter.getCurrentList();
                            for (int i = 0; i < current.size(); i++) {
                                if (current.get(i).getId().equals(lastPostedParentId)) {
                                    getBinding().rvComments.smoothScrollToPosition(i);
                                    break;
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
        shouldScrollToBottom = true;
        lastPostedParentId = selectedParentId;
        repository.postComment(transactionId, text, selectedParentId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                getBinding().etComment.setText("");
                getBinding().btnSend.setEnabled(true);

                if (selectedParentId != null) {
                    adapter.forceExpand(selectedParentId);
                }

                selectedParentId = null;
                getBinding().etComment.setHint("Write a comment...");
                
                loadComments(false);
            }

            @Override
            public void onError(Throwable error) {
                getBinding().btnSend.setEnabled(true);
                showToast("Failed to post comment", UiHelper.Status.ERROR);
            }
        });
    }
}
