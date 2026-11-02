package com.example.weightloss;

import android.app.Application;
import android.content.Context;

import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.reminder.ReminderScheduler;
import com.example.weightloss.util.ThemeUtils;

/** 应用入口：初始化数据仓库、主题与每日提醒。 */
public class WeightApplication extends Application {

    private WeightRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化数据仓库并应用已保存的主题
        repository = new WeightRepository(this);
        ThemeUtils.apply(repository.getPreferences().getThemeMode());
        ReminderScheduler.createChannel(this);
        ReminderScheduler.scheduleIfEnabled(this);
    }

    public WeightRepository getRepository() {
        return repository;
    }

    /** 从任意 Context 获取全局仓库。 */
    public static WeightRepository repository(Context context) {
        return ((WeightApplication) context.getApplicationContext()).getRepository();
    }
}
