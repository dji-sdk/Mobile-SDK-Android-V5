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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.communication.UXKey;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Base WidgetModel class to be extended by all the individual
 * widget models
 */
public abstract class WidgetModel {

    //region Constants
    public final String tag = LogUtils.getTag(this);
    //endregion
    //region Fields
    /**
     * Allows communication with the SDK KeyManager using the DJIKey keyed interface.
     */
    protected final DJISDKModel djiSdkModel;
    /**
     * Allows communication using the UXKeys keyed interface.
     */
    protected final ObservableInMemoryKeyedStore uxKeyManager;
    protected DataProcessor<Boolean> productConnectionProcessor;
    private CompositeDisposable keyDisposables;
    private CompositeDisposable compositeDisposable;
    private final List<BaseModule> moduleList = new ArrayList<>();
    private StatesChangeListener statesChangedListener;
    //endregion

    //region Default Constructor
    protected WidgetModel(@NonNull DJISDKModel djiSdkModel,
                       @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        this.djiSdkModel = djiSdkModel;
        this.uxKeyManager = uxKeyManager;
        productConnectionProcessor = DataProcessor.create(false);
    }
    //endregion

    //region Lifecycle

    protected void addModule(@NonNull BaseModule baseModule) {
        if (isStarted()) {
            throw new IllegalStateException("WidgetModel is already setup. Modules should" +
                    " be added during initialization.");
        }
        if (!moduleList.contains(baseModule)) {
            moduleList.add(baseModule);
        }
    }

    /**
     * Set up the widget model by initializing all the required resources
     */
    public synchronized void setup() {
        if (isStarted()) {
            throw new IllegalStateException("WidgetModel is already setup. Call cleanup first.");
        }
        keyDisposables = new CompositeDisposable();
        compositeDisposable = new CompositeDisposable();
        initializeConnection();
        inSetup();
        for (BaseModule module : moduleList) {
            module.setup(this);
        }
    }

    /**
     * Clean up the widget model by destroying all the resources used
     */
    public synchronized void cleanup() {
        if (keyDisposables != null) {
            keyDisposables.dispose();
            keyDisposables = null;
        }

        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }

        djiSdkModel.removeListener(this);

        for (BaseModule module : moduleList) {
            module.cleanup();
        }

