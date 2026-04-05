package com.example.inventoryapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "InventorySession";

    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL        = "email";
    private static final String KEY_USERNAME     = "username";
    private static final String KEY_ROLE         = "role";
    // Feature 3: night mode preference
    private static final String KEY_NIGHT_MODE   = "nightMode";


    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Context context;


    public SessionManager(Context context) {

        this.context = context;

        prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        editor = prefs.edit();
    }


    public void createLoginSession(String email,
                                   String username,
                                   String role) {

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);

        editor.apply();
    }


    public boolean isLoggedIn() {

        return prefs.getBoolean(
                KEY_IS_LOGGED_IN,
                false
        );
    }


    public String getEmail() {

        return prefs.getString(
                KEY_EMAIL,
                null
        );
    }


    public String getUsername() {

        return prefs.getString(
                KEY_USERNAME,
                "User"
        );
    }


    public String getRole() {

        return prefs.getString(
                KEY_ROLE,
                "employee"
        );
    }


    public void logout() {

        editor.clear();
        editor.apply();

        Intent intent =
                new Intent(
                        context,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        context.startActivity(intent);
    }

    // ── Feature 3: Dark Mode helpers ──

    /**
     * Returns the saved AppCompatDelegate night mode integer.
     * Default: -1 (MODE_NIGHT_FOLLOW_SYSTEM)
     */
    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE,
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        editor.putInt(KEY_NIGHT_MODE, mode);
        editor.apply();
    }
}