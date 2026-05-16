package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.upreyvan.carti.model.Income;

import java.util.List;

@Dao
public interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE familyId = :familyId ORDER BY createdAt DESC")
    LiveData<List<Income>> getAllIncomes(String familyId);

    @Query("SELECT * FROM incomes WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Income>> getIncomesByUserId(String userId);

    @Query("SELECT SUM(amount) FROM incomes WHERE familyId = :familyId")
    LiveData<Double> getTotalIncomeByFamilyId(String familyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Income> incomes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Income income);

    @Query("DELETE FROM incomes WHERE id = :incomeId")
    void deleteById(String incomeId);

    @Query("DELETE FROM incomes WHERE familyId = :familyId")
    void deleteAll(String familyId);
}