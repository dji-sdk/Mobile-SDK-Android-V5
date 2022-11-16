package dji.v5.ux.mapkit.maplibre.annotations;

import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapDelegate;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 12/9/17.
 */
public class MLineLayerPolyline implements DJIPolyline {

    private MapboxMap mapboxMap;
    private LineLayer lineLayer;
    private GeoJsonSource source;
    private MaplibreMapDelegate maplibreMapDelegate;
    private DJIPolylineOptions options;

    private String sourceId;
    private String layerId;
    private final List<DJILatLng> pointsCache = new ArrayList<>();

    private MLineLayerPolyline() {

    }

    public MLineLayerPolyline(MaplibreMapDelegate maplibreMapDelegate,
                              MapboxMap mapboxMap,
                              LineLayer lineLayer,
                              GeoJsonSource source,
                              DJIPolylineOptions options) {
        this.maplibreMapDelegate = maplibreMapDelegate;
        this.mapboxMap = mapboxMap;
        this.lineLayer = lineLayer;
        this.source = source;
        lineLayer.setProperties(
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        );
        this.sourceId = source.getId();
        this.layerId = lineLayer.getId();
        this.options = options;
    }

    public void updateSourceLayer() {
        source = new GeoJsonSource(sourceId);
        options.setPoints(this.pointsCache);
        setPoints(options.getPoints());

        mapboxMap.getStyle().addSource(source);
        lineLayer = new LineLayer(layerId, sourceId);

        setWidth(options.getWidth());
        setColor(options.getColor());
        maplibreMapDelegate.updateLayerByZIndex((int)(options.getZIndex()), lineLayer);
    }

    @Override
    public void remove() {
        maplibreMapDelegate.onPolylineRemove(this);
    }

    @Override
    public void setWidth(float width) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        lineLayer.setProperties(
                PropertyFactory.lineWidth(width / 5f)
        );
    }

    @Override
    public float getWidth() {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return 0f;
        }
        PropertyValue<Float> propertyValue = lineLayer.getLineWidth();
        return propertyValue.getValue();
    }

    @Override
    public void setPoints(List<DJILatLng> points) {
        // 在Stopping world时，外部调用此方法时，所传进来的points并没有被保存
        pointsCache.clear();
        pointsCache.addAll(points);
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.setPoints(points);
        List<Point> positionList = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            Point position = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
            positionList.add(position);
        }

        LineString lineString = LineString.fromLngLats(positionList);
        FeatureCollection featureCollection =
                FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(lineString)});

        source.setGeoJson(featureCollection);
    }

    @Override
    public List<DJILatLng> getPoints() {
        return options.getPoints();
    }

    @Override
    public void setColor(@ColorInt int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        lineLayer.setProperties(
                PropertyFactory.lineColor(color)
        );
    }

    @Override
    public int getColor() {
        return lineLayer.getLineColorAsInt();
    }

    @Override
    public void setZIndex(float zIndex) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.zIndex(zIndex);
        maplibreMapDelegate.updateLayerByZIndex((int)zIndex, this.lineLayer);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    public DJIPolylineOptions getOptions() {
        return null;
    }

    public void setOptions(DJIPolylineOptions options) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        this.options = options;
        List<Point> positionList = new ArrayList<>(options.getPoints().size());
        for (DJILatLng latLng : options.getPoints()) {
            Point position = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
            positionList.add(position);
        }

        LineString lineString = LineString.fromLngLats(positionList);
        source.setGeoJson(lineString);
        setColor(options.getColor());
        setWidth(options.getWidth());
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getLayerId() {
        return layerId;
    }
}
