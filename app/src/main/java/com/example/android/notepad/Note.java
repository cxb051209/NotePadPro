package com.example.android.notepad;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 笔记实体类。
 */
public class Note {

    public long id;
    public String title = "";
    public String content = "";
    public long created;
    public long modified;
    public String category = "";
    public boolean pinned;

    public Note() {
    }

    /**
     * 正文摘要：把换行压成空格，最多保留 120 个字符。
     */
    public String snippet() {
        if (content == null) {
            return "";
        }
        String text = content.replace("\r", " ").replace("\n", " ").trim();
        if (text.length() > 120) {
            text = text.substring(0, 120) + "…";
        }
        return text;
    }

    /**
     * 正文字数（不含空白字符）。
     */
    public int wordCount() {
        if (content == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c)) {
                count++;
            }
        }
        return count;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("title", title == null ? "" : title);
        o.put("content", content == null ? "" : content);
        o.put("created", created);
        o.put("modified", modified);
        o.put("category", category == null ? "" : category);
        o.put("pinned", pinned ? 1 : 0);
        return o;
    }

    public static Note fromJson(JSONObject o) {
        Note note = new Note();
        note.title = o.optString("title", "");
        note.content = o.optString("content", "");
        note.created = o.optLong("created", System.currentTimeMillis());
        note.modified = o.optLong("modified", note.created);
        note.category = o.optString("category", "");
        note.pinned = o.optInt("pinned", 0) == 1;
        return note;
    }
}
