package dji.v5.ux.mapkit.maplibre.annotations;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircle;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircleOptions;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapDelegate;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;

/**
 * Created by dickensdai 10/9/18
 * Mapbox的GRoupCircle代理类
 */
public class MGroupCircle implements DJIGroupCircle {

    private MapboxMap mapboxMap;
    private FillLayer groupCircleLayer;
    private GeoJsonSource source;
    private MaplibreMapDelegate maplibreMapDelegate;
    private float zindex;
    private DJIGroupCircleOptions options;

    private String sourceId;
    private String layerId;
    private int size;

    private MGroupCircle() {

    }

    public MGroupCircle(MaplibreMapDelegate maplibreMapDelegate,
                        MapboxMap mapboxMap,
                        FillLayer groupCircleLayer,
                        GeoJsonSource source,
                        DJIGroupCircleOptions options) {
        this.maplibreMapDelegate = maplibreMapDelegate;
        this.mapboxMap = mapboxMap;
        this.groupCircleLayer = groupCircleLayer;
        this.source = source;
        this.sourceId = source.getId();
        this.layerId = groupCircleLayer.getId();
        this.options = options;
    }

    public void updateSourceLayer() {
        source = new GeoJsonSource(sourceId);
        setCircles(options.getCenters(), options.getRadius());

        mapboxMap.getStyle().addSource(source);
        groupCircleLayer = new FillLayer(layerId, sourceId);

        maplibreMapDelegate.updateLayerByZIndex((int) options.getZIndex(), groupCircleLayer);
    }

    @Override
    public void remove() {
        maplibreMapDelegate.onGroupCircleRemove(this);
    }

    @Override
    public void setCircles(List<DJILatLng> centers, List<Double> radius) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        if(centers.size() != radius.size()) {
            return;
        }
        size = centers.size();
        options.radius(radius);
        options.centers(centers);
        List<Feature> features = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            Point point = Point.fromLngLat(centers.get(i).getLongitude(), centers.get(i).getLatitude());
            Polygon center = MaplibreUtils.getCircle(point, radius.get(i));
            features.add(Feature.fromGeometry(center));
        }
        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
        source.setGeoJson(featureCollection);
    }

    @Override
    public void setVisible(boolean visible) {
        if(maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        if(visible) {
            groupCircleLayer.setProperties(
                    PropertyFactory.visibility(VISIBLE)
            );
        }else {
            groupCircleLayer.setProperties(
                    PropertyFactory.visibility(NONE)
            );
        }
    }

    @Override
    public boolean isVisible() {
        boolean visible;
        if (groupCircleLayer.getVisibility().value.equals(VISIBLE)){
            visible = true;
        } else {
            visible = false;
        }
        return visible;
    }

    @Override
    public void setZIndex(float zIndex) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.zIndex(zindex);
        maplibreMapDelegate.updateLayerByZIndex((int)zIndex, this.groupCircleLayer);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    @Override
    public void setFillColor(int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.fillColor(color);
        groupCircleLayer.setProperties(
                PropertyFactory.fillColor(color)
        );
    }

    @Override
    public void setStrokeColor(int color) {
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        options.strokeColor(color);
        groupCircleLayer.setProperties(
                PropertyFactory.fillOutlineColor(color)
        );
    }

    @Override
    public DJIGroupCircleOptions getOptions() {
        return null;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getLayerId() {
        return layerId;
    }
}
