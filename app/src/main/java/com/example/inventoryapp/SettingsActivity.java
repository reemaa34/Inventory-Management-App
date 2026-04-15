package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroupTheme;
    private RadioButton radioLight, radioDark, radioSystem;
    private TextView tvProfileName, tvProfileEmail, tvProfileRole;
    private Button btnLogout, btnViewAudits;
    private LinearLayout adminSection;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
        }

        sessionManager   = new SessionManager(this);
        
        // Profile views
        tvProfileName    = findViewById(R.id.tvProfileName);
        tvProfileEmail   = findViewById(R.id.tvProfileEmail);
        tvProfileRole    = findViewById(R.id.tvProfileRole);
        btnLogout        = findViewById(R.id.btnLogout);

        // Admin views
        adminSection     = findViewById(R.id.adminSection);
        btnViewAudits    = findViewById(R.id.btnViewAudits);

        // Appearance views
        radioGroupTheme  = findViewById(R.id.radioGroupTheme);
        radioLight       = findViewById(R.id.radioLight);
        radioDark        = findViewById(R.id.radioDark);
        radioSystem      = findViewById(R.id.radioSystem);

        // Populate Profile Data
        tvProfileName.setText("Name: " + sessionManager.getUsername());
        tvProfileEmail.setText("Email: " + sessionManager.getEmail());
        tvProfileRole.setText("Role: " + sessionManager.getRole());

        btnLogout.setOnClickListener(v -> sessionManager.logout());

        // Role-based visibility for Admin Section
        if ("admin".equalsIgnoreCase(sessionManager.getRole())) {
            adminSection.setVisibility(View.VISIBLE);
            btnViewAudits.setOnClickListener(v -> {
                startActivity(new Intent(this, AuditActivity.class));
            });
        } else {
            adminSection.setVisibility(View.GONE);
        }

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
            // Recreate activity to apply theme
            recreate();
        });

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellStockActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddItemActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
