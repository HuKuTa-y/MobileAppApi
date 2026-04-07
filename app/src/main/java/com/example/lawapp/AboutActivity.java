package com.example.lawapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.ProgressDialog;
import android.os.AsyncTask;

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
import java.util.Locale;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AboutActivity extends AppCompatActivity {

    private TextView versionText, lastUpdateText, sourceText, contactText;
    private Button checkUpdatesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ℹ️ О приложении");
        }

        // Инициализация View
        versionText = findViewById(R.id.versionText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        sourceText = findViewById(R.id.sourceText);
        contactText = findViewById(R.id.contactText);
        checkUpdatesButton = findViewById(R.id.checkUpdatesButton);

        // 🔥 Заполняем информацию
        loadAppInfo();

        // 🔥 Кнопка проверки обновлений
        checkUpdatesButton.setOnClickListener(v -> {
            checkForUpdates();
        });

        // 🔥 Клик по контактам (открывает почтовый клиент)
        contactText.setOnClickListener(v -> {
            sendEmail();
        });
        Button downloadAllButton = findViewById(R.id.downloadAllButton);
        Button clearCacheButton = findViewById(R.id.clearCacheButton);
        downloadAllButton.setOnClickListener(v -> {
            if (!NetworkUtils.isOnline(this)) {
                Toast.makeText(this, "❌ Нет интернета! Подключитесь к Wi-Fi.", Toast.LENGTH_LONG).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("️ Внимание: Полная загрузка")
                    .setMessage("Это действие загрузит ВСЕ кодексы, законы и тексты всех статей. Это может занять несколько минут и потребовать много места. Продолжить?")
                    .setPositiveButton("Да, скачать всё", (dialog, which) -> startFullDownload())
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // 2. Логика кнопки "Откатить / Очистить"
        clearCacheButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("️ Очистка кэша")
                    .setMessage("Вы уверены? Будут удалены ВСЕ загруженные данные. Приложение перестанет работать без интернета.")
                    .setPositiveButton("Да, удалить всё", (dialog, which) -> {
                        CacheManager.clearAllCache(this);
                        Toast.makeText(this, "✅ Весь кэш очищен!", Toast.LENGTH_SHORT).show();
                        // Перезапуск приложения или обновление UI
                        recreate();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

    }



    private void startFullDownload() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Загрузка данных... Пожалуйста, подождите.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Используем Executor для фоновой задачи
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Скачиваем списки кодексов и законов
                List<Codek> codeks = ApiClient.getService().getCodeks().execute().body();
                List<Law> laws = ApiClient.getService().getLaws().execute().body();

                int totalSources = (codeks != null ? codeks.size() : 0) + (laws != null ? laws.size() : 0);
                int processed = 0;

                // 2. Для каждого источника скачиваем список статей
                if (codeks != null) {
                    for (Codek codek : codeks) {
                        downloadArticlesForSource(codek.номер, progressDialog, totalSources, ++processed);
                    }
                }
                if (laws != null) {
                    for (Law law : laws) {
                        downloadArticlesForSource(law.номер, progressDialog, totalSources, ++processed);
                    }
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(AboutActivity.this, "✅ Все данные успешно загружены!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(AboutActivity.this, "❌ Ошибка при загрузке: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        });
    }

    private void downloadArticlesForSource(String sourceNumber, ProgressDialog dialog, int totalSources, int currentSourceIndex) {
        try {
            // 1. Скачиваем список статей
            List<ArticleFull> articles = ApiClient.getService()
                    .getArticlesBySource(sourceNumber)
                    .execute()
                    .body();

            if (articles == null || articles.isEmpty()) return;

            // Сохраняем список
            CacheManager.saveListToCache(this,
                    "/api/articles/by-source?source_number=" + sourceNumber,
                    "articles_" + sourceNumber + ".cache.json",
                    articles);

            Log.d("FULL_DOWNLOAD", "Список сохранен: " + sourceNumber + " (" + articles.size() + " шт.)");

            // 2. 🔥 ПАРАЛЛЕЛЬНАЯ ЗАГРУЗКА ТЕКСТОВ
            // Создаем пул потоков (например, 10 одновременных запросов)
            ExecutorService textExecutor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(articles.size());
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (ArticleFull article : articles) {
                // Пропускаем разделы/главы
                if (article.название.startsWith("Раздел") || article.название.startsWith("Глава")) {
                    latch.countDown();
                    continue;
                }

                final String title = article.название;

                textExecutor.execute(() -> {
                    try {
                        TextArticle textData = ApiClient.getService()
                                .getArticleText(title)
                                .execute()
                                .body();

                        if (textData != null && textData.контент != null) {
                            CacheManager.saveTextToCache(this, title, textData.контент);
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        Log.e("PARALLEL_ERR", "Ошибка для: " + title, e);
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();

                        // Обновляем прогресс диалога (опционально, может тормозить UI если слишком часто)
                        if (latch.getCount() % 10 == 0) {
                            runOnUiThread(() -> {
                                int progress = (int) ((articles.size() - latch.getCount()) * 100.0 / articles.size());
                                dialog.setMessage("Загрузка текстов: " + progress + "% (" + successCount.get() + "/" + articles.size() + ")");
                                dialog.setProgress(progress);
                            });
                        }
                    }
                });
            }

            // Ждем завершения всех потоков
            latch.await();
            textExecutor.shutdown();

            Log.d("FULL_DOWNLOAD", "Готово! Успех: " + successCount.get() + ", Ошибок: " + failCount.get());

        } catch (Exception e) {
            Log.e("FULL_DOWNLOAD", "Критическая ошибка источника: " + sourceNumber, e);
        }
    }

    private void loadAppInfo() {
        // Версия приложения (через PackageManager — работает всегда!)
        String versionName = "1.0";
        int versionCode = 1;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName != null ? pInfo.versionName : "1.0";
            versionCode = pInfo.versionCode;
        } catch (Exception e) {
            Log.e("ABOUT", "Ошибка получения версии: " + e.getMessage());
        }

        versionText.setText("Версия: " + versionName + " (" + versionCode + ")");

        // Дата последнего обновления базы (из кэша)
        long lastUpdateTime = CacheManager.getLastUpdateTime();
        if (lastUpdateTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(lastUpdateTime));
            lastUpdateText.setText("📅 База обновлена: " + dateStr);
        } else {
            lastUpdateText.setText("📅 База обновлена: никогда");
        }

        // 🔹 Источник данных
        sourceText.setText("Источник: Законодательство РФ\nДанные предоставляются через API");

        // 🔹 Контакты
        contactText.setText("📧 support@lawapp.example.com\nНажмите, чтобы связаться");
    }

    private void checkForUpdates() {
        // Имитация проверки обновлений (в реальности — запрос к серверу)
        checkUpdatesButton.setEnabled(false);
        checkUpdatesButton.setText("⏳ Проверка...");

        // В реальном проекте здесь будет запрос к API
        // Например: apiService.checkForUpdates().enqueue(...)

        // Для демонстрации — просто показываем сообщение
        new Handler().postDelayed(() -> {
            checkUpdatesButton.setEnabled(true);
            checkUpdatesButton.setText("🔄 Проверить обновления");

            // Здесь можно вызвать реальную синхронизацию
            Toast.makeText(this, "✅ База актуальна", Toast.LENGTH_SHORT).show();

            // Обновляем дату
            loadAppInfo();
        }, 1500);
    }

    private void sendEmail() {
        // Получаем версию через PackageManager (вместо BuildConfig)
        String versionName = "1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName != null ? pInfo.versionName : "1.0";
        } catch (Exception e) {
            Log.e("ABOUT", "Ошибка получения версии: " + e.getMessage());
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@lawapp.example.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Вопрос по приложению LawApp");
        intent.putExtra(Intent.EXTRA_TEXT,
                "Здравствуйте!\n\n" +
                        "Версия приложения: " + versionName + "\n" +
                        "Версия Android: " + android.os.Build.VERSION.RELEASE + "\n" +
                        "Модель устройства: " + android.os.Build.MODEL + "\n\n" +
                        "Мой вопрос:\n"
        );

        try {
            startActivity(Intent.createChooser(intent, "Отправить письмо"));
        } catch (Exception e) {
            Toast.makeText(this, "Нет почтового клиента", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}