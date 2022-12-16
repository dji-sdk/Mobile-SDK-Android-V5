package dji.v5.ux.mapkit.core.places;

import android.content.Context;
import androidx.annotation.NonNull;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.providers.MapProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by joeyang on 1/25/18.
 */

public class DJIPlacesClient {

    // private static final int AMAP_SEARCH_SUCCESS_CODE = 1000;
    private static final String TAG = LogUtils.getTag(DJIPlacesClient.class.getSimpleName());
    /**
     * poi的搜索半径，单位是米
     */
    // private static final int POI_RADIUS = 300;

    private IInternalPlacesClient client;

    public DJIPlacesClient(@NonNull Context context, @NonNull MapkitOptions mapkitOptions) {
        List<Integer> providerList = mapkitOptions.getProviderList();
        for (int i = 0; i < providerList.size(); i++) {
            String className = Mapkit.getMapProviderClassName(providerList.get(i));
            try {
                Class<?> c = Class.forName(className);
                Constructor<?> constructor = c.getConstructor();
                Object object = constructor.newInstance();
                client = ((MapProvider) object).dispatchGeocodingClientRequest(context, mapkitOptions);
                if (client != null) {
                    break;
                }
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException | ClassNotFoundException | NoClassDefFoundError e) {
                LogUtils.e(TAG, e.getMessage());
            }
        }
    }

    public void setOnPoiSearchListener(OnPoiSearchListener onPoiSearchListener) {
        client.setOnPoiSearchListener(onPoiSearchListener);
    }

    public void setPoiSearchQuery(DJIPoiSearchQuery poiSearchQuery) {
        client.setPoiSearchQuery(poiSearchQuery);
    }

    public void searchPOIAsyn(DJILatLng latLng) {
        client.searchPOIAsyn(latLng);
    }

    public void searchPOIAsyn(DJILatLng latLng, int radius) {
        client.searchPOIAsyn(latLng, radius);
    }

    public void regeocodeSearchAsyn(DJILatLng latLng) {
        client.regeocodeSearchAsyn(latLng);
    }

    public void setOnRegeocodeSearchListener(OnRegeocodeSearchListener onRegeocodeSearchListener) {
        client.setOnRegeocodeSearchListener(onRegeocodeSearchListener);
    }

    public interface OnPoiSearchListener {
        void onPoiSearched(List<DJIPoiItem> pois);

        void onPoiSearchFailed();
    }

    public interface OnRegeocodeSearchListener {
        void onSearched(DJIRegeocodeResult result);
    }
}
