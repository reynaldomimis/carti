package com.upreyvan.carti.base;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import java.util.List;
import java.util.Objects;

public class BaseMultiAdapter extends ListAdapter<BaseMultiItem, BaseMultiAdapter.MultiViewHolder> {

    private final SparseArray<BaseMultiItem> typeTemplates = new SparseArray<>();

    public BaseMultiAdapter() {
        super(new DiffUtil.ItemCallback<>() {
            @Override public boolean areItemsTheSame(@NonNull BaseMultiItem old, @NonNull BaseMultiItem newIt) {
                return old.getViewType() == newIt.getViewType() && Objects.equals(old.getItemUniqueId(), newIt.getItemUniqueId());
            }
            @Override public boolean areContentsTheSame(@NonNull BaseMultiItem old, @NonNull BaseMultiItem newIt) {
                return Objects.equals(old, newIt);
            }
            @Override public Object getChangePayload(@NonNull BaseMultiItem old, @NonNull BaseMultiItem newIt) {
                return old.getChangePayload(newIt);
            }
        });
    }

    @Override public int getItemViewType(int position) {
        BaseMultiItem item = getItem(position);
        if (typeTemplates.get(item.getViewType()) == null) {
            typeTemplates.put(item.getViewType(), item);
        }
        return item.getViewType();
    }

    @NonNull
    @Override public MultiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BaseMultiItem template = typeTemplates.get(viewType);
        if (template == null) {
            for (BaseMultiItem item : getCurrentList()) {
                if (item.getViewType() == viewType) {
                    template = item;
                    typeTemplates.put(viewType, item);
                    break;
                }
            }
        }
        
        if (template == null) throw new IllegalStateException("ViewType not found: " + viewType);
        return new MultiViewHolder(template.inflateBinding(LayoutInflater.from(parent.getContext()), parent));
    }

    @Override public void onBindViewHolder(@NonNull MultiViewHolder holder, int position) {
        getItem(position).bind(holder.binding, position, getItemCount());
    }

    @Override public void onBindViewHolder(@NonNull MultiViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            getItem(position).bind(holder.binding, position, getItemCount(), payloads);
        }
    }

    public static class MultiViewHolder extends RecyclerView.ViewHolder {
        public final ViewBinding binding;
        public MultiViewHolder(@NonNull ViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
