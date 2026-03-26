package com.example.lawapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HistoryManager {

    private static final String TAG = "HistoryManager";
    private static final String PREF_NAME = "lawapp_history";
    private static final String KEY_HISTORY = "viewed_articles";
    private static final int MAX_HISTORY_SIZE = 40;

    private final SharedPreferences prefs;
    private final Gson gson;
    private List<HistoryItem> historyCache;

    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        historyCache = null;
    }

    // 🔹 Добавить статью в историю
    public void addViewedArticle(String articleTitle, String source) {
        if (articleTitle == null || articleTitle.trim().isEmpty()) {
            Log.w(TAG, "Попытка добавить пустую статью в историю");
            return;
        }

        List<HistoryItem> history = getHistory();

        // 🔥 ИСПРАВЛЕНО: явное объявление типа в lambda
        history.removeIf((HistoryItem item) ->
                item.articleTitle != null && item.articleTitle.equals(articleTitle)
        );

        history.add(0, new HistoryItem(articleTitle, source));

        // 🔥 ИСПРАВЛЕНО: создаём новую копию списка
        if (history.size() > MAX_HISTORY_SIZE) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY_SIZE));
        }

        saveHistory(history);
        Log.d(TAG, "Добавлено в историю: " + articleTitle);
    }

    // 🔹 Получить всю историю
    public List<HistoryItem> getHistory() {
        if (historyCache != null) {
            return historyCache;
        }

        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) {
            historyCache = new ArrayList<>();
            return historyCache;
        }

        Type type = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
        List<HistoryItem> list = gson.fromJson(json, type);
        historyCache = (list != null) ? list : new ArrayList<>();
        return historyCache;
    }

    // 🔹 Получить историю, отсортированную по времени
    public List<HistoryItem> getHistorySorted() {
        List<HistoryItem> history = new ArrayList<>(getHistory());
        Collections.sort(history, new Comparator<HistoryItem>() {
            @Override
            public int compare(HistoryItem o1, HistoryItem o2) {
                return Long.compare(o2.viewedAt, o1.viewedAt);
            }
        });
        return history;
    }

    // 🔹 Удалить конкретную статью
    public void removeArticle(String articleTitle) {
        if (articleTitle == null) return;

        List<HistoryItem> history = getHistory();

        // 🔥 ИСПРАВЛЕНО: явное объявление типа
        boolean removed = history.removeIf((HistoryItem item) ->
                item.articleTitle != null && item.articleTitle.equals(articleTitle)
        );

        if (removed) {
            saveHistory(history);
            Log.d(TAG, "Удалено из истории: " + articleTitle);
        }
    }

    // 🔹 Очистить всю историю
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
        historyCache = null;
        Log.d(TAG, "История очищена");
    }

    // 🔹 Количество записей
    public int getCount() {
        return getHistory().size();
    }

    // 🔹 Сохранить историю
    private void saveHistory(List<HistoryItem> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
        historyCache = history;
    }

    // 🔹 Модель записи истории
    public static class HistoryItem {
        public String articleTitle;
        public String source;
        public long viewedAt;

        public HistoryItem() {}

        public HistoryItem(String articleTitle, String source) {
            this.articleTitle = articleTitle;
            this.source = source;
            this.viewedAt = System.currentTimeMillis();
        }
    }
}