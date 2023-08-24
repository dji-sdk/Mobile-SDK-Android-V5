package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;

import androidx.core.widget.ScrollerCompat;

import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.model.Viewport;

public class ChartScroller {
    private Viewport scrollerStartViewport = new Viewport();
    private Point surfaceSizeBuffer = new Point();
    private ScrollerCompat scroller;

    public ChartScroller(Context context) {
        this.scroller = ScrollerCompat.create(context);
    }

    public boolean startScroll(ChartComputator computator) {
        this.scroller.abortAnimation();
        this.scrollerStartViewport.set(computator.getCurrentViewport());
        return true;
    }

    public boolean scroll(ChartComputator computator, float distanceX, float distanceY, ScrollResult scrollResult) {
        Viewport maxViewport = computator.getMaximumViewport();
        Viewport visibleViewport = computator.getVisibleViewport();
        Viewport currentViewport = computator.getCurrentViewport();
        Rect contentRect = computator.getContentRectMinusAllMargins();
        boolean canScrollLeft = currentViewport.left > maxViewport.left;
        boolean canScrollRight = currentViewport.right < maxViewport.right;
        boolean canScrollTop = currentViewport.top < maxViewport.top;
        boolean canScrollBottom = currentViewport.bottom > maxViewport.bottom;
        boolean canScrollX = false;
        boolean canScrollY = false;
        if ((canScrollLeft && distanceX <= 0.0F) || (canScrollRight && distanceX >= 0.0F)) {
            canScrollX = true;
        }

        if ((canScrollTop && distanceY <= 0.0F) || (canScrollBottom && distanceY >= 0.0)) {
            canScrollY = true;
        }
        if (canScrollX || canScrollY) {
            computator.computeScrollSurfaceSize(this.surfaceSizeBuffer);
            float viewportOffsetX = distanceX * visibleViewport.width() / contentRect.width();
            float viewportOffsetY = -distanceY * visibleViewport.height() / contentRect.height();
            computator.setViewportTopLeft(currentViewport.left + viewportOffsetX, currentViewport.top + viewportOffsetY);
        }

        scrollResult.canScrollX = canScrollX;
        scrollResult.canScrollY = canScrollY;
        return canScrollX || canScrollY;
    }

    public boolean computeScrollOffset(ChartComputator computator) {
        if (this.scroller.computeScrollOffset()) {
            Viewport maxViewport = computator.getMaximumViewport();
            computator.computeScrollSurfaceSize(this.surfaceSizeBuffer);
            float currXRange = maxViewport.left + maxViewport.width() * this.scroller.getCurrX() / this.surfaceSizeBuffer.x;
            float currYRange = maxViewport.top - maxViewport.height() * this.scroller.getCurrY() / this.surfaceSizeBuffer.y;
            computator.setViewportTopLeft(currXRange, currYRange);
            return true;
        } else {
            return false;
        }
    }

    public boolean fling(int velocityX, int velocityY, ChartComputator computator) {
        computator.computeScrollSurfaceSize(this.surfaceSizeBuffer);
        this.scrollerStartViewport.set(computator.getCurrentViewport());
        int startX = (int)(this.surfaceSizeBuffer.x * (this.scrollerStartViewport.left - computator.getMaximumViewport().left) / computator.getMaximumViewport().width());
        int startY = (int)(this.surfaceSizeBuffer.y * (computator.getMaximumViewport().top - this.scrollerStartViewport.top) / computator.getMaximumViewport().height());
        this.scroller.abortAnimation();
        int width = computator.getContentRectMinusAllMargins().width();
        int height = computator.getContentRectMinusAllMargins().height();
        this.scroller.fling(startX, startY, velocityX, velocityY, 0, this.surfaceSizeBuffer.x - width + 1, 0, this.surfaceSizeBuffer.y - height + 1);
        return true;
    }

    public static class ScrollResult {
        public boolean canScrollX;
        public boolean canScrollY;

        public ScrollResult() {
            //do nothing
        }
    }
}

