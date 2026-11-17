package com.example.android.notepad;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化工具（列表时间戳 / 备份时间等）。
 */
public class TimeUtils {

    private TimeUtils() {
    }

    private static SimpleDateFormat formatter(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault());
    }

    /**
     * 列表条目时间戳：今天 14:32 / 昨天 09:05 / 11-05 / 2025-12-01
     */
    public static String formatTimestamp(long time) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(time);

        Date date = new Date(time);
        if (isSameDay(now, target)) {
            return "今天 " + formatter("HH:mm").format(date);
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday, target)) {
            return "昨天 " + formatter("HH:mm").format(date);
        }

        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            return formatter("MM-dd HH:mm").format(date);
        }
        return formatter("yyyy-MM-dd").format(date);
    }

    /** 完整时间：yyyy-MM-dd HH:mm */
    public static String formatFull(long time) {
        return formatter("yyyy-MM-dd HH:mm").format(new Date(time));
    }

    /** 相对时间：刚刚 / 5 分钟前 / 3 小时前 / 2 天前 */
    public static String formatRelative(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < 0) {
            return formatFull(time);
        }
        if (diff < 60 * 1000L) {
            return "刚刚";
        }
        if (diff < 60 * 60 * 1000L) {
            return (diff / (60 * 1000L)) + " 分钟前";
        }
        if (diff < 24 * 60 * 60 * 1000L) {
            return (diff / (60 * 60 * 1000L)) + " 小时前";
        }
        if (diff < 30 * 24 * 60 * 60 * 1000L) {
            return (diff / (24 * 60 * 60 * 1000L)) + " 天前";
        }
        return formatFull(time);
    }

    /** 备份文件名使用的时间：yyyyMMdd_HHmmss */
    public static String fileStamp(long time) {
        return formatter("yyyyMMdd_HHmmss").format(new Date(time));
    }

    /** 备份展示用时间：yyyy-MM-dd HH:mm:ss */
    public static String formatSecond(long time) {
        return formatter("yyyy-MM-dd HH:mm:ss").format(new Date(time));
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
