package dji.v5.ux.mapkit.maplibre.map;

import android.graphics.Point;
import android.graphics.PointF;

import dji.v5.ux.mapkit.core.maps.DJIProjection;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Projection;

/**
 * Created by joeyang on 11/3/17.
 */
public class MProjection implements DJIProjection {

    Projection projection;

    public MProjection(Projection projection) {
        this.projection = projection;
    }

    @Override
    public DJILatLng fromScreenLocation(Point point) {
        DJILatLng result;
        LatLng latLng = projection.fromScreenLocation(new PointF(point));

        result = MaplibreUtils.fromLatLng(latLng);
        return result;
    }

    @Override
    public Point toScreenLocation(DJILatLng location) {
        LatLng latLng = MaplibreUtils.fromDJILatLng(location);
        PointF screentLocation = projection.toScreenLocation(latLng);
        return new Point((int)screentLocation.x, (int)screentLocation.y);
    }
}
