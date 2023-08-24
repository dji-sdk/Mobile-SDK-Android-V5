package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.Iterator;

import dji.v5.ux.core.base.charts.model.Column;
import dji.v5.ux.core.base.charts.model.ColumnChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.SubcolumnValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.provider.ColumnChartDataProvider;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class ColumnChartRenderer extends AbstractChartRenderer {
    private ColumnChartDataProvider dataProvider;
    private int touchAdditionalWidth;
    private int subcolumnSpacing;
    private Paint columnPaint = new Paint();
    private RectF drawRect = new RectF();
    private PointF touchedPoint = new PointF();
    private float fillRatio;
    private float baseValue;
    private Viewport tempMaximumViewport = new Viewport();

    public ColumnChartRenderer(Context context, Chart chart, ColumnChartDataProvider dataProvider) {
        super(context, chart);
        this.dataProvider = dataProvider;
        this.subcolumnSpacing = ChartUtils.dp2px(this.density, 1);
        this.touchAdditionalWidth = ChartUtils.dp2px(this.density, 4);
        this.columnPaint.setAntiAlias(true);
        this.columnPaint.setStyle(Paint.Style.FILL);
        this.columnPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    public void onChartSizeChanged() {
        //do nothing
    }

    @Override
    public void onChartDataChanged() {
        super.onChartDataChanged();
        ColumnChartData data = this.dataProvider.getColumnChartData();
        this.fillRatio = data.getFillRatio();
        this.baseValue = data.getBaseValue();
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
        ColumnChartData data = this.dataProvider.getColumnChartData();
        if (data.isStacked()) {
            this.drawColumnForStacked(canvas);
            if (this.isTouched()) {
                this.highlightColumnForStacked(canvas);
            }
        } else {
            this.drawColumnsForSubcolumns(canvas);
            if (this.isTouched()) {
                this.highlightColumnsForSubcolumns(canvas);
            }
        }

    }

    public void drawUnclipped(Canvas canvas) {
        //do nothing
    }

    public boolean checkTouch(float touchX, float touchY) {
        this.selectedValue.clear();
        ColumnChartData data = this.dataProvider.getColumnChartData();
        if (data.isStacked()) {
            this.checkTouchForStacked(touchX, touchY);
        } else {
            this.checkTouchForSubcolumns(touchX, touchY);
        }

        return this.isTouched();
    }

    private void calculateMaxViewport() {
        ColumnChartData data = this.dataProvider.getColumnChartData();
        this.tempMaximumViewport.set(-0.5F, this.baseValue, (float)data.getColumns().size() - 0.5F, this.baseValue);
        if (data.isStacked()) {
            this.calculateMaxViewportForStacked(data);
        } else {
            this.calculateMaxViewportForSubcolumns(data);
        }

    }

    private void calculateMaxViewportForSubcolumns(ColumnChartData data) {
        Iterator<Column> var2 = data.getColumns().iterator();

        while(var2.hasNext()) {
            Column column = (Column)var2.next();
            Iterator<SubcolumnValue> var4 = column.getValues().iterator();

            while(var4.hasNext()) {
                SubcolumnValue columnValue = (SubcolumnValue)var4.next();
                if (columnValue.getValue() >= this.baseValue && columnValue.getValue() > this.tempMaximumViewport.top) {
                    this.tempMaximumViewport.top = columnValue.getValue();
                }

                if (columnValue.getValue() < this.baseValue && columnValue.getValue() < this.tempMaximumViewport.bottom) {
                    this.tempMaximumViewport.bottom = columnValue.getValue();
                }
            }
        }

    }

    private void calculateMaxViewportForStacked(ColumnChartData data) {
        Iterator<Column> var2 = data.getColumns().iterator();

        while(var2.hasNext()) {
            Column column = (Column)var2.next();
            float sumPositive = this.baseValue;
            float sumNegative = this.baseValue;
            Iterator<SubcolumnValue> var6 = column.getValues().iterator();

            while(var6.hasNext()) {
                SubcolumnValue columnValue = (SubcolumnValue)var6.next();
                if (columnValue.getValue() >= this.baseValue) {
                    sumPositive += columnValue.getValue();
                } else {
                    sumNegative += columnValue.getValue();
                }
            }

            if (sumPositive > this.tempMaximumViewport.top) {
                this.tempMaximumViewport.top = sumPositive;
            }

            if (sumNegative < this.tempMaximumViewport.bottom) {
                this.tempMaximumViewport.bottom = sumNegative;
            }
        }

    }

    private void drawColumnsForSubcolumns(Canvas canvas) {
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        int columnIndex = 0;

        for(Iterator<Column> var5 = data.getColumns().iterator(); var5.hasNext(); ++columnIndex) {
            Column column = (Column)var5.next();
            this.processColumnForSubcolumns(canvas, column, columnWidth, columnIndex, 0);
        }

    }

    private void highlightColumnsForSubcolumns(Canvas canvas) {
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        Column column = (Column)data.getColumns().get(this.selectedValue.getFirstIndex());
        this.processColumnForSubcolumns(canvas, column, columnWidth, this.selectedValue.getFirstIndex(), 2);
    }

    private void checkTouchForSubcolumns(float touchX, float touchY) {
        this.touchedPoint.x = touchX;
        this.touchedPoint.y = touchY;
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        int columnIndex = 0;

        for(Iterator<Column> var6 = data.getColumns().iterator(); var6.hasNext(); ++columnIndex) {
            Column column = (Column)var6.next();
            this.processColumnForSubcolumns((Canvas)null, column, columnWidth, columnIndex, 1);
        }

    }

    private void processColumnForSubcolumns(Canvas canvas, Column column, float columnWidth, int columnIndex, int mode) {
        float subcolumnWidth = (columnWidth - (float)(this.subcolumnSpacing * (column.getValues().size() - 1))) / (float)column.getValues().size();
        if (subcolumnWidth < 1.0F) {
            subcolumnWidth = 1.0F;
        }

        float rawX = this.computator.computeRawX((float)columnIndex);
        float halfColumnWidth = columnWidth / 2.0F;
        float baseRawY = this.computator.computeRawY(this.baseValue);
        float subcolumnRawX = rawX - halfColumnWidth;
        int valueIndex = 0;

        for(Iterator<SubcolumnValue> var12 = column.getValues().iterator(); var12.hasNext(); ++valueIndex) {
            SubcolumnValue columnValue = (SubcolumnValue)var12.next();
            this.columnPaint.setColor(columnValue.getColor());
            if (subcolumnRawX > rawX + halfColumnWidth) {
                break;
            }

            float rawY = this.computator.computeRawY(columnValue.getValue());
            this.calculateRectToDraw(columnValue, subcolumnRawX, subcolumnRawX + subcolumnWidth, baseRawY, rawY);
            switch (mode) {
                case 0:
                    this.drawSubcolumn(canvas, column, columnValue, false);
                    break;
                case 1:
                    this.checkRectToDraw(columnIndex, valueIndex);
                    break;
                case 2:
                    this.highlightSubcolumn(canvas, column, columnValue, valueIndex, false);
                    break;
                default:
                    throw new IllegalStateException("Cannot process column in mode: " + mode);
            }

            subcolumnRawX += subcolumnWidth + (float)this.subcolumnSpacing;
        }

    }

    private void drawColumnForStacked(Canvas canvas) {
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        int columnIndex = 0;

        for(Iterator<Column> var5 = data.getColumns().iterator(); var5.hasNext(); ++columnIndex) {
            Column column = (Column)var5.next();
            this.processColumnForStacked(canvas, column, columnWidth, columnIndex, 0);
        }

    }

    private void highlightColumnForStacked(Canvas canvas) {
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        Column column = (Column)data.getColumns().get(this.selectedValue.getFirstIndex());
        this.processColumnForStacked(canvas, column, columnWidth, this.selectedValue.getFirstIndex(), 2);
    }

    private void checkTouchForStacked(float touchX, float touchY) {
        this.touchedPoint.x = touchX;
        this.touchedPoint.y = touchY;
        ColumnChartData data = this.dataProvider.getColumnChartData();
        float columnWidth = this.calculateColumnWidth();
        int columnIndex = 0;

        for(Iterator<Column> var6 = data.getColumns().iterator(); var6.hasNext(); ++columnIndex) {
            Column column = (Column)var6.next();
            this.processColumnForStacked((Canvas)null, column, columnWidth, columnIndex, 1);
        }

    }

    private void processColumnForStacked(Canvas canvas, Column column, float columnWidth, int columnIndex, int mode) {
        float rawX = this.computator.computeRawX(columnIndex);
        float halfColumnWidth = columnWidth / 2.0F;
        float mostPositiveValue = this.baseValue;
        float mostNegativeValue = this.baseValue;
        float subcolumnBaseValue;
        int valueIndex = 0;

        for(Iterator<SubcolumnValue> var12 = column.getValues().iterator(); var12.hasNext(); ++valueIndex) {
            SubcolumnValue columnValue = (SubcolumnValue)var12.next();
            this.columnPaint.setColor(columnValue.getColor());
            if (columnValue.getValue() >= this.baseValue) {
                subcolumnBaseValue = mostPositiveValue;
                mostPositiveValue += columnValue.getValue();
            } else {
                subcolumnBaseValue = mostNegativeValue;
                mostNegativeValue += columnValue.getValue();
            }

            float rawBaseY = this.computator.computeRawY(subcolumnBaseValue);
            float rawY = this.computator.computeRawY(subcolumnBaseValue + columnValue.getValue());
            this.calculateRectToDraw(columnValue, rawX - halfColumnWidth, rawX + halfColumnWidth, rawBaseY, rawY);
            switch (mode) {
                case 0:
                    this.drawSubcolumn(canvas, column, columnValue, true);
                    break;
                case 1:
                    this.checkRectToDraw(columnIndex, valueIndex);
                    break;
                case 2:
                    this.highlightSubcolumn(canvas, column, columnValue, valueIndex, true);
                    break;
                default:
                    throw new IllegalStateException("Cannot process column in mode: " + mode);
            }
        }

    }

    private void drawSubcolumn(Canvas canvas, Column column, SubcolumnValue columnValue, boolean isStacked) {
        canvas.drawRect(this.drawRect, this.columnPaint);
        if (column.hasLabels()) {
            this.drawLabel(canvas, column, columnValue, isStacked, (float)this.labelOffset);
        }

    }

    private void highlightSubcolumn(Canvas canvas, Column column, SubcolumnValue columnValue, int valueIndex, boolean isStacked) {
        if (this.selectedValue.getSecondIndex() == valueIndex) {
            this.columnPaint.setColor(columnValue.getDarkenColor());
            canvas.drawRect(this.drawRect.left - (float)this.touchAdditionalWidth, this.drawRect.top, this.drawRect.right + (float)this.touchAdditionalWidth, this.drawRect.bottom, this.columnPaint);
            if (column.hasLabels() || column.hasLabelsOnlyForSelected()) {
                this.drawLabel(canvas, column, columnValue, isStacked, (float)this.labelOffset);
            }
        }

    }

    private void checkRectToDraw(int columnIndex, int valueIndex) {
        if (this.drawRect.contains(this.touchedPoint.x, this.touchedPoint.y)) {
            this.selectedValue.set(columnIndex, valueIndex, SelectedValue.SelectedValueType.COLUMN);
        }

    }

    private float calculateColumnWidth() {
        float columnWidth = this.fillRatio * (float)this.computator.getContentRectMinusAllMargins().width() / this.computator.getVisibleViewport().width();
        if (columnWidth < 2.0F) {
            columnWidth = 2.0F;
        }

        return columnWidth;
    }

    private void calculateRectToDraw(SubcolumnValue columnValue, float left, float right, float rawBaseY, float rawY) {
        this.drawRect.left = left;
        this.drawRect.right = right;
        if (columnValue.getValue() >= this.baseValue) {
            this.drawRect.top = rawY;
            this.drawRect.bottom = rawBaseY - (float)this.subcolumnSpacing;
        } else {
            this.drawRect.bottom = rawY;
            this.drawRect.top = rawBaseY + (float)this.subcolumnSpacing;
        }

    }

    private void drawLabel(Canvas canvas, Column column, SubcolumnValue columnValue, boolean isStacked, float offset) {
        int numChars = column.getFormatter().formatChartValue(this.labelBuffer, columnValue);
        if (numChars != 0) {
            float labelWidth = this.labelPaint.measureText(this.labelBuffer, this.labelBuffer.length - numChars, numChars);
            int labelHeight = Math.abs(this.fontMetrics.ascent);
            float left = this.drawRect.centerX() - labelWidth / 2.0F - (float)this.labelMargin;
            float right = this.drawRect.centerX() + labelWidth / 2.0F + (float)this.labelMargin;
            float top;
            float bottom;
            if (isStacked && (float)labelHeight < this.drawRect.height() - (float)(2 * this.labelMargin)) {
                if (columnValue.getValue() >= this.baseValue) {
                    top = this.drawRect.top;
                    bottom = this.drawRect.top + (float)labelHeight + (float)(this.labelMargin * 2);
                } else {
                    top = this.drawRect.bottom - (float)labelHeight - (float)(this.labelMargin * 2);
                    bottom = this.drawRect.bottom;
                }
            } else {
                if (isStacked) {
                    return;
                }

                if (columnValue.getValue() >= this.baseValue) {
                    top = this.drawRect.top - offset - (float)labelHeight - (float)(this.labelMargin * 2);
                    if (top < (float)this.computator.getContentRectMinusAllMargins().top) {
                        top = this.drawRect.top + offset;
                        bottom = this.drawRect.top + offset + (float)labelHeight + (float)(this.labelMargin * 2);
                    } else {
                        bottom = this.drawRect.top - offset;
                    }
                } else {
                    bottom = this.drawRect.bottom + offset + (float)labelHeight + (float)(this.labelMargin * 2);
                    if (bottom > (float)this.computator.getContentRectMinusAllMargins().bottom) {
                        top = this.drawRect.bottom - offset - (float)labelHeight - (float)(this.labelMargin * 2);
                        bottom = this.drawRect.bottom - offset;
                    } else {
                        top = this.drawRect.bottom + offset;
                    }
                }
            }

            this.labelBackgroundRect.set(left, top, right, bottom);
            this.drawLabelTextAndBackground(canvas, this.labelBuffer, this.labelBuffer.length - numChars, numChars, columnValue.getDarkenColor());
        }
    }
}
