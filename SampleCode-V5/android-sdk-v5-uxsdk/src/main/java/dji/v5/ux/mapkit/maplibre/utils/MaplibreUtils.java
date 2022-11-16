package dji.v5.ux.mapkit.maplibre.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;

import dji.v5.ux.mapkit.core.camera.DJICameraUpdate;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdateFactory;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.DJILatLngBounds;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by joeyang on 2/10/18.
 */

public class MaplibreUtils {

    private static final float ZOOM_OFFSET = 2F;


    private MaplibreUtils(){}

    public static final LatLng fromDJILatLng(DJILatLng latLng) {
        return new LatLng(latLng.getLatitude(), latLng.getLongitude(), latLng.getAltitude());
    }

    public static final DJILatLng fromLatLng(LatLng latLng) {
        DJILatLng ll = new DJILatLng(latLng.getLatitude(), latLng.getLongitude(), latLng.getAltitude());
        return ll;
    }

    public static final CameraUpdate fromDJICameraUpdate(DJICameraUpdate cameraUpdate) {
        CameraUpdate u = CameraUpdateFactory.newLatLng(new LatLng(0.0, 0.0));
        if (cameraUpdate instanceof DJICameraUpdateFactory.CameraBoundsUpdate) {
            final DJICameraUpdateFactory.CameraBoundsUpdate boundsUpdate
                    = (DJICameraUpdateFactory.CameraBoundsUpdate) cameraUpdate;
            int padding = boundsUpdate.getPadding();
            int paddingLeft = boundsUpdate.getPaddingLeft();
            int paddingRight = boundsUpdate.getPaddingRight();
            int paddingBottom = boundsUpdate.getPaddingBottom();
            int paddingTop = boundsUpdate.getPaddingTop();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            DJILatLngBounds bounds = boundsUpdate.getBounds();
            DJILatLng northeast = bounds.getNortheast();
            DJILatLng southwest = bounds.getSouthwest();

            if (northeast.equals(southwest)) {
                u = CameraUpdateFactory.newLatLng(fromDJILatLng(northeast));
            } else {
                builder.include(fromDJILatLng(northeast))
                        .include(fromDJILatLng(southwest));
                u = CameraUpdateFactory.newLatLngBounds(builder.build(),
                        paddingLeft + padding,
                        paddingTop + padding,
                        paddingRight + padding,
                        paddingBottom + padding);
            }
        } else if (cameraUpdate instanceof DJICameraUpdateFactory.CameraPositionUpdate) {
            final DJICameraUpdateFactory.CameraPositionUpdate positionUpdate
                    = (DJICameraUpdateFactory.CameraPositionUpdate) cameraUpdate;
            CameraPosition p;
            LatLng target = fromDJILatLng(positionUpdate.getTarget());
            p = new CameraPosition.Builder().target(target)
                    .zoom(positionUpdate.getZoom() - ZOOM_OFFSET)
                    .tilt(positionUpdate.getTilt())
                    .bearing(positionUpdate.getBearing())
                    .build();
            u = CameraUpdateFactory.newCameraPosition(p);
        }
        return u;
    }

    public static final DJICameraPosition fromCameraPosition(CameraPosition cameraPosition) {
        DJICameraPosition.Builder builder = new DJICameraPosition.Builder();
        builder.target(fromLatLng(cameraPosition.target))
                .zoom((float) cameraPosition.zoom + ZOOM_OFFSET)
                .tilt((float) cameraPosition.tilt)
                .bearing((float) cameraPosition.bearing);
        return builder.build();
    }

    public static final Icon fromDJIBitmapDescriptor(Context context,  DJIBitmapDescriptor descriptor) {
        Icon icon;
        String path = descriptor.getPath();
        switch (descriptor.getType()) {
            case BITMAP:
                icon = IconFactory.getInstance(context).fromBitmap(descriptor.getBitmap());
                break;
            case PATH_ABSOLUTE:
                icon = IconFactory.getInstance(context).fromPath(path);
                break;
            case PATH_ASSET:
                icon = IconFactory.getInstance(context).fromAsset(path);
                break;
            case PATH_FILEINPUT:
                icon = IconFactory.getInstance(context).fromFile(path);
                break;
            case RESOURCE_ID:
                icon = IconFactory.getInstance(context).fromResource(descriptor.getResourceId());
                break;
            default:
                throw new AssertionError();
        }
        descriptor.updateBitmap(icon.getBitmap());
        return icon;
    }

    public static final PolygonOptions fromDJIPolygonOptions(DJIPolygonOptions options) {
        PolygonOptions result = new PolygonOptions();
        List<LatLng> points = new ArrayList<>();
        for (DJILatLng point : options.getPoints()) {
            points.add(fromDJILatLng(point));
        }

        // mapbox使用带透明度的颜色时，可能出现类似反色情况，导致地图看不清楚
        // 所以把透明度单独用alpha来赋值
        int fillColor = options.getFillColor();
        int rgbColor = Color.rgb(Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor));
        float alpha = Color.alpha(fillColor) * 1.0f / 255;
        options.setAlpha(alpha);

        result.strokeColor(options.getStrokeColor())
                .alpha(alpha)
                .fillColor(rgbColor)
                .addAll(points);
        return result;
    }

    public static String getLanguageString(Context context) {
        Locale locale;
        Configuration configuration = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !configuration.getLocales().isEmpty()) {
            locale = configuration.getLocales().get(0);
        } else {
            locale = configuration.locale;
        }

        String result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            result = TextUtils.isEmpty(locale.getScript()) ? locale.getLanguage() : locale.getLanguage() + "-" + locale.getScript();
        } else {
            result = locale.getLanguage();
            // 支持列表中只有中文有这个问题，简中:zh-Hans，繁中:zh-Hant
            if (Locale.CHINESE.getLanguage().equals(result)) {
                result += Locale.SIMPLIFIED_CHINESE.getCountry().equals(locale.getCountry()) ? "-Hans" : "-Hant";
            }
        }
        return result;
    }

    public static Polygon getCircle(Point center, double radius) {
        double radiusInKilometers = radius / 1000;
        List<Point> positions = new ArrayList<>();
        double distanceX = radiusInKilometers / (111.319 * Math.cos(center.latitude() * Math.PI / 180));
        double distanceY = radiusInKilometers / 110.574;

        double slice = (2 * Math.PI) / 64;

        double theta;
        double x;
        double y;
        Point position;
        for (int i = 0; i < 64; ++i) {
            theta = i * slice;
            x = distanceX * Math.cos(theta);
            y = distanceY * Math.sin(theta);

            position = Point.fromLngLat(center.longitude() + x,
                    center.latitude() + y);
            positions.add(position);
        }

        List<List<Point>> circleList = new ArrayList<>(1);
        circleList.add(positions);
        return Polygon.fromLngLats(circleList);
    }

}
