package dji.v5.ux.remotecontroller.calibration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;

/**
 * Created by Kurt.Ren on 2020/6/5.
 * kurt.ren@dji.com
 */
public class SmartControllerCalibrationInfo {
    public  boolean connection;
    public  RcCalibrateState calibrationState =RcCalibrateState.UNKNOWN;
    public  int leftGyroValue;
    public  int rightGyroValue;

    public SmartControllerCalibrationInfo() {

    }


    public SmartControllerCalibrationInfo(boolean connection, RcCalibrateState calibrationState, int leftGyroValue, int rightGyroValue) {
        this.connection = connection;
        this.calibrationState = calibrationState;
        this.leftGyroValue = leftGyroValue;
        this.rightGyroValue = rightGyroValue;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof SmartControllerCalibrationInfo) {
            SmartControllerCalibrationInfo newState = (SmartControllerCalibrationInfo) obj;
            if (newState.connection == connection
                    && newState.calibrationState == calibrationState
                    && newState.leftGyroValue == leftGyroValue
                    && newState.rightGyroValue == rightGyroValue) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection, calibrationState, leftGyroValue, rightGyroValue);
    }

    @NonNull
    @Override
    public String toString() {
        return "SmartControllerCalibrationInfo{"
                + "\nisConnection:" + connection
                + "\ncalibrationState:" + (calibrationState == null ? "null" : calibrationState.name())
                + "\ngyroValue:" + leftGyroValue
                + "\nrightGyroValue:" + rightGyroValue
                + "\n}";
    }
}
