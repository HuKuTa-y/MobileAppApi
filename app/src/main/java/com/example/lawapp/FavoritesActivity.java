package com.example.lawapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.lawapp.utils.FavoritesManager;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<ArticleFull> favoritesList = new ArrayList<>();

    private FavoritesManager favoritesManager;
    private TextView emptyText;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("⭐Избранное");
        }

        favoritesManager = new FavoritesManager(this);

        // UI
        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);
        clearButton = findViewById(R.id.clearButton);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(favoritesList, this::onArticleClick);
        recyclerView.setAdapter(adapter);

        // Кнопка "Очистить всё"
        clearButton.setOnClickListener(v -> {
            favoritesManager.clearAll();
            loadFavorites();
            Toast.makeText(this, "Избранное очищено", Toast.LENGTH_SHORT).show();
        });

        // Загрузка избранного
        loadFavorites();
    }

    private void loadFavorites() {
        favoritesList.clear();
        List<ArticleFull> favorites = favoritesManager.getFavorites();

        Log.d("FAVORITES", "Загружено избранного: " + favorites.size());

        if (favorites != null) {
            favoritesList.addAll(favorites);
        }

        adapter.notifyDataSetChanged();

        // Показать/скрыть пустой экран
        if (favoritesList.isEmpty()) {
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
        // Обновить список при возврате в активность
        loadFavorites();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}