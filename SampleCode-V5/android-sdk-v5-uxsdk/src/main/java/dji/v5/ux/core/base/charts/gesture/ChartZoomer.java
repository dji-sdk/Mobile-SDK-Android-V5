package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.model.Viewport;

public class ChartZoomer {
    public static final float ZOOM_AMOUNT = 0.25F;
    private ZoomerCompat zoomer;
    private ZoomType zoomType;
    private PointF zoomFocalPoint = new PointF();
    private PointF viewportFocus = new PointF();
    private Viewport scrollerStartViewport = new Viewport();

    public ChartZoomer(Context context, ZoomType zoomType) {
        this.zoomer = new ZoomerCompat(context);
        this.zoomType = zoomType;
    }

    public boolean startZoom(MotionEvent e, ChartComputator computator) {
        this.zoomer.forceFinished(true);
        this.scrollerStartViewport.set(computator.getCurrentViewport());
        if (!computator.rawPixelsToDataPoint(e.getX(), e.getY(), this.zoomFocalPoint)) {
            return false;
        } else {
            this.zoomer.startZoom(0.25F);
            return true;
        }
    }

    public boolean computeZoom(ChartComputator computator) {
        if (this.zoomer.computeZoom()) {
            float newWidth = (1.0F - this.zoomer.getCurrZoom()) * this.scrollerStartViewport.width();
            float newHeight = (1.0F - this.zoomer.getCurrZoom()) * this.scrollerStartViewport.height();
            float pointWithinViewportX = (this.zoomFocalPoint.x - this.scrollerStartViewport.left) / this.scrollerStartViewport.width();
            float pointWithinViewportY = (this.zoomFocalPoint.y - this.scrollerStartViewport.bottom) / this.scrollerStartViewport.height();
            float left = this.zoomFocalPoint.x - newWidth * pointWithinViewportX;
            float top = this.zoomFocalPoint.y + newHeight * (1.0F - pointWithinViewportY);
            float right = this.zoomFocalPoint.x + newWidth * (1.0F - pointWithinViewportX);
            float bottom = this.zoomFocalPoint.y - newHeight * pointWithinViewportY;
            this.setCurrentViewport(computator, left, top, right, bottom);
            return true;
        } else {
            return false;
        }
    }

    public boolean scale(ChartComputator computator, float focusX, float focusY, float scale) {
        float newWidth = scale * computator.getCurrentViewport().width();
        float newHeight = scale * computator.getCurrentViewport().height();
        if (!computator.rawPixelsToDataPoint(focusX, focusY, this.viewportFocus)) {
            return false;
        } else {
            float left = this.viewportFocus.x - (focusX - (float)computator.getContentRectMinusAllMargins().left) * (newWidth / (float)computator.getContentRectMinusAllMargins().width());
            float top = this.viewportFocus.y + (focusY - (float)computator.getContentRectMinusAllMargins().top) * (newHeight / (float)computator.getContentRectMinusAllMargins().height());
            float right = left + newWidth;
            float bottom = top - newHeight;
            this.setCurrentViewport(computator, left, top, right, bottom);
            return true;
        }
    }

    private void setCurrentViewport(ChartComputator computator, float left, float top, float right, float bottom) {
        Viewport currentViewport = computator.getCurrentViewport();
        if (ZoomType.HORIZONTAL_AND_VERTICAL == this.zoomType) {
            computator.setCurrentViewport(left, top, right, bottom);
        } else if (ZoomType.HORIZONTAL == this.zoomType) {
            computator.setCurrentViewport(left, currentViewport.top, right, currentViewport.bottom);
        } else if (ZoomType.VERTICAL == this.zoomType) {
            computator.setCurrentViewport(currentViewport.left, top, currentViewport.right, bottom);
        }

    }

    public ZoomType getZoomType() {
        return this.zoomType;
    }

    public void setZoomType(ZoomType zoomType) {
        this.zoomType = zoomType;
    }
}

