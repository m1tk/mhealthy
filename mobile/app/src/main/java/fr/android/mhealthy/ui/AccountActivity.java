package fr.android.mhealthy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.SecretDAO;
import io.getstream.avatarview.AvatarView;

public class AccountActivity extends AppCompatActivity {
    SecretDAO db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        Session session = (Session) intent.getSerializableExtra("session");

        db = new SecretDAO(getApplication(), session);

        TextView name = findViewById(R.id.profile_name);
        TextView cin = findViewById(R.id.profile_cin);
        AvatarView avatar = findViewById(R.id.profile_avatar);

        name.setText(session.name);
        cin.setText(session.cin);
        avatar.setAvatarInitials(session.name);

        Button qr_token = findViewById(R.id.btnQrGet);
        qr_token.setOnClickListener(v -> {
            String token = db.get_token();
            final ImageView qr = new ImageView(this);
            qr.setPadding(20, 40, 20, 40);

            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(token, BarcodeFormat.QR_CODE, 600, 600);
                qr.setImageBitmap(bitmap);
            } catch (Exception e) {
                AlertDialog err = new AlertDialog.Builder(this)
                        .setMessage(e.toString())
                        .create();
                err.show();
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.profile_token))
                    .setMessage(session.account_type.equals("caregiver") ? "" : getString(R.string.profile_qr_gen_warn_patient))
                    .setView(qr)
                    .create();
            dialog.show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
