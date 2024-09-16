package fr.android.mhealthy.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

import fr.android.mhealthy.R;

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

        FloatingActionButton dismiss = findViewById(R.id.dismissButton);

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
    }
}
