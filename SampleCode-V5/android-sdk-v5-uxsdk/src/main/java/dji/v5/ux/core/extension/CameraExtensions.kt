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

@file:JvmName("CameraExtensions")

package dji.v5.ux.core.extension

import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode
import dji.sdk.keyvalue.value.camera.CameraFlatMode

/**
 * Convert [CameraFlatMode] to [CameraShootPhotoMode]
 */
fun CameraFlatMode.toShootPhotoMode(): CameraShootPhotoMode {
    return when (this) {
        CameraFlatMode.PHOTO_NORMAL -> CameraShootPhotoMode.NORMAL
        CameraFlatMode.PHOTO_HDR -> CameraShootPhotoMode.HDR
        CameraFlatMode.PHOTO_BURST -> CameraShootPhotoMode.BURST
        CameraFlatMode.PHOTO_AEB -> CameraShootPhotoMode.AEB
        CameraFlatMode.PHOTO_INTERVAL -> CameraShootPhotoMode.INTERVAL
        CameraFlatMode.PHOTO_PANO -> CameraShootPhotoMode.VISION_PANO
        CameraFlatMode.PHOTO_EHDR -> CameraShootPhotoMode.EHDR
        CameraFlatMode.PHOTO_HYPERLIGHT -> CameraShootPhotoMode.HYPER_LIGHT
        else -> CameraShootPhotoMode.UNKNOWN
    }
}

/**
 * Convert [CameraShootPhotoMode] to [CameraFlatMode]
 */
fun CameraShootPhotoMode.toFlatCameraMode(): CameraFlatMode {
    return when (this) {
        CameraShootPhotoMode.NORMAL -> CameraFlatMode.PHOTO_NORMAL
        CameraShootPhotoMode.HDR -> CameraFlatMode.PHOTO_HDR
        CameraShootPhotoMode.BURST -> CameraFlatMode.PHOTO_BURST
        CameraShootPhotoMode.AEB -> CameraFlatMode.PHOTO_AEB
        CameraShootPhotoMode.INTERVAL -> CameraFlatMode.PHOTO_INTERVAL
        CameraShootPhotoMode.VISION_PANO -> CameraFlatMode.PHOTO_PANO
        CameraShootPhotoMode.EHDR -> CameraFlatMode.PHOTO_EHDR
        CameraShootPhotoMode.HYPER_LIGHT -> CameraFlatMode.PHOTO_HYPERLIGHT
        else -> CameraFlatMode.UNKNOWN
    }
}

/**
 * Check if flat camera mode is picture mode
 */
fun CameraFlatMode.isPictureMode(): Boolean {
    return this == CameraFlatMode.PHOTO_COUNTDOWN
            || this == CameraFlatMode.PHOTO_AEB
            || this == CameraFlatMode.PHOTO_NORMAL
            || this == CameraFlatMode.PHOTO_BURST
            || this == CameraFlatMode.PHOTO_HDR
            || this == CameraFlatMode.PHOTO_INTERVAL
            || this == CameraFlatMode.PHOTO_HYPERLIGHT
            || this == CameraFlatMode.PHOTO_PANO
            || this == CameraFlatMode.PHOTO_EHDR
}