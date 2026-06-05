package com.upreyvan.carti.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;

import com.upreyvan.carti.BuildConfig;
import com.upreyvan.carti.data.local.db.dao.LikeDao;
import com.upreyvan.carti.data.local.db.dao.MemberDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.Transaction;

@Database(entities = {Member.class, Transaction.class, Like.class}, version = 1, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MemberDao memberDao();
    public abstract TransactionDao transactionDao();
    public abstract LikeDao likeDao();

    private static final Migration[] ALL_MIGRATIONS = new Migration[]{
            // Linear migration path is enforced here.
            // Example: MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4
            // Room will automatically chain these for jumps (e.g. 1 -> 4).
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    RoomDatabase.Builder<AppDatabase> builder = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class, "carti_database")
                            .addMigrations(ALL_MIGRATIONS);

                    if (BuildConfig.DEBUG) {
                        builder.fallbackToDestructiveMigration();
                    }

                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }
}
