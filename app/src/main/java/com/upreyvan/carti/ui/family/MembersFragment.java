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

import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.data.local.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pref = new PreferenceManager(requireContext());
        setupToolbar();
        setupRecyclerView();
        adapter.setLoading(true);
        fetchMembers();
        initRealtime();
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
                String eventType = event.getEvents().get(0);
                
                String id = String.valueOf(payload.get("$id"));
                String name = String.valueOf(payload.get("username"));
                String role = String.valueOf(payload.get("role"));
                String status = String.valueOf(payload.get("status"));
                Member member = new Member(id, name, role, status, R.drawable.ic_person);

                requireActivity().runOnUiThread(() -> {
                    if (eventType.contains(".create")) {
                        adapter.addMember(member);
                    } else if (eventType.contains(".update")) {
                        adapter.updateMember(member);
                    } else if (eventType.contains(".delete")) {
                        adapter.removeMember(id);
                    }
                });
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
                        String id = doc.getId();
                        String name = String.valueOf(data.get("username"));
                        String role = String.valueOf(data.get("role"));
                        String status = String.valueOf(data.get("status"));
                        members.add(new Member(id, name, role, status, R.drawable.ic_person));
                    }
                    requireActivity().runOnUiThread(() -> adapter.submitList(members));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.setLoading(false);
                        showError(error);
                    });
                }
            }
        });
    }

    private void setupToolbar() {
        getBinding().toolbar.tvToolbarTitle.setText(R.string.family_members_title);
        getBinding().toolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
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
    }
}
