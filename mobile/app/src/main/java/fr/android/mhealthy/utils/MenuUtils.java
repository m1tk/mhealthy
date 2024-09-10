package fr.android.mhealthy.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Locale;

import fr.android.mhealthy.AuthenticationActivity;
import fr.android.mhealthy.R;
import fr.android.mhealthy.service.SessionManager;
import fr.android.mhealthy.ui.SettingsActivity;


public class MenuUtils {
    public static void onClickMenuItem(AppCompatActivity act, int id) {
        if (id == R.id.c_logout) {
            logout(act);
        } else if (id == R.id.c_settings) {
            settings(act);
        }
    }

    static void logout(AppCompatActivity act) {
        AlertDialog dialog = new AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.confirm))
                .setMessage(act.getString(R.string.logout_dialog))
                .setPositiveButton(act.getString(R.string.yes), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    logout_inner(act);
                })
                .setNegativeButton(act.getString(R.string.no), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .create();
        dialog.show();
    }

    static void logout_inner(AppCompatActivity act) {
        SessionManager manager;
        try {
            manager = new SessionManager(act.getApplicationContext());
            manager.logout();
        } catch (IOException e) {
            AlertDialog warn = new AlertDialog.Builder(act)
                    .setMessage(e.getMessage())
                    .create();
            warn.show();
            return;
        }

        Intent intent = new Intent(act, AuthenticationActivity.class);
        act.startActivity(intent);
        act.finish();
    }

    static void settings(AppCompatActivity act) {
        Intent intent = new Intent(act, SettingsActivity.class);
        act.startActivity(intent);
    }
}
