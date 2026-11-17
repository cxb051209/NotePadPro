package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * NotePad 的数据契约类（在原生 NotePad 示例基础上扩展）。
 *
 * <p>在原示例只有 title / note / created / modified 的基础上，
 * 增加了 category（分类）与 pinned（置顶）两个字段，
 * 从而支持“笔记分类”“置顶排序”等扩展功能。</p>
 */
public final class NotePad {

    /** ContentProvider 的 authority，需与 AndroidManifest 中声明一致 */
    public static final String AUTHORITY = "com.example.android.notepad.provider";

    private NotePad() {
    }

    /**
     * notes 表契约
     */
    public static final class Notes implements BaseColumns {

        private Notes() {
        }

        /** 表名 */
        public static final String TABLE_NAME = "notes";

        private static final String SCHEME = "content://";
        private static final String PATH_NOTES = "/notes";
        private static final String PATH_NOTE_ID = "/notes/";

        /** URI 中 id 段的位置 */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /** 全部笔记 */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);

        /** 单条笔记 URI 前缀（后面拼接 id） */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);

        /** 单条笔记 URI 匹配模板 */
        public static final Uri CONTENT_ID_URI_PATTERN =
                Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "#");

        /** 笔记集合的 MIME 类型 */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.notepad.note";

        /** 单条笔记的 MIME 类型 */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.notepad.note";

        /* ---------------- 列名 ---------------- */

        /** 标题，TEXT */
        public static final String COLUMN_NAME_TITLE = "title";

        /** 正文，TEXT */
        public static final String COLUMN_NAME_NOTE = "note";

        /** 创建时间，INTEGER（System.currentTimeMillis()） */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /** 修改时间，INTEGER（System.currentTimeMillis()） */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";

        /** 分类名，TEXT */
        public static final String COLUMN_NAME_CATEGORY = "category";

        /** 是否置顶，INTEGER（0 / 1） */
        public static final String COLUMN_NAME_PINNED = "pinned";

        /** 默认排序：置顶优先，然后按修改时间倒序 */
        public static final String DEFAULT_SORT_ORDER =
                COLUMN_NAME_PINNED + " DESC, " + COLUMN_NAME_MODIFICATION_DATE + " DESC";
    }
}
