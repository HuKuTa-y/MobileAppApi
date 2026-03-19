package com.example.lawapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lawapp.models.ArticleFull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {

    private static final String PREF_NAME = "lawapp_favorites";
    private static final String KEY_FAVORITES = "favorite_articles";

    private SharedPreferences prefs;
    private Gson gson;

    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 🔹 Получить все избранные статьи
    public List<ArticleFull> getFavorites() {
        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<ArticleFull>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // 🔹 Добавить статью в избранное
    public void addFavorite(ArticleFull article) {
        List<ArticleFull> favorites = getFavorites();

        // Проверка: не дубликат
        for (ArticleFull a : favorites) {
            if (a.название != null && a.название.equals(article.название)) {
                return; // Уже есть в избранном
            }
        }

        favorites.add(article);
        saveFavorites(favorites);
    }

    // 🔹 Удалить статью из избранного
    public void removeFavorite(String articleTitle) {
        List<ArticleFull> favorites = getFavorites();
        favorites.removeIf(a -> a.название != null && a.название.equals(articleTitle));
        saveFavorites(favorites);
    }

    // 🔹 Проверить, есть ли статья в избранном
    public boolean isFavorite(String articleTitle) {
        List<ArticleFull> favorites = getFavorites();
        for (ArticleFull a : favorites) {
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
    }

    // 🔹 Очистить всё избранное
    public void clearAll() {
        prefs.edit().remove(KEY_FAVORITES).apply();
    }

    // 🔹 Количество избранных
    public int getCount() {
        return getFavorites().size();
    }
}