//package fr.android.mhealthy;
//
//import android.os.Bundle;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class AuthenticationActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_authentication); // Referencing the XML layout
//    }
//}

package fr.android.mhealthy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AuthenticationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication); // Referencing the XML layout

        // Find the Scan QR Code button by its ID
        MaterialButton btnQrScan = findViewById(R.id.btnQrScan);

        // Set an OnClickListener to handle the button click
        btnQrScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to start the PatientMainActivity
                Intent intent = new Intent(AuthenticationActivity.this, PatientMainActivity.class);
                startActivity(intent);
            }
        });
    }
}
