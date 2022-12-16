package dji.v5.ux.core.ui.hsi.config;

/**
 * M30 避障参数
 *
 */
object M30OmniAbility : IOmniAbility {
    private const val MAX_PERCEPTION_DISTANCE_IN_METER = 33

    private val MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER = Pair(1f, 10f)
    private val MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER = Pair(0.5f, 3f)

    /**
     * 实际可视角度65，和 M300 原本逻辑一致，为方便计算，取偶数
     */
    private const val PERCEPTION_BLIND_AREA_ANGLE = 26

    override fun getUpDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_IN_METER
    }

    override fun getUpAvoidanceDistanceRange() = MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER

    override fun getDownDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_IN_METER
    }

    override fun getDownAvoidanceDistanceRange() = MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER

    override fun getHorizontalDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_IN_METER
    }

    override fun getHorizontalAvoidanceDistanceRange() = MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER

    override fun getPerceptionBlindAreaAngle(): Int {
        return PERCEPTION_BLIND_AREA_ANGLE
    }
}