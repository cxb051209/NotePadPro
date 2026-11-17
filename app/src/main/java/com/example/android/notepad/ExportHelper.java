package com.example.android.notepad;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 笔记导出工具：单条笔记导出为 .txt，全部笔记导出为 .md。
 */
public class ExportHelper {

    private ExportHelper() {
    }

    /** 单条笔记的纯文本内容 */
    public static String noteToText(Context context, Note note) {
        StringBuilder builder = new StringBuilder();
        builder.append(TextUtils.isEmpty(note.title)
                ? context.getString(R.string.untitled) : note.title).append("\n");
        builder.append("分类：").append(displayCategory(context, note.category)).append("\n");
        builder.append("创建于：").append(TimeUtils.formatFull(note.created)).append("\n");
        builder.append("更新于：").append(TimeUtils.formatFull(note.modified)).append("\n");
        builder.append("----------------------------------------\n\n");
        builder.append(note.content == null ? "" : note.content).append("\n");
        return builder.toString();
    }

    /** 全部笔记导出为 Markdown */
    public static String notesToMarkdown(Context context, List<Note> notes) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 全部笔记\n\n");
        builder.append("导出时间：").append(TimeUtils.formatFull(System.currentTimeMillis())).append("\n\n");
        builder.append("笔记数量：").append(notes.size()).append("\n\n");

        int index = 1;
        for (Note note : notes) {
            builder.append("---\n\n");
            builder.append("## ").append(index++).append(". ")
                    .append(TextUtils.isEmpty(note.title)
                            ? context.getString(R.string.untitled) : note.title).append("\n\n");
            builder.append("- 分类：").append(displayCategory(context, note.category)).append("\n");
            builder.append("- 创建：").append(TimeUtils.formatFull(note.created)).append("\n");
            builder.append("- 更新：").append(TimeUtils.formatFull(note.modified)).append("\n");
            if (note.pinned) {
                builder.append("- 置顶：是\n");
            }
            builder.append("\n");
            builder.append(note.content == null ? "" : note.content).append("\n\n");
        }
        return builder.toString();
    }

    /** 生成安全的文件名 */
    public static String suggestedFileName(String name) {
        String sanitized = name == null ? "" : name.trim();
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|\\r\\n]", " ");
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        if (TextUtils.isEmpty(sanitized)) {
            sanitized = "笔记";
        }
        return sanitized;
    }

    /** 写入用户通过系统文件选择器选中的位置（支持网盘等） */
    public static boolean writeToUri(Context context, Uri uri, String content) {
        if (uri == null) {
            return false;
        }
        try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                return false;
            }
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 调起系统分享 */
    public static void shareText(Context context, String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        Intent chooser = Intent.createChooser(intent, context.getString(R.string.share_via));
        if (!(context instanceof Activity)) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.toast_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private static String displayCategory(Context context, String category) {
        return TextUtils.isEmpty(category)
                ? context.getString(R.string.editor_uncategorized) : category;
    }
}
