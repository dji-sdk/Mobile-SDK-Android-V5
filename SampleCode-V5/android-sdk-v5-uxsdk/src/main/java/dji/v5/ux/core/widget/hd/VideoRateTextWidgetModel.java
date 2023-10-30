package dji.v5.ux.core.widget.hd;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

public class VideoRateTextWidgetModel extends WidgetModel {
    private DataProcessor<Double> dynamicDataRateProcessor = DataProcessor.create(0.0);

    protected VideoRateTextWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyDynamicDataRate), dynamicDataRateProcessor);
    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    public Flowable<Double> getDynamicDataRate() {
        return dynamicDataRateProcessor.toFlowableOnUI();
    }
}
