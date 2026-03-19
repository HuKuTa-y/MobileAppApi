package com.example.lawapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<ArticleFull> articlesList = new ArrayList<>();

    // 🔥 Только SwipeRefreshLayout (ProgressBar убран)
    private SwipeRefreshLayout swipeRefresh;

    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    private String sourceNumber;
    private String sourceTitle;

    // 🔥 Оптимизация: флаг загрузки (защита от повторных запросов)
    private boolean isLoading = false;
    // 🔥 Оптимизация: время последнего обновления (минимум 5 сек между запросами)
    private long lastRefreshTime = 0;
    private static final long MIN_REFRESH_INTERVAL_MS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        sourceNumber = getIntent().getStringExtra("source_number");
        sourceTitle = getIntent().getStringExtra("source_title");

        Log.d("ARTICLE_LIST", "sourceNumber=" + sourceNumber + ", sourceTitle=" + sourceTitle);

        if (sourceNumber == null) {
            Toast.makeText(this, "Ошибка: не передан источник", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(sourceTitle != null ? sourceTitle : "Статьи");
        }

        // Инициализация
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // 🔥 Инициализация UI (без ProgressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.articlesRecyclerView);

        if (recyclerView == null) {
            Log.e("ARTICLE_LIST", "❌ articlesRecyclerView не найден!");
            finish();
            return;
        }

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(articlesList, this::onArticleClick);
        recyclerView.setAdapter(adapter);

        // 🔥 Настройка SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadArticles);
        swipeRefresh.setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200,
                R.color.purple_700
        );

        // Загрузка статей
        loadArticles();
    }

    private void loadArticles() {
        // 🔥 ОПТИМИЗАЦИЯ 1: Защита от повторных запросов
        if (isLoading) {
            Log.d("ARTICLE_LIST", "⏳ Загрузка уже выполняется, пропуск");
            return;
        }

        // 🔥 ОПТИМИЗАЦИЯ 2: Минимальный интервал между обновлениями (5 сек)
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < MIN_REFRESH_INTERVAL_MS && !swipeRefresh.isRefreshing()) {
            Log.d("ARTICLE_LIST", "⏱ Слишком частый запрос, пропуск");
            return;
        }

        isLoading = true;

        // 🔥 Показываем индикатор свайпа (единственный визуальный сигнал)
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }

        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(sourceNumber, StandardCharsets.UTF_8.toString());
                Log.d("ARTICLE_LIST", "Запрос: /api/articles/by-source?source_number=" + encoded);

                // 🔥 ОПТИМИЗАЦИЯ 3: Принудительное обновление только при свайпе
                boolean forceRefresh = swipeRefresh.isRefreshing();

                List<ArticleFull> articles = CacheManager.getData(
                        "/api/articles/by-source?source_number=" + encoded,
                        "articles_" + encoded + ".cache.json",
                        apiService.getArticlesBySource(sourceNumber),
                        forceRefresh
                );

                Log.d("ARTICLE_LIST", "Получено статей: " + (articles != null ? articles.size() : "NULL"));

                if (articles != null) {
                    mainHandler.post(() -> {
                        articlesList.clear();
                        for (ArticleFull a : articles) {
                            if (a != null && a.название != null) {
                                articlesList.add(a);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // Обновляем заголовок с количеством статей
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(sourceTitle + " (" + articlesList.size() + ")");
                        }

                        Log.d("ARTICLE_LIST", "✅ UI обновлён: " + articlesList.size() + " статей");

                        if (articlesList.isEmpty()) {
                            Toast.makeText(this, "Статьи не найдены", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Не удалось загрузить статьи", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e("ARTICLE_LIST", "Ошибка: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                // 🔥 Скрываем индикатор и сбрасываем флаг
                mainHandler.post(() -> {
                    swipeRefresh.setRefreshing(false);
                    isLoading = false;  // 🔥 Разрешаем новые запросы
                    lastRefreshTime = System.currentTimeMillis();  // 🔥 Обновляем время
                });
            }
        });
    }



    private void onArticleClick(ArticleFull article) {
        if (article == null || article.название == null) {
            Toast.makeText(this, "Ошибка: статья некорректна", Toast.LENGTH_SHORT).show();
            return;
        }


        Log.d("ARTICLE_CLICK", "Клик: " + article.название);

        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("source_title", sourceTitle);  // 🔥 ДОБАВИТЬ ЭТУ СТРОКУ
        intent.putExtra("article_title", article.название);

        // Плавный переход
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}