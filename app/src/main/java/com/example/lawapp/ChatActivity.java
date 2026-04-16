    package com.example.lawapp;
    import com.google.firebase.crashlytics.FirebaseCrashlytics;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.graphics.Color;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Looper;
    import android.text.SpannableString;
    import android.text.Spanned;
    import android.text.TextPaint;
    import android.text.style.ClickableSpan;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageButton;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.Toolbar;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.lawapp.adapters.ChatAdapter;
    import com.example.lawapp.api.ApiClient;
    import com.example.lawapp.api.GptApiClient;
    import com.example.lawapp.api.GptApiService;
    import com.example.lawapp.models.ArticleFull;
    import com.example.lawapp.models.ChatSession;
    import com.example.lawapp.models.SmartSearchRequest;
    import com.example.lawapp.models.SmartSearchResponse;
    import com.google.android.material.bottomsheet.BottomSheetDialog;
    import com.google.gson.Gson;
    import com.google.gson.reflect.TypeToken;

    import java.lang.reflect.Type;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.UUID;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class ChatActivity extends AppCompatActivity {

        private RecyclerView chatRecyclerView;
        private EditText messageInput;
        private Button sendButton;
        private ProgressBar loadingIndicator;
        private ImageButton btnHistory;
        private ChatAdapter adapter;
        private GptApiService gptApiService;

        // Хранение сессий
        private SharedPreferences sessionPrefs;
        private static final String PREF_SESSIONS = "chat_sessions_v2";
        private Gson gson = new Gson();

        private String currentSessionId;
        private List<ChatSession> allSessions = new ArrayList<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_chat);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            FirebaseCrashlytics.getInstance().log("CodeX запущен успешно!");
            // 🔥 ТЕСТОВЫЙ КРАШ (УДАЛИ ЭТУ СТРОКУ ПОСЛЕ ПРОВЕРКИ!)


            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(" AI Помощник");
            }


            chatRecyclerView = findViewById(R.id.chatRecyclerView);
            messageInput = findViewById(R.id.messageInput);
            sendButton = findViewById(R.id.sendButton);
            loadingIndicator = findViewById(R.id.loadingIndicator);
            btnHistory = findViewById(R.id.btnHistory);

            gptApiService = GptApiClient.getService();
            sessionPrefs = getSharedPreferences(PREF_SESSIONS, MODE_PRIVATE);

            // Инициализация
            adapter = new ChatAdapter();
            chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatRecyclerView.setAdapter(adapter);

            // Загружаем список всех сохраненных чатов
            allSessions = loadSessions();

            // Создаем НОВЫЙ чат при запуске
            startNewChat();

            sendButton.setOnClickListener(v -> sendMessage());

            if (btnHistory != null) {
                btnHistory.setOnClickListener(v -> showHistorySheet());
            }

            messageInput.setOnEditorActionListener((v, actionId, event) -> {
                sendMessage();
                return true;
            });
            throw new RuntimeException("TEST CRASH: CodeX работает отлично, это проверка Crashlytics!");
        }


        //  СОЗДАНИЕ НОВОГО ЧАТА
        private void startNewChat() {
            currentSessionId = UUID.randomUUID().toString();
            adapter.clear();
            adapter.addMessage(new ChatAdapter.Message("Здравствуйте! Я ваш юридический помощник. Опишите вашу проблему.", false));
            getSupportActionBar().setTitle("Новый чат");
        }

        // 🔥 ЗАГРУЗКА ВЫБРАННОГО ЧАТА
        private void loadChatSession(ChatSession session) {
            currentSessionId = session.id;
            adapter.clear();

            for (ChatAdapter.Message msg : session.messages) {
                if (!msg.isUser && msg.text != null) {
                    // 🔥 Если это сообщение бота, снова делаем ссылки кликабельными
                    SpannableString clickableText = makeArticlesClickable(msg.text);
                    adapter.addMessage(new ChatAdapter.Message(clickableText, false));
                } else {
                    // Для пользователя или если текст пустой — добавляем как есть
                    adapter.addMessage(new ChatAdapter.Message(msg.text, msg.isUser));
                }
            }

            getSupportActionBar().setTitle(session.title.length() > 20 ? session.title.substring(0, 20) + "..." : session.title);
            chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
            Toast.makeText(this, "Чат загружен", Toast.LENGTH_SHORT).show();
        }

        // 🔥 СОХРАНЕНИЕ ТЕКУЩЕГО ЧАТА (вызывается после каждого ответа бота)
        private void saveCurrentSession() {
            List<ChatAdapter.Message> currentMessages = adapter.getMessages();
            if (currentMessages.isEmpty()) return;

            // Формируем заголовок из первого сообщения пользователя
            String title = "Новый диалог";
            for (ChatAdapter.Message msg : currentMessages) {
                if (msg.isUser) {
                    title = msg.text.toString();
                    if (title.length() > 30) title = title.substring(0, 30) + "...";
                    break;
                }
            }

            // Ищем, есть ли уже такой чат в списке (по ID)
            ChatSession existingSession = null;
            for (ChatSession s : allSessions) {
                if (s.id.equals(currentSessionId)) {
                    existingSession = s;
                    break;
                }
            }

            ChatSession sessionToSave = new ChatSession(currentSessionId, System.currentTimeMillis(), title, new ArrayList<>(currentMessages));

            if (existingSession != null) {
                // Обновляем существующий чат
                allSessions.remove(existingSession);
                allSessions.add(0, sessionToSave); // Перемещаем наверх списка
            } else {
                // Добавляем новый чат
                allSessions.add(0, sessionToSave);
            }

            // Сохраняем в память (храним последние 20 чатов)
            if (allSessions.size() > 20) allSessions.remove(allSessions.size() - 1);

            SharedPreferences.Editor editor = sessionPrefs.edit();
            editor.putString("list", gson.toJson(allSessions));
            editor.apply();
        }

        // 🔥 ПОКАЗ СПИСКА ЧАТОВ (BOTTOM SHEET)
        private void showHistorySheet() {
            // Перед показом обновим текущий чат в памяти, если он изменился
            saveCurrentSession();

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_history, null);
            bottomSheetDialog.setContentView(sheetView);

            RecyclerView rvHistory = sheetView.findViewById(R.id.rvHistory);
            Button btnClear = sheetView.findViewById(R.id.btnClearHistory);
            Button btnNewChat = sheetView.findViewById(R.id.btnNewChat); // Добавим кнопку "Новый чат"

            // Адаптер для списка чатов
            SessionListAdapter sessionAdapter = new SessionListAdapter(allSessions, session -> {
                loadChatSession(session);
                bottomSheetDialog.dismiss();
            });

            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            rvHistory.setAdapter(sessionAdapter);

            btnClear.setOnClickListener(v -> {
                allSessions.clear();
                SharedPreferences.Editor editor = sessionPrefs.edit();
                editor.clear();
                editor.apply();
                sessionAdapter.updateData(new ArrayList<>());
                Toast.makeText(this, "Все чаты удалены", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
                startNewChat();
            });

            // Кнопка создания нового чата прямо из меню
            if (btnNewChat != null) {
                btnNewChat.setOnClickListener(v -> {
                    startNewChat();
                    bottomSheetDialog.dismiss();
                });
            }

            bottomSheetDialog.show();
        }

        // --- ЛОГИКА AI И ПОИСКА (без изменений сути) ---

        private SpannableString makeArticlesClickable(String text) {
            if (text == null || text.isEmpty()) return new SpannableString("");
            SpannableString spannable = new SpannableString(text);
            Pattern pattern = Pattern.compile("(?i)(статья|ст\\.)\\s*(\\d+)(?:\\s+([а-яА-ЯЁё\\s]+))?");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String number = matcher.group(2);
                String codeName = matcher.group(3);

                ClickableSpan clickSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        openArticleBySearch(number, codeName);
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.parseColor("#6C5CE7"));
                        ds.setUnderlineText(true);
                        ds.bgColor = Color.TRANSPARENT;
                    }
                };
                spannable.setSpan(clickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }

        private void openArticleBySearch(String number, String codeNameHint) {
            // (Твой код поиска статьи остался прежним)
            List<ArticleFull> database = MainActivity.articlesFull;
            ArticleFull foundArticle = null;
            if (database != null && !database.isEmpty()) {
                for (ArticleFull article : database) {
                    if (article.название != null && article.название.contains(number)) {
                        if (codeNameHint == null || article.название.toLowerCase().contains(codeNameHint.toLowerCase())) {
                            foundArticle = article;
                            break;
                        }
                    }
                }
            }
            if (foundArticle != null) {
                openArticleDetail(foundArticle.название);
            } else {
                fetchAndOpenArticleFromServer(number, codeNameHint);
            }
        }

        private void fetchAndOpenArticleFromServer(String number, String codeNameHint) {
            final String fNum = number;
            final String fCode = codeNameHint;

            Toast.makeText(this, "Поиск...", Toast.LENGTH_SHORT).show();

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String q = "Статья " + fNum + (fCode != null ? " " + fCode : "");
                    Response<List<ArticleFull>> resp = ApiClient.getService().searchByText(q).execute();

                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        ArticleFull best = resp.body().get(0);

                        if (fCode != null) {
                            for (ArticleFull a : resp.body()) {
                                if (a.название.toLowerCase().contains(fCode.toLowerCase())) {
                                    best = a;
                                    break;
                                }
                            }
                        }

                        // 🔥 ИСПРАВЛЕНО: Тип ArticleFull
                        final ArticleFull articleToOpen = best;

                        runOnUiThread(() -> openArticleDetail(articleToOpen.название));
                    } else {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Статья не найдена", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
                }
            });
        }

        private void openArticleDetail(String title) {
            Intent intent = new Intent(this, ArticleDetailActivity.class);
            intent.putExtra("article_title", title);
            startActivity(intent);
        }

        private void sendMessage() {
            String text = messageInput.getText().toString().trim();
            if (text.isEmpty()) return;

            // Добавляем сообщение пользователя
            adapter.addMessage(new ChatAdapter.Message(text, true));
            messageInput.setText("");
            loadingIndicator.setVisibility(View.VISIBLE);
            chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);

            SmartSearchRequest request = new SmartSearchRequest(text);

            gptApiService.smartSearch(request).enqueue(new Callback<SmartSearchResponse>() {
                @Override
                public void onResponse(Call<SmartSearchResponse> call, Response<SmartSearchResponse> response) {
                    loadingIndicator.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        SmartSearchResponse body = response.body();
                        if (body.isSuccess()) {
                            SpannableString clickableText = makeArticlesClickable(body.getResult());
                            adapter.addMessage(new ChatAdapter.Message(clickableText, false));

                            // 🔥 ВАЖНО: Сохраняем весь диалог после получения ответа
                            saveCurrentSession();
                        } else {
                            adapter.addMessage(new ChatAdapter.Message("Ошибка: " + body.getError(), false));
                        }
                    } else {
                        adapter.addMessage(new ChatAdapter.Message("Ошибка сервера.", false));
                    }
                    chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }

                @Override
                public void onFailure(Call<SmartSearchResponse> call, Throwable t) {
                    loadingIndicator.setVisibility(View.GONE);
                    adapter.addMessage(new ChatAdapter.Message("Нет соединения.", false));
                    chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            });
        }

        private List<ChatSession> loadSessions() {
            String json = sessionPrefs.getString("list", null);
            if (json == null) return new ArrayList<>();
            Type type = new TypeToken<List<ChatSession>>(){}.getType();
            return gson.fromJson(json, type);
        }

        @Override
        public boolean onSupportNavigateUp() {
            finish();
            return true;
        }

        // Адаптер для списка чатов
        private static class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.ViewHolder> {
            private List<ChatSession> data;
            private OnSessionClickListener listener;

            interface OnSessionClickListener { void onSessionClick(ChatSession session); }

            public SessionListAdapter(List<ChatSession> data, OnSessionClickListener listener) {
                this.data = data;
                this.listener = listener;
            }

            public void updateData(List<ChatSession> newData) {
                this.data = newData;
                notifyDataSetChanged();
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(30, 25, 30, 25);
                tv.setTextSize(16);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundResource(android.R.drawable.list_selector_background);
                return new ViewHolder(tv);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                ChatSession session = data.get(position);
                // Форматируем дату и текст
                String date = new java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(session.timestamp));
                holder.textView.setText(date + "\n" + session.title);
                holder.itemView.setOnClickListener(v -> listener.onSessionClick(session));
            }

            @Override
            public int getItemCount() { return data.size(); }

            static class ViewHolder extends RecyclerView.ViewHolder {
                TextView textView;
                ViewHolder(View itemView) { super(itemView); textView = (TextView) itemView; }
            }
        }
    }