package fr.android.mhealthy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.android.mhealthy.R;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.model.PendingTransactionNotification;
import fr.android.mhealthy.model.Session;

public class EventHandlerBackground extends Service {
    private static final String CHANNEL_ID = "EventHandler";
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private final Semaphore mutex = new Semaphore(1, true);
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Session s;

    static ConcurrentHashMap<Thread, Optional<SSE>> tasks = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        EventBus.getDefault().register(this);
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
        if (!running.compareAndSet(false, true)) {
            return START_NOT_STICKY;
        }
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
        s = m.get_logged_session();
        if (s == null) {
            return START_NOT_STICKY;
        }

        if (s.id != -2) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    try {
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        return;
                    }
                    Log.d("ForegroundTask", "Starting background tasks.");
                    start_tasks(s);
                    mutex.release();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    try {
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        return;
                    }
                    Log.d("ForegroundTask", "Stopping background tasks due to no network activity");
                    stop_tasks();
                    mutex.release();
                }
            };

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .build();

            connectivityManager = getSystemService(ConnectivityManager.class);
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }

        if (s.account_type.equals("patient")
            || s.account_type.equals("selfcarepatient")) {
            PatientAlarmScheduler.schedule(getApplicationContext(), s);
        }

        // If the service is killed, restart it with the last intent
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        running.set(false);
    }

    public static boolean isServiceRunning() {
        return running.get();
    }

    private void start_tasks(Session s) {
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
    }

    private void stop_tasks() {
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
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void alarm_update(PatientAlarmScheduler.Updated updated) {
        if (s != null) {
            PatientAlarmScheduler.schedule(getApplicationContext(), s);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void alarm_update2(PatientAlarmScheduler.Reschedule re) {
        if (s != null) {
            PatientAlarmScheduler.reschedule(getApplicationContext(), re.key);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void pending_transaction(PendingTransactionNotification p) {
        if (TransactionHandler.update_id.get() < p.id) {
            TransactionHandler.update_id.set(p.id);
        }
    }

    public static class StopForegroundTask {}
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void stop_tasks_event(StopForegroundTask s) {
        stop_tasks();
        Log.d("ForegroundTask", "All tasks stopped, exiting.");
        stopSelf();
    }

    public static class NewNotificationTask {
        public int title;
        public int id;
        public String[] args;
        public NewNotificationTask(int title, int id, String... args) {
            this.title = title;
            this.id = id;
            this.args = args;
        }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void new_notification_event(NewNotificationTask n) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String lang = p.getString("language_preference", "en");
        Locale loc  = new Locale(lang);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(loc);
        Resources localizedResources = createConfigurationContext(configuration).getResources();
        String tit_str = localizedResources.getString(n.title);
        String string = localizedResources.getString(n.id, (Object[])n.args);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(tit_str)
                .setContentText(string)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }
}