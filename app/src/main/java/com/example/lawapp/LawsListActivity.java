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

import com.example.lawapp.adapters.LawAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.Law;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LawsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LawAdapter adapter;
    private List<Law> lawsList = new ArrayList<>();

    private LawApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laws_list);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Законы");
        }

        // Инициализация
        apiService = ApiClient.getService();
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LawAdapter(lawsList, this::onLawClick);
        recyclerView.setAdapter(adapter);

        // Загрузка
        loadLaws();
    }

    private void loadLaws() {
        executor.execute(() -> {
            try {
                List<Law> newLaws = CacheManager.getData(
                        "/api/laws",
                        "laws.cache.json",
                        apiService.getLaws(),
                        true
                );

                if (newLaws != null) {
                    mainHandler.post(() -> {
                        lawsList.clear();
                        for (Law l : newLaws) {
                            if (l != null && l.название != null) {
                                lawsList.add(l);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("LAWS_LOAD", "Загружено: " + lawsList.size());
                    });
                }
            } catch (Exception e) {
                Log.e("LAWS_LOAD", "Ошибка: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void onLawClick(Law law) {
        if (law == null || law.номер == null) return;

        Log.d("LAW_CLICK", "Клик: " + law.название);

        Intent intent = new Intent(this, ArticleListActivity.class);
        intent.putExtra("source_type", "law");
        intent.putExtra("source_number", law.номер);
        intent.putExtra("source_title", law.название);
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