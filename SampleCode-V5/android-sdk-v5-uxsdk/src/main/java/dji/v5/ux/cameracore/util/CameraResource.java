/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.cameracore.util;

import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;
import dji.sdk.keyvalue.value.camera.PhotoPanoramaMode;
import dji.v5.ux.R;

public class CameraResource {

    public static int getPhotoModeImgResId(final int mode, final int value) {
        if (CameraShootPhotoMode.HDR.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_hdr;
        } else if (CameraShootPhotoMode.BURST.value() == mode) {
            return getImgResIdFromBurstMode(value);
        } else if (CameraShootPhotoMode.RAW_BURST.value() == mode) {
            return getImgResIdFromRawBurstMode(value);
        } else if (CameraShootPhotoMode.AEB.value() == mode) {
            return getImgResIdFromAebMode(value);
        } else if (CameraShootPhotoMode.INTERVAL.value() == mode) {
            return getImgResIdFromIntervalMode(value);
        } else if (CameraShootPhotoMode.VISION_BOKEH.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_shallow_focus;
        } else if (CameraShootPhotoMode.VISION_PANO.value() == mode) {
            return getImgResIdFromVisionPanoMode(value);
        } else if (CameraShootPhotoMode.EHDR.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        } else if (CameraShootPhotoMode.HYPER_LIGHT.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        }
    }

    private static int getImgResIdFromBurstMode(int value) {
        if (value == 14) {
            return R.drawable.uxsdk_ic_photo_mode_continuous_14;
        } else if (value == 10) {
            return R.drawable.uxsdk_ic_photo_mode_continuous_10;
        } else if (value == 7) {
            return R.drawable.uxsdk_ic_photo_mode_continuous_7;
        } else if (value == 5) {
            return R.drawable.uxsdk_ic_photo_mode_continuous_5;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_continuous_3;
        }
    }

    private static int getImgResIdFromRawBurstMode(int value) {
        if (value == 255) {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_infinity;
        } else if (value == 14) {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_14;
        } else if (value == 10) {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_10;
        } else if (value == 7) {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_7;
        } else if (value == 5) {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_5;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_raw_burst_3;
        }
    }

    private static int getImgResIdFromAebMode(int value) {
        if (value == 7) {
            return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_7;
        } else if (value == 5) {
            return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_5;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_3;
        }
    }

    private static int getImgResIdFromIntervalMode(int value) {
        if (value == 60) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_60s;
        } else if (value == 30) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_30s;
        } else if (value == 20) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_20s;
        } else if (value == 15) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_15s;
        } else if (value == 10) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_10s;
        } else if (value == 7) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_7s;
        } else if (value == 4) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_4s;
        } else if (value == 3) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_3s;
        } else if (value == 2) {
            return R.drawable.uxsdk_ic_photo_mode_timepause_2s;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_timepause_5s;
        }
    }

    private static int getImgResIdFromVisionPanoMode(int value) {
        if (PhotoPanoramaMode.MODE_3x1.value() == value) {
            return R.drawable.uxsdk_ic_photo_mode_pano_3x1;
        } else if (PhotoPanoramaMode.MODE_1x3.value() == value) {
            return R.drawable.uxsdk_ic_photo_mode_pano_180;
        } else if (PhotoPanoramaMode.MODE_3x3.value() == value) {
            return R.drawable.uxsdk_ic_photo_mode_pano_3x3;
        } else if (PhotoPanoramaMode.MODE_SUPER_RESOLUTION.value() == value) {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        } else if (PhotoPanoramaMode.MODE_SPHERE.value() == value) {
            return R.drawable.uxsdk_ic_photo_mode_pano_sphere;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_pano_180;
        }
    }

    private CameraResource() {

    }
}
