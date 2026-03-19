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
import com.example.lawapp.utils.NotesManager;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<ArticleFull> notesList = new ArrayList<>();

    private NotesManager notesManager;
    private TextView emptyText;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📝 Заметки");
        }

        notesManager = new NotesManager(this);

        // UI
        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);
        clearButton = findViewById(R.id.clearButton);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(notesList, this::onArticleClick);
        recyclerView.setAdapter(adapter);

        // Кнопка "Очистить всё"
        clearButton.setOnClickListener(v -> {
            notesManager.clearAll();
            loadNotes();
            Toast.makeText(this, "Все заметки удалены", Toast.LENGTH_SHORT).show();
        });

        // Загрузка заметок
        loadNotes();
    }

    private void loadNotes() {
        notesList.clear();
        List<NotesManager.Note> notes = notesManager.getAllNotes();

        for (NotesManager.Note note : notes) {
            ArticleFull article = new ArticleFull();
            article.название = note.articleTitle;
            notesList.add(article);
        }

        adapter.notifyDataSetChanged();

        // Показать/скрыть пустой экран
        if (notesList.isEmpty()) {
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

        // Открываем статью + показываем заметку
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_title", article.название);
        intent.putExtra("show_note", true);  // 🔥 Флаг для показа заметки
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();  // Обновить при возврате
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}