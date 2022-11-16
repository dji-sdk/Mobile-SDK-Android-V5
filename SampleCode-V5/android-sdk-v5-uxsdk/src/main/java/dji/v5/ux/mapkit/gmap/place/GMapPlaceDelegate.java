package dji.v5.ux.mapkit.gmap.place;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;


import dji.v5.ux.mapkit.core.models.DJILatLng;

import dji.v5.ux.mapkit.core.places.DJIPlacesClient;
import dji.v5.ux.mapkit.core.places.DJIPoiItem;
import dji.v5.ux.mapkit.core.places.DJIPoiSearchQuery;

import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Created by joeyang on 10/10/17.
 */
public class GMapPlaceDelegate implements IInternalPlacesClient, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;
    private DJIPlacesClient.OnPoiSearchListener onPoiSearchListener;
    private Context context;

    /**
     * Reverse geocoding search results listener.
     */
    // private DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener;

    public GMapPlaceDelegate(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        this.context = context;
    }

    @Override
    public void setOnPoiSearchListener(DJIPlacesClient.OnPoiSearchListener onPoiSearchListener) {
        this.onPoiSearchListener = onPoiSearchListener;
    }

    @Override
    public void setPoiSearchQuery(DJIPoiSearchQuery poiSearchQuery) {
        //do something
    }

    @Override
    public void setOnRegeocodeSearchListener(DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener) {
      //  this.onRegeocodeSearchListener = onRegeocodeSearchListener;
    }

    @Override
    public void regeocodeSearchAsyn(DJILatLng latLng) {
        //geocode search
    }

    /**
     * 谷歌地图的api只能获取设备当前位置附近的poi，因此参数{@param latLng}没有用。可以使用 Google Places Web Service API
     * <h>https://developers.google.com/places/web-service/search</h>
     *
     * @param latLng
     */
    @Override
    public void searchPOIAsyn(DJILatLng latLng) {
        searchPOIAsyn(latLng, POI_RADIUS);
    }

    @Override
    public void searchPOIAsyn(DJILatLng latLng, int radius) {
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(googleApiClient, null);
        result.setResultCallback(placeLikelihoods -> {
            List<DJIPoiItem> poiList = new ArrayList<>(placeLikelihoods.getCount());
            for (PlaceLikelihood placeLikelihood : placeLikelihoods) {
                Place place = placeLikelihood.getPlace();
                LatLng latLng = place.getLatLng();
                DJILatLng ll = GoogleUtils.fromLatLng(latLng);
                DJIPoiItem item = new DJIPoiItem(place.getName().toString(),
                        place.getAddress().toString(), ll);
                poiList.add(item);
            }
            if (onPoiSearchListener != null) {
                onPoiSearchListener.onPoiSearched(poiList);
            }
            googleApiClient.disconnect();
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        //do something
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //do something
    }

}
