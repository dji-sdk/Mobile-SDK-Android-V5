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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.UXSDKError;
import dji.v5.ux.core.base.UXSDKErrorDescription;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.PublishProcessor;

/**
 * `ObservableInMemoryKeyedStore` provides access to the keyed interface using `UXKeys` and
 * corresponding subclass objects. It implements the ObservableKeyedStore interface.
 */
public class ObservableInMemoryKeyedStore implements ObservableKeyedStore {
    private static final int INITIAL_CAPACITY = 100;
    private final ConcurrentHashMap<String, PublishProcessor<BroadcastValues>> keyStringProcessorMap;
    private final FlatStore store;

    private final Lock lock = new ReentrantLock();

    private ObservableInMemoryKeyedStore() {
        keyStringProcessorMap = new ConcurrentHashMap<>(INITIAL_CAPACITY);
        store = FlatStore.getInstance();
        //Initialize any internal default UXKey classes here
        UXKeys.addNewKeyClass(GlobalPreferenceKeys.class);
        UXKeys.addNewKeyClass(MessagingKeys.class);
    }

    public static ObservableInMemoryKeyedStore getInstance() {
        return ObservableInMemoryKeyedStore.SingletonHolder.instance;
    }

    /**
     * Adds an observer object which will receive all changes of value for the given key.
     *
     * @param key A valid UXKey
     * @return A flowable that emits BroadcastValues based on the given key.
     * This flowable can be used to subscribe to the keys as required.
     */
    @Override
    @NonNull
    public Flowable<BroadcastValues> addObserver(@NonNull UXKey key) {
        lock.lock();
        try {
            PublishProcessor<BroadcastValues> processor = keyStringProcessorMap.get(key.getKeyPath());
            if (processor == null) {
                processor = PublishProcessor.create();
            }
            keyStringProcessorMap.put(key.getKeyPath(), processor);
            return processor.observeOn(SchedulerProvider.computation()).onBackpressureLatest();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the observer object for the given key so it no longer receives updates
     *
     * @param disposable Disposable used for observing key values
     * @param key        A valid UXKey
     */
    @Override
    public void removeObserver(@NonNull Disposable disposable, @NonNull UXKey key) {
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    /**
     * Removes the subscription to updates for all observers of a specific key value.
     * Does not affect the observers of any other key.
     *
     * @param key A valid UXKey
     */
    @Override
    public void removeAllObserversForKey(@NonNull UXKey key) {
        lock.lock();
        try {
            PublishProcessor<BroadcastValues> removedProcessor = keyStringProcessorMap.remove(key.getKeyPath());
            if (removedProcessor != null){
                removedProcessor.onComplete();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops the subscription to updates for all observers of all key values.
     * There will be no active observers on any key after this function is called.
     */
    @Override
    public void removeAllObservers() {
        lock.lock();
        try {
            for (PublishProcessor<BroadcastValues> processorToRemove : keyStringProcessorMap.values()) {
                processorToRemove.onComplete();
            }
            keyStringProcessorMap.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Performs a get on a UXKey and returns the latest known value for the key
     * if available, null otherwise.
     *
     * @param key A valid UXKey
     * @return Object value for the key if available, null otherwise.
     */
    @Override
    @Nullable
    public Object getValue(@NonNull UXKey key) {
        lock.lock();
        try {
            //This function will return the value or return null if it doesn't
            //have a reference to the key - it should not have any other errors.
            ModelValue value = store.getModelValue(key.getKeyPath());
            if (value != null) {
                return value.getData();
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Performs a set on a UXKey, changing the value for the key.
     *
     * @param key   A valid settable key
     * @param value A value object relevant to the given key
     * @return Completable which indicates success or error setting the value.
     */
    @Override
    @NonNull
    public Completable setValue(@NonNull UXKey key, @NonNull Object value) {
        lock.lock();
        try {
            return Completable.create(emitter -> {
                if (value.getClass().equals(key.getValueType())) {
                    ModelValue previousValue = store.getModelValue(key.getKeyPath());
                    if (key.getUpdateType() == UXKeys.UpdateType.ON_EVENT ||
                            (key.getUpdateType() == UXKeys.UpdateType.ON_CHANGE &&
                                    (previousValue == null || !previousValue.getData().equals(value)))) {
                        ModelValue currentValue = new ModelValue(value);
                        store.setModelValue(currentValue, key.getKeyPath());
                        if (keyStringProcessorMap.containsKey(key.getKeyPath())) {
                            PublishProcessor<BroadcastValues> processor = keyStringProcessorMap.get(key.getKeyPath());
                            if (processor != null){
                                processor.onNext(new BroadcastValues(previousValue, currentValue));
                            }
                        }
                    }
                    emitter.onComplete();
                } else {
                    emitter.onError(new UXSDKError(UXSDKErrorDescription.FACTORY.build(UXSDKErrorDescription.VALUE_TYPE_MISMATCH)));
                }
            }).subscribeOn(SchedulerProvider.computation());
        } finally {
            lock.unlock();
        }
    }

    private static class SingletonHolder {
        private static final ObservableInMemoryKeyedStore instance = new ObservableInMemoryKeyedStore();
    }
}
