package fr.android.mhealthy.ui;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import fr.android.mhealthy.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
