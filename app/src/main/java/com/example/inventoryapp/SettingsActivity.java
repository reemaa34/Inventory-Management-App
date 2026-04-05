package com.example.inventoryapp;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

/**
 * Feature 3 — Dark Mode Settings
 * Lets the user choose Light / Dark / System Default.
 * Preference is persisted via SessionManager and applied immediately.
 */
public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroupTheme;
    private RadioButton radioLight, radioDark, radioSystem;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager   = new SessionManager(this);
        radioGroupTheme  = findViewById(R.id.radioGroupTheme);
        radioLight       = findViewById(R.id.radioLight);
        radioDark        = findViewById(R.id.radioDark);
        radioSystem      = findViewById(R.id.radioSystem);

        // Load saved preference and check the matching radio
        int savedMode = sessionManager.getNightMode();
        if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) {
            radioLight.setChecked(true);
        } else if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) {
            radioDark.setChecked(true);
        } else {
            radioSystem.setChecked(true);
        }

        // React to radio selection
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radioLight) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioDark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            sessionManager.setNightMode(mode);
            AppCompatDelegate.setDefaultNightMode(mode);
            // recreate() is called automatically by AppCompatDelegate when mode changes,
            // but we also call it here to be safe.
            recreate();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
