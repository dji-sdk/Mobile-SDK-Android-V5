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
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;

/**
 * A view with a single widget and an indicator of the current size of the widget. This widget
 * can be resized using pinch-to-zoom and the size indicator updates to match the current
 * dimensions of the widget.
 */
public class WidgetView extends ConstraintLayout {

    protected LinearLayout containerView;
    protected TextView aspectRatioTextView;
    protected TextView currentSizeTextView;
    //region Fields
    private WidgetViewHolder<?> widgetViewHolder;
    private ScaleGestureDetector scaleGestureDetector;
    //endregion
    private int originalHeight;
    private int originalWidth;
    private float scaleFactor = 1.0f;
    //region Views
    private ViewGroup widget;

    public WidgetView(Context context) {
        super(context);
    }

    public WidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Initializes the view with the given {@link WidgetViewHolder} object and returns the widget
     * that is part of this WidgetView.
     *
     * @param widgetViewHolder An instance of {@link WidgetViewHolder}.
     * @return The initialized widget.
     */
    public ViewGroup init(WidgetViewHolder<?> widgetViewHolder) {
        this.widgetViewHolder = widgetViewHolder;
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        inflate(getContext(), R.layout.uxsdk_view_widget, this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        setLayoutParams(params);
        containerView = findViewById(R.id.widget_container);
        aspectRatioTextView = findViewById(R.id.textview_aspect_ratio);
        currentSizeTextView = findViewById(R.id.textview_current_size);
        findViewById(R.id.widget_container).setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        widget = widgetViewHolder.getWidget(getContext());
        if (widget != null) {
            containerView.addView(widget);
        }
        aspectRatioTextView.setText(widgetViewHolder.getIdealDimensionRatioString());
        return widget;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(()->{
            if (originalHeight == 0 && originalWidth == 0) {
                originalHeight = widget.getHeight();
                originalWidth = widget.getWidth();
                this.invalidate();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (widgetViewHolder == null) {
            return;
        }
        currentSizeTextView.setText(widgetViewHolder.getWidgetSize());
    }

    /**
     * A gesture listener that scales the widget.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleFactor *= scaleGestureDetector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
            if (widget != null) {
                ViewGroup.LayoutParams layoutParams = widget.getLayoutParams();
                layoutParams.height = originalHeight * (int) scaleFactor;
                layoutParams.width = originalWidth * (int) scaleFactor;
                widget.setLayoutParams(layoutParams);
                if (widget instanceof FPVInteractionWidget) {
                    ((FPVInteractionWidget) widget).adjustAspectRatio(layoutParams.width, layoutParams.height);
                }
                currentSizeTextView.setText(widgetViewHolder.getWidgetSize());
            }
            return true;
        }
    }

}
