package dji.v5.ux.remotecontroller.calibration.stick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;


import java.util.Locale;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.core.ui.BaseView;


/**
 * Created by richard.liao on 2018/5/30:21:00
 */

public class HallStickCalibrationView extends BaseView {
    private final int FRONT_SIZE;
    private final int FRONT_WIDTH;
    private final int RECTANGLE_GAP;
    private final int CIRCLE_RADIUS;
    private final int RECTANGLE_LINE_WIDTH = 2;

    private static final int CIRCLE_COLOR = Color.parseColor("#1FA3F6");
    private static final int PROGRESS_STROKE_COLOR = Color.parseColor("#093049");

    private int[] progress = new int[4];

    private int left = 0;
    private int right = 0;
    private int top = 0;
    private int bottom = 0;

    private int segmentNum = 15;

    private int viewWidth;
    private int viewHeight;
    private int movementCircleX;
    private int movementCircleY;

    private int connerNumber;

    private float unitStrokeLength;
    private float topLeftStartX;
    private float topLeftStartY;
    private float topRightStartX;
    private float topRightStartY;
    private float bottomRightStartX;
    private float bottomRightStartY;
    private float bottomLeftStartX;
    private float bottomLeftStartY;

    private RectF outsideRectangle;
    private RectF insideRectangle;

    private Paint fontPaint;
    private Paint rectanglePainter;
    private Paint movementCirclePainter;
    private Paint progressStrokePainter;


    public HallStickCalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        FRONT_SIZE = AndUtil.dip2px(context, 10);
        FRONT_WIDTH = AndUtil.dip2px(context, 35);
        RECTANGLE_GAP = AndUtil.dip2px(context, 20);
        CIRCLE_RADIUS =  RECTANGLE_GAP / 2;

