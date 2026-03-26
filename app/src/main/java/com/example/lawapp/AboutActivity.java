package com.example.lawapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lawapp.cache.CacheManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    }

    private void loadAppInfo() {
        // 🔹 Версия приложения (через PackageManager — работает всегда!)
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

        // 🔹 Дата последнего обновления базы (из кэша)
        long lastUpdateTime = CacheManager.getLastUpdateTime();
        if (lastUpdateTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(lastUpdateTime));
            lastUpdateText.setText("📅 База обновлена: " + dateStr);
        } else {
            lastUpdateText.setText("📅 База обновлена: никогда");
        }

        // 🔹 Источник данных
        sourceText.setText("📚 Источник: Законодательство РФ\n🌐 Данные предоставляются через API");

        // 🔹 Контакты
        contactText.setText("📧 support@lawapp.example.com\n💬 Нажмите, чтобы связаться");
    }

    private void checkForUpdates() {
        // 🔥 Имитация проверки обновлений (в реальности — запрос к серверу)
        checkUpdatesButton.setEnabled(false);
        checkUpdatesButton.setText("⏳ Проверка...");

        // В реальном проекте здесь будет запрос к API
        // Например: apiService.checkForUpdates().enqueue(...)

        // Для демонстрации — просто показываем сообщение
        new Handler().postDelayed(() -> {
            checkUpdatesButton.setEnabled(true);
            checkUpdatesButton.setText("🔄 Проверить обновления");

            // 🔥 Здесь можно вызвать реальную синхронизацию
            Toast.makeText(this, "✅ База актуальна", Toast.LENGTH_SHORT).show();

            // Обновляем дату
            loadAppInfo();
        }, 1500);
    }

    private void sendEmail() {
        // 🔥 Получаем версию через PackageManager (вместо BuildConfig)
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