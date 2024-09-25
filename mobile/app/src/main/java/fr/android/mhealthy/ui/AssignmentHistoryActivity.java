package fr.android.mhealthy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.adapter.HistoryRecycler;
import fr.android.mhealthy.model.Caregiver;
import fr.android.mhealthy.model.History;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class AssignmentHistoryActivity extends AppCompatActivity {
    RecyclerView history_view;
    HistoryRecycler adapter;
    PatientDAO pdb;

    Session session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra("session");

        pdb = new PatientDAO(getApplicationContext(), session);
        history_view = findViewById(R.id.history_view);
        adapter = new HistoryRecycler(
                new PatientDAO(getApplicationContext(), session),
                "",
                History.HistoryType.Assignment,
                null
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
        new Thread(() -> {
            List<Caregiver> users = pdb.get_caregivers();
            adapter.load_data(null, users);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
}
