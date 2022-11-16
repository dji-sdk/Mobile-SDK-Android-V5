package dji.v5.ux.mapkit.core.maps;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.constants.MapkitConstants;

import java.util.ArrayList;
import java.util.List;

public class DJIMapFragment extends Fragment implements DJIMapView.OnDJIMapReadyCallback {

    private DJIMapView mapView;
    private DJIMap djiMap;
    private List<DJIMapView.OnDJIMapReadyCallback> mapReadyCallbackList = new ArrayList<>();

    public static DJIMapFragment newInstance(MapkitOptions mapkitOptions) {
        DJIMapFragment fragment = new DJIMapFragment();
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList(MapkitConstants.KEY_SUPPORT_PROVIDER_LIST, (ArrayList<Integer>) mapkitOptions.getProviderList());
        bundle.putInt(MapkitConstants.KEY_SUPPORT_MAP_TYPE, mapkitOptions.getMapType());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Context context = inflater.getContext();
        mapView = new DJIMapView((Activity) context, parseBundle(getArguments()));
        return mapView;
    }

    private MapkitOptions parseBundle(Bundle arguments) {
        int mapType = arguments.getInt(MapkitConstants.KEY_SUPPORT_MAP_TYPE);
        ArrayList<Integer> supportList = arguments.getIntegerArrayList(MapkitConstants.KEY_SUPPORT_PROVIDER_LIST);
        MapkitOptions.Builder builder = new MapkitOptions.Builder();
        builder.mapType(mapType);
        if (supportList != null && !supportList.isEmpty()) {
            for (int provider : supportList) {
                builder.addMapProvider(provider);
            }
        }
        return builder.build();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView.onCreate(savedInstanceState);
        mapView.getDJIMapAsync(this);
    }

    @Override
    public void onDJIMapReady(DJIMap map) {
        this.djiMap = map;
        for (DJIMapView.OnDJIMapReadyCallback onMapReadyCallback : mapReadyCallbackList) {
            onMapReadyCallback.onDJIMapReady(djiMap);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
        mapReadyCallbackList.clear();
    }

    public void getDJIMapAsync(DJIMapView.OnDJIMapReadyCallback onDJIMapReadyCallback) {
        if (djiMap == null) {
            mapReadyCallbackList.add(onDJIMapReadyCallback);
        } else {
            onDJIMapReadyCallback.onDJIMapReady(djiMap);
        }
    }

}
