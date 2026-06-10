package com.upreyvan.carti.managers.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.upreyvan.carti.BuildConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AiOrchestrator {
    private static volatile AiOrchestrator instance;
    private final OkHttpClient client;
    private final List<AiModelProvider> providers = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AiGatewayCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        default void onActionDetected(JSONObject action) {}
    }

    private AiOrchestrator() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
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
                if (instance == null) instance = new AiOrchestrator();
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
                        attemptProvider(index + 1, prompt, callback);
                        return;
                    }
                    String result = provider.parseResponse(raw);
                    mainHandler.post(() -> handleResult(result, callback));
                } catch (Exception e) {
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
        
        String cleanedText = text.replaceAll("(?i)^(h:|carti:|@carti:|assistant:|ai:|user:|ctx:)\\s*", "").trim();

        if (cleanedText.contains("[IGNORE]")) {
            cb.onSuccess("I'm sorry, I cannot process that request right now.");
            return;
        }
        if (cleanedText.contains("INVALID_SESSION")) {
            cb.onSuccess("Session invalid or guest mode. Please ensure you are logged in and part of a family.");
            return;
        }
        if (cleanedText.contains("{")) {
            try {
                int start = cleanedText.indexOf("{");
                int last = cleanedText.lastIndexOf("}") + 1;
                cb.onActionDetected(new JSONObject(cleanedText.substring(start, last)));
                String before = cleanedText.substring(0, start).trim();
                String after = cleanedText.substring(last).trim();
                String msg = (before + " " + after).replaceAll("(?i)^(h:|carti:|@carti:|assistant:)\\s*", "").trim();
                if (!msg.isEmpty()) cb.onSuccess(msg);
            } catch (Exception e) {
                cb.onSuccess(cleanedText);
            }
        } else {
            cb.onSuccess(cleanedText);
        }
    }

    public abstract static class AiModelProvider {
        abstract Request buildRequest(String prompt);
        abstract String parseResponse(String raw) throws Exception;
    }

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
            } catch (Exception ignored) {}
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
            if (!json.has("candidates") || json.getJSONArray("candidates").length() == 0) throw new Exception("Gemini returned no response candidates.");
            return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        }
    }

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
                    .header("Authorization", "Bearer " + BuildConfig.DEEPSEEK_API_KEY)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response from DeepSeek");
            return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

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
                    .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();
        }

        @Override
        String parseResponse(String r) throws Exception {
            if (r == null || r.isEmpty()) throw new Exception("Empty response from OpenAI");
            return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

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
