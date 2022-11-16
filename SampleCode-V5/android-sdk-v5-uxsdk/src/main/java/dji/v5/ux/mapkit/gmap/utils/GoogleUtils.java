package dji.v5.ux.mapkit.gmap.utils;

import dji.v5.ux.mapkit.core.camera.DJICameraUpdate;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdateFactory;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.DJILatLngBounds;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;
import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by joeyang on 2/10/18.
 */

public class GoogleUtils {

    private GoogleUtils(){
        //utils
    }

    public static final LatLng fromDJILatLng(DJILatLng latLng) {
        DJILatLng gcjLatLng = DJIGpsUtils.wgs2gcjJustInMainlandChina(latLng);
        return new LatLng(gcjLatLng.getLatitude(), gcjLatLng.getLongitude());
    }

    public static final DJILatLng fromLatLng(LatLng latLng) {
        DJILatLng ll = new DJILatLng(latLng.latitude, latLng.longitude);
        DJILatLng transformed = DJIGpsUtils.gcj2wgsJustInMainlandChina(ll);
        return transformed;
    }

    public static final CameraUpdate fromDJICameraUpdate(DJICameraUpdate cameraUpdate) {
        CameraUpdate u = CameraUpdateFactory.newLatLng(new LatLng(0.0, 0.0));
        if (cameraUpdate instanceof DJICameraUpdateFactory.CameraBoundsUpdate) {
            final DJICameraUpdateFactory.CameraBoundsUpdate boundsUpdate
                    = (DJICameraUpdateFactory.CameraBoundsUpdate) cameraUpdate;
            int width = boundsUpdate.getWidth();
            int height = boundsUpdate.getHeight();
            int padding = boundsUpdate.getPadding();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            DJILatLngBounds bounds = boundsUpdate.getBounds();
            DJILatLng northeast = bounds.getNortheast();
            DJILatLng southwest = bounds.getSouthwest();

            builder.include(fromDJILatLng(northeast))
                    .include(fromDJILatLng(southwest));

            if (width == 0 || height == 0) {
                u = CameraUpdateFactory.newLatLngBounds(builder.build(), boundsUpdate.getPadding());
            } else {
                u = CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, padding);
            }
        } else if (cameraUpdate instanceof DJICameraUpdateFactory.CameraPositionUpdate) {
            final DJICameraUpdateFactory.CameraPositionUpdate positionUpdate
                    = (DJICameraUpdateFactory.CameraPositionUpdate) cameraUpdate;
            CameraPosition p;
            LatLng target = fromDJILatLng(positionUpdate.getTarget());
            p = new CameraPosition.Builder().target(target)
                    .zoom(positionUpdate.getZoom())
                    .tilt(positionUpdate.getTilt())
                    .bearing(positionUpdate.getBearing())
                    .build();
            u = CameraUpdateFactory.newCameraPosition(p);
        }
        return u;
    }

    public static final DJICameraPosition fromCameraPosition(CameraPosition cameraPosition) {
        DJICameraPosition.Builder builder = new DJICameraPosition.Builder();
        LatLng target = cameraPosition.target;
        builder.target(fromLatLng(target))
                .zoom(cameraPosition.zoom)
                .tilt(cameraPosition.tilt)
                .bearing(cameraPosition.bearing);
        return builder.build();
    }

    public static final BitmapDescriptor fromDJIBitmapDescriptor(DJIBitmapDescriptor descriptor) {
        BitmapDescriptor bitmapDescriptor;
        String path = descriptor.getPath();
        switch (descriptor.getType()) {
            case BITMAP:
                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(descriptor.getBitmap());
                break;
            case PATH_ABSOLUTE:
                bitmapDescriptor = BitmapDescriptorFactory.fromPath(path);
                break;
            case PATH_ASSET:
                bitmapDescriptor = BitmapDescriptorFactory.fromAsset(path);
                break;
            case PATH_FILEINPUT:
                bitmapDescriptor = BitmapDescriptorFactory.fromFile(path);
                break;
            case RESOURCE_ID:
                bitmapDescriptor = BitmapDescriptorFactory.fromResource(descriptor.getResourceId());
                break;
            default:
                throw new AssertionError();
        }
        return bitmapDescriptor;
    }

    public static final PolygonOptions fromDJIPolygonOptions(DJIPolygonOptions options) {
        PolygonOptions result = new PolygonOptions();
        List<LatLng> points = new ArrayList<>();
        for (DJILatLng point : options.getPoints()) {
            points.add(fromDJILatLng(point));
        }
        result.strokeWidth(options.getStrokeWidth())
                .zIndex(options.getZIndex())
                .strokeColor(options.getStrokeColor())
                .fillColor(options.getFillColor())
                .visible(options.isVisible())
                .addAll(points);
        return result;
    }

    public static final PolylineOptions fromDJIPolylineOptions(DJIPolylineOptions options) {
        PolylineOptions result = new PolylineOptions();
        List<LatLng> points = new ArrayList<>();
        for (DJILatLng point : options.getPoints()) {
            points.add(fromDJILatLng(point));
        }
        result.width(options.getWidth())
                .zIndex(options.getZIndex())
                .color(options.getColor())
                .visible(options.isVisible())
                .geodesic(options.isGeodesic())
                .addAll(points);
        // 谷歌地图可以设置多种类型，这里默认dash & gap
        if (options.isDashed()) {
            PatternItem dash = new Dash(options.getDashLength());
            PatternItem gap = new Gap(options.getDashLength());
            List<PatternItem> patternItems = Arrays.asList(dash, gap);
            result.pattern(patternItems);
        }
        return result;
    }
}
