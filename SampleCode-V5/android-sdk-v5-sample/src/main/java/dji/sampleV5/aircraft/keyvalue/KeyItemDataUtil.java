package dji.sampleV5.aircraft.keyvalue;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.converter.DJIValueConverter;
import dji.sdk.keyvalue.converter.IDJIValueConverter;
import dji.sdk.keyvalue.converter.SingleValueConverter;
import dji.sdk.keyvalue.key.AIBoxKey;
import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.BleKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.FlightAssistantKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.LidarKey;
import dji.sdk.keyvalue.key.MobileNetworkKey;
import dji.sdk.keyvalue.key.MobileNetworkLinkRCKey;
import dji.sdk.keyvalue.key.OcuSyncKey;
import dji.sdk.keyvalue.key.OnboardKey;
import dji.sdk.keyvalue.key.PayloadKey;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.key.RadarKey;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.key.RtkBaseStationKey;
import dji.sdk.keyvalue.key.RtkMobileStationKey;
import dji.sdk.keyvalue.value.base.DJIValue;
import dji.v5.utils.common.LogUtils;

/**
 * @author feel.feng
 * @time 2022/03/16 3:36 下午
 * @description:
 */
public class KeyItemDataUtil {
    private static final String TAG = KeyItemDataUtil.class.getSimpleName();
    private static final List<KeyItem<?, ?>> allKeyList = new ArrayList<>();

    private KeyItemDataUtil() {
        //do something
    }

    public static void initBatteryKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, BatteryKey.getKeyList());
    }

    public static void initAirlinkKeyList(List<KeyItem<?, ?>> keylist) {
        initList(keylist, AirLinkKey.getKeyList());
    }

    public static void initGimbalKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, GimbalKey.getKeyList());
    }

    public static void initCameraKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, CameraKey.getKeyList());
    }

    public static void initWiFiKeyList(List<KeyItem<?, ?>> keyList) {
//        initList(keyList , WiFiKey.getKeyList());
    }

    public static void initFlightAssistantKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, FlightAssistantKey.getKeyList());
    }

    public static void initFlightControllerKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, FlightControllerKey.getKeyList());
    }

    public static void initRemoteControllerKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, RemoteControllerKey.getKeyList());
    }

    public static void initBleKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, BleKey.getKeyList());
    }

    public static void initProductKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, ProductKey.getKeyList());
    }

    public static void initRtkBaseStationKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, RtkBaseStationKey.getKeyList());
    }

    public static void initRtkMobileStationKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, RtkMobileStationKey.getKeyList());
    }

    public static void initOcuSyncKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, OcuSyncKey.getKeyList());
    }

    public static void initRadarKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, RadarKey.getKeyList());
    }

    public static void initIntelligentBoxList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, AIBoxKey.getKeyList());
    }

    public static void initMobileNetworkKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, MobileNetworkKey.getKeyList());
    }

    public static void initMobileNetworkLinkRCKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, MobileNetworkLinkRCKey.getKeyList());
    }

    public static void initOnboardKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, OnboardKey.getKeyList());
    }

    public static void initPayloadKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, PayloadKey.getKeyList());
    }

    public static void initLidarKeyList(List<KeyItem<?, ?>> keyList) {
        initList(keyList, LidarKey.getKeyList());
    }

    private static void initList(List<KeyItem<?, ?>> keyList, List<DJIKeyInfo<?>> keyInfoList) {
        if (keyList == null || !keyList.isEmpty()){
            return;
        }
        for (DJIKeyInfo<?> info : keyInfoList) {
            KeyItem<DJIValue, DJIValue> item = new KeyItem<>(info);
            genericItem(item, info);
            keyList.add(item);
        }
    }

    public static <P extends DJIValue, R extends DJIValue> void genericItem(KeyItem<P, R> item, DJIKeyInfo<?> keyInfo) {

        Class<?> tdClazz;
        Field field = null;
        boolean isDjiValue = false;
        try {
            IDJIValueConverter<P, R> clazzConvert = keyInfo.getTypeConverter();
            if (clazzConvert instanceof SingleValueConverter) {
                field = clazzConvert.getClass().getDeclaredField("dClass");
                Field tmp = clazzConvert.getClass().getDeclaredField("isDJIValue");
                tmp.setAccessible(true);
                isDjiValue = tmp.getBoolean(clazzConvert);
            } else if (clazzConvert instanceof DJIValueConverter) {
                field = clazzConvert.getClass().getDeclaredField("tClass");
            }

            if (field != null) {
                field.setAccessible(true);
                tdClazz = (Class<?>) field.get(clazzConvert);
                item.param = (P) tdClazz.newInstance();
                item.result = (R) tdClazz.newInstance();
                item.setSingleDJIValue(isDjiValue);
                item.initGenericInstance();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    public static int getAllKeyListCount() {
        List<KeyItem<?, ?>> allKeyList = new ArrayList<>();
        getAllKeyList(allKeyList);
        return allKeyList.size();
    }

    public static void getAllKeyList(List<KeyItem<?, ?>> keylist) {
        if (!allKeyList.isEmpty()) {
            keylist.addAll(allKeyList);
            return;
        }
        List<KeyItem<?, ?>> keyList = new ArrayList<>();
        initBatteryKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initAirlinkKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initGimbalKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initCameraKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initFlightAssistantKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initFlightControllerKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initRemoteControllerKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initBleKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initProductKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initRtkBaseStationKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initRtkMobileStationKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initOcuSyncKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initRadarKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initMobileNetworkKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initMobileNetworkLinkRCKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initOnboardKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initPayloadKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initLidarKeyList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();

        initIntelligentBoxList(keyList);
        allKeyList.addAll(keyList);
        keyList.clear();
    }
}
