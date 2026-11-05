package dji.sampleV5.aircraft.cable;

/**
 * 拍照间隔计算结果。
 */
public class PhotoIntervalResult {
    /** 最终拍照间隔（ms），下限 200ms */
    public final int intervalMs;
    /** 是否发生了自动降速 */
    public final boolean speedAdjusted;
    /** 调整后的实际巡检速度（m/s） */
    public final double adjustedSpeed;

    public PhotoIntervalResult(int intervalMs, boolean speedAdjusted, double adjustedSpeed) {
        this.intervalMs = intervalMs;
        this.speedAdjusted = speedAdjusted;
        this.adjustedSpeed = adjustedSpeed;
    }
}
