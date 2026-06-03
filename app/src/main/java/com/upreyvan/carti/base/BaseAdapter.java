package com.upreyvan.carti.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class BaseAdapter<T, VB extends ViewBinding> extends ListAdapter<T, BaseAdapter.ViewHolder<VB>> {

    public interface Binder<VB extends ViewBinding, T> {
        void bind(VB binding, T item, int position, int totalCount);
    }

    private final BiFunction<LayoutInflater, ViewGroup, VB> bindingInflater;
    private final Binder<VB, T> binder;
    protected OnItemClickListener<T> listener;
    protected OnItemLongClickListener<T> longClickListener;

    protected BaseAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback,
                        BiFunction<LayoutInflater, ViewGroup, VB> bindingInflater,
                        Binder<VB, T> binder) {
        super(diffCallback);
        this.bindingInflater = bindingInflater;
        this.binder = binder;
    }

    @Deprecated
    protected BaseAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback,
                        BiFunction<LayoutInflater, ViewGroup, VB> bindingInflater,
                        java.util.function.BiConsumer<VB, T> binder) {
        this(diffCallback, bindingInflater, (b, item, pos, count) -> binder.accept(b, item));
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public interface OnItemLongClickListener<T> {
        boolean onItemLongClick(T item);
    }

    @Override
    public T getItem(int position) {
        return super.getItem(position);
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VB binding = bindingInflater.apply(LayoutInflater.from(parent.getContext()), parent);
        return new ViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<VB> holder, int position) {
        T item = getItem(position);
        binder.bind(holder.binding, item, position, getItemCount());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) return longClickListener.onItemLongClick(item);
            return false;
        });
    }

    public static class ViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        public final VB binding;
        public ViewHolder(@NonNull VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
