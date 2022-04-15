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

package dji.v5.ux.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.utils.common.LogUtils;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Util class to help with reactive functions
 */
public final class RxUtil {

    private RxUtil() {
        // Util class
    }

    /**
     * Get a throwable error consumer for the given error.
     *
     * @param tag     Tag for the log
     * @param message Message to be logged
     * @return Throwable consumer
     */
    public static Consumer<Throwable> logErrorConsumer(@NonNull String tag, @NonNull String message) {
        return errorConsumer(null, tag, message);
    }

    public static Consumer<Throwable> errorConsumer(@Nullable errorHandler handler, @NonNull String tag, @NonNull String message) {
        return throwable -> {
            if (handler != null) {
                handler.onErrorHandler();
            }
            LogUtils.e(tag, message + throwable.getLocalizedMessage());
        };
    }

    public interface errorHandler {
        void onErrorHandler();
    }
}
