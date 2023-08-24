package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Iterator;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.formatter.BubbleChartValueFormatter;
import dji.v5.ux.core.base.charts.model.BubbleChartData;
import dji.v5.ux.core.base.charts.model.BubbleValue;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.ValueShape;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.provider.BubbleChartDataProvider;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class BubbleChartRenderer extends AbstractChartRenderer {
    private BubbleChartDataProvider dataProvider;
    private int touchAdditional;
    private float bubbleScaleX;
    private float bubbleScaleY;
    private boolean isBubbleScaledByX = true;
    private float maxRadius;
    private float minRawRadius;
    private PointF bubbleCenter = new PointF();
    private Paint bubblePaint = new Paint();
    private RectF bubbleRect = new RectF();
    private boolean hasLabels;
    private boolean hasLabelsOnlyForSelected;
    private BubbleChartValueFormatter valueFormatter;
    private Viewport tempMaximumViewport = new Viewport();

    public BubbleChartRenderer(Context context, Chart chart, BubbleChartDataProvider dataProvider) {
        super(context, chart);
        this.dataProvider = dataProvider;
        this.touchAdditional = ChartUtils.dp2px(this.density, 4);
        this.bubblePaint.setAntiAlias(true);
        this.bubblePaint.setStyle(Paint.Style.FILL);
    }

    public void onChartSizeChanged() {
        ChartComputator computator = this.chart.getChartComputator();
        Rect contentRect = computator.getContentRectMinusAllMargins();
        if (contentRect.width() < contentRect.height()) {
            this.isBubbleScaledByX = true;
        } else {
            this.isBubbleScaledByX = false;
        }

    }

    @Override
    public void onChartDataChanged() {
        super.onChartDataChanged();
        BubbleChartData data = this.dataProvider.getBubbleChartData();
        this.hasLabels = data.hasLabels();
        this.hasLabelsOnlyForSelected = data.hasLabelsOnlyForSelected();
        this.valueFormatter = data.getFormatter();
        this.onChartViewportChanged();
    }

    public void onChartViewportChanged() {
        if (this.isViewportCalculationEnabled) {
            this.calculateMaxViewport();
            this.computator.setMaxViewport(this.tempMaximumViewport);
            this.computator.setCurrentViewport(this.computator.getMaximumViewport());
        }

    }

    public void draw(Canvas canvas) {
        this.drawBubbles(canvas);
        if (this.isTouched()) {
            this.highlightBubbles(canvas);
        }

    }

    public void drawUnclipped(Canvas canvas) {
        //do nothing
    }

    public boolean checkTouch(float touchX, float touchY) {
        this.selectedValue.clear();
        BubbleChartData data = this.dataProvider.getBubbleChartData();
        int valueIndex = 0;

        for(Iterator<BubbleValue> var5 = data.getValues().iterator(); var5.hasNext(); ++valueIndex) {
            BubbleValue bubbleValue = (BubbleValue)var5.next();
            float rawRadius = this.processBubble(bubbleValue);
            if (ValueShape.SQUARE.equals(bubbleValue.getShape())) {
                if (this.bubbleRect.contains(touchX, touchY)) {
                    this.selectedValue.set(valueIndex, valueIndex, SelectedValue.SelectedValueType.NONE);
                }
            } else {
                if (!ValueShape.CIRCLE.equals(bubbleValue.getShape())) {
                    throw new IllegalArgumentException("Invalid bubble shape: " + bubbleValue.getShape());
                }

                float diffX = touchX - this.bubbleCenter.x;
                float diffY = touchY - this.bubbleCenter.y;
                float touchDistance = (float)Math.sqrt((diffX * diffX + diffY * diffY));
                if (touchDistance <= rawRadius) {
                    this.selectedValue.set(valueIndex, valueIndex, SelectedValue.SelectedValueType.NONE);
                }
            }
        }

        return this.isTouched();
    }

    public void removeMargins() {
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        if (contentRect.height() != 0 && contentRect.width() != 0) {
            float pxX = this.computator.computeRawDistanceX(this.maxRadius * this.bubbleScaleX);
            float pxY = this.computator.computeRawDistanceY(this.maxRadius * this.bubbleScaleY);
            float scaleX = this.computator.getMaximumViewport().width() / (float)contentRect.width();
            float scaleY = this.computator.getMaximumViewport().height() / (float)contentRect.height();
            float dx = 0.0F;
            float dy = 0.0F;
            if (this.isBubbleScaledByX) {
                dy = (pxY - pxX) * scaleY * 0.75F;
            } else {
                dx = (pxX - pxY) * scaleX * 0.75F;
            }

            Viewport maxViewport = this.computator.getMaximumViewport();
            maxViewport.inset(dx, dy);
            Viewport currentViewport = this.computator.getCurrentViewport();
            currentViewport.inset(dx, dy);
            this.computator.setMaxViewport(maxViewport);
            this.computator.setCurrentViewport(currentViewport);
        }
    }

    private void drawBubbles(Canvas canvas) {
        BubbleChartData data = this.dataProvider.getBubbleChartData();
        Iterator<BubbleValue> var3 = data.getValues().iterator();

        while(var3.hasNext()) {
            BubbleValue bubbleValue = (BubbleValue)var3.next();
            this.drawBubble(canvas, bubbleValue);
        }

    }

    private void drawBubble(Canvas canvas, BubbleValue bubbleValue) {
        float rawRadius = this.processBubble(bubbleValue);
        rawRadius -= (float)this.touchAdditional;
        this.bubbleRect.inset((float)this.touchAdditional, (float)this.touchAdditional);
        this.bubblePaint.setColor(bubbleValue.getColor());
        this.drawBubbleShapeAndLabel(canvas, bubbleValue, rawRadius, 0);
    }

    private void drawBubbleShapeAndLabel(Canvas canvas, BubbleValue bubbleValue, float rawRadius, int mode) {
        if (ValueShape.SQUARE.equals(bubbleValue.getShape())) {
            canvas.drawRect(this.bubbleRect, this.bubblePaint);
        } else {
            if (!ValueShape.CIRCLE.equals(bubbleValue.getShape())) {
                throw new IllegalArgumentException("Invalid bubble shape: " + bubbleValue.getShape());
            }

            canvas.drawCircle(this.bubbleCenter.x, this.bubbleCenter.y, rawRadius, this.bubblePaint);
        }

        if (1 == mode) {
            if (this.hasLabels || this.hasLabelsOnlyForSelected) {
                this.drawLabel(canvas, bubbleValue, this.bubbleCenter.x, this.bubbleCenter.y);
            }
        } else {
            if (0 != mode) {
                throw new IllegalStateException("Cannot process bubble in mode: " + mode);
            }

            if (this.hasLabels) {
                this.drawLabel(canvas, bubbleValue, this.bubbleCenter.x, this.bubbleCenter.y);
            }
        }

    }

    private void highlightBubbles(Canvas canvas) {
        BubbleChartData data = this.dataProvider.getBubbleChartData();
        BubbleValue bubbleValue = (BubbleValue)data.getValues().get(this.selectedValue.getFirstIndex());
        this.highlightBubble(canvas, bubbleValue);
    }

    private void highlightBubble(Canvas canvas, BubbleValue bubbleValue) {
        float rawRadius = this.processBubble(bubbleValue);
        this.bubblePaint.setColor(bubbleValue.getDarkenColor());
        this.drawBubbleShapeAndLabel(canvas, bubbleValue, rawRadius, 1);
    }

    private float processBubble(BubbleValue bubbleValue) {
        float rawX = this.computator.computeRawX(bubbleValue.getX());
        float rawY = this.computator.computeRawY(bubbleValue.getY());
        float radius = (float)Math.sqrt((double)Math.abs(bubbleValue.getZ()) / Math.PI);
        float rawRadius;
        if (this.isBubbleScaledByX) {
            radius *= this.bubbleScaleX;
            rawRadius = this.computator.computeRawDistanceX(radius);
        } else {
            radius *= this.bubbleScaleY;
            rawRadius = this.computator.computeRawDistanceY(radius);
        }

        if (rawRadius < this.minRawRadius + (float)this.touchAdditional) {
            rawRadius = this.minRawRadius + (float)this.touchAdditional;
        }

        this.bubbleCenter.set(rawX, rawY);
        if (ValueShape.SQUARE.equals(bubbleValue.getShape())) {
            this.bubbleRect.set(rawX - rawRadius, rawY - rawRadius, rawX + rawRadius, rawY + rawRadius);
        }

        return rawRadius;
    }

    private void drawLabel(Canvas canvas, BubbleValue bubbleValue, float rawX, float rawY) {
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        int numChars = this.valueFormatter.formatChartValue(this.labelBuffer, bubbleValue);
        if (numChars != 0) {
            float labelWidth = this.labelPaint.measureText(this.labelBuffer, this.labelBuffer.length - numChars, numChars);
            int labelHeight = Math.abs(this.fontMetrics.ascent);
            float left = rawX - labelWidth / 2.0F - (float)this.labelMargin;
            float right = rawX + labelWidth / 2.0F + (float)this.labelMargin;
            float top = rawY - (float)((double)labelHeight / 2) - (float)this.labelMargin;
            float bottom = rawY + (float)((double)labelHeight / 2) + (float)this.labelMargin;
            if (top < (float)contentRect.top) {
                top = rawY;
                bottom = rawY + (float)labelHeight + (float)(this.labelMargin * 2);
            }

            if (bottom > (float)contentRect.bottom) {
                top = rawY - (float)labelHeight - (float)(this.labelMargin * 2);
                bottom = rawY;
            }

            if (left < (float)contentRect.left) {
                left = rawX;
                right = rawX + labelWidth + (float)(this.labelMargin * 2);
            }

            if (right > (float)contentRect.right) {
                left = rawX - labelWidth - (float)(this.labelMargin * 2);
                right = rawX;
            }

            this.labelBackgroundRect.set(left, top, right, bottom);
            this.drawLabelTextAndBackground(canvas, this.labelBuffer, this.labelBuffer.length - numChars, numChars, bubbleValue.getDarkenColor());
        }
    }

    private void calculateMaxViewport() {
        float maxZ = Float.MIN_VALUE;
        this.tempMaximumViewport.set(Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        BubbleChartData data = this.dataProvider.getBubbleChartData();
        Iterator<BubbleValue> var3 = data.getValues().iterator();

        while(var3.hasNext()) {
            BubbleValue bubbleValue = (BubbleValue)var3.next();
            if (Math.abs(bubbleValue.getZ()) > maxZ) {
                maxZ = Math.abs(bubbleValue.getZ());
            }

            if (bubbleValue.getX() < this.tempMaximumViewport.left) {
                this.tempMaximumViewport.left = bubbleValue.getX();
            }

            if (bubbleValue.getX() > this.tempMaximumViewport.right) {
                this.tempMaximumViewport.right = bubbleValue.getX();
            }

            if (bubbleValue.getY() < this.tempMaximumViewport.bottom) {
                this.tempMaximumViewport.bottom = bubbleValue.getY();
            }

            if (bubbleValue.getY() > this.tempMaximumViewport.top) {
                this.tempMaximumViewport.top = bubbleValue.getY();
            }
        }

        this.maxRadius = (float)Math.sqrt((double)maxZ / Math.PI);
        this.bubbleScaleX = this.tempMaximumViewport.width() / (this.maxRadius * 4.0F);
        if (this.bubbleScaleX == 0.0F) {
            this.bubbleScaleX = 1.0F;
        }

        this.bubbleScaleY = this.tempMaximumViewport.height() / (this.maxRadius * 4.0F);
        if (this.bubbleScaleY == 0.0F) {
            this.bubbleScaleY = 1.0F;
        }

        this.bubbleScaleX *= data.getBubbleScale();
        this.bubbleScaleY *= data.getBubbleScale();
        this.tempMaximumViewport.inset(-this.maxRadius * this.bubbleScaleX, -this.maxRadius * this.bubbleScaleY);
        this.minRawRadius = (float)ChartUtils.dp2px(this.density, this.dataProvider.getBubbleChartData().getMinBubbleRadius());
    }
}

