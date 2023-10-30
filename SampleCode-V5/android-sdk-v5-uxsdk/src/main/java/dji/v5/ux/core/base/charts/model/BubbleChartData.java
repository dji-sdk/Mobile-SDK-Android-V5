package dji.v5.ux.core.base.charts.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dji.v5.ux.core.base.charts.formatter.BubbleChartValueFormatter;
import dji.v5.ux.core.base.charts.formatter.SimpleBubbleChartValueFormatter;

public class BubbleChartData extends AbstractChartData {
    private BubbleChartValueFormatter formatter = new SimpleBubbleChartValueFormatter();
    private boolean hasLabels = false;
    private boolean hasLabelsOnlyForSelected = false;
    private int minBubbleRadius = 6;
    private float bubbleScale = 1.0F;
    private List<BubbleValue> values = new ArrayList<>();

    public BubbleChartData() {
    }

    public BubbleChartData(List<BubbleValue> values) {
        this.setValues(values);
    }

    public BubbleChartData(BubbleChartData data) {
        super(data);
        this.formatter = data.formatter;
        this.hasLabels = data.hasLabels;
        this.hasLabelsOnlyForSelected = data.hasLabelsOnlyForSelected;
        this.minBubbleRadius = data.minBubbleRadius;
        this.bubbleScale = data.bubbleScale;
        Iterator<BubbleValue> var2 = data.getValues().iterator();

        while(var2.hasNext()) {
            BubbleValue bubbleValue = (BubbleValue)var2.next();
            this.values.add(new BubbleValue(bubbleValue));
        }

    }

    public static BubbleChartData generateDummyData() {
        BubbleChartData data = new BubbleChartData();
        List<BubbleValue> values = new ArrayList<>(4);
        values.add(new BubbleValue(0.0F, 20.0F, 15000.0F));
        values.add(new BubbleValue(3.0F, 22.0F, 20000.0F));
        values.add(new BubbleValue(5.0F, 25.0F, 5000.0F));
        values.add(new BubbleValue(7.0F, 30.0F, 30000.0F));
        values.add(new BubbleValue(11.0F, 22.0F, 10.0F));
        data.setValues(values);
        return data;
    }

    public void update(float scale) {
        Iterator<BubbleValue> var2 = this.values.iterator();

        while(var2.hasNext()) {
            BubbleValue value = (BubbleValue)var2.next();
            value.update(scale);
        }

    }

    public void finish() {
        Iterator<BubbleValue> var1 = this.values.iterator();

        while(var1.hasNext()) {
            BubbleValue value = (BubbleValue)var1.next();
            value.finish();
        }

    }

    public List<BubbleValue> getValues() {
        return this.values;
    }

    public BubbleChartData setValues(List<BubbleValue> values) {
        if (null == values) {
            this.values = new ArrayList<>();
        } else {
            this.values = values;
        }

        return this;
    }

    public boolean hasLabels() {
        return this.hasLabels;
    }

    public BubbleChartData setHasLabels(boolean hasLabels) {
        this.hasLabels = hasLabels;
        if (hasLabels) {
            this.hasLabelsOnlyForSelected = false;
        }

        return this;
    }

    public boolean hasLabelsOnlyForSelected() {
        return this.hasLabelsOnlyForSelected;
    }

    public BubbleChartData setHasLabelsOnlyForSelected(boolean hasLabelsOnlyForSelected) {
        this.hasLabelsOnlyForSelected = hasLabelsOnlyForSelected;
        if (hasLabelsOnlyForSelected) {
            this.hasLabels = false;
        }

        return this;
    }

    public int getMinBubbleRadius() {
        return this.minBubbleRadius;
    }

    public void setMinBubbleRadius(int minBubbleRadius) {
        this.minBubbleRadius = minBubbleRadius;
    }

    public float getBubbleScale() {
        return this.bubbleScale;
    }

    public void setBubbleScale(float bubbleScale) {
        this.bubbleScale = bubbleScale;
    }

    public BubbleChartValueFormatter getFormatter() {
        return this.formatter;
    }

    public BubbleChartData setFormatter(BubbleChartValueFormatter formatter) {
        if (null != formatter) {
            this.formatter = formatter;
        }

        return this;
    }
}
