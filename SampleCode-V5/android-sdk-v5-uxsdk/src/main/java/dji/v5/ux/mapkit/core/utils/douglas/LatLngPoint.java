package dji.v5.ux.mapkit.core.utils.douglas;

import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 10/22/17.
 */

public class LatLngPoint implements Comparable<LatLngPoint> {

    /**
     * 用于记录每一个点的序号
     */
    public int id;

    /**
     * 每一个点的经纬度
     */
    public DJILatLng latLng;

    public LatLngPoint(int id, DJILatLng latLng) {
        this.id = id;
        this.latLng = latLng;
    }

    @Override
    public int compareTo(@NonNull LatLngPoint o) {
        if (this.id < o.id) {
            return -1;
        } else if (this.id > o.id) {
            return 1;
        } else {
            return 0;
        }
    }
}
