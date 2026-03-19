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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.adapters.ArticleAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private EditText articleNumberEditText, searchEditText;
    private Button findArticlesButton, cancelSearchButton, searchButton;
    private Button codeksButton, lawsButton;
    private RecyclerView articlesRecyclerView;
    private Button favoritesButton, notesButton, historyButton;

    // Adapters (только для поиска)
    private ArticleAdapter articleAdapter;

    // Data
    private List<ArticleFull> articlesFull = new ArrayList<>();
    private List<ArticleFull> currentArticles = new ArrayList<>();

    // API & Threading
    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CacheManager.initialize(this);
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadArticlesFullForSearch();
    }

    private void initViews() {
        articleNumberEditText = findViewById(R.id.articleNumberEditText);
        searchEditText = findViewById(R.id.searchEditText);
        findArticlesButton = findViewById(R.id.findArticlesButton);
        cancelSearchButton = findViewById(R.id.cancelSearchButton);
        searchButton = findViewById(R.id.searchButton);

        favoritesButton = findViewById(R.id.favoritesButton);
        notesButton = findViewById(R.id.notesButton);
        historyButton = findViewById(R.id.historyButton);
        codeksButton = findViewById(R.id.codeksButton);
        lawsButton = findViewById(R.id.lawsButton);
        articlesRecyclerView = findViewById(R.id.articlesRecyclerView);
    }



    private void setupRecyclerViews() {
        articleAdapter = new ArticleAdapter(currentArticles, this::onArticleClick);
        articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        articlesRecyclerView.setAdapter(articleAdapter);
    }

    private void setupClickListeners() {

        findArticlesButton.setOnClickListener(v -> searchByNumber());
        cancelSearchButton.setOnClickListener(v -> cancelSearch());
        searchButton.setOnClickListener(v -> searchByText());

        codeksButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CodeksListActivity.class);
            startActivity(intent);

        });


        lawsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LawsListActivity.class);
            startActivity(intent);
        });
        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });
        notesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(intent);
        });
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

    }

    private void loadArticlesFullForSearch() {
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
                        Log.d("SEARCH_INIT", "Загружено статей: " + articlesFull.size());
                    });
                }
            } catch (Exception e) {
                Log.e("SEARCH_INIT", "Ошибка: " + e.getMessage(), e);
            }
        });
    }

    private void onArticleClick(ArticleFull article) {
        if (article == null || article.название == null) return;

        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        startActivity(intent);
    }

    // 🔹 Поиск по номеру
    private void searchByNumber() {
        String text = articleNumberEditText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите номер статьи", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int number = Integer.parseInt(text);

            if (!articlesFull.isEmpty()) {
                List<ArticleFull> results = new ArrayList<>();
                for (ArticleFull a : articlesFull) {
                    if (a == null || a.название == null) continue;
                    if (extractNumberFromTitle(a.название) == number) {
                        results.add(a);
                    }
                }
                // 🔥 Передаем запрос для подсветки (хотя для номера это менее важно)
                articleAdapter.setSearchQuery(text);
                showSearchResults(results);
                return;
            }

            // ... остальной код поиска на сервере ...
            executor.execute(() -> {
                try {
                    retrofit2.Response<List<ArticleFull>> response = apiService.searchByNumber(number).execute();
                    List<ArticleFull> results = response.body();
                    articleAdapter.setSearchQuery(text); // 🔥 Важно!
                    showSearchResults(results != null ? results : new ArrayList<>());
                } catch (Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите число", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔹 Поиск по тексту
    private void searchByText() {
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!articlesFull.isEmpty()) {
            String[] words = query.toLowerCase().split("\\s+");
            List<ArticleFull> results = new ArrayList<>();

            for (ArticleFull a : articlesFull) {
                if (a == null || a.название == null) continue;
                String title = a.название.toLowerCase();
                for (String word : words) {
                    if (title.contains(word)) {
                        results.add(a);
                        break;
                    }
                }
            }
            // 🔥 ПЕРЕДАЕМ ЗАПРОС В АДАПТЕР
            articleAdapter.setSearchQuery(query);
            showSearchResults(results);
            return;
        }

        executor.execute(() -> {
            try {
                retrofit2.Response<List<ArticleFull>> response = apiService.searchByText(query).execute();
                List<ArticleFull> results = response.body();
                articleAdapter.setSearchQuery(query); // 🔥 Важно!
                showSearchResults(results != null ? results : new ArrayList<>());
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    // 🔹 Отображение результатов (СКРЫВАЕМ кнопки)
    // 🔹 Отображение результатов поиска + УВЕДОМЛЕНИЕ
    private void showSearchResults(List<ArticleFull> results) {
        mainHandler.post(() -> {
            currentArticles.clear();
            if (results != null) currentArticles.addAll(results);
            articleAdapter.notifyDataSetChanged();

            // Показать кнопку отмены
            cancelSearchButton.setVisibility(View.VISIBLE);

            // Скрыть кнопки Кодексы и Законы
            codeksButton.setVisibility(View.GONE);
            lawsButton.setVisibility(View.GONE);

            // 🔥 УВЕДОМЛЕНИЕ О РЕЗУЛЬТАТАХ ПОИСКА
            int count = currentArticles.size();
            String query = searchEditText.getText().toString().trim();

            if (count > 0) {
                showSearchNotification("Найдено статей: " + count);
            } else {
                showSearchNotification("По вашему запросу ничего не найдено");
            }

            Log.d("SEARCH", "Показано результатов: " + count);
        });
    }

    // 🔥 Метод для показа плавного уведомления
    private void showSearchNotification(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);

        // 🔥 Получаем TextView внутри Toast для кастомизации
        View view = toast.getView();
        if (view != null) {
            TextView text = view.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextSize(16);
                text.setTextColor(Color.WHITE);
                view.setBackgroundColor(Color.parseColor("#DD333333")); // Тёмный полупрозрачный фон
                view.setPadding(40, 30, 40, 30);
            }
        }

        // 🔥 Показываем с анимацией появления/исчезновения
        toast.show();
    }

    // 🔹 Отмена поиска (ВОЗВРАЩАЕМ кнопки)
    private void cancelSearch() {
        articleNumberEditText.setText("");
        searchEditText.setText("");
        currentArticles.clear();

        // 🔥 Очищаем запрос поиска, чтобы подсветка исчезла
        articleAdapter.setSearchQuery("");

        articleAdapter.notifyDataSetChanged();
        cancelSearchButton.setVisibility(View.GONE);
        codeksButton.setVisibility(View.VISIBLE);
        lawsButton.setVisibility(View.VISIBLE);
    }

    private int extractNumberFromTitle(String title) {
        if (title == null || title.isEmpty()) return -1;
        StringBuilder digits = new StringBuilder();
        for (char c : title.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        try {
            String num = digits.toString();
            return num.isEmpty() ? -1 : Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}