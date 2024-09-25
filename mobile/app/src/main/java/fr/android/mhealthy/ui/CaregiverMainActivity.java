package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import fr.android.mhealthy.utils.MenuUtils;
import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.PatientRecycler;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class CaregiverMainActivity extends AppCompatActivity {
    RecyclerView patient_view;
    PatientRecycler adapter;

    Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_main);

        Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra("session");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        patient_view = findViewById(R.id.patient_view);
        adapter = new PatientRecycler(
                new CaregiverDAO(getApplicationContext(), session),
                v -> {
                    Intent i = new Intent(this, PatientMainActivity.class);
                    i.putExtra("session", session);
                    i.putExtra("patient", v);
                    startActivity(i);
                });
        patient_view.setLayoutManager(new LinearLayoutManager(this));
        patient_view.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.spacing)));
        patient_view.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.caregiver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        MenuUtils.onClickMenuItem(this, item.getItemId());
        if (item.getItemId() == R.id.show_deleted) {
            if (!adapter.show_hidden) {
                adapter.show_hidden = true;
                adapter.notifyDataSetChanged();
            }
        } else if (item.getItemId() == R.id.hide_deleted) {
            if (adapter.show_hidden) {
                adapter.show_hidden = false;
                adapter.notifyDataSetChanged();
            }
        } else if (item.getItemId() == R.id.assign_hist) {
            Intent i = new Intent(this, AssignmentHistoryActivity.class);
            i.putExtra("session", session);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        new Thread(() -> {
            adapter.load_data();
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void new_patient_event(Patient p) {
        adapter.insert(p);
        patient_view.smoothScrollToPosition(0);
    }

    public static class UnassignPatient {
        public int id;
        public UnassignPatient(int id) { this.id = id; }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void unassign_patient_event(UnassignPatient p) {
        adapter.remove(p.id);
        patient_view.smoothScrollToPosition(0);
    }
}
