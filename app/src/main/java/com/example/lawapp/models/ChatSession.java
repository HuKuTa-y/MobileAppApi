package com.example.lawapp.models;

import com.example.lawapp.adapters.ChatAdapter;
import java.io.Serializable;
import java.util.List;

public class ChatSession implements Serializable {
    public String id;             // Уникальный ID чата
    public long timestamp;        // Время создания
    public String title;          // Заголовок (первый вопрос пользователя)
    public List<ChatAdapter.Message> messages; // ПОЛНЫЙ СПИСОК СООБЩЕНИЙ

    public ChatSession(String id, long timestamp, String title, List<ChatAdapter.Message> messages) {
        this.id = id;
        this.timestamp = timestamp;
        this.title = title;
        this.messages = messages;
    }
}