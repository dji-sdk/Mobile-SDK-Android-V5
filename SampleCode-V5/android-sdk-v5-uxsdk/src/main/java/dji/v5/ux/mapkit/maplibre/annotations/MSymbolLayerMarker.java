package dji.v5.ux.mapkit.maplibre.annotations;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptorFactory;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarkerOptions;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapDelegate;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

/**
 * Created by joeyang on 11/5/17.
 */
public class MSymbolLayerMarker extends DJIMarker {

    private MapboxMap mapboxMap;
    private GeoJsonSource source;
    private SymbolLayer symbolLayer;
    private Context context;
    private MaplibreMapDelegate maplibreMapDelegate;
    private DJIBitmapDescriptor bitmapDescriptor;
//    private Float anchorU;
//    private Float anchorV;
    private Marker shadowMarker;
    private boolean isInfoWindowClosed;
    private boolean draggable;
    private boolean visibleCache = true;
    private String title;

    /**
     * 保存sourceId
     */
    private String sourceId;

    /**
     * 保存layerId
     */
    private String layerId;

    private DJIMarkerOptions markerOptions;

    public MSymbolLayerMarker(MaplibreMapDelegate maplibreMapDelegate, MapboxMap mapboxMap, GeoJsonSource geoJsonSource, SymbolLayer symbolLayer, Marker shadowMarker, Context context, DJIMarkerOptions markerOptions) {
        this.mapboxMap = mapboxMap;
        this.source = geoJsonSource;
        this.symbolLayer = symbolLayer;
        this.context = context;
        this.maplibreMapDelegate = maplibreMapDelegate;
        this.shadowMarker = shadowMarker;
        this.sourceId = this.source.getId();
        this.layerId = this.symbolLayer.getId();
        this.markerOptions = markerOptions;
    }

    public void updateSourceLayer() {
        source = new GeoJsonSource(sourceId);
        setPosition(positionCache);

        symbolLayer = new SymbolLayer(layerId, sourceId);
        mapboxMap.getStyle().addSource(source);

        setIcon(bitmapDescriptor);
        setRotation(markerOptions.getRotation());
        setTitle(markerOptions.getTitle());
        setAnchor(markerOptions.getAnchorU(), markerOptions.getAnchorV());
        setVisible(this.visibleCache); // 地图加载完成后，设置visible
        maplibreMapDelegate.updateLayerByZIndex(markerOptions.getZIndex(), symbolLayer);
//        mapboxMap.addLayer(symbolLayer);
    }

    public Marker getShadowMarker() {
        return shadowMarker;
    }

    /**
     * 1. 将latLng转换成Position
     * 2. 将position转换成point
     * 3. 将point加入到source[GeoJsonSource]
     * 3. 将source加入到 mapboxMap
     * @param latLng
     */
    @Override
    public void setPosition(DJILatLng latLng) {
        markerOptions.position(latLng);
        setPositionCache(latLng);
        if (source == null) {
            return;
        }
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        Point point = Point.fromLngLat(latLng.getLongitude(),latLng.getLatitude(),latLng.getAltitude());
        source.setGeoJson(point);

        shadowMarker.setPosition(MaplibreUtils.fromDJILatLng(latLng));
//        // 将该位置加入到 mapboxMap
    }

    /**
     * 给该 symbolLayer 设置旋转
     * @param rotate
     */
    @Override
    public void setRotation(float rotate) {
        setRotationCache(rotate);
        markerOptions.rotation(rotate);
        if (symbolLayer == null) {
            return;
        }

        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        symbolLayer.setProperties(PropertyFactory.iconRotate(rotate));
//        symbolLayer.setProperties(PropertyFactory.iconRotationAlignment("map"));
    }

