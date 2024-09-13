package fr.android.mhealthy.ui;

import android.app.Activity;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import fr.android.mhealthy.utils.SettingsUtils;
import fr.android.mhealthy.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    Activity act;

    public SettingsFragment(Activity act) {
        this.act = act;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("language_preference")
                .setOnPreferenceChangeListener((p, n) -> {
                    SettingsUtils.restart_on_change(act);
                    return true;
                });

        findPreference("dark_mode")
                .setOnPreferenceChangeListener((p, n) -> {
                    SettingsUtils.restart_on_change(act);
                    return true;
                });
    }
}
