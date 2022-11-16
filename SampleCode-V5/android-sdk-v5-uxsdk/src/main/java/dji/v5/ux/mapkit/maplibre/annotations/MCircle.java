package dji.v5.ux.mapkit.maplibre.annotations;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJICircle;
import dji.v5.ux.mapkit.core.models.annotations.DJICircleOptions;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapDelegate;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;

/**
 * Created by joeyang on 11/3/17.
 */
public class MCircle implements DJICircle {

    private static final float NO_ALPHA = 0.0F;

    private MapboxMap mapboxMap;
    private FillLayer singleCircleLayer;
    private GeoJsonSource source;
    private MaplibreMapDelegate maplibreMapDelegate;
    private DJICircleOptions options;
    private Polyline border;
    private float borderAlpha;

    private String sourceId;
    private String layerId;


    public MCircle(MaplibreMapDelegate maplibreMapDelegate,
                   MapboxMap mapboxMap,
                   FillLayer singleCircleLayer,
                   GeoJsonSource source,
                   DJICircleOptions options) {
        this.maplibreMapDelegate = maplibreMapDelegate;
        this.mapboxMap = mapboxMap;
        this.singleCircleLayer = singleCircleLayer;
        this.source = source;
        this.sourceId = source.getId();
        this.layerId = singleCircleLayer.getId();
        this.options = options;
        //PolylineOptions polylineOptions = new PolylineOptions();
        //polylineOptions.addAll(polygon.getPoints())
        //              .add(polygon.getPoints().get(0))
        //               .color(options.getStrokeColor())
        //               .width(options.getStrokeWidth() / 5f);
        //borderAlpha = polylineOptions.getAlpha();
        //border = mapboxMap.addPolyline(polylineOptions);
    }


    public void updateSourceLayer() {
        source = new GeoJsonSource(sourceId);
        setCircle(options.getCenter(), options.getRadius());

        mapboxMap.getStyle().addSource(source);
        singleCircleLayer = new FillLayer(layerId, sourceId);

        setFillColor(options.getFillColor());
        setStrokeColor(options.getStrokeColor());
        maplibreMapDelegate.updateLayerByZIndex((int) (options.getZIndex()), singleCircleLayer);
    }

    @Override
    public void remove() {
        maplibreMapDelegate.onSingleCircleRemove(this);
        if (border != null) {
            mapboxMap.removePolyline(border);
        }
    }

    @Override
    public void setCircle(DJILatLng center, Double radius) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.center(center);
        options.radius(radius);
        Point point = Point.fromLngLat(center.getLongitude(), center.getLatitude(), center.getAltitude());
        Polygon polygon = MaplibreUtils.getCircle(point, radius);

        source.setGeoJson(polygon);

        //Mapbox不能设置边线宽度，太窄了，所以这里通过自己添加边的方式实现
        ArrayList<LatLng> pointsList = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            LatLng latLng = new LatLng();
            latLng.setLatitude(polygon.coordinates().get(0).get(i).latitude());
            latLng.setLongitude(polygon.coordinates().get(0).get(i).longitude());
            pointsList.add(latLng);
        }
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(pointsList)
                .add(pointsList.get(0))
                .color(options.getStrokeColor())
                .width(options.getStrokeWidth() / 5f);
        borderAlpha = polylineOptions.getAlpha();
        border = mapboxMap.addPolyline(polylineOptions);

    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        if (border != null) {
            border.setWidth(strokeWidth);
        }
    }

    @Override
    public float getStrokeWidth() {
        if (border != null) {
            return border.getWidth();
        }
        return 0;
    }

    @Override
    public void setVisible(boolean visible) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        if (visible) {
            singleCircleLayer.setProperties(
                    PropertyFactory.visibility(VISIBLE)
            );
            if (border != null) {
                border.setAlpha(borderAlpha);
            }
        } else {
            singleCircleLayer.setProperties(
                    PropertyFactory.visibility(NONE)
            );
            if (border != null) {
                border.setAlpha(NO_ALPHA);
            }
        }
    }

    @Override
    public boolean isVisible() {
        boolean visible;
        if (singleCircleLayer.getVisibility().value.equals(VISIBLE)) {
            visible = true;
        } else {
            visible = false;
        }
        return visible;
    }

    @Override
    public void setCenter(DJILatLng center) {
        options.center(center);
        setCircle(center, getRadius());
    }

    @Override
    public DJILatLng getCenter() {
        return options.getCenter();
    }

    @Override
    public void setRadius(double radius) {
        options.radius(radius);
        setCircle(getCenter(), radius);
    }

    @Override
    public double getRadius() {
        return options.getRadius();
    }

    @Override
    public void setFillColor(@ColorInt int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        // color和alpha需要分开设置，否则在西班牙语、法语、德语等某些语言的情况下，画出的圆会是黑色的。英语情况下则没问题
        // 很奇怪，已经联系mapbox解决，我们这里先采用这种workaround
        int fillColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        float alpha = Color.alpha(color) / 255f;
        singleCircleLayer.setProperties(
                PropertyFactory.fillColor(fillColor), PropertyFactory.fillOpacity(alpha)
        );
    }

    @Override
    public int getFillColor() {
        return singleCircleLayer.getFillColorAsInt();
    }

    @Override
    public void setStrokeColor(@ColorInt int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        singleCircleLayer.setProperties(
                PropertyFactory.fillOutlineColor(color)
        );
        if (border != null) {
            border.setColor(color);
        }
    }

    @Override
    public int getStrokeColor() {
        if (border != null) {
            return border.getColor();
        }
        return singleCircleLayer.getFillOutlineColorAsInt();
    }

    @Override
    public void setZIndex(float zIndex) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.zIndex(zIndex);
        maplibreMapDelegate.updateLayerByZIndex((int) zIndex, this.singleCircleLayer);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }


    public String getSourceId() {
        return sourceId;
    }

    public String getLayerId() {
        return layerId;
    }
}
