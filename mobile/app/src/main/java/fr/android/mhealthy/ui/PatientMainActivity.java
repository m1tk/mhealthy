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

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.utils.MenuUtils;

public class PatientMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_main);

        TextView im = findViewById(R.id.menu_button);
        PopupMenu menu = new PopupMenu(this, im);
        menu.getMenuInflater()
                .inflate(R.menu.patient_menu, menu.getMenu());
        menu.setOnMenuItemClickListener(v -> {
            MenuUtils.onClickMenuItem(this, v.getItemId());
            return true;
        });
        im.setOnClickListener(v -> {
            menu.show();
        });

        Intent intent   = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        if (session.account_type.equals("patient")) {
            TextView welcome = findViewById(R.id.tvWelcome);
            welcome.setText(getString(R.string.welcome, session.name));
        } else {
            Patient p = (Patient) intent.getSerializableExtra("patient");
            TextView welcome = findViewById(R.id.tvWelcome);
            welcome.setText(getString(R.string.caregiver_patient_main, p.name));
        }
    }
}
