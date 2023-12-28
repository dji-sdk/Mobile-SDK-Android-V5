package dji.sampleV5.aircraft.keyvalue;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dji.sdk.keyvalue.key.DJIActionKeyInfo;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;


public class KeyBaseStructure<P, R>   {

    private static final String TAG = KeyBaseStructure.class.getSimpleName();

    /**
     * 设置参数
     */
    protected P param;

    /**
     * 返回结果
     */
    protected R result;

    /**
     * 推送数据记录
     */
    protected String listenRecord = "";

    /**
     * 推送Listener宿主
     */
    protected Object listenHolder;

    public int getComponetIndex() {
        return componetIndex;
    }

    public void setComponetIndex(int componetIndex) {
        this.componetIndex = componetIndex;
    }

    public int getSubComponetType() {
        return subComponetType;
    }

    public void setSubComponetType(int subComponetType) {
        this.subComponetType = subComponetType;
    }

    public int getSubComponetIndex() {
        return subComponetIndex;
    }

    public void setSubComponetIndex(int subComponetIndex) {
        this.subComponetIndex = subComponetIndex;
    }

    protected int componetIndex = -1;

    protected int subComponetType = -1;

    protected int subComponetIndex = -1;


    /**
     * 枚举列表
     */
    protected Map<String, List<EnumItem>> subItemMap = new HashMap<>();


    /**
     * 通过反射获取泛型类型数据并实例化
     */
    protected void initGenericInstance() {
        try {
            KeyItemHelper.INSTANCE.initClassData(param);
            KeyItemHelper.INSTANCE.initClassData(result);
            initSubItemData();
        } catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
    }

    /**
     * 如果为枚举或者枚举嵌套列表，则初始化该列表数据
     */
    protected void initSubItemData() {
        if (param == null) {
            return;
        }
        subItemMap.clear();
        subItemMap.putAll(KeyItemHelper.INSTANCE.initSubItemData(param));
    }


    /**
     * 获取需要设置的参数实例
     *
     * @return
     */
    public P getParam() {
        return param;
    }

    /**
     * 获取参数映射列表
     *
     * @return
     */
    public Map<String, List<EnumItem>> getSubItemMap() {
        return subItemMap;
    }

    /**
     * 获取推送数据记录
     *
     * @return
     */
    public String getListenRecord() {
        return listenRecord;
    }



    /**
     * 异步get
     *
     * @param keyInfo
     * @param getCallback
     */
    protected void get(DJIKeyInfo<R> keyInfo, CommonCallbacks.CompletionCallbackWithParam<R> getCallback) {
        DJIKey<R> key = createKey(keyInfo);
        KeyManager.getInstance().getValue(key, getCallback);
    }

    /**
     * 同步get
     *
     * @param keyInfo
     * @return
     */
    protected R get(DJIKeyInfo<R> keyInfo) {
        DJIKey<R> key = createKey(keyInfo);
       return KeyManager.getInstance().getValue(key);

    }

    /**
     * 设置属性
     *
     * @param keyInfo
     * @param setCallback
     */
    protected void set(DJIKeyInfo<P> keyInfo, P param, CommonCallbacks.CompletionCallback setCallback) {

        DJIKey<P> key = createKey(keyInfo);
        KeyManager.getInstance().setValue(key, param, setCallback);
    }



    /**
     * 设置Listener
     *
     * @param keyInfo
     * @param listenHolder
     * @param listenCallback
     */
    protected void listen(DJIKeyInfo<R> keyInfo, Object listenHolder, CommonCallbacks.KeyListener<R> listenCallback) {
        this.listenHolder = listenHolder;

        DJIKey<R> key = createKey(keyInfo);
        KeyManager.getInstance().listen(key, listenHolder, listenCallback);
    }

    /**
     * 取消Listener
     *
     * @param keyInfo
     * @param listenHolder
     */
    protected void cancelListen(DJIKeyInfo<R> keyInfo, Object listenHolder) {

        KeyManager.getInstance().cancelListen(createKey(keyInfo), listenHolder);
    }

    /**
     * 带参action
     *
     * @param keyInfo
     * @param param
     * @param actonCallback
     */
    protected void action(DJIActionKeyInfo<P, R> keyInfo, P param, CommonCallbacks.CompletionCallbackWithParam<R> actonCallback) {

        DJIKey.ActionKey<P,R> key = createActionKey(keyInfo);
        KeyManager.getInstance().performAction(key, param, actonCallback);
    }

    protected DJIKey.ActionKey<P,R> createActionKey(DJIActionKeyInfo<P,R> keyInfo) {
        DJIKey.ActionKey<P,R> key = null;
        key = KeyTools.createKey(keyInfo, 0, getComponetIndex(),getSubComponetType(), getSubComponetIndex());
        return key;
    }

    protected<Parame> DJIKey<Parame> createKey(DJIKeyInfo<Parame> keyInfo ) {
        DJIKey<Parame> key = null;
        key = KeyTools.createKey(keyInfo, 0 , getComponetIndex(),getSubComponetType(), getSubComponetIndex());
        return key;
    }

}
