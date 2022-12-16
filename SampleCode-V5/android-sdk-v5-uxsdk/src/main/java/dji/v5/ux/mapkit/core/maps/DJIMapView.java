package dji.v5.ux.mapkit.core.maps;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;

import dji.v5.ux.mapkit.core.maps.DJIEmptyMapView;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import dji.v5.ux.mapkit.core.providers.MapProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

//Doc key: DJIMap_DJIMapView
/**
 * A View that contains a `DJIMapViewInternal` and initializes it based on the given
 * defaultProvider attribute. If no attribute is given, the provider will be AMaps.
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
public class DJIMapView extends FrameLayout {

    private static final String TAG = LogUtils.getTag(DJIMapView.class.getSimpleName());
    private DJIMapViewInternal internalMapView;
    // private static final int MAP_PROVIDER_GOOGLEMAP = 0x1;
    private static final int MAP_PROVIDER_AMAP = 0x2;
    //private static final int MAP_PROVIDER_MAPBOX = 0x3;

    public DJIMapView(@NonNull Context context) {
        this(context, null);
    }

    public DJIMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DJIMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DJIMapView);
        int defaultProvider = typedArray.getInt(R.styleable.DJIMapView_defaultProvider, MAP_PROVIDER_AMAP);
        MapkitOptions.Builder builder = new MapkitOptions.Builder();
        builder.addMapProvider(defaultProvider);
        initialise((Activity) context, builder.build());
        typedArray.recycle();
    }

    public DJIMapView(Activity activity, MapkitOptions options) {
        super(activity);
        initialise(activity, options);
    }

    private void initialise(@NonNull final Activity activity, @NonNull final MapkitOptions options) {
        List<Integer> providerList = options.getProviderList();
        for (int i = 0; i < providerList.size(); i++) {
            String className = Mapkit.getMapProviderClassName(providerList.get(i));
            try {
                Class<?> c = Class.forName(className);
                Constructor<?> constructor = c.getConstructor();
                Object object = constructor.newInstance();
                internalMapView = ((MapProvider) object).dispatchMapViewRequest(activity, options);
                if (internalMapView != null) {
                    break;
                }
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException | ClassNotFoundException | NoClassDefFoundError e) {
                LogUtils.e(TAG, e.getMessage());

            }
        }
        if (internalMapView != null) {
            addView((View) internalMapView);
        } else {
            internalMapView = new DJIEmptyMapView();
            Toast.makeText(getContext(), getContext().getString(R.string.uxsdk_map_provider_init_failed), Toast.LENGTH_LONG).show();
        }
    }

    //Doc key: DJIMap_DJIMapView_onCreate
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     *
     * @param savedInstanceState Bundle which contains the saved instance state.
     */
    public void onCreate(Bundle savedInstanceState) {
        internalMapView.onCreate(savedInstanceState);
    }

    //Doc key: DJIMap_DJIMapView_onStart
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onStart() {
        internalMapView.onStart();
    }

    //Doc key: DJIMap_DJIMapView_onResume
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onResume() {
        internalMapView.onResume();
    }

    //Doc key: DJIMap_DJIMapView_onPause
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onPause() {
        internalMapView.onPause();
    }

    //Doc key: DJIMap_DJIMapView_onStop
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onStop() {
        internalMapView.onStop();
    }

    //Doc key: DJIMap_DJIMapView_onDestroy
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onDestroy() {
        internalMapView.onDestroy();
    }

    //Doc key: DJIMap_DJIMapView_onSaveInstanceState
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     *
     * @param outState Bundle in which to place your saved state.
     */
    public void onSaveInstanceState(Bundle outState) {
        internalMapView.onSaveInstanceState(outState);
    }

    //Doc key: DJIMap_DJIMapView_onLowMemory
    /**
     * Must be called from the parent Activity/Fragment's corresponding method.
     */
    public void onLowMemory() {
        internalMapView.onLowMemory();
    }

    //Doc key: DJIMap_DJIMapView_getDJIMapAsync
    /**
     * Initializes the map view.
     *
     * @param callback The callback that will be invoked when the map is ready to be used.
     */
    public void getDJIMapAsync(@NonNull final OnDJIMapReadyCallback callback) {
        internalMapView.getDJIMapAsync(callback);
    }

    // Doc key: DJIMap_DJIMapView_OnDJIMapReadyCallbackInterface
    /**
     * Callback for the map ready event.
     */
    public interface OnDJIMapReadyCallback {

        // Doc key: DJIMap_DJIMapView_OnDJIMapReadyCallback
        /**
         * A callback indicating that the DJIMap is ready.
         *
         * @param map The DJIMap.
         */
        void onDJIMapReady(DJIMap map);
    }

    public DJIMapViewInternal getInternalMapView() {
        return internalMapView;
    }
}
