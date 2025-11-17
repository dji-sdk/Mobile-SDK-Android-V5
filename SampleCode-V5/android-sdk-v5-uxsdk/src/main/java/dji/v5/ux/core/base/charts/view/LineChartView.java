package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.graphics.Shader;
import android.util.AttributeSet;

import dji.v5.ux.core.base.charts.listener.DummyLineChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.listener.LineChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.Line;
import dji.v5.ux.core.base.charts.model.LineChartData;
import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.provider.LineChartDataProvider;
import dji.v5.ux.core.base.charts.renderer.LineChartRenderer;

public class LineChartView extends AbstractChartView implements LineChartDataProvider {
    protected LineChartData data;
    protected LineChartOnValueSelectListener onValueTouchListener;

    public LineChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.onValueTouchListener = new DummyLineChartOnValueSelectListener();
        this.setChartRenderer(new LineChartRenderer(context, this, this));
        this.setLineChartData(LineChartData.generateDummyData());
    }

    public LineChartData getLineChartData() {
        return this.data;
    }

    public void setLineChartData(LineChartData data) {
        if (null == data) {
            this.data = LineChartData.generateDummyData();
        } else {
            this.data = data;
        }

        super.onChartDataChange();
    }

    public ChartData getChartData() {
        return this.data;
    }

    public void callTouchListener() {
        SelectedValue selectedValue = this.chartRenderer.getSelectedValue();
        if (selectedValue.isSet()) {
            PointValue point = (PointValue)((Line)this.data.getLines().get(selectedValue.getFirstIndex())).getValues().get(selectedValue.getSecondIndex());
            this.onValueTouchListener.onValueSelected(selectedValue.getFirstIndex(), selectedValue.getSecondIndex(), point);
        } else {
            this.onValueTouchListener.onValueDeselected();
        }

    }

    public LineChartOnValueSelectListener getOnValueTouchListener() {
        return this.onValueTouchListener;
    }

    public void setOnValueTouchListener(LineChartOnValueSelectListener touchListener) {
        if (null != touchListener) {
            this.onValueTouchListener = touchListener;
        }

    }

    public void setLineShader(Shader shader) {
        if (this.chartRenderer instanceof LineChartRenderer) {
            ((LineChartRenderer)this.chartRenderer).setShader(shader);
        }

    }
}
