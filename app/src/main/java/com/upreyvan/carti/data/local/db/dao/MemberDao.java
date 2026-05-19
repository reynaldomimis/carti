package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upreyvan.carti.model.Member;

import java.util.List;

@Dao
public interface MemberDao {
    @Query("SELECT * FROM members WHERE familyId = :familyId")
    LiveData<List<Member>> getAllMembers(String familyId);

    @Query("SELECT * FROM members WHERE familyId = :familyId")
    List<Member> getAllMembersList(String familyId);

    @Query("SELECT * FROM members WHERE id IN (:memberIds)")
    LiveData<List<Member>> getMembersByIds(List<String> memberIds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Member> members);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Member member);

    @Update
    void update(Member member);

    @Query("DELETE FROM members WHERE id = :memberId")
    void deleteById(String memberId);

    @Query("DELETE FROM members WHERE familyId = :familyId")
    void deleteAll(String familyId);
}