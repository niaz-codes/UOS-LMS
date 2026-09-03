package com.example.uos_lms.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Small decorative trend flourish for a premium stat card. Not a literal chart of historical
 * data - the app has no day-by-day history to plot - just a stylised curve whose overall slope
 * honestly reflects the real trend direction (rising when this month's delta is positive, flat
 * when it's zero), set via {@link #setTrendUp(boolean)}. */
public class TrendSparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean trendUp = true;

    public TrendSparklineView(Context context) {
        this(context, null);
    }

    public TrendSparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float strokeWidth = 2f * getResources().getDisplayMetrics().density;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(strokeWidth);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setAccentColor(int color) {
        linePaint.setColor(color);
        dotPaint.setColor(color);
        invalidate();
    }

    public void setTrendUp(boolean up) {
        if (this.trendUp == up) return;
        this.trendUp = up;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float pad = h * 0.15f;
        float top = pad;
        float bottom = h - pad;
        float span = bottom - top;

        float x0 = 0, y0 = trendUp ? bottom : bottom - span * 0.35f;
        float x1 = w * 0.35f, y1 = trendUp ? bottom - span * 0.25f : bottom - span * 0.4f;
        float x2 = w * 0.68f, y2 = trendUp ? bottom - span * 0.55f : bottom - span * 0.3f;
        float x3 = w, y3 = trendUp ? top : bottom - span * 0.3f;

        path.reset();
        path.moveTo(x0, y0);
        path.cubicTo(x1, y1, x2, y2, x3, y3);
        canvas.drawPath(path, linePaint);

        float dotRadius = h * 0.09f;
        canvas.drawCircle(x3, y3, dotRadius, dotPaint);
    }
}
