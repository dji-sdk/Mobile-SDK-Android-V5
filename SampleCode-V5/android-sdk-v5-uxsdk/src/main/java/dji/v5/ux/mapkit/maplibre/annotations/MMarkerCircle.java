package dji.v5.ux.mapkit.maplibre.annotations;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJICircle;
import dji.v5.ux.mapkit.core.models.annotations.DJICircleOptions;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapDelegate;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;

public class MMarkerCircle implements DJICircle {

    private MapboxMap mapboxMap;
    private CircleLayer markerCircleLayer;
    private GeoJsonSource source;
    private MaplibreMapDelegate maplibreMapDelegate;
    private DJICircleOptions options;

    private String sourceId;
    private String layerId;

    private MMarkerCircle() {

    }

    public MMarkerCircle(MaplibreMapDelegate maplibreMapDelegate,
                         MapboxMap mapboxMap,
                         CircleLayer circleMarkerLayer,
                         GeoJsonSource source,
                         DJICircleOptions options) {
        this.maplibreMapDelegate = maplibreMapDelegate;
        this.mapboxMap = mapboxMap;
        this.markerCircleLayer = circleMarkerLayer;
        this.source = source;
        this.sourceId = source.getId();
        this.layerId = circleMarkerLayer.getId();
        this.options = options;
    }


    public void updateSourceLayer() {
        source = new GeoJsonSource(sourceId);
        setCircle(options.getCenter(), options.getRadius());

        mapboxMap.getStyle().addSource(source);
        markerCircleLayer = new CircleLayer(layerId, sourceId);

        setFillColor(options.getFillColor());
        setStrokeColor(options.getStrokeColor());
        maplibreMapDelegate.updateLayerByZIndex((int) (options.getZIndex()), markerCircleLayer);
    }

    @Override
    public void remove() {
        maplibreMapDelegate.onMarkerCircleRemove(this);
    }

    @Override
    public void setCircle(DJILatLng center, Double radius) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.center(center);
        options.radius(radius);
        Point point = Point.fromLngLat(center.getLongitude(), center.getLatitude(), center.getAltitude());
        source.setGeoJson(point);
        markerCircleLayer.setProperties(
                PropertyFactory.circleRadius(radius.floatValue())
        );
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        // No Implementation
    }

    @Override
    public float getStrokeWidth() {
        // No Implementation
        return 0;
    }

    @Override
    public void setVisible(boolean visible) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        if (visible) {
            markerCircleLayer.setProperties(
                    PropertyFactory.visibility(VISIBLE)
            );
        } else {
            markerCircleLayer.setProperties(
                    PropertyFactory.visibility(NONE)
            );
        }
    }

    @Override
    public boolean isVisible() {
        boolean visible;
        if (markerCircleLayer.getVisibility().value.equals(VISIBLE)) {
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
    public void setFillColor(int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.fillColor(color);
        markerCircleLayer.setProperties(
                PropertyFactory.circleColor(color)
        );
    }

    @Override
    public int getFillColor() {
        return markerCircleLayer.getCircleColorAsInt();
    }

    @Override
    public void setStrokeColor(int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.strokeColor(color);
        markerCircleLayer.setProperties(
                PropertyFactory.circleStrokeColor(color)
        );
    }

    @Override
    public int getStrokeColor() {
        return markerCircleLayer.getCircleStrokeColorAsInt();
    }

    @Override
    public void setZIndex(float zIndex) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.zIndex(zIndex);
        maplibreMapDelegate.updateLayerByZIndex((int) zIndex, this.markerCircleLayer);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    public String getSourceID() {
        return sourceId;
    }

    public String getLayerId() {
        return layerId;
    }
}
