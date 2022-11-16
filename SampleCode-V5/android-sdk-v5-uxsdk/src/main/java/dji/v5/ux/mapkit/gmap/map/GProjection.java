package dji.v5.ux.mapkit.gmap.map;

import android.graphics.Point;

import dji.v5.ux.mapkit.core.maps.DJIProjection;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joeyang on 6/12/17.
 */
public class GProjection implements DJIProjection {

    Projection mProjection;

    public GProjection(Projection projection) {
        mProjection = projection;
    }

    @Override
    public DJILatLng fromScreenLocation(Point point) {
        DJILatLng result = null;
        LatLng latLng = mProjection.fromScreenLocation(point);
        if (latLng != null) {
//            result = new DJILatLng(latLng);
            result = GoogleUtils.fromLatLng(latLng);
        }
        return result;
    }

    @Override
    public Point toScreenLocation(DJILatLng location) {
        LatLng latLng = GoogleUtils.fromDJILatLng(location);
        return mProjection.toScreenLocation(latLng);
    }
}
