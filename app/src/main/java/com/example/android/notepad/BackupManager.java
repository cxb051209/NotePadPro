package com.example.android.notepad;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 云备份管理。
 *
 * <p>备份文件以 JSON 形式保存在应用的“云端备份空间”（外部私有目录 NotePadCloud）中，
 * 在云备份页可以一键备份全部笔记，需要时用任意一份备份覆盖恢复。</p>
 */
public class BackupManager {

    /** 备份目录名 */
    public static final String DIR_NAME = "NotePadCloud";
    /** 备份文件格式标记 */
    private static final String APP_TAG = "NotePadPro";
    /** 最多保留的本地备份数量 */
    private static final int MAX_BACKUPS = 10;

    private BackupManager() {
    }

    /** 备份信息（用于列表展示） */
    public static class BackupInfo {
        public final File file;
        public final long time;
        public final int count;

        public BackupInfo(File file, long time, int count) {
            this.file = file;
            this.time = time;
            this.count = count;
        }

        public long size() {
            return file == null ? 0 : file.length();
        }
    }

    /** 云端备份目录 */
    public static File backupDir(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) {
            base = context.getFilesDir();
        }
        File dir = new File(base, DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            // 目录创建失败时退回内部存储
            dir = new File(context.getFilesDir(), DIR_NAME);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
        }
        return dir;
    }

    /** 生成并保存一次备份，返回备份文件 */
    public static File createBackup(Context context, NoteRepository repository) throws IOException {
        long now = System.currentTimeMillis();
        String json = buildJson(context, repository, now);

        File file = new File(backupDir(context),
                "backup_" + TimeUtils.fileStamp(now) + ".json");
        writeText(file, json);

        AppPrefs.setLastBackupTime(context, now);
        prune(context, MAX_BACKUPS);
        return file;
    }

    /** 构造备份 JSON 内容 */
    public static String buildJson(Context context, NoteRepository repository, long time)
            throws IOException {
        List<Note> notes = repository.all();
        try {
            JSONObject root = new JSONObject();
            root.put("app", APP_TAG);
            root.put("version", 2);
            root.put("exportedAt", time);
            root.put("count", notes.size());

            JSONArray array = new JSONArray();
            for (Note note : notes) {
                array.put(note.toJson());
            }
            root.put("notes", array);
            return root.toString(2);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /** 备份列表，按时间倒序 */
    public static List<BackupInfo> listBackups(Context context) {
        List<BackupInfo> result = new ArrayList<>();
        File dir = backupDir(context);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            result.add(readInfo(file));
        }
        Collections.sort(result, new Comparator<BackupInfo>() {
            @Override
            public int compare(BackupInfo a, BackupInfo b) {
                return Long.compare(b.time, a.time);
            }
        });
        return result;
    }

    /** 从文件中解析备份信息 */
    public static BackupInfo readInfo(File file) {
        long time = file.lastModified();
        int count = 0;
        try {
            JSONObject root = new JSONObject(readText(file));
            time = root.optLong("exportedAt", time);
            JSONArray array = root.optJSONArray("notes");
            count = array == null ? 0 : array.length();
        } catch (Exception ignored) {
            // 文件损坏时用文件信息兜底
        }
        return new BackupInfo(file, time, count);
    }

    /**
     * 从 JSON 内容恢复笔记：先清空现有笔记，再写入备份内容（覆盖恢复）。
     *
     * @return 恢复的笔记条数
     */
    public static int restore(Context context, NoteRepository repository, String json)
            throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray array = root.optJSONArray("notes");
        if (array == null) {
            throw new JSONException("备份文件缺少 notes 字段");
        }

        List<Note> notes = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                continue;
            }
            Note note = Note.fromJson(object);
            notes.add(note);
            if (note.category != null && !note.category.isEmpty()
                    && !categories.contains(note.category)) {
                categories.add(note.category);
            }
        }

        CategoryManager.get(context).ensureCategories(categories);

        repository.deleteAll();
        for (Note note : notes) {
            repository.insertRaw(note);
        }
        return notes.size();
    }

    /** 只保留最新的若干份备份 */
    public static void prune(Context context, int keep) {
        List<BackupInfo> list = listBackups(context);
        for (int i = keep; i < list.size(); i++) {
            //noinspection ResultOfMethodCallIgnored
            list.get(i).file.delete();
        }
    }

    /* ---------------- 文件读写 ---------------- */

    public static String readText(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return readStream(input);
        }
    }

    public static void writeText(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (OutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static String readStream(InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
        }
        return builder.toString();
    }
}
