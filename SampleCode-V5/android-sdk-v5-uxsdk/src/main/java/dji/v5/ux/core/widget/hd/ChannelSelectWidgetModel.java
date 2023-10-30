package dji.v5.ux.core.widget.hd;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class ChannelSelectWidgetModel extends WidgetModel {
    private DataProcessor<ChannelSelectionMode> channelSelectionModeProcessor = DataProcessor.create(ChannelSelectionMode.UNKNOWN);

    protected ChannelSelectWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), channelSelectionModeProcessor);
    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    public Flowable<ChannelSelectionMode> getChannelSelectionMode() {
        return channelSelectionModeProcessor.toFlowableOnUI();
    }

    public Completable setChannelSelectionMode(ChannelSelectionMode value) {
        return djiSdkModel.setValue(KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode), value);
    }
}
