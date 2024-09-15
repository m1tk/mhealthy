package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;

public class MedicineActivity extends AppCompatActivity {
    PatientDAO pdb;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");
        Medicine medicine = (Medicine) intent.getSerializableExtra("medicine");

        TextView med_name = findViewById(R.id.tvMedTitle);
        med_name.setText(medicine.name);

        MaterialButton btnMedTaken = findViewById(R.id.btnMedTaken);
        FloatingActionButton edit   = findViewById(R.id.edit_act_fab);
        if (session.account_type.equals("caregiver")) {
            edit.setVisibility(View.VISIBLE);
            btnMedTaken.setVisibility(View.GONE);
            edit.setOnClickListener(v -> {
                Patient p = (Patient) intent.getSerializableExtra("patient");
                Intent intent1 = new Intent(this, MedicationActionActivity.class);
                intent1.putExtra("session", session);
                intent1.putExtra("patient", p);
                intent1.putExtra("medicine", medicine);
                startActivity(intent1);
            });
        } else {
            pdb = new PatientDAO(getApplicationContext(), session);
            edit.setVisibility(View.GONE);
            btnMedTaken.setVisibility(View.VISIBLE);
            btnMedTaken.setOnClickListener(v -> {
                medicine_taken(medicine.name);
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void medicine_taken(String name) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.pending))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            try {
                PatientInfo.MedicineTaken med = new PatientInfo.MedicineTaken();
                med.time = System.currentTimeMillis() / 1000;
                PatientInfo info = new PatientInfo(
                        PatientInfo.PatientInfoType.MedicineTaken,
                        med,
                        name,
                        0
                );
                JsonObject op = info.to_server_json_format(new Gson());
                pdb.add_info(
                        info,
                        op.get("data").getAsJsonObject().toString(),
                        name,
                        med.time
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
                        this, getString(R.string.med_taken_notice), Toast.LENGTH_LONG
                ).show();
            });
        }).start();
    }
}
