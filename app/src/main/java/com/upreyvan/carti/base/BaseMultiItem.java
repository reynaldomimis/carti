package com.upreyvan.carti.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import java.util.List;

public interface BaseMultiItem {
    int getViewType();
    String getItemUniqueId();
    @NonNull ViewBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);
    void bind(@NonNull ViewBinding binding, int position, int totalCount);

    default void bind(@NonNull ViewBinding binding, int position, int totalCount, @NonNull List<Object> payloads) {
        bind(binding, position, totalCount);
    }

    default Object getChangePayload(@NonNull BaseMultiItem newItem) {
        return null;
    }
}
