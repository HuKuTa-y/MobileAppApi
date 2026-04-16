package com.example.lawapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NotesManager {

    private static final String PREF_NAME = "lawapp_notes";
    private static final String KEY_NOTES = "article_notes";

    private SharedPreferences prefs;
    private Gson gson;

    // 🔹 Модель заметки
    public static class Note {
        public String articleTitle;
        public String noteText;
        public long createdAt;

        public Note() {}

        public Note(String articleTitle, String noteText) {
            this.articleTitle = articleTitle;
            this.noteText = noteText;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public NotesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 🔹 Добавить/обновить заметку
    public void addOrUpdateNote(String articleTitle, String noteText) {
        List<Note> notes = getAllNotes();

        // Удаляем старую заметку если есть
        notes.removeIf(n -> n.articleTitle != null && n.articleTitle.equals(articleTitle));

        // Добавляем новую
        if (noteText != null && !noteText.trim().isEmpty()) {
            notes.add(new Note(articleTitle, noteText));
        }

        saveNotes(notes);
    }

    // 🔹 Получить заметку для статьи
    public String getNote(String articleTitle) {
        List<Note> notes = getAllNotes();
        for (Note n : notes) {
            if (n.articleTitle != null && n.articleTitle.equals(articleTitle)) {
                return n.noteText;
            }
        }
        return null;
    }

    // 🔹 Проверить, есть ли заметка
    public boolean hasNote(String articleTitle) {
        return getNote(articleTitle) != null;
    }

    // 🔹 Удалить заметку
    public void removeNote(String articleTitle) {
        List<Note> notes = getAllNotes();
        notes.removeIf(n -> n.articleTitle != null && n.articleTitle.equals(articleTitle));
        saveNotes(notes);
    }

    // 🔹 Получить все заметки
    public List<Note> getAllNotes() {
        String json = prefs.getString(KEY_NOTES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Note>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // 🔹 Количество заметок
    public int getCount() {
        return getAllNotes().size();
    }

    // 🔹 Очистить все заметки
    public void clearAll() {
        prefs.edit().remove(KEY_NOTES).apply();
    }

    // 🔹 Сохранить список
    private void saveNotes(List<Note> notes) {
        String json = gson.toJson(notes);
        prefs.edit().putString(KEY_NOTES, json).apply();
    }
}