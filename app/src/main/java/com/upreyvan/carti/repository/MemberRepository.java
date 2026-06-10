package com.upreyvan.carti.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.upreyvan.carti.R;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.models.Member;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MemberRepository {
    private static MemberRepository instance;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final MutableLiveData<List<Member>> membersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    private MemberRepository(Context context) {
        apiHelper = new ApiHelper(context);
        pref = PreferenceManager.getInstance(context);
        refreshMembers();
    }

    public static synchronized MemberRepository getInstance(Context context) {
        if (instance == null) instance = new MemberRepository(context.getApplicationContext());
        return instance;
    }

    public LiveData<List<Member>> getMembers() {
        return membersLiveData;
    }

    public LiveData<List<Member>> getMembersByIds(List<String> ids) {
        return Transformations.map(membersLiveData, members -> 
            members.stream()
                .filter(m -> ids.contains(m.getId()))
                .collect(Collectors.toList())
        );
    }

    public void refreshMembers() {
        if (isRefreshing.getAndSet(true)) return;

        String familyId = pref.getFamilyId();
        if (familyId == null || familyId.isEmpty()) {
            isRefreshing.set(false);
            return;
        }

        apiHelper.getMembers(new AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                List<Member> members = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    double salary = 0;
                    Object salaryObj = data.get("salary");
                    if (salaryObj != null) {
                        try { salary = Double.parseDouble(salaryObj.toString()); } catch (Exception ignored) {}
                    }
                    
                    Object isEmployedObj = data.get("isEmployed");
                    boolean isEmployed = false;
                    if (isEmployedObj instanceof Boolean b) isEmployed = b;
                    else if (isEmployedObj instanceof String s) isEmployed = Boolean.parseBoolean(s);
                    
                    String status = isEmployed ? "Employed" : "Unemployed";
                    Member member = new Member(doc.getId(), familyId, String.valueOf(data.get("username")), String.valueOf(data.get("role")), status, R.drawable.ai_holder, salary);
                    if (data.containsKey("avatarUrl")) member.setAvatarUrl(String.valueOf(data.get("avatarUrl")));
                    members.add(member);
                }
                membersLiveData.postValue(members);
                isRefreshing.set(false);
            }
            @Override public void onError(Throwable error) {
                isRefreshing.set(false);
            }
        });
    }
}
