package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.model.History;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.SpaceItemDecoration;

public class ActivityActivity extends AppCompatActivity {
    RecyclerView history_view;
    HistoryRecycler adapter;
    PatientDAO pdb;
    Integer p;

    Activity activity;
    String name_str;

    TextView name;
    TextView goal;
    TextView time;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");
        activity = (Activity) intent.getSerializableExtra("activity");

        try {
            name_str = ActivityActionActivity.get_options(getApplicationContext())[Integer.parseInt(activity.name)];
        } catch (NumberFormatException e) {
            name_str = activity.name;
        }

        TextView act_name = findViewById(R.id.tvActTitle);
        act_name.setText(name_str);

        name = findViewById(R.id.activity_name);
        goal = findViewById(R.id.activity_goal);
        time = findViewById(R.id.activity_time);

        name.setText(name_str);
        goal.setText(activity.goal.isEmpty() ? getString(R.string.no_goal)
                                             : getString(R.string.goal, activity.goal));
        time.setText(activity.time);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            activity.goal = data.getStringExtra("goal");
                            activity.time = data.getStringExtra("time");
                            goal.setText(activity.goal.isEmpty() ? getString(R.string.no_goal)
                                    : getString(R.string.goal, activity.goal));
                            time.setText(activity.time);
                        }
                    }
                }
        );

        MaterialButton btnActFinished = findViewById(R.id.btnActFinished);
        FloatingActionButton edit   = findViewById(R.id.edit_act_fab);
        if (session.account_type.equals("caregiver")) {
            edit.setVisibility(View.VISIBLE);
            btnActFinished.setVisibility(View.GONE);
            edit.setOnClickListener(v -> {
                Patient p = (Patient) intent.getSerializableExtra("patient");
                Intent intent1 = new Intent(this, ActivityActionActivity.class);
                intent1.putExtra("session", session);
                intent1.putExtra("patient", p);
                intent1.putExtra("activity", activity);
                activityResultLauncher.launch(intent1);
            });
        } else {
            pdb = new PatientDAO(getApplicationContext(), session);
            edit.setVisibility(View.GONE);
            btnActFinished.setVisibility(View.VISIBLE);
            btnActFinished.setOnClickListener(v -> {
                activity_finished(activity.name);
            });
        }

        Patient patient = (Patient) intent.getSerializableExtra("patient");
        p = patient != null ? patient.id : null;
        history_view = findViewById(R.id.history_view);
        adapter = new HistoryRecycler(
                new PatientDAO(getApplicationContext(), session),
                activity.name,
                History.HistoryType.Activity,
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
            adapter.load_data(p);
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
    public void edit_activity_event(Activity.EditActivityNotification n) {
        if (Objects.equals(n.patient, p) && n.name.equals(activity.name)) {
            goal.setText(n.name.isEmpty() ? getString(R.string.no_goal)
                    : getString(R.string.goal, n.goal));
            time.setText(n.time);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void new_history(History n) {
        if (Objects.equals(n.patient, p) && n.type == History.HistoryType.Activity
                && n.name.equals(activity.name)) {
            adapter.insert(n.info);
        }
    }

    private void activity_finished(String name) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.act_result_title))
                .setMessage(getString(R.string.act_result))
                .setView(input)
                .setPositiveButton(getString(R.string.continue_dial), (dialogInterface, i) -> {
                    if (input.getText().toString().isEmpty()) {
                        return;
                    }
                    dialogInterface.dismiss();
                    activity_finished_inner(name, input.getText().toString());
                })
                .setNegativeButton(getString(R.string.cancel_dial), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .create();
        dialog.show();
    }

    private void activity_finished_inner(String name, String value) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.pending))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            try {
                PatientInfo.ActivityFinished act = new PatientInfo.ActivityFinished();
                act.time = System.currentTimeMillis() / 1000;
                act.zoned_time = ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                act.value = value;
                PatientInfo info = new PatientInfo(
                        PatientInfo.PatientInfoType.ActivityFinished,
                        act,
                        name,
                        0
                );
                pdb.add_info(
                        info,
                        info.to_store_json_format(new Gson()).toString(),
                        name,
                        act.time
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
                        this, getString(R.string.act_finished_notice), Toast.LENGTH_LONG
                ).show();
            });
        }).start();
    }
}
