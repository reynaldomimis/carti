package com.upreyvan.carti.data.ai;

import android.content.Context;
import com.upreyvan.carti.BuildConfig;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.util.Log;

public class AiOrchestrator {
    private static volatile AiOrchestrator instance;
    private final OkHttpClient client;
    private final List<AiModelProvider> providers = new ArrayList<>();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public interface AiGatewayCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        default void onActionDetected(JSONObject action) {}
    }

    private AiOrchestrator(Context context) {
        // May sapat na timeout para sa mga LLM responses
        this.client = new OkHttpClient.Builder()
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build();
        initProviders();
    }

    private void initProviders() {
        providers.add(new GeminiProvider());
        providers.add(new NvidiaNimProvider());
        providers.add(new DeepSeekProvider());
        providers.add(new OpenAiProvider());
    }

    public static AiOrchestrator getInstance(Context context) {
        if (instance == null) {
            synchronized (AiOrchestrator.class) {
                if (instance == null) instance = new AiOrchestrator(context);
            }
        }
        return instance;
    }

    public void request(String prompt, AiGatewayCallback callback) {
        attemptProvider(0, prompt, callback);
    }

    private void attemptProvider(int index, String prompt, AiGatewayCallback callback) {
        if (index >= providers.size()) {
            mainHandler.post(() -> callback.onError(new Exception("All AI providers exhausted.")));
            return;
        }

        AiModelProvider provider = providers.get(index);
        Request request = provider.buildRequest(prompt);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                attemptProvider(index + 1, prompt, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String raw = body != null ? body.string() : "";
                    if (!response.isSuccessful()) {
                        Log.e("AiOrchestrator", "Provider " + provider.getClass().getSimpleName() + " failed: " + response.code() + " - " + raw);
                        attemptProvider(index + 1, prompt, callback);
                        return;
                    }

                    String result = provider.parseResponse(raw);
                    mainHandler.post(() -> handleResult(result, callback));
                } catch (Exception e) {
                    Log.e("AiOrchestrator", "Error in " + provider.getClass().getSimpleName() + ": " + e.getMessage());
                    attemptProvider(index + 1, prompt, callback);
                }
            }
        });
    }

    private void handleResult(String text, AiGatewayCallback cb) {
        if (text == null) {
            cb.onError(new Exception("AI returned an empty response."));
            return;
        }
        if (text.contains("[IGNORE]")) {
            cb.onSuccess("I'm sorry, I cannot process that request right now.");
            return;
        }
        if (text.contains("INVALID_SESSION")) {
            cb.onSuccess("I haven't been trained for that. I only assist with family finance tracking.");
            return;
        }
        if (text.contains("{")) {
            try {
                int start = text.indexOf("{");
                int last = text.lastIndexOf("}") + 1;
                cb.onActionDetected(new JSONObject(text.substring(start, last)));
                String msg = text.substring(last).trim();
                cb.onSuccess(msg.isEmpty() ? "Processed successfully. ✅" : msg);
            } catch (Exception e) {
                cb.onSuccess(text);
            }
        } else {
            cb.onSuccess(text);
        }
    }

    public abstract static class AiModelProvider {
        abstract Request buildRequest(String prompt);
        abstract String parseResponse(String raw) throws Exception;
    }

    // ==========================================
    // 1. GEMINI PROVIDER
    // ==========================================
    private static class GeminiProvider extends AiModelProvider {
        @Override
        Request buildRequest(String p) {
            String apiKey = BuildConfig.GEMINI_API_KEY;
            if (apiKey != null) apiKey = apiKey.replace("\"", "").trim();

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            
            JSONObject json = new JSONObject();
            try {
                JSONArray partsArray = new JSONArray().put(new JSONObject().put("text", p));
                JSONObject contentObj = new JSONObject().put("parts", partsArray);
                json.put("contents", new JSONArray().put(contentObj));
            } catch (Exception e) {
                Log.e("AiOrchestrator", "Gemini JSON Error: " + e.getMessage());
            }

            return new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response body");
            JSONObject json = new JSONObject(r);
            
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                throw new Exception("Gemini API Error (" + error.optInt("code") + "): " + error.optString("message"));
            }
            
            if (!json.has("candidates") || json.getJSONArray("candidates").length() == 0) {
                if (json.has("promptFeedback")) return "I cannot respond to this due to safety filters.";
                throw new Exception("Gemini returned no response candidates.");
            }

            return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        }
    }

    // ==========================================
    // 2. DEEPSEEK PROVIDER
    // ==========================================
    private static class DeepSeekProvider extends AiModelProvider {
        @Override
        Request buildRequest(String p) {
            String url = "https://api.deepseek.com/v1/chat/completions";
            JSONObject json = new JSONObject();
            try {
                json.put("model", "deepseek-chat")
                        .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", p)));
            } catch (Exception ignored) {}

            return new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + "dasdas")
                    .header("Content-Type", "application/json") // Importante para sa OpenAI-compatible APIs
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response from DeepSeek");
            return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    // ==========================================
    // 3. OPENAI PROVIDER
    // ==========================================
    private static class OpenAiProvider extends AiModelProvider {
        @Override
        Request buildRequest(String p) {
            String url = "https://api.openai.com/v1/chat/completions";
            JSONObject json = new JSONObject();
            try {
                json.put("model", "gpt-4o-mini")
                        .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", p)));
            } catch (Exception ignored) {}

            return new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + "3434")
                    .header("Content-Type", "application/json") // Importante para maiwasan ang 415 o 400 error
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response from OpenAI");
            return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    // ==========================================
    // 4. NVIDIA NIM PROVIDER
    // ==========================================
    private static class NvidiaNimProvider extends AiModelProvider {
        @Override
        Request buildRequest(String p) {
            String url = "https://integrate.api.nvidia.com/v1/chat/completions";
            String apiKey = BuildConfig.NVIDIA_API_KEY;
            if (apiKey != null) apiKey = apiKey.replace("\"", "").trim();

            JSONObject json = new JSONObject();
            try {
                json.put("model", "meta/llama-3.1-8b-instruct")
                        .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", p)));
            } catch (Exception ignored) {}

            return new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response from NVIDIA NIM");
            return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }
}
