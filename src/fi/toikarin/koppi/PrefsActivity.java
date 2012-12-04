package fi.toikarin.koppi;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // deprecation v11
        addPreferencesFromResource(R.layout.preferences);
        PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // deprecation v11
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        // deprecation v11
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Main main = (Main) this.getApplicationContext();
        main.setState(sharedPreferences);
    }
}
