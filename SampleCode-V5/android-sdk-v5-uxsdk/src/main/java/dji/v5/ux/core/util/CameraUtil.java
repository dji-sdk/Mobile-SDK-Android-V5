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

package dji.v5.ux.core.util;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.value.camera.CameraAperture;
import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.camera.CameraFlatMode;
import dji.sdk.keyvalue.value.camera.CameraISO;
import dji.sdk.keyvalue.value.camera.CameraShutterSpeed;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.PhotoFileFormat;
import dji.sdk.keyvalue.value.camera.VideoFrameRate;
import dji.sdk.keyvalue.value.camera.VideoResolution;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.payload.PayloadCameraType;
import dji.v5.ux.R;

/**
 * Utility class for displaying camera information.
 */
public final class CameraUtil {

    private static final String SHUTTER_SUBSTITUENT1 = "SHUTTER_SPEED1_";
    private static final String SHUTTER_SUPPLANTER1 = "";

    private static final String SHUTTER_WIFI_REGEX_2 = "(\\d+)_DOT_(\\d+)";
    private static final String SHUTTER_WIFI_REPLACE_2 = "$1.$2";

    private static final String SHUTTER_SUBSTITUENT3 = "SHUTTER_SPEED_([0-9.]+)";
    private static final String SHUTTER_SUPPLANTER3 = "$1\"";

    private static final String SHUTTER_SUBSTITUENT4 = "DOT_(\\d+)";
    private static final String SHUTTER_SUPPLANTER4 = "1.$1\"";

    // Convert the  N_5_0 to -5.0, N_0_0 to 0, P_1_0 to +1.0
    private static final String EV_SUBSTITUENT1 = "EV";
    private static final String EV_SUPPLANTER1 = "";

    private static final String EV_SUBSTITUENT2 = "NEG_";
    private static final String EV_SUPPLANTER2 = "-";

    private static final String EV_SUBSTITUENT3 = "POS_";
    private static final String EV_SUPPLANTER3 = "+";

    private static final String EV_SUBSTITUENT4 = "P";
    private static final String EV_SUPPLANTER4 = ".";

    private static final String DEFAULT_PAYLOAD_NAME = "Payload Camera";

    private final static Map<CameraType, String> CAMERA_TYPE_STRING_MAP = new HashMap<CameraType, String>();

