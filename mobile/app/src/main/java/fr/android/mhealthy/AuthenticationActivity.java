package fr.android.mhealthy;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.google.zxing.integration.android.IntentIntegrator;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.LoginRequest;
import fr.android.mhealthy.ui.PatientMainActivity;
import fr.android.mhealthy.ui.QRScanActivity;

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
                this.authenticate(idToken);
            } else {
                Toast.makeText(this, getString(R.string.enter_token), Toast.LENGTH_SHORT).show();
            }
        });

        btnQrScan.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan a QR code");
            integrator.setCaptureActivity(QRScanActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
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
                this.authenticate(scannedData);
            } else {
                Toast.makeText(this, getString(R.string.nothing_scanned), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void authenticate(String token) {
        ApiService client = HttpClient.getClient();
        LoginRequest req  = new LoginRequest();
        req.token         = token;
        client.login(req);
        //navigateToPatientMain();
    }
}