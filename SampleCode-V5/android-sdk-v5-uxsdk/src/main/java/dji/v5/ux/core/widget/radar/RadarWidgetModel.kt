/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package dji.v5.ux.core.widget.radar

import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore

private const val MM_IN_METER = 1000
private const val MAX_PERCEPTION_DISTANCE = 45
private const val DEFAULT_RADAR_DISTANCE = 10f
private const val DEFAULT_AVOIDANCE_DISTANCE = 3f
private const val DEGREES_IN_CIRCLE = 360
private const val SIDE_RADAR_DANGER_DISTANCE = 3
private const val SIDE_RADAR_WARNING_DISTANCE = 6

/**
 * Widget Model for the [RadarWidget] used to define
 * the underlying logic and communication
 */
class RadarWidgetModel(djiSdkModel: DJISDKModel,
                       keyedStore: ObservableInMemoryKeyedStore,
                       private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

//    //region Fields
//    private val visionDetectionStateProcessor: DataProcessor<VisionDetectionState> = DataProcessor.create(
//            VisionDetectionState.createInstance(
//                    false, 0.0,
//                    VisionSystemWarning.UNKNOWN,
//                    null,
//                    VisionSensorPosition.UNKNOWN,
//                    false,
//                    0))
//    private val isAscentLimitedByObstacleProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
//    private val modelProcessor: DataProcessor<Model> = DataProcessor.create(Model.UNKNOWN_AIRCRAFT)
//    private val isMotorOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
//    private val flightModeProcessor: DataProcessor<FlightMode> = DataProcessor.create(FlightMode.GPS_ATTI)
//    private val radarDistancesProcessor: DataProcessor<IntArray> = DataProcessor.create(intArrayOf())
//    private val horizontalRadarDistanceProcessor: DataProcessor<Float> = DataProcessor.create(DEFAULT_RADAR_DISTANCE)
//    private val horizontalAvoidanceDistanceProcessor: DataProcessor<Float> = DataProcessor.create(DEFAULT_AVOIDANCE_DISTANCE)
//    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
//    private val obstacleAvoidanceLevelProcessor: DataProcessor<ObstacleAvoidanceLevel> = DataProcessor.create(ObstacleAvoidanceLevel.NONE)
//    private val warningLevelRanges: MutableMap<Model, FloatArray> = ConcurrentHashMap()
//    //endregion
//
//    //region Data
//    /**
//     * The vision detection state.
//     */
//    val visionDetectionState: Flowable<VisionDetectionState>
//        get() = Flowable.combineLatest(visionDetectionStateProcessor.toFlowable(),
//                unitTypeProcessor.toFlowable(),
//                isRadarEnabled,
//                Function3 { state: VisionDetectionState,
//                            unitType: UnitConversionUtil.UnitType,
//                            isRadarEnabled: Boolean ->
//                    var sectors: Array<ObstacleDetectionSector>? = null
//                    var obstacleDistanceInMeters = 0.0
//
//                    if (isRadarEnabled) {
//                        sectors = state.detectionSectors?.map { sector ->
//                            ObstacleDetectionSector(sector.warningLevel,
//                                    sector.obstacleDistanceInMeters.toDistance(unitType))
//                        }?.toTypedArray()
//                        obstacleDistanceInMeters = state.obstacleDistanceInMeters.toDistance(unitType)
//
//                        if (state.position == VisionSensorPosition.TAIL) {
//                            sectors?.reverse()
//                        } else if (state.position == VisionSensorPosition.LEFT
//                                || state.position == VisionSensorPosition.RIGHT) {
//                            val warningLevel = getSideRadarWarningLevel(state)
//                            sectors = arrayOf(ObstacleDetectionSector(warningLevel,
//                                    obstacleDistanceInMeters.toFloat()))
//                        }
//                        adjustWarningLevels(sectors)
//                    }
//                    VisionDetectionState.createInstance(state.isSensorBeingUsed,
//                            obstacleDistanceInMeters,
//                            state.systemWarning,
//                            sectors,
//                            state.position,
//                            state.isDisabled,
//                            state.avoidAlertLevel)
//                })
//
//    /**
//     * Whether the radar is enabled. The radar is disabled if the product is in sport mode or no
//     * product is connected.
//     */
//    val isRadarEnabled: Flowable<Boolean>
//        get() = Flowable.combineLatest(flightModeProcessor.toFlowable(),
//                productConnectionProcessor.toFlowable(),
//                BiFunction { flightMode: FlightMode,
//                             isConnected: Boolean ->
//                    flightMode != FlightMode.GPS_SPORT && isConnected
//                })
//
//    /**
//     * The obstacle avoidance level. Will only emit updates if the Model is M200 V2 series or M300
//     * series and the motors are on.
//     */
//    val obstacleAvoidanceLevel: Flowable<ObstacleAvoidanceLevel>
//        get() = Flowable.combineLatest(obstacleAvoidanceLevelProcessor.toFlowable(),
//                modelProcessor.toFlowable(),
//                isMotorOnProcessor.toFlowable(),
//                Function3 { level: ObstacleAvoidanceLevel,
//                            model: Model,
//                            isMotorOn: Boolean ->
//                    if (ProductUtil.isM200V2M300(model) && isMotorOn) {
//                        level
//                    } else {
//                        ObstacleAvoidanceLevel.NONE
//                    }
//                }).distinctUntilChanged()
//
//    /**
//     * Whether the aircraft's upward sensor has detected an obstacle.
//     */
//    val ascentLimitedByObstacle: Flowable<Boolean>
//        get() = isAscentLimitedByObstacleProcessor.toFlowable()
//
//    /**
//     * The unit type of the distance value.
//     */
//    val unitType: Flowable<UnitConversionUtil.UnitType>
//        get() = unitTypeProcessor.toFlowable()
//    //endregion
//
//    //region Constructor
//    init {
//        if (preferencesManager != null) {
//            unitTypeProcessor.onNext(preferencesManager.unitType)
//        }
//    }
//    //endregion
//
//    //region Lifecycle
//    override fun inSetup() {
//        val visionDetectionStateKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE)
//        val isAscentLimitedByObstacleKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ASCENT_LIMITED_BY_OBSTACLE)
//        val modelKey: DJIKey = ProductKey.create(ProductKey.MODEL_NAME)
//        val isMotorOnKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON)
//        val flightModeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE)
//        val radarDistancesKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE)
//        val horizontalRadarDistanceKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE)
//        val horizontalAvoidanceDistanceKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE)
//        bindDataProcessor(visionDetectionStateKey, visionDetectionStateProcessor) { visionDetectionState: Any? ->
//            obstacleAvoidanceLevelProcessor.onNext(getObstacleAvoidanceLevel(visionDetectionState as VisionDetectionState))
//        }
//        bindDataProcessor(isAscentLimitedByObstacleKey, isAscentLimitedByObstacleProcessor)
//        bindDataProcessor(modelKey, modelProcessor)
//        bindDataProcessor(isMotorOnKey, isMotorOnProcessor)
//        bindDataProcessor(flightModeKey, flightModeProcessor)
//        bindDataProcessor(radarDistancesKey, radarDistancesProcessor) { radarDistances: Any? ->
//            parsePerceptionInformation(radarDistances as IntArray).forEach {
//                visionDetectionStateProcessor.onNext(it)
//            }
//            val distanceInMeters = getMinDistance(radarDistances).toDouble() / MM_IN_METER
//            obstacleAvoidanceLevelProcessor.onNext(getObstacleAvoidanceLevel(distanceInMeters.toFloat()))
//        }
//        bindDataProcessor(horizontalRadarDistanceKey, horizontalRadarDistanceProcessor)
//        bindDataProcessor(horizontalAvoidanceDistanceKey, horizontalAvoidanceDistanceProcessor)
//        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
//        bindDataProcessor(unitKey, unitTypeProcessor)
//        preferencesManager?.setUpListener()
//    }
//
//    override fun inCleanup() {
//        preferencesManager?.cleanup()
//    }
//
//    override fun updateStates() {
//        // Nothing to update
//    }
//    //endregion
//
//    //region Customization
//    /**
//     * Sets the warning level ranges for the specified product models.
//     *
//     * @param models      The product models for which these level ranges apply.
//     * @param levelRanges An array where each number represents the maximum distance in meters for
//     * the corresponding warning level. For example [70, 4, 2] would indicate
//     * that warning level LEVEL_1 has the range (4-70], warning level LEVEL_2
//     * has the range (2,4], and warning level LEVEL_3 has the range [0,2].
//     * A distance with a value above the largest number in the array will have
//     * the warning level INVALID.
//     */
//    fun setWarningLevelRanges(models: Array<ProductType>, levelRanges: FloatArray) {
//        if (models.isEmpty() || levelRanges.isEmpty()) {
//            return
//        }
//        val ranges = levelRanges.copyOf(levelRanges.size)
//        ranges.sortDescending()
//        models.forEach { warningLevelRanges[it] = ranges }
//    }
//    //endregion
//
//    //region Helpers
//    private fun adjustWarningLevels(sectors: Array<ObstacleDetectionSector>?) {
//        val currentModel = modelProcessor.value
//        if (!warningLevelRanges.containsKey(currentModel) || sectors == null) {
//            return
//        }
//        val currentModelLevelRanges = warningLevelRanges[currentModel]
//        if (currentModelLevelRanges != null) {
//            sectors.forEachIndexed { i, sector ->
//                val distanceInMeters = sector.obstacleDistanceInMeters
//                val j = currentModelLevelRanges.indexOfLast { distanceInMeters < it }
//                if (j >= 0) {
//                    sectors[i] = ObstacleDetectionSector(ObstacleDetectionSectorWarning.find(j), distanceInMeters)
//                }
//            }
//        }
//    }
//
//    private fun getSideRadarWarningLevel(state: VisionDetectionState) : ObstacleDetectionSectorWarning {
//        return when {
//            state.obstacleDistanceInMeters <= 0 || state.obstacleDistanceInMeters >= SIDE_RADAR_WARNING_DISTANCE -> {
//                ObstacleDetectionSectorWarning.UNKNOWN
//            }
//            state.obstacleDistanceInMeters < SIDE_RADAR_DANGER_DISTANCE -> {
//                ObstacleDetectionSectorWarning.LEVEL_2
//            }
//            else -> ObstacleDetectionSectorWarning.LEVEL_1
//        }
//    }
//
//    private fun parsePerceptionInformation(info: IntArray): List<VisionDetectionState> {
//        val numDegreesPerValue = DEGREES_IN_CIRCLE / info.size
//
//        val forwardSlice1 = (323 / numDegreesPerValue)..(359 / numDegreesPerValue)
//        val forwardSlice2 = 0..(36 / numDegreesPerValue)
//        val rightSlice = (53 / numDegreesPerValue)..(126 / numDegreesPerValue)
//        val backwardSlice = (143 / numDegreesPerValue)..(216 / numDegreesPerValue)
//        val leftSlice = (233 / numDegreesPerValue)..(306 / numDegreesPerValue)
//        return arrayListOf(
//                getVisionDetectionState(info.sliceArray(forwardSlice1) + info.sliceArray(forwardSlice2),
//                        VisionSensorPosition.NOSE),
//                getVisionDetectionState(info.sliceArray(rightSlice),
//                        VisionSensorPosition.RIGHT),
//                getVisionDetectionState(info.sliceArray(backwardSlice),
//                        VisionSensorPosition.TAIL),
//                getVisionDetectionState(info.sliceArray(leftSlice),
//                        VisionSensorPosition.LEFT))
//    }
//
//    private fun getVisionDetectionState(distances: IntArray, position: VisionSensorPosition): VisionDetectionState {
//        val distanceInMeters = getMinDistance(distances).toDouble() / MM_IN_METER
//        val visionSectorState = when (position) {
//            VisionSensorPosition.NOSE, VisionSensorPosition.TAIL -> getVisionSectorState(distances)
//            else -> null
//        }
//
//        return VisionDetectionState.createInstance(true,
//                distanceInMeters,
//                VisionSystemWarning.UNKNOWN,
//                visionSectorState,
//                position,
//                false,
//                0)
//    }
//
//    private fun getVisionSectorState(distances: IntArray?): Array<ObstacleDetectionSector>? {
//        if (distances == null || distances.isEmpty()) {
//            return null
//        }
//
//        val slice1 = 0 until (distances.size / 4)
//        val slice2 = (distances.size / 4) until (distances.size / 2)
//        val slice3 = (distances.size / 2) until (distances.size * 3 / 4)
//        val slice4 = (distances.size * 3 / 4) until (distances.size)
//        return arrayOf(
//                getSector(getMinDistance(distances.sliceArray(slice1)) / MM_IN_METER.toFloat()),
//                getSector(getMinDistance(distances.sliceArray(slice2)) / MM_IN_METER.toFloat()),
//                getSector(getMinDistance(distances.sliceArray(slice3)) / MM_IN_METER.toFloat()),
//                getSector(getMinDistance(distances.sliceArray(slice4)) / MM_IN_METER.toFloat()))
//    }
//
//    private fun getSector(distanceInMeters: Float): ObstacleDetectionSector {
//        val sectorWarningLevel = if (modelProcessor.value == Model.MATRICE_300_RTK) {
//            when {
//                distanceInMeters < 0 -> ObstacleDetectionSectorWarning.UNKNOWN
//                distanceInMeters >= MAX_PERCEPTION_DISTANCE -> ObstacleDetectionSectorWarning.INVALID
//                distanceInMeters > horizontalRadarDistanceProcessor.value -> ObstacleDetectionSectorWarning.LEVEL_1
//                distanceInMeters > horizontalAvoidanceDistanceProcessor.value + 2 -> ObstacleDetectionSectorWarning.LEVEL_4
//                else -> ObstacleDetectionSectorWarning.LEVEL_6
//            }
//        } else {
//            when {
//                distanceInMeters < 3 -> ObstacleDetectionSectorWarning.LEVEL_6
//                distanceInMeters < 6 -> ObstacleDetectionSectorWarning.LEVEL_5
//                distanceInMeters < 10 -> ObstacleDetectionSectorWarning.LEVEL_4
//                distanceInMeters < 15 -> ObstacleDetectionSectorWarning.LEVEL_3
//                distanceInMeters < 20 -> ObstacleDetectionSectorWarning.LEVEL_2
//                else -> ObstacleDetectionSectorWarning.LEVEL_1
//            }
//        }
//        return ObstacleDetectionSector(sectorWarningLevel, distanceInMeters)
//    }
//
//    private fun getObstacleAvoidanceLevel(distanceInMeters: Float): ObstacleAvoidanceLevel {
//        return when {
//            distanceInMeters > horizontalRadarDistanceProcessor.value -> ObstacleAvoidanceLevel.NONE
//            distanceInMeters >= horizontalRadarDistanceProcessor.value / 3 + horizontalAvoidanceDistanceProcessor.value * 2 / 3 -> ObstacleAvoidanceLevel.LEVEL_1
//            distanceInMeters >= horizontalRadarDistanceProcessor.value / 6 + horizontalAvoidanceDistanceProcessor.value * 5 / 6 -> ObstacleAvoidanceLevel.LEVEL_2
//            else -> ObstacleAvoidanceLevel.LEVEL_3
//        }
//    }
//
//    private fun getObstacleAvoidanceLevel(visionDetectionState: VisionDetectionState): ObstacleAvoidanceLevel {
//        val distanceInMeters = if (visionDetectionState.position == VisionSensorPosition.NOSE
//                || visionDetectionState.position == VisionSensorPosition.TAIL) {
//            visionDetectionState.detectionSectors?.map { sector: ObstacleDetectionSector ->
//                sector.obstacleDistanceInMeters
//            }?.toTypedArray()?.min() ?: 0f
//        } else {
//            visionDetectionState.obstacleDistanceInMeters.toFloat()
//        }
//
//        return when {
//            distanceInMeters < 2.5 -> ObstacleAvoidanceLevel.LEVEL_3
//            distanceInMeters < 5 -> ObstacleAvoidanceLevel.LEVEL_2
//            distanceInMeters < 10 -> ObstacleAvoidanceLevel.LEVEL_1
//            else -> ObstacleAvoidanceLevel.NONE
//        }
//    }
//
//    private fun getMinDistance(distances: IntArray): Int {
//        return distances.filter { it in 1..60000 }.min() ?: Int.MAX_VALUE
//    }
//    //endregion
//
//    //region Classes
//    /**
//     * The proximity of obstacles
//     */
//    enum class ObstacleAvoidanceLevel(@get:JvmName("value") val value: Int) {
//
//        /**
//         * No obstacles are detected
//         */
//        NONE(0),
//
//        /**
//         * Obstacle is detected far away
//         */
//        LEVEL_1(1),
//
//        /**
//         * Obstacle is detected close by
//         */
//        LEVEL_2(2),
//
//        /**
//         * Obstacle is detected very close by
//         */
//        LEVEL_3(3);
//
//        companion object {
//            @JvmStatic
//            val values = values()
//
//            @JvmStatic
//            fun find(value: Int): ObstacleAvoidanceLevel {
//                return values.find { it.value == value } ?: NONE
//            }
//        }
//    }
    //endregion

    override fun inSetup() {
        //暂无实现
    }

    override fun inCleanup() {
        //暂无实现
    }
}