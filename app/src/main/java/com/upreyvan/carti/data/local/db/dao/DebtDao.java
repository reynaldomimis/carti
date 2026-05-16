package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upreyvan.carti.model.Debt;

import java.util.List;

@Dao
public interface DebtDao {
    @Query("SELECT * FROM debts WHERE familyId = :familyId")
    LiveData<List<Debt>> getAllDebts(String familyId);

    @Query("SELECT * FROM debts WHERE familyId = :familyId")
    List<Debt> getAllDebtsList(String familyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Debt> debts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Debt debt);

    @Update
    void update(Debt debt);

    @Query("DELETE FROM debts WHERE id = :debtId")
    void deleteById(String debtId);

    @Query("DELETE FROM debts WHERE familyId = :familyId")
    void deleteAll(String familyId);
}