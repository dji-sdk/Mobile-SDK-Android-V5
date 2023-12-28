package dji.v5.ux.flight.flightparam;


import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.GoHomePathMode;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class DistanceLimitWidgetModel extends WidgetModel {

    private final DataProcessor<Integer> goHomeHeightDataProcessor = DataProcessor.create(500);
    private final DataProcessor<Integer> heightLimitDataProcessor = DataProcessor.create(500);
    private final DataProcessor<Integer> distanceLimitDataProcessor = DataProcessor.create(5000);
    private final DataProcessor<Boolean> distanceLimitEnableDataProcessor = DataProcessor.create(false);
    private final DataProcessor<GoHomePathMode> goHomePathModeProcessor = DataProcessor.create(GoHomePathMode.UNKNOWN);

    protected DistanceLimitWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), goHomeHeightDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHeightLimit), heightLimitDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyDistanceLimit) , distanceLimitDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled) , distanceLimitEnableDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGoHomePathMode) , goHomePathModeProcessor);
    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    public Flowable<GoHomePathMode> getGoHomePathMode(){
        return goHomePathModeProcessor.toFlowable();
    }

    public Flowable<Integer> getGoHomeHeight(){
        return goHomeHeightDataProcessor.toFlowable();
    }


    public Completable setGoHomeHeight(int value) {
       return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight) , value);
    }


    public Flowable<Integer> getHomeLimitHeight(){
        return heightLimitDataProcessor.toFlowable();
    }

    public Completable setHeightLimit(int value) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyHeightLimit) , value);
    }


    public Flowable<Integer> getDistanceLimit(){
        return distanceLimitDataProcessor.toFlowable();
    }

    public Completable setDistanceLimit(int value) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyDistanceLimit) , value);
    }

    public Flowable<Boolean> getDistanceLimitEnabled(){
        return distanceLimitEnableDataProcessor.toFlowable();
    }

    public Completable setDistanceLimitEnabled(boolean value) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled) , value);
    }

}
