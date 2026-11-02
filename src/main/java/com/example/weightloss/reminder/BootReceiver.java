package com.example.weightloss.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机广播：重启后重新注册每日提醒。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.scheduleIfEnabled(context);
    }
}
