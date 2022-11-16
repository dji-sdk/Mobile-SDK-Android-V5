package dji.v5.ux.mapkit.core.models;

import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.exceptions.InvalidLatLngBoundsException;

import java.util.ArrayList;
import java.util.List;

//Doc key: DJIMap_DJILatLngBounds
/**
 * Represents a rectangle defined by the coordinates of its northeast and southwest corners.
 */
public class DJILatLngBounds {

    private final DJILatLng northeast;
    private final DJILatLng southwest;

    private DJILatLngBounds(DJILatLng northeast, DJILatLng southwest) {
        this.northeast = northeast;
        this.southwest = southwest;
    }

    public DJILatLng getNortheast() {
        return northeast;
    }

    public DJILatLng getSouthwest() {
        return southwest;
    }

    //Doc key: DJIMap_DJILatLngBounds_fromLatLngs
    /**
     * Creates a new DJILatLngBounds defined by the minimum and maximum coordinate values in the
     * given list.
     *
     * @param latLngs A list of coordinates.
     * @return A `DJILatLngBounds` object.
     */
    public static DJILatLngBounds fromLatLngs(final List<DJILatLng> latLngs) {
        double minLat = 90;
        double minLon = 180;
        double maxLat = -90;
        double maxLon = -180;

        for (final DJILatLng point : latLngs) {
            final double latitude = point.latitude;
            final double longitude = point.longitude;

            minLat = Math.min(minLat, latitude);
            minLon = Math.min(minLon, longitude);
            maxLat = Math.max(maxLat, latitude);
            maxLon = Math.max(maxLon, longitude);
        }

        DJILatLng northeast = new DJILatLng(maxLat, maxLon);
        DJILatLng southwest = new DJILatLng(minLat, minLon);
        return new DJILatLngBounds(northeast, southwest);
    }

    /**
     * Builder for composing LatLngBounds objects.
     */
    public static final class Builder {
        private List<DJILatLng> mLatLngList;

        public Builder() {
            mLatLngList = new ArrayList<>();
        }

        public DJILatLngBounds build() {
            if (mLatLngList.size() < 2) {
                throw new InvalidLatLngBoundsException(mLatLngList.size());
            }
            return DJILatLngBounds.fromLatLngs(mLatLngList);
        }

        public Builder includes(List<DJILatLng> latLngs) {
            for (DJILatLng point : latLngs) {
                mLatLngList.add(point);
            }
            return this;
        }

        public Builder include(@NonNull DJILatLng latLng) {
            mLatLngList.add(latLng);
            return this;
        }
    }
}
