package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import dji.v5.ux.core.base.charts.view.Chart;

public class PreviewChartTouchHandler extends ChartTouchHandler {
    public PreviewChartTouchHandler(Context context, Chart chart) {
        super(context, chart);
        this.gestureDetector = new GestureDetector(context, new PreviewChartGestureListener());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ChartScaleGestureListener());
        this.isValueTouchEnabled = false;
        this.isValueSelectionEnabled = false;
    }

    protected class PreviewChartGestureListener extends ChartTouchHandler.ChartGestureListener {
        protected PreviewChartGestureListener() {
            super();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return super.onScroll(e1, e2, -distanceX, -distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, -velocityX, -velocityY);
        }
    }

    protected class ChartScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        protected ChartScaleGestureListener() {
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (PreviewChartTouchHandler.this.isZoomEnabled) {
                float scale = detector.getCurrentSpan() / detector.getPreviousSpan();
                if (Float.isInfinite(scale)) {
                    scale = 1.0F;
                }

                return PreviewChartTouchHandler.this.chartZoomer.scale(PreviewChartTouchHandler.this.computator, detector.getFocusX(), detector.getFocusY(), scale);
            } else {
                return false;
            }
        }
    }
}

