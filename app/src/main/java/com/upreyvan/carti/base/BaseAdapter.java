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
        bind(holder.binding, getItem(position));
    }

    protected abstract void bind(VB binding, T item);

    public static class BaseViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        public final VB binding;

        public BaseViewHolder(VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
