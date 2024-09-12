package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;

public class MedicationActionActivity extends AppCompatActivity {
    private CaregiverDAO db;

    private Session session;
    private Patient patient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_medicine);

        Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra("session");
        patient = (Patient) intent.getSerializableExtra("patient");

        db = new CaregiverDAO(getApplicationContext(), session);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        MaterialButton btnAddMedication = findViewById(R.id.btnAddMedication);
        TextView tvMedTitle = findViewById(R.id.tvMedTitle);

        TextInputEditText med_name = findViewById(R.id.etMedicationName);
        TextInputEditText med_dose = findViewById(R.id.etMedicationDose);

        btnAddMedication.setOnClickListener(v -> {
            if (med_name.getText() == null || med_name.getText().toString().isEmpty()
                    || med_dose.getText() == null || med_dose.getText().toString().isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_field), Toast.LENGTH_SHORT).show();
                return;
            }
            int hour   = tp.getHour();
            int minute = tp.getMinute();
            String time = String.format("%02d:%02d", hour, minute);
            if (intent.getBooleanExtra("add", false)) {
                add_medicine(
                        med_name.getText().toString(),
                        med_dose.getText().toString(),
                        time
                );
            } else {
                edit_medicine(
                        med_name.getText().toString(),
                        med_dose.getText().toString(),
                        time
                );
            }
        });

        if (intent.getBooleanExtra("add", false)) {
            tvMedTitle.setText(getString(R.string.add_med));
            btnAddMedication.setText(getString(R.string.add_med));
        } else {
            tvMedTitle.setText(getString(R.string.edit_med));
            btnAddMedication.setText(getString(R.string.edit_med));
        }
    }

    private void add_medicine(String name, String dose, String time) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.pending))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            Instruction.AddMedicine add = new Instruction.AddMedicine();
            add.dose_time = time;
            add.name = name;
            add.dose = dose;
            add.time = System.currentTimeMillis() / 1000;
            Instruction ins = new Instruction(
                    Instruction.InstructionType.AddMedicine,
                    add,
                    session.id,
                    0
            );
            try {
                JsonObject op = ins.to_server_json_format(new Gson(), patient.id);
                db.instruction_operation(
                        ins,
                        op.get("data").getAsJsonObject().toString(),
                        op.toString(),
                        patient.id
                );
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    AlertDialog err = new AlertDialog.Builder(this)
                            .setMessage(e.toString())
                            .create();
                    err.show();
                });
                return;
            }
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(
                        this,
                        getString(R.string.med_assigned_patient, add.name, patient.name),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            });
        }).start();
    }

    private void edit_medicine(String name, String dose, String time) {
    }
}
