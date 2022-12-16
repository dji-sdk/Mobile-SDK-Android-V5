package dji.v5.ux.core.ui.hsi.config;

object M300OmniAbility : IOmniAbility {
    const val MAX_PERCEPTION_DISTANCE_HORIZONTAL_IN_METER = 40
    const val MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER = 30

    private val MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER = Pair(1f, 10f)
    private val MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER = Pair(0.5f, 3f)

    const val PERCEPTION_BLIND_AREA_ANGLE = 16

    override fun getUpDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER
    }

    override fun getUpAvoidanceDistanceRange() = MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER

    override fun getDownDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER
    }

    override fun getDownAvoidanceDistanceRange() = MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER

    override fun getHorizontalDetectionCapability(): Int {
        return MAX_PERCEPTION_DISTANCE_HORIZONTAL_IN_METER
    }

    override fun getHorizontalAvoidanceDistanceRange() = MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER

    override fun getPerceptionBlindAreaAngle(): Int {
        return PERCEPTION_BLIND_AREA_ANGLE
    }
}