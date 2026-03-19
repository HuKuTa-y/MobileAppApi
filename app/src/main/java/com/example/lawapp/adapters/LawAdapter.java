package com.example.lawapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lawapp.R;
import com.example.lawapp.models.Law;
import java.util.List;

public class LawAdapter extends RecyclerView.Adapter<LawAdapter.ViewHolder> {
    private List<Law> laws;
    private OnLawClickListener listener;

    public interface OnLawClickListener {
        void onLawClick(Law law);
    }

    public LawAdapter(List<Law> laws, OnLawClickListener listener) {
        this.laws = laws;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_law, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Law law = laws.get(position);

        // ДОБАВЬТЕ ЗАЩИТУ:
        if (law != null && law.название != null) {
            holder.button.setText(law.название);
        } else {
            holder.button.setText("Без названия");
        }

        holder.button.setOnClickListener(v -> {
            if (listener != null && law != null) {
                listener.onLawClick(law);
            }
        });
    }

    @Override
    public int getItemCount() {
        return laws != null ? laws.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Button button;
        ViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.lawButton);
        }
    }
}