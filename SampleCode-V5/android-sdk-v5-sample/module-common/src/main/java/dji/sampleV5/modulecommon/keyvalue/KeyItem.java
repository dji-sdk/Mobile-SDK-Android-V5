package dji.sampleV5.modulecommon.keyvalue;


import org.json.JSONObject;

import androidx.annotation.NonNull;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.sampleV5.modulecommon.util.Util;
import dji.sdk.keyvalue.converter.DJIValueConverter;
import dji.sdk.keyvalue.converter.EmptyValueConverter;
import dji.sdk.keyvalue.converter.SingleValueConverter;
import dji.sdk.keyvalue.key.DJIActionKeyInfo;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.value.base.DJIValue;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.utils.common.LogUtils;

/**
 *
 * KeyItem作为key能力和动作的载体来进行封装
 */

public class KeyItem<P, R> extends KeyBaseStructure<P , R> implements  Comparable<KeyItem<?,?>>{

    private static final String  TAG = KeyItem.class.getSimpleName();
    public KeyItem(DJIKeyInfo<?> keyInfo) {
        super();
        this.keyInfo = (DJIKeyInfo<R>)keyInfo;
        this.keyInfoSet = (DJIKeyInfo<P>)keyInfo;
    }

    /**
     * 属性展示名
     */
    protected String name;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    /**
     * 使用次数，用户排序
     */
    private long count;
    public boolean isSingleDJIValue;

    /**
     * 参数key的能力携带实体
     */
    protected DJIKeyInfo<R> keyInfo;

    protected DJIKeyInfo<P> keyInfoSet;


    /**
     * 需要调用者注入的回调接口，用于结果通知
     */
    protected KeyItemActionListener<Object> keyOperateCallBack;
    private boolean isItemSelected ;
    public boolean isItemSelected() {
        return isItemSelected;
    }

    public void setItemSelected(boolean itemSelected) {
        isItemSelected = itemSelected;
    }



    /**
     * 推送数据回调（需要调用者注入）
     */
    protected KeyItemActionListener<String> pushCallBack;

    public String getName() {
        return Util.isBlank(name) ? keyInfo.getIdentifier() : name;
    }
    public void setName(String name){
        this.name = name;
    }

    public DJIKeyInfo<P> getKeyInfo() {
        return keyInfoSet;
    }


    /**
     * 获取listen宿主
     *
     * @return
     */
    public Object getListenHolder() {
        return listenHolder;
    }

    /**
     * 是否可以Get
     *
     * @return
     */
    public boolean canGet() {
        return keyInfo.isCanGet();
    }

    /**
     * 是否可以Set
     *
     * @return
     */
    public boolean canSet() {
        return keyInfo.isCanSet();
    }

    /**
     * 是否可以Listen
     *
     * @return
     */
    public boolean canListen() {
        return keyInfo.isCanListen();
    }

    /**
     * 是否为action
     *
     * @return
     */
    public boolean canAction() {
        return keyInfo.isCanPerformAction();
    }

    /**
     * 需要调用者注入的回调接口，用于结果通知
     *
     * @param keyOperateCallBack
     */
    public void setKeyOperateCallBack(KeyItemActionListener<Object> keyOperateCallBack) {
        this.keyOperateCallBack = keyOperateCallBack;
    }

    /**
     * 推送回调
     *
     * @param pushCallBack
     */
    public void setPushCallBack(KeyItemActionListener<String> pushCallBack) {
        this.pushCallBack = pushCallBack;
    }

