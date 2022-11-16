package dji.v5.ux.mapkit.core.maps;

import dji.v5.ux.mapkit.core.callback.OnCameraChangeListener;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nathan on 2018/5/26.
 * Description:
 * <p>
 * Copyright (c) 2018. DJI All Rights Reserved.
 */
public abstract class DJIBaseMap implements DJIMap {


    /**
     * {@linkplain OnCameraChangeListener} 列表
     */
    protected List<OnCameraChangeListener> onCameraChangeListeners = new LinkedList<>();

    /**
     * {@linkplain OnMarkerClickListener} 列表
     */
    protected List<OnMarkerClickListener> onMarkerClickListeners = new LinkedList<>();


    /**
     * {@linkplain OnMapLongClickListener} 列表
     */
    protected List<OnMapLongClickListener> onMapLongClickListeners = new LinkedList<>();

    /**
     * {@linkplain OnInfoWindowClickListener} 列表
     */
    protected List<OnInfoWindowClickListener> onInfoWindowClickListeners = new LinkedList<>();


    protected List<OnMarkerDragListener> onMarkerDragListeners = new LinkedList<>();


    protected List<OnMapClickListener> onMapClickListeners = new LinkedList<>();

    protected boolean onMarkerClick(DJIMarker marker) {
        for (OnMarkerClickListener listener: onMarkerClickListeners){
            listener.onMarkerClick(marker);
        }
        return true;
    }

    protected void onMapClick(DJILatLng latLng) {

        for (OnMapClickListener listener: onMapClickListeners){
            listener.onMapClick(latLng);
        }

    }

    protected void onMapLongClick(DJILatLng latLng) {

        for (OnMapLongClickListener listener: onMapLongClickListeners){
            listener.onMapLongClick(latLng);
        }

    }

    protected void onInfoWindowClick(DJIMarker marker) {

        for (OnInfoWindowClickListener listener: onInfoWindowClickListeners){
            listener.onInfoWindowClick(marker);
        }

    }

    protected void onMarkerDragStart(DJIMarker marker) {
        for (OnMarkerDragListener listener: onMarkerDragListeners){
            listener.onMarkerDragStart(marker);
        }
    }


    protected void onMarkerDrag(DJIMarker marker) {
        for (OnMarkerDragListener listener: onMarkerDragListeners){
            listener.onMarkerDrag(marker);
        }
    }


    protected void onMarkerDragEnd(DJIMarker marker) {
        for (OnMarkerDragListener listener: onMarkerDragListeners){
            listener.onMarkerDragEnd(marker);
        }
    }

    protected void onCameraChange(DJICameraPosition cameraPosition) {
        for (OnCameraChangeListener listener: onCameraChangeListeners){
            listener.onCameraChange(cameraPosition);
        }
    }

    protected void onCameraChangeFinish(DJICameraPosition cameraPosition) {
        for (OnCameraChangeListener listener: onCameraChangeListeners){
            listener.onCameraChange(cameraPosition);
        }
    }


    @Override
    public void setOnMarkerClickListener(OnMarkerClickListener listener) {
        if (listener != null && !onMarkerClickListeners.contains(listener)) {
            onMarkerClickListeners.add(listener);
        }
    }

    @Override
    public void removeOnMarkerClickListener(OnMarkerClickListener listener) {
        onMarkerClickListeners.remove(listener);
    }

    @Override
    public void setOnMapClickListener(OnMapClickListener listener) {
        if (listener != null && !onMapClickListeners.contains(listener)) {
            onMapClickListeners.add(listener);
        }
    }

    @Override
    public void removeOnMapClickListener(OnMapClickListener listener) {
        onMapClickListeners.clear();
    }

    @Override
    public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener) {
        if (listener != null && !onInfoWindowClickListeners.contains(listener)) {
            onInfoWindowClickListeners.add(listener);
        }
    }

    @Override
    public void setOnMarkerDragListener(final OnMarkerDragListener listener) {

        if (listener != null && !onMarkerDragListeners.contains(listener)) {
            onMarkerDragListeners.add(listener);
        }
    }

    @Override
    public void removeOnMarkerDragListener(OnMarkerDragListener listener) {
        onMarkerDragListeners.remove(listener);
    }

    @Override
    public void removeAllOnMarkerDragListener() {
        onMarkerDragListeners.clear();
    }

    @Override
    public void setOnMapLongClickListener(final OnMapLongClickListener listener) {

        if (listener != null && !onMapLongClickListeners.contains(listener)) {
            onMapLongClickListeners.add(listener);
        }

    }

    @Override
    public void removeOnMapLongClickListener(OnMapLongClickListener listener) {
        onMapLongClickListeners.remove(listener);
    }

    @Override
    public void removeAllOnMapLongClickListener() {
        onMapLongClickListeners.clear();
    }

    @Override
    public void setOnCameraChangeListener(OnCameraChangeListener listener) {
        if (listener != null && !onCameraChangeListeners.contains(listener)) {
            onCameraChangeListeners.add(listener);
        }
    }

    @Override
    public void removeOnCameraChangeListener(OnCameraChangeListener listener) {
        onCameraChangeListeners.remove(listener);
    }

    @Override
    public void removeAllOnCameraChangeListeners() {
        onCameraChangeListeners.clear();
    }

    @Override
    public void removeAllOnMarkerClickListener() {
        onMarkerDragListeners.clear();
    }

    @Override
    public void removeAllOnMapClickListener() {
        onMapClickListeners.clear();
    }

}
