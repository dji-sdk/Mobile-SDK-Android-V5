package dji.v5.ux.core.widget.battery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.battery.BatteryOverviewValue;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

public class BatteryGroupInfoWidgetModel extends WidgetModel {

    private final DataProcessor<Integer> batteryChargeRemainingProcessorForConsume = DataProcessor.create(0);
    private final DataProcessor<Integer> batteryChargeRemainingProcessor = DataProcessor.create(0);
    private final DataProcessor<List<BatteryOverviewValue>> batteryOverviewProcessor = DataProcessor.create(new ArrayList<>());
    private final DataProcessor<Integer> flightTimeInSecondsProcessor = DataProcessor.create(0);
    private final DataProcessor<Boolean> connectionProcessor = DataProcessor.create(false);
    private final DataProcessor<ProductType> productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN);

    public BatteryGroupInfoWidgetModel(
            @NonNull DJISDKModel djiSdkModel,
            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent, ComponentIndexType.AGGREGATION), batteryChargeRemainingProcessor);
        bindDataProcessor(KeyTools.createKey(BatteryKey.KeyBatteryOverviews, ComponentIndexType.AGGREGATION), batteryOverviewProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFlightTimeInSeconds), flightTimeInSecondsProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyConnection), connectionProcessor);

        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productTypeProcessor, productType -> {
            if (ProductUtil.isConsumeMachine()){
                batteryOverviewProcessor.onNext(Collections.singletonList(new BatteryOverviewValue(
                        ComponentIndexType.LEFT_OR_MAIN.value(),
                        true,
                        0,
                        0,
                        0
                )));
            }
        });
        bindDataProcessor(KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent, ComponentIndexType.LEFT_OR_MAIN), batteryChargeRemainingProcessorForConsume, integer -> {
            if (ProductUtil.isConsumeMachine()){
                batteryChargeRemainingProcessor.onNext(integer);
            }
        });
    }

    @Override
    protected void inCleanup() {
        //do noting
    }

    public void reset() {
        batteryChargeRemainingProcessor.onNext(0);
        batteryOverviewProcessor.onNext(new ArrayList<>());
        flightTimeInSecondsProcessor.onNext(0);
        connectionProcessor.onNext(false);
    }

    public Flowable<Integer> getBatteryChargeRemaining() {
        return batteryChargeRemainingProcessor.toFlowableOnUI();
    }

    public Flowable<List<BatteryOverviewValue>> getBatteryOverview() {
        return batteryOverviewProcessor.toFlowableOnUI();
    }

    public Flowable<Integer> getFlightTimeInSeconds() {
        return flightTimeInSecondsProcessor.toFlowableOnUI();
    }

    public Flowable<Boolean> getConnection() {
        return connectionProcessor.toFlowableOnUI();
    }
}
