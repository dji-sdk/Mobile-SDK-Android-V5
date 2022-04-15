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

package dji.v5.ux.cameracore.widget.cameracapture.shootphoto;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;

/**
 * Camera Interval Photo State
 * <p>
 * Class represents camera photo state. It will be returned when device is in
 * CameraShootPhotoMode interval mode.
 * It will also return the capture count and interval duration in seconds
 */
public class CameraIntervalPhotoState extends CameraPhotoState {

    private int captureCount;
    private int timeIntervalInSeconds;

    public CameraIntervalPhotoState(@NonNull CameraShootPhotoMode shootPhotoMode, int captureCount, int timeIntervalInSeconds) {
        super(shootPhotoMode);
        this.captureCount = captureCount;
        this.timeIntervalInSeconds = timeIntervalInSeconds;
    }

    /**
     * Get capture count
     *
     * @return int value
     */
    public int getCaptureCount() {
        return captureCount;
    }

    /**
     * Get time interval in seconds
     *
     * @return int value
     */
    public int getTimeIntervalInSeconds() {
        return timeIntervalInSeconds;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CameraIntervalPhotoState) {
            return ((CameraIntervalPhotoState) obj).getShootPhotoMode() == this.getShootPhotoMode()
                    && ((CameraIntervalPhotoState) obj).getCaptureCount() == this.getCaptureCount()
                    && ((CameraIntervalPhotoState) obj).getTimeIntervalInSeconds() == this.getTimeIntervalInSeconds();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31 * getShootPhotoMode().value();
        result = result + 31 * getCaptureCount();
        result = result + 31 * getTimeIntervalInSeconds();
        return result;
    }
}
