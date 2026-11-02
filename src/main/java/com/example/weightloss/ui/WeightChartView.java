package com.example.weightloss.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.weightloss.R;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 自定义体重趋势图：绘制平滑折线、目标线与可拖动的数据提示。 */
public class WeightChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tooltipRect = new RectF();

    private final float density;
    private final int colorLine;
    private final int colorGrid;
    private final int colorMuted;
    private final int colorGoal;
    private final int colorSurface;
    private final int colorTooltipText;
    private final int touchSlop;

    private List<WeightRecord> records = Collections.emptyList();
    private double targetWeight;
    private int selectedIndex = -1;
    private float downX;
    private float downY;
    private boolean draggingHorizontally;

    private float plotLeft;
    private float plotRight;
    private float plotTop;
    private float plotBottom;

    public WeightChartView(Context context) {
        this(context, null);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        colorLine = ContextCompat.getColor(context, R.color.chart_line);
        colorGrid = ContextCompat.getColor(context, R.color.chart_grid);
        colorMuted = ContextCompat.getColor(context, R.color.ink_soft);
        colorGoal = ContextCompat.getColor(context, R.color.chart_goal);
        colorSurface = ContextCompat.getColor(context, R.color.chart_tooltip);
        colorTooltipText = ContextCompat.getColor(context, R.color.chart_tooltip_text);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(colorGrid);

        axisTextPaint.setColor(colorMuted);
        axisTextPaint.setTextSize(sp(9));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(colorLine);

        pointFillPaint.setStyle(Paint.Style.FILL);
        pointFillPaint.setColor(colorSurface);

        pointStrokePaint.setStyle(Paint.Style.STROKE);
        pointStrokePaint.setStrokeWidth(dp(1.5f));
        pointStrokePaint.setColor(colorLine);

        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(dp(1));
        goalPaint.setColor(colorGoal);
        goalPaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));

        goalTextPaint.setColor(colorGoal);
        goalTextPaint.setTextSize(sp(9));

        emptyPaint.setColor(colorMuted);
        emptyPaint.setTextSize(sp(11));

        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(dp(1));
        guidePaint.setColor(colorGrid);
        guidePaint.setPathEffect(new DashPathEffect(new float[]{dp(2), dp(3)}, 0));

        tooltipPaint.setStyle(Paint.Style.FILL);
        tooltipPaint.setColor(colorSurface);

        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(dp(1));
        tooltipBorderPaint.setColor(colorGrid);

        tooltipTextPaint.setColor(colorTooltipText);
        tooltipTextPaint.setTextSize(sp(10));

        setClickable(true);
    }

    public void setData(@Nullable List<WeightRecord> data, double targetWeight) {
        // 按日期排序后默认选中最新数据点
        List<WeightRecord> sorted = new ArrayList<>();
        if (data != null) {
            sorted.addAll(data);
        }
        sorted.sort(Comparator.comparingLong(WeightRecord::getDate)
                .thenComparingLong(WeightRecord::getId));
        records = sorted;
        this.targetWeight = targetWeight;
        selectedIndex = records.isEmpty() ? -1 : records.size() - 1;
        setContentDescription(getContext().getString(
                R.string.chart_content_desc, records.size()));
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        plotLeft = getPaddingLeft() + dp(38);
        plotRight = getWidth() - getPaddingRight() - dp(8);
        plotTop = getPaddingTop() + dp(26);
        plotBottom = getHeight() - getPaddingBottom() - dp(24);
        float plotWidth = plotRight - plotLeft;
        float plotHeight = plotBottom - plotTop;

        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        if (records.isEmpty()) {
            emptyPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(getContext().getString(R.string.chart_empty),
                    plotLeft, plotTop + plotHeight / 2f, emptyPaint);
            return;
        }

        double minWeight = records.get(0).getWeight();
        double maxWeight = minWeight;
        for (WeightRecord record : records) {
            minWeight = Math.min(minWeight, record.getWeight());
            maxWeight = Math.max(maxWeight, record.getWeight());
        }

        boolean showGoal = targetWeight > 0
                && targetWeight >= minWeight - 12
                && targetWeight <= maxWeight + 4;
        if (showGoal) {
            minWeight = Math.min(minWeight, targetWeight);
            maxWeight = Math.max(maxWeight, targetWeight);
        }

        double padding = Math.max(1.0, (maxWeight - minWeight) * 0.25);
        minWeight -= padding;
        maxWeight += padding;
        double range = maxWeight - minWeight;

        for (int i = 0; i <= 2; i++) {
            float y = plotTop + plotHeight * i / 2f;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);

            double value = maxWeight - range * i / 2.0;
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(Locale.CHINA, "%.1f", value),
                    plotLeft - dp(7), y + axisTextPaint.getTextSize() * 0.35f, axisTextPaint);
        }

        int count = records.size();
        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            float fraction = count == 1 ? 0.5f : (float) i / (count - 1);
            xs[i] = plotLeft + plotWidth * fraction;
            double weight = records.get(i).getWeight();
            ys[i] = (float) (plotTop + (maxWeight - weight) / range * plotHeight);
        }

        Path linePath = new Path();
        // 用三阶贝塞尔曲线连接相邻点，使折线更平滑
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) {
            float middleX = (xs[i - 1] + xs[i]) / 2f;
            linePath.cubicTo(middleX, ys[i - 1], middleX, ys[i], xs[i], ys[i]);
        }
        canvas.drawPath(linePath, linePaint);

        if (showGoal) {
            float goalY = (float) (plotTop + (maxWeight - targetWeight) / range * plotHeight);
            canvas.drawLine(plotLeft, goalY, plotRight, goalY, goalPaint);
            goalTextPaint.setTextAlign(Paint.Align.RIGHT);
            String goalLabel = String.format(Locale.CHINA, "目标 %s",
                    HealthUtils.formatWeight(targetWeight));
            float goalTextY = goalY - dp(4);
            if (goalTextY < plotTop + goalTextPaint.getTextSize()) {
                goalTextY = goalY + goalTextPaint.getTextSize() + dp(2);
            }
            canvas.drawText(goalLabel, plotRight, goalTextY, goalTextPaint);
        }

        for (int i = 0; i < count; i++) {
            if (i == selectedIndex) {
                continue;
            }
            canvas.drawCircle(xs[i], ys[i], dp(2.6f), pointFillPaint);
            canvas.drawCircle(xs[i], ys[i], dp(2.6f), pointStrokePaint);
        }

        if (selectedIndex >= 0 && selectedIndex < count) {
            float x = xs[selectedIndex];
            float y = ys[selectedIndex];
            canvas.drawLine(x, y + dp(6), x, plotBottom, guidePaint);
            pointStrokePaint.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(x, y, dp(4.2f), pointStrokePaint);
            pointFillPaint.setColor(colorLine);
            canvas.drawCircle(x, y, dp(2.4f), pointFillPaint);
            pointFillPaint.setColor(colorSurface);
            drawTooltip(canvas, x, y);
        }

        axisTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(DateUtils.formatDate(records.get(0).getDate()),
                plotLeft, plotBottom + dp(17), axisTextPaint);
        if (count > 1) {
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(DateUtils.formatDate(records.get(count - 1).getDate()),
                    plotRight, plotBottom + dp(17), axisTextPaint);
        }
    }

    private void drawTooltip(Canvas canvas, float x, float y) {
        if (selectedIndex < 0 || selectedIndex >= records.size()) {
            return;
        }
        WeightRecord record = records.get(selectedIndex);
        String label = getContext().getString(R.string.chart_tooltip_format,
                DateUtils.formatDate(record.getDate()),
                HealthUtils.formatWeight(record.getWeight()));

        float paddingH = dp(9);
        float paddingV = dp(6);
        float textWidth = tooltipTextPaint.measureText(label);
        float textHeight = tooltipTextPaint.getTextSize();
        float width = textWidth + paddingH * 2;
        float height = textHeight + paddingV * 2;

        float left = x - width / 2f;
        left = Math.max(plotLeft, Math.min(left, plotRight - width));
        float top = y - height - dp(12);
        if (top < plotTop - dp(4)) {
            top = y + dp(12);
        }
        tooltipRect.set(left, top, left + width, top + height);

        canvas.drawRoundRect(tooltipRect, dp(4), dp(4), tooltipPaint);
        canvas.drawRoundRect(tooltipRect, dp(4), dp(4), tooltipBorderPaint);
        tooltipTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(label,
                tooltipRect.left + paddingH,
                tooltipRect.top + paddingV + textHeight * 0.82f,
                tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (records.isEmpty()) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                draggingHorizontally = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (dx > touchSlop && dx > dy) {
                    draggingHorizontally = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    selectNearest(event.getX());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!draggingHorizontally) {
                    selectNearest(event.getX());
                }
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void selectNearest(float x) {
        if (records.size() <= 1) {
            selectedIndex = 0;
            invalidate();
            return;
        }
        float plotWidth = plotRight - plotLeft;
        if (plotWidth <= 0) {
            return;
        }
        float fraction = Math.max(0, Math.min(1, (x - plotLeft) / plotWidth));
        int index = Math.round(fraction * (records.size() - 1));
        if (index != selectedIndex) {
            selectedIndex = index;
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
