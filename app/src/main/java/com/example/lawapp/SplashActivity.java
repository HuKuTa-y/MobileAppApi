package com.example.lawapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.utils.DataPrefetcher;
import com.example.lawapp.utils.MemoryCache;

/**
 * Экран заставки — скрывает время инициализации
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 800;  // Всего 800 мс!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Инициализация в фоне
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Инициализируем кэши
            CacheManager.initialize(this);
            MemoryCache.getInstance();
            DataPrefetcher.getInstance(this).prefetchEssentials();

            // Переход на главный экран
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Плавный переход без анимации
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}