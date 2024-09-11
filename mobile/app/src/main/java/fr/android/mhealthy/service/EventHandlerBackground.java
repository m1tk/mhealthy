package fr.android.mhealthy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import fr.android.mhealthy.R;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.model.PendingTransactionNotification;
import fr.android.mhealthy.model.Session;

public class EventHandlerBackground extends Service {
    private static final String CHANNEL_ID = "EventHandler";
    private static boolean isServiceRunning = false;

    static ConcurrentHashMap<Thread, Optional<SSE>> tasks = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        EventBus.getDefault().register(this);
        isServiceRunning = true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "mhealthy",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("mhealty")
                .setContentText("Fetching latest info")
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // Replace with your icon
                .build();

        startForeground(1, notification);

        SessionManager m;
        try {
            m = new SessionManager(getApplicationContext());
        } catch (IOException e) {
            return START_NOT_STICKY;
        }
        Session s = m.get_logged_session();
        if (s == null) {
            return START_NOT_STICKY;
        }
        new Thread(() -> {
            if (s.account_type.equals("caregiver")) {
                new CaregiverEvents(getApplicationContext(), s);
            } else if (s.account_type.equals("patient")) {
                new PatientEvents(getApplicationContext(), s);
            }
        }).start();

        new Thread(() -> {
            new TransactionHandler(getApplicationContext(), s);
        }).start();

        // If the service is killed, restart it with the last intent
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void pending_transaction(PendingTransactionNotification p) {
        if (TransactionHandler.update_id.get() < p.id) {
            TransactionHandler.update_id.set(p.id);
        }
    }

    public static class StopForegroundTask {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void pending_transaction(StopForegroundTask s) {
        tasks.forEach((t, sse) -> {
            t.interrupt();
            if (sse.isPresent()) {
                try {
                    sse.get().close();
                } catch (Exception e) {}
            }
        });
        for (Thread t: tasks.keySet()) {
            try {
                t.join(1000);
            } catch (Exception e) {}
        }
        tasks.clear();
        Log.d("ForegroundTask", "All tasks stopped, exiting.");
        stopSelf();
    }
}