package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.MemberDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

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

    public void syncMembersIfNeeded() {
        executor.execute(() -> {
            List<Member> localMembers = memberDao.getAllMembersList(pref.getFamilyId());
            if (localMembers.isEmpty()) {
                // Fetch only if local is empty to save bandwidth
                refreshMembers();
            }
        });
    }

    public void refreshMembers() {
        String familyId = pref.getFamilyId();
        apiHelper.getMembers(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<Member> members = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        members.add(new Member(
                            doc.getId(),
                            familyId,
                            String.valueOf(data.get("username")),
                            String.valueOf(data.get("role")),
                            String.valueOf(data.get("status")),
                            R.drawable.ic_person
                        ));
                    }
                    memberDao.deleteAll(familyId);
                    memberDao.insertAll(members);
                });
            }

            @Override
            public void onError(Throwable error) {
                // Silently fail, UI will show whatever is in Room
            }
        });
    }

    public void saveMemberLocally(Member member) {
        member.setFamilyId(pref.getFamilyId());
        executor.execute(() -> memberDao.insert(member));
    }

    public void deleteMemberLocally(String memberId) {
        executor.execute(() -> memberDao.deleteById(memberId));
    }
}