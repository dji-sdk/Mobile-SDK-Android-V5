package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;

import java.util.Iterator;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.model.Axis;
import dji.v5.ux.core.base.charts.model.AxisValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.util.AxisAutoValues;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.util.FloatUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class AxesRenderer {
    private static final char[] labelWidthChars = new char[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'};
    private Chart chart;
    private ChartComputator computator;
    private int axisMargin;
    private float density;
    private float scaledDensity;
    private Paint[] labelPaintTab = new Paint[]{new Paint(), new Paint(), new Paint(), new Paint()};
    private Paint[] namePaintTab = new Paint[]{new Paint(), new Paint(), new Paint(), new Paint()};
    private Paint[] linePaintTab = new Paint[]{new Paint(), new Paint(), new Paint(), new Paint()};
    private float[] nameBaselineTab = new float[4];
    private float[] labelBaselineTab = new float[4];
    private float[] separationLineTab = new float[4];
    private int[] labelWidthTab = new int[4];
    private int[] labelTextAscentTab = new int[4];
    private int[] labelTextDescentTab = new int[4];
    private int[] labelDimensionForMarginsTab = new int[4];
    private int[] labelDimensionForStepsTab = new int[4];
    private int[] tiltedLabelXTranslation = new int[4];
    private int[] tiltedLabelYTranslation = new int[4];
    private Paint.FontMetricsInt[] fontMetricsTab = new Paint.FontMetricsInt[]{new Paint.FontMetricsInt(), new Paint.FontMetricsInt(), new Paint.FontMetricsInt(), new Paint.FontMetricsInt()};
    private char[] labelBuffer = new char[64];
    private int[] valuesToDrawNumTab = new int[4];
    private float[][] rawValuesTab = new float[4][0];
    private float[][] autoValuesToDrawTab = new float[4][0];
    private AxisValue[][] valuesToDrawTab = new AxisValue[4][0];
    private float[][] linesDrawBufferTab = new float[4][0];
    private AxisAutoValues[] autoValuesBufferTab = new AxisAutoValues[]{new AxisAutoValues(), new AxisAutoValues(), new AxisAutoValues(), new AxisAutoValues()};

    public AxesRenderer(Context context, Chart chart) {
        this.chart = chart;
        this.computator = chart.getChartComputator();
        this.density = context.getResources().getDisplayMetrics().density;
        this.scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        this.axisMargin = ChartUtils.dp2px(this.density, 2);

        for(int position = 0; position < 4; ++position) {
            this.labelPaintTab[position].setStyle(Paint.Style.FILL);
            this.labelPaintTab[position].setAntiAlias(true);
            this.namePaintTab[position].setStyle(Paint.Style.FILL);
            this.namePaintTab[position].setAntiAlias(true);
            this.linePaintTab[position].setStyle(Paint.Style.STROKE);
            this.linePaintTab[position].setAntiAlias(true);
        }

    }

    public void onChartSizeChanged() {
        this.onChartDataOrSizeChanged();
    }

    public void onChartDataChanged() {
        this.onChartDataOrSizeChanged();
    }

    private void onChartDataOrSizeChanged() {
        this.initAxis(this.chart.getChartData().getAxisXTop(), 0);
        this.initAxis(this.chart.getChartData().getAxisXBottom(), 3);
        this.initAxis(this.chart.getChartData().getAxisYLeft(), 1);
        this.initAxis(this.chart.getChartData().getAxisYRight(), 2);
    }

    public void resetRenderer() {
        this.computator = this.chart.getChartComputator();
    }

    private void initAxis(Axis axis, int position) {
        if (null != axis) {
            this.initAxisAttributes(axis, position);
            this.initAxisMargin(axis, position);
            this.initAxisMeasurements(axis, position);
        }
    }

    private void initAxisAttributes(Axis axis, int position) {
        this.initAxisPaints(axis, position);
        this.initAxisTextAlignment(axis, position);
        if (axis.hasTiltedLabels()) {
            this.initAxisDimensionForTiltedLabels(position);
            this.intiTiltedLabelsTranslation(axis, position);
        } else {
            this.initAxisDimension(position);
        }

    }

    private void initAxisPaints(Axis axis, int position) {
        Typeface typeface = axis.getTypeface();
        if (null != typeface) {
            this.labelPaintTab[position].setTypeface(typeface);
            this.namePaintTab[position].setTypeface(typeface);
        }

        this.labelPaintTab[position].setColor(axis.getTextColor());
        this.labelPaintTab[position].setTextSize((float)ChartUtils.sp2px(this.scaledDensity, axis.getTextSize()));
        this.labelPaintTab[position].getFontMetricsInt(this.fontMetricsTab[position]);
        this.namePaintTab[position].setColor(axis.getTextColor());
        this.namePaintTab[position].setTextSize((float)ChartUtils.sp2px(this.scaledDensity, axis.getTextSize()));
        this.linePaintTab[position].setColor(axis.getLineColor());
        this.labelTextAscentTab[position] = Math.abs(this.fontMetricsTab[position].ascent);
        this.labelTextDescentTab[position] = Math.abs(this.fontMetricsTab[position].descent);
        this.labelWidthTab[position] = (int)this.labelPaintTab[position].measureText(labelWidthChars, 0, axis.getMaxLabelChars());
    }

    private void initAxisTextAlignment(Axis axis, int position) {
        this.namePaintTab[position].setTextAlign(Paint.Align.CENTER);
        if (0 != position && 3 != position) {
            if (1 == position) {
                if (axis.isInside()) {
                    this.labelPaintTab[position].setTextAlign(Paint.Align.LEFT);
                } else {
                    this.labelPaintTab[position].setTextAlign(Paint.Align.RIGHT);
                }
            } else if (2 == position) {
                if (axis.isInside()) {
                    this.labelPaintTab[position].setTextAlign(Paint.Align.RIGHT);
                } else {
                    this.labelPaintTab[position].setTextAlign(Paint.Align.LEFT);
                }
            }
        } else {
            this.labelPaintTab[position].setTextAlign(Paint.Align.CENTER);
        }

    }

    private void initAxisDimensionForTiltedLabels(int position) {
        int pythagoreanFromLabelWidth = (int)Math.sqrt(Math.pow((double)this.labelWidthTab[position], 2.0) / 2.0);
        int pythagoreanFromAscent = (int)Math.sqrt(Math.pow((double)this.labelTextAscentTab[position], 2.0) / 2.0);
        this.labelDimensionForMarginsTab[position] = pythagoreanFromAscent + pythagoreanFromLabelWidth;
        this.labelDimensionForStepsTab[position] = Math.round((float)this.labelDimensionForMarginsTab[position] * 0.75F);
    }

    private void initAxisDimension(int position) {
        if (1 != position && 2 != position) {
            if (0 == position || 3 == position) {
                this.labelDimensionForMarginsTab[position] = this.labelTextAscentTab[position] + this.labelTextDescentTab[position];
                this.labelDimensionForStepsTab[position] = this.labelWidthTab[position];
            }
        } else {
            this.labelDimensionForMarginsTab[position] = this.labelWidthTab[position];
            this.labelDimensionForStepsTab[position] = this.labelTextAscentTab[position];
        }

    }

    private void intiTiltedLabelsTranslation(Axis axis, int position) {
        int pythagoreanFromLabelWidth = (int)Math.sqrt(Math.pow((double)this.labelWidthTab[position], 2.0) / 2.0);
        int pythagoreanFromAscent = (int)Math.sqrt(Math.pow((double)this.labelTextAscentTab[position], 2.0) / 2.0);
        int dx = 0;
        int dy = 0;
        if (axis.isInside()) {
            if (1 == position) {
                dx = pythagoreanFromAscent;
            } else if (2 == position) {
                dy = -pythagoreanFromLabelWidth / 2;
            } else if (0 == position) {
                dy = pythagoreanFromAscent + pythagoreanFromLabelWidth / 2 - this.labelTextAscentTab[position];
            } else if (3 == position) {
                dy = -pythagoreanFromLabelWidth / 2;
            }
        } else if (1 == position) {
            dy = -pythagoreanFromLabelWidth / 2;
        } else if (2 == position) {
            dx = pythagoreanFromAscent;
        } else if (0 == position) {
            dy = -pythagoreanFromLabelWidth / 2;
        } else if (3 == position) {
            dy = pythagoreanFromAscent + pythagoreanFromLabelWidth / 2 - this.labelTextAscentTab[position];
        }

        this.tiltedLabelXTranslation[position] = dx;
        this.tiltedLabelYTranslation[position] = dy;
    }

    private void initAxisMargin(Axis axis, int position) {
        int margin = 0;
        if (!axis.isInside() && (axis.isAutoGenerated() || !axis.getValues().isEmpty())) {
            margin += this.axisMargin + this.labelDimensionForMarginsTab[position];
        }

        margin += this.getAxisNameMargin(axis, position);
        this.insetContentRectWithAxesMargins(margin, position);
    }

    private int getAxisNameMargin(Axis axis, int position) {
        int margin = 0;
        if (!TextUtils.isEmpty(axis.getName())) {
            margin += this.labelTextAscentTab[position];
            margin += this.labelTextDescentTab[position];
            margin += this.axisMargin;
        }

        return margin;
    }

    private void insetContentRectWithAxesMargins(int axisMargin, int position) {
        if (1 == position) {
            this.chart.getChartComputator().insetContentRect(axisMargin, 0, 0, 0);
        } else if (2 == position) {
            this.chart.getChartComputator().insetContentRect(0, 0, axisMargin, 0);
        } else if (0 == position) {
            this.chart.getChartComputator().insetContentRect(0, axisMargin, 0, 0);
        } else if (3 == position) {
            this.chart.getChartComputator().insetContentRect(0, 0, 0, axisMargin);
        }

    }

    private void initAxisMeasurements(Axis axis, int position) {
        if (1 == position) {
            if (axis.isInside()) {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAllMargins().left + this.axisMargin);
                this.nameBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().left - this.axisMargin - this.labelTextDescentTab[position]);
            } else {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().left - this.axisMargin);
                this.nameBaselineTab[position] = this.labelBaselineTab[position] - (float)this.axisMargin - (float)this.labelTextDescentTab[position] - (float)this.labelDimensionForMarginsTab[position];
            }

            this.separationLineTab[position] = (float)this.computator.getContentRectMinusAllMargins().left;
        } else if (2 == position) {
            if (axis.isInside()) {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAllMargins().right - this.axisMargin);
                this.nameBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().right + this.axisMargin + this.labelTextAscentTab[position]);
            } else {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().right + this.axisMargin);
                this.nameBaselineTab[position] = this.labelBaselineTab[position] + (float)this.axisMargin + (float)this.labelTextAscentTab[position] + (float)this.labelDimensionForMarginsTab[position];
            }

            this.separationLineTab[position] = (float)this.computator.getContentRectMinusAllMargins().right;
        } else if (3 == position) {
            if (axis.isInside()) {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAllMargins().bottom - this.axisMargin - this.labelTextDescentTab[position]);
                this.nameBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().bottom + this.axisMargin + this.labelTextAscentTab[position]);
            } else {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().bottom + this.axisMargin + this.labelTextAscentTab[position]);
                this.nameBaselineTab[position] = this.labelBaselineTab[position] + (float)this.axisMargin + (float)this.labelDimensionForMarginsTab[position];
            }

            this.separationLineTab[position] = (float)this.computator.getContentRectMinusAllMargins().bottom;
        } else {
            if (0 != position) {
                throw new IllegalArgumentException("Invalid axis position: " + position);
            }

            if (axis.isInside()) {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAllMargins().top + this.axisMargin + this.labelTextAscentTab[position]);
                this.nameBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().top - this.axisMargin - this.labelTextDescentTab[position]);
            } else {
                this.labelBaselineTab[position] = (float)(this.computator.getContentRectMinusAxesMargins().top - this.axisMargin - this.labelTextDescentTab[position]);
                this.nameBaselineTab[position] = this.labelBaselineTab[position] - (float)this.axisMargin - (float)this.labelDimensionForMarginsTab[position];
            }

            this.separationLineTab[position] = (float)this.computator.getContentRectMinusAllMargins().top;
        }

    }

    public void drawInBackground(Canvas canvas) {
        Axis axis = this.chart.getChartData().getAxisYLeft();
        if (null != axis) {
            this.prepareAxisToDraw(axis, 1);
            this.drawAxisLines(canvas, axis, 1);
        }

        axis = this.chart.getChartData().getAxisYRight();
        if (null != axis) {
            this.prepareAxisToDraw(axis, 2);
            this.drawAxisLines(canvas, axis, 2);
        }

        axis = this.chart.getChartData().getAxisXBottom();
        if (null != axis) {
            this.prepareAxisToDraw(axis, 3);
            this.drawAxisLines(canvas, axis, 3);
        }

        axis = this.chart.getChartData().getAxisXTop();
        if (null != axis) {
            this.prepareAxisToDraw(axis, 0);
            this.drawAxisLines(canvas, axis, 0);
        }

    }

    private void prepareAxisToDraw(Axis axis, int position) {
        if (axis.isAutoGenerated()) {
            this.prepareAutoGeneratedAxis(axis, position);
        } else {
            this.prepareCustomAxis(axis, position);
        }

    }

    public void drawInForeground(Canvas canvas) {
        Axis axis = this.chart.getChartData().getAxisYLeft();
        if (null != axis) {
            this.drawAxisLabelsAndName(canvas, axis, 1);
        }

        axis = this.chart.getChartData().getAxisYRight();
        if (null != axis) {
            this.drawAxisLabelsAndName(canvas, axis, 2);
        }

        axis = this.chart.getChartData().getAxisXBottom();
        if (null != axis) {
            this.drawAxisLabelsAndName(canvas, axis, 3);
        }

        axis = this.chart.getChartData().getAxisXTop();
        if (null != axis) {
            this.drawAxisLabelsAndName(canvas, axis, 0);
        }

    }

    private void prepareCustomAxis(Axis axis, int position) {
        Viewport maxViewport = this.computator.getMaximumViewport();
        Viewport visibleViewport = this.computator.getVisibleViewport();
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        boolean isAxisVertical = this.isAxisVertical(position);
        float scale = 1.0F;
        float viewportMin;
        float viewportMax;
        if (isAxisVertical) {
            if (maxViewport.height() > 0.0F && visibleViewport.height() > 0.0F) {
                scale = (float)contentRect.height() * (maxViewport.height() / visibleViewport.height());
            }

            viewportMin = visibleViewport.bottom;
            viewportMax = visibleViewport.top;
        } else {
            if (maxViewport.width() > 0.0F && visibleViewport.width() > 0.0F) {
                scale = (float)contentRect.width() * (maxViewport.width() / visibleViewport.width());
            }

            viewportMin = visibleViewport.left;
            viewportMax = visibleViewport.right;
        }

        if (scale == 0.0F) {
            scale = 1.0F;
        }

        int module = (int)Math.max(1.0, Math.ceil((double)(axis.getValues().size() * this.labelDimensionForStepsTab[position]) * 1.5 / (double)scale));
        if (axis.hasLines() && this.linesDrawBufferTab[position].length < axis.getValues().size() * 4) {
            this.linesDrawBufferTab[position] = new float[axis.getValues().size() * 4];
        }

        if (this.rawValuesTab[position].length < axis.getValues().size()) {
            this.rawValuesTab[position] = new float[axis.getValues().size()];
        }

        if (this.valuesToDrawTab[position].length < axis.getValues().size()) {
            this.valuesToDrawTab[position] = new AxisValue[axis.getValues().size()];
        }

        int valueIndex = 0;
        int valueToDrawIndex = 0;
        Iterator<AxisValue> var14 = axis.getValues().iterator();

        while(var14.hasNext()) {
            AxisValue axisValue = (AxisValue)var14.next();
            float value = axisValue.getValue();
            if (value >= viewportMin && value <= viewportMax) {
                if (0 == valueIndex % module) {
                    float rawValue;
                    if (isAxisVertical) {
                        rawValue = this.computator.computeRawY(value);
                    } else {
                        rawValue = this.computator.computeRawX(value);
                    }

                    if (this.checkRawValue(contentRect, rawValue, axis.isInside(), position, isAxisVertical)) {
                        this.rawValuesTab[position][valueToDrawIndex] = rawValue;
                        this.valuesToDrawTab[position][valueToDrawIndex] = axisValue;
                        ++valueToDrawIndex;
                    }
                }

                ++valueIndex;
            }
        }

        this.valuesToDrawNumTab[position] = valueToDrawIndex;
    }

    private void prepareAutoGeneratedAxis(Axis axis, int position) {
        Viewport visibleViewport = this.computator.getVisibleViewport();
        Rect contentRect = this.computator.getContentRectMinusAllMargins();
        boolean isAxisVertical = this.isAxisVertical(position);
        float start;
        float stop;
        int contentRectDimension;
        if (isAxisVertical) {
            start = visibleViewport.bottom;
            stop = visibleViewport.top;
            contentRectDimension = contentRect.height();
        } else {
            start = visibleViewport.left;
            stop = visibleViewport.right;
            contentRectDimension = contentRect.width();
        }

        FloatUtils.computeAutoGeneratedAxisValues(start, stop, Math.abs(contentRectDimension) / this.labelDimensionForStepsTab[position] / 2, this.autoValuesBufferTab[position]);
        if (axis.hasLines() && this.linesDrawBufferTab[position].length < this.autoValuesBufferTab[position].valuesNumber * 4) {
            this.linesDrawBufferTab[position] = new float[this.autoValuesBufferTab[position].valuesNumber * 4];
        }

        if (this.rawValuesTab[position].length < this.autoValuesBufferTab[position].valuesNumber) {
            this.rawValuesTab[position] = new float[this.autoValuesBufferTab[position].valuesNumber];
        }

        if (this.autoValuesToDrawTab[position].length < this.autoValuesBufferTab[position].valuesNumber) {
            this.autoValuesToDrawTab[position] = new float[this.autoValuesBufferTab[position].valuesNumber];
        }

        int valueToDrawIndex = 0;

        for(int i = 0; i < this.autoValuesBufferTab[position].valuesNumber; ++i) {
            float rawValue;
            if (isAxisVertical) {
                rawValue = this.computator.computeRawY(this.autoValuesBufferTab[position].values[i]);
            } else {
                rawValue = this.computator.computeRawX(this.autoValuesBufferTab[position].values[i]);
            }

            if (this.checkRawValue(contentRect, rawValue, axis.isInside(), position, isAxisVertical)) {
                this.rawValuesTab[position][valueToDrawIndex] = rawValue;
                this.autoValuesToDrawTab[position][valueToDrawIndex] = this.autoValuesBufferTab[position].values[i];
                ++valueToDrawIndex;
            }
        }

        this.valuesToDrawNumTab[position] = valueToDrawIndex;
    }

    private boolean checkRawValue(Rect rect, float rawValue, boolean axisInside, int position, boolean isVertical) {
        if (axisInside) {
            float margin;
            if (isVertical) {
                margin = (float)(this.labelTextAscentTab[3] + this.axisMargin);
                float marginTop = (float)(this.labelTextAscentTab[0] + this.axisMargin);
                return rawValue <= (float)rect.bottom - margin && rawValue >= (float)rect.top + marginTop;
            } else {
                margin = (float)((double)this.labelWidthTab[position] / 2);
                return rawValue >= (float)rect.left + margin && rawValue <= (float)rect.right - margin;
            }
        } else {
            return true;
        }
    }

    private void drawAxisLines(Canvas canvas, Axis axis, int position) {
        Rect contentRectMargins = this.computator.getContentRectMinusAxesMargins();
        float separationY2 = 0.0F;
        float separationX2 = 0.0F;
        float separationY1 = 0.0F;
        float separationX1 = 0.0F;
        float lineY2 = 0.0F;
        float lineX2 = 0.0F;
        float lineY1 = 0.0F;
        float lineX1 = 0.0F;
        boolean isAxisVertical = this.isAxisVertical(position);
        if (1 != position && 2 != position) {
            if (0 == position || 3 == position) {
                separationX1 = (float)contentRectMargins.left;
                separationX2 = (float)contentRectMargins.right;
                separationY1 = separationY2 = this.separationLineTab[position];
                lineY1 = (float)contentRectMargins.top;
                lineY2 = (float)contentRectMargins.bottom;
            }
        } else {
            separationX1 = separationX2 = this.separationLineTab[position];
            separationY1 = (float)contentRectMargins.bottom;
            separationY2 = (float)contentRectMargins.top;
            lineX1 = (float)contentRectMargins.left;
            lineX2 = (float)contentRectMargins.right;
        }

        if (axis.hasSeparationLine()) {
            canvas.drawLine(separationX1, separationY1, separationX2, separationY2, this.labelPaintTab[position]);
        }

        if (axis.hasLines()) {
            int valueToDrawIndex;
            for(valueToDrawIndex = 0; valueToDrawIndex < this.valuesToDrawNumTab[position]; ++valueToDrawIndex) {
                if (isAxisVertical) {
                    lineY1 = lineY2 = this.rawValuesTab[position][valueToDrawIndex];
                } else {
                    lineX1 = lineX2 = this.rawValuesTab[position][valueToDrawIndex];
                }

                this.linesDrawBufferTab[position][valueToDrawIndex * 4 + 0] = lineX1;
                this.linesDrawBufferTab[position][valueToDrawIndex * 4 + 1] = lineY1;
                this.linesDrawBufferTab[position][valueToDrawIndex * 4 + 2] = lineX2;
                this.linesDrawBufferTab[position][valueToDrawIndex * 4 + 3] = lineY2;
            }

            canvas.drawLines(this.linesDrawBufferTab[position], 0, valueToDrawIndex * 4, this.linePaintTab[position]);
        }

    }

    private void drawAxisLabelsAndName(Canvas canvas, Axis axis, int position) {
        float labelY = 0.0F;
        float labelX = 0.0F;
        boolean isAxisVertical = this.isAxisVertical(position);
        if (1 != position && 2 != position) {
            if (0 == position || 3 == position) {
                labelY = this.labelBaselineTab[position];
            }
        } else {
            labelX = this.labelBaselineTab[position];
        }

        for(int valueToDrawIndex = 0; valueToDrawIndex < this.valuesToDrawNumTab[position]; ++valueToDrawIndex) {
            int charsNumber = 0;
            if (axis.isAutoGenerated()) {
                float value = this.autoValuesToDrawTab[position][valueToDrawIndex];
                charsNumber = axis.getFormatter().formatValueForAutoGeneratedAxis(this.labelBuffer, value, this.autoValuesBufferTab[position].decimals);
            } else {
                AxisValue axisValue = this.valuesToDrawTab[position][valueToDrawIndex];
                charsNumber = axis.getFormatter().formatValueForManualAxis(this.labelBuffer, axisValue);
            }

            if (isAxisVertical) {
                labelY = this.rawValuesTab[position][valueToDrawIndex];
            } else {
                labelX = this.rawValuesTab[position][valueToDrawIndex];
            }

            if (axis.hasTiltedLabels()) {
                canvas.save();
                canvas.translate((float)this.tiltedLabelXTranslation[position], (float)this.tiltedLabelYTranslation[position]);
                canvas.rotate(-45.0F, labelX, labelY);
                canvas.drawText(this.labelBuffer, this.labelBuffer.length - charsNumber, charsNumber, labelX, labelY, this.labelPaintTab[position]);
                canvas.restore();
            } else {
                canvas.drawText(this.labelBuffer, this.labelBuffer.length - charsNumber, charsNumber, labelX, labelY, this.labelPaintTab[position]);
            }
        }

        Rect contentRectMargins = this.computator.getContentRectMinusAxesMargins();
        if (!TextUtils.isEmpty(axis.getName())) {
            if (isAxisVertical) {
                canvas.save();
                canvas.rotate(-90.0F, (float)contentRectMargins.centerY(), (float)contentRectMargins.centerY());
                canvas.drawText(axis.getName(), (float)contentRectMargins.centerY(), this.nameBaselineTab[position], this.namePaintTab[position]);
                canvas.restore();
            } else {
                canvas.drawText(axis.getName(), (float)contentRectMargins.centerX(), this.nameBaselineTab[position], this.namePaintTab[position]);
            }
        }

    }

    private boolean isAxisVertical(int position) {
        if (1 != position && 2 != position) {
            if (0 != position && 3 != position) {
                throw new IllegalArgumentException("Invalid axis position " + position);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}

