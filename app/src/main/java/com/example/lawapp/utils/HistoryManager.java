package com.example.lawapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HistoryManager {

    private static final String PREF_NAME = "lawapp_history";
    private static final String KEY_HISTORY = "viewed_articles";
    private static final int MAX_HISTORY_SIZE = 40;  // 🔥 Ограничение: 40 статей

    private SharedPreferences prefs;
    private Gson gson;

    // 🔹 Модель записи истории
    public static class HistoryItem {
        public String articleTitle;
        public String source;        // Источник (кодекс/закон)
        public long viewedAt;        // Время просмотра

        public HistoryItem() {}

        public HistoryItem(String articleTitle, String source) {
            this.articleTitle = articleTitle;
            this.source = source;
            this.viewedAt = System.currentTimeMillis();
        }
    }

    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 🔹 Добавить статью в историю
    public void addViewedArticle(String articleTitle, String source) {
        if (articleTitle == null || articleTitle.trim().isEmpty()) return;

        List<HistoryItem> history = getHistory();

        // Удаляем дубликат (если статья уже есть в истории)
        history.removeIf(item -> item.articleTitle != null && item.articleTitle.equals(articleTitle));

        // Добавляем новую запись в начало списка
        history.add(0, new HistoryItem(articleTitle, source));

        // Ограничиваем размер (максимум 40)
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        saveHistory(history);
    }

    // 🔹 Получить всю историю
    public List<HistoryItem> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
        List<HistoryItem> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    // 🔹 Получить историю, отсортированную по времени (новые первые)
    public List<HistoryItem> getHistorySorted() {
        List<HistoryItem> history = getHistory();
        Collections.sort(history, new Comparator<HistoryItem>() {
            @Override
            public int compare(HistoryItem o1, HistoryItem o2) {
                return Long.compare(o2.viewedAt, o1.viewedAt);  // По убыванию
            }
        });
        return history;
    }

    // 🔹 Удалить конкретную статью из истории
    public void removeArticle(String articleTitle) {
        List<HistoryItem> history = getHistory();
        history.removeIf(item -> item.articleTitle != null && item.articleTitle.equals(articleTitle));
        saveHistory(history);
    }

    // 🔹 Очистить всю историю
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    // 🔹 Количество записей в истории
    public int getCount() {
        return getHistory().size();
    }

    // 🔹 Сохранить историю
    private void saveHistory(List<HistoryItem> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }
}