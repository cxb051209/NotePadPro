package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类管理：分类是用户可自定义的数据，结构与笔记本身独立，
 * 因此保存在 SharedPreferences 的 JSON 中（避免为一个很小的表再建一套表结构）。
 */
public class CategoryManager {

    /** 兜底分类，不允许删除 */
    public static final String UNCATEGORIZED = "未分类";

    private static final String PREF_NAME = "notepad_categories";
    private static final String KEY_LIST = "list";

    private static CategoryManager sInstance;

    private final SharedPreferences prefs;
    private List<Category> cache;

    private CategoryManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized CategoryManager get(Context context) {
        if (sInstance == null) {
            sInstance = new CategoryManager(context);
        }
        return sInstance;
    }

    /** 全部分类（副本） */
    public List<Category> getCategories() {
        return new ArrayList<>(loaded());
    }

    /** 分类名列表 */
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (Category category : loaded()) {
            names.add(category.name);
        }
        return names;
    }

    public Category find(String name) {
        if (name == null) {
            return null;
        }
        for (Category category : loaded()) {
            if (category.name.equals(name)) {
                return category;
            }
        }
        return null;
    }

    /** 分类颜色，找不到时返回灰色 */
    public int colorOf(String name) {
        Category category = find(name);
        if (category != null) {
            return category.color;
        }
        return find(UNCATEGORIZED) != null
                ? find(UNCATEGORIZED).color : 0xFF8F9BA8;
    }

    public boolean exists(String name) {
        return find(name) != null;
    }

    /** 新增分类，重名或空名返回 false */
    public boolean add(String name, int color) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (exists(trimmed)) {
            return false;
        }
        List<Category> list = loaded();
        list.add(new Category(trimmed, color));
        save(list);
        return true;
    }

    /** 重命名分类；「未分类」不允许改名 */
    public boolean rename(String oldName, String newName, int color) {
        if (TextUtils.isEmpty(newName) || UNCATEGORIZED.equals(oldName)) {
            return false;
        }
        String trimmed = newName.trim();
        List<Category> list = loaded();
        for (int i = 0; i < list.size(); i++) {
            Category category = list.get(i);
            if (category.name.equals(oldName)) {
                list.set(i, new Category(trimmed, color));
                save(list);
                return true;
            }
            if (category.name.equals(trimmed) && !trimmed.equals(oldName)) {
                return false;
            }
        }
        return false;
    }

    /** 修改分类颜色 */
    public void updateColor(String name, int color) {
        List<Category> list = loaded();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(name)) {
                list.set(i, new Category(name, color));
                save(list);
                return;
            }
        }
    }

    /** 删除分类；「未分类」与最后一个分类不允许删除 */
    public boolean remove(String name) {
        if (UNCATEGORIZED.equals(name)) {
            return false;
        }
        List<Category> list = loaded();
        if (list.size() <= 1) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(name)) {
                list.remove(i);
                save(list);
                return true;
            }
        }
        return false;
    }

    /** 备份恢复时，把新增的分类补进列表 */
    public void ensureCategories(List<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        List<Category> list = loaded();
        boolean changed = false;
        for (String name : names) {
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            boolean found = false;
            for (Category category : list) {
                if (category.name.equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(new Category(name, 0xFF8F9BA8));
                changed = true;
            }
        }
        if (changed) {
            save(list);
        }
    }

    /** 恢复默认分类 */
    public void reset() {
        cache = defaults();
        save(cache);
    }

    /* ---------------- 内部实现 ---------------- */

    private List<Category> loaded() {
        if (cache == null) {
            cache = load();
        }
        return cache;
    }

    private List<Category> load() {
        String json = prefs.getString(KEY_LIST, null);
        if (!TextUtils.isEmpty(json)) {
            try {
                JSONArray array = new JSONArray(json);
                List<Category> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    list.add(Category.fromJson(array.optJSONObject(i)));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (JSONException ignored) {
                // 数据损坏时回退到默认分类
            }
        }
        return defaults();
    }

    private void save(List<Category> list) {
        JSONArray array = new JSONArray();
        for (Category category : list) {
            array.put(category.toJson());
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply();
        cache = list;
    }

    private List<Category> defaults() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("工作", 0xFF5B7FFF));
        list.add(new Category("学习", 0xFFFF9F43));
        list.add(new Category("生活", 0xFF26C6DA));
        list.add(new Category("灵感", 0xFFAB47BC));
        list.add(new Category(UNCATEGORIZED, 0xFF8F9BA8));
        return list;
    }
}
