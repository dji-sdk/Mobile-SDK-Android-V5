package dji.v5.ux.visualcamera.zoom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class FocalZoomWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {

    private FocalZoomWidgetView focalZoomWidgetView;

    public FocalZoomWidget(Context context) {
        super(context);
    }

    public FocalZoomWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocalZoomWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        int widthMeasureSpec = (int) getResources().getDimension(R.dimen.uxsdk_42_dp);
        int heightMeasureSpec = (int) getResources().getDimension(R.dimen.uxsdk_250_dp);
        focalZoomWidgetView = new FocalZoomWidgetView(context, attrs, defStyleAttr);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(widthMeasureSpec, heightMeasureSpec);
        this.addView(focalZoomWidgetView, layoutParams);
    }

    @NonNull
    @Override
    public ComponentIndexType getCameraIndex() {
        if (focalZoomWidgetView != null) {
            return focalZoomWidgetView.getCameraIndex();
        }
        return ComponentIndexType.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraLensType getLensType() {
        if (focalZoomWidgetView != null) {
            return focalZoomWidgetView.getLensType();
        }
        return CameraLensType.UNKNOWN;
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        if (showZoomView(lensType)) {
            focalZoomWidgetView.setVisibility(VISIBLE);
            focalZoomWidgetView.updateCameraSource(cameraIndex, lensType);
            return;
        }
        focalZoomWidgetView.setVisibility(GONE);
    }

    @Override
    protected void reactToModelChanges() {
        //do nothing
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
    }

    private boolean showZoomView(CameraLensType lensType) {
        return lensType == CameraLensType.CAMERA_LENS_ZOOM || lensType == CameraLensType.CAMERA_LENS_THERMAL;
    }
}
