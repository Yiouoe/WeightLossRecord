package com.example.weightloss.util;

import android.content.Context;
import android.content.SharedPreferences;

/** 偏好设置封装：保存身高、目标体重、主题与提醒配置。 */
public class Preferences {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String FILE_NAME = "weight_loss_prefs";
    private static final String KEY_HEIGHT = "height_cm";
    private static final String KEY_TARGET = "target_weight";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private final SharedPreferences preferences;

    public Preferences(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public float getHeightCm() {
        return preferences.getFloat(KEY_HEIGHT, 170f);
    }

    public float getTargetWeight() {
        return preferences.getFloat(KEY_TARGET, 0f);
    }

    public boolean hasTarget() {
        return getTargetWeight() > 0;
    }

    public void saveProfile(float heightCm, float targetWeight) {
        preferences.edit()
                .putFloat(KEY_HEIGHT, heightCm)
                .putFloat(KEY_TARGET, targetWeight)
                .apply();
    }

    public int getThemeMode() {
        return preferences.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setThemeMode(int themeMode) {
        preferences.edit().putInt(KEY_THEME, themeMode).apply();
    }

    public boolean isReminderEnabled() {
        return preferences.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public void setReminderEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public int getReminderHour() {
        return preferences.getInt(KEY_REMINDER_HOUR, 20);
    }

    public int getReminderMinute() {
        return preferences.getInt(KEY_REMINDER_MINUTE, 0);
    }

    public void setReminderTime(int hour, int minute) {
        preferences.edit()
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();
    }
}
