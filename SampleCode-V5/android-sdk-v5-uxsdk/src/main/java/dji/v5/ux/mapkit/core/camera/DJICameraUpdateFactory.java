package dji.v5.ux.mapkit.core.camera;

import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.maps.DJIMap;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.DJILatLngBounds;

//Doc key: DJIMap_DJICameraUpdateFactory
/**
 * Factory for creating DJICameraUpdate objects.
 */
public final class DJICameraUpdateFactory {
    private DJICameraUpdateFactory(){}

    //Doc key: DJIMap_DJICameraUpdateFactory_newCameraPosition
    /**
     * Creates a camera update that moves the center of the screen to the `DJICameraPosition`.
     *
     * @param cameraPosition The new position of the camera.
     * @return A DJICameraUpdate object.
     */
    public static DJICameraUpdate newCameraPosition(@NonNull DJICameraPosition cameraPosition) {
        return new CameraPositionUpdate(cameraPosition.target, cameraPosition.zoom,
                cameraPosition.tilt, cameraPosition.bearing);
    }

    /**
     * {@hide}
     * 新的{@link DJILatLngBounds}
     * @param bounds
     * @param width bounds的宽度（here下失效）
     * @param height bounds的高度（here下失效）
     * @param zoom 放大级别
     * @param padding 间距
     * @return
     */
    public static DJICameraUpdate newLatLngBounds(@NonNull DJILatLngBounds bounds, int width, int height, int zoom, int padding) {
        return new CameraBoundsUpdate(bounds, width, height, zoom, padding);
    }

    /**
     * {@hide}
     * 新的{@link DJILatLngBounds}
     * @param bounds
     * @param width bounds的宽度（here下失效）
     * @param height bounds的高度（here下失效）
     * @param zoom 放大级别
     * @return
     */
    public static DJICameraUpdate newLatLngBounds(@NonNull DJILatLngBounds bounds, int width, int height, int zoom) {
        return new CameraBoundsUpdate(bounds, width, height, zoom, 0);
    }

    //Doc key: DJIMap_DJICameraUpdateFactory_newLatLngBoundsWithPadding
    /**
     * Creates a camera update that moves the center of the screen to the `DJILatLngBounds`
     * at the given zoom level or with the given padding.
     *
     * @param bounds The bounding box.
     * @param zoom The zoom level.
     * @param padding Additional space in pixels to leave between the bounds and the edge.
     * @return A DJICameraUpdate object.
     */
    public static DJICameraUpdate newLatLngBounds(@NonNull DJILatLngBounds bounds, int zoom, int padding) {
        return new CameraBoundsUpdate(bounds, 0, 0, zoom, padding);
    }

    /**
     * {@hide}
     * @param bounds
     * @param paddingtop
     * @param paddingRight
     * @param paddingBottom
     * @param paddingLeft
     * @param zoom
     * @return
     */
    public static DJICameraUpdate newLatLngBounds(@NonNull DJILatLngBounds bounds, int paddingtop, int paddingRight, int paddingBottom, int paddingLeft, int zoom) {
        return new CameraBoundsUpdate(bounds, paddingtop, paddingRight, paddingBottom, paddingLeft, zoom);
    }

    //Doc key: DJIMap_DJICameraUpdateFactory_newLatLngBounds
    /**
     * Creates a camera update that moves the center of the screen to the `DJILatLngBounds`
     * at the given zoom level.
     *
     * @param bounds The bounding box.
     * @param zoom The zoom level.
     * @return A DJICameraUpdate object.
     */
    public static DJICameraUpdate newLatLngBounds(@NonNull DJILatLngBounds bounds, int zoom) {
        return new CameraBoundsUpdate(bounds, 0, 0, zoom, 0);
    }

    /**
     * 用于更新Camera的位置
     */
    public static final class CameraPositionUpdate implements DJICameraUpdate {

        private final DJILatLng target;
        private final float zoom;
        private final float tilt;
        private final float bearing;

        public CameraPositionUpdate(DJILatLng target, float zoom, float tilt, float bearing) {
            this.target = target;
            this.zoom = zoom;
            this.tilt = tilt;
            this.bearing = bearing;
        }

        public DJILatLng getTarget() {
            return target;
        }

        public float getZoom() {
            return zoom;
        }

        public float getTilt() {
            return tilt;
        }

        public float getBearing() {
            return bearing;
        }

        @Override
        public DJICameraPosition getCameraPosition(@NonNull DJIMap map) {
            DJICameraPosition previousPosition = map.getCameraPosition();
            DJICameraPosition.Builder builder = new DJICameraPosition.Builder()
                    .zoom(zoom)
                    .tilt(tilt)
                    .bearing(bearing);
            if (target == null) {
                builder.target(previousPosition.target);
            } else {
                builder.target(target);
            }
            return builder.build();
        }
    }

    /**
     * 用于更新CameraBounds
     */
    public static final class CameraBoundsUpdate implements DJICameraUpdate {
        private DJILatLngBounds bounds;
        private int width;
        private int height;
        private int zoom;
        private int padding = -1;

        private int paddingTop;
        private int paddingRight;
        private int paddingBottom;
        private int paddingLeft;

        CameraBoundsUpdate(DJILatLngBounds bounds, int width, int height, int zoom, int padding) {
            this.bounds = bounds;
            this.width = width;
            this.height = height;
            this.zoom = zoom;
            this.padding = padding;
        }

        CameraBoundsUpdate(DJILatLngBounds bounds, int paddingTop, int paddingRight, int paddingBottom, int paddingLeft, int zoom) {
            this.bounds = bounds;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
            this.paddingLeft = paddingLeft;
            this.zoom = zoom;
        }

        @Override
        public DJICameraPosition getCameraPosition(@NonNull DJIMap map) {
            // TODO: 6/21/17 map.getCameraForLatLngBounds()
            return null;
        }

        @Override
        public DJILatLng getTarget() {
            return null;
        }

        public DJILatLngBounds getBounds() {
            return bounds;
        }

        @Override
        public float getZoom() {
            return zoom;
        }

        @Override
        public float getTilt() {
            return 0;
        }

        @Override
        public float getBearing() {
            return 0;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getPadding() {
            return padding;
        }

        public int getPaddingTop() {
            return paddingTop;
        }

        public int getPaddingRight() {
            return paddingRight;
        }

        public int getPaddingBottom() {
            return paddingBottom;
        }

        public int getPaddingLeft() {
            return paddingLeft;
        }
    }
}
