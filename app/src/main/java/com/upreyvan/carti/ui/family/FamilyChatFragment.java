package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentFamilyChatBinding;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.VoiceToTextHelper;

import java.util.ArrayList;

public class FamilyChatFragment extends BaseFragment<FragmentFamilyChatBinding> {
    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private VoiceToTextHelper voiceToTextHelper;

    @Override
    protected FragmentFamilyChatBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentFamilyChatBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
        setupToolbar();
        setupChatList();
        setupSuggestions();
        setupInput();
        setupVoiceInput();
        checkConnectionAndLoad();

        getBinding().btnBackContainer.setOnClickListener(v -> {
            Utils.hideKeyboard(requireContext(), getBinding().layoutInput.etInput);
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading && (viewModel.getMessages().getValue() == null || viewModel.getMessages().getValue().isEmpty())) {
                getBinding().layoutShimmer.setVisibility(View.VISIBLE);
                getBinding().layoutShimmer.startShimmer();
                getBinding().rvChat.setVisibility(View.GONE);
                getBinding().layoutEmpty.setVisibility(View.GONE);
            } else {
                getBinding().layoutShimmer.stopShimmer();
                getBinding().layoutShimmer.setVisibility(View.GONE);
            }
        });

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages == null) return;
            chatAdapter.submitList(new ArrayList<>(messages), () -> {
                if (!messages.isEmpty()) getBinding().rvChat.smoothScrollToPosition(messages.size() - 1);
            });
            updateEmptyState(messages.isEmpty());
        });

        viewModel.getIsAiThinking().observe(getViewLifecycleOwner(), thinking -> {
            if (thinking) {
                getBinding().tvSessionStatus.setText(R.string.carti_ai_is_thinking);
            }
        });

        viewModel.getSessionTimeRemaining().observe(getViewLifecycleOwner(), remaining -> {
            if (remaining > 0) {
                int seconds = (int) (remaining / 1000) + 1;
                getBinding().tvSessionStatus.setText(getString(R.string.ai_status_online_format, seconds));
                getBinding().tvSessionStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_green));
            } else {
                getBinding().tvSessionStatus.setText(R.string.ai_status_offline);
                getBinding().tvSessionStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.gray));
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                ToastHelper.show(requireContext(), error, com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                viewModel.clearError();
            }
        });

        RealtimeRepository.getInstance(requireContext()).getChatStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) viewModel.handleIncomingMessage(payload);
        });
    }

    private void checkConnectionAndLoad() {
        if (!Utils.isNetworkAvailable(requireContext())) {
            getBinding().layoutNoInternet.setVisibility(View.VISIBLE);
            getBinding().btnRetry.setOnClickListener(v -> checkConnectionAndLoad());
        } else {
            getBinding().layoutNoInternet.setVisibility(View.GONE);
            viewModel.loadHistory(false);
        }
    }

    private void setupChatList() {
        chatAdapter = new ChatAdapter();
        getBinding().rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvChat.setAdapter(chatAdapter);
        getBinding().rvChat.setVisibility(View.VISIBLE);
        getBinding().rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(-1) && dy < 0) {
                    viewModel.loadHistory(true);
                }
            }
        });
    }

    private void setupSuggestions() {
        AiSuggestionAdapter suggestionAdapter = new AiSuggestionAdapter();
        suggestionAdapter.setOnItemClickListener(suggestion -> {
            getBinding().layoutInput.etInput.setText(suggestion.getTitle());
            getBinding().layoutEmpty.setVisibility(View.GONE);
        });
        getBinding().rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupInput() {
        getBinding().layoutInput.etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && !s.toString().trim().isEmpty();
                getBinding().layoutInput.btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                getBinding().layoutInput.btnEmojiLike.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            String text = getBinding().layoutInput.etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                getBinding().layoutInput.etInput.setText("");
            }
        });

        getBinding().layoutInput.btnEmojiLike.setOnClickListener(v -> viewModel.sendMessage("👍"));
    }

    private void updateEmptyState(boolean empty) {
        getBinding().layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        getBinding().rvChat.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void setupVoiceInput() {
        voiceToTextHelper = new VoiceToTextHelper(requireContext(), new VoiceToTextHelper.VoiceCallback() {
            @Override
            public void onResult(String text) {
                getBinding().layoutInput.etInput.setText(text);
                resetVoiceUi();
            }

            @Override
            public void onError(String error) {
                resetVoiceUi();
            }

            @Override
            public void onPartialResult(String text) {
                getBinding().layoutInput.etInput.setHint(text);
            }

            @Override
            public void onEndOfSpeech() {
                resetVoiceUi();
            }
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> toggleVoiceRecognition());
    }

    private void toggleVoiceRecognition() {
        if (voiceToTextHelper.isListening()) {
            voiceToTextHelper.stopListening();
            resetVoiceUi();
        } else {
            getBinding().layoutInput.btnVoice.setColorFilter(requireContext().getColor(R.color.status_red));
            voiceToTextHelper.startListening();
        }
    }

    private void resetVoiceUi() {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                getBinding().layoutInput.btnVoice.clearColorFilter();
                getBinding().layoutInput.etInput.setHint(R.string.hint_ask_me);
            });
        }
    }

    private void setupToolbar() {
        getBinding().tvTitle.setText(R.string.family_chat_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceToTextHelper != null) voiceToTextHelper.destroy();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(true);
        }
    }
}
