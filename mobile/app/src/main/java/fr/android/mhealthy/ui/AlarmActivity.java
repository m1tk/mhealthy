package fr.android.mhealthy.ui;

import static fr.android.mhealthy.ui.ActivityActionActivity.get_options;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import fr.android.mhealthy.R;
import fr.android.mhealthy.service.AlarmReceiver;
import fr.android.mhealthy.service.PatientAlarmScheduler;

public class AlarmActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        TextView name = findViewById(R.id.alarmName);
        TextView desc = findViewById(R.id.alarmDescription);
        FloatingActionButton dismiss = findViewById(R.id.dismissButton);

        boolean is_med = getIntent().getBooleanExtra("medicine", true);
        String name_str = getIntent().getStringExtra("name");
        if (!is_med) {
            try {
                name_str = get_options(getApplicationContext())[Integer.parseInt(name_str)];
            } catch (NumberFormatException e) {}
        }
        String desc_str = getIntent().getStringExtra("desc");
        name.setText(name_str);
        desc.setText(is_med ? getString(R.string.dose, desc_str)
                            : (desc_str.isEmpty() ? getString(R.string.no_goal)
                                                  : getString(R.string.goal, desc_str)));

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Play alarm sound
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mediaPlayer.setLooping(true);
        try {
            mediaPlayer.setDataSource(getApplicationContext(),
                    Uri.parse("android.resource://"+getPackageName()+"/" + R.raw.oberon_48k));
            mediaPlayer.prepare();
        } catch (IOException e) {}
        mediaPlayer.start();
        vibrator.vibrate(new long[]{1000, 1000}, 0);

        dismiss.setOnClickListener(v -> stopAlarm());

        // Stop the alarm after 1 minute
        new Handler().postDelayed(this::stopAlarm, 60 * 1000);
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
        AlarmReceiver.wakeLock.release();
        AlarmReceiver.wakeLock = null;

        EventBus.getDefault().post(new PatientAlarmScheduler.Reschedule(
                new PatientAlarmScheduler.Key(
                        getIntent().getStringExtra("name"),
                        getIntent().getBooleanExtra("medicine", true)
                                ? PatientAlarmScheduler.EventType.Medicine
                                : PatientAlarmScheduler.EventType.Activity
                )
        ));
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        // Check if the new intent is from the alarm
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // Finish the current instance of the activity
            finish();

            // Start a new instance of the activity with the new intent
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
