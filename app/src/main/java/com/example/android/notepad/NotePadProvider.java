package com.example.android.notepad;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 提供笔记数据的 ContentProvider（基于 SQLite）。
 *
 * <p>结构与原生 NotePad 示例保持一致：notes 表 + notes/# 两条 URI 规则，
 * 在此基础上新增了 category、pinned 两列，并支持按关键字 / 分类查询。</p>
 */
public class NotePadProvider extends ContentProvider {

    private static final String TAG = "NotePadProvider";

    /** 数据库名 */
    private static final String DATABASE_NAME = "note_pad.db";
    /**
     * 数据库版本。
     * 1 = 原生 NotePad 的表结构
     * 2 = 加了 color 列的旧改版
     * 3 = 加了 category / pinned 列（当前）
     */
    private static final int DATABASE_VERSION = 3;

    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;

    private static final UriMatcher sUriMatcher;
    private static HashMap<String, String> sNotesProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        sNotesProjectionMap = new HashMap<>();
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_CATEGORY);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_PINNED,
                NotePad.Notes.COLUMN_NAME_PINNED);
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NotePad.Notes.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setProjectionMap(sNotesProjectionMap);
                break;
            case NOTE_ID:
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(NotePad.Notes._ID + "="
                        + uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy = TextUtils.isEmpty(sortOrder)
                ? NotePad.Notes.DEFAULT_SORT_ORDER : sortOrder;

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY)) {
            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_PINNED)) {
            values.put(NotePad.Notes.COLUMN_NAME_PINNED, 0);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(NotePad.Notes.TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, rowId);
            notifyChange(noteUri);
            return noteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(NotePad.Notes.TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                count = db.update(NotePad.Notes.TABLE_NAME, values,
                        NotePad.Notes._ID + "=" + noteId
                                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(NotePad.Notes.TABLE_NAME, selection, selectionArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                count = db.delete(NotePad.Notes.TABLE_NAME,
                        NotePad.Notes._ID + "=" + noteId
                                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

    private void notifyChange(Uri uri) {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
            // 同时通知集合 URI，保证列表页能收到单条笔记的变更
            getContext().getContentResolver().notifyChange(NotePad.Notes.CONTENT_URI, null);
        }
    }

    /**
     * 数据库帮助类
     *
     * <p>升级策略刻意做成「幂等」的：不去相信版本号，而是在每次打开数据库时
     * 逐列检查 notes 表缺哪一列就补哪一列。原因是本机曾经装过一个把
     * DATABASE_VERSION 已经写成 3、但表里还没有 pinned 列的中间版本，
     * 这种情况下 onUpgrade 永远不会再触发，只靠版本号判断会把库卡死。</p>
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_PINNED + " INTEGER DEFAULT 0"
                    + ");");
            createIndexIfNeeded(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            ensureColumns(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 旧版本回装时也不要丢数据，按列补齐即可
            ensureColumns(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            // 兜底：即使版本号已经是 3，也和真实表结构对一次账
            if (!db.isReadOnly()) {
                ensureColumns(db);
            }
        }

        /** 逐列比对并补齐缺失的列，可重复执行 */
        private void ensureColumns(SQLiteDatabase db) {
            Set<String> columns = tableColumns(db, NotePad.Notes.TABLE_NAME);
            if (columns.isEmpty()) {
                // notes 表都不存在（异常库），直接重建
                onCreate(db);
                return;
            }
            if (!columns.contains(NotePad.Notes.COLUMN_NAME_CATEGORY)) {
                db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME
                        + " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT");
            }
            if (!columns.contains(NotePad.Notes.COLUMN_NAME_PINNED)) {
                db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME
                        + " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_PINNED
                        + " INTEGER DEFAULT 0");
            }
            createIndexIfNeeded(db);
        }

        private Set<String> tableColumns(SQLiteDatabase db, String table) {
            Set<String> result = new HashSet<>();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        result.add(cursor.getString(nameIndex));
                    }
                }
            } catch (SQLException ignored) {
                // 表不存在等情况，返回空集合由调用方处理
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return result;
        }

        private void createIndexIfNeeded(SQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS note_modified_index ON "
                    + NotePad.Notes.TABLE_NAME
                    + "(" + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + ")");
        }
    }
}
