//package fr.android.mhealthy;
//
//import android.os.Bundle;
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//public class PatientMainActivity extends AppCompatActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_patient_main);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // TODO: Add button click listeners and implement functionality
//    }
//}

package fr.android.mhealthy.ui;

import static fr.android.mhealthy.utils.SettingsUtils.hasCallPermission;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Caregiver;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PatientDAO;
import fr.android.mhealthy.utils.MenuUtils;

public class PatientMainActivity extends AppCompatActivity {
    Session session;
    PatientDAO con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        session       = (Session) intent.getSerializableExtra("session");

        con = new PatientDAO(getApplicationContext(), session);

        TextView welcome = findViewById(R.id.tvWelcome);
        MaterialCardView chatCard = findViewById(R.id.cardChat);
        MaterialCardView emerCard = findViewById(R.id.cardEmergency);

        if (session.account_type.equals("patient")) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            welcome.setText(getString(R.string.welcome, session.name));
            chatCard.setVisibility(View.VISIBLE);
            emerCard.setVisibility(View.VISIBLE);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            Patient p = (Patient) intent.getSerializableExtra("patient");
            welcome.setText(getString(R.string.caregiver_patient_main, p.name));
            chatCard.setVisibility(View.GONE);
            emerCard.setVisibility(View.GONE);
        }

        Button med = findViewById(R.id.btnMedication);
        med.setOnClickListener(v -> {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            Intent intent1 = new Intent(this, MedicationManagerActivity.class);
            intent1.putExtra("session", session);
            intent1.putExtra("patient", p);
            startActivity(intent1);
        });
        Button act = findViewById(R.id.btnActivities);
        act.setOnClickListener(v -> {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            Intent intent1 = new Intent(this, ActivityManagerActivity.class);
            intent1.putExtra("session", session);
            intent1.putExtra("patient", p);
            startActivity(intent1);
        });

        Button chat = findViewById(R.id.btnChat);
        chat.setOnClickListener(v -> {
            Caregiver caregiver;
            try {
                caregiver = con.get_caregiver();
            } catch (Exception e) {
                AlertDialog err = new AlertDialog.Builder(this)
                        .setMessage(e.toString())
                        .create();
                err.show();
                return;
            }
            Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + caregiver.phone);
            Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(sendIntent);
        });

        Button emer = findViewById(R.id.btnEmergency);
        emer.setOnClickListener(v -> {
            Caregiver caregiver;
            try {
                caregiver = con.get_caregiver();
            } catch (Exception e) {
                AlertDialog err = new AlertDialog.Builder(this)
                        .setMessage(e.toString())
                        .create();
                err.show();
                return;
            }
            Intent intent1 = new Intent(hasCallPermission(this) ? Intent.ACTION_CALL : Intent.ACTION_DIAL);
            intent1.setData(Uri.parse("tel:" + caregiver.phone)); // Replace phoneNumber with the actual number
            startActivity(intent1);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (session.account_type.equals("patient")) {
            getMenuInflater().inflate(R.menu.patient_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            MenuUtils.onClickMenuItem(this, item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }
}
