package dji.sampleV5.modulecommon.keyvalue;


import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.sdk.keyvalue.converter.DJIValueConverter;
import dji.sdk.keyvalue.converter.IDJIValueConverter;
import dji.sdk.keyvalue.converter.SingleValueConverter;
import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.BleKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIActionKeyInfo;
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
    private static final String KEY_RREFIX = "Key";

    private KeyItemDataUtil(){
        //dosomething
    }
    public static void initBatteryKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , BatteryKey.class);

    }
    public static void initAirlinkKeyList(List<KeyItem<? ,?>> keylist) {

        initList(keylist , AirLinkKey.class);
    }

    public static void initGimbalKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , GimbalKey.class);
    }

    public static void initCameraKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , CameraKey.class);
    }

    public static void initWiFiKeyList(List<KeyItem<? ,?>> keyList) {

//        initList(keyList , WiFiKey.class);
    }

    public static void initFlightAssistantKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , FlightAssistantKey.class);
    }

    public static void initFlightControllerKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , FlightControllerKey.class);
    }

    public static void initRemoteControllerKeyList(List<KeyItem<?,?>> keyList) {

        initList(keyList , RemoteControllerKey.class);
    }

    public static void initBleKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , BleKey.class);
    }

    public static void initProductKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , ProductKey.class);
    }

    public static void initRtkBaseStationKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , RtkBaseStationKey.class);
    }

    public static void initRtkMobileStationKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , RtkMobileStationKey.class);
    }

    public static void initOcuSyncKeyList(List<KeyItem<?,?>> keyList) {

        initList(keyList , OcuSyncKey.class);
    }

    public static void initRadarKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList, RadarKey.class);
    }

    public static void initAppKeyList(List<KeyItem<?,?>> keyList) {

//        initList(keyList , AppKey.class);
    }

    public static void initMobileNetworkKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , MobileNetworkKey.class);
    }

    public static void initMobileNetworkLinkRCKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , MobileNetworkLinkRCKey.class);
    }

    public static void initOnboardKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , OnboardKey.class);
    }

    public static void initPayloadKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList , PayloadKey.class);
    }

    public static void initLidarKeyList(List<KeyItem<? ,?>> keyList) {

        initList(keyList, LidarKey.class);
    }


    /**
     * 获取本类及其父类的属性的方法
     * @param clazz 当前类对象
     * @return 字段数组
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null){
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        return fieldList.toArray(fields);
    }




    public static boolean checkList(List<KeyItem<? ,?>> keyList){
        return keyList != null && !keyList.isEmpty();
    }

    private static void initList(List<KeyItem<? ,?>> keylist , Class<?> clazz){
        if (checkList(keylist)) {
            return;
        }
        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            try {
                Object fieldValue = field.get(null);
                Class<?> fieldClass = field.getType();
                String keyName = field.getName();

                if (fieldClass.equals(DJIKeyInfo.class) || fieldClass.equals(DJIActionKeyInfo.class)) {
                    DJIKeyInfo<?> keyInfo = (DJIKeyInfo<?>) fieldValue;

                    KeyItem<?, ?> item = new KeyItem<DJIValue, DJIValue>(keyInfo);
                    genericItem( item , keyInfo , keyName);

                    if (keylist != null) {
                        keylist.add(item);
                    }

                }

            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage());
            }

        }
    }

    public static void genericItem( KeyItem<?, ?> item , DJIKeyInfo<?> keyInfo , String keyName){

        Class<?> tdClazz;
        Field field = null;
        boolean isDjiValue = false;
        try {
            if (keyInfo.getIdentifier().isEmpty()) {
                item.setName(keyName.contains(KEY_RREFIX) ? keyName.substring(KEY_RREFIX.length()) : keyName);
            }
            IDJIValueConverter<?, ?> clazzConvert = keyInfo.getTypeConverter();
            if (clazzConvert instanceof SingleValueConverter) {
                field = clazzConvert.getClass().getDeclaredField("dClass");
                Field tmp = clazzConvert.getClass().getDeclaredField("isDJIValue");
                tmp.setAccessible(true);
                isDjiValue = tmp.getBoolean(clazzConvert);
            }
             else if (clazzConvert instanceof DJIValueConverter) {
                field  = clazzConvert.getClass().getDeclaredField("tClass");
            }

             if (field != null) {
                 field.setAccessible(true);
                 tdClazz = (Class<?>) field.get(clazzConvert);
                 item.param = tdClazz.newInstance();
                 item.result = tdClazz.newInstance();
                 item.setSingleDJIValue(isDjiValue);
                 item.initGenericInstance();
             }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }

    }
}
