package com.upreyvan.carti.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkMonitor {
    private static NetworkMonitor instance;
    private final ConnectivityManager cm;
    private final MutableLiveData<Boolean> status = new MutableLiveData<>();

    private NetworkMonitor(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        register();
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) instance = new NetworkMonitor(context.getApplicationContext());
        return instance;
    }

    private void register() {
        NetworkRequest req = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        cm.registerNetworkCallback(req, new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network n) { status.postValue(true); }
            @Override public void onLost(@NonNull Network n) { status.postValue(false); }
        });
        status.postValue(isOnline());
    }

    public LiveData<Boolean> getStatus() { return status; }

    public boolean isOnline() {
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
