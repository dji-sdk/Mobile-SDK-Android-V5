package dji.v5.ux.core.base.charts.model;

import android.graphics.Typeface;

public interface ChartData {
    void update(float var1);

    void finish();

    Axis getAxisXBottom();

    void setAxisXBottom(Axis var1);

    Axis getAxisYLeft();

    void setAxisYLeft(Axis var1);

    Axis getAxisXTop();

    void setAxisXTop(Axis var1);

    Axis getAxisYRight();

    void setAxisYRight(Axis var1);

    int getValueLabelTextColor();

    void setValueLabelsTextColor(int var1);

    int getValueLabelTextSize();

    void setValueLabelTextSize(int var1);

    Typeface getValueLabelTypeface();

    void setValueLabelTypeface(Typeface var1);

    boolean isValueLabelBackgroundEnabled();

    void setValueLabelBackgroundEnabled(boolean var1);

    boolean isValueLabelBackgroundAuto();

    void setValueLabelBackgroundAuto(boolean var1);

    int getValueLabelBackgroundColor();

    void setValueLabelBackgroundColor(int var1);
}

