package dji.v5.ux.remotecontroller.calibration;

import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;

public interface CalibrationCallback {
    void onUpdateCalibrationMode(RcCalibrateState mode);
}
