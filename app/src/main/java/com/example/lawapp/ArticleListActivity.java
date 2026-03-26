package com.example.lawapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lawapp.adapters.ArticleAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.utils.MemoryCache;
import com.example.lawapp.utils.NetworkUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Экран списка статей кодекса/закона
 * Оптимизированная загрузка и кэширование
 * 60 FPS прокрутка
 */
public class ArticleListActivity extends AppCompatActivity {

    private static final String TAG = "ArticleListActivity";
    private static final long MIN_REFRESH_INTERVAL_MS = 5000;  // 5 секунд

    // UI Elements
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View offlineIndicator;

    // Adapter & Data
    private ArticleAdapter adapter;
    private final List<ArticleFull> articlesList = new ArrayList<>();

    // API & Threading
    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    // State
    private String sourceNumber;
    private String sourceTitle;
    private boolean isLoading = false;
    private long lastRefreshTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        initializeComponents();
        setupUI();
        loadArticles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOfflineIndicator();
        adapter.notifyDataSetChanged();  // Обновить цвета офлайн-режима
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }


    private void initializeComponents() {
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Статьи");
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.articlesRecyclerView);
        offlineIndicator = findViewById(R.id.offlineIndicator);
    }

    private void setupUI() {
        initViews();
        setupRecyclerView();
        setupSwipeRefresh();
        updateOfflineIndicator();
    }

    private void setupRecyclerView() {
        // ОПТИМИЗАЦИЯ: Отключаем лишние анимации для производительности
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ArticleAdapter(articlesList, this::onArticleClick);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadArticles);
        swipeRefresh.setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200,
                R.color.purple_700
        );
    }


    private void loadArticles() {
        // Защита от повторных запросов
        if (isLoading) {
            Log.d(TAG, "⏳ Загрузка уже выполняется");
            return;
        }

        // Минимальный интервал между обновлениями
        if (shouldSkipRefresh()) {
            Log.d(TAG, "⏱ Слишком частый запрос");
            if (!swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(false);
            }
            isLoading = false;
            return;
        }

        isLoading = true;
        showLoadingIndicator();

        executor.execute(this::fetchArticlesFromSource);
    }

    private boolean shouldSkipRefresh() {
        long now = System.currentTimeMillis();
        return (now - lastRefreshTime < MIN_REFRESH_INTERVAL_MS) && !swipeRefresh.isRefreshing();
    }

    private void showLoadingIndicator() {
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }
    }

    private void fetchArticlesFromSource() {
        try {
            String encoded = URLEncoder.encode(sourceNumber, StandardCharsets.UTF_8.toString());
            Log.d(TAG, "Запрос: /api/articles/by-source?source_number=" + encoded);

            boolean forceRefresh = swipeRefresh.isRefreshing();

            List<ArticleFull> articles = CacheManager.getData(
                    "/api/articles/by-source?source_number=" + encoded,
                    "articles_" + encoded + ".cache.json",
                    apiService.getArticlesBySource(sourceNumber),
                    forceRefresh
            );

            onArticlesLoaded(articles);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки: " + e.getMessage(), e);
            runOnUiThread(() ->
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } finally {
            hideLoadingIndicator();
        }
    }

    private void onArticlesLoaded(List<ArticleFull> articles) {
        mainHandler.post(() -> {
            articlesList.clear();

            if (articles != null) {
                for (ArticleFull article : articles) {
                    if (isValidArticle(article)) {
                        articlesList.add(article);

                        // ПРЕДЗАГРУЗКА текста в кэш (для мгновенного открытия)
                        MemoryCache.getInstance().putArticle(article.название, article);
                    }
                }
            }

            adapter.notifyDataSetChanged();
            updateTitleWithCount();

            if (articlesList.isEmpty()) {
                Toast.makeText(this, "Статьи не найдены", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "Загружено статей: " + articlesList.size());
        });
    }

    private void hideLoadingIndicator() {
        mainHandler.post(() -> {
            swipeRefresh.setRefreshing(false);
            isLoading = false;
            lastRefreshTime = System.currentTimeMillis();
            updateOfflineIndicator();
        });
    }

    private void updateTitleWithCount() {
        if (getSupportActionBar() != null) {
            String title = sourceTitle != null ? sourceTitle : "Статьи";
            getSupportActionBar().setTitle(title + " (" + articlesList.size() + ")");
        }
    }


    private void updateOfflineIndicator() {
        boolean isOnline = NetworkUtils.isOnline(this);
        offlineIndicator.setVisibility(isOnline ? View.GONE : View.VISIBLE);

        if (!isOnline && articlesList.isEmpty()) {
            Toast.makeText(this, "📴 Офлайн-режим. Показаны кэшированные данные.", Toast.LENGTH_LONG).show();
        }
    }


    private void onArticleClick(ArticleFull article) {
        if (!isValidArticle(article)) {
            Toast.makeText(this, "Ошибка: статья некорректна", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Открытие статьи: " + article.название);

        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        intent.putExtra("source_title", sourceTitle);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return true;
    }


    private boolean isValidArticle(ArticleFull article) {
        return article != null && article.название != null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Получаем данные из Intent
        sourceNumber = getIntent().getStringExtra("source_number");
        sourceTitle = getIntent().getStringExtra("source_title");

        Log.d(TAG, "Источник: " + sourceNumber + " — " + sourceTitle);

        if (sourceNumber == null) {
            Toast.makeText(this, "Ошибка: не передан источник", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}