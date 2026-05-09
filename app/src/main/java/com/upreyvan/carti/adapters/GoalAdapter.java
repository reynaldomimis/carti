package com.upreyvan.carti.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.model.Goal;

import java.util.Locale;

public class GoalAdapter extends BaseAdapter<Goal, ItemGoalBinding> {

    public GoalAdapter() {
        super(new DiffUtil.ItemCallback<Goal>() {
            @Override
            public boolean areItemsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
                return oldItem.getCurrentAmount() == newItem.getCurrentAmount() &&
                        oldItem.getTargetAmount() == newItem.getTargetAmount() &&
                        oldItem.getTargetDate().equals(newItem.getTargetDate());
            }
        });
    }

    @Override
    protected ItemGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemGoalBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(ItemGoalBinding binding, Goal item) {
        binding.tvGoalTitle.setText(item.getTitle());
        binding.ivGoalIcon.setImageResource(item.getImageRes());
        binding.ivGoalIcon.setBackgroundColor(item.getBackgroundColor());
        
        // Gamitin ang bagong string formats para sa pera at petsa
        String progressText = String.format(Locale.getDefault(), 
                binding.getRoot().getContext().getString(R.string.goal_progress_amount_format),
                String.format(Locale.getDefault(), "%,.0f", item.getCurrentAmount()),
                String.format(Locale.getDefault(), "%,.0f", item.getTargetAmount()));
        
        binding.tvGoalProgressAmount.setText(progressText);
        binding.progressIndicator.setProgress(item.getProgress());
        
        // Porsyento format
        binding.tvPercentage.setText(binding.getRoot().getContext().getString(R.string.percentage_format, item.getProgress()));
        
        // Petsa format
        binding.tvTargetDate.setText(binding.getRoot().getContext().getString(R.string.target_date_label_format, item.getTargetDate()));
    }
}
