package com.example.lawapp;

import android.content.res.ColorStateList;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageButton;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.models.TextArticle;
import com.example.lawapp.utils.FavoritesManager;
import com.example.lawapp.utils.HistoryManager;
import com.example.lawapp.utils.NotesManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleDetailActivity extends AppCompatActivity {

    private TextView textViewTitle, textViewContent;
    private LawApiService apiService;
    private ExecutorService executor;
    private String sourceTitle;
    private Handler mainHandler;
    private HistoryManager historyManager;
    private ImageButton copyCitationButton;
    private Button favoriteButton, noteButton;
    private NotesManager notesManager;
    private FavoritesManager favoritesManager;
    private String currentArticleTitle;  // 🔥 Будет установлен после получения из Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Статья");
        }

        // Инициализация View
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewContent = findViewById(R.id.textViewContent);
        copyCitationButton = findViewById(R.id.copyCitationButton);
        copyCitationButton.setOnClickListener(v -> copyArticleCitation());
        favoriteButton = findViewById(R.id.favoriteButton);

        // Инициализация логики
        favoritesManager = new FavoritesManager(this);
        historyManager = new HistoryManager(this);
        notesManager = new NotesManager(this);
        sourceTitle = getIntent().getStringExtra("source_title");
        noteButton = findViewById(R.id.noteButton);
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // 🔥 Получаем название статьи из Intent
        String articleTitle = getIntent().getStringExtra("article_title");
        Log.d("ARTICLE_DETAIL", "Получено название: " + articleTitle);

        if (articleTitle != null) {
            // 🔥 Устанавливаем currentArticleTitle ПОСЛЕ получения articleTitle
            currentArticleTitle = articleTitle;

            textViewTitle.setText(articleTitle);

            // 🔥 Обновляем кнопку избранного ПОСЛЕ установки currentArticleTitle
            updateFavoriteButton();
            updateNoteButton();  // Обновляем кнопку заметок

            // Обработчик клика по кнопке
            favoriteButton.setOnClickListener(v -> toggleFavorite());
            noteButton.setOnClickListener(v -> showNoteDialog());

            // Загружаем текст статьи
            loadArticleText(articleTitle);
        } else {
            Toast.makeText(this, "Ошибка: название статьи не передано", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateNoteButton() {
        if (currentArticleTitle == null) return;

        if (notesManager.hasNote(currentArticleTitle)) {
            noteButton.setText("Редактировать заметку");
            noteButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        } else {
            noteButton.setText("Сделать заметку");
            noteButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C4DFF")));
        }
    }

    private void updateFavoriteButton() {
        if (currentArticleTitle == null) return;

        if (favoritesManager.isFavorite(currentArticleTitle)) {
            favoriteButton.setText("⭐ В избранном");
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
        } else {
            favoriteButton.setText("⭐ Добавить в избранное");
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C4DFF")));
        }
    }

    private void showNoteDialog() {
        if (currentArticleTitle == null) return;

        String currentNote = notesManager.getNote(currentArticleTitle);

        // Диалог для ввода заметки
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Пометка к статье");

        final EditText input = new EditText(this);
        input.setHint("Введите ваш комментарий...");
        input.setText(currentNote != null ? currentNote : "");
        input.setMinLines(5);
        input.setGravity(android.view.Gravity.TOP);

        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String noteText = input.getText().toString().trim();
            notesManager.addOrUpdateNote(currentArticleTitle, noteText);
            updateNoteButton();
            Toast.makeText(this, "Пометка сохранена", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("🗑Удалить", (dialog, which) -> {
            notesManager.removeNote(currentArticleTitle);
            updateNoteButton();
            Toast.makeText(this, "Пометка удалена", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Отмена", null);

        builder.show();
    }

    private void toggleFavorite() {
        if (currentArticleTitle == null) return;

        if (favoritesManager.isFavorite(currentArticleTitle)) {
            favoritesManager.removeFavorite(currentArticleTitle);
            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
        } else {
            ArticleFull article = new ArticleFull();
            article.название = currentArticleTitle;
            favoritesManager.addFavorite(article);
            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
        }
        updateFavoriteButton();
    }

    private void loadArticleText(String articleName) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(articleName, StandardCharsets.UTF_8.toString());
                Log.d("ARTICLE_DETAIL", "Запрос: /api/article/text?article_name=" + encoded);

                TextArticle text = CacheManager.getData(
                        "/api/article/text?article_name=" + encoded,
                        "text_" + encoded + ".cache.json",
                        apiService.getArticleText(articleName),
                        false
                );

                Log.d("ARTICLE_DETAIL", "Ответ API: " + (text != null ? "OK" : "NULL"));

                if (text != null && text.контент != null) {
                    mainHandler.post(() -> {
                        textViewContent.setText(text.контент);

                        historyManager.addViewedArticle(currentArticleTitle, sourceTitle != null ? sourceTitle : "");

                        Log.d("ARTICLE_DETAIL", "✅ Текст отображён");
                    });
                } else {
                    mainHandler.post(() -> {
                        textViewContent.setText("Текст статьи не найден");
                        Toast.makeText(ArticleDetailActivity.this, "Текст не загружен", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e("ARTICLE_DETAIL", "Ошибка: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    textViewContent.setText("Ошибка: " + e.getMessage());
                    Toast.makeText(ArticleDetailActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void copyArticleCitation() {
        if (currentArticleTitle == null) return;

        // Формируем текст для копирования
        String citation = "📜 " + currentArticleTitle +
                (sourceTitle != null ? " (" + sourceTitle + ")" : "");

        // Копируем в буфер обмена
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("article_citation", citation);
        clipboard.setPrimaryClip(clip);

        // Показываем уведомление
        Toast.makeText(this, "📋 Скопировано: " + citation, Toast.LENGTH_LONG).show();

        // 🔥 Вибрация для тактильной отдачи (опционально)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Vibrator vibrator = (Vibrator) getSystemService(Vibrator.class);
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}