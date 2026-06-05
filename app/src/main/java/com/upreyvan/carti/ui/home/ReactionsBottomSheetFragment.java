package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentReactionsBottomSheetBinding;
import com.upreyvan.carti.model.Reactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class ReactionsBottomSheetFragment extends BaseBottomSheetFragment<FragmentReactionsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    private String transactionId;
    private ReactorAdapter adapter;
    private ApiHelper apiHelper;

    public static ReactionsBottomSheetFragment newInstance(String transactionId) {
        ReactionsBottomSheetFragment fragment = new ReactionsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRANSACTION_ID, transactionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentReactionsBottomSheetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentReactionsBottomSheetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transactionId = getArguments().getString(ARG_TRANSACTION_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        setupRecyclerView();
        loadReactions();
        setupRealtime();
    }

    private void setupRealtime() {
        com.upreyvan.carti.data.repository.RealtimeRepository.getInstance(requireContext()).getLikeStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null && transactionId.equals(payload.get("transactionId"))) {
                loadReactions();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ReactorAdapter();
        getBinding().rvReactors.setAdapter(adapter);
    }

    private void loadReactions() {
        getBinding().progressBar.setVisibility(View.VISIBLE);
        apiHelper.getLikes(transactionId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().progressBar.setVisibility(View.GONE);
                    List<Reactor> reactors = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        String username = String.valueOf(data.get("username"));
                        String emoji = String.valueOf(data.get("emojiType"));
                        String userId = String.valueOf(data.get("userId"));
                        String avatarUrl = "https://cloud.appwrite.io/v1/avatars/initials?name=" + username + "&project=carti";
                        
                        reactors.add(new Reactor(username, emoji, 0, avatarUrl, userId));
                    }
                    adapter.submitList(reactors);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    getBinding().progressBar.setVisibility(View.GONE);
                    // Handle error
                });
            }
        });
    }
}
