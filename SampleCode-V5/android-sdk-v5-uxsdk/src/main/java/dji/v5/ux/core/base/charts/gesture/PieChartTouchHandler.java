package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.core.widget.ScrollerCompat;

import dji.v5.ux.core.base.charts.view.PieChartView;

public class PieChartTouchHandler extends ChartTouchHandler {
    protected ScrollerCompat scroller;
    protected PieChartView pieChart;
    private boolean isRotationEnabled = true;

    public PieChartTouchHandler(Context context, PieChartView chart) {
        super(context, chart);
        this.pieChart = chart;
        this.scroller = ScrollerCompat.create(context);
        this.gestureDetector = new GestureDetector(context, new ChartGestureListener());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ChartScaleGestureListener());
        this.isZoomEnabled = false;
    }

    @Override
    public boolean computeScroll() {
        if (!this.isRotationEnabled) {
            return false;
        }
        if (this.scroller.computeScrollOffset()) {
            this.pieChart.setChartRotation(this.scroller.getCurrY(), false);
        }
        return true;
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event) {
        boolean needInvalidate = super.handleTouchEvent(event);
        if (this.isRotationEnabled) {
            needInvalidate = this.gestureDetector.onTouchEvent(event) || needInvalidate;
        }

        return needInvalidate;
    }

    public boolean isRotationEnabled() {
        return this.isRotationEnabled;
    }

    public void setRotationEnabled(boolean isRotationEnabled) {
        this.isRotationEnabled = isRotationEnabled;
    }

    private class ChartGestureListener extends GestureDetector.SimpleOnGestureListener {
        private ChartGestureListener() {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (PieChartTouchHandler.this.isRotationEnabled) {
                PieChartTouchHandler.this.scroller.abortAnimation();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (PieChartTouchHandler.this.isRotationEnabled) {
                RectF circleOval = PieChartTouchHandler.this.pieChart.getCircleOval();
                float centerX = circleOval.centerX();
                float centerY = circleOval.centerY();
                float scrollTheta = this.vectorToScalarScroll(distanceX, distanceY, e2.getX() - centerX, e2.getY() - centerY);
                PieChartTouchHandler.this.pieChart.setChartRotation(PieChartTouchHandler.this.pieChart.getChartRotation() - (int)scrollTheta / 4, false);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (PieChartTouchHandler.this.isRotationEnabled) {
                RectF circleOval = PieChartTouchHandler.this.pieChart.getCircleOval();
                float centerX = circleOval.centerX();
                float centerY = circleOval.centerY();
                float scrollTheta = this.vectorToScalarScroll(velocityX, velocityY, e2.getX() - centerX, e2.getY() - centerY);
                PieChartTouchHandler.this.scroller.abortAnimation();
                PieChartTouchHandler.this.scroller.fling(0, PieChartTouchHandler.this.pieChart.getChartRotation(), 0, (int)scrollTheta / 4, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                return true;
            } else {
                return false;
            }
        }

        private float vectorToScalarScroll(float dx, float dy, float x, float y) {
            float l = (float)Math.sqrt((double)(dx * dx + dy * dy));
            float crossX = -y;
            float dot = crossX * dx + x * dy;
            float sign = Math.signum(dot);
            return l * sign;
        }
    }

    private class ChartScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ChartScaleGestureListener() {
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }
    }
}

