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
import com.example.lawapp.utils.OfflineException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleListActivity extends AppCompatActivity {

    private static final String TAG = "ArticleListActivity";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        sourceNumber = getIntent().getStringExtra("source_number");
        sourceTitle = getIntent().getStringExtra("source_title");

        Log.d(TAG, " Источник: " + sourceNumber + " — " + sourceTitle);

        if (sourceNumber == null || sourceNumber.isEmpty()) {
            Toast.makeText(this, "Ошибка: не указан кодекс", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeComponents();
        setupUI();
        loadArticles();
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
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(articlesList, this::onArticleClick);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadArticles);
        swipeRefresh.setColorSchemeResources(R.color.purple_500, R.color.teal_200, R.color.purple_700);
    }

    private void loadArticles() {
        if (isLoading) return;
        isLoading = true;
        showLoadingIndicator();
        executor.execute(this::fetchArticlesFromSource);
    }

    private void showLoadingIndicator() {
        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
    }

    private void fetchArticlesFromSource() {
        try {
            if (sourceNumber == null) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка источника", Toast.LENGTH_LONG).show());
                hideLoadingIndicator();
                return;
            }

            // Формируем имя файла
            String encoded = URLEncoder.encode(sourceNumber, StandardCharsets.UTF_8.toString());
            String cacheFileName = "articles_" + encoded + ".cache.json";

            File baseCacheDir = getApplicationContext().getCacheDir();
            File lawAppCacheDir = new File(baseCacheDir, "LawApp_Cache");
            File targetFile = new File(lawAppCacheDir, cacheFileName);

            Log.d(TAG, "🔍 Ищу файл: " + cacheFileName);
            Log.d(TAG, "Путь: " + targetFile.getAbsolutePath());
            Log.d(TAG, "Существует? " + targetFile.exists());

            List<ArticleFull> articles = null;

            if (targetFile.exists()) {
                Log.d(TAG, "✅ Читаю из кэша...");
                try (FileReader reader = new FileReader(targetFile)) {
                    Gson gson = new Gson();
                    articles = gson.fromJson(reader, new TypeToken<List<ArticleFull>>(){}.getType());
                }
            } else {
                if (NetworkUtils.isOnline(this)) {
                    Log.d(TAG, " Качаю из сети...");
                    // Используем стандартный метод CacheManager для загрузки и сохранения
                    articles = CacheManager.getData(
                            "/api/articles/by-source?source_number=" + encoded,
                            cacheFileName,
                            apiService.getArticlesBySource(sourceNumber),
                            false
                    );
                } else {
                    throw new OfflineException("Нет интернета и нет кэша");
                }
            }

            onArticlesLoaded(articles);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } finally {
            hideLoadingIndicator();
        }
    }

    private void onArticlesLoaded(List<ArticleFull> articles) {
        mainHandler.post(() -> {
            articlesList.clear();
            if (articles != null) {
                for (ArticleFull article : articles) {
                    if (article.название != null) {
                        articlesList.add(article);
                        MemoryCache.getInstance().putArticle(article.название, article);
                    }
                }
            }
            adapter.notifyDataSetChanged();
            updateTitleWithCount();
            if (articlesList.isEmpty()) Toast.makeText(this, "Статьи не найдены", Toast.LENGTH_SHORT).show();
        });
    }

    private void hideLoadingIndicator() {
        mainHandler.post(() -> {
            swipeRefresh.setRefreshing(false);
            isLoading = false;
            updateOfflineIndicator();
        });
    }

    private void updateTitleWithCount() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(sourceTitle + " (" + articlesList.size() + ")");
        }
    }

    private void updateOfflineIndicator() {
        boolean isOnline = NetworkUtils.isOnline(this);
        offlineIndicator.setVisibility(isOnline ? View.GONE : View.VISIBLE);
    }

    private void onArticleClick(ArticleFull article) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        intent.putExtra("source_title", sourceTitle);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}