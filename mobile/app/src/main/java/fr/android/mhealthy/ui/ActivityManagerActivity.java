package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.ActivityRecycler;
import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class ActivityManagerActivity extends AppCompatActivity {
    RecyclerView act_view;
    ActivityRecycler adapter;
    Integer patient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        patient = null;


        Intent intent   = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        TextView title = findViewById(R.id.medTitle);

        FloatingActionButton fab = findViewById(R.id.add_act_fab);
        if (session.account_type.equals("caregiver")) {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            title.setText(getString(R.string.act_list_caregiver));
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent1 = new Intent(this, ActivityActionActivity.class);
                intent1.putExtra("add", true);
                intent1.putExtra("session", session);
                intent1.putExtra("patient", p);
                startActivity(intent1);
            });
            patient = p.id;
        } else {
            title.setText(getString(R.string.act_list_patient));
            fab.setVisibility(View.INVISIBLE);
        }

        act_view = findViewById(R.id.act_view);
        adapter = new ActivityRecycler(
                new PatientDAO(getApplicationContext(), session),
                patient,
                v -> {
                    // TODO: PUT THIS IN ADEQUATE LOCATION
                    if (session.account_type.equals("caregiver")) {
                        Patient p = (Patient) intent.getSerializableExtra("patient");
                        Intent intent1 = new Intent(this, ActivityActionActivity.class);
                        intent1.putExtra("session", session);
                        intent1.putExtra("patient", p);
                        intent1.putExtra("activity", v);
                        startActivity(intent1);
                    }
                });
        act_view.setLayoutManager(new LinearLayoutManager(this));
        act_view.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.spacing)));
        act_view.setAdapter(adapter);
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
    public void new_activity_event(Activity.AddActivityNotification p) {
        if (patient.equals(p.patient)) {
            adapter.insert(act_view, p.act);
            act_view.smoothScrollToPosition(0);
        }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void edit_activity_event(Activity.EditActivityNotification p) {
        if (patient.equals(p.patient)) {
            adapter.edit(p);
            act_view.smoothScrollToPosition(0);
        }
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void edit_activity_event(Activity.RemoveActivityNotification p) {
        if (patient.equals(p.patient)) {
            adapter.remove(act_view, p);
            act_view.smoothScrollToPosition(0);
        }
    }
}
