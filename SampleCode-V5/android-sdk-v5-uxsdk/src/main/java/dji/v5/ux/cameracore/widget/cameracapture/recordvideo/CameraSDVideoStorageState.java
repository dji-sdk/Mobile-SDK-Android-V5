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

import dji.sdk.keyvalue.value.camera.CameraSDCardState;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.SSDOperationState;

/**
 * Class represents storage state for SD card and internal storage
 * when camera is in record video mode
 */
public class CameraSDVideoStorageState extends CameraVideoStorageState {

    private CameraSDCardState sdCardOperationState;

    public CameraSDVideoStorageState(CameraStorageLocation storageLocation, int availableRecordingTimeInSeconds, CameraSDCardState sdCardOperationState) {
        super(storageLocation, availableRecordingTimeInSeconds);
        this.sdCardOperationState = sdCardOperationState;
    }

    /**
     * Get the operation state of current storage
     *
     * @return instance of {@link SSDOperationState} representing state for SDCard or internal Storage
     */
    public CameraSDCardState getSdCardOperationState() {
        return sdCardOperationState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraSDVideoStorageState)) return false;
        if (!super.equals(o)) return false;
        CameraSDVideoStorageState that = (CameraSDVideoStorageState) o;
        return getSdCardOperationState() == that.getSdCardOperationState();
    }

    @Override
    public int hashCode() {
        return 31 * getSdCardOperationState().value();
    }
}
