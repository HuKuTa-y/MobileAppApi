package com.example.lawapp.utils;

import android.util.LruCache;
import android.util.Log;

import com.example.lawapp.models.ArticleFull;

/**
 * Кэш в оперативной памяти
 * 🔥 LruCache для автоматической очистки
 * 🔥 Динамический размер на основе доступной памяти
 */
public class MemoryCache {

    private static final String TAG = "MemoryCache";
    private static MemoryCache instance;

    // 🔥 Кэш текстов (до 1/8 доступной памяти)
    private final LruCache<String, String> textCache;

    // 🔥 Кэш объектов статей (до 100 штук)
    private final LruCache<String, ArticleFull> articleCache;

    private boolean isInitialized = false;

    private MemoryCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;  // 1/8 доступной памяти

        textCache = new LruCache<String, String>(cacheSize) {
            @Override
            protected int sizeOf(String key, String value) {
                return value.length() / 1024;  // Размер в КБ
            }

            @Override
            protected void entryRemoved(boolean evicted, String key,
                                        String oldValue, String newValue) {
                if (evicted) {
                    Log.d(TAG, "🗑️ Удалено из кэша: " + key);
                }
            }
        };

        articleCache = new LruCache<>(100);
        isInitialized = true;

        Log.d(TAG, "✅ MemoryCache инициализирован (размер: " + cacheSize + " КБ)");
    }

    public static synchronized MemoryCache getInstance() {
        if (instance == null) {
            instance = new MemoryCache();
        }
        return instance;
    }

    // 🔹 Сохранить текст статьи
    public void putText(String articleTitle, String text) {
        if (!isInitialized || articleTitle == null || text == null) return;
        textCache.put(articleTitle, text);
    }

    // 🔹 Получить текст статьи
    public String getText(String articleTitle) {
        if (!isInitialized || articleTitle == null) return null;
        return textCache.get(articleTitle);
    }

    // 🔹 Сохранить статью
    public void putArticle(String key, ArticleFull article) {
        if (!isInitialized || key == null || article == null) return;
        articleCache.put(key, article);
    }

    // 🔹 Получить статью
    public ArticleFull getArticle(String key) {
        if (!isInitialized || key == null) return null;
        return articleCache.get(key);
    }

    // 🔹 Проверить наличие текста
    public boolean containsText(String articleTitle) {
        return isInitialized && articleTitle != null && textCache.get(articleTitle) != null;
    }

    // 🔹 Очистить всё
    public void clear() {
        if (!isInitialized) return;
        textCache.evictAll();
        articleCache.evictAll();
        Log.d(TAG, "🗑️ RAM-кэш очищен");
    }

    // 🔹 Статистика
    public int getCacheSize() {
        return isInitialized ? textCache.size() : 0;
    }

    public int getCacheHits() {
        return isInitialized ? textCache.hitCount() : 0;
    }

    public int getCacheMisses() {
        return isInitialized ? textCache.missCount() : 0;
    }

    public float getHitRate() {
        if (!isInitialized) return 0;
        int hits = textCache.hitCount();
        int misses = textCache.missCount();
        return (hits + misses > 0) ? (float) hits / (hits + misses) : 0;
    }

    // 🔹 Проверка инициализации
    public boolean isInitialized() {
        return isInitialized;
    }
}