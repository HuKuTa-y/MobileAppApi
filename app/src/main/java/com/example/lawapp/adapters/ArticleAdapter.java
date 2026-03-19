package com.example.lawapp.adapters;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.R;
import com.example.lawapp.models.ArticleFull;
import java.util.List;
import java.util.Locale;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    private List<ArticleFull> articlesList;
    private OnArticleClickListener listener;

    // 🔥 Поле для хранения текущего поискового запроса
    private String searchQuery = "";

    public interface OnArticleClickListener {
        void onArticleClick(ArticleFull article);
    }

    public ArticleAdapter(List<ArticleFull> articlesList, OnArticleClickListener listener) {
        this.articlesList = articlesList;
        this.listener = listener;
    }

    // 🔥 Метод для установки запроса поиска (вызывать из MainActivity перед обновлением)
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim() : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false); // Убедитесь, что файл называется item_article.xml
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticleFull article = articlesList.get(position);

        if (article != null && article.название != null) {
            String title = article.название;

            // 🔥 Логика подсветки
            if (!searchQuery.isEmpty()) {
                SpannableString spannable = new SpannableString(title);
                String lowerTitle = title.toLowerCase(Locale.getDefault());
                String lowerQuery = searchQuery.toLowerCase(Locale.getDefault());

                int startIndex = 0;
                while ((startIndex = lowerTitle.indexOf(lowerQuery, startIndex)) != -1) {
                    int endIndex = startIndex + lowerQuery.length();

                    // Цвет выделения (например, желтый фон или красный текст)
                    // Вариант 1: Красный цвет текста
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.RED),
                            startIndex, endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );

                    // Вариант 2 (продвинутый): Желтый фон (раскомментируйте, если нужно)
                    // spannable.setSpan(new BackgroundColorSpan(Color.YELLOW), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    startIndex = endIndex;
                }
                holder.textView.setText(spannable);
            } else {
                // Если поиска нет — обычный текст
                holder.textView.setText(title);
            }

        } else {
            holder.textView.setText("Без названия");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && article != null) {
                listener.onArticleClick(article);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articlesList != null ? articlesList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.articleTitle); // Проверьте ID в вашем XML
        }
    }
}