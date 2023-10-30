package dji.v5.ux.core.widget.hd;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class FrequencyTabSelectWidgetModel extends WidgetModel {

    private DataProcessor<FrequencyBand> airlinkFrequencyBandMsgDataProcessor = DataProcessor.create(FrequencyBand.UNKNOWN);
    private DataProcessor<List<FrequencyBand>> airlinkFrequencyBandRangeMsgDataProcessor = DataProcessor.create(new ArrayList<>());
    private DataProcessor<ChannelSelectionMode> channelSelectionModeDataProcessor = DataProcessor.create(ChannelSelectionMode.UNKNOWN);
    private DataProcessor<Boolean> connectionProcessor = DataProcessor.create(false);
    protected FrequencyTabSelectWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyBand), airlinkFrequencyBandMsgDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyBandRange), airlinkFrequencyBandRangeMsgDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), channelSelectionModeDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyConnection), connectionProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    public Completable setFrequencyBand(FrequencyBand value) {
        return djiSdkModel.setValue(KeyTools.createKey(AirLinkKey.KeyFrequencyBand), value);
    }

    public Completable setChannelSelectionMode(ChannelSelectionMode value) {
        return djiSdkModel.setValue(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), value);
    }

    public Flowable<FrequencyBand> getFrequencyBand() {
        return airlinkFrequencyBandMsgDataProcessor.toFlowableOnUI();
    }

    public Flowable<ChannelSelectionMode> getChannelSelectionMode() {
        return channelSelectionModeDataProcessor.toFlowableOnUI();
    }

    public Flowable<List<FrequencyBand>> getFrequencyBandRange() {
        return airlinkFrequencyBandRangeMsgDataProcessor.toFlowableOnUI();
    }

    public Flowable<Boolean> getConnection() {
        return connectionProcessor.toFlowableOnUI();
    }

}
