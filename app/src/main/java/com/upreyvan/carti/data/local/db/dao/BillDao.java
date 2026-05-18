package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.upreyvan.carti.model.Bill;

import java.util.List;

@Dao
public interface BillDao {
    @Query("SELECT * FROM bills WHERE familyId = :familyId ORDER BY date DESC")
    LiveData<List<Bill>> getAllBills(String familyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Bill bill);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Bill> bills);

    @Query("DELETE FROM bills WHERE id = :id")
    void deleteById(String id);
}
