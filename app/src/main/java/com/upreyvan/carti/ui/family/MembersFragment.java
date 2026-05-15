package com.upreyvan.carti.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.family.InviteFamilyActivity;
import com.upreyvan.carti.ui.family.MemberAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentMembersBinding;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    private MemberAdapter adapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        fetchMembers();
    }

    private void fetchMembers() {
        new ApiHelper(requireContext()).getMembers(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;
                List<Document<Map<String, Object>>> documents = result.getDocuments();
                if (documents != null) {
                    List<Member> members = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : documents) {
                        Map<String, Object> data = doc.getData();
                        String name = String.valueOf(data.get("username"));
                        String role = String.valueOf(data.get("role"));
                        members.add(new Member(name, role, R.drawable.ic_person));
                    }
                    requireActivity().runOnUiThread(() -> adapter.submitList(members));
                }
            }

            @Override
            public void onError(Throwable error) {
                // Keep mocks or show error
            }
        });
    }

    private void setupToolbar() {
        getBinding().toolbar.tvToolbarTitle.setText(R.string.family_members_title);
        getBinding().toolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
        getBinding().toolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().toolbar.btnAction.setText(R.string.btn_invite);
        getBinding().toolbar.btnAction.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), InviteFamilyActivity.class));
        });
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter();
        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMembers.setAdapter(adapter);

        // Initial mock data until cloud loads
        List<Member> members = new ArrayList<>();
        members.add(new Member(getString(R.string.mock_name_juan), getString(R.string.role_admin), R.drawable.ic_person));
        adapter.submitList(members);
    }
}
