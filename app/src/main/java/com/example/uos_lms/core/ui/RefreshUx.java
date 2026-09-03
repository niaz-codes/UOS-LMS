package com.example.uos_lms.core.ui;

import android.animation.ObjectAnimator;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;

/** Wires a refresh trigger - a toolbar icon (inflated into a screen's {@code toolbarActionSlot})
 * or a top-level dashboard's overflow-menu item - and an optional {@link SwipeRefreshLayout} to
 * the same refresh action, so the icon tap, the swipe gesture, and the ViewModel's own
 * `refreshing` flag all stay in sync everywhere in the app: one call in onViewCreated, one call
 * in the uiState observer, same as every other screen. The ViewModel-side `refreshing` flag
 * (surviving rotation) is still the source of truth for whether a request is in flight; this
 * just mirrors it into the widgets and short-circuits a second tap/pull before it even reaches
 * the ViewModel. */
public final class RefreshUx {

    private RefreshUx() {
    }

    public static final class Binding {
        @Nullable
        private final ImageButton button;
        @Nullable
        private final MenuItem menuItem;
        @Nullable
        private final SwipeRefreshLayout swipeRefreshLayout;
        private final Runnable onRefresh;
        @Nullable
        private ObjectAnimator spinAnimator;
        private boolean refreshing;

        private Binding(@Nullable ImageButton button, @Nullable MenuItem menuItem,
                @Nullable SwipeRefreshLayout swipeRefreshLayout, Runnable onRefresh) {
            this.button = button;
            this.menuItem = menuItem;
            this.swipeRefreshLayout = swipeRefreshLayout;
            this.onRefresh = onRefresh;
        }

        /** Fires the refresh action unless one is already in flight - call this from a manual
         * trigger (e.g. a menu item click) that isn't already wired via {@link #bindToolbarIcon}
         * or the swipe listener. */
        public void trigger() {
            if (refreshing) return;
            onRefresh.run();
        }

        /** Reflects the ViewModel's current refreshing flag into the spinner/icon/menu item.
         * Safe to call unconditionally from every uiState observer emission. */
        public void setRefreshing(boolean refreshing) {
            if (this.refreshing == refreshing) return;
            this.refreshing = refreshing;
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing() != refreshing) {
                swipeRefreshLayout.setRefreshing(refreshing);
            }
            if (menuItem != null) menuItem.setEnabled(!refreshing);
            if (button == null) return;
            button.setEnabled(!refreshing);
            if (refreshing) {
                spinAnimator = ObjectAnimator.ofFloat(button, View.ROTATION, 0f, 360f);
                spinAnimator.setDuration(700);
                spinAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                spinAnimator.setInterpolator(new LinearInterpolator());
                spinAnimator.start();
            } else if (spinAnimator != null) {
                spinAnimator.cancel();
                spinAnimator = null;
                button.setRotation(0f);
            }
        }
    }

    /** For a gradient-toolbar list/detail screen with a {@code toolbarActionSlot}.
     * @param swipeRefreshLayout nullable - pass null for non-scrollable/form screens that only
     *                            want the toolbar icon, not a pull gesture. */
    public static Binding bindToolbarIcon(ViewGroup toolbarActionSlot, @Nullable SwipeRefreshLayout swipeRefreshLayout, Runnable onRefresh) {
        toolbarActionSlot.removeAllViews();
        ImageButton button = (ImageButton) LayoutInflater.from(toolbarActionSlot.getContext())
                .inflate(R.layout.view_toolbar_refresh_button, toolbarActionSlot, false);
        toolbarActionSlot.addView(button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            button.setTooltipText(button.getContext().getString(R.string.refresh));
        }

        Binding binding = new Binding(button, null, swipeRefreshLayout, onRefresh);
        button.setOnClickListener(v -> binding.trigger());
        bindSwipe(swipeRefreshLayout, binding);
        return binding;
    }

    /** For a top-level dashboard using a {@link com.google.android.material.appbar.MaterialToolbar}
     * with a menu - pass the inflated {@code R.id.actionRefresh} MenuItem. The caller's own
     * onMenuItemClickListener should call {@link Binding#trigger()} for that item's id, matching
     * how every other menu action on these screens is already handled in one central listener. */
    public static Binding bindMenuItem(MenuItem refreshItem, @Nullable SwipeRefreshLayout swipeRefreshLayout, Runnable onRefresh) {
        Binding binding = new Binding(null, refreshItem, swipeRefreshLayout, onRefresh);
        bindSwipe(swipeRefreshLayout, binding);
        return binding;
    }

    private static void bindSwipe(@Nullable SwipeRefreshLayout swipeRefreshLayout, Binding binding) {
        if (swipeRefreshLayout == null) return;
        swipeRefreshLayout.setColorSchemeResources(
                R.color.role_admin_start, R.color.role_hod_start,
                R.color.role_teacher_start, R.color.role_student_start);
        swipeRefreshLayout.setOnRefreshListener(binding::trigger);
    }
}
