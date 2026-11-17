package com.example.android.notepad;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记仓库：把 ContentResolver 的样板代码集中在这里，界面层只关心业务。
 */
public class NoteRepository {

    /** 查询使用的列 */
    public static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_CREATE_DATE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_CATEGORY,
            NotePad.Notes.COLUMN_NAME_PINNED
    };

    private final Context context;
    private final ContentResolver resolver;

    public NoteRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    /**
     * 查询笔记。
     *
     * @param keyword   关键字，匹配标题或内容，可为 null
     * @param category  分类过滤，null 表示全部
     * @param sortOrder 排序语句
     */
    public List<Note> query(String keyword, String category, String sortOrder) {
        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if (!TextUtils.isEmpty(keyword)) {
            selection.append("(")
                    .append(NotePad.Notes.COLUMN_NAME_TITLE).append(" LIKE ? OR ")
                    .append(NotePad.Notes.COLUMN_NAME_NOTE).append(" LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }

        if (!TextUtils.isEmpty(category)) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(NotePad.Notes.COLUMN_NAME_CATEGORY).append(" = ?");
            args.add(category);
        }

        String selectionString = selection.length() == 0 ? null : selection.toString();
        String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);

        Cursor cursor = resolver.query(NotePad.Notes.CONTENT_URI, PROJECTION,
                selectionString, selectionArgs, sortOrder);

        List<Note> notes = new ArrayList<>();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    notes.add(read(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return notes;
    }

    /** 全部笔记 */
    public List<Note> all() {
        return query(null, null, AppPrefs.getSortOrder(context));
    }

    /** 按 id 查询单条笔记，不存在则返回 null */
    public Note find(long id) {
        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        Cursor cursor = resolver.query(uri, PROJECTION, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return read(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /** 新建笔记，返回自增 id */
    public long insert(String title, String content, String category) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, category);
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        values.put(NotePad.Notes.COLUMN_NAME_PINNED, 0);

        Uri uri = resolver.insert(NotePad.Notes.CONTENT_URI, values);
        return uri == null ? -1 : ContentUris.parseId(uri);
    }

    /** 按原样插入（用于备份恢复，保留创建 / 修改时间） */
    public long insertRaw(Note note) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, note.title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, note.content);
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, note.category);
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                note.created > 0 ? note.created : System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                note.modified > 0 ? note.modified : System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_PINNED, note.pinned ? 1 : 0);

        Uri uri = resolver.insert(NotePad.Notes.CONTENT_URI, values);
        return uri == null ? -1 : ContentUris.parseId(uri);
    }

    /** 保存编辑结果，同时刷新修改时间 */
    public int save(long id, String title, String content, String category, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, category);
        values.put(NotePad.Notes.COLUMN_NAME_PINNED, pinned ? 1 : 0);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        return resolver.update(uri, values, null, null);
    }

    /** 只切换置顶状态（不刷新修改时间） */
    public int setPinned(long id, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_PINNED, pinned ? 1 : 0);
        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        return resolver.update(uri, values, null, null);
    }

    /** 只修改分类（刷新修改时间） */
    public int setCategory(long id, String category) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, category);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        return resolver.update(uri, values, null, null);
    }

    /** 分类改名 / 删除时批量迁移笔记 */
    public int reassignCategory(String from, String to) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, to);
        return resolver.update(NotePad.Notes.CONTENT_URI, values,
                NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?", new String[]{from});
    }

    public int delete(long id) {
        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        return resolver.delete(uri, null, null);
    }

    public int deleteAll() {
        return resolver.delete(NotePad.Notes.CONTENT_URI, null, null);
    }

    public int countAll() {
        Cursor cursor = resolver.query(NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes._ID}, null, null, null);
        int count = cursor == null ? 0 : cursor.getCount();
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    public int countByCategory(String category) {
        Cursor cursor = resolver.query(NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes._ID},
                NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?",
                new String[]{category}, null);
        int count = cursor == null ? 0 : cursor.getCount();
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    private Note read(Cursor cursor) {
        Note note = new Note();
        note.id = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes._ID));
        note.title = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
        note.content = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));
        note.created = cursor.getLong(
                cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CREATE_DATE));
        note.modified = cursor.getLong(
                cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
        note.category = cursor.getString(
                cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CATEGORY));
        note.pinned = cursor.getInt(
                cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_PINNED)) == 1;

        if (note.title == null) {
            note.title = "";
        }
        if (note.content == null) {
            note.content = "";
        }
        if (note.category == null) {
            note.category = "";
        }
        return note;
    }
}
