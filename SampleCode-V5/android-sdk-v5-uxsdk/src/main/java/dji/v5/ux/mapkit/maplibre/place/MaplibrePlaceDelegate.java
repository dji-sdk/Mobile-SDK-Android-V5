package dji.v5.ux.mapkit.maplibre.place;

import android.content.Context;
import androidx.annotation.NonNull;


import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.places.DJIPlacesClient;
import dji.v5.ux.mapkit.core.places.DJIPoiSearchQuery;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;



import com.mapbox.geojson.Point;




/**
 * Mapbox的Places相关搜索代理类
 * Created by joeyang on 1/12/18.
 */

public class MaplibrePlaceDelegate implements IInternalPlacesClient {

    // private static final boolean GOOGLE_SERVICE_DELEGATE = true;

    /**
     * key word of this search
     */
    // private String keyWord;
    /**
     * Poi search results listener.
     */
    // private DJIPlacesClient.OnPoiSearchListener onPoiSearchListener;

    /**
     * Reverse geocoding search results listener.
     */
    // private DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener;

//    private String accessToken = null;
//    private String poiGeocodingTypes = null;
//    private String [] recodeGeocodingTypes;
//    private int limit = 10;
//    private String languages = null;
//    private Point point;

    /**
     * init the parameters and the builder
     * @param context
     */

    public MaplibrePlaceDelegate() {
       // accessToken = Mapkit.getMapboxAccessToken();
       // languages = MaplibreUtils.getLanguageString(context);
       // keyWord = "";

    }

    @Override
    public void searchPOIAsyn(DJILatLng latLng) {
        searchPOIAsyn(latLng, POI_RADIUS);
    }

    @Override
    public void searchPOIAsyn(final DJILatLng latLng, int radius) {
       // point = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
    }

    @Override
    public void setOnPoiSearchListener(DJIPlacesClient.OnPoiSearchListener onPoiSearchListener) {
       // this.onPoiSearchListener = onPoiSearchListener;
    }

    @Override
    public void setPoiSearchQuery(DJIPoiSearchQuery poiSearchQuery) {
       // keyWord = poiSearchQuery.keyWord();
    }

    @Override
    public void setOnRegeocodeSearchListener(DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener) {
      //  this.onRegeocodeSearchListener = onRegeocodeSearchListener;
    }

    @Override
    public void regeocodeSearchAsyn(DJILatLng latLng) {
       // geoSearch
    }
}
