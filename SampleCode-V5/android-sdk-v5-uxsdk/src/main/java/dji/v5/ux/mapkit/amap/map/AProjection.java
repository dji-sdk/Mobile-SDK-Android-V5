package dji.v5.ux.mapkit.amap.map;

import android.graphics.Point;

import com.amap.api.maps.Projection;
import com.amap.api.maps.model.LatLng;
import dji.v5.ux.mapkit.amap.utils.AMapUtils;
import dji.v5.ux.mapkit.core.maps.DJIProjection;
import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 6/12/17.
 */

public class AProjection implements DJIProjection {

    Projection mProjection;

    public AProjection(Projection projection) {
        mProjection = projection;
    }

    @Override
    public DJILatLng fromScreenLocation(Point point) {
        DJILatLng result = null;
        LatLng latLng = mProjection.fromScreenLocation(point);
        if (latLng != null) {
//            result = new DJILatLng(latLng);
            result = AMapUtils.fromLatLng(latLng);
        }
        return result;
    }

    @Override
    public Point toScreenLocation(DJILatLng location) {
        LatLng latLng = AMapUtils.fromDJILatLng(location);
        return mProjection.toScreenLocation(latLng);
    }
}