    static {
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_XT, "XT");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_X30, "X30");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_XT2, "XT2");
        CAMERA_TYPE_STRING_MAP.put(CameraType.PAYLOAD, DEFAULT_PAYLOAD_NAME);
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_XTS, "XT S");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_H20, "H20");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_H20T, "H20T");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_H20N, "H20N");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M200_V2_CAMERA, "FPV");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M30, "M30");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M30T, "M30T");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M3E, "M3E");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M3T, "M3T");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M3TA, "M3TA");
        CAMERA_TYPE_STRING_MAP.put(CameraType.M3M, "M3M");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_L1, "L1");
        CAMERA_TYPE_STRING_MAP.put(CameraType.ZENMUSE_P1, "P1");
    }

    private CameraUtil() {
        // prevent instantiation of util class
    }

    @NonNull
    public static String resolutionShortDisplayName(@NonNull VideoResolution resolution) {

        String shortName;

        switch (resolution) {
            case RESOLUTION_336x256:
                shortName = "256P";
                break;
            case RESOLUTION_640x480:
                shortName = "480P";
                break;
            case RESOLUTION_640x512:
                shortName = "512P";
                break;
            case RESOLUTION_1280x720:
                shortName = "720P";
                break;
            case RESOLUTION_1920x1080:
                shortName = "1080P";
                break;
            case RESOLUTION_2048x1080:
                shortName = "2K";
                break;
            case RESOLUTION_2688x1512:
            case RESOLUTION_2704x1520:
            case RESOLUTION_2720x1530:
                shortName = "2.7K";
                break;
            case RESOLUTION_3840x1572:
            case RESOLUTION_3840x2160:
            case RESOLUTION_4096x2160:
                shortName = "4K";
                break;
            case RESOLUTION_4608x2160:
            case RESOLUTION_4608x2592:
                shortName = "4.5K";
                break;
            case RESOLUTION_5280x2160:
            case RESOLUTION_5280x2972:
                shortName = "5K";
                break;
            case RESOLUTION_5760x3240:
            case RESOLUTION_6016x3200:
                shortName = "6K";
                break;
            default:
                shortName = "Unknown";
        }

        return shortName;
    }

    @NonNull
    public static String frameRateDisplayName(@NonNull VideoFrameRate frameRate) {
        final String originalFrameRateString = frameRate.toString();
        String processedFrameRateString;
        Matcher matcher = Pattern.compile("RATE_(\\d{2,3})DOT_.*").matcher(originalFrameRateString);
        if (matcher.find()) {
            String tempRate = matcher.group(1);
            processedFrameRateString = Integer.toString(Integer.parseInt(tempRate) + 1);
        } else {
            matcher = Pattern.compile("RATE_(\\d{2,3})FPS").matcher(originalFrameRateString);
            if (matcher.find()) {
                processedFrameRateString = matcher.group(1);
            } else {
                processedFrameRateString = "Null";
            }
        }

        return processedFrameRateString;
    }

    @NonNull
    public static String convertPhotoFileFormatToString(@NonNull Resources resources, @NonNull PhotoFileFormat photoFileFormat) {
        String formatString;
        if (photoFileFormat.value() == PhotoFileFormat.RAW.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_raw);
        } else if (photoFileFormat.value() == PhotoFileFormat.JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_jpeg);
        } else if (photoFileFormat.value() == PhotoFileFormat.RAW_JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_jpegraw);
        } else if (photoFileFormat.value() == PhotoFileFormat.TIFF_14_BIT.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_tiff);
        } else if (photoFileFormat.value() == PhotoFileFormat.RADIOMETRIC_JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_radiometic_jpeg);
        } else if (photoFileFormat.value() == PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_low_tiff);
        } else if (photoFileFormat.value() == PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_high_tiff);
        } else {
            formatString = photoFileFormat.toString();
        }

        return formatString;
    }

    @NonNull
    public static String formatVideoTime(@NonNull Resources resources, int flyTime) {
        String result;
        final int[] time = UnitConversionUtil.formatSecondToHour(flyTime);
        if (time[2] > 0) {
            result = resources.getString(R.string.uxsdk_video_time_hours, time[2], time[1], time[0]);
        } else {
            result = resources.getString(R.string.uxsdk_video_time, time[1], time[0]);
        }
        return result;

    }


    /**
     * Convert the Aperture enum name to the string to show on the UI.
     *
     * @param resources A resources object.
     * @param aperture  The aperture value to convert.
     * @return A String to display.
     */
    public static String apertureDisplayName(@NonNull Resources resources, @NonNull CameraAperture aperture) {
        String displayName;
        if (aperture == CameraAperture.UNKNOWN) {
            displayName = resources.getString(R.string.uxsdk_string_default_value);
        } else {
            // Convert the F_1p7 to 1.7, F_4p0 to 4, F_4p5 to 4.5
            int apertureValue = aperture.value();
            int apertureInteger = apertureValue / 100;
            // Just keep one decimal number, so divide by 10.
            int apertureDecimal = apertureValue % 100 / 10;
            if (apertureDecimal == 0) {
                displayName = String.format(Locale.US, "%d", apertureInteger);
            } else {
                displayName = String.format(Locale.US, "%d.%d", apertureInteger, apertureDecimal);
            }
        }

        return displayName;
    }

    /**
     * Convert the Shutter Speed enum name to the string to show on the UI.
     *
     * @param shutterSpeed The shutter speed value to convert.
     * @return A String to display.
     */
    @NonNull
    public static String shutterSpeedDisplayName(@NonNull CameraShutterSpeed shutterSpeed) {

        String shutterName = shutterSpeed.name();
        shutterName = shutterName.replace(SHUTTER_SUBSTITUENT1, SHUTTER_SUPPLANTER1).
                replace(SHUTTER_WIFI_REGEX_2, SHUTTER_WIFI_REPLACE_2).
                replace(SHUTTER_SUBSTITUENT3, SHUTTER_SUPPLANTER3).
                replace(SHUTTER_SUBSTITUENT4, SHUTTER_SUPPLANTER4);

        return shutterName;
    }

    /**
     * Convert the Exposure Compensation enum name to the string to show on the UI.
     *
     * @param ev The exposure compensation value to convert.
     * @return A String to display.
     */
    @NonNull
    public static String exposureValueDisplayName(@NonNull CameraExposureCompensation ev) {
        if (ev == CameraExposureCompensation.NEG_0EV) {
            return "0";
        }
        String enumName = ev.name();
        return enumName.replace(EV_SUBSTITUENT1, EV_SUPPLANTER1).
                replace(EV_SUBSTITUENT2, EV_SUPPLANTER2).
                replace(EV_SUBSTITUENT3, EV_SUPPLANTER3).
                replace(EV_SUBSTITUENT4, EV_SUPPLANTER4);
    }

    public static int convertISOToInt(CameraISO iso) {
        if (iso == CameraISO.ISO_AUTO || iso == CameraISO.UNKNOWN) {
            return 0;
        }

        String name = iso.toString();
        String[] isoValue = name.split("_");
        return Integer.parseInt(isoValue[1]);
    }

    @NonNull
    public static CameraISO convertIntToISO(int isoValue) {
        if (isoValue > 0 && isoValue < 200) {
            return CameraISO.ISO_100;
        } else if (isoValue >= 200 && isoValue < 400) {
            return CameraISO.ISO_200;
        } else if (isoValue >= 400 && isoValue < 800) {
            return CameraISO.ISO_400;
        } else if (isoValue >= 800 && isoValue < 1600) {
            return CameraISO.ISO_800;
        } else {
            return convertInt2ISO(isoValue);
        }
    }

    private static CameraISO convertInt2ISO(int isoValue) {
        if (isoValue >= 1600 && isoValue < 3200) {
            return CameraISO.ISO_1600;
        } else if (isoValue >= 3200 && isoValue < 6400) {
            return CameraISO.ISO_3200;
        } else if (isoValue >= 6400 && isoValue < 12800) {
            return CameraISO.ISO_6400;
        } else if (isoValue >= 12800 && isoValue < 25600) {
            return CameraISO.ISO_12800;
        } else if (isoValue >= 25600) {
            return CameraISO.ISO_25600;
        } else {
            return CameraISO.UNKNOWN;
        }
    }

    /**
     * Get the lens index based on the given stream source and camera name.
     *
     * @param streamSource The streamSource
     * @return The lens index
     */
    public static CameraLensType getLensIndex(CameraVideoStreamSourceType streamSource) {
        if (streamSource == CameraVideoStreamSourceType.WIDE_CAMERA) {
            return CameraLensType.CAMERA_LENS_WIDE;
        } else if (streamSource == CameraVideoStreamSourceType.INFRARED_CAMERA) {
            return CameraLensType.CAMERA_LENS_THERMAL;
        } else {
            return CameraLensType.CAMERA_LENS_ZOOM;
        }
    }

    public static boolean isPictureMode(CameraFlatMode flatCameraMode) {
        return flatCameraMode == CameraFlatMode.VIDEO_TIMELAPSE
                || flatCameraMode == CameraFlatMode.PHOTO_AEB
                || flatCameraMode == CameraFlatMode.PHOTO_NORMAL
                || flatCameraMode == CameraFlatMode.PHOTO_BURST
                || flatCameraMode == CameraFlatMode.PHOTO_HDR
                || flatCameraMode == CameraFlatMode.PHOTO_INTERVAL
                || flatCameraMode == CameraFlatMode.PHOTO_HYPERLIGHT
                || flatCameraMode == CameraFlatMode.PHOTO_PANO
                || flatCameraMode == CameraFlatMode.PHOTO_EHDR;
    }

    public static boolean isAutoISOSupportedByProduct() {
        return true;
//        return (!ProductUtil.isMavicAir()) && (!ProductUtil.isMavicPro() && (!ProductUtil.isMavicMini()));
    }

    public static boolean isSupportForNDVI(CameraLensType lensType) {
        return lensType == CameraLensType.CAMERA_LENS_MS_G ||
                lensType == CameraLensType.CAMERA_LENS_MS_R ||
                lensType == CameraLensType.CAMERA_LENS_MS_RE ||
                lensType == CameraLensType.CAMERA_LENS_MS_NIR ||
                lensType == CameraLensType.CAMERA_LENS_MS_NDVI;
    }

    public static boolean isFPVTypeView(ComponentIndexType devicePosition) {
        return devicePosition == ComponentIndexType.FPV || devicePosition == ComponentIndexType.VISION_ASSIST;
    }

    public static String getCameraDisplayName(CameraType cameraType) {
        String name = CAMERA_TYPE_STRING_MAP.get(cameraType);
        if (name == null) {
            name = "";
        }
        return name;
    }

    public static String getPayloadCameraName(PayloadCameraType payloadCameraType) {
        String name = "UNKNOWN";
        switch (payloadCameraType) {
            case EP600:
                name = "P1";
                break;
            case EP800:
                name = "L1";
                break;
            default:
                break;
        }

        return name;
    }

    public static List<ComponentIndexType> getConnectionCameraList(
            boolean cameraConnection1,
            boolean cameraConnection2,
            boolean cameraConnection3
    ) {
        List<ComponentIndexType> list = new ArrayList<>();
        if (cameraConnection1) {
            list.add(ComponentIndexType.LEFT_OR_MAIN);
        }
        if (cameraConnection2) {
            list.add(ComponentIndexType.RIGHT);
        }
        if (cameraConnection3) {
            list.add(ComponentIndexType.UP);
        }
        return list;
    }

}
