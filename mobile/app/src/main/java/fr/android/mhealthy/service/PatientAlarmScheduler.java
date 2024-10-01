package fr.android.mhealthy.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;

public class PatientAlarmScheduler {
    static Semaphore mutex = new Semaphore(1, true);
    static HashMap<Key, Value> events = new HashMap<>();

    public static class Updated {}
    public static class Reschedule {
        public Key key;
        public Reschedule(Key key) {
            this.key = key;
        }
    }

    public enum EventType {
        Medicine,
        Activity
    }

    public static class Key {
        String name;
        EventType type;
        public Key(String name, EventType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            return type == other.type && Objects.equals(name, other.name);
        }
    }

    static class Value {
        String description;
        int hour;
        int mins;
        int requestCode;
        PendingIntent intent;
        public Value(String description, int hour, int mins) {
            this.description = description;
            this.hour = hour;
            this.mins = mins;
            requestCode = 0;
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void schedule(Context ctx, Session s) {
        PatientDAO db = new PatientDAO(ctx, s);

        Integer id;
        if (s.account_type.equals("patient")) {
            id = null;
        } else {
            id = 0;
        }

        List<Medicine> meds = db.get_all_meds(id);
        List<Activity> acts = db.get_all_activities(id);

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            return;
        }
        for (Medicine med : meds) {
            Key key = new Key(med.name, EventType.Medicine);
            String[] parts = med.time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            Value val = new Value(med.dose, hours, minutes);
            schedule_inner(alarmManager, ctx, key, val, med.active, false);
        }

        for (Activity act : acts) {
            Key key = new Key(act.name, EventType.Activity);
            String[] parts = act.time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            Value val = new Value(act.goal, hours, minutes);
            schedule_inner(alarmManager, ctx, key, val, act.active, false);
        }

        mutex.release();
    }

    public static void reschedule(Context ctx, Key key) {
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            return;
        }
        Value val = events.get(key);
        schedule_inner(alarmManager, ctx, key, val, true, true);
        mutex.release();
    }

    private static void schedule_inner(AlarmManager alarmManager, Context ctx, Key key, Value val,
                                       boolean active, boolean reclock) {

        if (!active) {
            Value v = events.remove(key);
            if (v != null && v.intent != null) {
                alarmManager.cancel(v.intent);
            }
            return;
        }
        AtomicBoolean reschedule = new AtomicBoolean(false);
        Value f = events.putIfAbsent(key, val);
        if (f != null) {
            events.compute(key, (k, v) -> {
                if (!v.description.equals(val.description) || v.hour != val.hour || v.mins != val.mins) {
                    val.requestCode = v.requestCode;
                    reschedule.set(true);
                    return val;
                } else {
                    if (reclock) {
                        reschedule.set(true);
                    }
                    return v;
                }
            });
        } else {
            reschedule.set(true);
            for (int i = 1; i < 10000; i++) {
                boolean is_unique = true;
                for (Value v : events.values()) {
                    if (v.requestCode == i) {
                        is_unique = false;
                        break;
                    }
                }
                if (is_unique) {
                    val.requestCode = i;
                    events.compute(key, (k, v) -> val);
                    break;
                }
            }
        }

        if (!reschedule.get()) {
            return;
        }

        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra("medicine", key.type == EventType.Medicine);
        intent.putExtra("name", key.name);
        intent.putExtra("desc", val.description);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                ctx,
                val.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, val.hour);
        calendar.set(Calendar.MINUTE, val.mins);
        calendar.set(Calendar.SECOND, 1);

        // Skip today if clock time already past
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                pendingIntent
        );

        events.compute(key, (k, v) -> {
            v.intent = pendingIntent;
            return v;
        });
    }
}
