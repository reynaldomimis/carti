package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentJoinFamilyBinding;
import com.upreyvan.carti.util.Utils;

public class JoinFamilyFragment extends BaseFragment<FragmentJoinFamilyBinding> {

    @Override
    protected FragmentJoinFamilyBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentJoinFamilyBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupListeners();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().joinFamilyHeader,
                getBinding().joinFamilyRoot,
                0f,
                0
        );
    }

    private void setupListeners() {
        getBinding().btnJoin.setOnClickListener(v -> {
            String familyId = getBinding().etFamilyId.getText().toString().trim();
            if (familyId.isEmpty()) {
                getBinding().tilFamilyId.setError("Please enter a valid Family ID");
                return;
            }

            Toast.makeText(requireContext(), "Request Sent to Admin!", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }
}