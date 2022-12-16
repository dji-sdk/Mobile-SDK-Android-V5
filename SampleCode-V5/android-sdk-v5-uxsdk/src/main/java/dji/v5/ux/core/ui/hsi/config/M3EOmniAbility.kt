package dji.v5.ux.core.ui.hsi.config;

object M3EOmniAbility : IOmniAbility {

    const val MAX_PERCEPTION_DISTANCE_HORIZONTAL_IN_METER = 16
    const val MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER = 10

    private val MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER = Pair(1f, 10f)
    private val MAX_UP_AVOIDANCE_DISTANCE_IN_METER = Pair(1f, 9.9f)
    private val MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER = Pair(0.5f, 2f)

    private const val PERCEPTION_BLIND_AREA_ANGLE = 0

    override fun getUpDetectionCapability() = MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER

    override fun getUpAvoidanceDistanceRange() = MAX_UP_AVOIDANCE_DISTANCE_IN_METER

    override fun getDownDetectionCapability() = MAX_PERCEPTION_DISTANCE_VERTICAL_IN_METER

    override fun getDownAvoidanceDistanceRange() = MAX_DOWN_AVOIDANCE_DISTANCE_IN_METER

    override fun getHorizontalDetectionCapability() = MAX_PERCEPTION_DISTANCE_HORIZONTAL_IN_METER

    override fun getHorizontalAvoidanceDistanceRange() = MAX_HORIZONTAL_AVOIDANCE_DISTANCE_IN_METER

    override fun getPerceptionBlindAreaAngle() = PERCEPTION_BLIND_AREA_ANGLE

}