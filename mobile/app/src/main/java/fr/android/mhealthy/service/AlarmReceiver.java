package fr.android.mhealthy.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.WindowManager;

import fr.android.mhealthy.ui.AlarmActivity;

public class AlarmReceiver extends BroadcastReceiver {
    public static PowerManager.WakeLock wakeLock;
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        wakeLock = pm.newWakeLock(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                context.getPackageName() + ":WakeLock"
        );
        wakeLock.acquire();

        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra("medicine", intent.getBooleanExtra("medicine", true));
        alarmIntent.putExtra("name", intent.getStringExtra("name"));
        alarmIntent.putExtra("desc", intent.getStringExtra("desc"));
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmIntent);
    }
}