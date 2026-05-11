package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.adapters.ChatAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentFamilyChatBinding;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

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
        messages.add(new ChatMessage("Juan (You)", "Magandang umaga pamilya! ☀️", "9:30 AM", false));
        messages.add(new ChatMessage("Maria (Asawa)", "Good morning! 😊", "9:31 AM", true));
        messages.add(new ChatMessage("Miguel (Anak)", "May update sa budget ngayong araw.", "9:32 AM", false));
        messages.add(new ChatMessage("Ana (Anak)", "Sige! Tingnan ko mamaya.", "9:33 AM", false));
        messages.add(new ChatMessage("Juan (You)", "Let's keep saving together! 💪", "9:35 AM", true));

        chatAdapter.submitList(messages);
        getBinding().rvChat.scrollToPosition(messages.size() - 1);
    }


    private void setupInput() {
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
        Utils.applySystemBarInsets(
                getBinding().layoutHeader,
                getBinding().layoutInputContainer,
                0.3f,
               0
        );
    }
}