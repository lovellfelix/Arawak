package com.lovellfelix.arawak.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.lovellfelix.arawak.R;
import com.lovellfelix.arawak.service.RefreshService;
import com.lovellfelix.arawak.utils.PrefUtils;
import com.lovellfelix.arawak.utils.UiUtils;

public class GeneralPrefsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.layout.activity_preferences);

        Preference preference = findPreference(PrefUtils.REFRESH_ENABLED);
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Boolean.TRUE.equals(newValue)) {
                    startService(new Intent(GeneralPrefsActivity.this, RefreshService.class));
                } else {
                    PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
                    stopService(new Intent(GeneralPrefsActivity.this, RefreshService.class));
                }
                return true;
            }
        });

        preference = findPreference(PrefUtils.LIGHT_THEME);
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefUtils.putBoolean(PrefUtils.LIGHT_THEME, Boolean.TRUE.equals(newValue));
                android.os.Process.killProcess(android.os.Process.myPid());

                // this return statement will never be reached
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }
}
