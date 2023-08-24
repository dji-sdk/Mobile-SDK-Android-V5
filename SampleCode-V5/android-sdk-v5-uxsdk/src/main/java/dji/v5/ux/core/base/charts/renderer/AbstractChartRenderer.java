package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public abstract class AbstractChartRenderer implements ChartRenderer {
    public int DEFAULT_LABEL_MARGIN_DP = 4;
    protected Chart chart;
    protected ChartComputator computator;
    protected Paint labelPaint = new Paint();
    protected Paint labelBackgroundPaint = new Paint();
    protected RectF labelBackgroundRect = new RectF();
    protected Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
    protected boolean isViewportCalculationEnabled = true;
    protected float density;
    protected float scaledDensity;
    protected SelectedValue selectedValue = new SelectedValue();
    protected char[] labelBuffer = new char[64];
    protected int labelOffset;
    protected int labelMargin;
    protected boolean isValueLabelBackgroundEnabled;
    protected boolean isValueLabelBackgroundAuto;

    protected AbstractChartRenderer(Context context, Chart chart) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        this.chart = chart;
        this.computator = chart.getChartComputator();
        this.labelMargin = ChartUtils.dp2px(this.density, this.DEFAULT_LABEL_MARGIN_DP);
        this.labelOffset = this.labelMargin;
        this.labelPaint.setAntiAlias(true);
        this.labelPaint.setStyle(Paint.Style.FILL);
        this.labelPaint.setTextAlign(Paint.Align.LEFT);
        this.labelPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        this.labelPaint.setColor(-1);
        this.labelBackgroundPaint.setAntiAlias(true);
        this.labelBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void resetRenderer() {
        this.computator = this.chart.getChartComputator();
    }

    public void onChartDataChanged() {
        ChartData data = this.chart.getChartData();
        Typeface typeface = this.chart.getChartData().getValueLabelTypeface();
        if (null != typeface) {
            this.labelPaint.setTypeface(typeface);
        }

        this.labelPaint.setColor(data.getValueLabelTextColor());
        this.labelPaint.setTextSize((float)ChartUtils.sp2px(this.scaledDensity, data.getValueLabelTextSize()));
        this.labelPaint.getFontMetricsInt(this.fontMetrics);
        this.isValueLabelBackgroundEnabled = data.isValueLabelBackgroundEnabled();
        this.isValueLabelBackgroundAuto = data.isValueLabelBackgroundAuto();
        this.labelBackgroundPaint.setColor(data.getValueLabelBackgroundColor());
        this.selectedValue.clear();
    }

    protected void drawLabelTextAndBackground(Canvas canvas, char[] labelBuffer, int startIndex, int numChars, int autoBackgroundColor) {
        float textX;
        float textY;
        if (this.isValueLabelBackgroundEnabled) {
            if (this.isValueLabelBackgroundAuto) {
                this.labelBackgroundPaint.setColor(autoBackgroundColor);
            }

            canvas.drawRect(this.labelBackgroundRect, this.labelBackgroundPaint);
            textX = this.labelBackgroundRect.left + (float)this.labelMargin;
            textY = this.labelBackgroundRect.bottom - (float)this.labelMargin;
        } else {
            textX = this.labelBackgroundRect.left;
            textY = this.labelBackgroundRect.bottom;
        }

        canvas.drawText(labelBuffer, startIndex, numChars, textX, textY, this.labelPaint);
    }

    public boolean isTouched() {
        return this.selectedValue.isSet();
    }

    public void clearTouch() {
        this.selectedValue.clear();
    }

    public Viewport getMaximumViewport() {
        return this.computator.getMaximumViewport();
    }

    public void setMaximumViewport(Viewport maxViewport) {
        if (null != maxViewport) {
            this.computator.setMaxViewport(maxViewport);
        }

    }

    public Viewport getCurrentViewport() {
        return this.computator.getCurrentViewport();
    }

    public void setCurrentViewport(Viewport viewport) {
        if (null != viewport) {
            this.computator.setCurrentViewport(viewport);
        }

    }

    public boolean isViewportCalculationEnabled() {
        return this.isViewportCalculationEnabled;
    }

    public void setViewportCalculationEnabled(boolean isEnabled) {
        this.isViewportCalculationEnabled = isEnabled;
    }

    public void selectValue(SelectedValue selectedValue) {
        this.selectedValue.set(selectedValue);
    }

    public SelectedValue getSelectedValue() {
        return this.selectedValue;
    }
}

