package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;

public class MedicationManagerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_manager);

        Intent intent   = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        FloatingActionButton fab = findViewById(R.id.add_med_fab);
        if (session.account_type.equals("caregiver")) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Patient p = (Patient) intent.getSerializableExtra("patient");
                Intent intent1 = new Intent(this, MedicationActionActivity.class);
                intent1.putExtra("add", true);
                intent1.putExtra("session", session);
                intent1.putExtra("patient", p);
                startActivity(intent1);
            });
        } else {
            fab.setVisibility(View.INVISIBLE);
        }
    }
}
