package com.example.uos_lms.core.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.example.uos_lms.R;

/** Small, lightweight motion helpers reused across dashboards - a count-up for stat numbers and a
 * fade+slide entrance for cards/rows. Deliberately minimal (no animator libraries) to stay fast
 * and match the rest of the app's plain-Android-view style. */
public final class AnimUtils {

    private static final long COUNT_UP_MILLIS = 500L;
    private static final long ENTRANCE_MILLIS = 300L;
    private static final long LOGO_REVEAL_MILLIS = 850L;
    private static final long LOGO_BREATH_MILLIS = 1500L;
    private static final long PRESS_SCALE_MILLIS = 110L;
    private static final long SHAKE_MILLIS = 420L;

    private AnimUtils() {
    }

    /** Animates a stat TextView from whatever integer it currently shows (0 if unparsable) up to
     * targetValue. Safe to call on every state update - re-renders animate incrementally from the
     * previous value instead of resetting to 0 each time. */
    public static void animateCount(TextView textView, int targetValue) {
        int startValue;
        try {
            startValue = Integer.parseInt(textView.getText().toString());
        } catch (NumberFormatException e) {
            startValue = 0;
        }
        if (startValue == targetValue) {
            textView.setText(String.valueOf(targetValue));
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(startValue, targetValue);
        animator.setDuration(COUNT_UP_MILLIS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> textView.setText(String.valueOf((int) a.getAnimatedValue())));
        animator.start();
    }

    /** Fade + slide-up entrance for a card/row that just appeared (e.g. a freshly bound list item). */
    public static void fadeSlideIn(View view) {
        fadeSlideIn(view, 0L);
    }

    /** Same entrance, delayed - lets a screen stagger multiple elements (e.g. logo first, then
     * the form card) instead of everything appearing in the same instant. */
    public static void fadeSlideIn(View view, long startDelayMillis) {
        view.setAlpha(0f);
        view.setTranslationY(24f * view.getResources().getDisplayMetrics().density);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelayMillis)
                .setDuration(ENTRANCE_MILLIS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** Login-screen logo reveal: starts small/transparent/tilted, spins and scales up into place
     * with a pronounced overshoot bounce, then settles into a continuous, clearly-visible
     * "floating" loop (scale + gentle vertical bob) so the screen keeps feeling alive. */
    public static void revealLogo(View logo) {
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);
        logo.setAlpha(0f);
        logo.setRotation(-25f);
        logo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .rotation(0f)
                .setDuration(LOGO_REVEAL_MILLIS)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .withEndAction(() -> startBreathing(logo))
                .start();
    }

    private static void startBreathing(View logo) {
        float floatDistancePx = 12f * logo.getResources().getDisplayMetrics().density;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.12f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.12f);
        ObjectAnimator floatY = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 0f, -floatDistancePx);
        for (ObjectAnimator animator : new ObjectAnimator[]{scaleX, scaleY, floatY}) {
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
        }

        AnimatorSet breathing = new AnimatorSet();
        breathing.playTogether(scaleX, scaleY, floatY);
        breathing.setDuration(LOGO_BREATH_MILLIS);
        breathing.setInterpolator(new AccelerateDecelerateInterpolator());
        breathing.start();
    }

    /** Subtle press-down/release scale feedback for a button, layered on top of its existing
     * ripple. Returns false from the touch listener so the view's own click handling (and any
     * OnClickListener already attached) keeps working exactly as before. */
    public static void applyPressScale(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(PRESS_SCALE_MILLIS).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(PRESS_SCALE_MILLIS).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    /** Gentle breathing alpha pulse for a small status dot (e.g. the "PENDING" badge's live
     * indicator) - continuous, subtle, never bounces. The animator is tagged on the view so a
     * later {@link #stopPulse(View)} (or a repeat call here) can cancel it - an infinite-repeat
     * ObjectAnimator otherwise keeps its target view alive forever via the Choreographer, which
     * leaks if the view is ever discarded (e.g. a list rebuilt from scratch) without being
     * explicitly stopped first. */
    public static void pulse(View dot) {
        stopPulse(dot);
        dot.setAlpha(1f);
        ObjectAnimator pulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.35f);
        pulse.setDuration(900L);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        dot.setTag(R.id.tag_pulse_animator, pulse);
        pulse.start();
    }

    /** Cancels a pulse started by {@link #pulse(View)}, if any - call this on every view a
     * caller is about to discard (e.g. before removeAllViews() on a rebuilt list) so the
     * animator doesn't keep the orphaned view alive indefinitely. */
    public static void stopPulse(View dot) {
        Object tagged = dot.getTag(R.id.tag_pulse_animator);
        if (tagged instanceof ObjectAnimator) {
            ((ObjectAnimator) tagged).cancel();
            dot.setTag(R.id.tag_pulse_animator, null);
        }
    }

    /** Short horizontal shake for validation/error feedback (e.g. a field that just failed). */
    public static void shake(View view) {
        view.animate().cancel();
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, -14f, 14f, -10f, 10f, -4f, 4f, 0f);
        shake.setDuration(SHAKE_MILLIS);
        shake.start();
    }
}
