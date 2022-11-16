package dji.v5.ux.mapkit.amap.utils;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.PolylineOptions;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdate;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdateFactory;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.DJILatLngBounds;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;
import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 2/10/18.
 */

public class AMapUtils {

    private AMapUtils(){}

    public static final LatLng fromDJILatLng(DJILatLng latLng) {
        DJILatLng gcjLatLng = DJIGpsUtils.wgs2gcjInChina(latLng);
        return new LatLng(gcjLatLng.getLatitude(), gcjLatLng.getLongitude());
    }

    public static final DJILatLng fromLatLng(LatLng latLng) {
        DJILatLng ll = new DJILatLng(latLng.latitude, latLng.longitude);
        DJILatLng transformed = DJIGpsUtils.gcj2wgsInChina(ll);
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
            int paddingLeft = boundsUpdate.getPaddingLeft();
            int paddingRight = boundsUpdate.getPaddingRight();
            int paddingBottom = boundsUpdate.getPaddingBottom();
            int paddingTop = boundsUpdate.getPaddingTop();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            DJILatLngBounds bounds = boundsUpdate.getBounds();
            DJILatLng northeast = bounds.getNortheast();
            DJILatLng southwest = bounds.getSouthwest();

            builder.include(fromDJILatLng(northeast))
                    .include(fromDJILatLng(southwest));

            if (width == 0 || height == 0) {
                u = CameraUpdateFactory.newLatLngBoundsRect(builder.build(),
                        padding + paddingLeft,
                        padding + paddingRight,
                        padding + paddingTop,
                        padding + paddingBottom);
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
            case VIEW:
                bitmapDescriptor = BitmapDescriptorFactory.fromView(descriptor.getView());
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
        List<DJILatLng> originPoints = options.getPoints();
        // 暂弃用此航点抽稀算法 缩放级别较大时，航迹绘不连贯，弯弯曲曲, 并且高德之外的地图都没有用
//        List<DJILatLng> compressPoints = originPoints;
//        // 若首尾两个点坐标相同，使用道格拉斯算法会有问题，需要排除这种情况
//        if (!originPoints.isEmpty() && !originPoints.get(0).equals(originPoints.get(originPoints.size() - 1))) {
//            compressPoints = DouglasUtils.compress(originPoints, 0.5);
//        }
        for (DJILatLng point : originPoints) {
            points.add(fromDJILatLng(point));
        }
        result.width(options.getWidth())
                .zIndex(options.getZIndex())
                .color(options.getColor())
                .visible(options.isVisible())
                .geodesic(options.isGeodesic())
                .addAll(points);
        // 高德地图不能设置虚线间隔和长短， 只能选择dot类型：圆形或方形, 这里默认选择方形
        if (options.isDashed()) {
            result.setDottedLine(true).setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE);
        }
        if (options.isEnableTexture() && options.getBitmapDescriptor() != null){
            result.setUseTexture(true);
            BitmapDescriptor bitmapDescriptor = fromDJIBitmapDescriptor(options.getBitmapDescriptor());
            result.setCustomTexture(bitmapDescriptor);
        }
        return result;
    }
}
