package fr.android.mhealthy.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.android.mhealthy.ui.AlarmActivity;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmIntent);
    }
}