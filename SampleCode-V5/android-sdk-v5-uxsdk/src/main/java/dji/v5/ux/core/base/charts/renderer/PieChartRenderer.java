package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

import java.util.Iterator;

import dji.v5.ux.core.base.charts.formatter.PieChartValueFormatter;
import dji.v5.ux.core.base.charts.model.PieChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.SliceValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.provider.PieChartDataProvider;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class PieChartRenderer extends AbstractChartRenderer {
    private int rotation = 45;
    private PieChartDataProvider dataProvider;
    private Paint slicePaint = new Paint();
    private float maxSum;
    private RectF originCircleOval = new RectF();
    private RectF drawCircleOval = new RectF();
    private PointF sliceVector = new PointF();
    private int touchAdditional;
    private float circleFillRatio = 1.0F;
    private boolean hasCenterCircle;
    private float centerCircleScale;
    private Paint centerCirclePaint = new Paint();
    private Paint centerCircleText1Paint = new Paint();
    private Paint.FontMetricsInt centerCircleText1FontMetrics = new Paint.FontMetricsInt();
    private Paint centerCircleText2Paint = new Paint();
    private Paint.FontMetricsInt centerCircleText2FontMetrics = new Paint.FontMetricsInt();
    private Paint separationLinesPaint = new Paint();
    private boolean hasLabelsOutside;
    private boolean hasLabels;
    private boolean hasLabelsOnlyForSelected;
    private PieChartValueFormatter valueFormatter;
    private Viewport tempMaximumViewport = new Viewport();
    private Bitmap softwareBitmap;
    private Canvas softwareCanvas = new Canvas();

    public PieChartRenderer(Context context, Chart chart, PieChartDataProvider dataProvider) {
        super(context, chart);
        this.dataProvider = dataProvider;
        this.touchAdditional = ChartUtils.dp2px(this.density, 8);
        this.slicePaint.setAntiAlias(true);
        this.slicePaint.setStyle(Paint.Style.FILL);
        this.centerCirclePaint.setAntiAlias(true);
        this.centerCirclePaint.setStyle(Paint.Style.FILL);
        this.centerCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.centerCircleText1Paint.setAntiAlias(true);
        this.centerCircleText1Paint.setTextAlign(Paint.Align.CENTER);
        this.centerCircleText2Paint.setAntiAlias(true);
        this.centerCircleText2Paint.setTextAlign(Paint.Align.CENTER);
        this.separationLinesPaint.setAntiAlias(true);
        this.separationLinesPaint.setStyle(Paint.Style.STROKE);
        this.separationLinesPaint.setStrokeCap(Paint.Cap.ROUND);
        this.separationLinesPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.separationLinesPaint.setColor(0);
    }

    public void onChartSizeChanged() {
        this.calculateCircleOval();
        if (this.computator.getChartWidth() > 0 && this.computator.getChartHeight() > 0) {
            this.softwareBitmap = Bitmap.createBitmap(this.computator.getChartWidth(), this.computator.getChartHeight(), Bitmap.Config.ARGB_8888);
            this.softwareCanvas.setBitmap(this.softwareBitmap);
        }

    }

    @Override
    public void onChartDataChanged() {
        super.onChartDataChanged();
        PieChartData data = this.dataProvider.getPieChartData();
        this.hasLabelsOutside = data.hasLabelsOutside();
        this.hasLabels = data.hasLabels();
        this.hasLabelsOnlyForSelected = data.hasLabelsOnlyForSelected();
        this.valueFormatter = data.getFormatter();
        this.hasCenterCircle = data.hasCenterCircle();
        this.centerCircleScale = data.getCenterCircleScale();
        this.centerCirclePaint.setColor(data.getCenterCircleColor());
        if (null != data.getCenterText1Typeface()) {
            this.centerCircleText1Paint.setTypeface(data.getCenterText1Typeface());
        }

        this.centerCircleText1Paint.setTextSize((float)ChartUtils.sp2px(this.scaledDensity, data.getCenterText1FontSize()));
        this.centerCircleText1Paint.setColor(data.getCenterText1Color());
        this.centerCircleText1Paint.getFontMetricsInt(this.centerCircleText1FontMetrics);
        if (null != data.getCenterText2Typeface()) {
            this.centerCircleText2Paint.setTypeface(data.getCenterText2Typeface());
        }

        this.centerCircleText2Paint.setTextSize((float)ChartUtils.sp2px(this.scaledDensity, data.getCenterText2FontSize()));
        this.centerCircleText2Paint.setColor(data.getCenterText2Color());
        this.centerCircleText2Paint.getFontMetricsInt(this.centerCircleText2FontMetrics);
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
        Canvas drawCanvas;
        if (null != this.softwareBitmap) {
            drawCanvas = this.softwareCanvas;
            drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        } else {
            drawCanvas = canvas;
        }

        this.drawSlices(drawCanvas);
        this.drawSeparationLines(drawCanvas);
        if (this.hasCenterCircle) {
            this.drawCenterCircle(drawCanvas);
        }

        this.drawLabels(drawCanvas);
        if (null != this.softwareBitmap) {
            canvas.drawBitmap(this.softwareBitmap, 0.0F, 0.0F, (Paint)null);
        }

    }

    public void drawUnclipped(Canvas canvas) {
        //do nothing
    }

    public boolean checkTouch(float touchX, float touchY) {
        this.selectedValue.clear();
        PieChartData data = this.dataProvider.getPieChartData();
        float centerX = this.originCircleOval.centerX();
        float centerY = this.originCircleOval.centerY();
        float circleRadius = this.originCircleOval.width() / 2.0F;
        this.sliceVector.set(touchX - centerX, touchY - centerY);
        if (this.sliceVector.length() > circleRadius + (float)this.touchAdditional) {
            return false;
        } else if (data.hasCenterCircle() && this.sliceVector.length() < circleRadius * data.getCenterCircleScale()) {
            return false;
        } else {
            float touchAngle = (this.pointToAngle(touchX, touchY, centerX, centerY) - (float)this.rotation + 360.0F) % 360.0F;
            float sliceScale = 360.0F / this.maxSum;
            float lastAngle = 0.0F;
            int sliceIndex = 0;

            for(Iterator<SliceValue> var11 = data.getValues().iterator(); var11.hasNext(); ++sliceIndex) {
                SliceValue sliceValue = (SliceValue)var11.next();
                float angle = Math.abs(sliceValue.getValue()) * sliceScale;
                if (touchAngle >= lastAngle) {
                    this.selectedValue.set(sliceIndex, sliceIndex, SelectedValue.SelectedValueType.NONE);
                }

                lastAngle += angle;
            }

            return this.isTouched();
        }
    }

    private void drawCenterCircle(Canvas canvas) {
        PieChartData data = this.dataProvider.getPieChartData();
        float circleRadius = this.originCircleOval.width() / 2.0F;
        float centerRadius = circleRadius * data.getCenterCircleScale();
        float centerX = this.originCircleOval.centerX();
        float centerY = this.originCircleOval.centerY();
        canvas.drawCircle(centerX, centerY, centerRadius, this.centerCirclePaint);
        if (!TextUtils.isEmpty(data.getCenterText1())) {
            int text1Height = Math.abs(this.centerCircleText1FontMetrics.ascent);
            if (!TextUtils.isEmpty(data.getCenterText2())) {
                int text2Height = Math.abs(this.centerCircleText2FontMetrics.ascent);
                canvas.drawText(data.getCenterText1(), centerX, centerY - (float)text1Height * 0.2F, this.centerCircleText1Paint);
                canvas.drawText(data.getCenterText2(), centerX, centerY + (float)text2Height, this.centerCircleText2Paint);
            } else {
                canvas.drawText(data.getCenterText1(), centerX, centerY + (float)((double)text1Height / 4), this.centerCircleText1Paint);
            }
        }

    }

    private void drawSlices(Canvas canvas) {
        PieChartData data = this.dataProvider.getPieChartData();
        float sliceScale = 360.0F / this.maxSum;
        float lastAngle = (float)this.rotation;
        int sliceIndex = 0;

        for(Iterator<SliceValue> var6 = data.getValues().iterator(); var6.hasNext(); ++sliceIndex) {
            SliceValue sliceValue = (SliceValue)var6.next();
            float angle = Math.abs(sliceValue.getValue()) * sliceScale;
            if (this.isTouched() && this.selectedValue.getFirstIndex() == sliceIndex) {
                this.drawSlice(canvas, sliceValue, lastAngle, angle, 1);
            } else {
                this.drawSlice(canvas, sliceValue, lastAngle, angle, 0);
            }

            lastAngle += angle;
        }

    }

    private void drawSeparationLines(Canvas canvas) {
        PieChartData data = this.dataProvider.getPieChartData();
        if (data.getValues().size() >= 2) {
            int sliceSpacing = ChartUtils.dp2px(this.density, data.getSlicesSpacing());
            if (sliceSpacing >= 1) {
                float sliceScale = 360.0F / this.maxSum;
                float lastAngle = (float)this.rotation;
                float circleRadius = this.originCircleOval.width() / 2.0F;
                this.separationLinesPaint.setStrokeWidth((float)sliceSpacing);

                float angle;
                for(Iterator<SliceValue> var7 = data.getValues().iterator(); var7.hasNext(); lastAngle += angle) {
                    SliceValue sliceValue = (SliceValue)var7.next();
                    angle = Math.abs(sliceValue.getValue()) * sliceScale;
                    this.sliceVector.set((float)Math.cos(Math.toRadians((double)lastAngle)), (float)Math.sin(Math.toRadians((double)lastAngle)));
                    this.normalizeVector(this.sliceVector);
                    float x1 = this.sliceVector.x * (circleRadius + (float)this.touchAdditional) + this.originCircleOval.centerX();
                    float y1 = this.sliceVector.y * (circleRadius + (float)this.touchAdditional) + this.originCircleOval.centerY();
                    canvas.drawLine(this.originCircleOval.centerX(), this.originCircleOval.centerY(), x1, y1, this.separationLinesPaint);
                }

            }
        }
    }

    public void drawLabels(Canvas canvas) {
        PieChartData data = this.dataProvider.getPieChartData();
        float sliceScale = 360.0F / this.maxSum;
        float lastAngle = (float)this.rotation;
        int sliceIndex = 0;

        for(Iterator<SliceValue> var6 = data.getValues().iterator(); var6.hasNext(); ++sliceIndex) {
            SliceValue sliceValue = (SliceValue)var6.next();
            float angle = Math.abs(sliceValue.getValue()) * sliceScale;
            if (this.hasLabels || (this.hasLabelsOnlyForSelected && this.selectedValue.getFirstIndex() == sliceIndex)) {
                this.drawLabel(canvas, sliceValue, lastAngle, angle);
            }

            lastAngle += angle;
        }

    }

    private void drawSlice(Canvas canvas, SliceValue sliceValue, float lastAngle, float angle, int mode) {
        this.sliceVector.set((float)Math.cos(Math.toRadians((double)(lastAngle + angle / 2.0F))), (float)Math.sin(Math.toRadians((double)(lastAngle + angle / 2.0F))));
        this.normalizeVector(this.sliceVector);
        this.drawCircleOval.set(this.originCircleOval);
        if (1 == mode) {
            this.drawCircleOval.inset((float)(-this.touchAdditional), (float)(-this.touchAdditional));
            this.slicePaint.setColor(sliceValue.getDarkenColor());
            canvas.drawArc(this.drawCircleOval, lastAngle, angle, true, this.slicePaint);
        } else {
            this.slicePaint.setColor(sliceValue.getColor());
            canvas.drawArc(this.drawCircleOval, lastAngle, angle, true, this.slicePaint);
        }

    }

    private void drawLabel(Canvas canvas, SliceValue sliceValue, float lastAngle, float angle) {
        this.sliceVector.set((float)Math.cos(Math.toRadians((double)(lastAngle + angle / 2.0F))), (float)Math.sin(Math.toRadians((double)(lastAngle + angle / 2.0F))));
        this.normalizeVector(this.sliceVector);
        int numChars = this.valueFormatter.formatChartValue(this.labelBuffer, sliceValue);
        if (numChars != 0) {
            float labelWidth = this.labelPaint.measureText(this.labelBuffer, this.labelBuffer.length - numChars, numChars);
            int labelHeight = Math.abs(this.fontMetrics.ascent);
            float centerX = this.originCircleOval.centerX();
            float centerY = this.originCircleOval.centerY();
            float circleRadius = this.originCircleOval.width() / 2.0F;
            float labelRadius;
            if (this.hasLabelsOutside) {
                labelRadius = circleRadius * 1.0F;
            } else if (this.hasCenterCircle) {
                labelRadius = circleRadius - (circleRadius - circleRadius * this.centerCircleScale) / 2.0F;
            } else {
                labelRadius = circleRadius * 0.7F;
            }

            float rawX = labelRadius * this.sliceVector.x + centerX;
            float rawY = labelRadius * this.sliceVector.y + centerY;
            float left;
            float right;
            float top;
            float bottom;
            if (this.hasLabelsOutside) {
                if (rawX > centerX) {
                    left = rawX + (float)this.labelMargin;
                    right = rawX + labelWidth + (float)(this.labelMargin * 3);
                } else {
                    left = rawX - labelWidth - (float)(this.labelMargin * 3);
                    right = rawX - (float)this.labelMargin;
                }

                if (rawY > centerY) {
                    top = rawY + (float)this.labelMargin;
                    bottom = rawY + (float)labelHeight + (float)(this.labelMargin * 3);
                } else {
                    top = rawY - (float)labelHeight - (float)(this.labelMargin * 3);
                    bottom = rawY - (float)this.labelMargin;
                }
            } else {
                left = rawX - labelWidth / 2.0F - (float)this.labelMargin;
                right = rawX + labelWidth / 2.0F + (float)this.labelMargin;
                top = rawY - (float)((double)labelHeight / 2) - (float)this.labelMargin;
                bottom = rawY + (float)((double)labelHeight / 2) + (float)this.labelMargin;
            }

            this.labelBackgroundRect.set(left, top, right, bottom);
            this.drawLabelTextAndBackground(canvas, this.labelBuffer, this.labelBuffer.length - numChars, numChars, sliceValue.getDarkenColor());
        }
    }

    private void normalizeVector(PointF point) {
        float abs = point.length();
        point.set(point.x / abs, point.y / abs);
    }

    private float pointToAngle(float x, float y, float centerX, float centerY) {
        double diffX = (double)(x - centerX);
        double diffY = (double)(y - centerY);
        double radian = Math.atan2(-diffX, diffY);
        float angle = ((float)Math.toDegrees(radian) + 360.0F) % 360.0F;
        angle += 90.0F;
        return angle;
    }

    private void calculateCircleOval() {
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        float circleRadius = Math.min((float)contentRect.width() / 2.0F, (float)contentRect.height() / 2.0F);
        float centerX = (float)contentRect.centerX();
        float centerY = (float)contentRect.centerY();
        float left = centerX - circleRadius + (float)this.touchAdditional;
        float top = centerY - circleRadius + (float)this.touchAdditional;
        float right = centerX + circleRadius - (float)this.touchAdditional;
        float bottom = centerY + circleRadius - (float)this.touchAdditional;
        this.originCircleOval.set(left, top, right, bottom);
        float inest = 0.5F * this.originCircleOval.width() * (1.0F - this.circleFillRatio);
        this.originCircleOval.inset(inest, inest);
    }

    private void calculateMaxViewport() {
        this.tempMaximumViewport.set(0.0F, 100.0F, 100.0F, 0.0F);
        this.maxSum = 0.0F;

        SliceValue sliceValue;
        for(Iterator<SliceValue> var1 = this.dataProvider.getPieChartData().getValues().iterator(); var1.hasNext(); this.maxSum += Math.abs(sliceValue.getValue())) {
            sliceValue = (SliceValue)var1.next();
        }

    }

    public RectF getCircleOval() {
        return this.originCircleOval;
    }

    public void setCircleOval(RectF orginCircleOval) {
        this.originCircleOval = orginCircleOval;
    }

    public int getChartRotation() {
        return this.rotation;
    }

    public void setChartRotation(int rotation) {
        rotation = (rotation % 360 + 360) % 360;
        this.rotation = rotation;
    }

    public SliceValue getValueForAngle(int angle, SelectedValue selectedValue) {
        PieChartData data = this.dataProvider.getPieChartData();
        float touchAngle = ((float)(angle - this.rotation) + 360.0F) % 360.0F;
        float sliceScale = 360.0F / this.maxSum;
        float lastAngle = 0.0F;
        int sliceIndex = 0;

        for(Iterator<SliceValue> var8 = data.getValues().iterator(); var8.hasNext(); ++sliceIndex) {
            SliceValue sliceValue = (SliceValue)var8.next();
            float tempAngle = Math.abs(sliceValue.getValue()) * sliceScale;
            if (touchAngle >= lastAngle) {
                if (null != selectedValue) {
                    selectedValue.set(sliceIndex, sliceIndex, SelectedValue.SelectedValueType.NONE);
                }

                return sliceValue;
            }

            lastAngle += tempAngle;
        }

        return null;
    }

    public float getCircleFillRatio() {
        return this.circleFillRatio;
    }

    public void setCircleFillRatio(float fillRatio) {
        if (fillRatio < 0.0F) {
            fillRatio = 0.0F;
        } else if (fillRatio > 1.0F) {
            fillRatio = 1.0F;
        }

        this.circleFillRatio = fillRatio;
        this.calculateCircleOval();
    }
}
