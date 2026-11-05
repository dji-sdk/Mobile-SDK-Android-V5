package dji.sampleV5.aircraft.cable;

import androidx.annotation.NonNull;

/**
 * 导线巡检配置参数，集中管理发送给飞机的所有参数。
 * <p>
 * 由 VM 通过 {@link ICableInspectionManager#getInspectionConfig()} 获取后直接读写字段，
 * Manager 在 {@code sendAdjustPosCmd} / {@code sendBeginFollowCmd} 时读取组装命令。
 */
public class InspectionConfig {
    /** 飞行距离（m），范围 5.0~10.0，默认 10.0 */
    public double flightDistance = 10.0;
    /** 云台俯仰角（度），范围 0.0~35.0，默认 20.0 */
    public double pitchAngle = 20.0;
    /** 巡检方向，默认 {@link InspectionDirection#NONE} */
    @NonNull public InspectionDirection direction = InspectionDirection.NONE;
    /** 是否允许手动控制云台俯仰，默认 false */
    public boolean allowManualPitch = false;
    /** 自定义文件夹名称，默认空串 */
    @NonNull public String manualInputText = "";
    /** 变焦倍率，默认 5.0 */
    public double zoomLevel = 5.0;
    /** 巡检速度（m/s），范围 1.0~10.0，默认 5.0 */
    public double inspectionSpeed = 5.0;

    /** 重置所有参数为默认值 */
    public void reset() {
        flightDistance = 10.0;
        pitchAngle = 20.0;
        direction = InspectionDirection.NONE;
        allowManualPitch = false;
        manualInputText = "";
        zoomLevel = 5.0;
        inspectionSpeed = 5.0;
    }

    @NonNull
    @Override
    public String toString() {
        return "InspectionConfig{" +
                "flightDistance=" + flightDistance +
                ", pitchAngle=" + pitchAngle +
                ", direction=" + direction +
                ", allowManualPitch=" + allowManualPitch +
                ", manualInputText='" + manualInputText + '\'' +
                ", zoomLevel=" + zoomLevel +
                ", inspectionSpeed=" + inspectionSpeed +
                '}';
    }
}
