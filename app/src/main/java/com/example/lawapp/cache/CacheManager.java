package com.example.lawapp.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;
import android.util.Log;

import com.example.lawapp.models.*;
import com.example.lawapp.utils.MemoryCache;
import com.example.lawapp.utils.NetworkUtils;
import com.example.lawapp.utils.OfflineException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import retrofit2.Call;
import retrofit2.Response;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_FOLDER = "LawApp_Cache";
    private static final long CACHE_LIFETIME = 24 * 60 * 60 * 1000L;
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final String PREF_NAME = "lawapp_cache";

    private static SharedPreferences prefs;
    private static Context context;
    private static final Gson gson = new Gson();
    private static LruCache<String, String> memoryCache;

    public static void initialize(Context ctx) {
        context = ctx.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        getCacheDir().mkdirs();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.min(maxMemory / 8, 2048);
        memoryCache = new LruCache<>(cacheSize);
        Log.d(TAG, "Инициализирован");
    }

    public static void setLastUpdateTime(long time) {
        prefs.edit().putLong(KEY_LAST_UPDATE, time).apply();
    }

    public static long getLastUpdateTime() {
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }

    private static File getCacheDir() {
        if (context == null) return null;
        return new File(context.getCacheDir(), CACHE_FOLDER);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getData(String endpoint, String cacheFileName, Call<T> apiCall, boolean isEssential) throws OfflineException {
        if (context == null) return null;
        File cacheDir = getCacheDir();
        if (cacheDir == null) return null;

        // Имя файла используется как есть, без лишних преобразований
        File cacheFile = new File(cacheDir, cacheFileName);
        File metaFile = new File(cacheFile.getAbsolutePath() + ".meta");

        // Чтение из кэша
        if (cacheFile.exists() && metaFile.exists()) {
            try {
                T cached = (T) loadFromCache(cacheFile, cacheFileName);
                Log.d(TAG, "Загружено из кэша: " + cacheFileName);
                return cached;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка чтения", e);
            }
        }

        // Загрузка из сети
        try {
            Response<T> response = apiCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                T data = response.body();
                saveToCache(cacheFile, data);
                saveMetadata(metaFile, endpoint);
                setLastUpdateTime(System.currentTimeMillis());
                Log.d(TAG, "Загружено из сети: " + cacheFileName);
                return data;
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сети", e);
        }

        // Fallback
        if (cacheFile.exists()) {
            try {
                return (T) loadFromCache(cacheFile, cacheFileName);
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (isEssential) throw new OfflineException("Нет кэша и сети");
        return null;
    }

    private static void saveToCache(File file, Object data) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
        Log.d(TAG, "Сохранено: " + file.getName());
    }

    public static void saveListToCache(Context context, String urlKey, String fileName, List<?> data) {
        if (context == null || data == null) return;
        try {
            File cacheDir = new File(context.getCacheDir(), CACHE_FOLDER);
            if (!cacheDir.exists()) cacheDir.mkdirs();

            // Сохраняем под именем, которое передали (без изменений)
            File cacheFile = new File(cacheDir, fileName);

            Gson gson = new Gson();
            String json = gson.toJson(data);
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Log.d(TAG, "СПИСОК СОХРАНЕН: " + fileName);

            // Метаданные
            File metaFile = new File(cacheFile.getAbsolutePath() + ".meta");
            saveMetadata(metaFile, urlKey);

        } catch (IOException e) {
            Log.e(TAG, "Ошибка сохранения списка", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object loadFromCache(File file, String cacheFileName) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            if (cacheFileName.contains("codeks")) return gson.fromJson(reader, new TypeToken<List<Codek>>(){}.getType());
            if (cacheFileName.contains("laws")) return gson.fromJson(reader, new TypeToken<List<Law>>(){}.getType());
            if (cacheFileName.contains("articles_")) return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
            if (cacheFileName.contains("text_")) return gson.fromJson(reader, TextArticle.class);
            return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
        }
    }

    private static void saveMetadata(File file, String url) throws IOException {
        Map<String, Object> meta = new HashMap<>();
        meta.put("cachedAt", System.currentTimeMillis());
        meta.put("url", url);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(meta, writer);
        }
    }

    public static void clearAllCache(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".json") || f.getName().endsWith(".meta")) f.delete();
                }
            }
        }
        if (memoryCache != null) memoryCache.evictAll();
        Log.d(TAG, "Кэш очищен");
    }

    // Методы для текстов (оставляем как были, они работали)
    public static void putTextInMemoryCache(String key, String text) {
        if (memoryCache != null) memoryCache.put(key, text);
    }
    public static String getTextFromMemoryCache(String key) {
        return memoryCache != null ? memoryCache.get(key) : null;
    }

    public static boolean isArticleTextCached(String articleTitle) {
        if (articleTitle == null || context == null) return false;
        String hash = generateMd5(articleTitle);
        String fileName = "text_" + hash + ".cache.json";

        // 🔥 Ищем в папке LawApp_Cache
        File f = new File(getCacheDir(), fileName);
        return f.exists();
    }


    public static boolean canOpenArticle(Context ctx, String title) {
        return NetworkUtils.isOnline(ctx) || isArticleTextCached(title);
    }

    public static void saveTextToCache(Context context, String title, String content) {
        if (context == null || title == null || content == null) return;
        try {
            String hash = generateMd5(title);
            String fileName = "text_" + hash + ".cache.json";

            // 🔥 ИСПРАВЛЕНИЕ: Сохраняем в ту же папку LawApp_Cache, что и списки
            File cacheDir = new File(context.getCacheDir(), CACHE_FOLDER); // CACHE_FOLDER = "LawApp_Cache"
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File f = new File(cacheDir, fileName);

            Map<String, String> map = new HashMap<>();
            map.put("Название", title);
            map.put("Контент", content);

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(new Gson().toJson(map).getBytes(StandardCharsets.UTF_8));
            fos.close();

            Log.d(TAG, "💾 Текст сохранен в LawApp_Cache: " + fileName);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения текста", e);
            e.printStackTrace();
        }
    }

    public static TextArticle getTextFromCache(Context context, String title) {
        if (context == null || title == null) return null;
        try {
            String hash = generateMd5(title);
            String fileName = "text_" + hash + ".cache.json";

            // 🔥 Ищем в папке LawApp_Cache
            File cacheDir = new File(context.getCacheDir(), CACHE_FOLDER);
            File f = new File(cacheDir, fileName);

            if (!f.exists()) return null;

            FileReader r = new FileReader(f);
            Map<String, String> map = new Gson().fromJson(r, new TypeToken<Map<String,String>>(){}.getType());
            r.close();

            if (map.containsKey("Контент")) {
                TextArticle t = new TextArticle();
                t.название = map.get("Название");
                t.контент = map.get("Контент");
                return t;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String generateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}