package com.pmp.front;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

/**
 * Utilidad para saber si el dispositivo tiene conexión de red (WiFi o datos).
 * Se usa para avisar y bloquear los botones de enviar cuando no hay conexión.
 */
public final class RedHelper {

    private RedHelper() {
    }

    public static boolean hayConexion(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network red = cm.getActiveNetwork();
                if (red == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(red);
                return caps != null && (
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
