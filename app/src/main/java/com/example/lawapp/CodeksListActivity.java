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

import com.example.lawapp.adapters.CodekAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.Codek;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CodeksListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CodekAdapter adapter;
    private List<Codek> codeksList = new ArrayList<>();

    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codeks_list);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Кодексы");
        }

        // Инициализация
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CodekAdapter(codeksList, this::onCodekClick);
        recyclerView.setAdapter(adapter);

        // Загрузка
        loadCodeks();
    }

    private void loadCodeks() {
        executor.execute(() -> {
            try {
                Log.d("CODEKS_LOAD", "🔄 Начинаю загрузку кодексов...");

                List<Codek> newCodeks = CacheManager.getData(
                        "/api/codeks",
                        "codeks.cache.json",
                        apiService.getCodeks(),
                        true
                );

                Log.d("CODEKS_LOAD", "📦 Ответ API: " + (newCodeks != null ? newCodeks.size() + " шт." : "NULL"));

                if (newCodeks != null) {
                    mainHandler.post(() -> {
                        codeksList.clear();
                        for (Codek c : newCodeks) {
                            if (c != null && c.название != null) {
                                codeksList.add(c);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("CODEKS_LOAD", "✅ UI обновлён, элементов: " + codeksList.size());

                        if (codeksList.isEmpty()) {
                            Toast.makeText(CodeksListActivity.this, "Список пуст", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("CODEKS_LOAD", "❌ Ошибка: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void onCodekClick(Codek codek) {
        if (codek == null || codek.номер == null) return;

        Log.d("CODEK_CLICK", "Клик: " + codek.название);

        Intent intent = new Intent(this, ArticleListActivity.class);
        intent.putExtra("source_type", "codek");
        intent.putExtra("source_number", codek.номер);
        intent.putExtra("source_title", codek.название);
        startActivity(intent);
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