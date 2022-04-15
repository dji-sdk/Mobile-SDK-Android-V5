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
import dji.sdk.keyvalue.value.camera.SSDOperationState;

/**
 * Class represents SSD Storage state for Record Video Mode
 */
public class CameraSSDVideoStorageState extends CameraVideoStorageState {

    private SSDOperationState ssdOperationState;

    public CameraSSDVideoStorageState(CameraStorageLocation storageLocation, int availableRecordingTimeInSeconds, SSDOperationState ssdOperationState) {
        super(storageLocation, availableRecordingTimeInSeconds);
        this.ssdOperationState = ssdOperationState;
    }

    /**
     * Get operation state of SSD
     *
     * @return {@link SSDOperationState}
     */
    public SSDOperationState getSsdOperationState() {
        return ssdOperationState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraSSDVideoStorageState)) return false;
        if (!super.equals(o)) return false;
        CameraSSDVideoStorageState that = (CameraSSDVideoStorageState) o;
        return getSsdOperationState() == that.getSsdOperationState();
    }

    @Override
    public int hashCode() {
        return 31 * getSsdOperationState().value();
    }
}
