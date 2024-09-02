package fr.android.mhealthy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("Scan a QR code");
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                setResult(Activity.RESULT_CANCELED);
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("SCAN_RESULT", result.getContents());
                setResult(Activity.RESULT_OK, resultIntent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        finish();
    }
}