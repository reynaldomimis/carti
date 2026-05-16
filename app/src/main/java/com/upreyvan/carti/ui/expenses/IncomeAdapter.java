package com.upreyvan.carti.ui.expenses;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemIncomeBinding;
import com.upreyvan.carti.model.Income;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.ViewHolder> {
    private List<Income> incomes = new ArrayList<>();
    private OnIncomeLongClickListener longClickListener;

    public interface OnIncomeLongClickListener {
        void onIncomeLongClick(Income income);
    }

    public void setIncomes(List<Income> incomes) {
        this.incomes = incomes;
        notifyDataSetChanged();
    }

    public void setOnIncomeLongClickListener(OnIncomeLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemIncomeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Income income = incomes.get(position);
        holder.binding.tvSource.setText(income.getSource());
        holder.binding.tvAmount.setText(String.format(Locale.getDefault(), "+₱%,.2f", income.getAmount()));
        
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(income.getCreatedAt());
            holder.binding.tvDate.setText(outputFormat.format(date));
        } catch (Exception e) {
            holder.binding.tvDate.setText(income.getCreatedAt());
        }
        
        if (income.getSource().toLowerCase().contains("salary")) {
            holder.binding.ivIcon.setImageResource(com.upreyvan.carti.R.drawable.ic_calendar);
            holder.binding.viewIconBg.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(holder.itemView.getContext(), com.upreyvan.carti.R.color.log_food));
        } else {
            holder.binding.ivIcon.setImageResource(com.upreyvan.carti.R.drawable.ic_chart);
            holder.binding.viewIconBg.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(holder.itemView.getContext(), com.upreyvan.carti.R.color.log_fare));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onIncomeLongClick(income);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return incomes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemIncomeBinding binding;
        ViewHolder(ItemIncomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
