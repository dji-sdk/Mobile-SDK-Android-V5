package dji.v5.ux.core.widget.hd;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class BandWidthSelectWidgetModel extends WidgetModel {
    private DataProcessor<Bandwidth> bandwidthDataProcessor = DataProcessor.create(Bandwidth.UNKNOWN);
    private DataProcessor<ChannelSelectionMode> channelSelectionModeDataProcessor = DataProcessor.create(ChannelSelectionMode.UNKNOWN);
    private DataProcessor<FrequencyBand> frequencyBandDataProcessor = DataProcessor.create(FrequencyBand.UNKNOWN);

    protected BandWidthSelectWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyBandwidth), bandwidthDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), channelSelectionModeDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyBand), frequencyBandDataProcessor);
    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    public Flowable<Bandwidth> getBandwidth() {
        return bandwidthDataProcessor.toFlowableOnUI();
    }

    public Flowable<ChannelSelectionMode> getChannelSelectionMode(){
        return channelSelectionModeDataProcessor.toFlowableOnUI();
    }

    public Flowable<FrequencyBand> getFrequencyBand(){
        return frequencyBandDataProcessor.toFlowableOnUI();
    }

    public Completable setBandwidth(Bandwidth value) {
        return djiSdkModel.setValue(KeyTools.createKey(AirLinkKey.KeyBandwidth), value);
    }
}
