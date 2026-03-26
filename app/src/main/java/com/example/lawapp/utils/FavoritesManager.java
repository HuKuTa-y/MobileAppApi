package com.example.lawapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.lawapp.models.ArticleFull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер избранных статей
 * 🔥 Кэширование в SharedPreferences
 * 🔥 Защита от дубликатов
 */
public class FavoritesManager {

    private static final String TAG = "FavoritesManager";
    private static final String PREF_NAME = "lawapp_favorites";
    private static final String KEY_FAVORITES = "favorite_articles";

    private final SharedPreferences prefs;
    private final Gson gson;
    private List<ArticleFull> favoritesCache;  // 🔥 Кэш для скорости

    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        favoritesCache = null;  // Ленивая загрузка
    }

    // 🔹 Получить все избранные статьи (с кэшированием)
    public List<ArticleFull> getFavorites() {
        if (favoritesCache != null) {
            return favoritesCache;  // ✅ Из кэша
        }

        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) {
            favoritesCache = new ArrayList<>();
            return favoritesCache;
        }

        Type type = new TypeToken<ArrayList<ArticleFull>>(){}.getType();
        List<ArticleFull> list = gson.fromJson(json, type);
        favoritesCache = (list != null) ? list : new ArrayList<>();
        return favoritesCache;
    }

    // 🔹 Добавить статью в избранное
    public void addFavorite(ArticleFull article) {
        if (article == null || article.название == null) {
            Log.w(TAG, "⚠️ Попытка добавить null статью");
            return;
        }

        List<ArticleFull> favorites = getFavorites();

        // Проверка на дубликат
        for (ArticleFull a : favorites) {
            if (a.название != null && a.название.equals(article.название)) {
                Log.d(TAG, "📌 Статья уже в избранном: " + article.название);
                return;
            }
        }

        favorites.add(article);
        saveFavorites(favorites);
        Log.d(TAG, "⭐ Добавлено в избранное: " + article.название);
    }

    // 🔹 Удалить статью из избранного
    public void removeFavorite(String articleTitle) {
        if (articleTitle == null) return;

        List<ArticleFull> favorites = getFavorites();
        boolean removed = favorites.removeIf(a ->
                a.название != null && a.название.equals(articleTitle)
        );

        if (removed) {
            saveFavorites(favorites);
            Log.d(TAG, "🗑️ Удалено из избранного: " + articleTitle);
        }
    }

    // 🔹 Проверить, есть ли статья в избранном
    public boolean isFavorite(String articleTitle) {
        if (articleTitle == null) return false;

        for (ArticleFull a : getFavorites()) {
            if (a.название != null && a.название.equals(articleTitle)) {
                return true;
            }
        }
        return false;
    }

    // 🔹 Сохранить список избранного
    private void saveFavorites(List<ArticleFull> favorites) {
        String json = gson.toJson(favorites);
        prefs.edit().putString(KEY_FAVORITES, json).apply();
        favoritesCache = favorites;  // 🔥 Обновляем кэш
    }

    // 🔹 Очистить всё избранное
    public void clearAll() {
        prefs.edit().remove(KEY_FAVORITES).apply();
        favoritesCache = null;  // 🔥 Сброс кэша
        Log.d(TAG, "🗑️ Избранное очищено");
    }

    // 🔹 Количество избранных
    public int getCount() {
        return getFavorites().size();
    }

    // 🔹 Сбросить кэш (при изменении извне)
    public void invalidateCache() {
        favoritesCache = null;
    }
}