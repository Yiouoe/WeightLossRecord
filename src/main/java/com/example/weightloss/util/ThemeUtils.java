package com.example.weightloss.util;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.weightloss.R;

/** 主题工具：把主题模式应用到全局日夜模式。 */
public final class ThemeUtils {

    private ThemeUtils() {
    }

    public static void apply(int themeMode) {
        switch (themeMode) {
            case Preferences.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Preferences.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int labelRes(int themeMode) {
        switch (themeMode) {
            case Preferences.THEME_LIGHT:
                return R.string.theme_light;
            case Preferences.THEME_DARK:
                return R.string.theme_dark;
            default:
                return R.string.theme_system;
        }
    }
}
