package com.example.android.notepad;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记列表适配器：卡片式布局，展示标题、摘要、时间戳、分类标签与置顶状态。
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    /** 列表交互回调 */
    public interface Listener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note, View anchor);
    }

    private final Context context;
    private final List<Note> items = new ArrayList<>();
    private final Listener listener;
    private final int surfaceColor;
    private final boolean dark;

    public NoteAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.surfaceColor = ThemeHelper.surface(context);
        this.dark = ThemeHelper.isDark(context);
    }

    public void submit(List<Note> notes) {
        items.clear();
        if (notes != null) {
            items.addAll(notes);
        }
        notifyDataSetChanged();
    }

    public Note getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, int position) {
        final Note note = items.get(position);

        holder.title.setText(TextUtils.isEmpty(note.title)
                ? context.getString(R.string.untitled) : note.title);

        String snippet = note.snippet();
        holder.snippet.setText(snippet);
        holder.snippet.setVisibility(snippet.isEmpty() ? View.GONE : View.VISIBLE);

        holder.time.setText(TimeUtils.formatTimestamp(note.modified));

        Category category = CategoryManager.get(context).find(note.category);
        String categoryName = category != null ? category.name : CategoryManager.UNCATEGORIZED;
        int color = category != null ? category.color
                : ContextCompat.getColor(context, R.color.category_uncategorized);
        int labelColor = dark ? ColorUtils.blendARGB(color, Color.WHITE, 0.4f) : color;

        holder.category.setText(categoryName);
        holder.category.setTextColor(labelColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(ThemeHelper.dp(context, 8));
        badge.setColor(ColorUtils.setAlphaComponent(color, dark ? 0x33 : 0x1F));
        holder.category.setBackground(badge);

        holder.accent.setBackgroundTintList(ColorStateList.valueOf(color));
        holder.card.setCardBackgroundColor(
                ColorUtils.blendARGB(surfaceColor, color, dark ? 0.16f : 0.05f));

        holder.pin.setVisibility(note.pinned ? View.VISIBLE : View.GONE);

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onNoteClick(note);
            }
        });
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onNoteLongClick(note, v);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View accent;
        final TextView title;
        final TextView snippet;
        final TextView time;
        final TextView category;
        final ImageView pin;

        NoteHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            accent = itemView.findViewById(R.id.view_accent);
            title = itemView.findViewById(R.id.text_title);
            snippet = itemView.findViewById(R.id.text_snippet);
            time = itemView.findViewById(R.id.text_time);
            category = itemView.findViewById(R.id.text_category);
            pin = itemView.findViewById(R.id.img_pin);
        }
    }
}
