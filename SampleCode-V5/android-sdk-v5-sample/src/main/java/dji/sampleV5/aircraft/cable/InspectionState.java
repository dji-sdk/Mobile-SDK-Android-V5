package dji.sampleV5.aircraft.cable;

/**
 * 导线巡检状态枚举，与飞控侧 CableInspectionState 对应映射。
 * 用于 {@link ICableInspectionManager.CableInspectionListener#onStateChanged} 回调。
 */
public enum InspectionState {
    /** 空闲，未开始。对应飞控 STANDBY */
    IDLE,
    /** 识别导线中，用户选择巡检方向。对应飞控 SELECT_CABLE */
    SELECTING_CABLE,
    /** 飞机正在飞向导线起始点。对应飞控 ADJUST_POS */
    ADJUSTING_POSITION,
    /** 到达起始点，悬停等待相机配置。对应飞控 WAITING */
    WAITING_FOR_CONFIG,
    /** 相机正在标定。对应飞控 CALIBRATION */
    CAMERA_CALIBRATING,
    /** 相机正在配置。对应飞控 CAM_CONFIG */
    CAMERA_CONFIGURING,
    /** 咬线跟随巡检中。对应飞控 FOLLOW */
    FOLLOWING,
    /** 巡检任务已完成。对应飞控 FINISH */
    FINISHED
}
