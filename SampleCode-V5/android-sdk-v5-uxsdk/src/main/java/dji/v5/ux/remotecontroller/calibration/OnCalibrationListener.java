package dji.v5.ux.remotecontroller.calibration;

import androidx.annotation.IntDef;

/**
 * Created by richard.liao on 2018/6/26:20:49
 * @author richard.liao
 */
public interface OnCalibrationListener {

    int TYPE_STICK = 0x0001;
    int TYPE_WHEEL = 0x0002;

    @IntDef({TYPE_STICK, TYPE_WHEEL})
    @interface RcPartType {
    }

    /**
     * 当前view负责的遥控器部分完成校准时的回调
     * @param partType 完成校准的遥控器部分
     */
    void onCalibrationFinished(@RcPartType int partType);

    /**
     * 当前view负责的遥控器部分校准取消时的回调
     * @param partType 取消校准的遥控器部分
     */
    void onCalibrationCanceled(@RcPartType int partType);
}
