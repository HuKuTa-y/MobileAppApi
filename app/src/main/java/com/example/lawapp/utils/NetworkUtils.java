package com.example.lawapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

/**
 * Утилиты для проверки сети
 * 🔥 Поддержка API 29+ (новый способ)
 * 🔥 Безопасная обработка null
 */
public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    // 🔹 Проверка наличия интернета (универсальный метод)
    public static boolean isOnline(Context context) {
        if (context == null) {
            Log.w(TAG, "⚠️ Context is null");
            return false;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                return false;
            }

            // 🔥 API 29+ (новый способ)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) return false;

                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
            // 🔥 API 28 и ниже (старый способ)
            else {
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка проверки сети: " + e.getMessage());
            return false;
        }
    }

    // 🔹 Проверка мобильного соединения
    public static boolean isMobile(Context context) {
        if (context == null) return false;

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) return false;

                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            } else {
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // 🔹 Проверка Wi-Fi соединения
    public static boolean isWifi(Context context) {
        if (context == null) return false;

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) return false;

                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else {
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI;
            }
        } catch (Exception e) {
            return false;
        }
    }
}