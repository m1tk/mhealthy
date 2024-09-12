package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;

public class ActivityActionActivity extends AppCompatActivity {
    private CaregiverDAO db;

    private Session session;
    private Patient patient;

    String activity_id = null;

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
        MaterialAutoCompleteTextView act_name = findViewById(R.id.etActName);
        act_name.setAdapter(actadapter);

        TextInputEditText act_goal = findViewById(R.id.etActGoal);

        act_name.setOnItemClickListener((parent, view, position, id) -> {
            activity_id = String.valueOf(position);
            act_goal.setEnabled(false);
            act_goal.setText("");
        });

        TextView tvActTitle = findViewById(R.id.tvActTitle);
        MaterialButton btnAddAct = findViewById(R.id.btnAddAct);
        if (intent.getBooleanExtra("add", false)) {
            tvActTitle.setText(getString(R.string.add_act));
            btnAddAct.setText(getString(R.string.add_act));
        } else {
            tvActTitle.setText(getString(R.string.edit_act));
            btnAddAct.setText(getString(R.string.edit_act));
        }

        btnAddAct.setOnClickListener(v -> {
            if (activity_id == null) {
                Toast.makeText(this, getString(R.string.empty_field), Toast.LENGTH_SHORT).show();
                return;
            }
            int hour   = tp.getHour();
            int minute = tp.getMinute();
            String time = String.format("%02d:%02d", hour, minute);
            if (intent.getBooleanExtra("add", false)) {
                add_activity(
                        activity_id,
                        act_goal.getText().toString(),
                        time
                );
            } else {
                edit_activity(
                        activity_id,
                        act_goal.getText().toString(),
                        time
                );
            }
        });
    }

    private void add_activity(String name, String goal, String time) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.pending))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            Instruction.AddActivity add = new Instruction.AddActivity();
            add.activity_time = time;
            add.name = name;
            add.goal = goal;
            add.time = System.currentTimeMillis() / 1000;
            Instruction ins = new Instruction(
                    Instruction.InstructionType.AddActivity,
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
                try {
                    add.name = getResources().getStringArray(R.array.activities_options)[Integer.parseInt(add.name)];
                } catch (NumberFormatException e) {}
                Toast.makeText(
                        this,
                        getString(R.string.med_assigned_patient, name, patient.name),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            });
        }).start();
    }

    private void edit_activity(String name, String goal, String time) {
    }
}