        init();
    }

    public void setProgress(int left, int top, int right, int bottom) {
        progress[0] = top;
        progress[1] = right;
        progress[2] = bottom;
        progress[3] = left;
        invalidate();
    }

    public void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    public void reset() {
        for (int i = 0; i < progress.length; i++) {
            progress[i] = 0;
        }
        invalidate();
    }

    public boolean hasSegNumSet() {
        return segmentNum != -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        DJILog.e("SegmentView", "=========================== Segment ========================== ");
//        DJILog.e("SegmentView", "Segment: leftTop = " + getBinaryString(progress[0]) + ", leftRight = " + getBinaryString(progress[1])
//                + ", leftBottom = " + getBinaryString(progress[2]) + ", leftLeft = " + getBinaryString(progress[3]));
//        DJILog.e("SegmentView", "-------------------------------------------------------------- ");

        // 绘制上边
        int currentProgress = progress[0];
        int factor = 1;
        for (int i = 0; i <= segmentNum - connerNumber; i++) {
            if (i > connerNumber && (currentProgress & factor) == factor) {
                if (i == 0) {
                    canvas.drawLine(topLeftStartX + RECTANGLE_GAP, topLeftStartY,
                            topLeftStartX + unitStrokeLength * i, topLeftStartY,
                            progressStrokePainter);
                } else {
                    canvas.drawLine(topLeftStartX + unitStrokeLength * (i - 1), topLeftStartY,
                            topLeftStartX + unitStrokeLength * i, topLeftStartY,
                            progressStrokePainter);
                }
            }
            factor = factor * 2;
        }
        // 绘制右边
        currentProgress = progress[1];
        factor = 1;
        for (int i = 0; i <= segmentNum - connerNumber; i++) {
            if (i > connerNumber && (currentProgress & factor) == factor) {
                if (i == 0) {
                    canvas.drawLine(topRightStartX, topRightStartY + RECTANGLE_GAP,
                            topRightStartX, topRightStartY + unitStrokeLength * i,
                            progressStrokePainter);
                } else {
                    canvas.drawLine(topRightStartX, topRightStartY + unitStrokeLength * (i - 1),
                            topRightStartX, topRightStartY + unitStrokeLength * i,
                            progressStrokePainter);
                }
            }
            factor = factor * 2;
        }
        // 绘制下边
        currentProgress = progress[2];
        factor = 1;
        for (int i = 0; i <= segmentNum - connerNumber; i++) {
            if (i > connerNumber && (currentProgress & factor) == factor) {
                if (i == 0) {
                    canvas.drawLine(bottomRightStartX - RECTANGLE_GAP, bottomRightStartY,
                            bottomRightStartX - unitStrokeLength * i, bottomRightStartY,
                            progressStrokePainter);
                } else {
                    canvas.drawLine(bottomRightStartX - unitStrokeLength * (i - 1), bottomRightStartY,
                            bottomRightStartX  - unitStrokeLength * i, bottomRightStartY,
                            progressStrokePainter);
                }
            }
            factor = factor * 2;
        }
        // 绘制左边
        currentProgress = progress[3];
        factor = 1;
        for (int i = 0; i <= segmentNum - connerNumber; i++) {
            if (i > connerNumber && (currentProgress & factor) == factor) {
                if (i == 0) {
                    canvas.drawLine(bottomLeftStartX, bottomLeftStartY - RECTANGLE_GAP,
                            bottomLeftStartX, bottomLeftStartY - unitStrokeLength * i,
                            progressStrokePainter);
                } else {
                    canvas.drawLine(bottomLeftStartX, bottomLeftStartY - unitStrokeLength * (i - 1),
                            bottomLeftStartX, bottomLeftStartY - unitStrokeLength * i,
                            progressStrokePainter);
                }
            }
            factor = factor * 2;
        }

        // 绘制左上转角
        if (checkCorner(0, HEAD) && checkCorner(3, TAIL)) {
            canvas.drawLine(topLeftStartX, topLeftStartY,
                    topLeftStartX + unitStrokeLength * connerNumber, topLeftStartY,
                    progressStrokePainter);
        } else {
//            DJILog.e("SegmentView", "Check Failed 左上 = ");
            getCorner(0, HEAD);
            getCorner(3, TAIL);
        }
        // 绘制右上转角
        if (checkCorner(1, HEAD) && checkCorner(0, TAIL)) {
            canvas.drawLine(topLeftStartX + unitStrokeLength * (segmentNum - connerNumber), topLeftStartY,
                    topLeftStartX + unitStrokeLength * segmentNum + 2 * RECTANGLE_LINE_WIDTH, topLeftStartY,
                    progressStrokePainter);
        } else {
//            DJILog.e("SegmentView", "Check Failed 右上 = ");
            getCorner(1, HEAD);
            getCorner(0, TAIL);
        }
        // 绘制左下转角
        if (checkCorner(3, HEAD) && checkCorner(2, TAIL)) {
            canvas.drawLine(bottomRightStartX - unitStrokeLength * (segmentNum - connerNumber), bottomRightStartY,
                    bottomRightStartX  - unitStrokeLength * segmentNum - 2 * RECTANGLE_LINE_WIDTH, bottomRightStartY,
                    progressStrokePainter);
        } else {
//            DJILog.e("SegmentView", "Check Failed 左下 = ");
            getCorner(3, HEAD);
            getCorner(2, TAIL);
        }
        // 绘制右下转角
        if (checkCorner(2, HEAD) && checkCorner(1, TAIL)) {
            canvas.drawLine(bottomRightStartX, bottomRightStartY,
                    bottomRightStartX - unitStrokeLength * connerNumber, bottomRightStartY,
                    progressStrokePainter);
        } else {
//            DJILog.e("SegmentView", "Check Failed 右下 = ");
            getCorner(2, HEAD);
            getCorner(1, TAIL);
        }

        canvas.drawRect(outsideRectangle, rectanglePainter);
        canvas.drawRect(insideRectangle, rectanglePainter);
        canvas.drawCircle(movementCircleX, movementCircleY, CIRCLE_RADIUS, movementCirclePainter);
//        DJILog.e("SegmentView", "=========================== Segment ========================== ");

        drawText(canvas);
    }

    private final int HEAD = 0;
    private final int TAIL = 1;
    private boolean checkCorner(int segment, int headOrTail) {
        int currentProgress = progress[segment];
//        DJILog.e("SegmentCorner", "corner " + getBinaryString(currentProgress) + " cornerNum = " + connerNumber);
        if (headOrTail == HEAD) {
            int factor = 1;
            for (int i = 0; i <= connerNumber; ++i) {
                if ((currentProgress & factor) != factor) {
                    return false;
                }
//                DJILog.e("SegmentCorner", "factor " + getBinaryString(factor));
                factor = factor << 1;
            }
            return true;

        } else if (headOrTail == TAIL) {
            int factor = (int) Math.pow( 2.00, (double) segmentNum - connerNumber - 1);
            for (int i = segmentNum - connerNumber; i < segmentNum; ++i) {
                if ((currentProgress & factor) != factor) {
                    return false;
                }
//                DJILog.e("SegmentCorner", "factor " + getBinaryString(factor));
                factor = factor << 1;
            }
            return true;
        }
        return false;
    }

    private int getCorner(int segment, int headOrTail) {
        int currentProgress = progress[segment];
        if (headOrTail == HEAD) {
            int factor = 1;
            for (int i = 0; i <= connerNumber; ++i) {
                if ((currentProgress & factor) != factor) {
//                    DJILog.e("SegmentView", "checkCorner " + segment + " head, " + i + " not passed");
                    return i;
                }
                factor = factor << 1;
            }
            return -1;

        } else if (headOrTail == TAIL) {
            int factor = (int) Math.pow(2, (double) segmentNum - connerNumber - 1);
            for (int i = segmentNum - connerNumber; i < segmentNum; ++i) {
                if ((currentProgress & factor) != factor) {
//                    DJILog.e("SegmentView", "checkCorner " + segment + " tail, " + i + " not passed");
                    return i;
                }
                factor = factor << 1;
            }
            return -1;
        }
        return -1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        viewWidth = w;
        viewHeight = h;
        movementCircleX = viewWidth / 2;
        movementCircleY = viewHeight / 2;

        unitStrokeLength = (float)(viewWidth - 2 * FRONT_WIDTH) / segmentNum;
        connerNumber = (int) (RECTANGLE_GAP / unitStrokeLength);

        topLeftStartX = FRONT_WIDTH + (float)RECTANGLE_LINE_WIDTH;
        topLeftStartY = FRONT_WIDTH + (float)RECTANGLE_GAP / 2 + RECTANGLE_LINE_WIDTH;
        topRightStartX = viewWidth - FRONT_WIDTH - (float)RECTANGLE_GAP / 2 - RECTANGLE_LINE_WIDTH;
        topRightStartY = FRONT_WIDTH + (float)RECTANGLE_LINE_WIDTH;
        bottomRightStartX = (float)viewWidth - FRONT_WIDTH - RECTANGLE_LINE_WIDTH;
        bottomRightStartY = viewHeight - FRONT_WIDTH - (float)RECTANGLE_GAP / 2 - RECTANGLE_LINE_WIDTH;
        bottomLeftStartX = FRONT_WIDTH + (float)RECTANGLE_GAP / 2 + RECTANGLE_LINE_WIDTH;
        bottomLeftStartY = (float)viewHeight - FRONT_WIDTH - RECTANGLE_LINE_WIDTH;

        setUpRectangle(FRONT_WIDTH, outsideRectangle);
        setUpRectangle(FRONT_WIDTH + RECTANGLE_GAP, insideRectangle);
    }

    private void init() {
        outsideRectangle = new RectF();
        insideRectangle = new RectF();

        rectanglePainter = new Paint();
        rectanglePainter.setAntiAlias(true);
        rectanglePainter.setDither(true);
        rectanglePainter.setColor(Color.WHITE);
        rectanglePainter.setStyle(Paint.Style.STROKE);
        rectanglePainter.setStrokeWidth(RECTANGLE_LINE_WIDTH);

        fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fontPaint.setStrokeWidth(0);
        fontPaint.setTextSize(FRONT_SIZE);
        fontPaint.setColor(Color.WHITE);
        fontPaint.setTextAlign(Paint.Align.CENTER);

        movementCirclePainter = new Paint();
        movementCirclePainter.setAntiAlias(true);
        movementCirclePainter.setDither(true);
        movementCirclePainter.setColor(Color.WHITE);
        movementCirclePainter.setStrokeWidth(10);

        progressStrokePainter = new Paint();
        progressStrokePainter.setAntiAlias(true);
        progressStrokePainter.setDither(true);
        progressStrokePainter.setColor(PROGRESS_STROKE_COLOR);
//        progressStrokePainter.setAlpha((int)(255 * 0.3));
        progressStrokePainter.setStyle(Paint.Style.STROKE);
        progressStrokePainter.setStrokeWidth(RECTANGLE_GAP);
    }

    private void switchCirclePainter(boolean isOnEdge) {
        if (isOnEdge) {
            movementCirclePainter = new Paint();
            movementCirclePainter.setAntiAlias(true);
            movementCirclePainter.setDither(true);
            movementCirclePainter.setColor(CIRCLE_COLOR);
            movementCirclePainter.setAlpha(255);
            movementCirclePainter.setStrokeWidth(5);
        } else {
            movementCirclePainter = new Paint();
            movementCirclePainter.setAntiAlias(true);
            movementCirclePainter.setDither(true);
            movementCirclePainter.setColor(Color.WHITE);
            movementCirclePainter.setStrokeWidth(5);
        }
        invalidate();
    }

    private void setUpRectangle(int offset, RectF target) {
        target.set(offset, offset, (float) viewWidth - offset, (float) viewHeight - offset);
    }

    private void drawText(Canvas canvas) {
        int w = getWidth();

        //左
        drawCenterText(canvas, left + "", new Rect(0, 0, FRONT_WIDTH, w));

        //右
        drawCenterText(canvas, right + "", new Rect(w - FRONT_WIDTH, 0, w, w));

        //上
        drawCenterText(canvas, top + "", new Rect(0, 0, w, FRONT_WIDTH));

        //下
        drawCenterText(canvas, bottom + "", new Rect(0, w - FRONT_WIDTH, w, w));
    }

    private void drawCenterText(Canvas canvas, String text, Rect rect) {
        Paint.FontMetricsInt fontMetrics = fontPaint.getFontMetricsInt();
        int baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
        canvas.drawText(text, rect.centerX(), baseline, fontPaint);
    }

    /**
     * 根据杆量判断圆点的位置和颜色
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setCircleCenter(int left, int top, int right, int bottom) {
        // 因为每个方向上都有正负两个100，所以是除以200
        movementCircleX = viewWidth / 2 - left * (viewWidth - 2 * FRONT_WIDTH - RECTANGLE_GAP) / 200 + right * (viewWidth - 2 * FRONT_WIDTH - RECTANGLE_GAP) / 200 ;
        movementCircleY = viewHeight / 2 - top * (viewHeight - 2 * FRONT_WIDTH - RECTANGLE_GAP) / 200 + bottom * (viewHeight  - 2 * FRONT_WIDTH - RECTANGLE_GAP) / 200;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        // 若某个杆量打满，把圆点改成蓝色实心
        if (bottom == 100 || top == 100 || right == 100 || left == 100) {
            switchCirclePainter(true);
        } else {
            switchCirclePainter(false);
        }
        invalidate();
    }

}
