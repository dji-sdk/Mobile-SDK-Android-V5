package dji.v5.ux.core.base.charts.computator;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

import dji.v5.ux.core.base.charts.listener.DummyVieportChangeListener;
import dji.v5.ux.core.base.charts.listener.ViewportChangeListener;
import dji.v5.ux.core.base.charts.model.Viewport;

public class ChartComputator {
    protected static final float DEFAULT_MAXIMUM_ZOOM = 20.0F;
    protected float maxZoom = 20.0F;
    protected int chartWidth;
    protected int chartHeight;
    protected Rect contentRectMinusAllMargins = new Rect();
    protected Rect contentRectMinusAxesMargins = new Rect();
    protected Rect maxContentRect = new Rect();
    protected Viewport currentViewport = new Viewport();
    protected Viewport maxViewport = new Viewport();
    protected float minViewportWidth;
    protected float minViewportHeight;
    protected ViewportChangeListener viewportChangeListener = new DummyVieportChangeListener();

    public ChartComputator() {
        //do nothing
    }

    public void setContentRect(int width, int height, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        this.chartWidth = width;
        this.chartHeight = height;
        this.maxContentRect.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
        this.contentRectMinusAxesMargins.set(this.maxContentRect);
        this.contentRectMinusAllMargins.set(this.maxContentRect);
    }

    public void resetContentRect() {
        this.contentRectMinusAxesMargins.set(this.maxContentRect);
        this.contentRectMinusAllMargins.set(this.maxContentRect);
    }

    public void insetContentRect(int deltaLeft, int deltaTop, int deltaRight, int deltaBottom) {
        this.contentRectMinusAxesMargins.left += deltaLeft;
        this.contentRectMinusAxesMargins.top += deltaTop;
        this.contentRectMinusAxesMargins.right -= deltaRight;
        this.contentRectMinusAxesMargins.bottom -= deltaBottom;
        this.insetContentRectByInternalMargins(deltaLeft, deltaTop, deltaRight, deltaBottom);
    }

    public void insetContentRectByInternalMargins(int deltaLeft, int deltaTop, int deltaRight, int deltaBottom) {
        this.contentRectMinusAllMargins.left += deltaLeft;
        this.contentRectMinusAllMargins.top += deltaTop;
        this.contentRectMinusAllMargins.right -= deltaRight;
        this.contentRectMinusAllMargins.bottom -= deltaBottom;
    }

    public void constrainViewport(float left, float top, float right, float bottom) {
        if (right - left < this.minViewportWidth) {
            right = left + this.minViewportWidth;
            if (left < this.maxViewport.left) {
                left = this.maxViewport.left;
                right = left + this.minViewportWidth;
            } else if (right > this.maxViewport.right) {
                right = this.maxViewport.right;
                left = right - this.minViewportWidth;
            }
        }

        if (top - bottom < this.minViewportHeight) {
            bottom = top - this.minViewportHeight;
            if (top > this.maxViewport.top) {
                top = this.maxViewport.top;
                bottom = top - this.minViewportHeight;
            } else if (bottom < this.maxViewport.bottom) {
                bottom = this.maxViewport.bottom;
                top = bottom + this.minViewportHeight;
            }
        }

        this.currentViewport.left = Math.max(this.maxViewport.left, left);
        this.currentViewport.top = Math.min(this.maxViewport.top, top);
        this.currentViewport.right = Math.min(this.maxViewport.right, right);
        this.currentViewport.bottom = Math.max(this.maxViewport.bottom, bottom);
        this.viewportChangeListener.onViewportChanged(this.currentViewport);
    }

    public void setViewportTopLeft(float left, float top) {
        float curWidth = this.currentViewport.width();
        float curHeight = this.currentViewport.height();
        left = Math.max(this.maxViewport.left, Math.min(left, this.maxViewport.right - curWidth));
        top = Math.max(this.maxViewport.bottom + curHeight, Math.min(top, this.maxViewport.top));
        this.constrainViewport(left, top, left + curWidth, top - curHeight);
    }

    public float computeRawX(float valueX) {
        float pixelOffset = (valueX - this.currentViewport.left) * (this.contentRectMinusAllMargins.width() / this.currentViewport.width());
        return (float)this.contentRectMinusAllMargins.left + pixelOffset;
    }

