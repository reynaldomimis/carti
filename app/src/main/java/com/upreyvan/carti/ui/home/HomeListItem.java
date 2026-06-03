package com.upreyvan.carti.ui.home;

import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

public interface HomeListItem {
    int TYPE_DASHBOARD = 1;
    int TYPE_BUDGET_PROMPT = 2;
    int TYPE_SECTION_HEADER = 3;
    int TYPE_BILL = 4;
    int TYPE_QUICK_ACTIONS = 5;
    int TYPE_QUICK_LOG = 6;
    int TYPE_TRANSACTION = 7;
    int TYPE_EMPTY_STATE = 8;
    int TYPE_BILL_CONTAINER = 9;
    int TYPE_AI_INSIGHT = 10;

    int getViewType();
    String getItemId();

    record DashboardItem(HomeViewModel.DashboardState state) implements HomeListItem {
        @Override public int getViewType() { return TYPE_DASHBOARD; }
        @Override public String getItemId() { return "DASHBOARD"; }
    }

    record AIInsightItem(String message) implements HomeListItem {
        @Override public int getViewType() { return TYPE_AI_INSIGHT; }
        @Override public String getItemId() { return "AI_INSIGHT"; }
    }

    record BudgetPromptItem() implements HomeListItem {
        @Override public int getViewType() { return TYPE_BUDGET_PROMPT; }
        @Override public String getItemId() { return "BUDGET_PROMPT"; }
    }

    record SectionHeaderItem(String title, String subtitle, boolean showAction, String actionText) implements HomeListItem {
        @Override public int getViewType() { return TYPE_SECTION_HEADER; }
        @Override public String getItemId() { return "HEADER_" + title; }
    }

    record BillItem(Bill bill) implements HomeListItem {
        @Override public int getViewType() { return TYPE_BILL; }
        @Override public String getItemId() { return "BILL_" + bill.getId(); }
    }

    record BillContainerItem(List<Bill> bills) implements HomeListItem {
        @Override public int getViewType() { return TYPE_BILL_CONTAINER; }
        @Override public String getItemId() { return "BILL_CONTAINER"; }
    }

    record QuickActionsItem(List<QuickLogItem> actions) implements HomeListItem {
        @Override public int getViewType() { return TYPE_QUICK_ACTIONS; }
        @Override public String getItemId() { return "QUICK_ACTIONS"; }
    }

    record QuickLogItemContainer(List<QuickLogItem> logs) implements HomeListItem {
        @Override public int getViewType() { return TYPE_QUICK_LOG; }
        @Override public String getItemId() { return "QUICK_LOG"; }
    }

    record TransactionItem(TransactionWithUser transaction) implements HomeListItem {
        @Override public int getViewType() { return TYPE_TRANSACTION; }
        @Override public String getItemId() { return "TRANS_" + transaction.getTransaction().getId(); }
    }

    record EmptyStateItem(String message) implements HomeListItem {
        @Override public int getViewType() { return TYPE_EMPTY_STATE; }
        @Override public String getItemId() { return "EMPTY_" + message; }
    }
}
