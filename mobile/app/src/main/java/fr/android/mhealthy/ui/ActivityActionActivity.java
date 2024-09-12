package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;

public class ActivityActionActivity extends AppCompatActivity {
    private CaregiverDAO db;

    private Session session;
    private Patient patient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_activity);

        Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra("session");
        patient = (Patient) intent.getSerializableExtra("patient");

        db = new CaregiverDAO(getApplicationContext(), session);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);

        String[] options = getResources().getStringArray(R.array.activities_options);
        ArrayAdapter<String> actadapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
        MaterialAutoCompleteTextView acts = findViewById(R.id.etActName);
        acts.setAdapter(actadapter);

        TextView tvActTitle = findViewById(R.id.tvActTitle);
        MaterialButton btnAddAct = findViewById(R.id.btnAddAct);
        if (intent.getBooleanExtra("add", false)) {
            tvActTitle.setText(getString(R.string.add_act));
            btnAddAct.setText(getString(R.string.add_act));
        } else {
            tvActTitle.setText(getString(R.string.edit_act));
            btnAddAct.setText(getString(R.string.edit_act));
        }
    }
}
