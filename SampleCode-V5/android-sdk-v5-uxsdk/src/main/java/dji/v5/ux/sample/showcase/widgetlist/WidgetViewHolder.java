/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.sample.showcase.widgetlist;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.utils.common.DisplayUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;

/**
 * A view holder for a single widget.
 */
public class WidgetViewHolder<T> {

    //region Fields
    private static final String TAG = "WidgetViewHolder";
    private ViewGroup widget;
    private final Class<T> clazz;
    private final int layoutWidthDp;
    private final int layoutHeightDp;
    //endregion

    //region Lifecycle
    public WidgetViewHolder(Class<T> clazz) {
        this(clazz, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public WidgetViewHolder(Class<T> clazz, int layoutWidthDp, int layoutHeightDp) {
        this.clazz = clazz;
        this.layoutWidthDp = layoutWidthDp;
        this.layoutHeightDp = layoutHeightDp;
    }
    //endregion

    /**
     * Create the widget using reflection.
     *
     * @param context An instance of {@link Context}.
     * @return The widget that was created.
     */
    @Nullable
    public ViewGroup getWidget(Context context) {
        int layoutWidthPx;
        int layoutHeightPx;
        if (layoutWidthDp == ViewGroup.LayoutParams.MATCH_PARENT || layoutWidthDp == ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutWidthPx = layoutWidthDp;
        } else {
            layoutWidthPx = (int) DisplayUtil.dipToPx(context, layoutWidthDp);
        }
        if (layoutHeightDp == ViewGroup.LayoutParams.MATCH_PARENT || layoutHeightDp == ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutHeightPx = layoutHeightDp;
        } else {
            layoutHeightPx = (int) DisplayUtil.dipToPx(context, layoutHeightDp);
        }

        try {
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            widget = (ViewGroup) constructor.newInstance(context);
            ViewGroup.LayoutParams simulatorIndicatorParams = new LinearLayout.LayoutParams(
                    layoutWidthPx,
                    layoutHeightPx);
            widget.setLayoutParams(simulatorIndicatorParams);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LogUtils.e(TAG, e + " " + e.getCause());
        }
        return widget;
    }

    /**
     * Gets the ideal dimension ratio of the widget.
     *
     * @return A String object with the ideal dimension ratio.
     */
    @NonNull
    public String getIdealDimensionRatioString() {
        if (widget instanceof ConstraintLayoutWidget) {
            return ((ConstraintLayoutWidget) widget).getIdealDimensionRatioString();
        } else if (widget instanceof FrameLayoutWidget) {
            return ((FrameLayoutWidget) widget).getIdealDimensionRatioString();
        } else {
            return "";
        }
    }

    /**
     * Gets the current measured size of the widget
     *
     * @return A String object with the current size in the format [w,h].
     */
    public String getWidgetSize() {
        if (widget == null) {
            return "[0,0]";
        }
        return "[" + (widget.getMeasuredWidth()) + "," + (widget.getMeasuredHeight()) + "]";
    }
}
