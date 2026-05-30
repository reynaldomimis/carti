package com.upreyvan.carti.ui.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.ai.AiResult;
import com.upreyvan.carti.data.ai.IntentType; // Ensure this is imported
import com.upreyvan.carti.databinding.FragmentAiAssistantBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.util.Utils;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import com.upreyvan.carti.ui.family.ChatAdapter;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.data.repository.AiRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.upreyvan.carti.data.ai.CartiAiManager;

public class AiAssistantFragment extends BaseFragment<FragmentAiAssistantBinding> {

    private AiSuggestionAdapter suggestionAdapter;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private AiRepository aiRepository;

    private final ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    List<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        getBinding().layoutInput.etInput.setText(results.get(0));
                        getBinding().layoutInput.etInput.setSelection(results.get(0).length());
                    }
                }
            }
    );

    @Override
    public void onDestroyView() {
        CartiAiManager.getInstance(requireContext()).resetState();
        super.onDestroyView();
    }

    @Override
    protected FragmentAiAssistantBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAiAssistantBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aiRepository = new AiRepository(requireContext());
        setupDynamicPadding();
        setupChat();
        setupToolbar();
        setupSuggestions();
        setupKeyboardHandling();
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnCancelListener((message, position) -> {
            // Logic to actually cancel the database entry if it was already saved
            // For now, we just show a toast or log it
            Toast.makeText(requireContext(), R.string.msg_canceled, Toast.LENGTH_SHORT).show();
        });
        getBinding().rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvChat.setAdapter(chatAdapter);
    }

    private void setupKeyboardHandling() {
        getBinding().layoutInput.etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().scrollView.postDelayed(() -> {
                    if (isAdded() && getBinding() != null) {
                        View child = getBinding().scrollView.getChildAt(0);
                        if (child != null) {
                            getBinding().scrollView.smoothScrollTo(0, child.getBottom());
                        }
                    }
                }, 300);
            }
        });
    }

    private void setupSuggestions() {
        suggestionAdapter = new AiSuggestionAdapter();
        getBinding().rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvSuggestions.setAdapter(suggestionAdapter);

        suggestionAdapter.setOnItemClickListener(suggestion -> {
            getBinding().layoutInput.etInput.setText(suggestion.getTitle());
            getBinding().layoutInput.etInput.setSelection(suggestion.getTitle().length());
            getBinding().layoutInput.etInput.requestFocus();
        });

        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestionAdapter.submitList(suggestions);
    }

    private void setupToolbar() {
        getBinding().layoutInput.btnSend.setEnabled(false);
        getBinding().layoutInput.btnSend.setAlpha(0.5f);

        getBinding().layoutInput.etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                getBinding().layoutInput.btnSend.setEnabled(hasText);
                getBinding().layoutInput.btnSend.setAlpha(hasText ? 1.0f : 0.5f);
                getBinding().layoutInput.btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                getBinding().layoutInput.btnEmojiLike.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener emojiClickListener = v -> {
            v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            ).start();
            if (v instanceof android.widget.TextView) {
                String emoji = ((android.widget.TextView) v).getText().toString();
                sendMessage(emoji);
            }
        };

        getBinding().layoutInput.btnEmojiLike.setOnClickListener(emojiClickListener);

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            String message = getBinding().layoutInput.etInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sabihin ang iyong katanungan...");
                speechResultLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Voice search not supported", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String text) {

        getBinding().layoutInput.etInput.setText("");
        getBinding().scrollView.setVisibility(View.GONE);
        getBinding().rvChat.setVisibility(View.VISIBLE);

        chatMessages.add(new ChatMessage(
                getString(R.string.chat_sender_me),
                text,
                getCurrentSystemTime(),
                true,
                R.drawable.ic_person
        ));

        // Add Thinking Indicator
        ChatMessage thinkingMsg = new ChatMessage(true, false);
        chatMessages.add(thinkingMsg);
        
        chatAdapter.submitList(new ArrayList<>(chatMessages));
        scrollToBottom();


        aiRepository.processChat(text, getString(R.string.chat_sender_me), chatMessages, true, new AiManager.AiCallback() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    chatMessages.remove(thinkingMsg);
                    chatMessages.add(new ChatMessage(
                            getString(R.string.chat_sender_ai),
                            response,
                            getCurrentSystemTime(),
                            false,
                            R.drawable.ai_holder,
                            IntentType.UNKNOWN
                    ));
                    chatAdapter.submitList(new ArrayList<>(chatMessages));
                    scrollToBottom();
                });
            }

            @Override
            public void onError(Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    chatMessages.remove(thinkingMsg);
                    AiResult result = CartiAiManager.getInstance(requireContext()).processMessage(text);
                    chatMessages.add(new ChatMessage(
                            getString(R.string.chat_sender_ai),
                            result.getMessage(),
                            getCurrentSystemTime(),
                            false,
                            R.drawable.ai_holder,
                            result.getIntent()
                    ));
                    chatAdapter.submitList(new ArrayList<>(chatMessages));
                    scrollToBottom();
                });
            }
        });
    }

    private String getCurrentSystemTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    private void scrollToBottom() {
        getBinding().rvChat.postDelayed(() -> {
            if (chatAdapter.getItemCount() > 0) {
                getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }
        }, 200);
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutHeader, null, 0.3f, 0);
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutBottom, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height) + getResources().getDimensionPixelSize(R.dimen.spacing_medium);
            int paddingBottom;

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                paddingBottom = ime.bottom;
            } else {
                paddingBottom = systemBars.bottom + bottomNavHeight;
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), paddingBottom);
            return insets;
        });
    }
}