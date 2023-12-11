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

package dji.v5.ux.core.widget.verticalvelocity

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Velocity3D
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.toVelocity
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil.UnitType
import dji.v5.ux.core.widget.verticalvelocity.VerticalVelocityWidgetModel.VerticalVelocityState.*
import kotlin.math.abs

/**
 * Widget Model for the [VerticalVelocityWidget] used to define
 * the underlying logic and communication
 */
class VerticalVelocityWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val velocityProcessor = DataProcessor.create(Velocity3D())
    private val unitTypeDataProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val verticalVelocityStateProcessor: DataProcessor<VerticalVelocityState> = DataProcessor.create(ProductDisconnected)

    /**
     * Value of the vertical velocity state of the aircraft
     */
    val verticalVelocityState: Flowable<VerticalVelocityState>
        get() = verticalVelocityStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftVelocity), velocityProcessor)
        bindDataProcessor(GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE), unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when {
                velocityProcessor.value.z < 0 -> {
                    verticalVelocityStateProcessor.onNext(
                        UpwardVelocity(
                            abs(velocityProcessor.value.z).toVelocity(unitTypeDataProcessor.value),
                            unitTypeDataProcessor.value
                        )
                    )
                }
                velocityProcessor.value.z > 0 -> {
                    verticalVelocityStateProcessor.onNext(
                        DownwardVelocity(
                            abs(velocityProcessor.value.z).toVelocity(unitTypeDataProcessor.value),
                            unitTypeDataProcessor.value
                        )
                    )
                }
                else -> {
                    verticalVelocityStateProcessor.onNext(Idle(unitTypeDataProcessor.value))
                }
            }
        } else {
            verticalVelocityStateProcessor.onNext(ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states of vertical velocity
     */
    sealed class VerticalVelocityState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : VerticalVelocityState()

        /**
         * When aircraft is not moving vertically
         */
        data class Idle(val unitType: UnitType) : VerticalVelocityState()

        /**
         * When aircraft is moving in upward direction
         */
        data class UpwardVelocity(val velocity: Double, val unitType: UnitType) :
            VerticalVelocityState()

        /**
         * When aircraft is moving in downward direction
         */
        data class DownwardVelocity(val velocity: Double, val unitType: UnitType) :
            VerticalVelocityState()
    }

}