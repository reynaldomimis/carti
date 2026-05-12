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
import com.upreyvan.carti.model.Member;

import java.util.ArrayList;
import java.util.List;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
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
        MemberAdapter adapter = new MemberAdapter();
        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMembers.setAdapter(adapter);

        List<Member> members = new ArrayList<>();
        members.add(new Member(getString(R.string.mock_name_juan), getString(R.string.role_admin), R.drawable.ic_person));
        members.add(new Member(getString(R.string.mock_name_maria), getString(R.string.role_member), R.drawable.ic_person));
        members.add(new Member(getString(R.string.mock_name_miguel), getString(R.string.role_member), R.drawable.ic_person));
        members.add(new Member(getString(R.string.mock_name_ana), getString(R.string.role_member), R.drawable.ic_person));
        adapter.submitList(members);
    }
}