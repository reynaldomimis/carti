package com.upreyvan.carti.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.upreyvan.carti.BuildConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.ID;
import io.appwrite.enums.ExecutionMethod;
import io.appwrite.models.Execution;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Functions;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

public class AppwriteManager {
    private static AppwriteManager instance;
    private final Account account;
    private final Functions functions;
    private final Gson gson = new Gson();
    private final CoroutineScope scope;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AppwriteCallback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    private AppwriteManager(Context context) {
        String endpoint = BuildConfig.APPWRITE_ENDPOINT;
        String projectId = BuildConfig.APPWRITE_PROJECT_ID;

        if (projectId == null || projectId.trim().isEmpty()) {
            android.util.Log.e("AppwriteManager", "CRITICAL ERROR: APPWRITE_PROJECT_ID is missing from BuildConfig!");
            mainHandler.post(() -> android.widget.Toast.makeText(context, "Error: Project ID is missing!", android.widget.Toast.LENGTH_LONG).show());
        }

        android.util.Log.d("AppwriteManager", "Initializing with Project: " + projectId + " | Endpoint: " + endpoint);

        Client client = new Client(
                context,
                endpoint != null ? endpoint : "https://cloud.appwrite.io/v1",
                (endpoint != null ? endpoint : "http").replaceFirst("http", "ws"),
                false
        );
        client.setProject(projectId != null ? projectId : "");

        account = new Account(client);
        functions = new Functions(client);
        scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getIO());
    }

    public static synchronized AppwriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppwriteManager(context.getApplicationContext());
        }
        return instance;
    }

    private <T> void postSuccess(AppwriteCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(AppwriteCallback<?> callback, Throwable error) {
        android.util.Log.e("AppwriteManager", "Operation Error: " + error.getMessage(), error);
        mainHandler.post(() -> callback.onError(error));
    }

    public void login(String email, String password, AppwriteCallback<Session> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Session result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.createEmailPasswordSession(email, password, continuation2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }

    public void getUser(AppwriteCallback<User<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                User<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.get(continuation2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }

    public void updatePrefs(Map<String, Object> prefs, AppwriteCallback<User<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                User<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.updatePrefs(prefs, continuation2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }

    public void logout(AppwriteCallback<Object> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.deleteSession("current", (kotlin.coroutines.Continuation<Object>) continuation2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }

    public void callGateway(String action, Map<String, Object> params, AppwriteCallback<Execution> callback) {
        getUser(new AppwriteCallback<>() {
            @Override
            public void onSuccess(User<Map<String, Object>> user) {
                executeGatewayCall(action, params, user.getId(), callback);
            }

            @Override
            public void onError(Throwable error) {
                if ("register".equals(action)) {
                    executeGatewayCall(action, params, null, callback);
                } else {
                    postError(callback, new Exception("Unauthorized: Please login first"));
                }
            }
        });
    }

    private void executeGatewayCall(String action, Map<String, Object> params, String userId, AppwriteCallback<Execution> callback) {
        Map<String, Object> payload = new HashMap<>(params);
        payload.put("a", action);
        if (userId != null) {
            payload.put("uid", userId);
        }

        String functionId = BuildConfig.APPWRITE_GATEWAY_FUNCTION_ID;

        if (functionId == null || functionId.trim().isEmpty()) {
            String endpointFunc = BuildConfig.APPWRITE_ENDPOINT_FUNCTION;
            if (endpointFunc != null && endpointFunc.contains("://")) {
                try {
                    String host = new java.net.URL(endpointFunc).getHost();
                    if (host != null && host.contains(".")) {
                        functionId = host.split("\\.")[0];
                    }
                } catch (Exception ignored) {}
            }
        }

        if (functionId == null || functionId.trim().isEmpty()) {
            postError(callback, new Exception("Appwrite Function ID is missing. Please check your secrets.properties"));
            return;
        }

        final String finalFunctionId = functionId.trim();
        android.util.Log.d("AppwriteManager", "Action: " + action + " | Using FunctionID: " + finalFunctionId + " | ProjectID: " + BuildConfig.APPWRITE_PROJECT_ID);

        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Execution result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return functions.createExecution(
                                finalFunctionId,
                                gson.toJson(payload),
                                false,
                                "/",
                                ExecutionMethod.POST,
                                Collections.emptyMap(),
                                null,
                                continuation2
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }
}
