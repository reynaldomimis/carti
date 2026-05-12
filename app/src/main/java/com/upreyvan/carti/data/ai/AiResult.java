package com.upreyvan.carti.data.ai;

public class AiResult {
    private final String message;
    private final IntentType intent;

    public AiResult(String message, IntentType intent) {
        this.message = message;
        this.intent = intent;
    }

    public String getMessage() {
        return message;
    }

    public IntentType getIntent() {
        return intent;
    }
}