package com.example.lawapp.cache;

import android.content.Context;
import android.util.Log;

import com.example.lawapp.models.*;
import com.example.lawapp.utils.OfflineException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.*;

import retrofit2.Call;
import retrofit2.Response;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_FOLDER = "LawApp_Cache";
    private static final long CACHE_LIFETIME = 24 * 60 * 60 * 1000L; // 24 часа

    private static Context context;
    private static final Gson gson = new Gson();

    public static void initialize(Context ctx) {
        context = ctx.getApplicationContext();
        getCacheDir().mkdirs();
    }

    private static File getCacheDir() {
        return new File(context.getCacheDir(), CACHE_FOLDER);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getData(String endpoint, String cacheFileName,
                                Call<T> apiCall, boolean isEssential) throws OfflineException {
        File cacheFile = new File(getCacheDir(), getSafeFileName(cacheFileName));
        File metaFile = new File(cacheFile.getAbsolutePath() + ".meta");

        // 🔥 Попытка загрузить из кэша
        if (cacheFile.exists() && metaFile.exists()) {
            CacheMetadata meta = loadMetadata(metaFile);
            if (meta != null && System.currentTimeMillis() - meta.cachedAt < CACHE_LIFETIME) {
                try {
                    T cached = (T) loadFromCache(cacheFile, cacheFileName);
                    Log.d(TAG, "Загружено из кэша: " + cacheFileName);
                    return cached;
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка чтения кэша", e);
                }
            }
        }

        // 🔥 Загрузка из сети
        try {
            Response<T> response = apiCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                saveToCache(cacheFile, response.body());
                saveMetadata(metaFile, endpoint);
                Log.d(TAG, "Загружено из сети: " + cacheFileName);
                return response.body();
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сети", e);
        }

        // 🔥 Fallback на кэш при ошибке сети
        if (cacheFile.exists()) {
            try {
                T cached = (T) loadFromCache(cacheFile, cacheFileName);
                Log.d(TAG, "Fallback на кэш: " + cacheFileName);
                return cached;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка fallback", e);
            }
        }

        if (isEssential) {
            throw new OfflineException("Нет интернета и нет кэша для: " + cacheFileName);
        }
        return null;
    }

    private static String getSafeFileName(String fileName) {
        if (fileName.length() > 100 || fileName.contains("%")) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md5.digest(fileName.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                String ext = fileName.contains(".") ?
                        fileName.substring(fileName.lastIndexOf(".")) : ".cache.json";
                return sb.toString() + ext;
            } catch (Exception e) {
                return fileName;
            }
        }
        return fileName;
    }

    private static void saveToCache(File file, Object data) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
    }

    @SuppressWarnings("unchecked")
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
            return gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
        }
    }

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
            return null;
        }
    }

    public static boolean hasCache(String fileName) {
        return new File(getCacheDir(), getSafeFileName(fileName)).exists();
    }

    public static void clearCache() {
        File dir = getCacheDir();
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
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

    private static class CacheMetadata {
        long cachedAt;
        String sourceUrl;

        CacheMetadata(long cachedAt, String sourceUrl) {
            this.cachedAt = cachedAt;
            this.sourceUrl = sourceUrl;
        }
    }
}