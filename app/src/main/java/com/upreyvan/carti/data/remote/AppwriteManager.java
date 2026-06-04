package com.upreyvan.carti.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.upreyvan.carti.BuildConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.enums.ExecutionMethod;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Functions;
import io.appwrite.services.Storage;

import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.ToastHelper;

public class AppwriteManager {

    public static final String RECOVERY_URL = "https://mintyai.vercel.app/reset-password";

    private static AppwriteManager instance;
    private Client client;
    private Account account;
    private Functions functions;
    private Databases databases;
    private Storage storage;
    private final Gson gson = new Gson();
    private final CoroutineScope scope;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService initExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.CountDownLatch initLatch = new java.util.concurrent.CountDownLatch(1);
    private volatile boolean isInitialized = false;

    public interface AppwriteCallback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    private AppwriteManager(Context context) {
        scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getIO());
        initExecutor.execute(() -> {
            try {
                String endpoint = BuildConfig.APPWRITE_ENDPOINT;
                String projectId = BuildConfig.APPWRITE_PROJECT_ID;

                if (com.upreyvan.carti.util.Validator.isEmpty(projectId)) {
                    android.util.Log.e("AppwriteManager", "CRITICAL ERROR: APPWRITE_PROJECT_ID is missing!");
                    mainHandler.post(() -> ToastHelper.show(context, "Error: Project ID is missing!", ToastHelper.Status.ERROR));
                }

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
                storage = new Storage(client);
                isInitialized = true;
            } finally {
                initLatch.countDown();
            }
        });
    }

    public static synchronized AppwriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppwriteManager(context.getApplicationContext());
        }
        return instance;
    }

    private void ensureInitialized(Runnable action) {
        if (isInitialized) {
            action.run();
        } else {
            initExecutor.execute(action);
        }
    }

    public Client getClient() {
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return client;
    }

    private <T> void postSuccess(AppwriteCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(AppwriteCallback<?> callback, Throwable error) {
        String message = error.getMessage();
        String userFriendlyMessage = Constants.ErrorCodes.GENERIC_ERROR;

        if (error instanceof io.appwrite.exceptions.AppwriteException ae) {
            int code = ae.getCode() != null ? ae.getCode() : 0;
            switch (code) {
                case 401: userFriendlyMessage = Constants.ErrorCodes.UNAUTHORIZED; break;
                case 404: userFriendlyMessage = Constants.ErrorCodes.NOT_FOUND; break;
                case 429: userFriendlyMessage = Constants.ErrorCodes.RATE_LIMIT; break;
                case 500:
                case 502:
                case 503: userFriendlyMessage = Constants.ErrorCodes.SERVER_ERROR; break;
                default:
                    if (message != null && (message.contains("Network") || message.contains("hostname"))) {
                        userFriendlyMessage = Constants.ErrorCodes.NETWORK_ERROR;
                    }
                    break;
            }
        } else if (error instanceof java.net.UnknownHostException || error instanceof java.net.ConnectException) {
            userFriendlyMessage = Constants.ErrorCodes.NETWORK_ERROR;
        }

        android.util.Log.e("AppwriteManager", "Error: " + message, error);
        final String finalMsg = userFriendlyMessage;
        mainHandler.post(() -> callback.onError(new Exception(finalMsg)));
    }

    public void login(String email, String password, AppwriteCallback<Session> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                try {
                    BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                        try { return account.deleteSession("current", (kotlin.coroutines.Continuation<Object>) c2); }
                        catch (Exception e) { return null; }
                    });
                } catch (Exception ignored) {}

                Session result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return account.createEmailPasswordSession(email, password, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void getCurrentUser(AppwriteCallback<User<Map<String, Object>>> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                User<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return account.get(c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void logout(AppwriteCallback<Object> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try {
                        return account.deleteSession("current", c2);
                    } catch (Exception e) {
                        if (e instanceof io.appwrite.exceptions.AppwriteException ae) {
                            if (Integer.valueOf(401).equals(ae.getCode())) return new Object(); 
                            throw new RuntimeException(ae);
                        }
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void logoutAll(AppwriteCallback<Object> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try {
                        return account.deleteSessions(c2);
                    } catch (Exception e) {
                        if (e instanceof io.appwrite.exceptions.AppwriteException ae) {
                            if (Integer.valueOf(401).equals(ae.getCode())) return new Object(); // Already logged out
                            throw new RuntimeException(ae);
                        }
                        throw new RuntimeException(e);
                    }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void createPasswordRecovery(String email, String url, AppwriteCallback<Object> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return account.createRecovery(email, url != null ? url : RECOVERY_URL, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void createPasswordRecovery(String email, AppwriteCallback<Object> callback) {
        createPasswordRecovery(email, RECOVERY_URL, callback);
    }

    public void updatePasswordRecovery(String userId, String secret, String password, AppwriteCallback<Object> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return account.updateRecovery(userId, secret, password, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void updatePassword(String newPassword, String oldPassword, AppwriteCallback<User<Map<String, Object>>> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                User<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return account.updatePassword(newPassword, oldPassword, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void listDocuments(String databaseId, String collectionId, List<String> queries, AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                DocumentList<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return databases.listDocuments(databaseId, collectionId, queries, null, (Class) Map.class, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void createDocument(String databaseId, String collectionId, String documentId, Map<String, Object> data, List<String> permissions, AppwriteCallback<Document<Map<String, Object>>> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Document<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return databases.createDocument(databaseId, collectionId, documentId, data, permissions, (Class) Map.class, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void deleteDocument(String databaseId, String collectionId, String documentId, AppwriteCallback<Object> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Object result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return databases.deleteDocument(databaseId, collectionId, documentId, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void getDocument(String databaseId, String collectionId, String documentId, AppwriteCallback<Document<Map<String, Object>>> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Document<Map<String, Object>> result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return databases.getDocument(databaseId, collectionId, documentId, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void uploadFile(String bucketId, String fileId, io.appwrite.models.InputFile file, List<String> permissions, AppwriteCallback<io.appwrite.models.File> callback) {
        ensureInitialized(() -> BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                io.appwrite.models.File result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try { return storage.createFile(bucketId, fileId, file, permissions, null, c2); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        }));
    }

    public void callGateway(String action, Map<String, Object> params, AppwriteCallback<Execution> callback) {
        getCurrentUser(new AppwriteCallback<>() {
            @Override
            public void onSuccess(User<Map<String, Object>> user) {
                executeGatewayCall(action, params, user.getId(), callback);
            }

            @Override
            public void onError(Throwable error) {
                if ("register".equals(action)) {
                    executeGatewayCall(action, params, null, callback);
                } else {
                    postError(callback, new Exception(Constants.ErrorCodes.UNAUTHORIZED));
                }
            }
        });
    }

    private void executeGatewayCall(String action, Map<String, Object> params, String userId, AppwriteCallback<Execution> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("data", params);

        String functionId = BuildConfig.APPWRITE_GATEWAY_FUNCTION_ID;
        if (functionId == null || functionId.trim().isEmpty()) {
            postError(callback, new Exception("Appwrite Function ID is missing."));
            return;
        }

        final String finalFunctionId = functionId.trim();
        android.util.Log.d("AppwriteManager", "callGateway | action: " + action + " | userId: " + userId + " | functionId: " + finalFunctionId);

        BuildersKt.launch(scope, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (s, continuation) -> {
            try {
                Execution result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s2, c2) -> {
                    try {
                        return functions.createExecution(
                                finalFunctionId,
                                gson.toJson(payload),
                                false,
                                "/",
                                ExecutionMethod.POST,
                                Collections.emptyMap(),
                                null,
                                c2
                        );
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getCause() != null ? e.getCause() : e);
            }
            return Unit.INSTANCE;
        });
    }
}