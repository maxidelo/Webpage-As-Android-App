package com.maxidelo.webapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

public class NetworkStateReceiver extends BroadcastReceiver {

    private static final int DISCONNECTED = 0;
    private static final int CONNECTED = 1;
    private static final int NO_PREVIOUS_STATE = -1;

    protected List<NetworkStateReceiverListener> listeners;
    protected Boolean connected;
    protected ConnectivityManager manager;
    protected int previousState;

    public NetworkStateReceiver() {
        listeners = new ArrayList<NetworkStateReceiverListener>();
        connected = null;
        previousState = NO_PREVIOUS_STATE;
    }

    public void onReceive(Context context, Intent intent) {
        if(intent == null || intent.getExtras() == null)
            return;

        manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        if(ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        } else if(intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
            connected = false;
        }

        notifyStateToAll();
    }


    private void notifyStateToAll() {
        for(NetworkStateReceiverListener listener : listeners)
            notifyState(listener);
    }

    private void notifyState(NetworkStateReceiverListener listener) {
        if(connected == null || listener == null)
            return;

        if(connected == true && (previousState == DISCONNECTED || previousState == NO_PREVIOUS_STATE)) {
            listener.networkAvailable();
            previousState = CONNECTED;
        }
        else if(connected == false && (previousState == CONNECTED || previousState == NO_PREVIOUS_STATE)) {
            listener.networkUnavailable();
            previousState = DISCONNECTED;
        }
    }

    public void addListener(NetworkStateReceiverListener l) {
        listeners.add(l);
        notifyState(l);
    }

    public void removeListener(NetworkStateReceiverListener l) {
        listeners.remove(l);
    }

    public interface NetworkStateReceiverListener {
        public void networkAvailable();
        public void networkUnavailable();
    }
}