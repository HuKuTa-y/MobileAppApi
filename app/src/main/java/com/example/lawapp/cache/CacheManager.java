package com.example.lawapp.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;
import android.content.Context;
import android.util.Log;

import com.example.lawapp.models.*;
import com.example.lawapp.utils.MemoryCache;
import com.example.lawapp.utils.NetworkUtils;
import com.example.lawapp.utils.OfflineException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Менеджер кэширования данных
 * 🔥 Двухуровневый кэш (RAM + Disk)
 * 🔥 Офлайн-режим
 * 🔥 Автоочистка старого кэша
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_FOLDER = "LawApp_Cache";
    private static final long CACHE_LIFETIME = 24 * 60 * 60 * 1000L; // 24 часа
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100 МБ
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final String PREF_NAME = "lawapp_cache";

    private static SharedPreferences prefs;
    private static Context context;
    private static final Gson gson = new Gson();

    // 🔥 Кэш в памяти (LruCache) для мгновенного доступа
    private static LruCache<String, String> memoryCache;

    // 🔥 Инициализация (вызывать в MainActivity.onCreate или Application)
    public static void initialize(Context ctx) {
        context = ctx.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        getCacheDir().mkdirs();

        // 🔥 Инициализация RAM-кэша (1/8 от доступной памяти, макс 2 МБ)
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.min(maxMemory / 8, 2048);
        memoryCache = new LruCache<>(cacheSize);

        Log.d(TAG, "✅ CacheManager инициализирован (RAM кэш: " + cacheSize + " КБ)");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Время обновления
    // ─────────────────────────────────────────────────────────────────────────────

    public static void setLastUpdateTime(long time) {
        prefs.edit().putLong(KEY_LAST_UPDATE, time).apply();
        Log.d(TAG, "📅 Время обновления сохранено: " + new Date(time));
    }

    public static long getLastUpdateTime() {
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Директория кэша
    // ─────────────────────────────────────────────────────────────────────────────

    private static File getCacheDir() {
        if (context == null) {
            Log.e(TAG, "❌ Context не инициализирован! Вызовите initialize()");
            return null;
        }
        return new File(context.getCacheDir(), CACHE_FOLDER);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Основной метод получения данных (кэш + сеть)
    // ─────────────────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getData(String endpoint, String cacheFileName,
                                Call<T> apiCall, boolean isEssential) throws OfflineException {

        if (context == null) {
            Log.e(TAG, "❌ CacheManager не инициализирован!");
            return null;
        }

        File cacheDir = getCacheDir();
        if (cacheDir == null) return null;

        File cacheFile = new File(cacheDir, getSafeFileName(cacheFileName));
        File metaFile = new File(cacheFile.getAbsolutePath() + ".meta");

        // 🔥 1. Попытка загрузить из кэша
        if (cacheFile.exists() && metaFile.exists()) {
            CacheMetadata meta = loadMetadata(metaFile);
            if (meta != null && System.currentTimeMillis() - meta.cachedAt < CACHE_LIFETIME) {
                try {
                    T cached = (T) loadFromCache(cacheFile, cacheFileName);
                    Log.d(TAG, "📦 Загружено из кэша: " + cacheFileName);
                    return cached;
                } catch (Exception e) {
                    Log.e(TAG, "⚠️ Ошибка чтения кэша", e);
                }
            }
        }

        // 🔥 2. Загрузка из сети
        try {
            Log.d(TAG, "🌐 Загрузка из сети: " + endpoint);
            Response<T> response = apiCall.execute();

            if (response.isSuccessful() && response.body() != null) {
                T data = response.body();

                // 🔥 СОХРАНЯЕМ В КЭШ
                saveToCache(cacheFile, data);
                saveMetadata(metaFile, endpoint);

                // 🔥 ОБНОВЛЯЕМ ВРЕМЯ ПОСЛЕДНЕГО УСПЕШНОГО ЗАПРОСА
                setLastUpdateTime(System.currentTimeMillis());

                Log.d(TAG, "✅ Загружено из сети: " + cacheFileName);
                return data;
            } else {
                Log.e(TAG, "❌ Ошибка API: " + response.code() + " - " + response.message());
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сети", e);
        }

        // 🔥 3. Fallback на кэш при ошибке сети
        if (cacheFile.exists()) {
            try {
                T cached = (T) loadFromCache(cacheFile, cacheFileName);
                Log.d(TAG, "🔄 Fallback на кэш: " + cacheFileName);
                return cached;
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Ошибка fallback", e);
            }
        }

        // 🔥 4. Если данные критичны и ничего не получилось — выбрасываем исключение
        if (isEssential) {
            throw new OfflineException("Нет интернета и нет кэша для: " + cacheFileName);
        }

        Log.w(TAG, "⚠️ Возвращаем null для: " + cacheFileName);
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Безопасное имя файла
    // ─────────────────────────────────────────────────────────────────────────────

    private static String getSafeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "default.cache.json";
        }

        if (fileName.length() > 100 || fileName.contains("%") || fileName.contains("/") || fileName.contains("\\")) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md5.digest(fileName.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                String ext = fileName.contains(".") ?
                        fileName.substring(fileName.lastIndexOf(".")) : ".cache.json";
                return sb.toString() + ext;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка хеширования имени файла", e);
                return "hashed.cache.json";
            }
        }
        return fileName;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Сохранение данных в кэш
    // ─────────────────────────────────────────────────────────────────────────────

    private static void saveToCache(File file, Object data) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
        Log.d(TAG, "💾 Сохранено в кэш: " + file.getName());

        // 🔥 Проверяем размер кэша после сохранения
        trimCache();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Загрузка данных из кэша
    // ─────────────────────────────────────────────────────────────────────────────

    public static void clearAllCache(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            String[] children = cacheDir.list();
            if (children != null) {
                for (String child : children) {
                    // Удаляем только наши файлы кэша (.json и .cache.json)
                    if (child.endsWith(".json") || child.endsWith(".cache.json")) {
                        new File(cacheDir, child).delete();
                    }
                }
            }
        }
        // Также очищаем MemoryCache (оперативную память)
        MemoryCache.getInstance().clear();

        Log.d("CacheManager", "✅ Весь кэш очищен");
    }

    /**
     * Метод для явного сохранения списка в кэш (используется при полной загрузке)
     */
    public static void saveListToCache(Context context, String urlKey, String fileName, List<?> data) {
        if (context == null || data == null) return;

        try {
            File cacheFile = new File(context.getCacheDir(), fileName);
            Gson gson = new Gson();
            String json = gson.toJson(data);

            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Log.d("CacheManager", "💾 Список сохранен в кэш: " + fileName);
        } catch (IOException e) {
            Log.e("CacheManager", "Ошибка сохранения списка: " + e.getMessage());
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object loadFromCache(File file, String cacheFileName) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            // 🔥 Определяем тип по имени файла
            if (cacheFileName.contains("codeks")) {
                return gson.fromJson(reader, new TypeToken<List<Codek>>(){}.getType());
            } else if (cacheFileName.contains("laws")) {
                return gson.fromJson(reader, new TypeToken<List<Law>>(){}.getType());
            } else if (cacheFileName.contains("articles_full") ||
                    cacheFileName.contains("articles_")) {
                return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
            } else if (cacheFileName.contains("text_") ||
                    cacheFileName.contains("text_new")) {
                return gson.fromJson(reader, TextArticle.class);
            } else if (cacheFileName.contains("search_")) {
                return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
            }
            // По умолчанию пробуем как List<ArticleFull>
            Log.w(TAG, "⚠️ Неизвестный тип кэша, используем ArticleFull: " + cacheFileName);
            return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Метаданные
    // ─────────────────────────────────────────────────────────────────────────────

    private static void saveMetadata(File file, String url) throws IOException {
        CacheMetadata meta = new CacheMetadata(System.currentTimeMillis(), url);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(meta, writer);
        }
    }

    private static CacheMetadata loadMetadata(File file) {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, CacheMetadata.class);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка чтения метаданных", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 Управление кэшем
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean hasCache(String fileName) {
        File dir = getCacheDir();
        if (dir == null) return false;
        return new File(dir, getSafeFileName(fileName)).exists();
    }

    public static void clearCache() {
        File dir = getCacheDir();
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {  // 🔥 ПРОВЕРКА НА NULL
                int count = 0;
                for (File f : files) {
                    if (f.delete()) count++;
                }
                Log.d(TAG, "🗑️ Очищено файлов кэша: " + count);
            }
        }

        // 🔥 Очищаем и RAM-кэш
        if (memoryCache != null) {
            memoryCache.evictAll();
        }
    }

    public static long getCacheAge(String fileName) {
        File metaFile = new File(getCacheDir(), getSafeFileName(fileName) + ".meta");
        if (!metaFile.exists()) return -1;
        try {
            CacheMetadata meta = loadMetadata(metaFile);
            return meta != null ? System.currentTimeMillis() - meta.cachedAt : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isCacheExpired(String fileName) {
        long age = getCacheAge(fileName);
        return age < 0 || age > CACHE_LIFETIME;
    }

    // 🔥 Автоматическая очистка старого кэша (если > 100 МБ)
    private static void trimCache() {
        File dir = getCacheDir();
        if (dir == null || !dir.exists()) return;

        long size = calculateCacheSize(dir);
        if (size > MAX_CACHE_SIZE) {
            Log.d(TAG, "Кэш превышает лимит (" + size / 1024 / 1024 + " МБ), обрезка...");
            File[] files = dir.listFiles();
            if (files != null) {
                // Сортируем по времени (самые старые первыми)
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File f : files) {
                    if (f.delete()) {
                        size -= f.length();
                        Log.d(TAG, "Удалён старый файл: " + f.getName());
                        if (size <= MAX_CACHE_SIZE) break;
                    }
                }
            }
        }
    }

    private static long calculateCacheSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) size += f.length();
            }
        }
        return size;
    }

    // 🔹 RAM-кэш (мгновенный доступ)

    public static void putTextInMemoryCache(String key, String text) {
        if (memoryCache != null && key != null && text != null) {
            memoryCache.put(key, text);
            Log.d(TAG, "💾 Текст в RAM-кэше: " + key);
        }
    }

    public static String getTextFromMemoryCache(String key) {
        if (memoryCache != null && key != null) {
            String cached = memoryCache.get(key);
            if (cached != null) {
                Log.d(TAG, "⚡ Текст из RAM-кэша: " + key);
            }
            return cached;
        }
        return null;
    }

    public static boolean containsTextInMemoryCache(String key) {
        return memoryCache != null && key != null && memoryCache.get(key) != null;
    }

    // 🔹 Проверка доступности статьи

    public static boolean isArticleTextCached(String articleTitle) {
        // 🔥 Сначала проверяем RAM-кэш
        if (containsTextInMemoryCache(articleTitle)) {
            return true;
        }

        // 🔥 Потом проверяем диск
        if (articleTitle == null || context == null) return false;

        try {
            String encoded = URLEncoder.encode(articleTitle, StandardCharsets.UTF_8.toString());
            String cacheFileName = "text_" + encoded + ".cache.json";
            File cacheFile = new File(getCacheDir(), getSafeFileName(cacheFileName));
            return cacheFile.exists();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки кэша статьи: " + e.getMessage());
            return false;
        }
    }

    public static boolean canOpenArticle(Context ctx, String articleTitle) {
        // 🔥 Явный вызов (не static import)
        return NetworkUtils.isOnline(ctx) || isArticleTextCached(articleTitle);
    }

    // 🔹 Метаданные кэша

    private static class CacheMetadata {
        long cachedAt;
        String sourceUrl;

        CacheMetadata(long cachedAt, String sourceUrl) {
            this.cachedAt = cachedAt;
            this.sourceUrl = sourceUrl;
        }
    }
    /**
     * Сохраняет текст конкретной статьи в кэш
     * @param context Контекст приложения
     * @param articleTitle Название статьи (используется для имени файла)
     * @param content Текст статьи
     */
    /**
     * Сохраняет текст статьи в кэш, используя ХЭШ названия вместо полного имени.
     * Это решает проблему ENAMETOOLONG.
     */
    public static void saveTextToCache(Context context, String articleTitle, String content) {
        if (context == null || articleTitle == null || content == null) return;

        try {
            // 🔥 ГЕНЕРИРУЕМ КРОТКОЕ УНИКАЛЬНОЕ ИМЯ ФАЙЛА ЧЕРЕЗ MD5
            String fileNameHash = generateMd5(articleTitle);
            String fileName = "text_" + fileNameHash + ".cache.json";

            File cacheFile = new File(context.getCacheDir(), fileName);

            // Создаем JSON структуру
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("Название", articleTitle); // Сохраняем оригинальное название внутри JSON
            map.put("Контент", content);

            String json = gson.toJson(map);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile);
            fos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.close();

            android.util.Log.d("CacheManager", "💾 Текст сохранен (хэш): " + fileNameHash);

        } catch (Exception e) {
            android.util.Log.e("CacheManager", "Ошибка сохранения текста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔥 Вспомогательный метод для генерации MD5 хэша
    private static String generateMd5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString(); // Возвращает строку из 32 символов
        } catch (Exception e) {
            // Если ошибка, возвращаем таймстамп как запасной вариант (редко случается)
            return String.valueOf(System.currentTimeMillis());
        }
    }
}