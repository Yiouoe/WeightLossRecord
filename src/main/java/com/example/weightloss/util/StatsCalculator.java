package com.example.weightloss.util;

import com.example.weightloss.model.WeightRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 统计工具：打卡天数、连续天数、均值与体重变化等。 */
public final class StatsCalculator {

    private static final long ONE_DAY = 24L * 60L * 60L * 1000L;

    private StatsCalculator() {
    }

    public static int distinctDays(List<WeightRecord> records) {
        Set<Long> days = new HashSet<>();
        for (WeightRecord record : records) {
            days.add(DateUtils.normalizeDate(record.getDate()));
        }
        return days.size();
    }

    public static int currentStreak(List<WeightRecord> records) {
        Set<Long> days = new HashSet<>();
        for (WeightRecord record : records) {
            days.add(DateUtils.normalizeDate(record.getDate()));
        }
        if (days.isEmpty()) {
            return 0;
        }

        long today = DateUtils.today();
        long cursor;
        if (days.contains(today)) {
            cursor = today;
        } else if (days.contains(today - ONE_DAY)) {
            cursor = today - ONE_DAY;
        } else {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor -= ONE_DAY;
        }
        return streak;
    }

    public static int bestStreak(List<WeightRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        Set<Long> days = new HashSet<>();
        for (WeightRecord record : records) {
            days.add(DateUtils.normalizeDate(record.getDate()));
        }

        List<Long> sorted = new ArrayList<>(days);
        sorted.sort(Long::compareTo);

        int best = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) - sorted.get(i - 1) == ONE_DAY) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 1;
            }
        }
        return best;
    }

    public static List<WeightRecord> lastDays(List<WeightRecord> records, int days) {
        long start = DateUtils.today() - (days - 1L) * ONE_DAY;
        List<WeightRecord> result = new ArrayList<>();
        for (WeightRecord record : records) {
            long date = DateUtils.normalizeDate(record.getDate());
            if (date >= start) {
                result.add(record);
            }
        }
        return result;
    }

    public static double average(List<WeightRecord> records) {
        if (records.isEmpty()) {
            return Double.NaN;
        }
        double sum = 0;
        for (WeightRecord record : records) {
            sum += record.getWeight();
        }
        return sum / records.size();
    }

    public static double changeInLastDays(List<WeightRecord> records, int days) {
        List<WeightRecord> recent = lastDays(records, days);
        if (recent.size() < 2) {
            return Double.NaN;
        }
        return recent.get(recent.size() - 1).getWeight() - recent.get(0).getWeight();
    }

    public static double averagePreviousDays(List<WeightRecord> records, int days) {
        long today = DateUtils.today();
        long currentStart = today - (days - 1L) * ONE_DAY;
        long previousStart = currentStart - days * ONE_DAY;
        List<WeightRecord> previous = new ArrayList<>();
        for (WeightRecord record : records) {
            long date = DateUtils.normalizeDate(record.getDate());
            if (date >= previousStart && date < currentStart) {
                previous.add(record);
            }
        }
        return average(previous);
    }

    public static double[] healthyWeightRange(float heightCm) {
        if (heightCm <= 0) {
            return new double[]{0, 0};
        }
        double heightM = heightCm / 100.0;
        return new double[]{18.5 * heightM * heightM, 24.0 * heightM * heightM};
    }

    public static boolean hasRecordOn(long day, List<WeightRecord> records) {
        long normalized = DateUtils.normalizeDate(day);
        for (WeightRecord record : records) {
            if (DateUtils.normalizeDate(record.getDate()) == normalized) {
                return true;
            }
        }
        return false;
    }

    public static long startOfWeek(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return DateUtils.normalizeDate(calendar.getTimeInMillis());
    }
}
