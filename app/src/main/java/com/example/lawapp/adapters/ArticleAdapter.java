package com.example.lawapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.R;
import com.example.lawapp.cache.CacheManager;
import com.example.lawapp.models.ArticleFull;
import com.example.lawapp.utils.NetworkUtils;

import java.util.List;
import java.util.Locale;

/**
 * Адаптер: Статьи - темные, Главы/Разделы - светлые (только в офлайне).
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    // ЦВЕТА
    private static final int COLOR_HIGHLIGHT = Color.parseColor("#D32F2F"); // Красный для поиска
    private static final int COLOR_TEXT_NORMAL = Color.parseColor("#FFFFFF"); // Белый текст

    // Синие оттенки для офлайна
    private static final int COLOR_BG_ARTICLE = Color.parseColor("#3364B5F5");   // ТЕМНО-СИНИЙ (для статей)
    private static final int COLOR_BG_SECTION = Color.parseColor("#5580C0E0");   // СВЕТЛО-СИНИЙ (для глав/разделов)
    private static final int COLOR_BG_UNAVAILABLE = Color.parseColor("#E664B5F5"); // Полупрозрачный (недоступные статьи)

    private static final int COLOR_BG_TRANSPARENT = Color.TRANSPARENT; // Прозрачный (онлайн)

    private List<ArticleFull> articlesList;
    private OnArticleClickListener listener;
    private String searchQuery = "";

    public interface OnArticleClickListener {
        void onArticleClick(ArticleFull article);
    }

    public ArticleAdapter(List<ArticleFull> articlesList, OnArticleClickListener listener) {
        this.articlesList = articlesList;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim() : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticleFull article = articlesList.get(position);
        holder.bind(article, searchQuery);
    }

    @Override
    public int getItemCount() {
        return articlesList != null ? articlesList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        ArticleFull article = articlesList.get(position);
        if (article != null && article.название != null) {
            return article.название.hashCode();
        }
        return super.getItemId(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final Context context;
        private OnArticleClickListener listener;

        ViewHolder(View itemView, OnArticleClickListener listener) {
            super(itemView);
            textView = itemView.findViewById(R.id.articleTitle);
            context = itemView.getContext();
            this.listener = listener;

            textView.setFocusable(false);
            textView.setClickable(false);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    ArticleFull article = (ArticleFull) itemView.getTag();
                    if (article != null && article.название != null) {
                        if (!CacheManager.canOpenArticle(context, article.название)) {
                            Toast.makeText(context,
                                    "⚠️ Статья недоступна офлайн. Подключитесь к интернету.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        listener.onArticleClick(article);
                    }
                }
            });
        }

        void bind(ArticleFull article, String searchQuery) {
            itemView.setTag(article);

            if (article == null || article.название == null) {
                textView.setText("Без названия");
                textView.setTextColor(Color.GRAY);
                itemView.setBackgroundColor(COLOR_BG_TRANSPARENT);
                return;
            }

            String title = article.название;

            if (!searchQuery.isEmpty()) {
                textView.setText(applyHighlight(title, searchQuery));
            } else {
                textView.setText(title);
            }

            textView.setTextColor(COLOR_TEXT_NORMAL);

            updateAvailability(article);
        }

        private SpannableString applyHighlight(String text, String query) {
            if (text == null) return new SpannableString("");
            if (query == null || query.isEmpty()) return new SpannableString(text);

            SpannableString spannable = new SpannableString(text);
            String lowerText = text.toLowerCase(Locale.getDefault());
            String lowerQuery = query.toLowerCase(Locale.getDefault());

            int startIndex = 0;
            while ((startIndex = lowerText.indexOf(lowerQuery, startIndex)) != -1) {
                int endIndex = startIndex + lowerQuery.length();
                spannable.setSpan(
                        new ForegroundColorSpan(COLOR_HIGHLIGHT),
                        startIndex, endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                startIndex = endIndex;
            }
            return spannable;
        }

        private void updateAvailability(ArticleFull article) {
            boolean isOnline = NetworkUtils.isOnline(context);
            boolean isCached = CacheManager.isArticleTextCached(article.название);

            String title = article.название;
            // Проверяем, является ли элемент Разделом или Главой
            boolean isSectionOrChapter = title.startsWith("Раздел") || title.startsWith("Глава");

            if (isOnline) {
                // ✅ ЕСТЬ ИНТЕРНЕТ: Ничего не выделяем (прозрачный фон)
                itemView.setBackgroundColor(COLOR_BG_TRANSPARENT);
                itemView.setAlpha(1.0f);
            } else {
                //  НЕТ ИНТЕРНЕТА: Используем синие оттенки

                if (isSectionOrChapter) {
                    // Это Раздел или Глава -> СВЕТЛО-СИНИЙ фон
                    itemView.setBackgroundColor(COLOR_BG_SECTION);
                    itemView.setAlpha(1.0f);
                } else {
                    // Это СТАТЬЯ
                    if (isCached) {
                        // Статья в кэше -> ТЕМНО-СИНИЙ фон
                        itemView.setBackgroundColor(COLOR_BG_ARTICLE);
                        itemView.setAlpha(1.0f);
                    } else {
                        // Статья НЕ в кэше -> Полупрозрачный темно-синий
                        itemView.setBackgroundColor(COLOR_BG_UNAVAILABLE);
                        itemView.setAlpha(0.4f);
                    }
                }
            }
        }

        void recycle() {
            itemView.setTag(null);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }
}