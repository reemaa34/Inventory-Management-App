package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ── Feature 3: Apply saved night mode before any view inflation ──
        SessionManager sm = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(sm.getNightMode());

        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        sessionManager = sm;

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void attemptLogin() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) return;
        if (TextUtils.isEmpty(password)) return;

        dbHelper.loginUser(email, password, success -> {

            if (!success) {
                Toast.makeText(this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.getUserName(email, username -> {

                dbHelper.getUserRole(email, role -> {

                    sessionManager.createLoginSession(
                            email,
                            username,
                            role
                    );

                    Toast.makeText(this,
                            "Welcome back, " + username + "!",
                            Toast.LENGTH_SHORT).show();

                    startActivity(
                            new Intent(this, DashboardActivity.class)
                    );

                    finish();
                });
            });
        });
    }
}