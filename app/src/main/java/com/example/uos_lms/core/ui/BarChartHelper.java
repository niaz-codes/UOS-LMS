package com.example.uos_lms.core.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.uos_lms.R;

import java.util.List;

/** Java port of the Compose SimpleBarChart - a dependency-free proportional bar chart built
 * from plain Views (two 0dp-width weighted children per row standing in for
 * fillMaxWidth(fraction)), not a charting library. */
public final class BarChartHelper {

    private BarChartHelper() {
    }

    public static void render(LinearLayout container, List<ChartEntry> items, String valueSuffix) {
        container.removeAllViews();
        int maxValue = 1;
        for (ChartEntry entry : items) {
            maxValue = Math.max(maxValue, entry.getValue());
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (ChartEntry entry : items) {
            View row = inflater.inflate(R.layout.item_bar_chart_row, container, false);
            ((TextView) row.findViewById(R.id.textLabel)).setText(entry.getLabel());
            ((TextView) row.findViewById(R.id.textValue)).setText(entry.getValue() + valueSuffix);
            View fill = row.findViewById(R.id.barFill);
            View spacer = row.findViewById(R.id.barSpacer);
            float fraction = entry.getValue() / (float) maxValue;
            ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = fraction;
            ((LinearLayout.LayoutParams) spacer.getLayoutParams()).weight = 1f - fraction;
            container.addView(row);
        }
    }
}
