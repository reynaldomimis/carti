package com.upreyvan.carti.data.ai;

import android.content.Context;
import org.json.JSONObject;

public class AiManager {
    private static volatile AiManager instance;
    private final AiOrchestrator orchestrator;

    private static final String PROMPT_CLASSIFY = """
            ROLE: Carti AI Engine (Strict Financial Classifier Only)
            
              YOU ARE NOT A CHATBOT.
              YOU ARE NOT AN ASSISTANT.
              YOU DO NOT TALK TO USERS.
            
              YOU ONLY CLASSIFY INPUT INTO JSON.
            
              ----------------------------------------
              ABSOLUTE OUTPUT RULE:
              - OUTPUT ONLY VALID JSON
              - NO TEXT BEFORE OR AFTER JSON
              - NO QUESTIONS
              - NO EXPLANATIONS
              - NO HUMAN SENTENCES
              ----------------------------------------
            
              LOCAL-FIRST ARCHITECTURE:
              - Local system is PRIMARY authority
              - AI is FALLBACK classifier only
              - AI must NOT override local logic
            
              ----------------------------------------
              CLASSIFICATION RULES:
            
              1. If input is unclear, nonsense, slang, or unknown word:
                 → intent = "UNKNOWN"
                 → category = null
                 → confidence <= 0.3
            
              2. NEVER guess category if not in SYSTEM_CATEGORIES
            
              3. NEVER infer missing amount
            
              4. NEVER assume intent from single word unless strong match
            
              5. NEVER generate follow-up questions or prompts
            
              ----------------------------------------
              TRANSACTION RULES:
            
              - EXPENSE = spending, buying, paying
              - INCOME = salary, allowance, earnings
              - DEBT = loan, utang, credit
              - GOAL = savings goal
              - ALLOCATION = subtype of INCOME only
            
              ----------------------------------------
              UNKNOWN WORD HANDLING:
              If word is not in known financial meaning:
              → ALWAYS return UNKNOWN
              → DO NOT interpret meaning
            
              Example:
              "foox" → UNKNOWN
              "asdf" → UNKNOWN
              "bakla" → UNKNOWN (non-financial content)
            
              ----------------------------------------
              OUTPUT FORMAT (STRICT JSON ONLY):
            
              {
                "intent": "EXPENSE|INCOME|DEBT|GOAL|ALLOCATION|SUMMARY|UNKNOWN",
                "amount": number | null,
                "category": string | null,
                "confidence": number,
                "needs_user_confirmation": boolean
              }
            """;

    private static final String PROMPT_INSIGHTS = """
            ROLE: Professional Financial Coach.
            TASK: Generate insights based on provided budget context.
            Understand Taglish, respond in English.
            Be concise and friendly.
            """;

    private AiManager(Context context) { this.orchestrator = AiOrchestrator.getInstance(context); }

    public static AiManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AiManager.class) {
                if (instance == null) instance = new AiManager(context);
            }
        }
        return instance;
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        default void onActionDetected(JSONObject action) {}
    }

    public void processChat(String msg, String ctx, AiCallback cb) {
        String cleanMsg = msg.replaceAll("[<>{}\\[\\]\\\\^`|]", "").trim().replaceAll("\\s+", " ");
        String fullPrompt = PROMPT_CLASSIFY + "\nCONTEXT:\n" + ctx + "\nUSER MESSAGE: " + cleanMsg;
        
        orchestrator.request(fullPrompt, new AiOrchestrator.AiGatewayCallback() {
            @Override public void onSuccess(String res) { cb.onSuccess(res); }
            @Override public void onError(Throwable t) { cb.onError(t); }
            @Override public void onActionDetected(JSONObject act) { cb.onActionDetected(act); }
        });
    }

    public void getInsights(String ctx, AiCallback cb) { 
        String prompt = PROMPT_INSIGHTS + "\nCONTEXT:\n" + ctx;
        orchestrator.request(prompt, new AiOrchestrator.AiGatewayCallback() {
            @Override public void onSuccess(String res) { cb.onSuccess(res); }
            @Override public void onError(Throwable t) { cb.onError(t); }
        });
    }
}
