package com.example.lawapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.adapters.ChatAdapter;
import com.example.lawapp.api.ApiClient;
import com.example.lawapp.api.GptApiClient;
import com.example.lawapp.api.GptApiService;
import com.example.lawapp.api.LawApiService;
import com.example.lawapp.models.SmartSearchRequest;
import com.example.lawapp.models.SmartSearchResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar loadingIndicator;
    private ChatAdapter adapter;
    private GptApiService gptApiService; // Используем сервис для GPT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(" AI Помощник");
        }

        // Инициализация
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        gptApiService = GptApiClient.getService(); // Получаем сервис для GPT

        // Настройка RecyclerView
        adapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        // Приветственное сообщение
        adapter.addMessage(new ChatAdapter.Message("Здравствуйте! Я ваш юридический помощник. Опишите вашу проблему, и я найду подходящие статьи законов.", false));

        // Обработчик кнопки отправки
        sendButton.setOnClickListener(v -> sendMessage());

        // Отправка по Enter (опционально)
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // Добавляем сообщение пользователя
        adapter.addMessage(new ChatAdapter.Message(text, true));
        messageInput.setText("");

        // Показываем индикатор загрузки
        loadingIndicator.setVisibility(View.VISIBLE);
        chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);

        // Отправляем запрос на сервер
        SmartSearchRequest request = new SmartSearchRequest(text);

        gptApiService.smartSearch(request).enqueue(new Callback<SmartSearchResponse>() {
            @Override
            public void onResponse(Call<SmartSearchResponse> call, Response<SmartSearchResponse> response) {
                loadingIndicator.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    SmartSearchResponse body = response.body();
                    if (body.isSuccess()) {
                        adapter.addMessage(new ChatAdapter.Message(body.getResult(), false));
                    } else {
                        adapter.addMessage(new ChatAdapter.Message("Ошибка: " + body.getError(), false));
                    }
                } else {
                    adapter.addMessage(new ChatAdapter.Message("Ошибка сервера. Проверьте соединение.", false));
                }
                chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onFailure(Call<SmartSearchResponse> call, Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                adapter.addMessage(new ChatAdapter.Message("Нет соединения: " + t.getMessage(), false));
                chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}