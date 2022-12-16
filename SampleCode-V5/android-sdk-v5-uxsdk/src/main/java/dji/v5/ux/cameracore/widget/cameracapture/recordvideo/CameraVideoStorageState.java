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

package dji.v5.ux.cameracore.widget.cameracapture.recordvideo;

import dji.sdk.keyvalue.value.camera.CameraStorageLocation;

/**
 * Class represents the storage state when camera is in Record Video Mode
 * The state determines the storage location and the available space
 * in terms of duration in seconds
 */
public abstract class CameraVideoStorageState {
    private final CameraStorageLocation storageLocation;
    private final int availableRecordingTimeInSeconds;

    protected CameraVideoStorageState(CameraStorageLocation storageLocation, int availableRecordingTimeInSeconds) {
        this.storageLocation = storageLocation;
        this.availableRecordingTimeInSeconds = availableRecordingTimeInSeconds;
    }

    /**
     * Get the current storage location
     *
     * @return instance of CameraStorageLocation
     */
    public CameraStorageLocation getStorageLocation() {
        return storageLocation;
    }

    /**
     * Get the duration in seconds that can be recorded before running out of storage
     *
     * @return integer value representing seconds
     */
    public int getAvailableRecordingTimeInSeconds() {
        return availableRecordingTimeInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraVideoStorageState)) return false;
        CameraVideoStorageState that = (CameraVideoStorageState) o;
        return getAvailableRecordingTimeInSeconds() == that.getAvailableRecordingTimeInSeconds() &&
                getStorageLocation() == that.getStorageLocation();
    }

    @Override
    public int hashCode() {
        int result = 31 * getStorageLocation().value();
        result = result + 31 * getAvailableRecordingTimeInSeconds();
        return result;
    }
}
