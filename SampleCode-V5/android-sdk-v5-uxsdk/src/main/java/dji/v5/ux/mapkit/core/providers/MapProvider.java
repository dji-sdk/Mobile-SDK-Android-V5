package dji.v5.ux.mapkit.core.providers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.maps.DJIMapView;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;

//Doc key: DJIMap_MapProvider
/**
 * Provides utility methods for creating map views.
 */
public abstract class MapProvider {

    protected static final String TAG = MapProvider.class.getSimpleName();

    protected int providerType;

    protected MapProvider nextProvider;

    protected MapProvider() {
        //do something
    }

    @Mapkit.MapProviderConstant
    public int getProviderType() {
        return providerType;
    }

    public MapProvider getNextProvider() {
        return nextProvider;
    }

    /**
     * 使用mapkit的接口来创建DJIMapView
     *
     * @param context       环境变量
     * @param mapkitOptions
     */
    protected abstract DJIMapViewInternal requestMapView(@NonNull Context context,
                                                         @NonNull MapkitOptions mapkitOptions);

    /**
     * 使用Mapkit的接口来创建{@linkplain IInternalPlacesClient}
     *
     * @param context
     * @param mapkitOptions
     * @return
     */
    protected abstract IInternalPlacesClient requestGeocodingClient(Context context,
                                                                    MapkitOptions mapkitOptions);

    //Doc key: DJIMap_MapProvider_dispatchMapViewRequest
    /**
     * Dispatch a request to create a `DJIMapViewInternal` based on the given MapkitOptions. If
     * MapkitOptions is null, a the default options will be used.
     *
     * @param context       A context object
     * @param mapkitOptions A set of options for initializing the map view.
     * @return A `DJIMapViewInternal` object.
     */
    public DJIMapViewInternal dispatchMapViewRequest(@NonNull Context context,
                                                     @Nullable MapkitOptions mapkitOptions) {
        if (mapkitOptions == null) {
            mapkitOptions = new MapkitOptions.Builder().build();
        }
        DJIMapViewInternal mapView = requestMapView(context, mapkitOptions);
        if (mapView != null) {
            return mapView;
        } else {
            if (nextProvider != null) {
                mapView = nextProvider.dispatchMapViewRequest(context, mapkitOptions);
            }
        }
        return mapView;
    }

    /**
     * 根据业务需要来分发创建{@linkplain IInternalPlacesClient}的请求
     *
     * @param context
     * @param mapkitOptions
     * @return
     */
    public IInternalPlacesClient dispatchGeocodingClientRequest(@NonNull Context context,
                                                                MapkitOptions mapkitOptions) {
        IInternalPlacesClient client = requestGeocodingClient(context, mapkitOptions);
        if (client != null) {
            return client;
        } else {
            if (nextProvider != null) {
                client = nextProvider.dispatchGeocodingClientRequest(context, mapkitOptions);
            }
        }
        return client;
    }

    /**
     * 设置责任链的下一个MapProvider
     *
     * @param nextProvider
     */
    public MapProvider next(MapProvider nextProvider) {
        this.nextProvider = nextProvider;
        return this;
    }
}
