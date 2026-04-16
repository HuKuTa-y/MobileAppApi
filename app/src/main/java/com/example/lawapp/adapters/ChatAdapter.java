package com.example.lawapp.adapters;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    //  ИЗМЕНЕНИЕ 1: Меняем тип text со String на CharSequence, чтобы принимать SpannableString
    public static class Message {
        public String text; // 🔥 ИЗМЕНЕНО: Теперь всегда String для сохранения
        public boolean isUser;

        // Конструктор принимает CharSequence, но сразу превращает в String
        public Message(CharSequence text, boolean isUser) {
            this.text = text != null ? text.toString() : "";
            this.isUser = isUser;
        }

        // Геттер возвращает CharSequence (для совместимости с остальным кодом)
        public CharSequence getText() {
            return text;
        }
    }

    private List<Message> messages = new ArrayList<>();

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messages.get(position);

        // Устанавливаем текст (теперь это может быть SpannableString)
        holder.messageText.setText(msg.text);
        holder.senderText.setText(msg.isUser ? "Вы" : "AI Помощник");

        if (msg.isUser) {
            // Сообщения пользователя: фиолетовый фон, белый текст
            holder.messageText.setBackgroundColor(0xFF6C5CE7);
            holder.messageText.setTextColor(0xFFFFFFFF);
            holder.messageText.setPadding(24, 16, 24, 16);
            holder.messageText.setMovementMethod(null); // Ссылки не нужны
            holder.senderText.setVisibility(View.GONE);
        } else {
            // Сообщения бота: серый фон, черный текст
            holder.messageText.setBackgroundColor(0xFFE0E0E0);
            holder.messageText.setTextColor(0xFF000000);
            holder.messageText.setPadding(24, 16, 24, 16);
            holder.senderText.setVisibility(View.VISIBLE);

            // 🔥 ИЗМЕНЕНИЕ 2: ВКЛЮЧАЕМ КЛИКИ ПО ССЫЛКАМ ДЛЯ БОТА
            // Это обязательно для работы ClickableSpan
            holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.messageText.setLinksClickable(true);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    // Также добавь геттер, чтобы Activity мог получить список сообщений для сохранения
    public List<Message> getMessages() {
        return messages;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderText = itemView.findViewById(R.id.messageSender);
        }
    }
}