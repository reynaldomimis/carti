package com.upreyvan.carti.util;

public class Constants {

    public static String[] sourcesFund = {"Cash", "GCash", "Maya", "Bank Transfer", "Credit Card"};
    public static final class Roles {
        public static final String FATHER = "Father";
        public static final String MOTHER = "Mother";
        public static final String BROTHER = "Brother";
        public static final String SISTER = "Sister";
        public static final String CHILD = "Child";

        public static final String[] PARENTS = {FATHER, MOTHER};
        public static final String[] CHILDREN = {BROTHER, SISTER, CHILD};
        public static final String[] ALL = {FATHER, MOTHER, BROTHER, SISTER, CHILD};

        public static final String AI_ID = "carti_ai";
        public static final String AI_NAME = "Carti AI";
    }

    public static final class Appwrite {
        public static final String DATABASE_ID = com.upreyvan.carti.BuildConfig.APPWRITE_DATABASE_ID;
        public static final String COL_USERS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_USERS;
        public static final String COL_TRANSACTIONS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_TRANSACTIONS;
        public static final String COL_GOALS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_GOALS;
        public static final String COL_DEBTS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_DEBTS;
        public static final String COL_FAMILIES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_FAMILIES;
        public static final String COL_MESSAGES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_MESSAGES;
        public static final String COL_INCOMES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_INCOMES;
        public static final String COL_NOTIFICATIONS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_NOTIFICATIONS;
        public static final String COL_LIKES = com.upreyvan.carti.BuildConfig.APPWRITE_COL_LIKES;
        public static final String COL_COMMENTS = com.upreyvan.carti.BuildConfig.APPWRITE_COL_COMMENTS;
    }

    public static final class Actions {
        public static final String REGISTER = "register";
        public static final String GET_USER = "get_user";
        public static final String CREATE_FAMILY = "create_family";
        public static final String JOIN_FAMILY = "request_join_family";
        public static final String APPROVE_JOIN = "approve_member";
        public static final String LEAVE_FAMILY = "leave_family";
        public static final String DELETE_ACCOUNT = "delete_account";
        public static final String ADD_TRANSACTION = "create_transaction";
        public static final String UPDATE_TRANSACTION = "update_transaction";
        public static final String ADD_INCOME = "create_transaction";
        public static final String UPDATE_INCOME = "update_transaction";
        public static final String DELETE_INCOME = "delete_transaction";
        public static final String ADD_GOAL = "create_transaction";
        public static final String ADD_DEBT = "create_transaction";
        public static final String UPDATE_GOAL = "update_transaction";
        public static final String UPDATE_DEBT = "update_transaction";
        public static final String MARK_DEBT_PAID = "update_transaction";
        public static final String DELETE_GOAL = "delete_transaction";
        public static final String DELETE_DEBT = "delete_transaction";
        public static final String UPDATE_FAMILY_TOTALS = "update_family_totals";
        public static final String GET_MEMBERS = "get_members";
        public static final String SEND_ANNOUNCEMENT = "send_announcement";
        public static final String ADD_LIKE = "add_like";
        public static final String REMOVE_LIKE = "remove_like";
        public static final String ADD_COMMENT = "add_comment";
        public static final String UPDATE_COMMENT = "update_comment";
        public static final String DELETE_COMMENT = "delete_comment";
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
        public static final String KEY_USER_NAME = "username";
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
        public static final String KEY_BUDGET_PLAN_DISMISSED_MONTH = "budget_plan_dismissed_month";
        public static final String KEY_HAS_NOTIFICATIONS = "has_notifications";
        public static final String KEY_LAST_NOTIF_CHECK = "last_notif_check";

        // Manager Keys
        public static final String KEY_TRANSACTIONS = "transactions_list";
        public static final String KEY_GOALS = "goals_list";
        public static final String KEY_DEBTS = "debts_list";
        public static final String KEY_CATEGORIES = "categories_list";
        public static final String KEY_IS_MONTHLY = "is_monthly";
        public static final String KEY_FIRST_PAYDAY = "first_payday";
        public static final String KEY_SECOND_PAYDAY = "second_payday";

        // Intent/Bundle Keys
        public static final String KEY_GOAL_ID = "goal_id";
        public static final String KEY_DEBT_ID = "debt_id";
        public static final String KEY_TRANSACTION_ID = "transaction_id";
        public static final String KEY_INCOME_ID = "income_id";
        public static final String KEY_LIKE_ID = "like_id";
        public static final String KEY_COMMENT_ID = "comment_id";
        public static final String KEY_PARENT_ID = "parentId";
        public static final String KEY_EMOJI_TYPE = "emojiType";
        public static final String KEY_COMMENT_TEXT = "text";
        public static final String KEY_LIKES_COUNT = "likesCount";
        public static final String KEY_COMMENTS_COUNT = "commentCount";
    }

    public static final class Navigation {
        public static final int HOME = 1;
        public static final int TRACK = 2;
        public static final int PLAN = 3;
        public static final int PROFILE = 4;
        public static final int CHAT = 5;
        public static final int GOALS = 6;
        public static final int ADD = 7;
        public static final int INCOME = 8;
    }

    public static final class ErrorCodes {
        public static final String UNAUTHORIZED = "Unauthorized access. Please login again.";
        public static final String NOT_FOUND = "The requested resource was not found.";
        public static final String ALREADY_IN_FAMILY = "You are already a member of a family group.";
        public static final String INVALID_INPUT = "Please check your input and try again.";
        public static final String RATE_LIMIT = "Too many requests. Please wait a moment.";
        public static final String SERVER_ERROR = "Server is currently unavailable. Please try later.";
        public static final String NO_FAMILY = "You are not part of any family group yet.";
        public static final String PARSE_ERROR = "Data synchronization failed.";
        public static final String NETWORK_ERROR = "Please check your internet connection.";
        public static final String GENERIC_ERROR = "An unexpected error occurred. Please try again.";
        public static final String SESSION_EXPIRED = "Your session has expired. Please log in again.";
        public static final String PROHIBITED_ACTION = "You do not have permission to perform this action.";
        public static final String PASSWORD_RECENTLY_USED = "This password was recently used. Please choose a different one for your security.";
    }
}
