package fr.android.mhealthy.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.android.mhealthy.R;

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        FloatingActionButton dismissButton = findViewById(R.id.dismissButton);
        FloatingActionButton snoozeButton = findViewById(R.id.snoozeButton);

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle dismiss action
                finish();
            }
        });

        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle snooze action
                // You might want to reschedule the alarm here
                finish();
            }
        });
    }
}
