package com.example.lawapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.adapters.ArticleAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.GptApiClient;
import com.example.lawapp.api.GptApiService;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.models.SmartSearchRequest;
import com.example.lawapp.models.SmartSearchResponse;
import com.example.lawapp.utils.DataPrefetcher;
import com.example.lawapp.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Главный экран приложения
 * Оптимизированный поиск, навигация и UI
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Elements
    private EditText articleNumberEditText;
    private EditText searchEditText;
    private Button findArticlesButton;
    private Button cancelSearchButton;
    private Button searchButton;
    private GptApiService gptApiService;
    private ProgressBar progressBar;
    private Button codeksButton;
    private Button lawsButton;
    private Button favoritesButton;
    private Button notesButton;
    private Button historyButton;
    private Button aboutButton;
    private RecyclerView articlesRecyclerView;
    private View offlineIndicator;

    // Adapters
    private ArticleAdapter articleAdapter;

    // Data (используем ArrayList с начальной емкостью)
    static final List<ArticleFull> articlesFull = new ArrayList<>(1000);
    private final List<ArticleFull> currentArticles = new ArrayList<>();

    // API & Threading
    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupUI();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOfflineIndicator();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }


    private void initializeComponents() {
        CacheManager.initialize(this);
        DataPrefetcher.getInstance(this).prefetchEssentials();
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        gptApiService = GptApiClient.getService();

        // progressBar пока не ищем, он будет найден в initViews
        // Button openChatButton пока не ищем
    }

    private void initViews() {
        articleNumberEditText = findViewById(R.id.articleNumberEditText);
        searchEditText = findViewById(R.id.searchEditText);
        findArticlesButton = findViewById(R.id.findArticlesButton);
        cancelSearchButton = findViewById(R.id.cancelSearchButton);
        searchButton = findViewById(R.id.searchButton);
        codeksButton = findViewById(R.id.codeksButton);
        lawsButton = findViewById(R.id.lawsButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        notesButton = findViewById(R.id.notesButton);
        historyButton = findViewById(R.id.historyButton);
        aboutButton = findViewById(R.id.aboutButton);
        offlineIndicator = findViewById(R.id.offlineIndicator);
        articlesRecyclerView = findViewById(R.id.articlesRecyclerView);

        // 🔥 ПЕРЕНЕСЛИ СЮДА:
        progressBar = findViewById(R.id.progressBar);
        Button openChatButton = findViewById(R.id.openChatButton);

        openChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });
    }

    private void setupUI() {
        initViews();
        setupRecyclerView();
        setupClickListeners();
        updateOfflineIndicator();
    }

    private void searchByGPT(String problem) {
        if (problem.isEmpty()) {
            Toast.makeText(this, "Введите описание проблемы", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        SmartSearchRequest request = new SmartSearchRequest(problem);

        gptApiService.smartSearch(request).enqueue(new Callback<SmartSearchResponse>() {
            @Override
            public void onResponse(Call<SmartSearchResponse> call, Response<SmartSearchResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    SmartSearchResponse body = response.body();
                    if (body.isSuccess()) {
                        // Покажи результат
                        showGPTResult(body.getResult());
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Ошибка: " + body.getError(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SmartSearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        "Нет соединения: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("GPT_ERROR", "Failed", t);
            }
        });
    }

    private List<ArticleFull> searchArticlesLocallyByNumber(int number) {
        List<ArticleFull> results = new ArrayList<>();
        for (ArticleFull article : articlesFull) {
            if (isValidArticle(article) && extractNumberFromTitle(article.название) == number) {
                results.add(article);
            }
        }
        return results;
    }

    private void setupRecyclerView() {
        // ОПТИМИЗАЦИЯ: Отключаем лишние аллокации
        articlesRecyclerView.setHasFixedSize(true);
        articlesRecyclerView.setItemAnimator(null);

        articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(currentArticles, this::onArticleClick);
        articlesRecyclerView.setAdapter(articleAdapter);
    }

    private void showGPTResult(String result) {
        // Создай диалог или новый Activity для показа результата
        new AlertDialog.Builder(this)
                .setTitle("AI Помощник")
                .setMessage(result)
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupClickListeners() {
        // Поиск
        findArticlesButton.setOnClickListener(v -> searchByNumber());
        cancelSearchButton.setOnClickListener(v -> cancelSearch());
        searchButton.setOnClickListener(v -> searchByText());

        // Навигация (единый метод вместо 8 лямбд)
        codeksButton.setOnClickListener(v -> navigateTo(CodeksListActivity.class));
        lawsButton.setOnClickListener(v -> navigateTo(LawsListActivity.class));
        favoritesButton.setOnClickListener(v -> navigateTo(FavoritesActivity.class));
        notesButton.setOnClickListener(v -> navigateTo(NotesActivity.class));
        historyButton.setOnClickListener(v -> navigateTo(HistoryActivity.class));
        aboutButton.setOnClickListener(v -> navigateTo(AboutActivity.class));
    }


    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(MainActivity.this, activityClass);
        startActivity(intent);
        // Мгновенный переход без анимации (экономит ресурсы)
        overridePendingTransition(0, 0);
    }


    private void loadData() {
        loadArticlesForSearch();
    }

    private void loadArticlesForSearch() {
        executor.execute(() -> {
            try {
                List<ArticleFull> newArticles = CacheManager.getData(
                        "/api/articles_full",
                        "articles_full.cache.json",
                        apiService.getArticlesFull(),
                        false
                );

                if (newArticles != null) {
                    mainHandler.post(() -> {
                        articlesFull.clear();
                        articlesFull.addAll(newArticles);
                        Log.d(TAG, "Загружено статей для поиска: " + articlesFull.size());
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки статей: " + e.getMessage(), e);
            }
        });
    }


    private void searchByNumber() {
        String text = articleNumberEditText.getText().toString().trim();
        if (text.isEmpty()) {
            showShortToast("Введите номер статьи");
            return;
        }

        try {
            int number = Integer.parseInt(text);

            // 🔥 ПРОВЕРКА: Если база пуста или поиск локальный не дал результата, идем в сеть
            // Но делаем это В ФОНОВОМ ПОТОКЕ!
            executor.execute(() -> {
                List<ArticleFull> results;

                // Сначала пробуем найти локально (если база загружена)
                if (!articlesFull.isEmpty()) {
                    results = searchArticlesLocallyByNumber(number);
                    if (!results.isEmpty()) {
                        showSearchResults(results);
                        return;
                    }
                }

                // Если локально не нашли или база пуста -> идем на сервер
                results = searchOnServerByNumber(number);
                showSearchResults(results);
            });

        } catch (NumberFormatException e) {
            showShortToast("Введите корректный номер");
        }
    }

    private void searchByText() {
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            showShortToast("Введите текст для поиска");
            return;
        }

        //  ЗАПУСКАЕМ В ФОНОВОМ ПОТОКЕ
        executor.execute(() -> {
            List<ArticleFull> results;

            // Сначала пробуем найти локально
            if (!articlesFull.isEmpty()) {
                results = searchLocallyByText(query);
                if (!results.isEmpty()) {
                    showSearchResults(results);
                    return;
                }
            }

            // Если локально не нашли -> идем на сервер
            results = searchOnServerByText(query);
            showSearchResults(results);
        });
    }

    private List<ArticleFull> searchArticlesByNumber(int number) {
        // Если база еще не загружена, сразу идем на сервер
        if (articlesFull.isEmpty()) {
            Log.d(TAG, "База пуста, ищем на сервере...");
            return searchOnServerByNumber(number);
        }

        List<ArticleFull> results = new ArrayList<>();
        for (ArticleFull article : articlesFull) {
            if (isValidArticle(article) && extractNumberFromTitle(article.название) == number) {
                results.add(article);
            }
        }

        // Если локально ничего не нашли, пробуем сервер (опционально)
        if (results.isEmpty()) {
            return searchOnServerByNumber(number);
        }

        return results;
    }

    private List<ArticleFull> searchArticlesByText(String query) {
        if (articlesFull.isEmpty()) {
            Log.d(TAG, "База пуста, ищем на сервере...");
            return searchOnServerByText(query);
        }

        List<ArticleFull> results = searchLocallyByText(query);

        if (results.isEmpty()) {
            return searchOnServerByText(query);
        }

        return results;
    }

    private List<ArticleFull> searchLocallyByText(String query) {
        List<ArticleFull> results = new ArrayList<>();
        String[] words = query.toLowerCase().split("\\s+");

        for (ArticleFull article : articlesFull) {
            if (!isValidArticle(article)) continue;

            String title = article.название.toLowerCase();
            for (String word : words) {
                if (title.contains(word)) {
                    results.add(article);
                    break;
                }
            }
        }
        return results;
    }

    private List<ArticleFull> searchOnServerByNumber(int number) {
        try {
            // Этот вызов .execute() теперь безопасен, так как мы внутри executor.execute()
            Response<List<ArticleFull>> response = apiService.searchByNumber(number).execute();
            if (response.body() == null) {
                Log.w(TAG, "Сервер вернул null при поиске по номеру");
                return new ArrayList<>();
            }
            return response.body();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка поиска по номеру: " + e.getMessage(), e);
            // Важно: показываем ошибку в UI потоке
            runOnUiThread(() -> showShortToast("Ошибка сети при поиске"));
            return new ArrayList<>();
        }
    }

    private List<ArticleFull> searchOnServerByText(String query) {
        try {
            Response<List<ArticleFull>> response = apiService.searchByText(query).execute();
            if (response.body() == null) {
                Log.w(TAG, "Сервер вернул null при поиске по тексту");
                return new ArrayList<>();
            }
            return response.body();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка поиска по тексту: " + e.getMessage(), e);
            runOnUiThread(() -> showShortToast("Ошибка сети при поиске"));
            return new ArrayList<>();
        }
    }


    private void showSearchResults(List<ArticleFull> results) {
        mainHandler.post(() -> {
            currentArticles.clear();

            // 🔥 ГАРАНТИЯ: Если results null, используем пустой список
            if (results != null) {
                currentArticles.addAll(results);
            }

            articleAdapter.notifyDataSetChanged();

            cancelSearchButton.setVisibility(View.VISIBLE);
            codeksButton.setVisibility(View.GONE);
            lawsButton.setVisibility(View.GONE);

            showSearchNotification(currentArticles.size());
            Log.d(TAG, "Показано результатов: " + currentArticles.size());
        });
    }

    private void showSearchNotification(int count) {
        String message = count > 0
                ? "Найдено статей: " + count
                : "По вашему запросу ничего не найдено";
        showCustomToast(message);
    }

    // Оптимизированный Toast (кэширование View)
    private void showCustomToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        View view = toast.getView();
        if (view != null) {
            TextView text = view.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextSize(16);
                text.setTextColor(Color.WHITE);
                view.setBackgroundColor(Color.parseColor("#DD333333"));
                view.setPadding(40, 30, 40, 30);
            }
        }
        toast.show();
    }

    private void showShortToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateOfflineIndicator() {
        boolean isOnline = NetworkUtils.isOnline(this);
        offlineIndicator.setVisibility(isOnline ? View.GONE : View.VISIBLE);
    }


    private void onArticleClick(ArticleFull article) {
        if (!isValidArticle(article)) {
            showShortToast("Ошибка: статья некорректна");
            return;
        }

        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void cancelSearch() {
        articleNumberEditText.setText("");
        searchEditText.setText("");
        currentArticles.clear();
        articleAdapter.setSearchQuery("");
        articleAdapter.notifyDataSetChanged();

        cancelSearchButton.setVisibility(View.GONE);
        codeksButton.setVisibility(View.VISIBLE);
        lawsButton.setVisibility(View.VISIBLE);
    }


    private boolean isValidArticle(ArticleFull article) {
        return article != null && article.название != null;
    }

    private int extractNumberFromTitle(String title) {
        if (title == null || title.isEmpty()) return -1;

        //Оптимизация: используем StringBuilder с начальной емкостью
        StringBuilder digits = new StringBuilder(10);
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }

        try {
            String num = digits.toString();
            return num.isEmpty() ? -1 : Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}