package com.upreyvan.carti.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.upreyvan.carti.data.local.db.dao.BillDao;
import com.upreyvan.carti.data.local.db.dao.DebtDao;
import com.upreyvan.carti.data.local.db.dao.GoalDao;
import com.upreyvan.carti.data.local.db.dao.IncomeDao;
import com.upreyvan.carti.data.local.db.dao.MemberDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.model.Income;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.Transaction;

@Database(entities = {Member.class, Transaction.class, Goal.class, Debt.class, Income.class, Bill.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MemberDao memberDao();
    public abstract TransactionDao transactionDao();
    public abstract GoalDao goalDao();
    public abstract DebtDao debtDao();
    public abstract IncomeDao incomeDao();
    public abstract BillDao billDao();

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