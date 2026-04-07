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
 * Оптимизированный адаптер для списка статей
 * 🔥 Кэширование цветов, подсветки и кликов
 * 🔥 60 FPS прокрутка
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    // 🔥 Кэшированные цвета (не создавать каждый раз!)
    private static final int COLOR_HIGHLIGHT = Color.parseColor("#D32F2F");
    private static final int COLOR_TEXT_NORMAL = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_DISABLED = Color.parseColor("#757575");
    private static final int COLOR_BG_AVAILABLE = Color.parseColor("#3364B5F5");
    private static final int COLOR_BG_UNAVAILABLE = Color.parseColor("#E664B5F5");
    private static final int COLOR_BG_TRANSPARENT = Color.TRANSPARENT;

    private List<ArticleFull> articlesList;
    private OnArticleClickListener listener;
    private String searchQuery = "";

    public interface OnArticleClickListener {
        void onArticleClick(ArticleFull article);
    }

    public ArticleAdapter(List<ArticleFull> articlesList, OnArticleClickListener listener) {
        this.articlesList = articlesList;
        this.listener = listener;

        // 🔥 ВКЛЮЧАЕМ Stable IDs (вместо переопределения метода)
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
        // 🔥 Лог для проверки что binding работает
        Log.d("ADAPTER_DEBUG", "Binding position " + position + " of " + getItemCount());

        ArticleFull article = articlesList.get(position);
        holder.bind(article, searchQuery);
    }

    @Override
    public int getItemCount() {
        return articlesList != null ? articlesList.size() : 0;
    }

    // 🔥 Возвращаем уникальный ID для каждой статьи
    @Override
    public long getItemId(int position) {
        ArticleFull article = articlesList.get(position);
        if (article != null && article.название != null) {
            return article.название.hashCode();
        }
        return super.getItemId(position);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 🔹 ViewHolder с оптимизацией
    // ─────────────────────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final Context context;
        private OnArticleClickListener listener;

        ViewHolder(View itemView, OnArticleClickListener listener) {
            super(itemView);
            textView = itemView.findViewById(R.id.articleTitle);
            context = itemView.getContext();
            this.listener = listener;

            // 🔥 Отключаем фокус для производительности
            textView.setFocusable(false);
            textView.setClickable(false);


            // 🔥 Клик устанавливается ОДИН РАЗ (не в onBindViewHolder!)
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    ArticleFull article = (ArticleFull) itemView.getTag();
                    if (article != null && article.название != null) {
                        // 🔥 Проверка доступности статьи
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

        // 🔥 Метод bind для чистой логики
        void bind(ArticleFull article, String searchQuery) {
            // Сохраняем статью в tag для клика
            itemView.setTag(article);

            if (article == null || article.название == null) {
                textView.setText("Без названия");
                textView.setTextColor(COLOR_TEXT_DISABLED);
                itemView.setAlpha(0.5f);
                itemView.setBackgroundColor(COLOR_BG_TRANSPARENT);
                return;
            }

            String title = article.название;

            // 🔥 Подсветка поиска (оптимизировано)
            if (!searchQuery.isEmpty()) {
                textView.setText(applyHighlight(title, searchQuery));
            } else {
                textView.setText(title);
            }

            textView.setTextColor(COLOR_TEXT_NORMAL);

            // 🔥 Визуальная доступность (офлайн-режим)
            updateAvailability(article);
        }

        // 🔥 Вынесенная логика подсветки (меньше аллокаций)
        private SpannableString applyHighlight(String text, String query) {
            // 🔥 ДОБАВЛЕНО: Защита от null
            if (text == null) {
                return new SpannableString("");
            }
            if (query == null || query.isEmpty()) {
                return new SpannableString(text);
            }

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

        // 🔥 Оптимизированная проверка доступности
        private void updateAvailability(ArticleFull article) {
            boolean isOnline = NetworkUtils.isOnline(context);
            boolean isCached = CacheManager.isArticleTextCached(article.название);

            if (isOnline) {
                // ✅ Есть интернет — обычный вид
                itemView.setBackgroundColor(COLOR_BG_TRANSPARENT);
                itemView.setAlpha(1.0f);
            } else {
                // ❌ Нет интернета — показываем доступность
                if (isCached) {
                    // ✅ В кэше — можно открыть
                    itemView.setBackgroundColor(COLOR_BG_AVAILABLE);
                    itemView.setAlpha(1.0f);
                } else {
                    // ❌ Не в кэше — нельзя открыть
                    itemView.setBackgroundColor(COLOR_BG_UNAVAILABLE);
                    itemView.setAlpha(0.4f);
                }
            }
        }

        // 🔥 Освобождаем ресурсы при рециклинге
        void recycle() {
            itemView.setTag(null);
        }
    }

    // 🔥 Освобождаем память при детаче
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }
}