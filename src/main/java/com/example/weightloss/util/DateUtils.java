package com.example.weightloss.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 日期工具：按“天”归一化时间戳并做中文格式化。 */
public final class DateUtils {

    private static final Locale LOCALE = Locale.CHINA;

    private DateUtils() {
    }

    /** 归一到当天零点，用于按天比较。 */
    public static long normalizeDate(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long today() {
        return normalizeDate(System.currentTimeMillis());
    }

    public static boolean isSameDay(long first, long second) {
        return normalizeDate(first) == normalizeDate(second);
    }

    public static boolean isToday(long millis) {
        return isSameDay(millis, System.currentTimeMillis());
    }

    public static String formatFull(long millis) {
        return new SimpleDateFormat("yyyy年M月d日 EEE", LOCALE).format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("M月d日", LOCALE).format(new Date(millis));
    }

    public static String formatListDate(long millis) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        int year = date.get(Calendar.YEAR);
        int currentYear = today.get(Calendar.YEAR);
        String pattern = year == currentYear ? "M月d日" : "yyyy年M月d日";
        return new SimpleDateFormat(pattern, LOCALE).format(new Date(millis));
    }

    public static String formatDayNumber(long millis) {
        return new SimpleDateFormat("d", LOCALE).format(new Date(millis));
    }

    public static String formatMonthWeek(long millis) {
        return new SimpleDateFormat("M月 · EEE", LOCALE).format(new Date(millis));
    }

    public static int[] getYearMonth(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)};
    }

    public static String formatWeekday(long millis) {
        return new SimpleDateFormat("EEE", LOCALE).format(new Date(millis));
    }

    public static String formatRelative(long millis) {
        long day = normalizeDate(millis);
        long today = today();
        long diff = today - day;
        long oneDay = 24L * 60L * 60L * 1000L;
        if (diff == 0) {
            return "今天";
        }
        if (diff == oneDay) {
            return "昨天";
        }
        return formatListDate(millis);
    }

    public static int[] getDateParts(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return new int[]{
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        };
    }

    public static long fromDateParts(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}

