package dji.v5.ux.mapkit.gmap.provider;

import static dji.v5.ux.mapkit.core.Mapkit.MapProviderConstant.GOOGLE_MAP_PROVIDER;

import android.content.Context;
import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.exceptions.MapkitInitializerException;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.core.providers.MapProvider;
import dji.v5.ux.mapkit.gmap.map.GMapView;
import dji.v5.ux.mapkit.gmap.place.GMapPlaceDelegate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;



public class GoogleProvider extends MapProvider {

    // private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 69;

    public GoogleProvider() {
        providerType = GOOGLE_MAP_PROVIDER;
    }

    @Override
    protected DJIMapViewInternal requestMapView(@NonNull Context context,
                                                @NonNull MapkitOptions mapkitOptions) {
        DJIMapViewInternal mapView = null;
        Mapkit.mapType(mapkitOptions.getMapType());
        Mapkit.mapProvider(providerType);
        mapView = new GMapView(context);
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (result != ConnectionResult.SUCCESS) {
            //if (googleApiAvailability.isUserResolvableError(result)) {
            //    googleApiAvailability.getErrorDialog(context, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            //}
            throw new MapkitInitializerException(GOOGLE_MAP_PROVIDER);
        }
        return mapView;
    }

    @Override
    protected IInternalPlacesClient requestGeocodingClient(Context context, MapkitOptions mapkitOptions) {
        IInternalPlacesClient client = null;
//            Mapkit.setOptions(mapkitOptions);
//            Mapkit.mapProvider(GOOGLE_MAP_PROVIDER);
        Mapkit.geocodingProvider(getProviderType());
        client = new GMapPlaceDelegate(context);
        return client;
    }
}
