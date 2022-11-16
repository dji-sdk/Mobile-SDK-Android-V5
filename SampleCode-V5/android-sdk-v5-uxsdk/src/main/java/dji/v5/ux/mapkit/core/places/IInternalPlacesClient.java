package dji.v5.ux.mapkit.core.places;

import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 10/10/17.
 */
public interface IInternalPlacesClient {

    /**
     * poi的搜索半径，单位是米
     */
    int POI_RADIUS = 300;

    /**
     * 以 {@code latLng} 为圆心搜索poi，默认半径{@linkplain IInternalPlacesClient#POI_RADIUS}
     * @param latLng 搜索圆心（谷歌places服务失效）
     */
    void searchPOIAsyn(DJILatLng latLng);

    /**
     * 以 latLng 为圆心搜索poi，半径为 radius
     * @param latLng 搜索圆心（谷歌places服务失效）
     * @param radius 搜索半径（谷歌places服务失效）
     */
    void searchPOIAsyn(DJILatLng latLng, int radius);

    /**
     * 设置poi搜索成功后的回调
     * @param onPoiSearchListener
     */
    void setOnPoiSearchListener(DJIPlacesClient.OnPoiSearchListener onPoiSearchListener);

    /**
     * 设置poi搜索条件
     * @param poiSearchQuery
     */
    void setPoiSearchQuery(DJIPoiSearchQuery poiSearchQuery);

    /**
     * 设置Reverse geocoding成功后回调
     * @param onRegeocodeSearchListener
     */
    void setOnRegeocodeSearchListener(DJIPlacesClient.OnRegeocodeSearchListener onRegeocodeSearchListener);

    /**
     * 以 {@code latLng} 为圆心Reverse geocoding，只实现了高德和mapbox的搜索
     * @param latLng
     */
    void regeocodeSearchAsyn(DJILatLng latLng);

}
