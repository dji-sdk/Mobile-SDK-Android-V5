package dji.v5.ux.core.widget.hd;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.FrequencyInterferenceInfo;
import dji.sdk.keyvalue.value.airlink.SDRHdOffsetParams;
import dji.sdk.keyvalue.value.common.IntMinMax;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

public class InfoWidgetModel extends WidgetModel {
    private DataProcessor<Bandwidth> bandwidthDataProcessor = DataProcessor.create(Bandwidth.UNKNOWN);
    private DataProcessor<List<FrequencyInterferenceInfo>> frequencyInterfaceInfoProcessor = DataProcessor.create(new ArrayList<>());
    private DataProcessor<IntMinMax> frequencyPointIndexRangeProcessor = DataProcessor.create(new IntMinMax());
    private DataProcessor<SDRHdOffsetParams> sdrHdOffsetParamsDataProcessor = DataProcessor.create(new SDRHdOffsetParams());
    private DataProcessor<Double> dynamicDataRateProcessor = DataProcessor.create(0.0);
    private DataProcessor<Integer> frequencyPointIndexProcessor = DataProcessor.create(0);

    private DataProcessor<Integer> downLinkQualityProcessor = DataProcessor.create(0);

    private DataProcessor<Integer> upLinkQualityProcessor = DataProcessor.create(0);

    private DataProcessor<Integer> linkSignalQualityProcessor = DataProcessor.create(0);

    private DataProcessor<Boolean> fcConnectionProcessor = DataProcessor.create(false);



    protected InfoWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyBandwidth), bandwidthDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyInterference), frequencyInterfaceInfoProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyPointRange), frequencyPointIndexRangeProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeySDRHdOffsetParams), sdrHdOffsetParamsDataProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyDynamicDataRate), dynamicDataRateProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyFrequencyPoint), frequencyPointIndexProcessor);

        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyDownLinkQuality), downLinkQualityProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyUpLinkQuality), upLinkQualityProcessor);
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyLinkSignalQuality), linkSignalQualityProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyConnection), fcConnectionProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
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

    public Flowable<SDRHdOffsetParams> getSdrHdOffsetParamsData() {
        return sdrHdOffsetParamsDataProcessor.toFlowableOnUI();
    }

    public Flowable<Double> getDynamicDataRate() {
        return dynamicDataRateProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getFrequencyPointIndex() {
        return frequencyPointIndexProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getDownLinkQuality() {
        return downLinkQualityProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getUpLinkQuality() {
        return upLinkQualityProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getLinkSignalQuality() {
        return linkSignalQualityProcessor.toFlowableOnUI();
    }

    public Flowable<Boolean> getFcConnection() {
        return fcConnectionProcessor.toFlowableOnUI();
    }
}
