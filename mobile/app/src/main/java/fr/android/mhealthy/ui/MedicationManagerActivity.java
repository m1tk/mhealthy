package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.MedicineRecycler;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class MedicationManagerActivity extends AppCompatActivity {
    RecyclerView medicine_view;
    MedicineRecycler adapter;
    Integer patient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.baseline_sort_24));

        patient = null;

        Intent intent   = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        TextView title = findViewById(R.id.medTitle);

        FloatingActionButton fab = findViewById(R.id.add_med_fab);
        if (session.account_type.equals("caregiver")
                || session.account_type.equals("selfcarepatient")) {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            if (session.account_type.equals("selfcarepatient")) {
                title.setText(getString(R.string.med_list_patient));
            } else {
                title.setText(getString(R.string.med_list_caregiver));
            }
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
                    if (!v.active) {
                        return;
                    }
                    Intent intent1 = new Intent(this, MedicineActivity.class);
                    intent1.putExtra("session", session);
                    intent1.putExtra("medicine", v);
                    if (!session.account_type.equals("patient")) {
                        intent1.putExtra("patient", intent.getSerializableExtra("patient"));
                    }
                    startActivity(intent1);
                });
        medicine_view.setLayoutManager(new LinearLayoutManager(this));
        medicine_view.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.spacing)));
        medicine_view.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.show_deleted) {
            if (!adapter.show_hidden) {
                adapter.show_hidden = true;
                adapter.notifyDataSetChanged();
            }
        } else if (item.getItemId() == R.id.hide_deleted) {
            if (adapter.show_hidden) {
                adapter.show_hidden = false;
                adapter.notifyDataSetChanged();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        new Thread(() -> {
            adapter.load_data(patient);
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
    public void new_medicine_event(Medicine.AddMedicineNotification p) {
        if (Objects.equals(patient, p.patient)) {
            adapter.insert(p.med);
            medicine_view.smoothScrollToPosition(0);
        }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void edit_medicine_event(Medicine.EditMedicineNotification p) {
        if (Objects.equals(patient, p.patient)) {
            adapter.edit(p);
            medicine_view.smoothScrollToPosition(0);
        }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void remove_medicine_event(Medicine.RemoveMedicineNotification p) {
        if (Objects.equals(patient, p.patient)) {
            adapter.remove(medicine_view, p);
            medicine_view.smoothScrollToPosition(0);
        }
    }
}
