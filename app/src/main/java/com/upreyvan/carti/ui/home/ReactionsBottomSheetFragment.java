package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentReactionsBottomSheetBinding;

public class ReactionsBottomSheetFragment extends BaseBottomSheetFragment<FragmentReactionsBottomSheetBinding> {

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    private String transactionId;
    private ReactorAdapter adapter;
    private ReactionsViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(ReactionsViewModel.class);
        setupRecyclerView();
        observeViewModel();
        setupRealtime();
        
        viewModel.loadReactions(transactionId);
    }

    private void observeViewModel() {
        viewModel.getReactors().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            getBinding().progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    private void setupRealtime() {
        RealtimeRepository.getInstance(requireContext()).getLikeStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null && transactionId.equals(payload.get("transactionId"))) {
                viewModel.loadReactions(transactionId);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ReactorAdapter();
        getBinding().rvReactors.setAdapter(adapter);
    }
}
