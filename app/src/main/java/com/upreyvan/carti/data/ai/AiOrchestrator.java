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

public class AiOrchestrator {
    private static volatile AiOrchestrator instance;
    private final OkHttpClient client;
    private final List<AiModelProvider> providers = new ArrayList<>();

    public interface AiGatewayCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        default void onActionDetected(JSONObject action) {}
    }

    private AiOrchestrator(Context context) {
        this.client = new OkHttpClient.Builder().connectTimeout(40, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build();
        initProviders();
    }

    private void initProviders() {
        providers.add(new GeminiProvider());
        providers.add(new DeepSeekProvider());
        providers.add(new OpenAiProvider());
    }

    public static AiOrchestrator getInstance(Context context) {
        if (instance == null) { synchronized (AiOrchestrator.class) { if (instance == null) instance = new AiOrchestrator(context); } }
        return instance;
    }

    public void request(String prompt, AiGatewayCallback callback) { attemptProvider(0, prompt, callback); }

    private void attemptProvider(int index, String prompt, AiGatewayCallback callback) {
        if (index >= providers.size()) { callback.onError(new Exception("All AI providers exhausted.")); return; }
        AiModelProvider provider = providers.get(index);
        Request request = provider.buildRequest(prompt);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { attemptProvider(index + 1, prompt, callback); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) { attemptProvider(index + 1, prompt, callback); return; }
                    handleResult(provider.parseResponse(body.string()), callback);
                } catch (Exception e) { attemptProvider(index + 1, prompt, callback); }
            }
        });
    }

    private void handleResult(String text, AiGatewayCallback cb) {
        if (text == null || text.contains("[IGNORE]")) return;
        if (text.trim().startsWith("{")) {
            try {
                int last = text.lastIndexOf("}") + 1;
                cb.onActionDetected(new JSONObject(text.substring(0, last)));
                String msg = text.substring(last).trim();
                cb.onSuccess(msg.isEmpty() ? "Recorded." : msg);
            } catch (Exception e) { cb.onSuccess(text); }
        } else cb.onSuccess(text);
    }

    public abstract static class AiModelProvider {
        abstract Request buildRequest(String prompt);
        abstract String parseResponse(String raw) throws Exception;
    }

    private static class GeminiProvider extends AiModelProvider {
        @Override Request buildRequest(String p) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
            JSONObject json = new JSONObject();
            try { json.put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", p))))); } catch (Exception ignored) {}
            return new Request.Builder().url(url).post(RequestBody.create(json.toString(), MediaType.get("application/json"))).build();
        }
        @Override String parseResponse(String r) throws Exception { return new JSONObject(r).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text"); }
    }

    private static class DeepSeekProvider extends AiModelProvider {
        @Override Request buildRequest(String p) {
            String url = "https://api.deepseek.com/v1/chat/completions";
            JSONObject json = new JSONObject();
            try { json.put("model", "deepseek-chat").put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", p))); } catch (Exception ignored) {}
            return new Request.Builder().url(url).header("Authorization", "Bearer YOUR_DEEPSEEK_KEY").post(RequestBody.create(json.toString(), MediaType.get("application/json"))).build();
        }
        @Override String parseResponse(String r) throws Exception { return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"); }
    }

    private static class OpenAiProvider extends AiModelProvider {
        @Override Request buildRequest(String p) {
            String url = "https://api.openai.com/v1/chat/completions";
            JSONObject json = new JSONObject();
            try { json.put("model", "gpt-4o-mini").put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", p))); } catch (Exception ignored) {}
            return new Request.Builder().url(url).header("Authorization", "Bearer YOUR_OPENAI_KEY").post(RequestBody.create(json.toString(), MediaType.get("application/json"))).build();
        }
        @Override String parseResponse(String r) throws Exception { return new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"); }
    }
}
