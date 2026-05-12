package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.family.ChatAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentFamilyChatBinding;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.ViewGroup;

public class FamilyChatFragment extends BaseFragment<FragmentFamilyChatBinding> {

    private ChatAdapter chatAdapter;

    @Override
    protected FragmentFamilyChatBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentFamilyChatBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        setupToolbar();
        setupChatList();
        setupInput();
        loadSampleConvo();
    }

    private void setupChatList() {
        chatAdapter = new ChatAdapter();
        getBinding().rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvChat.setAdapter(chatAdapter);
    }

    private void loadSampleConvo() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("Juan (You)", "Magandang umaga pamilya! ☀️", "9:30 AM", false, 0));
        messages.add(new ChatMessage("Maria (Asawa)", "Good morning! 😊", "9:31 AM", true, 0));
        messages.add(new ChatMessage("Miguel (Anak)", "May update sa budget ngayong araw.", "9:32 AM", false,0));
        messages.add(new ChatMessage("Ana (Anak)", "Sige! Tingnan ko mamaya.", "9:33 AM", false, 0));
        messages.add(new ChatMessage("Juan (You)", "Let's keep saving together! 💪", "9:35 AM", true, 0));

        chatAdapter.submitList(messages);
        getBinding().rvChat.scrollToPosition(messages.size() - 1);
    }


    private void setupInput() {
        getBinding().layoutInput.etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().rvChat.postDelayed(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        getBinding().rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });

        getBinding().layoutInput.btnSend.setOnClickListener(v -> {
            getBinding().layoutInput.etInput.setText("");
        });

        getBinding().layoutInput.btnVoice.setOnClickListener(v -> {
            // Handle voice record
        });
    }

    private void setupToolbar() {
        getBinding().btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().btnInfo.setOnClickListener(v -> {
            // Chat info logic
        });
    }

    private void setupDynamicPadding() {
        // Handle top padding via Utils (status bar only)
        Utils.applySystemBarInsets(getBinding().layoutHeader, null, 0.3f, 0);

        // Custom local handle for Bottom Input + Keyboard
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().layoutInputContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            
            if (ime.bottom > 0) {
                lp.bottomMargin = ime.bottom;
            } else {
                lp.bottomMargin = systemBars.bottom + bottomNavHeight;
            }
            
            v.setLayoutParams(lp);
            return insets;
        });
    }
}