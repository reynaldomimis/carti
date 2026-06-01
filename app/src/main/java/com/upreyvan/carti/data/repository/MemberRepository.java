package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.MemberDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.model.Member;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemberRepository {
    private final MemberDao memberDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MemberRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        memberDao = db.memberDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
    }

    public LiveData<List<Member>> getMembers() {
        return memberDao.getAllMembers(pref.getFamilyId());
    }

    public LiveData<List<Member>> getMembersByIds(List<String> ids) {
        return memberDao.getMembersByIds(ids);
    }

    public void syncMembersIfNeeded() {
        executor.execute(() -> {
            List<Member> localMembers = memberDao.getAllMembersList(pref.getFamilyId());
            if (localMembers.isEmpty()) refreshMembers();
        });
    }

    public void refreshMembers() {
        String familyId = pref.getFamilyId();
        apiHelper.getMembers(new AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
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
                    memberDao.insertAll(members);
                });
            }
            @Override public void onError(Throwable error) {}
        });
    }
}
