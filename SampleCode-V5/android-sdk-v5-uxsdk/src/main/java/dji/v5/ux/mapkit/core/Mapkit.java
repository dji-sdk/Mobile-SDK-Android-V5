package dji.v5.ux.mapkit.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.mapkit.core.exceptions.MapkitInitializerException;
import dji.v5.ux.mapkit.core.maps.DJIMap;

import java.util.HashMap;



//Doc key: DJIMap_Mapkit
/**
 * Contains methods to set information that is required for map initialization.
 */
public class Mapkit {

     private static final String TAG = LogUtils.getTag(Mapkit.class.getSimpleName());

    private static final String MAPBOX_TOKEN_KEY = "com.dji.mapkit.maplibre.apikey";
    private static String MAPBOX_ACCESS_TOKEN ;

    private volatile static Boolean sIsInMainlandChina;
    private volatile static Boolean sIsInHongKong;
    private volatile static Boolean sIsInMacau;

    private static int sMapProvider = MapProviderConstant.INVALID_PROVIDER;
    private static int sMapType = DJIMap.MAP_TYPE_NORMAL;
    private static int sGeocodingProvider = MapProviderConstant.INVALID_PROVIDER;

    private static final HashMap<Integer, String> providerClassName = new HashMap<>();
    private static final String CLASS_PROVIDER_AMAP = "dji.v5.ux.mapkit.amap.provider.AMapProvider";
    private static final String CLASS_PROVIDER_MAPLIBRE = "dji.v5.ux.mapkit.maplibre.provider.MaplibreProvider";
    private static final String CLASS_PROVIDER_GOOGLE = "com.dji.mapkit.google.provider.GoogleProvider";
   // private static final String CLASS_PROVIDER_MAPLIBRE = "dji.v5.ux.mapkit.maplibre.provider.MapLibreProvider";

    static {
        providerClassName.put(MapProviderConstant.GOOGLE_MAP_PROVIDER, CLASS_PROVIDER_GOOGLE);
        providerClassName.put(MapProviderConstant.MAPLIBRE_MAP_PROVIDER, CLASS_PROVIDER_MAPLIBRE);
        providerClassName.put(MapProviderConstant.AMAP_PROVIDER, CLASS_PROVIDER_AMAP);
    }

    public static void init(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            MAPBOX_ACCESS_TOKEN = appInfo.metaData.getString(MAPBOX_TOKEN_KEY);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    /**
     * 地图Provider类型
     */
    @IntDef({MapProviderConstant.INVALID_PROVIDER, MapProviderConstant.GOOGLE_MAP_PROVIDER,
    MapProviderConstant.AMAP_PROVIDER, MapProviderConstant.MAPLIBRE_MAP_PROVIDER, MapProviderConstant.HERE_MAP_PROVIDER})
    public @interface MapProviderConstant {
        int INVALID_PROVIDER = 0;
        int GOOGLE_MAP_PROVIDER = 1;
        int AMAP_PROVIDER = 2;
        int MAPLIBRE_MAP_PROVIDER = 3;
        int HERE_MAP_PROVIDER = 4;
    }

    private Mapkit() {
    }

    //Doc key: DJIMap_Mapkit_mapboxAccessToken
    /**
     * Sets the maplibre access token. This must be called before fetching the Mapbox map view.
     *
     * @param accessToken The maplibre access token.
     */
    public static void mapboxAccessToken(@NonNull String accessToken) {
        MAPBOX_ACCESS_TOKEN = accessToken;
    }

    /**
     * 获取 maplibre 的 access token
     * @return
     */
    public static String getMapboxAccessToken() {
        if (MAPBOX_ACCESS_TOKEN == null) {
            throw new MapkitInitializerException("Mapbox token is not set yet.");
        } else {
            return MAPBOX_ACCESS_TOKEN;
        }
    }

    //Doc key: DJIMap_Mapkit_inMainlandChina
    /**
     * Sets whether the drone's location is in mainland China. This information is used for GPS
     * offset, and does not affect which map is used.
     *
     * @param inMainlandChina `true` if the user is in mainland China.
     */
    public static void inMainlandChina(boolean inMainlandChina) {
        sIsInMainlandChina = inMainlandChina;
    }

    /**
     * 获取是否在中国大陆。用于gps偏移，不影响具体使用哪个地图。
     * @return
     */
    public static boolean isInMainlandChina() {
        if (sIsInMainlandChina == null) {
            throw new MapkitInitializerException("You should set if Mapkit is used in mainland China, " +
                    "so that Mapkit can correct the coordinate offset.");
        } else {
            return sIsInMainlandChina;
        }
    }

    //Doc key: DJIMap_Mapkit_inHongKong
    /**
     * Sets whether the drone's location is in Hong Kong. This information is used for GPS
     * offset, and does not affect which map is used.
     *
     * @param inHongKong `true` if the user is in Hong Kong.
     */
    public static void inHongKong(boolean inHongKong) {
        sIsInHongKong = inHongKong;
    }

    /**
     * 获取是否在香港，该参数只影响高德定位的纠偏
     * @return
     */
    public static boolean isInHongKong() {
        if (sIsInHongKong == null) {
            throw new MapkitInitializerException("You should set if Mapkit is used in Hong Kong, " +
                    "so that Mapkit can correct the coordinate offset.");
        } else {
            return sIsInHongKong;
        }
    }

    //Doc key: DJIMap_Mapkit_inMacau
    /**
     * Sets whether the user's location is in Macau. This information is used for GPS
     * offset, and does not affect which map is used.
     *
     * @param inMacau `true` if the user is in Macau.
     */
    public static void inMacau(boolean inMacau) {
        sIsInMacau = inMacau;
    }

    /**
     * 获取是否在澳门，该参数只影响高德定位的纠偏
     * @return
     */
    public static boolean isInMacau() {
        if (sIsInMacau == null) {
            throw new MapkitInitializerException("You should set if Mapkit is used in Macau, " +
                    "so that Mapkit can correct the coordinate offset.");
        } else {
            return sIsInMacau;
        }
    }

    @MapProviderConstant
    public static int getMapProvider() {
        return sMapProvider;
    }

    public static int getMapType() {
        return sMapType;
    }

    public static int getGeocodingProvider() {
        return sGeocodingProvider;
    }

    /**
     * 设置具体的地图供应商
     * @param provider
     */
    public static void mapProvider(@MapProviderConstant int provider) {
        sMapProvider = provider;
    }

    /**
     * Set the map type. You should never call this.
     * @param mapType
     */
    public static void mapType(int mapType) {
        sMapType = mapType;
    }

    public static void geocodingProvider(int provider) {
        sGeocodingProvider = provider;
    }

    public static String getMapProviderClassName(@MapProviderConstant int provider) {
        return providerClassName.get(provider);
    }
}
