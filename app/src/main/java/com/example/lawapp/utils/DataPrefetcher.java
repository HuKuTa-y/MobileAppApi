package com.example.lawapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Предзагружает данные в фоне
 * 🔥 Низкий приоритет потоков
 * 🔥 Безопасная очистка ресурсов
 */
public class DataPrefetcher {

    private static final String TAG = "DataPrefetcher";
    private static DataPrefetcher instance;

    private final LawApiService apiService;
    private final ExecutorService executor;
    private final Context context;
    private volatile boolean isShutdown = false;  // 🔥 Флаг завершения

    private DataPrefetcher(Context ctx) {
        context = ctx.getApplicationContext();
        apiService = ApiClient.getService();
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DataPrefetcher-Worker");
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);  // 🔥 Не блокирует выход из приложения
            return t;
        });
    }

    public static synchronized DataPrefetcher getInstance(Context ctx) {
        if (instance == null) {
            instance = new DataPrefetcher(ctx);
        }
        return instance;
    }

    // 🔹 Предзагружает кодексы и законы при старте
    public void prefetchEssentials() {
        if (isShutdown) {
            Log.w(TAG, "⚠️ Prefetcher завершён, предзагрузка невозможна");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "🚀 Предзагрузка кодексов...");
                CacheManager.getData("/api/codeks", "codeks.cache.json",
                        apiService.getCodeks(), false);

                Log.d(TAG, "🚀 Предзагрузка законов...");
                CacheManager.getData("/api/laws", "laws.cache.json",
                        apiService.getLaws(), false);

                Log.d(TAG, "✅ Предзагрузка завершена");
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка предзагрузки: " + e.getMessage(), e);
            }
        });
    }

    // 🔹 Предзагружает текст статьи
    public void prefetchArticleText(String articleTitle) {
        if (isShutdown || articleTitle == null) return;

        if (MemoryCache.getInstance().containsText(articleTitle)) {
            return;  // Уже в кэше
        }

        executor.execute(() -> {
            try {
                retrofit2.Response<com.example.lawapp.models.TextArticle> response = apiService.getArticleText(articleTitle).execute();
                if (response.body() != null && response.body().контент != null) {
                    MemoryCache.getInstance().putText(articleTitle, response.body().контент);
                    Log.d(TAG, "⚡ Текст предзагружен: " + articleTitle);
                }
            } catch (Exception e) {
                // Тихая ошибка — это только предзагрузка
            }
        });
    }

    // 🔹 Очистка ресурсов (вызывать в Application.onTerminate)
    public void shutdown() {
        isShutdown = true;
        if (!executor.isShutdown()) {
            executor.shutdown();
            Log.d(TAG, "🔒 Prefetcher завершён");
        }
    }

    // 🔹 Проверка статуса
    public boolean isReady() {
        return !isShutdown && !executor.isShutdown();
    }
}