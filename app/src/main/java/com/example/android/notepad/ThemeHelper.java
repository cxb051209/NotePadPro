package com.example.android.notepad;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;

/**
 * 主题工具：负责在 Activity 创建前套用用户选择的主题，并提供取色能力。
 */
public class ThemeHelper {

    private ThemeHelper() {
    }

    /** 主题 id → 样式资源 */
    public static int themeRes(int theme) {
        switch (theme) {
            case AppPrefs.THEME_DARK:
                return R.style.Theme_NotePad_Dark;
            case AppPrefs.THEME_GREEN:
                return R.style.Theme_NotePad_Green;
            case AppPrefs.THEME_OCEAN:
                return R.style.Theme_NotePad_Ocean;
            case AppPrefs.THEME_LIGHT:
            default:
                return R.style.Theme_NotePad;
        }
    }

    /**
     * 在 super.onCreate() 之前调用，保证控件按所选主题创建。
     */
    public static void apply(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).setTheme(themeRes(AppPrefs.getTheme(context)));
        }
    }

    public static boolean isDark(Context context) {
        return AppPrefs.getTheme(context) == AppPrefs.THEME_DARK;
    }

    /** 读取主题属性对应的颜色 */
    public static int color(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(context, value.resourceId);
            }
            return value.data;
        }
        return 0xFF000000;
    }

    public static int surface(Context context) {
        return color(context, com.google.android.material.R.attr.colorSurface);
    }

    public static int onSurface(Context context) {
        return color(context, com.google.android.material.R.attr.colorOnSurface);
    }

    public static int onSurfaceVariant(Context context) {
        return color(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
    }

    public static int primary(Context context) {
        return color(context, com.google.android.material.R.attr.colorPrimary);
    }

    /**
     * 编辑器背景色：默认与页面背景一致，随主题切换。
     */
    public static int editorBackgroundColor(Context context) {
        return color(context, com.google.android.material.R.attr.colorSurface);
    }

    /** 统一给菜单图标上色，保证深色主题下也可见 */
    public static void tintMenuIcons(Menu menu, int color) {
        if (menu == null) {
            return;
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable icon = item.getIcon();
            if (icon != null) {
                icon.mutate().setTint(color);
                item.setIcon(icon);
            }
        }
    }

    public static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, context.getResources().getDisplayMetrics()));
    }
}