    /**
     * 1. 将bitmap加入到 mapboxMap
     * 2. 将该 id 的 iconImage 加入到 symbolLayer
     * @param icon {@link DJIBitmapDescriptor}
     */
    @Override
    public void setIcon(DJIBitmapDescriptor icon) {
        if (symbolLayer == null) {
            return;
        }
        if (icon == bitmapDescriptor) {
            symbolLayer.setProperties(PropertyFactory.iconImage(icon.getId()));
            symbolLayer.setProperties(PropertyFactory.iconAllowOverlap(true));
            return;
        }
        if (maplibreMapDelegate.isStoppingWorld()) {
            // onStyleLoaded后，调用updateSourceLayer需要bitmapDescriptor，
            // 否则会出现空指针异常
            if (bitmapDescriptor == null) {
                bitmapDescriptor = icon;
            }
            return;
        }
        if (bitmapDescriptor != null) {
            mapboxMap.getStyle().removeImage(bitmapDescriptor.getId());
        } else {
            /* 需让每一个marker都持有一个新的bitmapDescr对象，否则在多个marker复用同一个DJIBitmapDescriptor资源时
             * 这些marker的bitmapDescr对象都指向了最新创建的那个
             * 从而在更新旧markerIcon时，导致removeImage会把最新的Image删掉。
             */
            bitmapDescriptor = DJIBitmapDescriptorFactory.fromBitmap(icon.getBitmap());
        }
        String bitmapId = maplibreMapDelegate.genMarkerBitmapId();
        icon.setId(bitmapId);
        bitmapDescriptor.setId(bitmapId); // 给当前bitmapDesc对象设置Id。
        markerOptions.icon(bitmapDescriptor);
        Icon i = MaplibreUtils.fromDJIBitmapDescriptor(context, icon);
        bitmapDescriptor.updateBitmap(i.getBitmap());
        mapboxMap.getStyle().addImage(icon.getId(), i.getBitmap());
        symbolLayer.setProperties(PropertyFactory.iconImage(icon.getId()));
        symbolLayer.setProperties(PropertyFactory.iconAllowOverlap(true));
    }

    @Override
    public void setAnchor(float u, float v) {
        if (symbolLayer == null) {
            return;
        }
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        markerOptions.anchor(u, v);
//        anchorU = u;
//        anchorV = v;
        if (bitmapDescriptor != null) {
            Bitmap bitmap = bitmapDescriptor.getBitmap();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap translatedBitmap = Bitmap.createBitmap(width * 2, height * 2, bitmap.getConfig());
            Canvas canvas = new Canvas(translatedBitmap);
            canvas.drawColor(Color.TRANSPARENT);
            canvas.drawBitmap(bitmap, width - width * u, height - height * v, null);
            mapboxMap.getStyle().removeImage(bitmapDescriptor.getId());
            mapboxMap.getStyle().addImage(bitmapDescriptor.getId(), translatedBitmap);
            symbolLayer.setProperties(PropertyFactory.iconImage(bitmapDescriptor.getId()));
        }
    }

    @Override
    public void setTitle(String title) {
        markerOptions.title(title);
        if (symbolLayer == null) {
            return;
        }
        if (maplibreMapDelegate.isStoppingWorld()) {
            return;
        }
        //symbolLayer.setProperties(PropertyFactory.textField(title));
        this.title = title;
    }

    @Override
    public String getTitle() {
        //return shadowMarker.getTitle();
        return title;
    }

    @Override
    public void setVisible(boolean visible) {
        markerOptions.visible(visible);
        if (symbolLayer == null) {
            return;
        }
        this.visibleCache = visible;
        if (maplibreMapDelegate.isStoppingWorld()) {
            // 若此时stop world，应该把这个visible属性Cache起来，等load完再刷新
            return;
        }
        if (visible) {
            symbolLayer.setProperties(PropertyFactory.visibility(Property.VISIBLE));
        } else {
            symbolLayer.setProperties(PropertyFactory.visibility(Property.NONE));
        }

    }

    @Override
    public boolean isVisible() {
        PropertyValue<String> visible = symbolLayer.getVisibility();
        return visible.getValue().equals(Property.VISIBLE);
    }

    @Override
    public void showInfoWindow() {
        mapboxMap.selectMarker(shadowMarker);
        isInfoWindowClosed = false;
    }

    @Override
    public void hideInfoWindow() {
        mapboxMap.deselectMarker(shadowMarker);
        isInfoWindowClosed = true;
    }

    @Override
    public boolean isInfoWindowShown() {
        return !isInfoWindowClosed;
    }

    @Override
    public void remove() {
        mapboxMap.removeMarker(shadowMarker);
        maplibreMapDelegate.onMarkerRemove(shadowMarker);
    }


    @Override
    public void setDraggable(boolean b) {
        draggable = b;
    }

    @Override
    public boolean isDraggable() {
        return draggable;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getLayerId() {
        return layerId;
    }

//    public void setSourceId(String sourceId) {
//        this.sourceId = sourceId;
//    }
//
//    public void setLayerId(String layerId) {
//        this.layerId = layerId;
//    }


    public void setSource(GeoJsonSource source) {
        this.source = source;
    }

    public void setSymbolLayer(SymbolLayer symbolLayer) {
        this.symbolLayer = symbolLayer;
    }
}
