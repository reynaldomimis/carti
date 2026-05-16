package com.upreyvan.carti.ui.goals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.model.Goal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalAdapter extends BaseAdapter<Goal, ItemGoalBinding> {

    private boolean isLoading = false;

    public GoalAdapter() {
        super(new DiffUtil.ItemCallback<Goal>() {
            @Override
            public boolean areItemsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
                if (oldItem.getTitle() == null || newItem.getTitle() == null) return false;
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
                return oldItem.getCurrentAmount() == newItem.getCurrentAmount() &&
                        oldItem.getTargetAmount() == newItem.getTargetAmount() &&
                        (oldItem.getTargetDate() != null && oldItem.getTargetDate().equals(newItem.getTargetDate()));
            }
        });
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            List<Goal> placeholders = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                placeholders.add(new Goal());
            }
            submitList(placeholders);
        }
    }

    @Override
    protected ItemGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemGoalBinding.inflate(inflater, parent, false);
    }

    public interface OnGoalClickListener {
        void onGoalClick(Goal goal);
    }

    private OnGoalClickListener listener;

    public void setOnGoalClickListener(OnGoalClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void bind(ItemGoalBinding binding, Goal item) {
        if (isLoading) {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
            return;
        }

        binding.shimmerView.getRoot().setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onGoalClick(item);
        });
        binding.tvGoalTitle.setText(item.getTitle());
        binding.ivGoalIcon.setImageResource(item.getImageRes());
        binding.ivGoalIcon.setBackgroundColor(item.getBackgroundColor());
        
        // Gamitin ang bagong string formats para sa pera at petsa
        String progressText = String.format(Locale.getDefault(), 
                binding.getRoot().getContext().getString(R.string.goal_progress_amount_format),
                String.format(Locale.getDefault(), "₱%,.0f", item.getCurrentAmount()),
                String.format(Locale.getDefault(), "₱%,.0f", item.getTargetAmount()));
        
        binding.tvGoalProgressAmount.setText(progressText);
        binding.progressIndicator.setProgress(item.getProgress());
        
        // Porsyento format
        binding.tvPercentage.setText(binding.getRoot().getContext().getString(R.string.percentage_format, item.getProgress()));
        
        // Petsa format
        binding.tvTargetDate.setText(binding.getRoot().getContext().getString(R.string.target_date_label_format, item.getTargetDate()));
    }
}
