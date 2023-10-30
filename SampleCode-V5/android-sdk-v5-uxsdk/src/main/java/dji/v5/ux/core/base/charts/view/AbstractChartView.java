package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.ViewCompat;

import dji.v5.ux.core.base.charts.animation.ChartAnimationListener;
import dji.v5.ux.core.base.charts.animation.ChartDataAnimator;
import dji.v5.ux.core.base.charts.animation.ChartDataAnimatorV14;
import dji.v5.ux.core.base.charts.animation.ChartDataAnimatorV8;
import dji.v5.ux.core.base.charts.animation.ChartViewportAnimator;
import dji.v5.ux.core.base.charts.animation.ChartViewportAnimatorV14;
import dji.v5.ux.core.base.charts.animation.ChartViewportAnimatorV8;
import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.gesture.ChartTouchHandler;
import dji.v5.ux.core.base.charts.gesture.ContainerScrollType;
import dji.v5.ux.core.base.charts.gesture.ZoomType;
import dji.v5.ux.core.base.charts.listener.ViewportChangeListener;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.renderer.AxesRenderer;
import dji.v5.ux.core.base.charts.renderer.ChartRenderer;
import dji.v5.ux.core.base.charts.util.ChartUtils;

public abstract class AbstractChartView extends View implements Chart {
    protected ChartComputator chartComputator;
    protected AxesRenderer axesRenderer;
    protected ChartTouchHandler touchHandler;
    protected ChartRenderer chartRenderer;
    protected ChartDataAnimator dataAnimator;
    protected ChartViewportAnimator viewportAnimator;
    protected boolean isInteractive;
    protected boolean isContainerScrollEnabled;
    protected ContainerScrollType containerScrollType;

