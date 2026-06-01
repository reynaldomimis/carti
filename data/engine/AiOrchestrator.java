package com.upreyvan.carti.data.engine;

import android.content.Context;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.model.IntentType;
import java.util.List;

public class AiOrchestrator {
    private final AiRepository aiRepo;
    private final Context context;

    public AiOrchestrator(Context context) {
        this.context = context.getApplicationContext();
        this.aiRepo = AiRepository.getInstance(context);
    }

    public interface OrchestrationCallback {
        void onRuleResult(ParsingHelper.ParsedResult result);
        void onAiRequired(String message);
        void onInsightRequest(String prompt);
    }

    public void route(String text, List<ChatMessage> history, OrchestrationCallback callback) {
        // 1. RULE-BASED FIRST (Stateless)
        ParsingHelper.ParsedResult ruleResult = ParsingHelper.parse(text);
        
        if (ruleResult.isConfident && ruleResult.intent != IntentType.UNKNOWN) {
            // Priority 1: Deterministic match
            callback.onRuleResult(ruleResult);
            return;
        }

        // 2. AI FALLBACK
        if (ruleResult.intent == IntentType.SUMMARY || ruleResult.intent == IntentType.QUESTION) {
            callback.onInsightRequest(text);
        } else {
            // Ambiguous transaction or general chat
            callback.onAiRequired(text);
        }
    }
}
