package com.example.weightloss.util;

import com.example.weightloss.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** 健康指标工具：体重/BMI 的格式化、分级与配色。 */
public final class HealthUtils {

    private static final DecimalFormat WEIGHT_FORMAT = new DecimalFormat(
            "0.0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat BMI_FORMAT = new DecimalFormat(
            "0.0", DecimalFormatSymbols.getInstance(Locale.US));

    private HealthUtils() {
    }

    public static String formatWeight(double weight) {
        return WEIGHT_FORMAT.format(weight);
    }

    public static String formatDelta(double delta) {
        return WEIGHT_FORMAT.format(Math.abs(delta));
    }

    public static double calculateBmi(double weightKg, double heightCm) {
        if (heightCm <= 0) {
            return 0;
        }
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    public static String formatBmi(double bmi) {
        if (bmi <= 0) {
            return "--";
        }
        return BMI_FORMAT.format(bmi);
    }

    public static int bmiCategoryRes(double bmi) {
        if (bmi < 18.5) {
            return R.string.bmi_underweight;
        }
        if (bmi < 24) {
            return R.string.bmi_normal;
        }
        if (bmi < 28) {
            return R.string.bmi_overweight;
        }
        return R.string.bmi_obese;
    }

    public static int bmiColorRes(double bmi) {
        if (bmi < 18.5) {
            return R.color.gold;
        }
        if (bmi < 24) {
            return R.color.forest;
        }
        if (bmi < 28) {
            return R.color.gold;
        }
        return R.color.clay;
    }

    public static String formatPercent(int percent) {
        return String.format(Locale.CHINA, "%d%%", percent);
    }
}
