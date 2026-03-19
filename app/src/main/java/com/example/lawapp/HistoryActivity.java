package com.example.lawapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.adapters.ArticleAdapter;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.utils.HistoryManager;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<ArticleFull> historyList = new ArrayList<>();

    private HistoryManager historyManager;
    private TextView emptyText;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📜 История просмотров");
        }

        historyManager = new HistoryManager(this);

        // UI
        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);
        clearButton = findViewById(R.id.clearButton);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(historyList, this::onArticleClick);
        recyclerView.setAdapter(adapter);

        // Кнопка "Очистить историю"
        clearButton.setOnClickListener(v -> {
            historyManager.clearHistory();
            loadHistory();
            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
        });

        // Загрузка истории
        loadHistory();
    }

    private void loadHistory() {
        historyList.clear();
        List<HistoryManager.HistoryItem> history = historyManager.getHistorySorted();

        for (HistoryManager.HistoryItem item : history) {
            ArticleFull article = new ArticleFull();
            article.название = item.articleTitle;
            article.номерИсточника = item.source;
            historyList.add(article);
        }

        adapter.notifyDataSetChanged();

        // Показать/скрыть пустой экран
        if (historyList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
        }
    }

    private void onArticleClick(ArticleFull article) {
        if (article == null || article.название == null) return;

        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();  // Обновить при возврате в активность
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}