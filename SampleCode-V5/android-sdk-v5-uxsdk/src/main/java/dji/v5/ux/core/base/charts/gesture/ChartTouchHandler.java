package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.renderer.ChartRenderer;
import dji.v5.ux.core.base.charts.view.Chart;

public class ChartTouchHandler {
    protected GestureDetector gestureDetector;
    protected ScaleGestureDetector scaleGestureDetector;
    protected ChartScroller chartScroller;
    protected ChartZoomer chartZoomer;
    protected Chart chart;
    protected ChartComputator computator;
    protected ChartRenderer renderer;
    protected boolean isZoomEnabled = true;
    protected boolean isScrollEnabled = true;
    protected boolean isValueTouchEnabled = true;
    protected boolean isValueSelectionEnabled = false;
    protected SelectedValue selectionModeOldValue = new SelectedValue();
    protected SelectedValue selectedValue = new SelectedValue();
    protected SelectedValue oldSelectedValue = new SelectedValue();
    protected ViewParent viewParent;
    protected ContainerScrollType containerScrollType;

    public ChartTouchHandler(Context context, Chart chart) {
        this.chart = chart;
        this.computator = chart.getChartComputator();
        this.renderer = chart.getChartRenderer();
        this.gestureDetector = new GestureDetector(context, new ChartGestureListener());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ChartScaleGestureListener());
        this.chartScroller = new ChartScroller(context);
        this.chartZoomer = new ChartZoomer(context, ZoomType.HORIZONTAL_AND_VERTICAL);
    }

    public void resetTouchHandler() {
        this.computator = this.chart.getChartComputator();
        this.renderer = this.chart.getChartRenderer();
    }

    public boolean computeScroll() {
        boolean needInvalidate = false;
        if (this.isScrollEnabled && this.chartScroller.computeScrollOffset(this.computator)) {
            needInvalidate = true;
        }

        if (this.isZoomEnabled && this.chartZoomer.computeZoom(this.computator)) {
            needInvalidate = true;
        }

        return needInvalidate;
    }

    public boolean handleTouchEvent(MotionEvent event) {
        boolean needInvalidate = false;
        needInvalidate = this.gestureDetector.onTouchEvent(event);
        needInvalidate = this.scaleGestureDetector.onTouchEvent(event) || needInvalidate;
        if (this.isZoomEnabled && this.scaleGestureDetector.isInProgress()) {
            this.disallowParentInterceptTouchEvent();
        }

        if (this.isValueTouchEnabled) {
            needInvalidate = this.computeTouch(event) || needInvalidate;
        }

        return needInvalidate;
    }

    public boolean handleTouchEvent(MotionEvent event, ViewParent viewParent, ContainerScrollType containerScrollType) {
        this.viewParent = viewParent;
        this.containerScrollType = containerScrollType;
        return this.handleTouchEvent(event);
    }

    private void disallowParentInterceptTouchEvent() {
        if (null != this.viewParent) {
            this.viewParent.requestDisallowInterceptTouchEvent(true);
        }

    }

    private void allowParentInterceptTouchEvent(ChartScroller.ScrollResult scrollResult) {
        if (this.viewParent == null) {
            return;
        }

        if ((ContainerScrollType.HORIZONTAL == this.containerScrollType && !scrollResult.canScrollX && !this.scaleGestureDetector.isInProgress()) || (ContainerScrollType.VERTICAL == this.containerScrollType && !scrollResult.canScrollY && !this.scaleGestureDetector.isInProgress())) {
            this.viewParent.requestDisallowInterceptTouchEvent(false);
        }
    }

    private boolean computeTouch(MotionEvent event) {
        boolean needInvalidate = false;
        switch (event.getAction()) {
            case 0:
                boolean wasTouched = this.renderer.isTouched();
                boolean isTouched = this.checkTouch(event.getX(), event.getY());
                if (wasTouched != isTouched) {
                    needInvalidate = true;
                    if (this.isValueSelectionEnabled) {
                        this.selectionModeOldValue.clear();
                        if (wasTouched && !this.renderer.isTouched()) {
                            this.chart.callTouchListener();
                        }
                    }
                }
                break;
            case 1:
                if (this.renderer.isTouched()) {
                    if (this.checkTouch(event.getX(), event.getY())) {
                        if (this.isValueSelectionEnabled) {
                            if (!this.selectionModeOldValue.equals(this.selectedValue)) {
                                this.selectionModeOldValue.set(this.selectedValue);
                                this.chart.callTouchListener();
                            }
                        } else {
                            this.chart.callTouchListener();
                            this.renderer.clearTouch();
                        }
                    } else {
                        this.renderer.clearTouch();
                    }

                    needInvalidate = true;
                }
                break;
            case 2:
                if (this.renderer.isTouched() && !this.checkTouch(event.getX(), event.getY())) {
                    this.renderer.clearTouch();
                    needInvalidate = true;
                }
                break;
            case 3:
                if (this.renderer.isTouched()) {
                    this.renderer.clearTouch();
                    needInvalidate = true;
                }
                break;
            default:
                break;
        }

        return needInvalidate;
    }

    private boolean checkTouch(float touchX, float touchY) {
        this.oldSelectedValue.set(this.selectedValue);
        this.selectedValue.clear();
        if (this.renderer.checkTouch(touchX, touchY)) {
            this.selectedValue.set(this.renderer.getSelectedValue());
        }

        return this.oldSelectedValue.isSet() && this.selectedValue.isSet() && !this.oldSelectedValue.equals(this.selectedValue) ? false : this.renderer.isTouched();
    }

    public boolean isZoomEnabled() {
        return this.isZoomEnabled;
    }

    public void setZoomEnabled(boolean isZoomEnabled) {
        this.isZoomEnabled = isZoomEnabled;
    }

    public boolean isScrollEnabled() {
        return this.isScrollEnabled;
    }

    public void setScrollEnabled(boolean isScrollEnabled) {
        this.isScrollEnabled = isScrollEnabled;
    }

    public ZoomType getZoomType() {
        return this.chartZoomer.getZoomType();
    }

    public void setZoomType(ZoomType zoomType) {
        this.chartZoomer.setZoomType(zoomType);
    }

    public boolean isValueTouchEnabled() {
        return this.isValueTouchEnabled;
    }

    public void setValueTouchEnabled(boolean isValueTouchEnabled) {
        this.isValueTouchEnabled = isValueTouchEnabled;
    }

    public boolean isValueSelectionEnabled() {
        return this.isValueSelectionEnabled;
    }

    public void setValueSelectionEnabled(boolean isValueSelectionEnabled) {
        this.isValueSelectionEnabled = isValueSelectionEnabled;
    }

    protected class ChartGestureListener extends GestureDetector.SimpleOnGestureListener {
        protected ChartScroller.ScrollResult scrollResult = new ChartScroller.ScrollResult();

        protected ChartGestureListener() {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (ChartTouchHandler.this.isScrollEnabled) {
                ChartTouchHandler.this.disallowParentInterceptTouchEvent();
                return ChartTouchHandler.this.chartScroller.startScroll(ChartTouchHandler.this.computator);
            } else {
                return false;
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return ChartTouchHandler.this.isZoomEnabled ? ChartTouchHandler.this.chartZoomer.startZoom(e, ChartTouchHandler.this.computator) : false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (ChartTouchHandler.this.isScrollEnabled) {
                boolean canScroll = ChartTouchHandler.this.chartScroller.scroll(ChartTouchHandler.this.computator, distanceX, distanceY, this.scrollResult);
                ChartTouchHandler.this.allowParentInterceptTouchEvent(this.scrollResult);
                return canScroll;
            } else {
                return false;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return ChartTouchHandler.this.isScrollEnabled ? ChartTouchHandler.this.chartScroller.fling((int)(-velocityX), (int)(-velocityY), ChartTouchHandler.this.computator) : false;
        }
    }

    protected class ChartScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        protected ChartScaleGestureListener() {
            //do nothing
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (ChartTouchHandler.this.isZoomEnabled) {
                float scale = 2.0F - detector.getScaleFactor();
                if (Float.isInfinite(scale)) {
                    scale = 1.0F;
                }

                return ChartTouchHandler.this.chartZoomer.scale(ChartTouchHandler.this.computator, detector.getFocusX(), detector.getFocusY(), scale);
            } else {
                return false;
            }
        }
    }
}

