package dji.v5.ux.core.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class RoundedLinearLayout extends LinearLayout {
    // private Paint drawPaint;
    // private Paint roundPaint;

    private int mCornerRadius = 10;
    private Paint mPaint;

    private RectF bounds = new RectF(0, 0, 1, 1);

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public RoundedLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        onInit();
    }

    public RoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        onInit();
    }

    public RoundedLinearLayout(Context context) {
        super(context);
        onInit();
    }

    protected void onInit() {
        // drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // drawPaint.setColor(0xffffffff);
        // drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //
        // roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // roundPaint.setColor(0xffffffff);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds = new RectF(0, 0, w, h);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int width = bounds.width() == 0 ? 1 : (int) bounds.width();
        int height = bounds.height() == 0 ? 1 : (int) bounds.height();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        super.dispatchDraw(c);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mPaint.setShader(shader);

        canvas.drawRoundRect(bounds, mCornerRadius, mCornerRadius, mPaint);
    }
}
