package dji.v5.ux.core.widget.hd.frequency;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.sdk.keyvalue.value.airlink.FrequencyInterferenceInfo;
import dji.sdk.keyvalue.value.airlink.SDRHdOffsetParams;
import dji.sdk.keyvalue.value.common.IntMinMax;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class FreqViewModel extends WidgetModel {
    private DataProcessor<Integer> frequencyPointIndexProcessor = DataProcessor.create(0);
    private DataProcessor<ChannelSelectionMode> channelSelectionModeDataProcessor = DataProcessor.create(ChannelSelectionMode.UNKNOWN);
    private DataProcessor<Bandwidth> bandwidthDataProcessor = DataProcessor.create(Bandwidth.UNKNOWN);
    private DataProcessor<List<FrequencyInterferenceInfo>> frequencyInterfaceInfoProcessor = DataProcessor.create(new ArrayList<>());
    private DataProcessor<IntMinMax> frequencyPointIndexRangeProcessor = DataProcessor.create(new IntMinMax());
    private DataProcessor<SDRHdOffsetParams> sdrHdOffsetParamsProcessor = DataProcessor.create(new SDRHdOffsetParams());
    private DataProcessor<FrequencyBand> frequencyBandDataProcessor = DataProcessor.create(FrequencyBand.UNKNOWN);
    private DataProcessor<Boolean> isDualBandSupportedProcessor = DataProcessor.create(false);
    private DataProcessor<Boolean> is5Dot7GSupportedProcessor = DataProcessor.create(false);

    protected FreqViewModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyInterference), frequencyInterfaceInfoProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyPoint), frequencyPointIndexProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), channelSelectionModeDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyBandwidth), bandwidthDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyPointRange), frequencyPointIndexRangeProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeySDRHdOffsetParams), sdrHdOffsetParamsProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyBand), frequencyBandDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyIsDualBandSupported), isDualBandSupportedProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyIs5Dot7GSupported), is5Dot7GSupportedProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    public Flowable<Boolean> isDualBandSupported() {
        return isDualBandSupportedProcessor.toFlowableOnUI();
    }

    public Flowable<Boolean> is5Dot7GSupported() {
        return is5Dot7GSupportedProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getFrequencyPointIndex() {
        return frequencyPointIndexProcessor.toFlowableOnUI();
    }

    public Completable setFrequencyPointIndex(Integer value) {
        return djiSdkModel.setValue(KeyTools.createKey(AirLinkKey.KeyFrequencyPoint), value);
    }

    public Flowable<ChannelSelectionMode> getChannelSelectionMode() {
        return channelSelectionModeDataProcessor.toFlowableOnUI();
    }

    public Flowable<Bandwidth> getBandwidth() {
        return bandwidthDataProcessor.toFlowableOnUI();
    }

    public Flowable<List<FrequencyInterferenceInfo>> getFrequencyInterfaceInfoList() {
        return frequencyInterfaceInfoProcessor.toFlowableOnUI();
    }

    public Flowable<IntMinMax> getFrequencyPointIndexRange() {
        return frequencyPointIndexRangeProcessor.toFlowableOnUI();
    }

    public Flowable<SDRHdOffsetParams> getSDRHdOffsetParams() {
        return sdrHdOffsetParamsProcessor.toFlowableOnUI();
    }

    public Flowable<FrequencyBand> getFrequencyBand() {
        return frequencyBandDataProcessor.toFlowableOnUI();
    }
}
