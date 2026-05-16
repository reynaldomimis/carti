package com.upreyvan.carti.util;

public class Constants {
    public static final class Roles {
        public static final String FATHER = "Father";
        public static final String MOTHER = "Mother";
        public static final String BROTHER = "Brother";
        public static final String SISTER = "Sister";
        public static final String CHILD = "Child";

        public static final String[] PARENTS = {FATHER, MOTHER};
        public static final String[] CHILDREN = {BROTHER, SISTER, CHILD};
        public static final String[] ALL = {FATHER, MOTHER, BROTHER, SISTER, CHILD};
    }

    public static final class Appwrite {
        public static final String DATABASE_ID = com.upreyvan.carti.BuildConfig.APPWRITE_DATABASE_ID;
        public static final String COL_USERS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_USERS;
        public static final String COL_TRANSACTIONS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_TRANSACTIONS;
        public static final String COL_GOALS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_GOALS;
        public static final String COL_DEBTS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_DEBTS;
        public static final String COL_FAMILIES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_FAMILIES;
        public static final String COL_MESSAGES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_MESSAGES;
    }

    public static final class Actions {
        public static final String GET_MEMBERS = "get_members";
        public static final String REGISTER = "register";
        public static final String GET_USER = "get_user";
        public static final String CREATE_FAMILY = "create_family";
        public static final String JOIN_FAMILY = "request_join_family";
        public static final String APPROVE_JOIN = "approve_join_request";
        public static final String ADD_TRANSACTION = "add_transaction";
        public static final String ADD_GOAL = "add_goal";
        public static final String ADD_DEBT = "add_debt";
        public static final String UPDATE_GOAL = "update_goal_amount";
        public static final String UPDATE_DEBT = "update_debt_amount";
        public static final String MARK_DEBT_PAID = "mark_debt_paid";
        public static final String UPDATE_FAMILY_TOTALS = "update_family_totals";
    }

    public static final class Keys {
        public static final String ACTION = "action";
        public static final String USER_ID = "userId";
        public static final String STATUS = "s";
        public static final String DATA = "data";
        public static final String MESSAGE = "m";
        
        // Preference Names
        public static final String PREF_NAME = "carti_prefs";
        public static final String PREF_EXPENSE = "expense_prefs";
        public static final String PREF_GOAL = "goal_prefs";
        public static final String PREF_SALARY = "salary_prefs";
        public static final String PREF_CATEGORY = "carti_categories";
        public static final String PREF_DEBT = "debt_prefs";

        // Main Preference Keys
        public static final String KEY_ONBOARDING_FINISHED = "onboarding_finished";
        public static final String KEY_USER_NAME = "user_name";
        public static final String KEY_USER_ID_PREF = "user_id";
        public static final String KEY_USER_EMAIL = "user_email";
        public static final String KEY_USER_ROLE = "user_role";
        public static final String KEY_FAMILY_ID = "family_id";
        public static final String KEY_INVITE_CODE = "invite_code";
        public static final String KEY_IS_EMPLOYED = "is_employed";
        public static final String KEY_BALANCE = "balance";
        public static final String KEY_TOTAL_INCOME = "total_income";
        public static final String KEY_TOTAL_EXPENSE = "total_expense";
        public static final String KEY_CHAT_AUTO_DELETE_DAYS = "chat_auto_delete_days";
        public static final String KEY_LAST_SYNC_TIME = "last_sync_time";
        public static final String KEY_LAST_GOAL_SYNC_TIME = "last_goal_sync_time";
        public static final String KEY_LAST_DEBT_SYNC_TIME = "last_debt_sync_time";

        // Manager Keys
        public static final String KEY_TRANSACTIONS = "transactions_list";
        public static final String KEY_GOALS = "goals_list";
        public static final String KEY_DEBTS = "debts_list";
        public static final String KEY_CATEGORIES = "categories_list";
        public static final String KEY_SALARY_AMOUNT = "salary_amount";
        public static final String KEY_FIRST_PAYDAY = "first_payday";
        public static final String KEY_SECOND_PAYDAY = "second_payday";
        public static final String KEY_IS_MONTHLY = "is_monthly";

        // Intent/Bundle Keys
        public static final String KEY_GOAL_ID = "goal_id";
        public static final String KEY_DEBT_ID = "debt_id";
        public static final String KEY_TRANSACTION_ID = "transaction_id";
    }

    public static final class ErrorCodes {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String ALREADY_IN_FAMILY = "ALREADY_IN_FAMILY";
        public static final String INVALID_INPUT = "INVALID_INPUT";
        public static final String RATE_LIMIT = "TOO_MANY_REQUESTS";
        public static final String SERVER_ERROR = "SERVER_ERROR";
        public static final String NO_FAMILY = "NO_FAMILY";
        public static final String PARSE_ERROR = "PARSE_ERROR";
        public static final String NETWORK_ERROR = "NETWORK_ERROR";
        public static final String GENERIC_ERROR = "An unexpected error occurred. Please try again.";
    }
}
