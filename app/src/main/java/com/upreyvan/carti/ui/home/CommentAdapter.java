package com.upreyvan.carti.ui.home;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemCommentBinding;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.AvatarHelper;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommentAdapter extends BaseAdapter<Comment, ItemCommentBinding> {

    private static final int MAX_NESTING_DEPTH = 1;

    private final String currentUserId;
    private final boolean isAdmin;
    private final int depth;
    private OnCommentInteractionListener interactionListener;
    private List<Comment> allComments = new ArrayList<>();
    private final Map<String, Boolean> expandedStates = new HashMap<>();

    public CommentAdapter(String currentUserId, boolean isAdmin) {
        this(currentUserId, isAdmin, 0);
    }

    private CommentAdapter(String currentUserId, boolean isAdmin, int depth) {
        super(Comment.DIFF_CALLBACK,
                (inflater, parent) -> ItemCommentBinding.inflate(inflater, parent, false),
                (binding, item, position, count) -> {
                    // Default binder
                });
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.depth = depth;
    }

    public void setAllComments(List<Comment> comments) {
        this.allComments = new ArrayList<>(comments);
        if (depth == 0) {
            List<Comment> topLevel = comments.stream()
                    .filter(c -> c.getParentId() == null || c.getParentId().isEmpty() || c.getParentId().equalsIgnoreCase("null"))
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(c -> {
                        long replyCount = comments.stream().filter(r -> c.getId().equals(r.getParentId())).count();
                        c.setChildSignature("v" + replyCount);
                        return c;
                    })
                    .collect(Collectors.toList());
            submitList(topLevel);
        } else {
            submitList(comments);
        }
    }

    public void setOnCommentInteractionListener(OnCommentInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void forceExpand(String commentId) {
        if (commentId == null) return;
        expandedStates.put(commentId, true);
    }

    @Override
    public void onBindViewHolder(ViewHolder<ItemCommentBinding> holder, int position) {
        Comment item = getItem(position);
        ItemCommentBinding binding = holder.binding;

        String rawUid = (currentUserId != null) ? currentUserId.trim() : "";
        String uid = rawUid.equalsIgnoreCase("null") ? "" : rawUid;
        
        String rawItemUid = (item.getUserId() != null) ? item.getUserId().trim() : "";
        String itemUid = rawItemUid.equalsIgnoreCase("null") ? "" : rawItemUid;

        boolean isOwner = !uid.isEmpty() && uid.equalsIgnoreCase(itemUid);
        boolean canDelete = isOwner || isAdmin;

        binding.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        binding.btnDelete.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onDeleteComment(item);
        });

        binding.btnReply.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onReplyComment(item);
        });

        int basePadding = binding.getRoot().getContext().getResources().getDimensionPixelSize(R.dimen.spacing_medium);
        float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
        int nestingPadding = (int) (depth * 24 * density);
        binding.getRoot().setPadding(basePadding + nestingPadding, binding.getRoot().getPaddingTop(), basePadding, binding.getRoot().getPaddingBottom());

        binding.tvUserName.setText(item.getUsername());
        binding.tvCommentText.setText(item.getText());
        binding.tvTime.setText(Utils.getTimeAgo(Utils.getMillisFromIso(item.getCreatedAt())));

        AvatarHelper.loadUserAvatar(binding.getRoot().getContext(), binding.ivAvatar, item.getUsername());

        if (depth >= MAX_NESTING_DEPTH) {
            binding.layoutRepliesContainer.setVisibility(View.GONE);
            binding.btnReply.setVisibility(View.VISIBLE);
        } else {
            List<Comment> threadReplies = new ArrayList<>();
            collectAllDescendants(item.getId(), allComments, threadReplies);
            threadReplies.sort(Comparator.comparing(Comment::getCreatedAt));

            if (threadReplies.isEmpty()) {
                binding.layoutRepliesContainer.setVisibility(View.GONE);
            } else {
                binding.layoutRepliesContainer.setVisibility(View.VISIBLE);
                
                Boolean expandedObj = expandedStates.get(item.getId());
                boolean isExpanded = expandedObj != null && expandedObj;
                
                binding.rvReplies.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                binding.tvViewReplies.setText(isExpanded ? "Hide replies" : String.format(Locale.getDefault(), "View %d replies", threadReplies.size()));

                binding.tvViewReplies.setOnClickListener(v -> {
                    expandedStates.put(item.getId(), !isExpanded);
                    notifyItemChanged(position);
                });

                if (isExpanded) {
                    CommentAdapter replyAdapter = new CommentAdapter(currentUserId, isAdmin, depth + 1);
                    replyAdapter.setOnCommentInteractionListener(interactionListener);
                    binding.rvReplies.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
                    binding.rvReplies.setAdapter(replyAdapter);
                    replyAdapter.setAllComments(threadReplies);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onReplyComment(item);
        });
    }

    private void collectAllDescendants(String parentId, List<Comment> all, List<Comment> result) {
        for (Comment c : all) {
            if (Objects.equals(parentId, c.getParentId())) {
                result.add(c);
                collectAllDescendants(c.getId(), all, result);
            }
        }
    }

    public interface OnCommentInteractionListener {
        void onReplyComment(Comment comment);
        void onDeleteComment(Comment comment);
    }
}
