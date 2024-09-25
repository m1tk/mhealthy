package fr.android.mhealthy.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.HistoryRecycler;
import fr.android.mhealthy.adapter.MedicineRecycler;
import fr.android.mhealthy.model.History;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class MedicineActivity extends AppCompatActivity {
    RecyclerView history_view;
    HistoryRecycler adapter;
    PatientDAO pdb;
    Integer p;

    Medicine medicine;

    TextView name;
    TextView dose;
    TextView time;

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
        medicine = (Medicine) intent.getSerializableExtra("medicine");

        TextView med_name = findViewById(R.id.tvMedTitle);
        med_name.setText(medicine.name);

        name = findViewById(R.id.medicine_name);
        dose = findViewById(R.id.medicine_dose);
        time = findViewById(R.id.medicine_time);

        name.setText(medicine.name);
        dose.setText(getString(R.string.dose, medicine.dose));
        time.setText(medicine.time);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            medicine.dose = data.getStringExtra("dose");
                            medicine.time = data.getStringExtra("time");
                            dose.setText(getString(R.string.dose, medicine.dose));
                            time.setText(medicine.time);
                        }
                    }
                }
        );

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
                activityResultLauncher.launch(intent1);
            });
        } else {
            pdb = new PatientDAO(getApplicationContext(), session);
            edit.setVisibility(View.GONE);
            btnMedTaken.setVisibility(View.VISIBLE);
            btnMedTaken.setOnClickListener(v -> {
                medicine_taken(medicine.name);
            });
        }

        Patient patient = (Patient) intent.getSerializableExtra("patient");
        p = patient != null ? patient.id : null;
        history_view = findViewById(R.id.history_view);
        adapter = new HistoryRecycler(
                new PatientDAO(getApplicationContext(), session),
                medicine.name,
                History.HistoryType.Medicine,
                p
        );
        history_view.setLayoutManager(new LinearLayoutManager(this));
        history_view.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.spacing)));
        history_view.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        new Thread(() -> {
            adapter.load_data(p, null);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void edit_medicine_event(Medicine.EditMedicineNotification n) {
        if (Objects.equals(n.patient, p) && n.name.equals(medicine.name)) {
            dose.setText(getString(R.string.dose, n.dose));
            time.setText(n.time);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void new_history(History n) {
        if (Objects.equals(n.patient, p) && n.type == History.HistoryType.Medicine
                && n.name.equals(medicine.name)) {
            adapter.insert(n.info);
        }
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
                med.zoned_time = ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                PatientInfo info = new PatientInfo(
                        PatientInfo.PatientInfoType.MedicineTaken,
                        med,
                        name,
                        0
                );
                pdb.add_info(
                        info,
                        info.to_store_json_format(new Gson()).toString(),
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
