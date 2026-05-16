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
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Functions;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

public class AppwriteManager {
    private static AppwriteManager instance;
    private final Client client;
    private final Account account;
    private final Functions functions;
    private final Databases databases;
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

        client = new Client(
                context,
                endpoint != null ? endpoint : "https://cloud.appwrite.io/v1",
                (endpoint != null ? endpoint : "http").replaceFirst("http", "ws"),
                false
        );
        client.setProject(projectId != null ? projectId : "");

        account = new Account(client);
        functions = new Functions(client);
        databases = new Databases(client);
        scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getIO());
    }

    public static synchronized AppwriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppwriteManager(context.getApplicationContext());
        }
        return instance;
    }

    public Client getClient() {
        return client;
    }

    public Account getAccount() {
        return account;
    }

    public Databases getDatabases() {
        return databases;
    }

    public Functions getFunctions() {
        return functions;
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
                // Step 1: Pre-emptively attempt to delete any existing session to avoid "prohibited" error
                try {
                    BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                        try {
                            return account.deleteSession("current", (kotlin.coroutines.Continuation<Object>) continuation2);
                        } catch (Exception e) {
                            return null;
                        }
                    });
                } catch (Exception ignored) {}

                // Step 2: Proceed with actual login
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

    public void logoutAll(AppwriteCallback<Object> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.deleteSessions((kotlin.coroutines.Continuation<Object>) continuation2);
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

    public void createPasswordRecovery(String email, String url, AppwriteCallback<Object> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.createRecovery(email, url, (kotlin.coroutines.Continuation<Object>) continuation2);
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

    public void updatePasswordRecovery(String userId, String secret, String password, AppwriteCallback<Object> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.updateRecovery(userId, secret, password, (kotlin.coroutines.Continuation<Object>) continuation2);
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

    public void updatePassword(String newPassword, String oldPassword, AppwriteCallback<User<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                User<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return account.updatePassword(newPassword, oldPassword, continuation2);
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

    public void deleteAccount(AppwriteCallback<Object> callback) {
        // As suggested, delete account is implemented via a Cloud Function (gateway)
        callGateway("deleteAccount", new HashMap<>(), new AppwriteCallback<Execution>() {
            @Override
            public void onSuccess(Execution result) {
                postSuccess(callback, result);
            }

            @Override
            public void onError(Throwable error) {
                postError(callback, error);
            }
        });
    }

    public void listDocuments(String databaseId, String collectionId, List<String> queries, AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                DocumentList<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return databases.listDocuments(
                                databaseId,
                                collectionId,
                                queries,
                                null,
                                (Class) Map.class,
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

    public void createDocument(String databaseId, String collectionId, String documentId, Map<String, Object> data, List<String> permissions, AppwriteCallback<Document<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Document<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return databases.createDocument(
                                databaseId,
                                collectionId,
                                documentId,
                                data,
                                permissions,
                                (Class) Map.class,
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

    public void deleteDocument(String databaseId, String collectionId, String documentId, AppwriteCallback<Object> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return databases.deleteDocument(
                                databaseId,
                                collectionId,
                                documentId,
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

    public void getDocument(String databaseId, String collectionId, String documentId, AppwriteCallback<Document<Map<String, Object>>> callback) {
        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Document<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, continuation2) -> {
                    try {
                        return databases.getDocument(
                                databaseId,
                                collectionId,
                                documentId,
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

    public void callGateway(String action, Map<String, Object> params, AppwriteCallback<Execution> callback) {
        getUser(new AppwriteCallback<>() {
            @Override
            public void onSuccess(User<Map<String, Object>> user) {
                executeGatewayCall(action, params, user.getId(), callback);
            }

            @Override
            public void onError(Throwable error) {
                android.util.Log.w("AppwriteManager", "User not logged in, checking action: " + action);
                if ("register".equals(action)) {
                    executeGatewayCall(action, params, null, callback);
                } else {
                    postError(callback, new Exception("App Error: Unauthorized. Please login first (Action: " + action + ")"));
                }
            }
        });
    }

    private void executeGatewayCall(String action, Map<String, Object> params, String userId, AppwriteCallback<Execution> callback) {
        Map<String, Object> payload = new HashMap<>(params);
        payload.put("action", action);
        if (userId != null) {
            payload.put("userId", userId);
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
