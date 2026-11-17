package com.example.android.notepad;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类列表适配器：显示颜色圆点、分类名与笔记数量。
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {

    /** 交互回调 */
    public interface Listener {
        void onCategoryClick(Category category);

        void onCategoryLongClick(Category category, View anchor);
    }

    private final NoteRepository repository;
    private final Listener listener;
    private final List<Category> items = new ArrayList<>();

    public CategoryAdapter(NoteRepository repository, Listener listener) {
        this.repository = repository;
        this.listener = listener;
    }

    public void submit(List<Category> categories) {
        items.clear();
        if (categories != null) {
            items.addAll(categories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final Category category = items.get(position);

        holder.name.setText(category.name);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(category.color);
        holder.dot.setBackground(dot);

        int count = repository.countByCategory(category.name);
        holder.count.setText(holder.itemView.getContext()
                .getString(R.string.category_note_count, count));

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCategoryClick(category);
            }
        });
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onCategoryLongClick(category, v);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View dot;
        final TextView name;
        final TextView count;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            dot = itemView.findViewById(R.id.view_color);
            name = itemView.findViewById(R.id.text_name);
            count = itemView.findViewById(R.id.text_count);
        }
    }
}
