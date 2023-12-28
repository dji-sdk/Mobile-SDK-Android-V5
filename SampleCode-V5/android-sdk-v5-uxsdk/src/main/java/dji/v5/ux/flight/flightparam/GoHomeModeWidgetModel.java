package dji.v5.ux.flight.flightparam;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.flightcontroller.GoHomePathMode;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;


public class GoHomeModeWidgetModel extends WidgetModel {

    private final DataProcessor<GoHomePathMode> goHomePathModeDataProcessor = DataProcessor.create(GoHomePathMode.UNKNOWN);
    private final DataProcessor<ProductType> productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN);


    protected GoHomeModeWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productTypeProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGoHomePathMode), goHomePathModeDataProcessor);

    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    @NonNull
    public Flowable<GoHomePathMode> goHomePathModeFlowable() {
        return goHomePathModeDataProcessor.toFlowable();
    }

    @NonNull Flowable<ProductType> getProductTypeFlowable() {
        return  productTypeProcessor.toFlowable();
    }
    //todo checkvalue
    boolean isSupportGoHomeMode(){
       return productTypeProcessor.getValue() == ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES;
    }

    public Flowable<GoHomePathMode> setGoHomePathMode(GoHomePathMode value) {
       return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyGoHomePathMode) ,value).toFlowable();
    }
}