    /**
     * Get请求
     */
    public void doGet() {
        try {

            get(keyInfo, new CommonCallbacks.CompletionCallbackWithParam<R>() {
                @Override
                public void onSuccess(R data) {

                    if (keyOperateCallBack != null && data != null) {
                        keyOperateCallBack.actionChange(getName()+"【GET】 == success " + data.toString());
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (keyOperateCallBack != null) {
                        keyOperateCallBack.actionChange(getName() + "【GET】 GetErrorMsg ==" + error.toString());
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
    }

    /**
     * SyncGet请求
     */
    public void doSyncGet() {
        try {
            DJIValue getResult = (DJIValue) get(keyInfo);
            if(keyOperateCallBack != null){
                keyOperateCallBack.actionChange(null == getResult ? "" : getResult.toJson());
            }
        } catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
    }

    /**
     * Set请求
     */
    public void doSet(String jsonStr) {
        try {
            final P p = validPrams(jsonStr);
            if (p == null) {
                return;
            }

            set(keyInfoSet, p, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    if (keyOperateCallBack != null) {
                        // 保存上一次设置成功的对象，下次set时可使用保存过的对象 序列化json
                        if (getKeyInfo().getTypeConverter() instanceof DJIValueConverter) {
                            param = p;
                        }
                        keyOperateCallBack.actionChange(getName() + "【SET】==" +  p.toString() + " | " + " success");
                    }
                    ToastUtils.INSTANCE.showToast("set " + p.getClass().getSimpleName() + " success");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (keyOperateCallBack != null) {
                        keyOperateCallBack.actionChange(getName() + "【SET】SetErrorMsg== " + p.toString() + "|" + error.toString());
                    }
                }
            });


        } catch (Exception e) {
            ToastUtils.INSTANCE.showToast("输入参数出错");

        }
    }

    /**
     * action请求
     */
    public void doAction(String jsonStr) {

        DJIActionKeyInfo<P,R> actionKeyInfo = (DJIActionKeyInfo<P,R>) keyInfo;
        P p = null;
        if(jsonStr != null && !jsonStr.isEmpty()){
            p = validPrams(jsonStr);
        }
        if (p == null && actionKeyInfo.getTypeConverter()!= EmptyValueConverter.converter) {
            return;
        }

        P pRes = p;
        action(actionKeyInfo, p, new CommonCallbacks.CompletionCallbackWithParam<R>() {
            @Override
            public void onSuccess(Object data) {
                if (keyOperateCallBack != null) {
                    if (data != null && !(data instanceof EmptyMsg)) {
                        keyOperateCallBack.actionChange(getName() + "【ACTION】== " + getActionTipsStr(pRes) +  "  success: " + data.toString());
                    } else {
                        keyOperateCallBack.actionChange(getName() + "【ACTION】== "  +  getActionTipsStr(pRes) +  " result: success");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                if (keyOperateCallBack != null) {
                    keyOperateCallBack.actionChange(getName() +"【ACTION】 ActionErrorMsg==" + error.toString());
                }
            }
        });
    }

    private String getActionTipsStr(P pRes){
        return   pRes == null ? "" : pRes.toString();
    }

    /**
     * 推送回调
     */
    private CommonCallbacks.KeyListener<R> listenSDKCallback =
        ( oldValue ,newValue) -> {
            StringBuffer sb = new StringBuffer("【LISTEN】");
            sb.append(getName());
            sb.append(" result:");
            sb.append("oldValue:").append(oldValue);
            sb.append(" newValue:").append(newValue);

            if (pushCallBack != null) {
                pushCallBack.actionChange(sb.toString());
            }
        };


    /**
     * 注册Listen（新接口）
     *
     * @param listenHolder
     */
    public void listen(Object listenHolder) {
        listen(keyInfo, listenHolder, listenSDKCallback);
    }

    /**
     * 取消Listen（新接口）
     *
     * @param listenHolder
     */
    public void cancelListen(Object listenHolder) {
        if (this.listenHolder == listenHolder) {
            this.listenHolder = null;
            cancelListen(keyInfo, listenHolder);
            pushCallBack = null;
            listenRecord = "";
        }
        //listenSDKCallback = null;
    }

    /**
     * 验证参数
     *
     * @param jsonStr
     * @return
     */
    private P validPrams(String jsonStr) {
        if (Util.isBlank(jsonStr)) {
            ToastUtils.INSTANCE.showToast("请先设置参数");
            return null;
        }
        final P p = buildParamFromJsonStr(jsonStr);
        if (p == null) {
            ToastUtils.INSTANCE.showToast("请先设置" + jsonStr + " 参数");
            return null;
        }
        return p;
    }

    /**
     * 反序列化：根据JSON串获取对象
     *
     * @param jsonStr
     * @return
     */
    public P buildParamFromJsonStr(String jsonStr) {
        P p;
        if (keyInfo.getTypeConverter() instanceof SingleValueConverter && !isSingleDJIValue) {
            p = (P) keyInfo.getTypeConverter().fromStr(getSingleJsonValue(jsonStr));
        } else {
             p = (P) keyInfo.getTypeConverter().fromStr(jsonStr);
        }
        return p;
    }

    /**
     * 获取SingleValue 中原始类型包装类的value值
     * @param jsonStr
     * @return
     */
    private String getSingleJsonValue(String jsonStr) {
        String value = "";
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            value = jsonObj.getString("value");
        }catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
        return  value;
    }

    /**
     * 序列化：获取默认JSON串
     *
     * @return
     */
    public String getParamJsonStr() {
        String jsonStr = null;
        try {
            jsonStr = param.toString();
        } catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
        return jsonStr;
    }

    /**
     * 移除回调
     */
    public void removeCallBack() {
        cancelListen(listenHolder);
        keyOperateCallBack = null;
    }

    @Override
    public int compareTo(KeyItem keyItem) {
        if (keyItem.count - this.count > 0) {
            return  1;
        } else if (keyItem.count - this.count < 0) {
            return  -1;
        } else {
            return  0;
        }
    }


    public boolean isSingleDJIValue() {
        return isSingleDJIValue;
    }

    public void setSingleDJIValue(boolean singleDJIValue) {
        isSingleDJIValue = singleDJIValue;
    }

    @Override
    public String toString(){
       return Util.isBlank(name) ? keyInfo.getIdentifier() : name;
    }
}
