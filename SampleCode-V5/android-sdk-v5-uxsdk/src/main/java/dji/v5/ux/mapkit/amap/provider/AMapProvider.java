package dji.v5.ux.mapkit.amap.provider;

import static dji.v5.ux.mapkit.core.Mapkit.MapProviderConstant.AMAP_PROVIDER;

import android.content.Context;
import android.os.RemoteException;
import androidx.annotation.NonNull;

import com.amap.api.maps.MapsInitializer;

import dji.v5.ux.mapkit.amap.map.AMapView;
import dji.v5.ux.mapkit.amap.place.AMapPlaceDelegate;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.exceptions.MapkitInitializerException;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.core.providers.MapProvider;



public class AMapProvider extends MapProvider {

    public AMapProvider() {
        providerType = AMAP_PROVIDER;
    }

    @Override
    protected DJIMapViewInternal requestMapView(@NonNull Context context,
                                                @NonNull MapkitOptions mapkitOptions) {
        DJIMapViewInternal mapView = null;
        Mapkit.mapType(mapkitOptions.getMapType());
        Mapkit.mapProvider(AMAP_PROVIDER);
        mapView = new AMapView(context);
        try {
            MapsInitializer.initialize(context);
        } catch (RemoteException e) {
            throw new MapkitInitializerException(AMAP_PROVIDER);
        }
        return mapView;
    }

    @Override
    protected IInternalPlacesClient requestGeocodingClient(Context context, MapkitOptions mapkitOptions) {
        IInternalPlacesClient client = null;
        Mapkit.geocodingProvider(getProviderType());
        client = new AMapPlaceDelegate(context);
        return client;
    }
}
