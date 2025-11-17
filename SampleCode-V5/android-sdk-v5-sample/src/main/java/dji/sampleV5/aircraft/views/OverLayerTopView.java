package dji.sampleV5.aircraft.views;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatImageView;
import dji.sampleV5.aircraft.models.LookAtVM;
import dji.sdk.keyvalue.value.common.DoubleRect;
import dji.v5.manager.datacenter.camera.view.PinPoint;
import dji.v5.manager.intelligent.AutoSensingTarget;

public class OverLayerTopView extends AppCompatImageView {
    private Paint rectBorderPaint;
    private Paint pointPaint;
    private TextPaint wordPaint;
    private int screenWidth, screenHeight;
    private final StringBuilder buffer = new StringBuilder();
    private List<LookAtVM.Point> points = new ArrayList<>();

    private DoubleRect selectBound;
    private List<AutoSensingTarget> currentTargetInfo;
    private final RectF rect = new RectF();

    public OverLayerTopView(Context context) {
        this(context, null, 0);
    }

    public OverLayerTopView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverLayerTopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化控件及参数
     */
    private void init() {
        initPaint();
        post(() -> {
            screenWidth = getWidth();
            screenHeight = getHeight();
        });
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        rectBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectBorderPaint.setColor(Color.GREEN);
        rectBorderPaint.setStyle(Paint.Style.STROKE);
        rectBorderPaint.setStrokeWidth(5f);
        rectBorderPaint.setAlpha(255);//透明度

        wordPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);//抗锯齿
        wordPaint.setColor(Color.YELLOW);//字体颜色
        wordPaint.setStrokeWidth(1f);//画笔的宽度
        wordPaint.setTextSize(25);//字体大小

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.BLUE);
        pointPaint.setStrokeWidth(40F);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (points != null) {
            for (LookAtVM.Point point : points) {
                if (point.getPinPointInfo().isValid()) {
                    for (PinPoint pinPoint : point.getPinPointInfo().getPinPoints()) {
                        drawPoint(canvas, (float) pinPoint.getX(), (float) pinPoint.getY());
                        drawTipText(canvas, initPointInfo(point), (float) pinPoint.getX() * screenWidth, (float) pinPoint.getY() * screenHeight);
                    }
                }
            }
        }
        if (selectBound != null) {
            drawBound(canvas, selectBound);
        }
        if (currentTargetInfo != null) {
            for (AutoSensingTarget targetInfo : currentTargetInfo) {
                DoubleRect boundInfo = targetInfo.getRect();
                if (boundInfo == null) {
                    continue;
                }
                drawBound(canvas, boundInfo);
                drawTipText(canvas, initTargetInfo(targetInfo), rect.centerX(), rect.centerY());
            }
            super.onDraw(canvas);
        }
        super.onDraw(canvas);
    }

    private String initPointInfo(LookAtVM.Point point) {
        buffer.setLength(0);
        buffer.append("Lng:").append(point.getPos().getLongitude()).append(" ");
        buffer.append("Lat:").append(point.getPos().getLatitude()).append(" ");
        buffer.append("Alt:").append(point.getPos().getAltitude()).append("\n");
        buffer.append("Direction:").append(point.getPinPointInfo().getPointDirection()).append("\n");
        buffer.append("ComponentIndexType:").append(point.getComponentIndexType());
        return buffer.toString();
    }

    private void drawPoint(Canvas canvas, float x, float y) {
        canvas.drawPoint(x * screenWidth, y * screenHeight, pointPaint);
    }

    private void drawBound(Canvas canvas, DoubleRect boundInfo) {
        rect.left = (float) (boundInfo.getX() - boundInfo.getWidth() / 2) * screenWidth;
        rect.right = (float) (boundInfo.getX() + boundInfo.getWidth() / 2) * screenWidth;
        rect.top = (float) (boundInfo.getY() - boundInfo.getHeight() / 2) * screenHeight;
        rect.bottom = (float) (boundInfo.getY() + boundInfo.getHeight() / 2) * screenHeight;
        canvas.drawRect(rect, rectBorderPaint);
    }

    private void drawTipText(Canvas canvas, String str, float x, float y) {
        StaticLayout sy = new StaticLayout(str, wordPaint, 800,
                Layout.Alignment.ALIGN_CENTER, 1, 0, true);
        canvas.save();
        canvas.translate(-sy.getWidth() / 2F + x, -sy.getHeight() / 2F + y);
        sy.draw(canvas);
        canvas.restore();
    }

    private String initTargetInfo(AutoSensingTarget info) {
        buffer.setLength(0);
        buffer.append("Id:").append(info.getTargetIndex()).append("\n");
        buffer.append(info.getLabel());
        return buffer.toString();
    }

    public void onPointsChanged(List<LookAtVM.Point> pinPoints) {
        if (points != null) {
            this.points = pinPoints;
        }
        invalidate();
    }

    public void onTargetChange(List<AutoSensingTarget> targetInfo) {
        currentTargetInfo = targetInfo;
        invalidate();
    }

    public void onSelectBoundInfo(DoubleRect info) {
        selectBound = info;
        invalidate();
    }
}
