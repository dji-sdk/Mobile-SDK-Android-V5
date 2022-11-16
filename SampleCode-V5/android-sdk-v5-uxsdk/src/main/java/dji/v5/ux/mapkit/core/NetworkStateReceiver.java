package dji.v5.ux.mapkit.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Created by joeyang on 3/22/18.
 */

public class NetworkStateReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkStateReceiver";
    private OnNetworkStateChangeListener listener;

    public NetworkStateReceiver(OnNetworkStateChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo dataNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) {
                isConnected = true;
            } else if (dataNetworkInfo != null && dataNetworkInfo.isConnected()) {
                isConnected = true;
            } else {
                isConnected = false;
            }
        } else {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network[] networks = connectivityManager.getAllNetworks();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < networks.length; i++) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(networks[i]);
                sb.append(" " + networkInfo.getTypeName() + " connect is " + networkInfo.isConnected());
                isConnected = isConnected || networkInfo.isConnected();
            }
            Log.i(TAG, "network infos: " + sb.toString());
        }
        listener.onNetworkStateChange(isConnected);
    }

    public interface OnNetworkStateChangeListener {
        void onNetworkStateChange(boolean isConnected);
    }
}
