package com.example.uos_lms.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.uos_lms.core.domain.model.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Holds the signed-in user's identity (uid/role, used app-wide) and the backend JWT (used by
 * AuthInterceptor for the REST API). Backed by EncryptedSharedPreferences since it stores a
 * bearer token. */
@Singleton
public class SessionManager {

    private static final String PREFS_NAME = "session_secure";
    private static final String KEY_UID = "uid";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private final SharedPreferences prefs;
    private final MutableLiveData<CachedSession> cachedSession;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        prefs = createPrefs(context);
        cachedSession = new MutableLiveData<>(readCachedSession());
    }

    private static SharedPreferences createPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Falling back to a plain (unencrypted) file keeps login working even if the
            // device's keystore is unavailable, rather than crashing the app on launch.
            Log.e("SessionManager", "Falling back to unencrypted session storage", e);
            return context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public LiveData<CachedSession> getCachedSession() {
        return cachedSession;
    }

    public void cache(String uid, UserRole role, String fullName) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_ROLE, role.name())
                .putString(KEY_FULL_NAME, fullName)
                .apply();
        cachedSession.setValue(new CachedSession(uid, role, fullName));
    }

    public void clear() {
        prefs.edit().clear().apply();
        cachedSession.setValue(null);
    }

    /** Java-callable equivalent of {@link #cache}, matching the Task<T> idiom the rest of
     * the Java layer uses. SharedPreferences.apply() already returns immediately, so no
     * background dispatch is needed here. */
    public Task<Void> cacheAsTask(String uid, UserRole role, String fullName) {
        cache(uid, role, fullName);
        return Tasks.<Void>forResult(null);
    }

    /** The backend JWT, attached by AuthInterceptor to every authenticated API call. */
    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    @Nullable
    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    @Nullable
    private CachedSession readCachedSession() {
        String uid = prefs.getString(KEY_UID, null);
        UserRole role = UserRole.fromStringOrNull(prefs.getString(KEY_ROLE, null));
        if (uid == null || role == null) return null;
        return new CachedSession(uid, role, prefs.getString(KEY_FULL_NAME, ""));
    }
}
