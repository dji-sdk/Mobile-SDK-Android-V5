package dji.v5.ux.mapkit.amap.map;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amap.api.maps.TextureMapView;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.maps.DJIMapView;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;

/**
 * Created by joeyang on 5/30/17.
 */
public class AMapView extends TextureMapView implements DJIMapViewInternal {

    public AMapView(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        //do something
    }

    @Override
    public void onStop() {
        //do something
    }

    @Override
    public void getDJIMapAsync(@NonNull final DJIMapView.OnDJIMapReadyCallback callback) {
        AMapDelegate amap = new AMapDelegate(getMap());
        int mapType = Mapkit.getMapType();
        amap.setMapType(mapType);
        callback.onDJIMapReady(amap);
    }
}
