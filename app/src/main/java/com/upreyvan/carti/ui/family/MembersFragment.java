package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.FragmentMembersBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Constants;

import java.util.List;
import java.util.Map;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    private MemberAdapter adapter;
    private Realtime realtime;
    private RealtimeSubscription subscription;
    private PreferenceManager pref;
    private MemberRepository repository;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pref = new PreferenceManager(requireContext());
        repository = new MemberRepository(requireContext());
        
        setupToolbar();
        setupRecyclerView();
        
        observeMembers();
        initRealtime();
        
        // Load from DB first, then sync if needed (fetch only if local is empty)
        repository.syncMembersIfNeeded();
    }

    private void observeMembers() {
        adapter.setLoading(true);
        repository.getMembers().observe(getViewLifecycleOwner(), new Observer<List<Member>>() {
            @Override
            public void onChanged(List<Member> members) {
                if (members != null) {
                    adapter.submitList(members);
                    if (!members.isEmpty()) {
                        adapter.setLoading(false);
                    }
                }
            }
        });
    }

    private void initRealtime() {
        realtime = new Realtime(AppwriteManager.getInstance(requireContext()).getClient());
        String familyId = pref.getFamilyId();
        
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + Constants.Appwrite.COL_USERS + ".documents";
        
        subscription = realtime.subscribe(new String[]{channel}, event -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String userFamilyId = String.valueOf(payload.get("familyId"));
            
            if (familyId != null && familyId.equals(userFamilyId)) {
                String eventType = "";
                if (!event.getEvents().isEmpty()) {
                    eventType = event.getEvents().iterator().next();
                }
                String id = String.valueOf(payload.get("$id"));
                
                if (eventType.contains(".delete")) {
                    repository.deleteMemberLocally(id);
                } else {
                    // Create or Update
                    String name = String.valueOf(payload.get("username"));
                    String role = String.valueOf(payload.get("role"));
                    String status = String.valueOf(payload.get("status"));
                    Member member = new Member(id, familyId, name, role, status, R.drawable.ic_person);
                    repository.saveMemberLocally(member);
                }
            }
            return null;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (subscription != null) {
            subscription.close();
        }
    }

    private void setupToolbar() {
        getBinding().toolbar.tvToolbarTitle.setText(R.string.family_members_title);
        getBinding().toolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter();
        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMembers.setAdapter(adapter);
    }
}
