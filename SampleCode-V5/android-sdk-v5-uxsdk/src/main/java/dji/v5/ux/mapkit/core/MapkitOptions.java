package dji.v5.ux.mapkit.core;

import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.maps.DJIMap;
import dji.v5.ux.mapkit.core.providers.MapProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个创建地图时的选项类
 * Created by joeyang on 1/5/18.
 */
public class MapkitOptions {
    /**
     * 默认的地图style
     */
    private int mapType = DJIMap.MAP_TYPE_NORMAL;

    private List<Integer> providerList;

    private MapProvider mapProvider;

    public MapProvider getMapProvider() {
        return mapProvider;
    }

    /**
     * 最终被选择的map provider
     */
    @Mapkit.MapProviderConstant
    private int provider;




    public MapkitOptions(int mapType, MapProvider mapProvider) {
        this.mapType = mapType;
        this.mapProvider = mapProvider;
    }

    public MapkitOptions(int mapType, @Mapkit.MapProviderConstant List<Integer> providerList) {
        this.mapType = mapType;
        this.providerList = providerList;
    }

    @Mapkit.MapProviderConstant
    public int getProvider() {
        return provider;
    }

    public int getMapType() {
        return mapType;
    }

    @Mapkit.MapProviderConstant
    public List<Integer> getProviderList() {
        return providerList;
    }

    public static final class Builder {
        private int mapType;
        @Mapkit.MapProviderConstant
        private List<Integer> providerList = new ArrayList<>();

        public Builder() {
            mapType = DJIMap.MAP_TYPE_NORMAL;
            providerList.clear();
        }

        public Builder mapType(int mapType) {
            this.mapType = mapType;
            return this;
        }

        public Builder addMapProvider(@Mapkit.MapProviderConstant int provider){
            providerList.add(provider);
            return this;
        }

        public MapkitOptions build() {
            return new MapkitOptions(mapType, providerList);
        }
    }
}
