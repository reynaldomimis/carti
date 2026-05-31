package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.ai.AiActionHandler;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.data.ai.LocalIntentParser;
import com.upreyvan.carti.model.ChatMessage;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiRepository {
    private final AiManager aiManager;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final BudgetManager budgetManager;
    private final AiActionHandler actionHandler;
    private final com.upreyvan.carti.data.local.db.dao.TransactionDao transactionDao;
    private long lastCallTime = 0;
    private static final long RATE_LIMIT_MS = 3000;
    private static final long SESSION_TIMEOUT_MS = 60000;
    private static AiRepository instance;

    public static AiRepository getInstance(Context context) {
        if (instance == null) instance = new AiRepository(context);
        return instance;
    }

    public AiRepository(Context context) {
        this.aiManager = AiManager.getInstance(context);
        this.apiHelper = new ApiHelper(context);
        this.pref = new PreferenceManager(context);
        this.budgetManager = BudgetManager.getInstance(context);
        this.actionHandler = new AiActionHandler(context);
        this.transactionDao = com.upreyvan.carti.data.local.db.AppDatabase.getInstance(context).transactionDao();
    }

    public void processChat(String msg, String sender, List<ChatMessage> history, boolean force, AiManager.AiCallback cb) {
        long now = System.currentTimeMillis();
        boolean sessionExpired = (now - lastCallTime > SESSION_TIMEOUT_MS);
        
        String lowerMsg = msg.toLowerCase().trim();
        boolean isCommand = lowerMsg.contains("@carti");

        String normalizedMsg = lowerMsg
                .replaceAll("(?i)@carti[:\\s]?", " ")
                .replaceAll("(?i)\\bcarti\\b", " ")
                .replaceAll("[^a-z0-9\\s₱pP.]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (handleHistoryFollowUp(normalizedMsg, history, cb)) {
            lastCallTime = now;
            return;
        }

        JSONObject localAction = LocalIntentParser.parse(normalizedMsg);
        
        if (localAction != null) {
            String intent = localAction.optString("intent");
            if (intent.equals("ADD_EXPENSE") || intent.equals("ADD_INCOME") || intent.equals("ADD_DEBT") || intent.equals("ADD_GOAL") || intent.equals("ALLOCATE")) {
                lastCallTime = now;
                cb.onActionDetected(localAction);
                
                JSONObject data = localAction.optJSONObject("data");
                if (data != null) {
                    double amount = data.optDouble("amount", 0.0);
                    String item = data.optString("item", "transaction");
                    String category = data.optString("category", "Others");
                    cb.onSuccess(String.format(Locale.US, "Success! Recorded ₱%,.2f for %s under %s. ✅", amount, item, category));
                } else {
                    cb.onSuccess("Success! Recorded successfully. ✅");
                }
                return;
            }
            if (intent.equals("LOCAL_GREETING")) {
                lastCallTime = now;
                cb.onSuccess(pref.isAiIntroDone() ? "Yes? How can I help you today?" : "I'm Carti, your family's finance assistant. How can I help you today? 🐧");
                pref.setAiIntroDone(true);
                return;
            }
            if (intent.equals("LOCAL_QUESTION")) {
                if (!force && now - lastCallTime < RATE_LIMIT_MS) {
                    cb.onError(new Exception("Rate limit reached. Please wait."));
                    return;
                }

                lastCallTime = now;
                cb.onSuccess(localAction.optString("message"));
                return;
            }
            if (intent.equals("LOCAL_COMPUTE_DYNAMIC")) {
                lastCallTime = now;
                handleLocalComputeDynamic(localAction.optJSONObject("data"), cb);
                return;
            }
        }

        if (!force && !isCommand && (now - lastCallTime < RATE_LIMIT_MS || msg.length() < 2 || lowerMsg.matches("^(ok|okay|salamat|thanks|bye|ty|k|tnx|haha|hehe)$"))) {
            if (msg.length() >= 2 && !lowerMsg.matches("^(ok|okay|salamat|thanks|bye|ty|k|tnx|haha|hehe)$")) {
                cb.onError(new Exception("Rate limit reached."));
            } else {
                cb.onSuccess("");
            }
            return;
        }
        
        lastCallTime = now;
        List<ChatMessage> effectiveHistory = sessionExpired ? null : history;

        new Thread(() -> {
            String familyId = pref.getFamilyId();
            try {
                List<com.upreyvan.carti.model.Transaction> allTxs = transactionDao.getAllTransactionsList(familyId);
                double monthIncome = calculateSum(allTxs, "TOTAL_INCOME", "THIS_MONTH");
                double monthExpense = calculateSum(allTxs, "TOTAL_EXPENSE", "THIS_MONTH");
                double todayIncome = calculateSum(allTxs, "TOTAL_INCOME", "TODAY");
                double todayExpense = calculateSum(allTxs, "TOTAL_EXPENSE", "TODAY");
                double balance = pref.getBalance();

                String baseCtx = String.format(Locale.US, 
                    "FAMILY_ID: %s | USER_ID: %s\nBAL: ₱%.2f | TODAY: I:₱%.2f E:₱%.2f | MONTH: I:₱%.2f E:₱%.2f",
                    familyId, pref.getUserId(), balance, todayIncome, todayExpense, monthIncome, monthExpense);

                mainHandler.post(() -> {
                    if (isCommand && !msg.matches(".*\\d+.*")) {
                        callAi(msg, baseCtx + buildHistoryContext(effectiveHistory), force, cb);
                        return;
                    }

                    apiHelper.getTransactionsSince("2024-01-01T00:00:00.000Z", new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
                        @Override public void onSuccess(DocumentList<Map<String, Object>> r) { 
                            callAi(msg, buildContextWithTotals(r.getDocuments(), effectiveHistory, monthIncome, monthExpense, todayIncome, todayExpense), force, cb); 
                        }
                        @Override public void onError(Throwable e) {
                            callAi(msg, baseCtx + buildHistoryContext(effectiveHistory), force, cb); 
                        }
                    });
                });
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e));
            }
        }).start();
    }

    private String buildContextWithTotals(List<Document<Map<String, Object>>> docs, List<ChatMessage> history, double mInc, double mExp, double tInc, double tExp) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "B:₱%.0f | TODAY: I:₱%.0f E:₱%.0f | MONTH: I:₱%.0f E:₱%.0f\n", pref.getBalance(), tInc, tExp, mInc, mExp));
        if (docs != null && !docs.isEmpty()) { 
            sb.append("TX:"); int count = 0; 
            for (Document<Map<String, Object>> d : docs) { 
                if (count++ >= 5) break;
                Map<String, Object> m = d.getData(); 
                sb.append(m.get("amount")).append(",").append(m.get("category")).append(";"); 
            } 
        }
        sb.append(buildHistoryContext(history));
        return sb.toString();
    }

    private boolean handleHistoryFollowUp(String normalizedMsg, List<ChatMessage> history, AiManager.AiCallback cb) {
        if (history == null || history.isEmpty()) return false;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage mH = history.get(i);

            if (!mH.isMe() && !mH.isShimmer()) {
                if (LocalIntentParser.isContextReset(mH.getMessage())) break;
                
                Matcher mAmount = Pattern.compile("(?<![\\d,])(?:₱|p|php)?\\s*(\\d+(?:\\.\\d{1,2})?)(?![\\d,])").matcher(mH.getMessage().toLowerCase());
                if (mAmount.find() && !normalizedMsg.matches(".*\\d+.*")) {
                    double val = Double.parseDouble(mAmount.group(1));
                    if (val < 1000000) return logFromHistory(val, normalizedMsg, cb);
                }
                
                Matcher mItem = Pattern.compile("for '(.+?)'").matcher(mH.getMessage().toLowerCase());
                if (mItem.find() && normalizedMsg.matches("\\d+(?:\\.\\d+)?")) {
                    return logFromHistory(Double.parseDouble(normalizedMsg), mItem.group(1), cb);
                }
                
                break;
            }
        }
        return false;
    }

    private boolean logFromHistory(double amount, String item, AiManager.AiCallback cb) {
        JSONObject action = new JSONObject();
        try {
            action.put("intent", "ADD_EXPENSE");
            JSONObject data = new JSONObject();
            data.put("amount", amount);
            data.put("item", item);
            String category = com.upreyvan.carti.data.ai.CategoryMapper.getCategory(item, "Others");
            data.put("category", category);
            action.put("data", data);
            action.put("local", true);
            cb.onActionDetected(action);
            cb.onSuccess(String.format(Locale.US, "Got it! Recorded ₱%,.2f for %s under %s. ✅", amount, item, category));
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void handleLocalComputeDynamic(JSONObject data, AiManager.AiCallback cb) {
        String period = data.optString("period", "MONTH");
        String familyId = pref.getFamilyId();
        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> txs = transactionDao.getAllTransactionsList(familyId);
                long[] range = getTimeRange(period, data);
                String periodLabel = (period.equals("CUSTOM")) ? String.format("%s to %s", data.optString("startDate"), data.optString("endDate")) : getPeriodLabel(period);
                StringBuilder sb = new StringBuilder("Here is your financial status (").append(periodLabel).append("):\n");
                JSONObject summaryData = new JSONObject();
                summaryData.put("period", periodLabel);


                double totalInflow = calculateSumInRange(txs, "TOTAL_INCOME", null);
                double totalOutflow = calculateSumInRange(txs, "TOTAL_EXPENSE", null);
                double walletBalance = totalInflow - totalOutflow;

                if (data.optBoolean("want_balance")) { 
                    sb.append("🏦 Balance: ").append(Utils.formatCurrency(walletBalance)).append("\n"); 
                    summaryData.put("balance", walletBalance); 
                }
                if (data.optBoolean("want_income")) { double val = calculateSumInRange(txs, "TOTAL_INCOME", range); sb.append("💰 Income: ").append(Utils.formatCurrency(val)).append("\n"); summaryData.put("income", val); }
                if (data.optBoolean("want_expense")) { double val = calculateSumInRange(txs, "TOTAL_EXPENSE", range); sb.append("📉 Expenses: ").append(Utils.formatCurrency(val)).append("\n"); summaryData.put("expense", val); }
                if (data.optBoolean("want_debt")) { double val = calculateSumInRange(txs, "TOTAL_DEBT", range); sb.append("💳 Debt: ").append(Utils.formatCurrency(val)).append("\n"); summaryData.put("debt", val); }
                if (data.optBoolean("want_goal")) { double val = calculateSumInRange(txs, "TOTAL_GOAL", range); sb.append("🏆 Savings/Goals: ").append(Utils.formatCurrency(val)).append("\n"); summaryData.put("goal", val); }
                if (data.optBoolean("want_alloc")) {
                    String cat = data.optString("targetCategory", "ALL");
                    if (cat.equals("ALL")) {
                        List<com.upreyvan.carti.model.BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
                        double totalAlloc = 0; for (com.upreyvan.carti.model.BudgetCategoryItem item : plan) totalAlloc += item.getAmount();
                        sb.append("📊 Allocation: ").append(Utils.formatCurrency(totalAlloc)).append("\n"); summaryData.put("allocation", totalAlloc);
                    } else {
                        double catSum = calculateCategorySumInRange(txs, cat, range); sb.append("📊 ").append(cat).append(" Budget: ").append(Utils.formatCurrency(catSum)).append("\n");
                    }
                }
                String finalMsg = sb.toString().trim(); if (finalMsg.endsWith(":")) finalMsg = "I couldn't find specific data for that request. Try asking about your balance or expenses. 🐧";

                if (data.optBoolean("is_list")) {
                    StringBuilder table = new StringBuilder("\n\n📋 **Detailed List**\n");
                    table.append("Item | Amount | Category\n");
                    table.append("---|---|---\n");
                    int listCount = 0;
                    for (com.upreyvan.carti.model.Transaction t : txs) {
                        if (t.getTimestampMillis() >= range[0] && t.getTimestampMillis() <= range[1]) {
                            boolean match = false;
                            String typeStr = t.getType() != null ? t.getType().toUpperCase() : "";
                            if (data.optBoolean("want_income") && typeStr.equals("INCOME")) match = true;
                            if (data.optBoolean("want_expense") && typeStr.equals("EXPENSE")) match = true;
                            if (data.optBoolean("want_debt") && typeStr.equals("DEBT")) match = true;
                            if (data.optBoolean("want_goal") && typeStr.equals("GOAL")) match = true;
                            if (data.optBoolean("want_alloc") && typeStr.equals("ALLOCATION")) match = true;
                            if (!data.optBoolean("want_income") && !data.optBoolean("want_expense") && !data.optBoolean("want_debt") && !data.optBoolean("want_goal") && !data.optBoolean("want_alloc")) match = true;
                            if (match) {
                                String title = t.getTitle() != null ? t.getTitle() : "Misc";
                                table.append(String.format(Locale.US, "%s | ₱%.2f | %s\n", title, t.getAmount(), t.getCategory()));
                                listCount++; if (listCount >= 15) break;
                            }
                        }
                    }
                    if (listCount > 0) finalMsg += table.toString();
                    else finalMsg += "\n\n(No specific items found for this list.)";
                }

                if (data.optBoolean("want_income") && data.optBoolean("want_expense")) {
                    double periodInc = calculateSumInRange(txs, "TOTAL_INCOME", range);
                    double periodExp = calculateSumInRange(txs, "TOTAL_EXPENSE", range);
                    double netSavings = periodInc - periodExp;
                    finalMsg += String.format(Locale.US, "\n💰 Net Savings: %s", Utils.formatCurrency(netSavings));
                    summaryData.put("savings", netSavings);
                }

                JSONObject action = new JSONObject(); action.put("intent", "SHOW_SUMMARY_CARD"); action.put("data", summaryData);
                String result = finalMsg; mainHandler.post(() -> { cb.onActionDetected(action); cb.onSuccess(result); });
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e)); }
        }).start();
    }

    private void handleLocalCompute(JSONObject data, AiManager.AiCallback cb) {
        String type = data.optString("queryType"); String period = data.optString("period"); String familyId = pref.getFamilyId();
        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> txs = transactionDao.getAllTransactionsList(familyId);
                long[] range = getTimeRange(period, data); double sum = calculateSumInRange(txs, type, range);
                String message; String periodLabel = (period.equals("CUSTOM")) ? String.format("%s to %s", data.optString("startDate"), data.optString("endDate")) : getPeriodLabel(period);
                if (type.equals("BALANCE")) message = String.format(Locale.US, "Your current balance is ₱%,.2f. 🏦", pref.getBalance());
                else if (type.equals("TOTAL_EXPENSE")) message = String.format(Locale.US, "Your total expenses for %s is ₱%,.2f. 📉", periodLabel, sum);
                else if (type.equals("TOTAL_INCOME")) message = String.format(Locale.US, "Your total income for %s is ₱%,.2f. 💰", periodLabel, sum);
                else if (type.equals("TOTAL_DEBT")) message = String.format(Locale.US, "Your total outstanding debt is ₱%,.2f. 💳", sum);
                else if (type.equals("TOTAL_GOAL")) message = String.format(Locale.US, "You have saved a total of ₱%,.2f for your goals. 🏆", sum);
                else if (type.equals("TOTAL_ALLOCATION")) {
                    List<com.upreyvan.carti.model.BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
                    double totalAlloc = 0; for (com.upreyvan.carti.model.BudgetCategoryItem item : plan) totalAlloc += item.getAmount();
                    message = String.format(Locale.US, "Your total budget allocation is ₱%,.2f. 📊", totalAlloc);
                } else if (type.equals("TOTAL_OVERVIEW")) {
                    List<com.upreyvan.carti.model.BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
                    double totalAlloc = 0; for (com.upreyvan.carti.model.BudgetCategoryItem item : plan) totalAlloc += item.getAmount();
                    double inc = calculateSumInRange(txs, "TOTAL_INCOME", range); double exp = calculateSumInRange(txs, "TOTAL_EXPENSE", range);
                    message = String.format(Locale.US, "Here is your status (%s):\n💰 Income: ₱%,.2f\n📉 Expenses: ₱%,.2f\n📊 Allocation: ₱%,.2f\n🏦 Balance: ₱%,.2f", periodLabel, inc, exp, totalAlloc, pref.getBalance());
                } else { String cat = data.optString("targetCategory"); double catSum = calculateCategorySumInRange(txs, cat, range); message = String.format(Locale.US, "Your spending for %s (%s) is ₱%,.2f.", cat, periodLabel, catSum); }
                mainHandler.post(() -> cb.onSuccess(message));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e)); }
        }).start();
    }

    public void getDailyInsights(AiManager.AiCallback cb) {
        new Thread(() -> {
            try {
                String ctx = String.format(Locale.US, "B:%.0f|I:%.0f|E:%.0f", pref.getBalance(), pref.getTotalIncome(), pref.getTotalExpense());
                aiManager.getInsights(ctx, new AiManager.AiCallback() {
                    @Override public void onSuccess(String r) { cb.onSuccess(r); }
                    @Override public void onError(Throwable t) { cb.onError(t); }
                });
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    public void getSmartSuggestions(AiManager.AiCallback callback) {
        Calendar cal = Calendar.getInstance(); String weekKey = cal.get(Calendar.WEEK_OF_YEAR) + "-" + cal.get(Calendar.YEAR);
        new Thread(() -> {
            try {
                String ctx = String.format(Locale.US, "B:%.0f|I:%.0f|E:%.0f", pref.getBalance(), pref.getTotalIncome(), pref.getTotalExpense());
                aiManager.generateResponse("TASK: generate 4 coaching suggestions as JSON Array [{\"title\":\"\",\"description\":\"\",\"type\":\"SAVINGS|EXPENSE|GOAL|BILL\",\"actionText\":\"\"}]. CONTEXT:\n" + ctx, new AiManager.AiCallback() {
                    @Override public void onSuccess(String r) { pref.saveDailyAiSuggestions(weekKey, r); callback.onSuccess(r); }
                    @Override public void onError(Throwable t) { callback.onError(t); }
                });
            } catch (Exception e) { callback.onError(e); }
        }).start();
    }

    private double calculateSum(List<com.upreyvan.carti.model.Transaction> txs, String type, String period) { return calculateSumInRange(txs, type, getTimeRange(period, null)); }

    private double calculateSumInRange(List<com.upreyvan.carti.model.Transaction> txs, String type, long[] range) {
        double total = 0; String targetType;
        switch (type) { case "TOTAL_INCOME": targetType = "INCOME"; break; case "TOTAL_DEBT": targetType = "DEBT"; break; case "TOTAL_GOAL": targetType = "GOAL"; break; case "TOTAL_ALLOCATION": targetType = "ALLOCATION"; break; default: targetType = "EXPENSE"; break; }
        for (com.upreyvan.carti.model.Transaction t : txs) {
            boolean inRange = (range == null) || (t.getTimestampMillis() >= range[0] && t.getTimestampMillis() <= range[1]);
            if (inRange) { String txType = t.getType() != null ? t.getType().trim().toUpperCase() : ""; if (txType.equals(targetType)) total += t.getAmount(); }
        }
        return total;
    }

    private double calculateCategorySumInRange(List<com.upreyvan.carti.model.Transaction> txs, String category, long[] range) {
        double total = 0;
        for (com.upreyvan.carti.model.Transaction t : txs) { if (t.getTimestampMillis() >= range[0] && t.getTimestampMillis() <= range[1]) { if (t.getCategory().equalsIgnoreCase(category)) total += t.getAmount(); } }
        return total;
    }

    private long[] getTimeRange(String period, JSONObject data) {
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis(); long start; Calendar startCal = Calendar.getInstance(); startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
        switch (period) {
            case "CUSTOM": if (data != null) { start = parseManualDate(data.optString("startDate"), true); end = parseManualDate(data.optString("endDate"), false); } else { startCal.set(Calendar.DAY_OF_MONTH, 1); start = startCal.getTimeInMillis(); } break;
            case "TODAY": start = startCal.getTimeInMillis(); break;
            case "YESTERDAY": startCal.add(Calendar.DAY_OF_YEAR, -1); start = startCal.getTimeInMillis(); Calendar endYesterday = (Calendar) startCal.clone(); endYesterday.set(Calendar.HOUR_OF_DAY, 23); endYesterday.set(Calendar.MINUTE, 59); endYesterday.set(Calendar.SECOND, 59); end = endYesterday.getTimeInMillis(); break;
            case "THIS_WEEK": startCal.set(Calendar.DAY_OF_WEEK, startCal.getFirstDayOfWeek()); start = startCal.getTimeInMillis(); break;
            case "LAST_WEEK": startCal.add(Calendar.WEEK_OF_YEAR, -1); startCal.set(Calendar.DAY_OF_WEEK, startCal.getFirstDayOfWeek()); start = startCal.getTimeInMillis(); Calendar endLastWeek = (Calendar) startCal.clone(); endLastWeek.add(Calendar.DAY_OF_WEEK, 6); endLastWeek.set(Calendar.HOUR_OF_DAY, 23); endLastWeek.set(Calendar.MINUTE, 59); endLastWeek.set(Calendar.SECOND, 59); end = endLastWeek.getTimeInMillis(); break;
            case "LAST_MONTH": startCal.add(Calendar.MONTH, -1); startCal.set(Calendar.DAY_OF_MONTH, 1); start = startCal.getTimeInMillis(); Calendar endLastMonth = (Calendar) startCal.clone(); endLastMonth.set(Calendar.DAY_OF_MONTH, endLastMonth.getActualMaximum(Calendar.DAY_OF_MONTH)); endLastMonth.set(Calendar.HOUR_OF_DAY, 23); endLastMonth.set(Calendar.MINUTE, 59); endLastMonth.set(Calendar.SECOND, 59); end = endLastMonth.getTimeInMillis(); break;
            case "THIS_MONTH": default: startCal.set(Calendar.DAY_OF_MONTH, 1); start = startCal.getTimeInMillis(); break;
        }
        return new long[]{start, end};
    }

    private long parseManualDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) return System.currentTimeMillis();
        try {
            Calendar cal = Calendar.getInstance();
            if (dateStr.contains("/")) { String[] parts = dateStr.split("/"); int month = Integer.parseInt(parts[0]) - 1; int day = Integer.parseInt(parts[1]); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, day); }
            else { String[] parts = dateStr.split("\\s+"); int month = getMonthIndex(parts[0]); int day = Integer.parseInt(parts[1]); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, day); }
            if (isStart) { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); }
            else { cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); }
            return cal.getTimeInMillis();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }

    private int getMonthIndex(String monthName) {
        String m = monthName.toLowerCase();
        if (m.startsWith("jan")) return 0; if (m.startsWith("feb")) return 1; if (m.startsWith("mar")) return 2; if (m.startsWith("apr")) return 3; if (m.startsWith("may")) return 4; if (m.startsWith("jun")) return 5;
        if (m.startsWith("jul")) return 6; if (m.startsWith("aug")) return 7; if (m.startsWith("sep")) return 8; if (m.startsWith("oct")) return 9; if (m.startsWith("nov")) return 10; if (m.startsWith("dec")) return 11;
        return 0;
    }

    private String getPeriodLabel(String period) {
        switch (period) { case "TODAY": return "today"; case "YESTERDAY": return "yesterday"; case "THIS_WEEK": return "this week"; case "LAST_WEEK": return "last week"; case "LAST_MONTH": return "last month"; default: return "this month"; }
    }

    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private String buildHistoryContext(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        
        // ELITE FIREWALL: Use centralized parser logic to find the last "Reset Point"
        int startIndex = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (!history.get(i).isMe() && LocalIntentParser.isContextReset(history.get(i).getMessage())) {
                startIndex = i + 1;
                break;
            }
        }

        startIndex = Math.max(startIndex, history.size() - 10);
        
        StringBuilder sb = new StringBuilder("\nH:");
        for (int i = startIndex; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            if (!m.isShimmer()) {
                String role = m.getSenderName().toLowerCase().contains("ai") ? "AI" : "USER";
                sb.append(role).append(":").append(m.getMessage()).append("|");
            }
        }
        return sb.toString();
    }

    private void callAi(String msg, String ctx, boolean f, AiManager.AiCallback cb) {
        String fullCtx = ctx + "\nINTRO_DONE: " + (pref.isAiIntroDone() ? "TRUE" : "FALSE");
        aiManager.processChat(msg, fullCtx, f, new AiManager.AiCallback() {
            @Override public void onSuccess(String r) { pref.setAiIntroDone(true); cb.onSuccess(r); }
            @Override public void onError(Throwable t) { cb.onError(t); }
            @Override public void onActionDetected(JSONObject a) { pref.setAiIntroDone(true); if (a.optString("intent").equals("LOCAL_COMPUTE")) { handleLocalCompute(a.optJSONObject("data"), cb); } else { cb.onActionDetected(a); } }
        });
    }

    public void executeAction(JSONObject action) { actionHandler.executeAction(action); }
}
