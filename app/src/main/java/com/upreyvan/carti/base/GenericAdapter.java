package com.upreyvan.carti.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewbinding.ViewBinding;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class GenericAdapter<T, VB extends ViewBinding> extends BaseAdapter<T, VB> {
    public GenericAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback,
                          BiFunction<LayoutInflater, ViewGroup, VB> bindingInflater,
                          Binder<VB, T> binder) {
        super(diffCallback, bindingInflater, binder);
    }

    @Deprecated
    public GenericAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback,
                          BiFunction<LayoutInflater, ViewGroup, VB> bindingInflater,
                          BiConsumer<VB, T> binder) {
        super(diffCallback, bindingInflater, binder);
    }
}
