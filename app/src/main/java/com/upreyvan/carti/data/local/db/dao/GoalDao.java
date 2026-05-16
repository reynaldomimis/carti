package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upreyvan.carti.model.Goal;

import java.util.List;

@Dao
public interface GoalDao {
    @Query("SELECT * FROM goals WHERE familyId = :familyId")
    LiveData<List<Goal>> getAllGoals(String familyId);

    @Query("SELECT * FROM goals WHERE familyId = :familyId")
    List<Goal> getAllGoalsList(String familyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Goal> goals);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Goal goal);

    @Update
    void update(Goal goal);

    @Query("DELETE FROM goals WHERE id = :goalId")
    void deleteById(String goalId);

    @Query("DELETE FROM goals WHERE familyId = :familyId")
    void deleteAll(String familyId);
}