package com.example.lawapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.R;
import com.example.lawapp.models.Codek;
import java.util.List;

public class CodekAdapter extends RecyclerView.Adapter<CodekAdapter.ViewHolder> {
    private List<Codek> codeks;
    private OnCodekClickListener listener;

    public interface OnCodekClickListener {
        void onCodekClick(Codek codek);
    }

    public CodekAdapter(List<Codek> codeks, OnCodekClickListener listener) {
        this.codeks = codeks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_codek, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Codek codek = codeks.get(position);

        // 🔥 ДОБАВЛЕНО: Защита от null
        if (codek != null && codek.название != null && !codek.название.isEmpty()) {
            holder.button.setText(codek.название);
        } else {
            holder.button.setText("Без названия");
        }

        holder.button.setOnClickListener(v -> {
            if (listener != null && codek != null) {
                listener.onCodekClick(codek);
            }
        });
    }

    @Override
    public int getItemCount() {
        return codeks != null ? codeks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Button button;
        ViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.codekButton);
        }
    }
}