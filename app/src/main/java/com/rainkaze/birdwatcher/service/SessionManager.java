package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME_USER = "user_session_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = this.context.getSharedPreferences(PREF_NAME_USER, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void createLoginSession(long userId, String username, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_TOKEN, token);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, 0L);
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}