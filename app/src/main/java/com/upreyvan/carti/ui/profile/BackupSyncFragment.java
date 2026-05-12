package com.upreyvan.carti.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentBackupSyncBinding;

public class BackupSyncFragment extends BaseFragment<FragmentBackupSyncBinding> {

    @Override
    protected FragmentBackupSyncBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBackupSyncBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupContent();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.offline_mode_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupContent() {
        getBinding().actionAdd.ivActionIcon.setImageResource(android.R.drawable.ic_input_add);
        getBinding().actionAdd.tvActionName.setText(R.string.action_add_expenses);

        getBinding().actionView.ivActionIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        getBinding().actionView.tvActionName.setText(R.string.action_view_transactions);

        getBinding().actionSummary.ivActionIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        getBinding().actionSummary.tvActionName.setText(R.string.action_view_summary);

        getBinding().btnSync.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Checking for internet connection...", Toast.LENGTH_SHORT).show();
        });
    }
}