    protected AbstractChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    protected AbstractChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    protected AbstractChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.isInteractive = true;
        this.isContainerScrollEnabled = false;
        this.chartComputator = new ChartComputator();
        this.touchHandler = new ChartTouchHandler(context, this);
        this.axesRenderer = new AxesRenderer(context, this);
        if (Build.VERSION.SDK_INT < 14) {
            this.dataAnimator = new ChartDataAnimatorV8(this);
            this.viewportAnimator = new ChartViewportAnimatorV8(this);
        } else {
            this.viewportAnimator = new ChartViewportAnimatorV14(this);
            this.dataAnimator = new ChartDataAnimatorV14(this);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.chartComputator.setContentRect(this.getWidth(), this.getHeight(), this.getPaddingLeft(), this.getPaddingTop(), this.getPaddingRight(), this.getPaddingBottom());
        this.chartRenderer.onChartSizeChanged();
        this.axesRenderer.onChartSizeChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.isEnabled()) {
            this.axesRenderer.drawInBackground(canvas);
            int clipRestoreCount = canvas.save();
            canvas.clipRect(this.chartComputator.getContentRectMinusAllMargins());
            this.chartRenderer.draw(canvas);
            canvas.restoreToCount(clipRestoreCount);
            this.chartRenderer.drawUnclipped(canvas);
            this.axesRenderer.drawInForeground(canvas);
        } else {
            canvas.drawColor(ChartUtils.DEFAULT_COLOR);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (this.isInteractive) {
            boolean needInvalidate;
            if (this.isContainerScrollEnabled) {
                needInvalidate = this.touchHandler.handleTouchEvent(event, this.getParent(), this.containerScrollType);
            } else {
                needInvalidate = this.touchHandler.handleTouchEvent(event);
            }

            if (needInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (this.isInteractive && this.touchHandler.computeScroll()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }

    }

    public void startDataAnimation() {
        this.dataAnimator.startAnimation(Long.MIN_VALUE);
    }

    public void startDataAnimation(long duration) {
        this.dataAnimator.startAnimation(duration);
    }

    public void cancelDataAnimation() {
        this.dataAnimator.cancelAnimation();
    }

    public void animationDataUpdate(float scale) {
        this.getChartData().update(scale);
        this.chartRenderer.onChartViewportChanged();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void animationDataFinished() {
        this.getChartData().finish();
        this.chartRenderer.onChartViewportChanged();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setDataAnimationListener(ChartAnimationListener animationListener) {
        this.dataAnimator.setChartAnimationListener(animationListener);
    }

    public void setViewportAnimationListener(ChartAnimationListener animationListener) {
        this.viewportAnimator.setChartAnimationListener(animationListener);
    }

    public void setViewportChangeListener(ViewportChangeListener viewportChangeListener) {
        this.chartComputator.setViewportChangeListener(viewportChangeListener);
    }

    public ChartRenderer getChartRenderer() {
        return this.chartRenderer;
    }

    public void setChartRenderer(ChartRenderer renderer) {
        this.chartRenderer = renderer;
        this.resetRendererAndTouchHandler();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public AxesRenderer getAxesRenderer() {
        return this.axesRenderer;
    }

    public ChartComputator getChartComputator() {
        return this.chartComputator;
    }

    public ChartTouchHandler getTouchHandler() {
        return this.touchHandler;
    }

    public boolean isInteractive() {
        return this.isInteractive;
    }

    public void setInteractive(boolean isInteractive) {
        this.isInteractive = isInteractive;
    }

    public boolean isZoomEnabled() {
        return this.touchHandler.isZoomEnabled();
    }

    public void setZoomEnabled(boolean isZoomEnabled) {
        this.touchHandler.setZoomEnabled(isZoomEnabled);
    }

    public boolean isScrollEnabled() {
        return this.touchHandler.isScrollEnabled();
    }

    public void setScrollEnabled(boolean isScrollEnabled) {
        this.touchHandler.setScrollEnabled(isScrollEnabled);
    }

    public void moveTo(float x, float y) {
        Viewport scrollViewport = this.computeScrollViewport(x, y);
        this.setCurrentViewport(scrollViewport);
    }

    public void moveToWithAnimation(float x, float y) {
        Viewport scrollViewport = this.computeScrollViewport(x, y);
        this.setCurrentViewportWithAnimation(scrollViewport);
    }

    private Viewport computeScrollViewport(float x, float y) {
        Viewport maxViewport = this.getMaximumViewport();
        Viewport currentViewport = this.getCurrentViewport();
        Viewport scrollViewport = new Viewport(currentViewport);
        if (maxViewport.contains(x, y)) {
            float width = currentViewport.width();
            float height = currentViewport.height();
            float halfWidth = width / 2.0F;
            float halfHeight = height / 2.0F;
            float left = x - halfWidth;
            float top = y + halfHeight;
            left = Math.max(maxViewport.left, Math.min(left, maxViewport.right - width));
            top = Math.max(maxViewport.bottom + height, Math.min(top, maxViewport.top));
            scrollViewport.set(left, top, left + width, top - height);
        }

        return scrollViewport;
    }

    public boolean isValueTouchEnabled() {
        return this.touchHandler.isValueTouchEnabled();
    }

    public void setValueTouchEnabled(boolean isValueTouchEnabled) {
        this.touchHandler.setValueTouchEnabled(isValueTouchEnabled);
    }

    public ZoomType getZoomType() {
        return this.touchHandler.getZoomType();
    }

    public void setZoomType(ZoomType zoomType) {
        this.touchHandler.setZoomType(zoomType);
    }

    public float getMaxZoom() {
        return this.chartComputator.getMaxZoom();
    }

    public void setMaxZoom(float maxZoom) {
        this.chartComputator.setMaxZoom(maxZoom);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public float getZoomLevel() {
        Viewport maxViewport = this.getMaximumViewport();
        Viewport currentViewport = this.getCurrentViewport();
        return Math.max(maxViewport.width() / currentViewport.width(), maxViewport.height() / currentViewport.height());
    }

    public void setZoomLevel(float x, float y, float zoomLevel) {
        Viewport zoomViewport = this.computeZoomViewport(x, y, zoomLevel);
        this.setCurrentViewport(zoomViewport);
    }

    public void setZoomLevelWithAnimation(float x, float y, float zoomLevel) {
        Viewport zoomViewport = this.computeZoomViewport(x, y, zoomLevel);
        this.setCurrentViewportWithAnimation(zoomViewport);
    }

    private Viewport computeZoomViewport(float x, float y, float zoomLevel) {
        Viewport maxViewport = this.getMaximumViewport();
        Viewport zoomViewport = new Viewport(this.getMaximumViewport());
        if (maxViewport.contains(x, y)) {
            if (zoomLevel < 1.0F) {
                zoomLevel = 1.0F;
            } else if (zoomLevel > this.getMaxZoom()) {
                zoomLevel = this.getMaxZoom();
            }

            float newWidth = zoomViewport.width() / zoomLevel;
            float newHeight = zoomViewport.height() / zoomLevel;
            float halfWidth = newWidth / 2.0F;
            float halfHeight = newHeight / 2.0F;
            float left = x - halfWidth;
            float right = x + halfWidth;
            float top = y + halfHeight;
            float bottom = y - halfHeight;
            if (left < maxViewport.left) {
                left = maxViewport.left;
                right = left + newWidth;
            } else if (right > maxViewport.right) {
                right = maxViewport.right;
                left = right - newWidth;
            }

            if (top > maxViewport.top) {
                top = maxViewport.top;
                bottom = top - newHeight;
            } else if (bottom < maxViewport.bottom) {
                bottom = maxViewport.bottom;
                top = bottom + newHeight;
            }

            ZoomType zoomType = this.getZoomType();
            if (ZoomType.HORIZONTAL_AND_VERTICAL == zoomType) {
                zoomViewport.set(left, top, right, bottom);
            } else if (ZoomType.HORIZONTAL == zoomType) {
                zoomViewport.left = left;
                zoomViewport.right = right;
            } else if (ZoomType.VERTICAL == zoomType) {
                zoomViewport.top = top;
                zoomViewport.bottom = bottom;
            }
        }

        return zoomViewport;
    }

    public Viewport getMaximumViewport() {
        return this.chartRenderer.getMaximumViewport();
    }

    public void setMaximumViewport(Viewport maxViewport) {
        this.chartRenderer.setMaximumViewport(maxViewport);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setCurrentViewportWithAnimation(Viewport targetViewport) {
        if (null != targetViewport) {
            this.viewportAnimator.cancelAnimation();
            this.viewportAnimator.startAnimation(this.getCurrentViewport(), targetViewport);
        }

        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setCurrentViewportWithAnimation(Viewport targetViewport, long duration) {
        if (null != targetViewport) {
            this.viewportAnimator.cancelAnimation();
            this.viewportAnimator.startAnimation(this.getCurrentViewport(), targetViewport, duration);
        }

        ViewCompat.postInvalidateOnAnimation(this);
    }

    public Viewport getCurrentViewport() {
        return this.getChartRenderer().getCurrentViewport();
    }

    public void setCurrentViewport(Viewport targetViewport) {
        if (null != targetViewport) {
            this.chartRenderer.setCurrentViewport(targetViewport);
        }

        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void resetViewports() {
        this.chartRenderer.setMaximumViewport((Viewport)null);
        this.chartRenderer.setCurrentViewport((Viewport)null);
    }

    public boolean isViewportCalculationEnabled() {
        return this.chartRenderer.isViewportCalculationEnabled();
    }

    public void setViewportCalculationEnabled(boolean isEnabled) {
        this.chartRenderer.setViewportCalculationEnabled(isEnabled);
    }

    public boolean isValueSelectionEnabled() {
        return this.touchHandler.isValueSelectionEnabled();
    }

    public void setValueSelectionEnabled(boolean isValueSelectionEnabled) {
        this.touchHandler.setValueSelectionEnabled(isValueSelectionEnabled);
    }

    public void selectValue(SelectedValue selectedValue) {
        this.chartRenderer.selectValue(selectedValue);
        this.callTouchListener();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public SelectedValue getSelectedValue() {
        return this.chartRenderer.getSelectedValue();
    }

    public boolean isContainerScrollEnabled() {
        return this.isContainerScrollEnabled;
    }

    public void setContainerScrollEnabled(boolean isContainerScrollEnabled, ContainerScrollType containerScrollType) {
        this.isContainerScrollEnabled = isContainerScrollEnabled;
        this.containerScrollType = containerScrollType;
    }

    protected void onChartDataChange() {
        this.chartComputator.resetContentRect();
        this.chartRenderer.onChartDataChanged();
        this.axesRenderer.onChartDataChanged();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    protected void resetRendererAndTouchHandler() {
        this.chartRenderer.resetRenderer();
        this.axesRenderer.resetRenderer();
        this.touchHandler.resetTouchHandler();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if ((double)this.getZoomLevel() <= 1.0) {
            return false;
        } else {
            Viewport currentViewport = this.getCurrentViewport();
            Viewport maximumViewport = this.getMaximumViewport();
            if (direction < 0) {
                return currentViewport.left > maximumViewport.left;
            } else {
                return currentViewport.right < maximumViewport.right;
            }
        }
    }
}
