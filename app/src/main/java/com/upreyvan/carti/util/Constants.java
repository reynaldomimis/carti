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
    }
}
