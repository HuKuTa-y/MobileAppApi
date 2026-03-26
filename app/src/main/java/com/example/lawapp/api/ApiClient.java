package com.example.lawapp.api;

import android.content.Context;
import android.util.Log;

import com.example.lawapp.utils.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Оптимизированный API клиент
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://192.168.133.20:5000/";
    private static final int CACHE_SIZE = 10 * 1024 * 1024;
    private static final boolean ENABLE_LOGS = true;

    private static LawApiService service;
    private static OkHttpClient okHttpClient;
    private static Cache cache;
    private static Context applicationContext;

    private ApiClient() {}

    public static void initialize(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static synchronized LawApiService getService() {
        if (service == null) {
            service = createRetrofit().create(LawApiService.class);
        }
        return service;
    }

    private static Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient createOkHttpClient() {
        if (okHttpClient != null) {
            return okHttpClient;
        }

        // Инициализация кэша
        if (applicationContext != null) {
            File cacheDir = new File(applicationContext.getCacheDir(), "http_cache");
            cache = new Cache(cacheDir, CACHE_SIZE);
        }

        // Логирование
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(
                ENABLE_LOGS ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE
        );

        okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request request = chain.request();

                        // Если нет интернета — пробуем кэш
                        if (applicationContext != null && !NetworkUtils.isOnline(applicationContext)) {
                            request = request.newBuilder()
                                    .cacheControl(CacheControl.FORCE_CACHE)
                                    .build();
                        }

                        return chain.proceed(request);
                    }
                })
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)
                .build();

        return okHttpClient;
    }

    public static void clearCache() {
        if (cache != null) {
            try {
                cache.evictAll();
                Log.d(TAG, "HTTP кэш очищен");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка очистки кэша: " + e.getMessage());
            }
        }
    }

    public static void shutdown() {
        if (okHttpClient != null) {
            okHttpClient.connectionPool().evictAll();
            okHttpClient = null;
        }
        service = null;
    }
    // В конец класса:
    public static long getCacheSize() {
        if (cache != null) {
            try {
                return cache.size();
            } catch (IOException e) {
                Log.e(TAG, "Ошибка получения размера кэша: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    public static boolean isCacheInitialized() {
        return cache != null;
    }
}