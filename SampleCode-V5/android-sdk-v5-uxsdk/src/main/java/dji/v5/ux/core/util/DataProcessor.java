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

import dji.v5.ux.core.base.SchedulerProvider;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/**
 * Processor that emits the most recent item it has observed and all subsequent observed items
 *
 * @param <T> the type of item expected to be observed and emitted by the Processor
 */
public final class DataProcessor<T> {

    private final BehaviorProcessor<T> processor;

    private final T defaultValue;

    /**
     * Creates a DataProcessor with the given default value
     *
     * @param defaultValue The first item that will be emitted
     * @param <T>          The type of item the processor will emit
     * @return The constructed DataProcessor
     */
    @NonNull
    public static <T> DataProcessor<T> create(@NonNull T defaultValue) {
        return new DataProcessor<>(defaultValue);
    }

    private DataProcessor(@NonNull T defaultValue) {
        processor = BehaviorProcessor.createDefault(defaultValue);
        this.defaultValue = defaultValue;
    }

    /**
     * Emit a new item
     *
     * @param data item to be emitted
     */
    public void onNext(@NonNull T data) {
        processor.onNext(data);
    }

    /**
     * Emit completion event
     */
    public void onComplete() {
        processor.onComplete();
    }

    /**
     * Emit an error event
     *
     * @param error The error to emit
     */
    public void onError(@NonNull Throwable error) {
        processor.onError(error);
    }

    /**
     * Get the latest value of the processor
     *
     * @return The latest value of the processor
     */
    @NonNull
    public T getValue() {
        T t = processor.getValue();
        if (t == null) {
            t = defaultValue;
        }
        return t;
    }

    /**
     * Get the stream of data from the processor
     *
     * @return A Flowable representing the stream of data
     */
    @NonNull
    public Flowable<T> toFlowable() {
        return processor.observeOn(SchedulerProvider.computation()).onBackpressureLatest();
    }

    @NonNull
    public Flowable<T> toFlowableOnUI() {
        return processor.observeOn(SchedulerProvider.ui()).onBackpressureLatest();
    }

    public Observable<T> toObservableOnUI() {
        return toFlowableOnUI().toObservable();
    }
}
