package dji.v5.ux.mapkit.amap.place;

import android.content.Context;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.places.DJIPlacesClient;
import dji.v5.ux.mapkit.core.places.DJIPoiItem;
import dji.v5.ux.mapkit.core.places.DJIPoiSearchQuery;
import dji.v5.ux.mapkit.core.places.DJIRegeocodeResult;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 10/10/17.
 */
public class AMapPlaceDelegate implements IInternalPlacesClient, PoiSearch.OnPoiSearchListener {

    private static final int AMAP_SEARCH_SUCCESS_CODE = 1000;

    private Context context;
    private PoiSearch poiSearch;
    private PoiSearch.Query query;
    private DJIPlacesClient.OnPoiSearchListener onPoiSearchListener;
    private DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener;

    public AMapPlaceDelegate(Context context) {
        this.context = context;
        query = new PoiSearch.Query("", "", "");
        query.setPageSize(10);
        poiSearch = new PoiSearch(context, query);
    }

    @Override
    public void setPoiSearchQuery(DJIPoiSearchQuery poiSearchQuery) {
        query = new PoiSearch.Query(poiSearchQuery.keyWord(), "");
        poiSearch.setQuery(query);
    }

    @Override
    public void setOnRegeocodeSearchListener(DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener) {
        this.onRegeocodeSearchListener = onRegeocodeSearchListener;
    }

    @Override
    public void setOnPoiSearchListener(DJIPlacesClient.OnPoiSearchListener onPoiSearchListener) {
        this.onPoiSearchListener = onPoiSearchListener;
    }

    @Override
    public void searchPOIAsyn(DJILatLng latLng) {
        searchPOIAsyn(latLng, POI_RADIUS);
    }

    @Override
    public void searchPOIAsyn(DJILatLng latLng, int radius) {
        DJILatLng gcjLatLng = DJIGpsUtils.wgs2gcjInChina(latLng);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(gcjLatLng.getLatitude(), gcjLatLng.getLongitude()), radius));
        poiSearch.searchPOIAsyn();
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        if (i == AMAP_SEARCH_SUCCESS_CODE) {
            List<PoiItem> pois = poiResult.getPois();
            List<DJIPoiItem> poiList = new ArrayList<>(pois.size());
            for (PoiItem poiItem : pois) {
                LatLonPoint latLonPoint = poiItem.getLatLonPoint();
                DJILatLng wgsLatLng = DJIGpsUtils.gcj2wgsInChina(new DJILatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()));
                DJIPoiItem item = new DJIPoiItem(poiItem.getTitle(), poiItem.getSnippet(), wgsLatLng);
                poiList.add(item);
            }

            if (onPoiSearchListener != null) {
                onPoiSearchListener.onPoiSearched(poiList);
            }

        } else {
            if (onPoiSearchListener != null) {
                onPoiSearchListener.onPoiSearchFailed();
            }
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
        // no code
    }

    @Override
    public void regeocodeSearchAsyn(DJILatLng latLng) {
        //第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
         RegeocodeQuery querys = new RegeocodeQuery(new LatLonPoint(latLng.getLatitude(), latLng.getLongitude()), POI_RADIUS, GeocodeSearch.AMAP);
         GeocodeSearch geoCoderSearch = new GeocodeSearch(this.context);
         geoCoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
             @Override
             public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int resultId) {
                 if (resultId == 1000) {
                     RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                     DJIRegeocodeResult result = new DJIRegeocodeResult();
                     result.setCountry("中国");
                     result.setRegion(regeocodeAddress.getProvince());
                     result.setCity(regeocodeAddress.getCity());
                     result.setDistrict(regeocodeAddress.getDistrict());
                     result.setStreet(regeocodeAddress.getTownship());
                     result.setSubStreet(regeocodeAddress.getStreetNumber() != null ? regeocodeAddress.getStreetNumber().getStreet() : null);
                     result.setAddress(regeocodeAddress.getFormatAddress());

                     if (onRegeocodeSearchListener != null) {
                         onRegeocodeSearchListener.onSearched(result);
                     }
                 } else {
                     if (onRegeocodeSearchListener != null) {
                         onRegeocodeSearchListener.onSearched(null);
                     }
                 }

             }

             @Override
             public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
                //do something
             }
         });
         geoCoderSearch.getFromLocationAsyn(querys);
    }
}