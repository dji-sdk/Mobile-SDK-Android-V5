package dji.v5.ux.flight.flightparam;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.FailsafeAction;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author feel.feng
 * @time 2023/08/11 11:20
 * @description:
 */
public class LostActionWidgetModel extends WidgetModel {
    private final DataProcessor<FailsafeAction> lostActionDataprocesser = DataProcessor.create(FailsafeAction.UNKNOWN);

    protected LostActionWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFailsafeAction), lostActionDataprocesser);
    }

    @Override
    protected void inCleanup() {
        //do nothing
    }


    public Flowable<FailsafeAction>  setLostAction(FailsafeAction value){
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyFailsafeAction) ,value).toFlowable();
    }

    @NonNull
    public Flowable<FailsafeAction> getLostActionFlowable(){
        return lostActionDataprocesser.toFlowable();
    }
}
