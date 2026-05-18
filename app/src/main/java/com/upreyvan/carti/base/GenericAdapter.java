package com.upreyvan.carti.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewbinding.ViewBinding;

import java.util.function.BiConsumer;

public class GenericAdapter<T, VB extends ViewBinding> extends BaseAdapter<T, VB> {

    public interface Inflater<VB> {
        VB inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent);
    }

    private final Inflater<VB> inflater;
    private final BiConsumer<VB, T> binder;

    public GenericAdapter(
            @NonNull DiffUtil.ItemCallback<T> diffCallback,
            @NonNull Inflater<VB> inflater,
            @NonNull BiConsumer<VB, T> binder) {
        super(diffCallback);
        this.inflater = inflater;
        this.binder = binder;
    }

    @Override
    protected VB inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return this.inflater.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(VB binding, T item) {
        binder.accept(binding, item);
    }
}
