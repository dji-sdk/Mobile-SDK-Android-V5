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

package dji.v5.ux.core.panel.listitem.obstacleavoidance

import dji.sdk.keyvalue.key.FlightAssistantKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.flightcontroller.ObstacleActionType
import dji.v5.et.create
import dji.v5.et.set
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

private const val TAG = "ObstacleAvoidanceListItemWidgetModel"

/**
 * Widget Model for the [ObstacleAvoidanceListItemWidgetModel] used to define
 * the underlying logic and communication
 */
class ObstacleAvoidanceListItemWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    val obstacleActionTypeProcessor = DataProcessor.create(ObstacleActionType.UNKNOWN)

    private val horizontalObstacleAvoidanceEnabledProcessor = DataProcessor.create(false)
    private val innerObstacleActionTypeProcessor = DataProcessor.create(ObstacleActionType.UNKNOWN)

    fun setObstacleActionType(type: ObstacleActionType): Completable {
        return Completable.create {
            FlightAssistantKey.KeyOmniHorizontalObstacleAvoidanceEnabled.create().set(true, {
                FlightControllerKey.KeyObstacleActionType.create().set(type, {
                    it.onComplete()
                })
            })
        }
    }

    override fun inSetup() {
        bindDataProcessor(FlightAssistantKey.KeyOmniHorizontalObstacleAvoidanceEnabled.create(), horizontalObstacleAvoidanceEnabledProcessor)
        bindDataProcessor(FlightControllerKey.KeyObstacleActionType.create(), innerObstacleActionTypeProcessor)
    }

    private fun updateObstacleActionType() {
        if (!horizontalObstacleAvoidanceEnabledProcessor.value) {
            obstacleActionTypeProcessor.onNext(ObstacleActionType.UNKNOWN)
            return
        }
        obstacleActionTypeProcessor.onNext(innerObstacleActionTypeProcessor.value)
    }

    override fun inCleanup() {
        // No clean up needed
    }

    override fun updateStates() {
        updateObstacleActionType()
    }
}