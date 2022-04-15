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
import dji.sdk.keyvalue.value.camera.PhotoPanoramaMode;

/**
 * Camera Panorama Photo State
 * <p>
 * Class represents camera photo state. It will be returned when device is in
 * CameraShootPhotoMode panorama mode.
 * It will also return the value of PhotoPanoramaMode
 */
public class CameraPanoramaPhotoState extends CameraPhotoState {
    private PhotoPanoramaMode photoPanoramaMode;

    public CameraPanoramaPhotoState(@NonNull CameraShootPhotoMode shootPhotoMode, @NonNull PhotoPanoramaMode photoPanoramaMode) {
        super(shootPhotoMode);
        this.photoPanoramaMode = photoPanoramaMode;
    }

    /**
     * Get the current panorama mode value
     *
     * @return instance of PhotoPanoramaMode
     */
    public PhotoPanoramaMode getPhotoPanoramaMode() {
        return photoPanoramaMode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CameraPanoramaPhotoState) {
            return ((CameraPanoramaPhotoState) obj).getShootPhotoMode() == this.getShootPhotoMode()
                    && ((CameraPanoramaPhotoState) obj).getPhotoPanoramaMode() == this.getPhotoPanoramaMode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31 * getShootPhotoMode().value();
        result = result + 31 * getPhotoPanoramaMode().value();
        return result;
    }

}
