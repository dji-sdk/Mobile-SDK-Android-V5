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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * A singleton class is used throughout the UXSDK for getting schedulers.
 * The class provides an option to inject custom schedulers using [SchedulerProviderInterface]
 */
object SchedulerProvider {

    /**
     * Custom scheduler to be used instead of default schedulers
     */
    @JvmStatic
    @Volatile
    var scheduler: SchedulerProviderInterface? = null

    @JvmStatic
    fun io(): Scheduler {
        return scheduler?.io() ?: Schedulers.io()
    }

    @JvmStatic
    fun computation(): Scheduler {
        return scheduler?.computation() ?: Schedulers.computation()
    }

    @JvmStatic
    fun ui(): Scheduler {
        return scheduler?.ui() ?: AndroidSchedulers.mainThread()
    }

}