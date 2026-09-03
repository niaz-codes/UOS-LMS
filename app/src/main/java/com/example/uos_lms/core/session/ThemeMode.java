package com.example.uos_lms.core.session;

import androidx.appcompat.app.AppCompatDelegate;

public enum ThemeMode {
    SYSTEM, LIGHT, DARK;

    public int toNightMode() {
        switch (this) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static ThemeMode fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (ThemeMode mode : values()) {
            if (mode.name().equals(raw)) return mode;
        }
        return null;
    }
}
