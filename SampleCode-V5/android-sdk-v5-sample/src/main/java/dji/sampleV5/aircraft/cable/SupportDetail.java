package dji.sampleV5.aircraft.cable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.common.ComponentIndexType;

/**
 * 导线巡检功能支持详情。
 */
public class SupportDetail {
    /** 综合判断结果 */
    public final boolean isSupported;
    /** 飞控固件版本 */
    @Nullable public final String flyVer;
    /** 当前可用相机列表 */
    @NonNull public final List<CameraInfo> cameraInfoList;
    /** 当前用户选中的相机 index */
    @Nullable public final ComponentIndexType selectedCameraIndex;
    /** 当前地区码 */
    @Nullable public final String areaCode;

    public SupportDetail(boolean isSupported, @Nullable String flyVer,
                         @NonNull List<CameraInfo> cameraInfoList,
                         @Nullable ComponentIndexType selectedCameraIndex,
                         @Nullable String areaCode) {
        this.isSupported = isSupported;
        this.flyVer = flyVer;
        this.cameraInfoList = cameraInfoList;
        this.selectedCameraIndex = selectedCameraIndex;
        this.areaCode = areaCode;
    }
}
