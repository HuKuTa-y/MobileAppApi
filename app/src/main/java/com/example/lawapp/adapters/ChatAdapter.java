package com.example.lawapp.adapters;

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

    public static class Message {
        public String text;
        public boolean isUser; // true = пользователь, false = бот

        public Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
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
        holder.messageText.setText(msg.text);
        holder.senderText.setText(msg.isUser ? "Вы" : "AI Помощник");

        // Стилизация пузырей сообщений
        // Стилизация пузырей сообщений
        if (msg.isUser) {
            // 🔥 УБРАЛ ссылку на R.drawable.bg_user_message
            // Просто задаем цвет фона программно
            holder.messageText.setBackgroundColor(0xFF6C5CE7); // Фиолетовый фон
            holder.messageText.setTextColor(0xFFFFFFFF);       // Белый текст
            holder.messageText.setPadding(24, 16, 24, 16);     // Отступы внутри пузыря
            // Делаем углы скругленными (опционально, если API позволяет, или через ShapeDrawable)

            holder.senderText.setVisibility(View.GONE);
        } else {
            // Для бота оставляем стандартный фон или светло-серый
            holder.messageText.setBackgroundColor(0xFFE0E0E0); // Светло-серый фон
            holder.messageText.setTextColor(0xFF000000);       // Черный текст
            holder.messageText.setPadding(24, 16, 24, 16);
            holder.senderText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
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