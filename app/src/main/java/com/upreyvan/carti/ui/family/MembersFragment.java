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
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemMemberBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    private GenericAdapter<Member, ItemMemberBinding> adapter;
    private boolean isLoading = true;
    private Realtime realtime;
    private RealtimeSubscription subscription;
    private PreferenceManager pref;
    private MemberRepository repository;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pref = new PreferenceManager(requireContext());
        repository = new MemberRepository(requireContext());
        
        setupToolbar(getBinding().toolbar, R.string.family_members_title);
        setupRecyclerView();
        
        observeMembers();
        initRealtime();
        
        repository.syncMembersIfNeeded();
    }

    private void observeMembers() {
        repository.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                isLoading = members.isEmpty();
                if (isLoading) {
                    List<Member> placeholders = new ArrayList<>();
                    for (int i = 0; i < 3; i++) placeholders.add(new Member("", "", "", "", "", 0, 0));
                    adapter.submitList(placeholders);
                } else {
                    adapter.submitList(members);
                }
            }
        });
    }

    private void initRealtime() {
        // ... (existing realtime code)
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
                    
                    // Map isEmployed to status since 'status' doesn't exist in users table
                    Object isEmployedObj = payload.get("isEmployed");
                    boolean isEmployed = false;
                    if (isEmployedObj instanceof Boolean) {
                        isEmployed = (Boolean) isEmployedObj;
                    } else if (isEmployedObj instanceof String) {
                        isEmployed = Boolean.parseBoolean((String) isEmployedObj);
                    }
                    String status = isEmployed ? "Employed" : "Unemployed";
                    
                    double salary = 0.0;
                    Object salaryObj = payload.get("salary");
                    if (salaryObj instanceof Number) {
                        salary = ((Number) salaryObj).doubleValue();
                    } else if (salaryObj instanceof String) {
                        try {
                            salary = Double.parseDouble((String) salaryObj);
                        } catch (NumberFormatException e) {
                            salary = 0.0;
                        }
                    }
                    
                    Member member = new Member(id, familyId, name, role, status, R.drawable.ic_person, salary);
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

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberBinding.inflate(inflater, parent, false),
                (binding, member) -> {
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        binding.tvTitle.setText(member.getTitle());
                        binding.chipRole.setText(member.getDescription());
                        binding.ivAvatar.setImageResource(member.getAvatarRes());
                        String statusText = member.getStatus() + " • " + Utils.formatCurrency(member.getAmount());
                        binding.tvDescription.setText(statusText);
                    }
                }
        );
        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMembers.setAdapter(adapter);
    }
}
