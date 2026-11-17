package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 应用偏好设置（主题、字号、行距、纸张背景、排序、自动保存、自动备份等）。
 */
public class AppPrefs {

    /* 主题 */
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_GREEN = 2;
    public static final int THEME_OCEAN = 3;

    /* 列表排序 */
    public static final int SORT_MODIFIED = 0;
    public static final int SORT_CREATED = 1;
    public static final int SORT_TITLE = 2;

    /* 编辑器纸张 */
    public static final int BG_PAPER = 0;
    public static final int BG_WHITE = 1;
    public static final int BG_CREAM = 2;
    public static final int BG_MINT = 3;

    private static final String NAME = "notepad_prefs";

    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_LINE_SPACING = "line_spacing";
    private static final String KEY_EDITOR_BG = "editor_bg";
    private static final String KEY_SORT = "sort";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_AUTO_BACKUP = "auto_backup";
    private static final String KEY_DEFAULT_CATEGORY = "default_category";
    private static final String KEY_LAST_BACKUP = "last_backup";

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /* ---------------- 主题 ---------------- */

    public static int getTheme(Context context) {
        return prefs(context).getInt(KEY_THEME, THEME_LIGHT);
    }

    public static void setTheme(Context context, int theme) {
        prefs(context).edit().putInt(KEY_THEME, theme).apply();
    }

    /* ---------------- 编辑器 ---------------- */

    /** 正文字号（sp） */
    public static int getFontSize(Context context) {
        return prefs(context).getInt(KEY_FONT_SIZE, 17);
    }

    public static void setFontSize(Context context, int size) {
        prefs(context).edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    /** 行间距（百分比，100 表示默认） */
    public static int getLineSpacing(Context context) {
        return prefs(context).getInt(KEY_LINE_SPACING, 140);
    }

    public static void setLineSpacing(Context context, int percent) {
        prefs(context).edit().putInt(KEY_LINE_SPACING, percent).apply();
    }

    /** 纸张背景 */
    public static int getEditorBackground(Context context) {
        return prefs(context).getInt(KEY_EDITOR_BG, BG_PAPER);
    }

    public static void setEditorBackground(Context context, int value) {
        prefs(context).edit().putInt(KEY_EDITOR_BG, value).apply();
    }

    /* ---------------- 列表 ---------------- */

    public static int getSort(Context context) {
        return prefs(context).getInt(KEY_SORT, SORT_MODIFIED);
    }

    public static void setSort(Context context, int sort) {
        prefs(context).edit().putInt(KEY_SORT, sort).apply();
    }

    /** 对应 ContentProvider 的 order by 子句 */
    public static String getSortOrder(Context context) {
        switch (getSort(context)) {
            case SORT_CREATED:
                return NotePad.Notes.COLUMN_NAME_PINNED + " DESC, "
                        + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " DESC";
            case SORT_TITLE:
                return NotePad.Notes.COLUMN_NAME_PINNED + " DESC, "
                        + NotePad.Notes.COLUMN_NAME_TITLE + " COLLATE LOCALIZED ASC";
            case SORT_MODIFIED:
            default:
                return NotePad.Notes.DEFAULT_SORT_ORDER;
        }
    }

    /* ---------------- 行为 ---------------- */

    public static boolean isAutoSave(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SAVE, true);
    }

    public static void setAutoSave(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_SAVE, value).apply();
    }

    public static boolean isAutoBackup(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_BACKUP, false);
    }

    public static void setAutoBackup(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP, value).apply();
    }

    /** 新建笔记的默认分类 */
    public static String getDefaultCategory(Context context) {
        return prefs(context).getString(KEY_DEFAULT_CATEGORY, "");
    }

    public static void setDefaultCategory(Context context, String category) {
        prefs(context).edit().putString(KEY_DEFAULT_CATEGORY, category).apply();
    }

    /* ---------------- 备份 ---------------- */

    public static long getLastBackupTime(Context context) {
        return prefs(context).getLong(KEY_LAST_BACKUP, 0L);
    }

    public static void setLastBackupTime(Context context, long time) {
        prefs(context).edit().putLong(KEY_LAST_BACKUP, time).apply();
    }

    /** 恢复默认设置 */
    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }
}
