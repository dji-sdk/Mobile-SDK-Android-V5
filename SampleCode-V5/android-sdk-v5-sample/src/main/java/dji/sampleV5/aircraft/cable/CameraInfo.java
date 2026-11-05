package dji.sampleV5.aircraft.cable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.common.ComponentIndexType;

/**
 * 单个相机的信息，通过 index 唯一标识一路相机。
 */
public class CameraInfo {
    @NonNull public final ComponentIndexType index;
    @Nullable public final String camVer;
    @Nullable public final String camType;

    public CameraInfo(@NonNull ComponentIndexType index, @Nullable String camVer, @Nullable String camType) {
        this.index = index;
        this.camVer = camVer;
        this.camType = camType;
    }
}
