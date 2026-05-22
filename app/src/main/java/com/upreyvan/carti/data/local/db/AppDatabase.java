package com.upreyvan.carti.data.local.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.upreyvan.carti.data.local.db.dao.MemberDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.Transaction;

@Database(entities = {Member.class, Transaction.class}, version = 19, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MemberDao memberDao();
    public abstract TransactionDao transactionDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "carti_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}