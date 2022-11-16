package dji.v5.ux.mapkit.gmap.map;

import android.content.Context;
import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.maps.DJIMapView;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

/**
 * Created by joeyang on 5/23/17.
 */
public class GMapView extends MapView implements DJIMapViewInternal {

    public GMapView(Context context) {
        super(context);
    }


    @Override
    public void getDJIMapAsync(@NonNull final DJIMapView.OnDJIMapReadyCallback callback) {
        getMapAsync(googleMap -> {
            GMapDelegate map = new GMapDelegate(googleMap);
//                int mapType = Mapkit.getOptions().getMapType();
            int mapType = Mapkit.getMapType();
            map.setMapType(mapType);
            callback.onDJIMapReady(map);
        });
    }
}
