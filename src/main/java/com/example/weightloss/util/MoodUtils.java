package com.example.weightloss.util;

import com.example.weightloss.R;

/** 心情值到文案的映射与合法性校正。 */
public final class MoodUtils {

    private MoodUtils() {
    }

    public static int labelRes(int mood) {
        switch (mood) {
            case 0:
                return R.string.mood_relaxed;
            case 2:
                return R.string.mood_tired;
            default:
                return R.string.mood_normal;
        }
    }

    public static int normalize(int mood) {
        return mood < 0 || mood > 2 ? 1 : mood;
    }
}
