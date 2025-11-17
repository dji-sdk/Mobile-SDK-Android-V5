package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.view.ViewCompat;
import dji.v5.ux.core.base.charts.listener.BubbleChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.listener.DummyBubbleChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.model.BubbleChartData;
import dji.v5.ux.core.base.charts.model.BubbleValue;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.provider.BubbleChartDataProvider;
import dji.v5.ux.core.base.charts.renderer.BubbleChartRenderer;

public class BubbleChartView extends AbstractChartView implements BubbleChartDataProvider {
    protected BubbleChartData data;
    protected BubbleChartOnValueSelectListener onValueTouchListener;
    protected BubbleChartRenderer bubbleChartRenderer;

    public BubbleChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public BubbleChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.onValueTouchListener = new DummyBubbleChartOnValueSelectListener();
        this.bubbleChartRenderer = new BubbleChartRenderer(context, this, this);
        this.setChartRenderer(this.bubbleChartRenderer);
        this.setBubbleChartData(BubbleChartData.generateDummyData());
    }

    public BubbleChartData getBubbleChartData() {
        return this.data;
    }

    public void setBubbleChartData(BubbleChartData data) {
        if (null == data) {
            this.data = BubbleChartData.generateDummyData();
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
            BubbleValue value = (BubbleValue)this.data.getValues().get(selectedValue.getFirstIndex());
            this.onValueTouchListener.onValueSelected(selectedValue.getFirstIndex(), value);
        } else {
            this.onValueTouchListener.onValueDeselected();
        }

    }

    public BubbleChartOnValueSelectListener getOnValueTouchListener() {
        return this.onValueTouchListener;
    }

    public void setOnValueTouchListener(BubbleChartOnValueSelectListener touchListener) {
        if (null != touchListener) {
            this.onValueTouchListener = touchListener;
        }

    }

    public void removeMargins() {
        this.bubbleChartRenderer.removeMargins();
        ViewCompat.postInvalidateOnAnimation(this);
    }
}

