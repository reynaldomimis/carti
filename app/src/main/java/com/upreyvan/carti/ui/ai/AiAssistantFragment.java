package com.upreyvan.carti.ui.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.ai.AiSuggestionAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAiAssistantBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AiAssistantFragment extends BaseFragment<FragmentAiAssistantBinding> {

    private AiSuggestionAdapter suggestionAdapter;

    @Override
    protected FragmentAiAssistantBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAiAssistantBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        setupToolbar();
        setupSuggestions();
    }

    private void setupSuggestions() {
        suggestionAdapter = new AiSuggestionAdapter();
        getBinding().rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvSuggestions.setAdapter(suggestionAdapter);

        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                getString(R.string.ai_suggestion_1),
                android.R.drawable.ic_menu_myplaces,
                ContextCompat.getColor(requireContext(), R.color.icon_food),
                ContextCompat.getColor(requireContext(), R.color.log_food)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.ai_suggestion_2),
                android.R.drawable.btn_star,
                ContextCompat.getColor(requireContext(), R.color.icon_store),
                ContextCompat.getColor(requireContext(), R.color.log_store)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.ai_suggestion_3),
                android.R.drawable.ic_menu_search,
                ContextCompat.getColor(requireContext(), R.color.icon_fare),
                ContextCompat.getColor(requireContext(), R.color.log_fare)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.ai_suggestion_4),
                android.R.drawable.ic_menu_manage,
                ContextCompat.getColor(requireContext(), R.color.icon_debt),
                ContextCompat.getColor(requireContext(), R.color.log_debt)
        ));
        suggestions.add(new AiSuggestion(
                getString(R.string.ai_suggestion_5),
                android.R.drawable.ic_menu_gallery,
                ContextCompat.getColor(requireContext(), R.color.icon_load),
                ContextCompat.getColor(requireContext(), R.color.log_load)
        ));

        suggestionAdapter.submitList(suggestions);
    }

    private void setupToolbar() {
        getBinding().btnRefresh.setOnClickListener(v -> {
        });

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            // Handle send
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            // Handle voice
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutHeader,
                getBinding().layoutBottom,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_medium)
        );
    }
}