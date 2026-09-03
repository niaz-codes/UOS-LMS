package com.example.uos_lms.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Runtime override for the backend's LAN host:port (e.g. "192.168.1.5:4000"), set via the
 * Login screen's long-press-logo dialog. Exists because this app's backend is a locally-hosted
 * dev server rather than a fixed cloud host - its LAN IP changes whenever the WiFi network
 * changes, and baking it into BuildConfig.API_BASE_URL would require a rebuild every time.
 * When no override is set, {@link com.example.uos_lms.core.data.remote.api.ServerConfigInterceptor}
 * leaves requests pointed at the BuildConfig default. */
@Singleton
public class ServerConfig {

    private static final String PREFS_NAME = "server_config";
    private static final String KEY_HOST_PORT = "override_host_port";

    private final SharedPreferences prefs;

    @Inject
    public ServerConfig(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public String getOverrideHostPort() {
        String value = prefs.getString(KEY_HOST_PORT, null);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /** Pass null or blank to clear the override and fall back to the BuildConfig default. */
    public void setOverrideHostPort(@Nullable String hostPort) {
        if (hostPort == null || hostPort.trim().isEmpty()) {
            prefs.edit().remove(KEY_HOST_PORT).apply();
        } else {
            prefs.edit().putString(KEY_HOST_PORT, hostPort.trim()).apply();
        }
    }
}
