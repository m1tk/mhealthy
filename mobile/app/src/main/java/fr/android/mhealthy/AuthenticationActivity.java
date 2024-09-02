package fr.android.mhealthy;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class AuthenticationActivity extends AppCompatActivity {

    private EditText etIdToken;
    private Button btnLogin;
    private Button btnQrScan;

    private static final int QR_SCAN_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        etIdToken = findViewById(R.id.etIdToken);
        btnLogin = findViewById(R.id.btnLogin);
        btnQrScan = findViewById(R.id.btnQrScan);

        btnLogin.setOnClickListener(v -> {
            String idToken = etIdToken.getText().toString().trim();
            if (!idToken.isEmpty()) {
                // TODO: Implement authentication logic here
                // If authentication is successful, navigate to PatientMainActivity
                navigateToPatientMain();
            } else {
                Toast.makeText(this, "Please enter ID or Token", Toast.LENGTH_SHORT).show();
            }
        });

        btnQrScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST_CODE);
        });
    }

    private void navigateToPatientMain() {
        Intent intent = new Intent(this, PatientMainActivity.class);
        startActivity(intent);
        finish(); // Close the authentication activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            String scannedData = data.getStringExtra("SCAN_RESULT");
            if (scannedData != null && !scannedData.isEmpty()) {
                etIdToken.setText(scannedData);
                // TODO: Implement authentication logic here with the scanned data
                // For now, we'll just navigate to PatientMainActivity
                navigateToPatientMain();
            } else {
                Toast.makeText(this, "No data scanned", Toast.LENGTH_SHORT).show();
            }
        }
    }
}