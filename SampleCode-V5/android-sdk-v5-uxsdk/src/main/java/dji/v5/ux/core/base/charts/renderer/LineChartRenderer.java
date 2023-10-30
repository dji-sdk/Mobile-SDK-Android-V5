package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;

import java.util.Iterator;

import dji.v5.ux.core.base.charts.model.Line;
import dji.v5.ux.core.base.charts.model.LineChartData;
import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.ValueShape;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.provider.LineChartDataProvider;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class LineChartRenderer extends AbstractChartRenderer {
    private LineChartDataProvider dataProvider;
    private int checkPrecision;
    private float baseValue;
    private int touchToleranceMargin;
    private Path path = new Path();
    private Paint linePaint = new Paint();
    private Paint pointPaint = new Paint();
    private Bitmap softwareBitmap;
    private Canvas softwareCanvas = new Canvas();
    private Viewport tempMaximumViewport = new Viewport();

    public LineChartRenderer(Context context, Chart chart, LineChartDataProvider dataProvider) {
        super(context, chart);
        this.dataProvider = dataProvider;
        this.touchToleranceMargin = ChartUtils.dp2px(this.density, 4);
        this.linePaint.setAntiAlias(true);
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeCap(Paint.Cap.ROUND);
        this.linePaint.setStrokeWidth((float)ChartUtils.dp2px(this.density, 3));
        this.pointPaint.setAntiAlias(true);
        this.pointPaint.setStyle(Paint.Style.FILL);
        this.checkPrecision = ChartUtils.dp2px(this.density, 2);
    }

    public void setShader(Shader shader) {
        this.linePaint.setShader(shader);
    }

    public void onChartSizeChanged() {
        int internalMargin = this.calculateContentRectInternalMargin();
        this.computator.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);
        if (this.computator.getChartWidth() > 0 && this.computator.getChartHeight() > 0) {
            this.softwareBitmap = Bitmap.createBitmap(this.computator.getChartWidth(), this.computator.getChartHeight(), Bitmap.Config.ARGB_8888);
            this.softwareCanvas.setBitmap(this.softwareBitmap);
        }

    }

    @Override
    public void onChartDataChanged() {
        super.onChartDataChanged();
        int internalMargin = this.calculateContentRectInternalMargin();
        this.computator.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);
        this.baseValue = this.dataProvider.getLineChartData().getBaseValue();
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
        LineChartData data = this.dataProvider.getLineChartData();
        Canvas drawCanvas;
        if (null != this.softwareBitmap) {
            drawCanvas = this.softwareCanvas;
            drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        } else {
            drawCanvas = canvas;
        }

        Iterator<Line> var4 = data.getLines().iterator();

        while(var4.hasNext()) {
            Line line = (Line)var4.next();
            if (line.hasLines()) {
                if (line.isCubic()) {
                    this.drawSmoothPath(drawCanvas, line);
                } else if (line.isSquare()) {
                    this.drawSquarePath(drawCanvas, line);
                } else {
                    this.drawPath(drawCanvas, line);
                }
            }
        }

        if (null != this.softwareBitmap) {
            canvas.drawBitmap(this.softwareBitmap, 0.0F, 0.0F, (Paint)null);
        }

    }

    public void drawUnclipped(Canvas canvas) {
        LineChartData data = this.dataProvider.getLineChartData();
        int lineIndex = 0;

        for(Iterator<Line> var4 = data.getLines().iterator(); var4.hasNext(); ++lineIndex) {
            Line line = (Line)var4.next();
            if (this.checkIfShouldDrawPoints(line)) {
                this.drawPoints(canvas, line, lineIndex, 0);
            }
        }

        if (this.isTouched()) {
            this.highlightPoints(canvas);
        }

    }

    private boolean checkIfShouldDrawPoints(Line line) {
        return line.hasPoints() || line.getValues().size() == 1;
    }

    public boolean checkTouch(float touchX, float touchY) {
        this.selectedValue.clear();
        LineChartData data = this.dataProvider.getLineChartData();
        int lineIndex = 0;

        for(Iterator<Line> var5 = data.getLines().iterator(); var5.hasNext(); ++lineIndex) {
            Line line = (Line)var5.next();
            if (this.checkIfShouldDrawPoints(line)) {
                int pointRadius = ChartUtils.dp2px(this.density, line.getPointRadius());
                int valueIndex = 0;

                for(Iterator<PointValue> var9 = line.getValues().iterator(); var9.hasNext(); ++valueIndex) {
                    PointValue pointValue = (PointValue)var9.next();
                    float rawValueX = this.computator.computeRawX(pointValue.getX());
                    float rawValueY = this.computator.computeRawY(pointValue.getY());
                    if (this.isInArea(rawValueX, rawValueY, touchX, touchY, (float)(pointRadius + this.touchToleranceMargin))) {
                        this.selectedValue.set(lineIndex, valueIndex, SelectedValue.SelectedValueType.LINE);
                    }
                }
            }
        }

        return this.isTouched();
    }

    private void calculateMaxViewport() {
        this.tempMaximumViewport.set(Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        LineChartData data = this.dataProvider.getLineChartData();
        Iterator<Line> var2 = data.getLines().iterator();

        while(var2.hasNext()) {
            Line line = (Line)var2.next();
            Iterator<PointValue> var4 = line.getValues().iterator();

            while(var4.hasNext()) {
                PointValue pointValue = (PointValue)var4.next();
                if (pointValue.getX() < this.tempMaximumViewport.left) {
                    this.tempMaximumViewport.left = pointValue.getX();
                }

                if (pointValue.getX() > this.tempMaximumViewport.right) {
                    this.tempMaximumViewport.right = pointValue.getX();
                }

                if (pointValue.getY() < this.tempMaximumViewport.bottom) {
                    this.tempMaximumViewport.bottom = pointValue.getY();
                }

                if (pointValue.getY() > this.tempMaximumViewport.top) {
                    this.tempMaximumViewport.top = pointValue.getY();
                }
            }
        }

    }

    private int calculateContentRectInternalMargin() {
        int contentAreaMargin = 0;
        LineChartData data = this.dataProvider.getLineChartData();
        Iterator<Line> var3 = data.getLines().iterator();

        while(var3.hasNext()) {
            Line line = (Line)var3.next();
            if (this.checkIfShouldDrawPoints(line)) {
                int margin = line.getPointRadius() + 4;
                if (margin > contentAreaMargin) {
                    contentAreaMargin = margin;
                }
            }
        }

        return ChartUtils.dp2px(this.density, contentAreaMargin);
    }

    private void drawPath(Canvas canvas, Line line) {
        this.prepareLinePaint(line);
        int valueIndex = 0;

        for(Iterator<PointValue> var4 = line.getValues().iterator(); var4.hasNext(); ++valueIndex) {
            PointValue pointValue = (PointValue)var4.next();
            float rawX = this.computator.computeRawX(pointValue.getX());
            float rawY = this.computator.computeRawY(pointValue.getY());
            if (valueIndex == 0) {
                this.path.moveTo(rawX, rawY);
            } else {
                this.path.lineTo(rawX, rawY);
            }
        }

        canvas.drawPath(this.path, this.linePaint);
        if (line.isFilled()) {
            this.drawArea(canvas, line);
        }

        this.path.reset();
    }

    private void drawSquarePath(Canvas canvas, Line line) {
        this.prepareLinePaint(line);
        int valueIndex = 0;
        float previousRawY = 0.0F;

        for(Iterator<PointValue> var5 = line.getValues().iterator(); var5.hasNext(); ++valueIndex) {
            PointValue pointValue = (PointValue)var5.next();
            float rawX = this.computator.computeRawX(pointValue.getX());
            float rawY = this.computator.computeRawY(pointValue.getY());
            if (valueIndex == 0) {
                this.path.moveTo(rawX, rawY);
            } else {
                this.path.lineTo(rawX, previousRawY);
                this.path.lineTo(rawX, rawY);
            }

            previousRawY = rawY;
        }

        canvas.drawPath(this.path, this.linePaint);
        if (line.isFilled()) {
            this.drawArea(canvas, line);
        }

        this.path.reset();
    }

    private void drawSmoothPath(Canvas canvas, Line line) {
        this.prepareLinePaint(line);
        int lineSize = line.getValues().size();
        float prePreviousPointX = Float.NaN;
        float prePreviousPointY = Float.NaN;
        float previousPointX = Float.NaN;
        float previousPointY = Float.NaN;
        float currentPointX = Float.NaN;
        float currentPointY = Float.NaN;
        float nextPointX;
        float nextPointY;

        for(int valueIndex = 0; valueIndex < lineSize; ++valueIndex) {
            PointValue linePoint;
            if (Float.isNaN(currentPointX)) {
                linePoint = (PointValue)line.getValues().get(valueIndex);
                currentPointX = this.computator.computeRawX(linePoint.getX());
                currentPointY = this.computator.computeRawY(linePoint.getY());
            }

            if (Float.isNaN(previousPointX)) {
                if (valueIndex > 0) {
                    linePoint = (PointValue)line.getValues().get(valueIndex - 1);
                    previousPointX = this.computator.computeRawX(linePoint.getX());
                    previousPointY = this.computator.computeRawY(linePoint.getY());
                } else {
                    previousPointX = currentPointX;
                    previousPointY = currentPointY;
                }
            }

            if (Float.isNaN(prePreviousPointX)) {
                if (valueIndex > 1) {
                    linePoint = (PointValue)line.getValues().get(valueIndex - 2);
                    prePreviousPointX = this.computator.computeRawX(linePoint.getX());
                    prePreviousPointY = this.computator.computeRawY(linePoint.getY());
                } else {
                    prePreviousPointX = previousPointX;
                    prePreviousPointY = previousPointY;
                }
            }

            if (valueIndex < lineSize - 1) {
                linePoint = (PointValue)line.getValues().get(valueIndex + 1);
                nextPointX = this.computator.computeRawX(linePoint.getX());
                nextPointY = this.computator.computeRawY(linePoint.getY());
            } else {
                nextPointX = currentPointX;
                nextPointY = currentPointY;
            }

            if (valueIndex == 0) {
                this.path.moveTo(currentPointX, currentPointY);
            } else {
                float firstDiffX = currentPointX - prePreviousPointX;
                float firstDiffY = currentPointY - prePreviousPointY;
                float secondDiffX = nextPointX - previousPointX;
                float secondDiffY = nextPointY - previousPointY;
                float firstControlPointX = previousPointX + 0.16F * firstDiffX;
                float firstControlPointY = previousPointY + 0.16F * firstDiffY;
                float secondControlPointX = currentPointX - 0.16F * secondDiffX;
                float secondControlPointY = currentPointY - 0.16F * secondDiffY;
                this.path.cubicTo(firstControlPointX, firstControlPointY, secondControlPointX, secondControlPointY, currentPointX, currentPointY);
            }

            prePreviousPointX = previousPointX;
            prePreviousPointY = previousPointY;
            previousPointX = currentPointX;
            previousPointY = currentPointY;
            currentPointX = nextPointX;
            currentPointY = nextPointY;
        }

        canvas.drawPath(this.path, this.linePaint);
        if (line.isFilled()) {
            this.drawArea(canvas, line);
        }

        this.path.reset();
    }

    private void prepareLinePaint(Line line) {
        this.linePaint.setStrokeWidth((float)ChartUtils.dp2px(this.density, line.getStrokeWidth()));
        this.linePaint.setColor(line.getColor());
        this.linePaint.setPathEffect(line.getPathEffect());
    }

    private void drawPoints(Canvas canvas, Line line, int lineIndex, int mode) {
        this.pointPaint.setColor(line.getPointColor());
        int valueIndex = 0;

        for(Iterator<PointValue> var6 = line.getValues().iterator(); var6.hasNext(); ++valueIndex) {
            PointValue pointValue = (PointValue)var6.next();
            int pointRadius = ChartUtils.dp2px(this.density, line.getPointRadius());
            float rawX = this.computator.computeRawX(pointValue.getX());
            float rawY = this.computator.computeRawY(pointValue.getY());
            if (this.computator.isWithinContentRect(rawX, rawY, (float)this.checkPrecision)) {
                if (0 == mode) {
                    this.drawPoint(canvas, line, rawX, rawY, (float)pointRadius);
                    if (line.hasLabels()) {
                        this.drawLabel(canvas, line, pointValue, rawX, rawY, (float)(pointRadius + this.labelOffset));
                    }
                } else {
                    if (1 != mode) {
                        throw new IllegalStateException("Cannot process points in mode: " + mode);
                    }

                    this.highlightPoint(canvas, line, pointValue, rawX, rawY, lineIndex, valueIndex);
                }
            }
        }

    }

    private void drawPoint(Canvas canvas, Line line, float rawX, float rawY, float pointRadius) {
        if (ValueShape.SQUARE.equals(line.getShape())) {
            canvas.drawRect(rawX - pointRadius, rawY - pointRadius, rawX + pointRadius, rawY + pointRadius, this.pointPaint);
        } else if (ValueShape.CIRCLE.equals(line.getShape())) {
            canvas.drawCircle(rawX, rawY, pointRadius, this.pointPaint);
        } else {
            if (!ValueShape.DIAMOND.equals(line.getShape())) {
                throw new IllegalArgumentException("Invalid point shape: " + line.getShape());
            }

            canvas.save();
            canvas.rotate(45.0F, rawX, rawY);
            canvas.drawRect(rawX - pointRadius, rawY - pointRadius, rawX + pointRadius, rawY + pointRadius, this.pointPaint);
            canvas.restore();
        }

    }

    private void highlightPoints(Canvas canvas) {
        int lineIndex = this.selectedValue.getFirstIndex();
        Line line = (Line)this.dataProvider.getLineChartData().getLines().get(lineIndex);
        this.drawPoints(canvas, line, lineIndex, 1);
    }

    private void highlightPoint(Canvas canvas, Line line, PointValue pointValue, float rawX, float rawY, int lineIndex, int valueIndex) {
        if (this.selectedValue.getFirstIndex() == lineIndex && this.selectedValue.getSecondIndex() == valueIndex) {
            int pointRadius = ChartUtils.dp2px(this.density, line.getPointRadius());
            this.pointPaint.setColor(line.getDarkenColor());
            this.drawPoint(canvas, line,  rawX, rawY, (float)(pointRadius + this.touchToleranceMargin));
            if (line.hasLabels() || line.hasLabelsOnlyForSelected()) {
                this.drawLabel(canvas, line, pointValue, rawX, rawY, (float)(pointRadius + this.labelOffset));
            }
        }

    }

    private void drawLabel(Canvas canvas, Line line, PointValue pointValue, float rawX, float rawY, float offset) {
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        int numChars = line.getFormatter().formatChartValue(this.labelBuffer, pointValue);
        if (numChars != 0) {
            float labelWidth = this.labelPaint.measureText(this.labelBuffer, this.labelBuffer.length - numChars, numChars);
            int labelHeight = Math.abs(this.fontMetrics.ascent);
            float left = rawX - labelWidth / 2.0F - (float)this.labelMargin;
            float right = rawX + labelWidth / 2.0F + (float)this.labelMargin;
            float top;
            float bottom;
            if (pointValue.getY() >= this.baseValue) {
                top = rawY - offset - (float)labelHeight - (float)(this.labelMargin * 2);
                bottom = rawY - offset;
            } else {
                top = rawY + offset;
                bottom = rawY + offset + (float)labelHeight + (float)(this.labelMargin * 2);
            }

            if (top < (float)contentRect.top) {
                top = rawY + offset;
                bottom = rawY + offset + (float)labelHeight + (float)(this.labelMargin * 2);
            }

            if (bottom > (float)contentRect.bottom) {
                top = rawY - offset - (float)labelHeight - (float)(this.labelMargin * 2);
                bottom = rawY - offset;
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
            this.drawLabelTextAndBackground(canvas, this.labelBuffer, this.labelBuffer.length - numChars, numChars, line.getDarkenColor());
        }
    }

    private void drawArea(Canvas canvas, Line line) {
        int lineSize = line.getValues().size();
        if (lineSize >= 2) {
            Rect contentRect = this.computator.getContentRectMinusAllMargins();
            float baseRawValue = Math.min((float)contentRect.bottom, Math.max(this.computator.computeRawY(this.baseValue), (float)contentRect.top));
            float left = Math.max(this.computator.computeRawX(((PointValue)line.getValues().get(0)).getX()), (float)contentRect.left);
            float right = Math.min(this.computator.computeRawX(((PointValue)line.getValues().get(lineSize - 1)).getX()), (float)contentRect.right);
            this.path.lineTo(right, baseRawValue);
            this.path.lineTo(left, baseRawValue);
            this.path.close();
            this.linePaint.setStyle(Paint.Style.FILL);
            this.linePaint.setAlpha(line.getAreaTransparency());
            canvas.drawPath(this.path, this.linePaint);
            this.linePaint.setStyle(Paint.Style.STROKE);
        }
    }

    private boolean isInArea(float x, float y, float touchX, float touchY, float radius) {
        float diffX = touchX - x;
        float diffY = touchY - y;
        return Math.pow((double)diffX, 2.0) + Math.pow((double)diffY, 2.0) <= 2.0 * Math.pow((double)radius, 2.0);
    }
}

