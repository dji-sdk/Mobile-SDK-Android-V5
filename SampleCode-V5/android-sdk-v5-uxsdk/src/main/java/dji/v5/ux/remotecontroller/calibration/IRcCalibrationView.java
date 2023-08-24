package dji.v5.ux.remotecontroller.calibration;


import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;

/**
 * Created by richard.liao on 2018/6/26:20:43
 */

public interface IRcCalibrationView {

    /**
     * 设置回调
     * @param listener listener to add
     */
    void setListener(OnCalibrationListener listener);

    /**
     * 移除回调
     * @param
     */
    void removeListener();

    /**
     * 判断当前view所负责的遥控器部分是否已完成校准
     * @return 是否已完成校准
     */
    boolean isCalibrated();

    /**
     * 重置校准状态
     */
    void reset();

    /**
     * 根据整个校准的状态更新UI界面
     * @param mode 校准状态
     */
    void updateViewByMode(RcCalibrateState mode);
}
