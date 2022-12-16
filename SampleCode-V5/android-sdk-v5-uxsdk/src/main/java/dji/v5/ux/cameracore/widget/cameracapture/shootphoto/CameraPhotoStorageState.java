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

import dji.sdk.keyvalue.value.camera.CameraStorageLocation;

/**
 * Class represents the storage state when camera is in Shoot Photo Mode
 * The state determines the storage location and the available space
 * in terms of capture count
 */
public abstract class CameraPhotoStorageState {
    private final CameraStorageLocation storageLocation;
    private final long availableCaptureCount;

    protected CameraPhotoStorageState(CameraStorageLocation storageLocation, long availableCaptureCount) {
        this.storageLocation = storageLocation;
        this.availableCaptureCount = availableCaptureCount;
    }

    /**
     *  Get the current storage location
     *
     * @return instance of CameraStorageLocation
     */
    public CameraStorageLocation getStorageLocation() {
        return storageLocation;
    }

    /**
     *  Get the number of photos that can be clicked before running out of storage
     *
     * @return long value representing count of photos
     */
    public long getAvailableCaptureCount() {
        return availableCaptureCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraPhotoStorageState)) return false;
        CameraPhotoStorageState that = (CameraPhotoStorageState) o;
        return availableCaptureCount == that.availableCaptureCount &&
                storageLocation == that.storageLocation;
    }


    @Override
    public int hashCode() {
        int result = 31 * storageLocation.value();
        result = result + 31 * (int) availableCaptureCount;
        return result;
    }
}
