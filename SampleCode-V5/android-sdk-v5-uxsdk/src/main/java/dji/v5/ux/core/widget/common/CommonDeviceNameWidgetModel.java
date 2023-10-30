package dji.v5.ux.core.widget.common;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class CommonDeviceNameWidgetModel extends WidgetModel {

    protected CommonDeviceNameWidgetModel(
            @NonNull DJISDKModel djiSdkModel,
            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        // do noting
    }

    @Override
    protected void inCleanup() {
        // do noting
    }

    public Single<String> getAircraftName() {
        return djiSdkModel.getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftName))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable setAircraftName(String name) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyAircraftName), name)
                .observeOn(AndroidSchedulers.mainThread());
    }
}
