package com.example.naziur.androidchat.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Naziur on 10/09/2018.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

    private OnNetworkStateChangeListener onNetworkStateChangeListener;

    public interface OnNetworkStateChangeListener{
        void onNetworkStateChanged(boolean connected);
    }

    public void setOnNetworkChangedListener(OnNetworkStateChangeListener networkStateChangeListener){
        onNetworkStateChangeListener = networkStateChangeListener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        int status = Network.getConnectivityStatusString(context);
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            if (status == Network.NETWORK_STATUS_NOT_CONNECTED) {
                onNetworkStateChangeListener.onNetworkStateChanged(false);
            } else {
                onNetworkStateChangeListener.onNetworkStateChanged(true);
            }
        }
    }
}
