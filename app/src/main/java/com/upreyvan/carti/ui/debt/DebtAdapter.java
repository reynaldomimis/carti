package com.upreyvan.carti.ui.debt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemDebtBinding;
import com.upreyvan.carti.model.Debt;
import java.util.ArrayList;
import java.util.List;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {

    private List<Debt> debts = new ArrayList<>();
    private OnDebtClickListener listener;
    private boolean isLoading = false;

    public interface OnDebtClickListener {
        void onDebtClick(Debt debt);
    }

    public void setOnDebtClickListener(OnDebtClickListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    public void setDebts(List<Debt> debts) {
        this.debts = debts;
        this.isLoading = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isLoading ? 1 : 0;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDebtBinding binding = ItemDebtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DebtViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        if (isLoading) {
            holder.showShimmer();
        } else {
            holder.bind(debts.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return isLoading ? 5 : debts.size();
    }

    class DebtViewHolder extends RecyclerView.ViewHolder {
        private final ItemDebtBinding binding;

        public DebtViewHolder(ItemDebtBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Debt debt) {
            binding.shimmerView.getRoot().setVisibility(View.GONE);
            binding.layoutContent.setVisibility(View.VISIBLE);
            binding.tvPersonName.setText(debt.getPersonName());
            binding.tvDescription.setText(debt.getDescription());
            binding.tvDate.setText(debt.getDate());
            binding.tvAmount.setText(String.format("₱%.0f", debt.getAmount()));
            binding.ivAvatar.setImageResource(debt.getAvatarResId());

            if (debt.isPaid()) {
                binding.tvStatus.setText(R.string.status_paid);
                binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(R.color.status_green));
            } else {
                binding.tvStatus.setText(R.string.status_not_paid);
                binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(R.color.status_red));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDebtClick(debt);
                }
            });
        }

        public void showShimmer() {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
        }
    }
}