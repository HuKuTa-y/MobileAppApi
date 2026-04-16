package com.example.lawapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lawapp.api.ApiClient;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.models.Codek;
import com.example.lawapp.models.Law;
import com.example.lawapp.models.TextArticle;
import com.example.lawapp.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AboutActivity extends AppCompatActivity {

    private TextView versionText, lastUpdateText;
    private Button checkUpdatesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ℹ️ О приложении");
        }

        versionText = findViewById(R.id.versionText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        checkUpdatesButton = findViewById(R.id.checkUpdatesButton);

        loadAppInfo();
        checkUpdatesButton.setOnClickListener(v -> checkForUpdates());

        Button downloadAllButton = findViewById(R.id.downloadAllButton);
        Button clearCacheButton = findViewById(R.id.clearCacheButton);

        downloadAllButton.setOnClickListener(v -> {
            if (!NetworkUtils.isOnline(this)) {
                Toast.makeText(this, "Нет интернета!", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Полная загрузка базы")
                    .setMessage("Загрузить списки статей И их полные тексты? Это займет время, но позволит работать полностью без интернета.")
                    .setPositiveButton("Да, скачать всё", (dialog, which) -> startFullDownload())
                    .setNegativeButton("Только списки", (dialog, which) -> startListsOnlyDownload())
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        clearCacheButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Очистить кэш")
                    .setMessage("Удалить все загруженные данные?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        CacheManager.clearAllCache(this);
                        Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show();
                        recreate();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    // Загрузка ТОЛЬКО списков (старая версия, если нужно)
    private void startListsOnlyDownload() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Загрузка списков...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Codek> codeks = ApiClient.getService().getCodeks().execute().body();
                List<Law> laws = ApiClient.getService().getLaws().execute().body();

                int total = (codeks != null ? codeks.size() : 0) + (laws != null ? laws.size() : 0);
                int processed = 0;

                if (codeks != null) for (Codek c : codeks) { processed++; downloadListForSource(c.номер, c.название, processed, total, progressDialog, false); }
                if (laws != null) for (Law l : laws) { processed++; downloadListForSource(l.номер, l.название, processed, total, progressDialog, false); }

                runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(this, "Списки готовы!", Toast.LENGTH_LONG).show(); });
            } catch (Exception e) {
                runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        });
    }

    // Загрузка СПИСКОВ + ТЕКСТОВ (НОВАЯ ВЕРСИЯ)
    private void startFullDownload() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Подготовка...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Codek> codeks = ApiClient.getService().getCodeks().execute().body();
                List<Law> laws = ApiClient.getService().getLaws().execute().body();

                if ((codeks == null || codeks.isEmpty()) && (laws == null || laws.isEmpty())) {
                    runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(this, "Не удалось получить списки", Toast.LENGTH_LONG).show(); });
                    return;
                }

                int totalSources = (codeks != null ? codeks.size() : 0) + (laws != null ? laws.size() : 0);
                int processed = 0;

                // Проходим по всем источникам
                if (codeks != null) {
                    for (Codek codek : codeks) {
                        processed++;
                        // true = скачивать и тексты тоже
                        downloadListForSource(codek.номер, codek.название, processed, totalSources, progressDialog, true);
                    }
                }
                if (laws != null) {
                    for (Law law : laws) {
                        processed++;
                        downloadListForSource(law.номер, law.название, processed, totalSources, progressDialog, true);
                    }
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "✅ База полностью загружена! Можно работать офлайн.", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("DOWNLOAD", "Ошибка", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Скачивает список и (опционально) тексты статей
     * @param downloadTexts Если true, скачивает тексты всех статей после сохранения списка
     */
    private void downloadListForSource(String sourceNumber, String sourceName, int current, int total, ProgressDialog dialog, boolean downloadTexts) {
        try {
            runOnUiThread(() -> {
                dialog.setMessage("[" + current + "/" + total + "] " + sourceName + (downloadTexts ? " (с текстами)" : ""));
                dialog.setProgress((current * 100) / total);
            });

            // 1. Скачиваем список статей
            List<ArticleFull> articles = ApiClient.getService()
                    .getArticlesBySource(sourceNumber)
                    .execute()
                    .body();

            if (articles == null || articles.isEmpty()) {
                Log.w("DOWNLOAD", "Список пуст: " + sourceName);
                return;
            }

            // Сохраняем список
            String encoded = java.net.URLEncoder.encode(sourceNumber, java.nio.charset.StandardCharsets.UTF_8.toString());
            String fileName = "articles_" + encoded + ".cache.json";

            CacheManager.saveListToCache(this,
                    "/api/articles/by-source?source_number=" + sourceNumber,
                    fileName,
                    articles
            );
            Log.d("DOWNLOAD", "Список сохранен: " + sourceName);

            // 2. ЕСЛИ НУЖНО, СКАЧИВАЕМ ТЕКСТЫ
            if (downloadTexts) {
                downloadTextsForSource(articles, sourceName);
            }

        } catch (Exception e) {
            Log.e("DOWNLOAD", "Ошибка источника " + sourceName, e);
        }
    }

    /**
     * Параллельно скачивает тексты для списка статей
     */
    private void downloadTextsForSource(List<ArticleFull> articles, String sourceName) {
        // Пул из 10-20 потоков для загрузки текстов (чтобы не перегрузить сервер)
        ExecutorService textExecutor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(articles.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (ArticleFull article : articles) {
            // Пропускаем разделы и главы, у них нет текста
            if (article.название.startsWith("Раздел") || article.название.startsWith("Глава")) {
                latch.countDown();
                continue;
            }

            final String title = article.название;

            textExecutor.execute(() -> {
                try {
                    // Запрос текста
                    TextArticle textData = ApiClient.getService().getArticleText(title).execute().body();

                    if (textData != null && textData.контент != null) {
                        // Сохраняем текст в кэш (используем наш надежный метод с хэшем)
                        CacheManager.saveTextToCache(AboutActivity.this, title, textData.контент);
                        // Кладем в RAM кэш для скорости
                        CacheManager.putTextInMemoryCache(title, textData.контент);
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    Log.e("TEXT_ERR", "Ошибка текста: " + title, e);
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Ждем окончания загрузки всех текстов для этого кодекса
            latch.await();
            textExecutor.shutdown();
            Log.d("DOWNLOAD", "Тексты для '" + sourceName + "' готовы. Успех: " + successCount.get() + ", Ошибок: " + failCount.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadAppInfo() {
        String versionName = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName != null ? pInfo.versionName : "1.0";
        } catch (Exception e) { e.printStackTrace(); }
        versionText.setText("Версия: " + versionName);

        long lastUpdateTime = CacheManager.getLastUpdateTime();
        if (lastUpdateTime > 0) {
            String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(lastUpdateTime));
            lastUpdateText.setText("Обновлено: " + dateStr);
        } else {
            lastUpdateText.setText("Обновлено: никогда");
        }
    }

    private void checkForUpdates() {
        checkUpdatesButton.setEnabled(false);
        checkUpdatesButton.setText("Проверка...");
        new Handler().postDelayed(() -> {
            checkUpdatesButton.setEnabled(true);
            checkUpdatesButton.setText("Проверить обновления");
            Toast.makeText(this, "База актуальна", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}