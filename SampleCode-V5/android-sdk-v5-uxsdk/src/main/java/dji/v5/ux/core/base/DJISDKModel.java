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

package dji.v5.ux.core.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.key.DJIKey;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Encapsulates communication with SDK KeyManager for SDKKeys.
 */
public class DJISDKModel {

    //region Fields
    private final String TAG = LogUtils.getTag(this);
    private static final int MAX_COMPONENT_INDEX = 10;
    //endregion

    private DJISDKModel() {
        //设置所有RX的全局Error代理，为了避免某些Widget不设置errorConsumer而导致crash。
        // 如果某个Widget有添加特定的error，则调用特定的error；否则调用全局的error
        RxJavaPlugins.setErrorHandler(throwable -> LogUtils.e(TAG, throwable.getMessage()));
    }

    public static DJISDKModel getInstance() {
        return DJISDKModel.SingletonHolder.instance;
    }

    /**
     * Stops observing changes of all keys registered for the given listener.
     *
     * @param listener The listener to unregister.
     */
    public void removeListener(@NonNull final Object listener) {
        removeKeyListeners(listener).subscribe();
    }

    /**
     * Subscribes the listener object to all changes of value on the given  key.
     *
     * @param key      A valid value-based key (get, set and/or action)
     * @param listener Listener that is subscribing.
     * @return A flowable that emits objects based on a key.
     */
    @NonNull
    public <T> Flowable<T> addListener(@NonNull final DJIKey<T> key, @NonNull final Object listener) {
        return Flowable.create((FlowableOnSubscribe<T>) emitter -> registerKey(emitter, key, listener), BackpressureStrategy.LATEST)
                .subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Performs a get on a gettable key, pulling the information from the product if
     * necessary.
     *
     * @param key A valid gettable key.
     * @return A single that emits one object based on a key.
     */
    @NonNull
    public <T> Single<T> getValue(@NonNull final DJIKey<T> key) {
        return Single.create((SingleOnSubscribe<T>) emitter -> KeyManager.getInstance().getValue(key,
                new CommonCallbacks.CompletionCallbackWithParam<T>() {
                    @Override
                    public void onSuccess(@NonNull T value) {
                        LogUtils.d(TAG, "Got current value for  key " + key.toString());
                        emitter.onSuccess(value);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError djiError) {
                        LogUtils.d(TAG, "Failure getting key " + key.toString() + ". " + djiError.toString());
                    }
                })).subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Returns the latest known value if available for the key. Does not pull it from
     * the product if unavailable.
     *
     * @param key An instance of DJIKey.
     * @return The value associated with the key.
     */
    @Nullable
    public <T> T getCacheValue(@NonNull final DJIKey<T> key) {
        return KeyManager.getInstance().getValue(key);
    }

    /**
     * Performs a set on a settable key, changing attributes on the connected product.
     *
     * @param key   A valid settable key.
     * @param value A value object relevant to the given key.
     * @return A completable indicating success/error setting the value.
     */
    @NonNull
    public <T> Completable setValue(@NonNull final DJIKey<T> key, @NonNull final T value) {
        return Completable.create(
                emitter -> KeyManager.getInstance().setValue(key, value, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError djiError) {
                                LogUtils.e(TAG, key, value, djiError);
                                emitter.onError(new UXSDKError(djiError));
                            }
                        }
                )).subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Performs an action on an actionable key.
     *
     * @param key      A valid actionable key.
     * @param argument Optional arguments relevant to the specific key.
     * @return A completable indicating success/error performing the action.
     */
    @NonNull
    public <Param, Result> Completable performAction(@NonNull final DJIKey.ActionKey<Param, Result> key,
                                                     final Param argument) {
        return Completable.create(emitter -> KeyManager.getInstance().performAction(key, argument,
                new CommonCallbacks.CompletionCallbackWithParam<Result>() {
                    @Override
                    public void onSuccess(@NonNull Object o) {
                        emitter.onComplete();
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError djiError) {
                        LogUtils.e(TAG, "Failure performing action key " + key.toString() + ". " + djiError.toString());
                        //emitter.onError(new UXSDKError(djiError));
                    }
                })).subscribeOn(SchedulerProvider.computation());
    }

    @NonNull
    public <Param, Result> Completable performAction(@NonNull final DJIKey.ActionKey<Param, Result> key) {
        return performAction(key, null);
    }

    /**
     * Determines if a key is supported by the connected product.
     *
     * @param key Key to be check on current product.
     * @return `true` if the key is supported.
     */
    public <T> boolean isKeySupported(DJIKey<T> key) {
        return KeyManager.getInstance().isKeySupported(key);
    }

    private <T> void registerKey(@NonNull final FlowableEmitter<T> emitter,
                                 @NonNull final DJIKey<T> key,
                                 @NonNull final Object listener) {
        // Get current value.这里改为采用同步方式获取值，一是避免异步带来的执行时机不确定；二是listen内部也会通过异常的形式获取一次最新值
        T value = KeyManager.getInstance().getValue(key);
        if (value != null) {
            emitter.onNext((T) value);
            LogUtils.d(TAG, "Got current value for  key ", ":", value);
        }


        // Start listening to changes
        CommonCallbacks.KeyListener<T> keyListener = (oldValue, newValue) -> {
            if (newValue != null && !emitter.isCancelled()) {
                emitter.onNext(newValue);
            }
        };
        KeyManager.getInstance().listen(key, listener, keyListener);
    }

    private Flowable<Boolean> removeKeyListeners(final Object listener) {
        if (listener == null) {
            return Flowable.just(true);
        }
        return Flowable.just(true).doOnSubscribe(subscription -> KeyManager.getInstance().cancelListen(listener));
    }

    private static class SingletonHolder {
        private static final DJISDKModel instance = new DJISDKModel();
    }
}