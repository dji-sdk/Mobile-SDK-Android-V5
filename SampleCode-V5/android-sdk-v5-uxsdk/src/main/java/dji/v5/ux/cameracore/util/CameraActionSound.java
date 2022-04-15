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

import android.content.Context;

import androidx.annotation.NonNull;
import dji.v5.ux.R;
import dji.v5.ux.core.util.AudioUtil;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Class used as a util for playing sounds of
 * capture photo and record video
 */
public class CameraActionSound {
    private final Context context;
    private ShutterSoundCount shutterCount;

    public CameraActionSound(Context con) {
        context = con;
    }

    private int shutterCountSound(ShutterSoundCount value) {
        int soundId;
        switch (value) {
            case ONE:
                soundId = R.raw.uxsdk_shutter_1;
                break;
            case THREE:
                soundId = R.raw.uxsdk_shutter_3;
                break;
            case FIVE:
                soundId = R.raw.uxsdk_shutter_5;
                break;
            case SEVEN:
                soundId = R.raw.uxsdk_shutter_7;
                break;
            case TEN:
                soundId = R.raw.uxsdk_shutter_10;
                break;
            case FOURTEEN:
                soundId = R.raw.uxsdk_shutter_14;
                break;

            case UNKNOWN:
            default:
                soundId = R.raw.uxsdk_shutter_3;
                break;
        }
        return soundId;
    }

    /**
     * Set the count of photos based on current mode
     *
     * @param count int value
     */
    public void setShutterCount(@NonNull ShutterSoundCount count) {
        shutterCount = count;
    }

    /**
     * Play sound for Capture Photo
     *
     * @return Disposable
     */
    @NonNull
    public Disposable playCapturePhoto() {
        return AudioUtil.playSoundInBackground(context, shutterCountSound(shutterCount));

    }

    /**
     * Play sound for start record video
     *
     * @return Disposable
     */
    @NonNull
    public Disposable playStartRecordVideo() {
        return AudioUtil.playSoundInBackground(context, R.raw.uxsdk_video_voice);
    }

    /**
     * Play sound for stop record video
     *
     * @return Disposable
     */
    @NonNull
    public Disposable playStopRecordVideo() {
        return AudioUtil.playSoundInBackground(context, R.raw.uxsdk_end_video_record);
    }

    /**
     * Enum for shutter sound count
     */
    public enum ShutterSoundCount {
        /**
         * Single photo shutter sound
         */
        ONE(1),
        /**
         * Three photos shutter sound
         */
        THREE(3),
        /**
         * Five photos shutter sound
         */
        FIVE(5),
        /**
         * Seven photos shutter sound
         */
        SEVEN(7),
        /**
         * Ten photos shutter sound
         */
        TEN(10),
        /**
         * Fourteen photos shutter sound
         */
        FOURTEEN(14),
        /**
         * Will default to one shutter sound
         */
        UNKNOWN(1);

        private int value;

        ShutterSoundCount(int value) {
            this.value = value;
        }

        /**
         * Returns the enum constant of this type with the input integer value.
         *
         * @param value The input integer value
         * @return The enum constant of this type
         */
        public static ShutterSoundCount find(int value) {
            ShutterSoundCount result = UNKNOWN;
            for (int i = 0; i < getValues().length; i++) {
                if (getValues()[i]._equals(value)) {
                    result = getValues()[i];
                    break;
                }
            }
            return result;
        }

        /**
         * Returns the real value of an enum value.
         *
         * @return integer The real value.
         */
        public int value() {
            return this.value;
        }

        /**
         * Compares the input integer value with the real value of an enum value.
         *
         * @param b The input integer value.
         * @return boolean The compared result.
         */
        private boolean _equals(int b) {
            return value == b;
        }

        private static ShutterSoundCount[] values;

        public static ShutterSoundCount[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

    }
}

