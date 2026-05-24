package com.upreyvan.carti.ui.home;

import android.view.View;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemCommentBinding;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.Utils;

public class CommentAdapter extends BaseAdapter<Comment, ItemCommentBinding> {

    private final String currentUserId;
    private final boolean isAdmin;
    private OnCommentInteractionListener interactionListener;

    public CommentAdapter(String currentUserId, boolean isAdmin) {
        super(Comment.DIFF_CALLBACK,
                (inflater, parent) -> ItemCommentBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    // Default binder
                });
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
    }

    public void setOnCommentInteractionListener(OnCommentInteractionListener listener) {
        this.interactionListener = listener;
    }

    @Override
    public void onBindViewHolder(ViewHolder<ItemCommentBinding> holder, int position) {
        Comment item = getItem(position);
        ItemCommentBinding binding = holder.binding;

        String uid = (currentUserId != null) ? currentUserId.trim() : "";
        if (uid.equalsIgnoreCase("null")) uid = "";

        String itemUid = (item.getUserId() != null) ? item.getUserId().trim() : "";
        if (itemUid.equalsIgnoreCase("null")) itemUid = "";


        boolean isOwner = !uid.isEmpty() && uid.equalsIgnoreCase(itemUid);
        boolean canDelete = isOwner || isAdmin;

        binding.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        binding.btnDelete.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onDeleteComment(item);
            }
        });

        float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
        String parentId = item.getParentId();
        boolean isReply = parentId != null && !parentId.isEmpty() && !parentId.equalsIgnoreCase("null");
        int marginStart = isReply ? (int) (32 * density) : 0;
        
        android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) binding.getRoot().getLayoutParams();
        if (params != null) {
            params.leftMargin = marginStart;
            binding.getRoot().setLayoutParams(params);
        }

        binding.tvUserName.setText(item.getUsername());
        binding.tvCommentText.setText(item.getText());
        binding.tvTime.setText(Utils.getTimeAgo(Utils.getMillisFromIso(item.getCreatedAt())));

        Glide.with(binding.getRoot().getContext())
                .load("https://cloud.appwrite.io/v1/avatars/initials?name=" + item.getUsername() + "&project=carti")
                .placeholder(R.drawable.ai_holder)
                .error(R.drawable.ai_holder)
                .into(binding.ivAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onCommentClicked(item, v);
            }
        });
    }

    public interface OnCommentInteractionListener {
        void onCommentClicked(Comment comment, View view);
        void onDeleteComment(Comment comment);
    }
}
