package com.example.uos_lms.feature.auth.presentation.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.feature.auth.presentation.AuthDestination;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashFragment extends Fragment {

    /** Floor on how long the splash stays up, so a fast backend fetch never cuts the entrance
     * animation short - a slow fetch is unaffected since it already exceeds this by itself. */
    private static final long MIN_DISPLAY_MILLIS = 1200L;
    private static final long LOGO_ANIM_MILLIS = 700L;
    private static final long NAV_RETRY_MILLIS = 150L;

    private SplashViewModel viewModel;
    private long shownAtMillis;
    private boolean navigationScheduled;

    public SplashFragment() {
        super(R.layout.fragment_splash);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shownAtMillis = System.currentTimeMillis();
        animateEntrance(view);

        viewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        viewModel.getDestination().observe(getViewLifecycleOwner(), this::scheduleNavigation);
    }

    /** A fresh re-observation after resume/config-change re-delivers the sticky destination, so
     * this is also the safety net for a navigation attempt that was dropped mid-splash. */
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            scheduleNavigation(viewModel.getDestination().getValue());
        }
    }

    private void scheduleNavigation(@Nullable AuthDestination destination) {
        if (destination == null || navigationScheduled || !isAdded() || getView() == null) return;
        navigationScheduled = true;
        long remaining = MIN_DISPLAY_MILLIS - (System.currentTimeMillis() - shownAtMillis);
        getView().postDelayed(() -> navigateWhenReady(destination), Math.max(remaining, 0L));
    }

    /** Navigation is only legal while the NavHost is RESUMED; a call made after the FragmentManager
     * saved its state (e.g. the screen locked while the splash was up) is silently dropped by
     * FragmentNavigator - so we wait for the host to come back to RESUMED (screen unlock, home ->
     * back, config change) and retry, which guarantees the login screen always opens. */
    private void navigateWhenReady(AuthDestination destination) {
        if (!isAdded() || getView() == null) return;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            navigateTo(destination);
            return;
        }
        getView().postDelayed(() -> navigateWhenReady(destination), NAV_RETRY_MILLIS);
    }

    private void navigateTo(AuthDestination destination) {
        AuthNavigator.navigate(
                NavHostFragment.findNavController(this),
                destination,
                R.id.splashFragment);
    }

    private void animateEntrance(View view) {
        View logo = view.findViewById(R.id.imageLogo);
        View progress = view.findViewById(R.id.progressIndicator);
        float slideDistance = 40f * getResources().getDisplayMetrics().density;

        logo.setAlpha(0f);
        logo.setScaleX(0.85f);
        logo.setScaleY(0.85f);
        logo.setTranslationY(slideDistance);
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(LOGO_ANIM_MILLIS)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        progress.setAlpha(0f);
        progress.animate()
                .alpha(1f)
                .setStartDelay(LOGO_ANIM_MILLIS / 2)
                .setDuration(LOGO_ANIM_MILLIS / 2)
                .start();
    }
}
