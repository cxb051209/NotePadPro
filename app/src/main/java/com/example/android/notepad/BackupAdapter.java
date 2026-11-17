package com.example.android.notepad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 备份记录适配器。
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.Holder> {

    /** 交互回调 */
    public interface Listener {
        void onBackupClick(BackupManager.BackupInfo info);

        void onBackupLongClick(BackupManager.BackupInfo info, View anchor);
    }

    private final List<BackupManager.BackupInfo> items = new ArrayList<>();
    private final Listener listener;

    public BackupAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<BackupManager.BackupInfo> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_backup, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final BackupManager.BackupInfo info = items.get(position);

        holder.name.setText(holder.itemView.getContext()
                .getString(R.string.backup_item_title, TimeUtils.formatSecond(info.time)));
        holder.info.setText(holder.itemView.getContext().getString(R.string.backup_info,
                info.count, formatSize(info.size())));

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onBackupClick(info);
            }
        });
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onBackupLongClick(info, v);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f);
        }
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView name;
        final TextView info;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            name = itemView.findViewById(R.id.text_name);
            info = itemView.findViewById(R.id.text_info);
        }
    }
}
