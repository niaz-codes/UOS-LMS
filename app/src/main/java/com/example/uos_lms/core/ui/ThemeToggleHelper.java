package com.example.uos_lms.core.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.view.MenuItem;

import com.example.uos_lms.R;
import com.example.uos_lms.core.session.ThemeMode;
import com.example.uos_lms.core.session.ThemePreferenceManager;

/**
 * Top-toolbar theme toggle shared by every role's Home/Dashboard screen. Deliberately stateless:
 * a mode change goes through the exact same {@link ThemePreferenceManager#setThemeMode} the
 * Settings screen's toggle group already uses, which (per its own existing behavior) calls
 * {@code AppCompatDelegate.setDefaultNightMode()} and recreates the current Activity when the
 * effective night state actually changes. That recreate is what keeps the icon and Settings in
 * sync in both directions - each screen just reads the current effective mode fresh on its next
 * onViewCreated, so no LiveData observation or manual sync bookkeeping is needed here. The
 * already-registered app-wide Fragment fade transition (MainActivity) supplies the "smooth
 * transition when switching themes" for free, since a night-mode change recreates the fragment.
 */
public final class ThemeToggleHelper {

    private ThemeToggleHelper() {
    }

    /** Sets the toggle's icon to reflect what's actually on screen right now - sun while in
     * light mode (tap to go dark), moon while in dark mode (tap to go light) - including when
     * the stored preference is SYSTEM, since what matters for the icon is the resolved render
     * state, not the raw preference value. */
    public static void applyIcon(MenuItem item, Context context) {
        item.setIcon(isCurrentlyDark(context) ? R.drawable.ic_dark_mode : R.drawable.ic_light_mode);
    }

    /** Flips the effective mode - dark to light, light to dark - bypassing SYSTEM entirely, since
     * a binary toggle icon has no third state to represent. */
    public static void toggle(Context context, ThemePreferenceManager themePreferenceManager) {
        themePreferenceManager.setThemeMode(isCurrentlyDark(context) ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    private static boolean isCurrentlyDark(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
