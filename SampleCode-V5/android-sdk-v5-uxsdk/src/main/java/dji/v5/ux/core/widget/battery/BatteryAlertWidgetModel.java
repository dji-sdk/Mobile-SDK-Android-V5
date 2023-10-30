package dji.v5.ux.core.widget.battery;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class BatteryAlertWidgetModel extends WidgetModel {

    private final DataProcessor<Integer> lowBatteryWarningProcessor = DataProcessor.create(0);
    private final DataProcessor<Integer> seriousLowBatteryWarningProcessor = DataProcessor.create(0);
    private final DataProcessor<Boolean> connectionProcessor = DataProcessor.create(false);

    protected BatteryAlertWidgetModel(
            @NonNull DJISDKModel djiSdkModel,
            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), lowBatteryWarningProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), seriousLowBatteryWarningProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyConnection), connectionProcessor);

    }

    @Override
    protected void inCleanup() {
        // do noting
    }

    public Completable changeLowBatteryWarning(int value) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), value);
    }

    public Completable changeSeriousLowBatteryWarning(int value) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), value);
    }

    public Flowable<Integer> getLowBatteryWarning() {
        return lowBatteryWarningProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getSeriousLowBatteryWarning() {
        return seriousLowBatteryWarningProcessor.toFlowableOnUI();
    }

    public Flowable<Boolean> getConnection() {
        return connectionProcessor.toFlowableOnUI();
    }
}
