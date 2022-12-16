package dji.v5.ux.core.util;

import java.util.ArrayList;
import java.util.List;

import dji.v5.common.video.stream.PhysicalDevicePosition;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/23
 * <p>
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
public class CommonUtils {

    public static List<Integer> toList(int [] ints){
        List<Integer> list = new ArrayList<>();
        if (ints == null){
            return list;
        }
        for (int i : ints){
            list.add(i);
        }
        return list;
    }

    public static SettingDefinitions.GimbalIndex getGimbalIndex(PhysicalDevicePosition devicePosition) {
        if (devicePosition == null){
            return SettingDefinitions.GimbalIndex.UNKONWN;
        }
        switch (devicePosition) {
            case PORTSIDE:
                return SettingDefinitions.GimbalIndex.PORT;
            case STARBOARD:
                return SettingDefinitions.GimbalIndex.STARBOARD;
            case UPSIDE:
                return SettingDefinitions.GimbalIndex.TOP;
            default:
                return SettingDefinitions.GimbalIndex.UNKONWN;
        }
    }

    private CommonUtils() {

    }
}
