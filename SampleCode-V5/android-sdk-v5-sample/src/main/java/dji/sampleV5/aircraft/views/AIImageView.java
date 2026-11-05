package dji.sampleV5.aircraft.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.datacenter.media.MediaManager;
import dji.v5.manager.datacenter.media.PhotoAIInfo;
import dji.v5.manager.intelligent.AutoSensingTarget;
import dji.v5.manager.intelligent.media.AIMediaHelper;
import dji.v5.utils.common.LogUtils;

public class AIImageView extends AppCompatImageView {
    private static String TAG = "AIImageView";

    private Paint rectPaint;
    private Bitmap toMap;
    public AIImageView(Context context) {
        super(context);
        init();
    }

    public AIImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AIImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4);
    }

    public void setMedia(String imgPath) {
        if (imgPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
            try {
                MediaManager.getInstance().parsePhotoAIInfo(imgPath, new CommonCallbacks.CompletionCallbackWithParam<PhotoAIInfo>() {
                    @Override
                    public void onSuccess(@NonNull PhotoAIInfo photoAIInfos) {
                        if (bitmap != null) {
                            toMap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(toMap);

                            List<AutoSensingTarget> TargetBoxes = photoAIInfos.getAutoSensingInfos();
                            for (AutoSensingTarget targetBox : TargetBoxes) {
                                if (targetBox.getRect() != null) {
                                    drawAiObject(targetBox, canvas);
                                }
                            }
                            setImageBitmap(toMap);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtils.i(TAG, "parsePhotoAIInfo fail, errorMsg " + error.errorCode());
                        setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                LogUtils.e(TAG, "parsePhotoAIInfo exception: " + e.getMessage());
                // 解析异常时至少显示原图，避免崩溃
                if (bitmap != null) {
                    setImageBitmap(bitmap);
                }
            }
        }
    }

    private void drawAiObject(AutoSensingTarget tagetBox, Canvas canvas) {
        if (tagetBox == null || canvas == null) {
            return;
        }
        RectF targetRectf = AIMediaHelper.getTargetRect(tagetBox, canvas);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.rgb(0, 233, 213));
        paint.setAntiAlias(true);
        canvas.drawRect(targetRectf, paint);

        String objectName =  tagetBox.getLabel();
        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(255, 191, 0));
        textPaint.setTextSize(80);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textX = targetRectf.centerX();
        float textY = targetRectf.top - 10;

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textTop = textY + fm.ascent;
        if (textTop < 0) {
            textY = targetRectf.bottom+4 - fm.ascent;
        }

        canvas.drawText(objectName, textX, textY, textPaint);
    }
}