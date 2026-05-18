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

import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import io.appwrite.models.RealtimeSubscription;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class MemberRepository {
    private final MemberDao memberDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final RealtimeHelper realtimeHelper;
    private RealtimeSubscription realtimeSubscription;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MemberRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        memberDao = db.memberDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
        realtimeHelper = new RealtimeHelper(context);
        initRealtime();
    }

    private void initRealtime() {
        if (realtimeSubscription != null) return;
        realtimeSubscription = realtimeHelper.subscribeToCollection(
                Constants.Appwrite.COL_USERS,
                event -> refreshMembers()
        );
    }

    public void onDestroy() {
        if (realtimeSubscription != null) {
            realtimeSubscription.close();
            realtimeSubscription = null;
        }
    }

    public LiveData<List<Member>> getMembers() {
        return memberDao.getAllMembers(pref.getFamilyId());
    }



    public void syncMembersIfNeeded() {
        executor.execute(() -> {
            List<Member> localMembers = memberDao.getAllMembersList(pref.getFamilyId());
            if (localMembers.isEmpty()) {
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
                        double salary = 0;
                        if (data.get("salary") != null) {
                            try {
                                salary = Double.parseDouble(String.valueOf(data.get("salary")));
                            } catch (Exception e) {
                                salary = 0;
                            }
                        }

                        Object isEmployedObj = data.get("isEmployed");
                        boolean isEmployed = false;
                        if (isEmployedObj instanceof Boolean) {
                            isEmployed = (Boolean) isEmployedObj;
                        } else if (isEmployedObj instanceof String) {
                            isEmployed = Boolean.parseBoolean((String) isEmployedObj);
                        }
                        String status = isEmployed ? "Employed" : "Unemployed";

                        members.add(new Member(
                            doc.getId(),
                            familyId,
                            String.valueOf(data.get("username")),
                            String.valueOf(data.get("role")),
                            status,
                            R.drawable.ai_holder,
                            salary
                        ));
                    }
                    memberDao.insertAll(members);
                });
            }

            @Override
            public void onError(Throwable error) {
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