package fr.android.mhealthy.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.TimePicker;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

import fr.android.mhealthy.R;

public class MedicationReminderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_reminder);

        Button setReminderButton = findViewById(R.id.setReminderButton);
        setReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                // Handle the time selected by the user
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                selectedTime.set(Calendar.SECOND, 0);

                if (selectedTime.getTimeInMillis() <= System.currentTimeMillis()) {
                    // If the selected time is in the past, add one day
                    selectedTime.add(Calendar.DAY_OF_MONTH, 1);
                }

                scheduleAlarm(MedicationReminderActivity.this, selectedTime.getTimeInMillis());
                Toast.makeText(MedicationReminderActivity.this,
                        "Reminder set for " + hourOfDay + ":" + minute,
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Instantiate the TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                MedicationReminderActivity.this,
                timeSetListener,
                hour,
                minute,
                true // 24-hour format
        );

        timePickerDialog.show();
    }


    private void scheduleAlarm(Context context, long timeInMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Cannot schedule exact alarm: permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

}