        statesChangedListener = null;
        inCleanup();
    }

    /**
     * Restart the widget model by cleaning up and then setting up the widget model again
     */
    protected void restart() {
        if (isStarted()) {
            cleanup();
            setup();
        }
    }

    /**
     * Setup method for initialization that must be implemented
     */
    protected abstract void inSetup();

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    protected abstract void inCleanup();

    /**
     * Method to update states for the required processors in the child classes as required
     */
    protected void updateStates() {
        StatesChangeListener listener = statesChangedListener;
        if (listener != null) {
            listener.onStatesChanged();
        }
    }

    private boolean isStarted() {
        return keyDisposables != null;
    }

    /**
     * Add a disposable to the composite disposable list to ensure cleanup after completion
     *
     * @param disposable Disposable to be added
     */
    protected void addDisposable(@NonNull Disposable disposable) {
        if (compositeDisposable != null) {
            compositeDisposable.add(disposable);
        }
    }

    protected void removeDisposable(@NonNull Disposable disposable) {
        if (compositeDisposable != null) {
            compositeDisposable.remove(disposable);
        }
    }

    private void initializeConnection() {
        DJIKey<Boolean> productConnectionKey = KeyTools.createKey(ProductKey.KeyConnection);
        bindDataProcessor(productConnectionKey, productConnectionProcessor, newValue -> onProductConnectionChanged((boolean) newValue));
    }

    protected void onProductConnectionChanged(boolean isConnected) {
        // do nothing
    }
    //endregion

    /**
     * Bind the given DJIKey to the given data processor. This data processor will be
     * invoked with every update to the key.
     *
     * @param key           DJIKey to be bound
     * @param dataProcessor DataProcessor to be bound
     */
    protected <T> void bindDataProcessor(@NonNull DJIKey<T> key, @NonNull DataProcessor<T> dataProcessor) {
        bindDataProcessor(key, dataProcessor, o -> {
        });
    }

    /**
     * Bind the given DJIKey to the given data processor and attach the given consumer to it.
     * The data processor and side effect consumer will be invoked with every update to the key.
     * The side effect consumer will be called before the data processor is updated.
     *
     * @param key                DJIKey to be bound
     * @param dataProcessor      DataProcessor to be bound
     * @param sideEffectConsumer Consumer to be called along with data processor
     */
    protected <T> void bindDataProcessor(@NonNull DJIKey<T> key,
                                         @NonNull DataProcessor<T> dataProcessor,
                                         @NonNull Consumer<T> sideEffectConsumer) {
        registerKey(key, dataProcessor::onNext, sideEffectConsumer);
    }

    /**
     * Bind the given UXKey to the given data processor. This data processor will be
     * invoked with every update to the key.
     *
     * @param key           UXKey to be bound
     * @param dataProcessor DataProcessor to be bound
     */
    protected <T> void bindDataProcessor(@NonNull UXKey key, @NonNull DataProcessor<T> dataProcessor) {
        bindDataProcessor(key, dataProcessor, o -> {
        });
    }

    /**
     * Bind the given UXKey to the given data processor and attach the given consumer to it.
     * The data processor and side effect consumer will be invoked with every update to the key.
     * The side effect consumer will be called before the data processor is updated.
     *
     * @param key                UXKey to be bound
     * @param dataProcessor      DataProcessor to be bound
     * @param sideEffectConsumer Consumer to be called along with data processor
     */
    protected <T> void bindDataProcessor(@NonNull UXKey key,
                                         @NonNull DataProcessor<T> dataProcessor,
                                         @NonNull Consumer<T> sideEffectConsumer) {
        registerKey(key, dataProcessor::onNext, sideEffectConsumer);
    }

    private <T> void registerKey(@NonNull DJIKey<T> djiKey,
                                 @NonNull Consumer<T> bindConsumer,
                                 @NonNull Consumer<T> sideEffectConsumer) {
        if (keyDisposables == null) {
            throw new IllegalStateException("Call this method only when in inSetup");
        }

        keyDisposables.add(djiSdkModel.addListener(djiKey, this)
                .doOnNext(sideEffectConsumer)
                .doOnNext(bindConsumer)
                .subscribe(o -> updateStates(), logError(djiKey)));
    }

    private <T> void registerKey(@NonNull UXKey uxKey,
                                 @NonNull Consumer<T> bindConsumer,
                                 @NonNull Consumer<T> sideEffectConsumer) {
        if (keyDisposables == null) {
            throw new IllegalStateException("Call this method only when in inSetup");
        }
        keyDisposables.add(
                uxKeyManager.addObserver(uxKey)
                        .filter(broadcastValues -> broadcastValues.getCurrentValue().getData() != null)
                        .map(broadcastValues -> broadcastValues.getCurrentValue().getData())
                        .doOnNext(o -> sideEffectConsumer.accept((T) o))
                        .doOnNext(o -> bindConsumer.accept((T) o))
                        .subscribe(o -> updateStates(), logError(uxKey)));
    }

    private <T> Consumer<Throwable> logError(@NonNull DJIKey<T> djiKey) {
        return throwable -> LogUtils.e(tag, "Error registering " + djiKey.toString() + ": " + throwable.getMessage());
    }

    private Consumer<Throwable> logError(@NonNull UXKey uxKey) {
        return throwable -> LogUtils.e(tag, "Error registering " + uxKey.toString() + ": " + throwable.getMessage());
    }

    /**
     * Get the product connection status - true if connected, false otherwise.
     *
     * @return Flowable of boolean type
     */
    public Flowable<Boolean> getProductConnection() {
        return productConnectionProcessor.toFlowable();
    }

    public interface StatesChangeListener {
        void onStatesChanged();
    }
}
