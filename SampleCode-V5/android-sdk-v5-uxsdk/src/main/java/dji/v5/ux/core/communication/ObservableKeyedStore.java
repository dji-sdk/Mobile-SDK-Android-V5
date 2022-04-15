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

package dji.v5.ux.core.communication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

public interface ObservableKeyedStore {
    /**
     * Adds an observer object which will receive all changes of value for the given key.
     *
     * @param key A valid UXKey
     * @return A flowable that emits BroadcastValues based on the given key.
     * This flowable can be used to subscribe to the keys as required.
     */
    @NonNull
    Flowable<BroadcastValues> addObserver(@NonNull UXKey key);

    /**
     * Removes the observer object for the given key so it no longer receives updates
     *
     * @param disposable Disposable used for observing key values
     * @param key        A valid UXKey
     */
    void removeObserver(@NonNull Disposable disposable, @NonNull UXKey key);

    /**
     * Removes the subscription to updates for all observers of a specific key value.
     * Does not affect the observers of any other key.
     * receive any further updates for this key
     *
     * @param key A valid UXKey
     */
    void removeAllObserversForKey(@NonNull UXKey key);

    /**
     * Stops the subscription to updates for all observers of all key values.
     * There will be no active observers on any key after this function is called.
     */
    void removeAllObservers();

    /**
     * Performs a get on a UXKey and returns the latest known value for the key
     * if available, null otherwise.
     *
     * @param key A valid UXKey
     * @return Object value for the key if available, null otherwise.
     */
    @Nullable
    Object getValue(@NonNull UXKey key);

    /**
     * Performs a set on a UXKey, changing the value for the key.
     *
     * @param key   A valid settable key
     * @param value A value object relevant to the given key
     * @return Completable which indicates success or error setting the value.
     */
    @NonNull
    Completable setValue(@NonNull UXKey key, Object value);
}
