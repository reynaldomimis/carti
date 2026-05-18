package com.upreyvan.carti.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public abstract class BaseAdapter<T, VB extends ViewBinding> extends ListAdapter<T, BaseAdapter.BaseViewHolder<VB>> {

    protected BaseAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public BaseViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VB binding = inflateBinding(LayoutInflater.from(parent.getContext()), parent);
        return new BaseViewHolder<>(binding);
    }

    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<VB> holder, int position) {
        T item = getItem(position);
        bind(holder.binding, item);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    protected abstract void bind(VB binding, T item);

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public interface OnItemLongClickListener<T> {
        void onItemLongClick(T item);
    }

    private OnItemClickListener<T> listener;
    private OnItemLongClickListener<T> longClickListener;

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        this.longClickListener = listener;
    }

    public static class BaseViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        public final VB binding;

        public BaseViewHolder(VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
