package com.example.uos_lms;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.transition.Fade;

import com.example.uos_lms.core.data.remote.api.ApiNotificationDataSource;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.notifications.NotificationChannels;
import com.example.uos_lms.core.notifications.NotificationRouter;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.session.ThemePreferenceManager;
import com.example.uos_lms.core.ui.InsetUtils;
import com.example.uos_lms.feature.notifications.AppNotificationCenter;
import com.google.firebase.messaging.FirebaseMessaging;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long SCREEN_TRANSITION_MILLIS = 180L;

    @Inject
    SessionManager sessionManager;
    @Inject
    AppNotificationCenter notificationCenter;
    @Inject
    ApiNotificationDataSource notificationDataSource;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    @Nullable
    private String activeNotificationUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(ThemePreferenceManager.readStoredThemeMode(this).toNightMode());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        registerScreenTransitions();

        notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            // No extra handling needed either way - AppNotificationCenter already checks the
            // permission itself before every post, so a denial just means it stays silent.
        });
        // LiveData.observe auto-starts/stops delivery around STARTED/DESTROYED, which replaces
        // the old addAuthStateListener/removeAuthStateListener pairing in onStart/onStop.
        sessionManager.getCachedSession().observe(this, this::onSessionChanged);

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    /** Routes a tapped notification (push or local) to its category's screen - see
     * NotificationRouter. Only acts if a session is already cached, so a tap that happens to
     * arrive before the user has signed in just opens the app normally instead of trying to
     * navigate into an authenticated-only screen. */
    private void handleNotificationIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String categoryName = intent.getStringExtra(NotificationRouter.EXTRA_CATEGORY);
        if (categoryName == null || sessionManager.getCachedSession().getValue() == null) return;
        NotificationCategory category = NotificationChannels.parseCategory(categoryName);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NotificationRouter.navigate(navController, category);
    }

    /** Starts/stops the Notification Center's listeners to match the signed-in user, registers
     * this device's FCM token for push (best-effort - a missing Firebase project just means this
     * silently no-ops, see registerFcmToken), and asks for POST_NOTIFICATIONS once per fresh
     * sign-in (Android 13+) so notifications can actually reach the tray. Guarded by
     * activeNotificationUid so redundant LiveData deliveries (e.g. app backgrounded/foregrounded)
     * don't restart already-running listeners. */
    private void onSessionChanged(@Nullable CachedSession session) {
        if (session == null) {
            activeNotificationUid = null;
            notificationCenter.stop();
            return;
        }
        if (session.getUid().equals(activeNotificationUid)) return;
        activeNotificationUid = session.getUid();
        notificationCenter.start();
        registerFcmToken();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** No-ops (with a log line, not a crash) if no Firebase project is configured yet - see
     * app/build.gradle.kts' conditional google-services plugin application. */
    private void registerFcmToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                    notificationDataSource.registerFcmToken(token)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to register FCM token with backend", e))
            ).addOnFailureListener(e -> Log.w(TAG, "Failed to obtain FCM token", e));
        } catch (IllegalStateException e) {
            Log.w(TAG, "Firebase not configured (no google-services.json yet) - push notifications disabled", e);
        }
    }

    /** Lightweight app-wide fade between screens, plus automatic status-bar clearance for every
     * screen's toolbar. Registered once, recursively, so both apply to every fragment the
     * NavHostFragment swaps in - no per-destination wiring, no changes to any of the existing
     * navigate() call sites or nav_graph.xml actions, and no need to touch every individual
     * Fragment that happens to have a view with id "toolbar". */
    private void registerScreenTransitions() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentPreAttached(FragmentManager fm, Fragment fragment, android.content.Context context) {
                Fade fade = new Fade();
                fade.setDuration(SCREEN_TRANSITION_MILLIS);
                fragment.setEnterTransition(fade);
                fragment.setExitTransition(fade);
            }

            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment fragment, @NonNull View view, @Nullable Bundle savedInstanceState) {
                View toolbar = view.findViewById(R.id.toolbar);
                if (toolbar != null) {
                    InsetUtils.applyStatusBarTopPadding(toolbar);
                }
                View bottomNav = view.findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    InsetUtils.applyNavigationBarBottomMargin(bottomNav);
                }
            }
        }, true);
    }
}
