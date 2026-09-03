package com.example.uos_lms.feature.auth.presentation.register;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uos_lms.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Premium tab-style option picker used by the registration screens.
 *
 * <p>Two modes:
 * <ul>
 *     <li><b>Single-select</b> (default) - a connected segmented navigation bar: a rounded track
 *         holds equal-width tabs and an elevated gradient pill glides across to the active tab
 *         with a smooth width + translation animation.</li>
 *     <li><b>Multi-select</b> - independent pill chips; tapping toggles a check mark (with a
 *         springy pop) and the tinted fill. Selections are never duplicated.</li>
 * </ul>
 *
 * <p>The auth screens sit on a dark image + scrim background by design, so the chrome is tuned
 * for that surface (white-alpha track/chips + indigo active states) and reads identically in both
 * light and dark themes. Programmatic {@link #setSelectedIds} never fires the listener - only
 * real user taps do, which keeps state/render loops clean.
 */
public class OptionTabGroup extends HorizontalScrollView {

    public static class Option {
        public final String id;
        public final String label;

        public Option(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(@NonNull OptionTabGroup group, @NonNull List<String> selectedIds);
    }

    private static final float LABEL_TEXT_SP = 13f;
    private static final int TAB_HEIGHT_DP = 44;
    private static final int TAB_H_PADDING_DP = 18;
    private static final int MIN_TAB_WIDTH_DP = 72;
    private static final int CHIP_GAP_DP = 8;
    private static final int CHECK_SIZE_DP = 14;
    private static final int TRACK_STROKE_DP = 1;

    private final FrameLayout contentFrame;
    private final LinearLayout content;
    private final View pill;

    private final List<Option> options = new ArrayList<>();
    private final List<TabHolder> tabs = new ArrayList<>();
    private final List<String> selectedIds = new ArrayList<>();

    private boolean singleSelect = true;
    private boolean themeAware = false;
    private OnSelectionChangedListener listener;

    private final int tabHeightPx;
    private final int tabHPaddingPx;
    private final int minTabWidthPx;
    private final int gapPx;
    private final int cornerRadiusPx;
    private final int trackStrokePx;

    public OptionTabGroup(Context context) {
        this(context, null);
    }

    public OptionTabGroup(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionTabGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        setOverScrollMode(OVER_SCROLL_NEVER);

        Resources res = getResources();
        tabHeightPx = dp(TAB_HEIGHT_DP);
        tabHPaddingPx = dp(TAB_H_PADDING_DP);
        minTabWidthPx = dp(MIN_TAB_WIDTH_DP);
        gapPx = dp(CHIP_GAP_DP);
        cornerRadiusPx = dp(20);
        trackStrokePx = dp(TRACK_STROKE_DP);

        contentFrame = new FrameLayout(context);
        contentFrame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, tabHeightPx));

        pill = new View(context);
        pill.setBackgroundResource(R.drawable.bg_tab_pill);
        pill.setElevation(dp(6));
        pill.setVisibility(GONE);
        FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(0, tabHeightPx - 2 * trackStrokePx);
        pillLp.gravity = Gravity.TOP | Gravity.START;
        pillLp.leftMargin = trackStrokePx;
        pillLp.topMargin = trackStrokePx;
        contentFrame.addView(pill, pillLp);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        // Row sits above the pill (pill z = 6dp) so tab labels stay crisp on top of the
        // sliding indicator while its gradient + shadow show through the transparent gaps.
        content.setElevation(dp(12));
        contentFrame.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, tabHeightPx));

        addView(contentFrame, new LayoutParams(LayoutParams.WRAP_CONTENT, tabHeightPx));
    }

    private static class TabHolder {
        final FrameLayout view;
        final TextView label;
        @Nullable
        final ImageView check;

        TabHolder(FrameLayout view, TextView label, @Nullable ImageView check) {
            this.view = view;
            this.label = label;
            this.check = check;
        }
    }

    // ---- Public API ----

    public void setSingleSelect(boolean singleSelect) {
        if (this.singleSelect == singleSelect) return;
        this.singleSelect = singleSelect;
        rebuild();
    }

    public boolean isSingleSelect() {
        return singleSelect;
    }

    /** Switches the chrome from the dark auth-surface variant (white-alpha track/chips +
     * indigo states, the default) to a surface-friendly variant (light track, theme-neutral
     * on-surface text, the role's brand gradient for the active pill) for light/dark screens
     * like the Teacher's Exam Result workspace. */
    public void setThemeAware(boolean themeAware) {
        if (this.themeAware == themeAware) return;
        this.themeAware = themeAware;
        rebuild();
    }

    public boolean isThemeAware() {
        return themeAware;
    }

    public void setOptions(@Nullable List<Option> options) {
        List<Option> normalized = options == null ? Collections.emptyList() : options;
        if (sameOptions(normalized)) return;

        this.options.clear();
        this.options.addAll(normalized);

        Set<String> valid = new HashSet<>();
        for (Option option : this.options) valid.add(option.id);
        selectedIds.removeIf(id -> !valid.contains(id));
        if (singleSelect && selectedIds.size() > 1) {
            selectedIds.subList(1, selectedIds.size()).clear();
        }
        rebuild();
    }

    private boolean sameOptions(List<Option> newOptions) {
        if (this.options.size() != newOptions.size()) return false;
        for (int i = 0; i < newOptions.size(); i++) {
            Option a = this.options.get(i);
            Option b = newOptions.get(i);
            if (!a.id.equals(b.id) || !a.label.equals(b.label)) return false;
        }
        return true;
    }

    public void setListener(@Nullable OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    /** Programmatic update - intentionally does NOT fire the listener (only user taps do). */
    public void setSelectedIds(@Nullable Collection<String> ids) {
        Set<String> valid = new HashSet<>();
        for (Option option : options) valid.add(option.id);

        List<String> next = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                if (valid.contains(id) && !next.contains(id)) next.add(id);
            }
        }
        if (singleSelect && next.size() > 1) {
            next = next.subList(0, 1);
        }
        if (next.equals(selectedIds)) return;

        selectedIds.clear();
        selectedIds.addAll(next);

        if (singleSelect) {
            int index = indexOfSelected();
            if (index >= 0 && tabs.get(index).view.getWidth() == 0) {
                contentFrame.post(() -> positionPill(indexOfSelected(), false));
            } else {
                positionPill(index, true);
            }
        } else {
            updateMultiTabStates();
        }
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    // ---- Internals ----

    private void rebuild() {
        content.removeAllViews();
        tabs.clear();
        pill.setVisibility(GONE);

        if (singleSelect && !options.isEmpty()) {
            contentFrame.setBackground(themeAware ? createTrackDrawable() : getContext().getDrawable(R.drawable.bg_tab_track));
        } else {
            contentFrame.setBackground(null);
        }
        pill.setBackground(themeAware ? createPillDrawable() : getContext().getDrawable(R.drawable.bg_tab_pill));

        int uniformWidth = computeUniformTabWidth();
        for (int i = 0; i < options.size(); i++) {
            TabHolder holder = createTab(options.get(i), i, uniformWidth);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    singleSelect ? uniformWidth : LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            if (!singleSelect && i < options.size() - 1) {
                lp.setMargins(0, 0, gapPx, 0);
            }
            content.addView(holder.view, lp);
            tabs.add(holder);
        }

        if (singleSelect) {
            // Tabs are not measured yet at rebuild time - position the pill once layout lands.
            contentFrame.post(() -> positionPill(indexOfSelected(), false));
        } else {
            updateMultiTabStates();
        }
    }

    private int computeUniformTabWidth() {
        if (options.isEmpty()) return 0;
        android.text.TextPaint paint = new android.text.TextPaint();
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_TEXT_SP,
                getResources().getDisplayMetrics()));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        int widest = 0;
        for (Option option : options) {
            widest = Math.max(widest, Math.round(paint.measureText(option.label)));
        }
        return Math.max(minTabWidthPx, widest + 2 * tabHPaddingPx);
    }

    private TabHolder createTab(Option option, int index, int uniformWidth) {
        FrameLayout tab = new FrameLayout(getContext());
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setForeground(getSelectableItemBackground());
        tab.setContentDescription(option.label);
        tab.setOnClickListener(v -> onTabClicked(index));
        applyRoundedOutline(tab);

        ImageView check = null;
        if (singleSelect) {
            TextView label = new TextView(getContext());
            label.setText(option.label);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_TEXT_SP);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(inactiveTextColor());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            tab.addView(label, lp);
            return new TabHolder(tab, label, null);
        }

        LinearLayout chipContent = new LinearLayout(getContext());
        chipContent.setOrientation(LinearLayout.HORIZONTAL);
        chipContent.setGravity(Gravity.CENTER);
        chipContent.setPadding(tabHPaddingPx, 0, tabHPaddingPx, 0);
        tab.addView(chipContent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        check = new ImageView(getContext());
        check.setImageResource(R.drawable.ic_check_single);
        check.setColorFilter(checkColor());
        check.setVisibility(GONE);
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dp(CHECK_SIZE_DP), dp(CHECK_SIZE_DP));
        checkLp.setMarginEnd(dp(6));
        chipContent.addView(check, checkLp);

        TextView label = new TextView(getContext());
        label.setText(option.label);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_TEXT_SP);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(inactiveTextColor());
        chipContent.addView(label);

        return new TabHolder(tab, label, check);
    }

    private void applyRoundedOutline(FrameLayout tab) {
        tab.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
            }
        });
        tab.setClipToOutline(true);
    }

    private void onTabClicked(int index) {
        if (!isEnabled() || index < 0 || index >= options.size()) return;
        Option option = options.get(index);
        boolean changed;

        if (singleSelect) {
            changed = selectedIds.size() != 1 || !selectedIds.get(0).equals(option.id);
            selectedIds.clear();
            selectedIds.add(option.id);
            positionPill(index, true);
            updateSingleTabStates();
        } else {
            changed = selectedIds.contains(option.id)
                    ? selectedIds.remove(option.id)
                    : selectedIds.add(option.id);
            updateMultiTabStates();
        }

        if (changed && listener != null) {
            listener.onSelectionChanged(this, getSelectedIds());
        }
    }

    private int indexOfSelected() {
        if (selectedIds.isEmpty()) return -1;
        String id = selectedIds.get(0);
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    private void positionPill(int index, boolean animate) {
        if (singleSelect) updateSingleTabStates();
        if (index < 0 || index >= tabs.size()) {
            pill.setVisibility(GONE);
            return;
        }
        View tab = tabs.get(index).view;
        int targetW = Math.max(1, tab.getWidth());
        int targetX = tab.getLeft();

        if (!animate || pill.getVisibility() == GONE || pill.getWidth() == 0) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pill.getLayoutParams();
            lp.width = targetW;
            pill.setLayoutParams(lp);
            pill.setTranslationX(targetX);
            pill.setVisibility(VISIBLE);
            return;
        }

        int startX = (int) pill.getTranslationX();
        int startW = pill.getWidth();
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(220);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float t = a.getAnimatedFraction();
            int x = Math.round(startX + (targetX - startX) * t);
            int w = Math.round(startW + (targetW - startW) * t);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pill.getLayoutParams();
            if (lp.width != w) {
                lp.width = w;
                pill.setLayoutParams(lp);
            }
            pill.setTranslationX(x);
        });
        animator.start();
    }

    private void updateSingleTabStates() {
        for (int i = 0; i < tabs.size(); i++) {
            boolean active = singleSelect && selectedIds.contains(options.get(i).id);
            tabs.get(i).label.setTextColor(active ? activeTextColor() : inactiveTextColor());
        }
    }

    private void updateMultiTabStates() {
        for (int i = 0; i < tabs.size(); i++) {
            boolean selected = selectedIds.contains(options.get(i).id);
            TabHolder holder = tabs.get(i);
            holder.view.setBackground(createChipBackground(selected));
            holder.label.setTextColor(selected ? activeTextColor() : inactiveTextColor());
            if (holder.check != null) {
                holder.check.setColorFilter(checkColor());
                animateCheck(holder.check, selected);
            }
        }
    }

    private void animateCheck(ImageView check, boolean show) {
        check.setVisibility(show ? VISIBLE : GONE);
        if (show) {
            check.setScaleX(0f);
            check.setScaleY(0f);
            check.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        }
    }

    private GradientDrawable createChipBackground(boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(cornerRadiusPx);
        if (selected) {
            bg.setColor(themeAware ? activeFillColor() : getContext().getColor(R.color.indigo_primary));
        } else {
            bg.setColor(themeAware ? getContext().getColor(R.color.surface_variant_color)
                    : getContext().getColor(R.color.white_alpha_14));
            bg.setStroke(trackStrokePx, themeAware ? getContext().getColor(R.color.outline_color)
                    : getContext().getColor(R.color.white_alpha_22));
        }
        return bg;
    }

    private int activeTextColor() {
        return themeAware ? Color.WHITE : getContext().getColor(R.color.white);
    }

    private int inactiveTextColor() {
        return themeAware ? getContext().getColor(R.color.on_surface_variant_color)
                : getContext().getColor(R.color.white_alpha_85);
    }

    private int activeFillColor() {
        return themeAware ? getContext().getColor(R.color.role_teacher_start)
                : getContext().getColor(R.color.indigo_primary);
    }

    private int checkColor() {
        return Color.WHITE;
    }

    private GradientDrawable createTrackDrawable() {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(cornerRadiusPx);
        bg.setColor(getContext().getColor(R.color.surface_variant_color));
        bg.setStroke(trackStrokePx, getContext().getColor(R.color.outline_color));
        return bg;
    }

    private GradientDrawable createPillDrawable() {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{
                getContext().getColor(R.color.role_teacher_start),
                getContext().getColor(R.color.role_teacher_end),
        });
        bg.setCornerRadius(cornerRadiusPx);
        return bg;
    }

    private android.graphics.drawable.Drawable getSelectableItemBackground() {
        android.content.res.TypedArray a = getContext().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground});
        android.graphics.drawable.Drawable d = a.getDrawable(0);
        a.recycle();
        return d;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
