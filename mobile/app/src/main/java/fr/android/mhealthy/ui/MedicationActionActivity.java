package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra("session");
        patient = (Patient) intent.getSerializableExtra("patient");

        db = new CaregiverDAO(getApplicationContext(), session);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        MaterialButton btnAddMedication    = findViewById(R.id.btnAddMedication);
        MaterialButton btnRemoveMedication = findViewById(R.id.btnRemoveMedication);
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
            Instruction ins;
            if (intent.getBooleanExtra("add", false)) {
                Instruction.AddMedicine add = new Instruction.AddMedicine();
                add.dose_time = time;
                add.name = med_name.getText().toString();
                add.dose = med_dose.getText().toString();
                add.time = System.currentTimeMillis() / 1000;
                ins = new Instruction(
                        Instruction.InstructionType.AddMedicine,
                        add,
                        session.id,
                        0
                );
            } else {
                Instruction.EditMedicine edit = new Instruction.EditMedicine();
                edit.dose_time = time;
                edit.name = med_name.getText().toString();
                edit.dose = med_dose.getText().toString();
                edit.time = System.currentTimeMillis() / 1000;
                ins = new Instruction(
                        Instruction.InstructionType.EditMedicine,
                        edit,
                        session.id,
                        0
                );
            }
            medicine_operation(ins, med_name.getText().toString());
        });

        if (intent.getBooleanExtra("add", false)) {
            tvMedTitle.setText(getString(R.string.add_med));
            btnAddMedication.setText(getString(R.string.add_med));
            med_name.setEnabled(true);
            btnRemoveMedication.setVisibility(View.GONE);
        } else {
            Medicine med = (Medicine) intent.getSerializableExtra("medicine");
            tvMedTitle.setText(getString(R.string.edit_med));
            btnAddMedication.setText(getString(R.string.edit_med));
            med_name.setEnabled(false);
            med_name.setText(med.name);
            med_dose.setText(med.dose);
            try {
                tp.setHour(Integer.parseInt(med.time.split(":")[0]));
                tp.setMinute(Integer.parseInt(med.time.split(":")[1]));
            } catch (NumberFormatException e) {}
            btnRemoveMedication.setVisibility(View.VISIBLE);
        }

        btnRemoveMedication.setOnClickListener(v -> {
            Instruction.RemoveMedicine edit = new Instruction.RemoveMedicine();
            edit.name = med_name.getText().toString();
            edit.time = System.currentTimeMillis() / 1000;
            Instruction ins = new Instruction(
                    Instruction.InstructionType.RemoveMedicine,
                    edit,
                    session.id,
                    0
            );
            medicine_operation(ins, med_name.getText().toString());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void medicine_operation(Instruction ins, String name) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.pending))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            try {
                JsonObject op = ins.to_server_json_format(new Gson(), patient.id);
                db.instruction_operation(
                        ins,
                        op.get("data").getAsJsonObject().toString(),
                        op.toString(),
                        patient.id
                );
            } catch (Exception e) {
                String err_msg;
                if (e instanceof SQLiteConstraintException) {
                    err_msg = getString(R.string.med_add_err);
                } else {
                    err_msg = e.toString();
                }
                runOnUiThread(() -> {
                    dialog.dismiss();
                    AlertDialog err = new AlertDialog.Builder(this)
                            .setMessage(err_msg)
                            .create();
                    err.show();
                });
                return;
            }
            runOnUiThread(() -> {
                dialog.dismiss();
                int str_show_id;
                switch (ins.type) {
                    case AddMedicine:
                        str_show_id = R.string.med_assigned_patient;
                        break;
                    case EditMedicine:
                        str_show_id = R.string.med_modify;
                        break;
                    default:
                        str_show_id = R.string.med_remove;
                        break;
                }
                Toast.makeText(
                        this,
                        getString(str_show_id, name, patient.name),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            });
        }).start();
    }
}
