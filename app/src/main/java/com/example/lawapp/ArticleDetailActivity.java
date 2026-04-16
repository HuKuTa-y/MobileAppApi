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
    import com.example.lawapp.utils.NetworkUtils;
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
            // Финальная копия контекста
            final Context context = ArticleDetailActivity.this;

            // Обертка для результата, чтобы избежать ошибки final
            final TextArticle[] resultHolder = new TextArticle[1];
            final String[] errorHolder = new String[1];

            mainHandler.post(() -> textViewContent.setText("")); // Очистка UI

            executor.execute(() -> {
                try {
                    Log.d("ARTICLE_DETAIL", "🚀 Старт загрузки: " + articleName);

                    // 1. RAM Кэш
                    String cachedInRam = CacheManager.getTextFromMemoryCache(articleName);
                    if (cachedInRam != null) {
                        Log.d("ARTICLE_DETAIL", " Текст из RAM");
                        TextArticle ramArticle = new TextArticle();
                        ramArticle.название = articleName;
                        ramArticle.контент = cachedInRam;
                        resultHolder[0] = ramArticle;
                    } else {
                        // 2. Диск
                        TextArticle diskText = CacheManager.getTextFromCache(context, articleName);
                        if (diskText != null && diskText.контент != null) {
                            Log.d("ARTICLE_DETAIL", "💾 Текст с диска");
                            resultHolder[0] = diskText;
                            CacheManager.putTextInMemoryCache(articleName, diskText.контент);
                        } else {
                            // 3. Сеть
                            if (NetworkUtils.isOnline(context)) {
                                Log.d("ARTICLE_DETAIL", "🌐 Загрузка из сети...");
                                TextArticle networkText = apiService.getArticleText(articleName).execute().body();

                                if (networkText != null && networkText.контент != null) {
                                    Log.d("ARTICLE_DETAIL", "✅ Получено из сети");
                                    CacheManager.putTextInMemoryCache(articleName, networkText.контент);
                                    CacheManager.saveTextToCache(context, articleName, networkText.контент);
                                    resultHolder[0] = networkText;
                                } else {
                                    errorHolder[0] = "Пустой ответ от сервера";
                                }
                            } else {
                                errorHolder[0] = "Нет интернета и текст не загружен ранее";
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("ARTICLE_DETAIL", "Ошибка: " + e.getMessage(), e);
                    errorHolder[0] = "Ошибка: " + e.getMessage();
                }

                // Обновление UI (здесь мы используем финальные массивы)
                mainHandler.post(() -> {
                    if (resultHolder[0] != null) {
                        textViewContent.setText(resultHolder[0].контент);
                        historyManager.addViewedArticle(currentArticleTitle, sourceTitle != null ? sourceTitle : "");
                        Log.d("ARTICLE_DETAIL", "✨ Текст отображен");
                    } else {
                        String errorMsg = (errorHolder[0] != null) ? errorHolder[0] : "Неизвестная ошибка";
                        textViewContent.setText(errorMsg);
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
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