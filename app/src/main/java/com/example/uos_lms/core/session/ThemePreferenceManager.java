package com.example.uos_lms.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ThemePreferenceManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_MODE = "theme_mode";

    private final SharedPreferences prefs;
    private final MutableLiveData<ThemeMode> themeMode;

    @Inject
    public ThemePreferenceManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        themeMode = new MutableLiveData<>(readStoredThemeMode(context));
    }

    public LiveData<ThemeMode> getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(ThemeMode mode) {
        prefs.edit().putString(KEY_MODE, mode.name()).apply();
        themeMode.setValue(mode);
        // Applies instantly (recreates the current Activity if the effective night state
        // changes) rather than waiting for MainActivity's next cold-start read.
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode());
    }

    /** Bootstrap-only read (no DI needed) so MainActivity can apply the saved theme
     * before super.onCreate(), avoiding a flash of the wrong theme on launch. */
    public static ThemeMode readStoredThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ThemeMode stored = ThemeMode.fromStringOrNull(prefs.getString(KEY_MODE, null));
        return stored != null ? stored : ThemeMode.SYSTEM;
    }
}