    public float computeRawY(float valueY) {
        float pixelOffset = (valueY - this.currentViewport.bottom) * (this.contentRectMinusAllMargins.height() / this.currentViewport.height());
        return (float)this.contentRectMinusAllMargins.bottom - pixelOffset;
    }

    public float computeRawDistanceX(float distance) {
        return distance * (this.contentRectMinusAllMargins.width() / this.currentViewport.width());
    }

    public float computeRawDistanceY(float distance) {
        return distance * (this.contentRectMinusAllMargins.height() / this.currentViewport.height());
    }

    public boolean rawPixelsToDataPoint(float x, float y, PointF dest) {
        if (!this.contentRectMinusAllMargins.contains((int)x, (int)y)) {
            return false;
        } else {
            dest.set(this.currentViewport.left + (x - this.contentRectMinusAllMargins.left) * this.currentViewport.width() / this.contentRectMinusAllMargins.width(), this.currentViewport.bottom + (y - this.contentRectMinusAllMargins.bottom) * this.currentViewport.height() / (-this.contentRectMinusAllMargins.height()));
            return true;
        }
    }

    public void computeScrollSurfaceSize(Point out) {
        out.set((int)(this.maxViewport.width() * this.contentRectMinusAllMargins.width() / this.currentViewport.width()), (int)(this.maxViewport.height() * this.contentRectMinusAllMargins.height() / this.currentViewport.height()));
    }

    public boolean isWithinContentRect(float x, float y, float precision) {
        return x >= this.contentRectMinusAllMargins.left - precision && x <= this.contentRectMinusAllMargins.right + precision && y <= this.contentRectMinusAllMargins.bottom + precision && y >= this.contentRectMinusAllMargins.top - precision;
    }

    public Rect getContentRectMinusAllMargins() {
        return this.contentRectMinusAllMargins;
    }

    public Rect getContentRectMinusAxesMargins() {
        return this.contentRectMinusAxesMargins;
    }

    public Viewport getCurrentViewport() {
        return this.currentViewport;
    }

    public void setCurrentViewport(Viewport viewport) {
        this.constrainViewport(viewport.left, viewport.top, viewport.right, viewport.bottom);
    }

    public void setCurrentViewport(float left, float top, float right, float bottom) {
        this.constrainViewport(left, top, right, bottom);
    }

    public Viewport getMaximumViewport() {
        return this.maxViewport;
    }

    public void setMaxViewport(Viewport maxViewport) {
        this.setMaxViewport(maxViewport.left, maxViewport.top, maxViewport.right, maxViewport.bottom);
    }

    public void setMaxViewport(float left, float top, float right, float bottom) {
        this.maxViewport.set(left, top, right, bottom);
        this.computeMinimumWidthAndHeight();
    }

    public Viewport getVisibleViewport() {
        return this.currentViewport;
    }

    public void setVisibleViewport(Viewport visibleViewport) {
        this.setCurrentViewport(visibleViewport);
    }

    public float getMinimumViewportWidth() {
        return this.minViewportWidth;
    }

    public float getMinimumViewportHeight() {
        return this.minViewportHeight;
    }

    public void setViewportChangeListener(ViewportChangeListener viewportChangeListener) {
        if (null == viewportChangeListener) {
            this.viewportChangeListener = new DummyVieportChangeListener();
        } else {
            this.viewportChangeListener = viewportChangeListener;
        }

    }

    public int getChartWidth() {
        return this.chartWidth;
    }

    public int getChartHeight() {
        return this.chartHeight;
    }

    public float getMaxZoom() {
        return this.maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        if (maxZoom < 1.0F) {
            maxZoom = 1.0F;
        }

        this.maxZoom = maxZoom;
        this.computeMinimumWidthAndHeight();
        this.setCurrentViewport(this.currentViewport);
    }

    private void computeMinimumWidthAndHeight() {
        this.minViewportWidth = this.maxViewport.width() / this.maxZoom;
        this.minViewportHeight = this.maxViewport.height() / this.maxZoom;
    }
}
