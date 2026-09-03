package com.example.uos_lms.core.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Applies the real system status-bar height, plus a small fixed breathing-room gap, as extra
 * top padding on a view. MainActivity calls EdgeToEdge.enable(), which draws every screen's
 * content behind the status bar by default - without this, a toolbar's icons render crowded
 * against/under the status bar. The inset value comes from the OS at runtime (notch/punch-hole/
 * etc. all report different heights), so this grows the toolbar by exactly the right amount on
 * whatever device it's running on, rather than a fixed dp value that would look wrong on other
 * screens; the extra gap on top of that inset is what keeps content from sitting flush against
 * the status bar edge. */
public final class InsetUtils {

    private static final int EXTRA_TOP_SPACING_DP = 8;

    private InsetUtils() {
    }

    public static void applyStatusBarTopPadding(View view) {
        int baseLeft = view.getPaddingLeft();
        int baseTop = view.getPaddingTop();
        int baseRight = view.getPaddingRight();
        int baseBottom = view.getPaddingBottom();
        int extraSpacingPx = Math.round(EXTRA_TOP_SPACING_DP * view.getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(baseLeft, baseTop + statusBar.top + extraSpacingPx, baseRight, baseBottom);
            return insets;
        });
    }

    /** Adds the real status-bar height, plus a caller-chosen design offset, as extra top margin -
     * for a view that isn't a padded toolbar container (e.g. the Login logo sitting directly in a
     * scroll layout), so it clears the status bar/notch by the right amount on every device
     * instead of a single fixed dp guess that's wrong on some screens. */
    public static void applyStatusBarTopMargin(View view, int extraDesignOffsetDp) {
        ViewGroup.MarginLayoutParams initial = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int baseLeft = initial.leftMargin;
        int baseTop = initial.topMargin;
        int baseRight = initial.rightMargin;
        int baseBottom = initial.bottomMargin;
        int extraPx = Math.round(extraDesignOffsetDp * view.getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.setMargins(baseLeft, baseTop + statusBar.top + extraPx, baseRight, baseBottom);
            v.setLayoutParams(params);
            return insets;
        });
    }

    /** Pushes a floating bottom-nav bar up above the gesture/3-button navigation bar by adding
     * the real system inset on top of whatever fixed XML margin the view already declares, so
     * the "floating pill" keeps its designed gap instead of being partly hidden behind it. */
    public static void applyNavigationBarBottomMargin(View view) {
        ViewGroup.MarginLayoutParams initial = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int baseLeft = initial.leftMargin;
        int baseTop = initial.topMargin;
        int baseRight = initial.rightMargin;
        int baseBottom = initial.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.setMargins(baseLeft, baseTop, baseRight, baseBottom + navBar.bottom);
            v.setLayoutParams(params);
            return insets;
        });
    }
}
