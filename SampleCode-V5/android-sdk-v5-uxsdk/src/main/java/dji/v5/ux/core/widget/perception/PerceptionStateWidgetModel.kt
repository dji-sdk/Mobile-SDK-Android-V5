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

package dji.v5.ux.core.widget.perception

import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener
import dji.v5.manager.interfaces.IPerceptionManager
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

/**
 * Widget Model for the [PerceptionStateWidget] used to define
 * the underlying logic and communication
 *
 * @author: William.Wong
 *  date : 2022/9/21
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PerceptionStateWidgetModel(
    djiSdkModel: DJISDKModel,
    val keyedStore: ObservableInMemoryKeyedStore,
    val perceptionManager: IPerceptionManager,
) : WidgetModel(djiSdkModel, keyedStore) {
    private val perceptionInfoProcessor= DataProcessor.create(PerceptionInfo())
    private val perceptionInfoListener = PerceptionInformationListener {
        perceptionInfoProcessor.onNext(it)
    }

    val perceptionInfo: Flowable<PerceptionInfo>
        get() = perceptionInfoProcessor.toFlowable()

    override fun inSetup() {
        perceptionManager.addPerceptionInformationListener(perceptionInfoListener)
    }

    override fun inCleanup() {
        perceptionManager.removePerceptionInformationListener(perceptionInfoListener)
    }

    /**
     * Obstacle avoidance system switch state
     */
    enum class PerceptionSwitchState {
        /**
         * All enabled
         */
        ALL_ENABLED,

        /**
         * Only horizontal obstacle avoidance system isn't working
         */
        ONLY_HORI_OFF,

        /**
         * Only upward obstacle avoidance system isn't working
         */
        ONLY_UP_OFF,

        /**
         * Only downward obstacle avoidance system isn't working
         */
        ONLY_DOWN_OFF,

        /**
         * Only horizontal obstacle avoidance system is working
         */
        ONLY_HORI_ON,

        /**
         * Only upward obstacle avoidance system is working
         */
        ONLY_UP_ON,

        /**
         * Only downward obstacle avoidance system is working
         */
        ONLY_DOWN_ON,

        /**
         * All disabled
         */
        ALL_DISABLED
    }
}