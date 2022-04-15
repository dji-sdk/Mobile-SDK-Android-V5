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

package dji.v5.ux.core.base

import dji.sdk.keyvalue.key.DJIKey
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.core.util.DataProcessor

/**
 * Base module class for grouping sets of data that are often used together.
 * Module生命周期已经刷新，依赖于widgetModel的刷新
 */
abstract class BaseModule {

    //region Lifecycle
    /**
     * Setup method for initialization that must be implemented
     */
    protected abstract fun setup(widgetModel: WidgetModel)

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    protected abstract fun cleanup()
    //endregion

    /**
     * Bind the given DJIKey to the given data processor and attach the given consumer to it.
     * The data processor and side effect consumer will be invoked with every update to the key.
     * The side effect consumer will be called before the data processor is updated.
     *
     * @param key                DJIKey to be bound
     * @param dataProcessor      DataProcessor to be bound
     * @param sideEffectConsumer Consumer to be called along with data processor
     */
    protected open fun<T> bindDataProcessor(widgetModel: WidgetModel,
                                         key: DJIKey<T>,
                                         dataProcessor: DataProcessor<T>,
                                         sideEffectConsumer: Consumer<T> = Consumer {}) {
        widgetModel.bindDataProcessor(key, dataProcessor, sideEffectConsumer)
    }
}