package fr.android.mhealthy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;

import java.io.IOException;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.ErrorResp;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.LoginReq;
import fr.android.mhealthy.api.LoginResp;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.service.EventHandlerBackground;
import fr.android.mhealthy.service.SessionManager;
import fr.android.mhealthy.ui.CaregiverMainActivity;
import fr.android.mhealthy.ui.PatientMainActivity;
import fr.android.mhealthy.ui.QRScanActivity;
import retrofit2.Response;

public class AuthenticationActivity extends AppCompatActivity {
    SessionManager manager;

    private EditText etIdToken;

    private static final int QR_SCAN_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        HttpClient.init_cookie_jar(getApplicationContext());
        try {
            manager = new SessionManager(getApplicationContext());
        } catch (IOException e) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(e.getMessage())
                    .create();
            dialog.show();
        }
        Session s = manager.get_logged_session();
        if (s != null) {
            spawn_event_handler_service(s);
            navigateToMain(s);
        }

        etIdToken = findViewById(R.id.etIdToken);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnQrScan = findViewById(R.id.btnQrScan);

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
            integrator.setRequestCode(QR_SCAN_REQUEST_CODE);
            integrator.setPrompt(getString(R.string.scan_qr));
            integrator.setCaptureActivity(QRScanActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
    }

    private void navigateToMain(Session session) {
        Intent intent;
        if (session.account_type.equals("patient")) {
            intent = new Intent(this, PatientMainActivity.class);
        } else {
            intent = new Intent(this, CaregiverMainActivity.class);
        }
        intent.putExtra("session", session);
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

    public void spawn_event_handler_service(Session session) {
        if (!EventHandlerBackground.isServiceRunning()) {
            Intent intent = new Intent(this, EventHandlerBackground.class);
            startService(intent);
        }
    }

    void authenticate(String token) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.loggin_in))
                .setCancelable(false)
                .create();
        dialog.show();
        new Thread(() -> {
            ApiService client = HttpClient.getClient();
            LoginReq req  = new LoginReq();
            req.token     = token;
            try {
                Response<LoginResp> resp = client.login(req).execute();
                if (!resp.isSuccessful()) {
                    Gson ser      = new Gson();
                    ErrorResp err = ser.fromJson(resp.errorBody().charStream(), ErrorResp.class);
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(this, err.error, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Session s;
                    try {
                        s = manager.login(resp.body(), req.token);
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
                        spawn_event_handler_service(s);
                        navigateToMain(s);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, getString(R.string.server_unreachable), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}