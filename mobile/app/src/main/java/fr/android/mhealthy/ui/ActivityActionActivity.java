package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
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

        String[] options = get_options(getApplicationContext());
        ArrayAdapter<String> actadapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
        MaterialAutoCompleteTextView act_name = findViewById(R.id.etActName);
        act_name.setAdapter(actadapter);

        TextInputEditText act_goal = findViewById(R.id.etActGoal);

        act_name.setOnItemClickListener((parent, view, position, id) -> {
            activity_id = String.valueOf(position);
            act_goal.setText("");
        });

        TextView tvActTitle = findViewById(R.id.tvActTitle);
        MaterialButton btnAddAct = findViewById(R.id.btnAddAct);
        MaterialButton btnRemoveAct = findViewById(R.id.btnRemoveAct);
        if (intent.getBooleanExtra("add", false)) {
            tvActTitle.setText(getString(R.string.add_act));
            btnAddAct.setText(getString(R.string.add_act));
            act_name.setEnabled(true);
            act_goal.setEnabled(false);
            btnRemoveAct.setVisibility(View.GONE);
        } else {
            Activity act = (Activity) intent.getSerializableExtra("activity");
            tvActTitle.setText(getString(R.string.edit_act));
            btnAddAct.setText(getString(R.string.edit_act));
            act_name.setEnabled(false);
            act_goal.setEnabled(false);
            activity_id = act.name;
            String name_i = act.name;
            try {
                name_i = options[Integer.parseInt(act.name)];
            } catch (NumberFormatException e) {}
            act_name.setText(name_i);
            act_goal.setText(act.goal);
            try {
                tp.setHour(Integer.parseInt(act.time.split(":")[0]));
                tp.setMinute(Integer.parseInt(act.time.split(":")[1]));
            } catch (NumberFormatException e) {}
            btnRemoveAct.setVisibility(View.VISIBLE);
        }

        btnAddAct.setOnClickListener(v -> {
            if (activity_id == null) {
                Toast.makeText(this, getString(R.string.empty_field), Toast.LENGTH_SHORT).show();
                return;
            }
            int hour   = tp.getHour();
            int minute = tp.getMinute();
            String time = String.format("%02d:%02d", hour, minute);
            Instruction ins;
            if (intent.getBooleanExtra("add", false)) {
                Instruction.AddActivity add = new Instruction.AddActivity();
                add.activity_time = time;
                add.name = activity_id;
                add.goal = act_goal.getText().toString();
                add.time = System.currentTimeMillis() / 1000;
                ins = new Instruction(
                        Instruction.InstructionType.AddActivity,
                        add,
                        session.id,
                        0
                );
            } else {
                Instruction.EditActivity edit = new Instruction.EditActivity();
                edit.activity_time = time;
                edit.name = activity_id;
                edit.goal = act_goal.getText().toString();
                edit.time = System.currentTimeMillis() / 1000;
                ins = new Instruction(
                        Instruction.InstructionType.EditActivity,
                        edit,
                        session.id,
                        0
                );
                Intent returnIntent = new Intent();
                returnIntent.putExtra("goal", edit.goal);
                returnIntent.putExtra("time", edit.activity_time);
                setResult(android.app.Activity.RESULT_OK, returnIntent);
            }
            activity_operation(ins, act_name.getText().toString());
        });

        btnRemoveAct.setOnClickListener(v -> {
            Instruction.RemoveActivity rm = new Instruction.RemoveActivity();
            rm.name = activity_id;
            rm.time = System.currentTimeMillis() / 1000;
            Instruction ins = new Instruction(
                    Instruction.InstructionType.RemoveActivity,
                    rm,
                    session.id,
                    0
            );
            activity_operation(ins, act_name.getText().toString());
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

    public static String[] get_options(Context ctx) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        int id;
        if (p.getString("language_preference", "en").equals("en")) {
            id = R.array.activities_options_en;
        } else {
            id = R.array.activities_options_fr;
        }
        return ctx.getResources().getStringArray(id);
    }

    private void activity_operation(Instruction ins, String name) {
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
                    err_msg = getString(R.string.act_add_err);
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
                    case AddActivity:
                        str_show_id = R.string.med_assigned_patient;
                        break;
                    case EditActivity:
                        str_show_id = R.string.act_modify;
                        break;
                    default:
                        str_show_id = R.string.act_remove;
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
