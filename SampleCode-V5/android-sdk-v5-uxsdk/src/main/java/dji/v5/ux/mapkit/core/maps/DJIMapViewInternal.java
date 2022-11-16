package dji.v5.ux.mapkit.core.maps;

import android.os.Bundle;

//Doc key: DJIMap_DJIMapViewInternal
/**
 * A view which displays a map.
 *
 * All the life cycle methods must be forwarded from the Activity or Fragment containing this view
 * to the corresponding ones in this class. In particular, the following methods must be forwarded:
 *
 * <ul>
 * <li>`onCreate`</li>
 * <li>`onStart`</li>
 * <li>`onResume`</li>
 * <li>`onPause`</li>
 * <li>`onStop`</li>
 * <li>`onDestroy`</li>
 * <li>`onSaveInstanceState`</li>
 * <li>`onLowMemory`</li>
 * </ul>
 */
public interface DJIMapViewInternal {

    //Doc key: DJIMap_DJIMapViewInternal_onCreate
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     *
     * @param saveInstanceState Bundle which contains the saved instance state.
     */
    void onCreate(Bundle saveInstanceState);

    //Doc key: DJIMap_DJIMapViewInternal_onStart
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onStart();

    //Doc key: DJIMap_DJIMapViewInternal_onResume
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onResume();

    //Doc key: DJIMap_DJIMapViewInternal_onPause
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onPause();

    //Doc key: DJIMap_DJIMapViewInternal_onStop
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onStop();

    //Doc key: DJIMap_DJIMapViewInternal_onDestroy
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onDestroy();

    //Doc key: DJIMap_DJIMapViewInternal_onSaveInstanceState
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     *
     * @param outState Bundle in which to place your saved state.
     */
    void onSaveInstanceState(Bundle outState);

    //Doc key: DJIMap_DJIMapViewInternal_onLowMemory
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    void onLowMemory();

    //Doc key: DJIMap_DJIMapViewInternal_getDJIMapAsync
    /**
     * Initializes the map view.
     *
     * @param callback The callback that will be invoked when the map is ready to be used.
     */
    void getDJIMapAsync(DJIMapView.OnDJIMapReadyCallback callback);
}
