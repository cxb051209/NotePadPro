package com.example.android.notepad;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 分类实体：名称 + 颜色。
 */
public class Category {

    public final String name;
    public final int color;

    public Category(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", name);
            object.put("color", color);
        } catch (JSONException ignored) {
            // 不可能发生：key 都是常量
        }
        return object;
    }

    public static Category fromJson(JSONObject object) {
        if (object == null) {
            return new Category(CategoryManager.UNCATEGORIZED, 0xFF8F9BA8);
        }
        return new Category(object.optString("name", CategoryManager.UNCATEGORIZED),
                object.optInt("color", 0xFF8F9BA8));
    }
}
