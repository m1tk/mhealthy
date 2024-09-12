package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.MedicineRecycler;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;

public class MedicationManagerActivity extends AppCompatActivity {
    RecyclerView medicine_view;
    MedicineRecycler adapter;
    Integer patient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_manager);

        patient = null;

        EventBus.getDefault().register(this);

        Intent intent   = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        TextView title = findViewById(R.id.medTitle);

        FloatingActionButton fab = findViewById(R.id.add_med_fab);
        if (session.account_type.equals("caregiver")) {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            title.setText(getString(R.string.med_list_caregiver));
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent1 = new Intent(this, MedicationActionActivity.class);
                intent1.putExtra("add", true);
                intent1.putExtra("session", session);
                intent1.putExtra("patient", p);
                startActivity(intent1);
            });
            patient = p.id;
        } else {
            title.setText(getString(R.string.med_list_patient));
            fab.setVisibility(View.INVISIBLE);
        }

        medicine_view = findViewById(R.id.medicine_view);
        adapter = new MedicineRecycler(
                new PatientDAO(getApplicationContext(), session),
                patient,
                v -> {
                    return;
                });
        medicine_view.setLayoutManager(new LinearLayoutManager(this));
        medicine_view.setAdapter(adapter);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void new_medicine_event(Medicine.AddMedicineNotification p) {
        if (patient.equals(p.patient)) {
            adapter.insert(p.med);
            medicine_view.smoothScrollToPosition(0);
        }
    }
}
