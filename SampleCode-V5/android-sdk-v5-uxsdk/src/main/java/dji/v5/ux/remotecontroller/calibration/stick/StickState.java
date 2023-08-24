package dji.v5.ux.remotecontroller.calibration.stick;

import androidx.annotation.Nullable;

import java.util.Objects;

import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;

/**
 * Created by Kurt.Ren on 2020/6/4.
 * kurt.ren@dji.com
 */
public class StickState {
    public boolean isConnection;
    public RcCalibrateState calibrationState;
    public int rightTop;
    public int rightRight;
    public int rightBottom;
    public int rightLeft;
    public int segmentNum;
    public int leftTop;
    public int leftRight;

    public StickState() {
        //do nothing
    }

    public int leftBottom;

    public boolean isConnection() {
        return isConnection;
    }

    public void setConnection(boolean connection) {
        isConnection = connection;
    }

    public RcCalibrateState getCalibrationState() {
        return calibrationState;
    }

    public void setCalibrationState(RcCalibrateState calibrationState) {
        this.calibrationState = calibrationState;
    }

    public int getRightTop() {
        return rightTop;
    }

    public void setRightTop(int rightTop) {
        this.rightTop = rightTop;
    }

    public int getRightRight() {
        return rightRight;
    }

    public void setRightRight(int rightRight) {
        this.rightRight = rightRight;
    }

    public int getRightBottom() {
        return rightBottom;
    }

    public void setRightBottom(int rightBottom) {
        this.rightBottom = rightBottom;
    }

    public int getRightLeft() {
        return rightLeft;
    }

    public void setRightLeft(int rightLeft) {
        this.rightLeft = rightLeft;
    }

    public int getSegmentNum() {
        return segmentNum;
    }

    public void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    public int getLeftTop() {
        return leftTop;
    }

    public void setLeftTop(int leftTop) {
        this.leftTop = leftTop;
    }

    public int getLeftRight() {
        return leftRight;
    }

    public void setLeftRight(int leftRight) {
        this.leftRight = leftRight;
    }

    public int getLeftBottom() {
        return leftBottom;
    }

    public void setLeftBottom(int leftBottom) {
        this.leftBottom = leftBottom;
    }

    public int getLeftLeft() {
        return leftLeft;
    }

    public void setLeftLeft(int leftLeft) {
        this.leftLeft = leftLeft;
    }

    public int leftLeft;


    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof StickState){
            StickState newState = (StickState)obj;
            if(newState.isConnection == isConnection &&
                newState.segmentNum == segmentNum &&
                newState.leftTop == leftTop &&
                newState.leftRight == leftRight &&
                newState.leftBottom  == leftBottom &&
                newState.leftLeft == leftLeft &&
                newState.rightTop == rightTop &&
                newState.rightRight == rightRight &&
                newState.rightBottom == rightBottom &&
                newState.rightLeft ==rightLeft){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isConnection, calibrationState, rightTop, rightRight, rightBottom, rightLeft, segmentNum, leftTop, leftRight,
                leftBottom, leftLeft);
    }

    @Override
    public String toString() {
        return "StickState{"
            + "isConnection="
            + isConnection
            + ", calibrationState="
            + calibrationState
            + ", rightTop="
            + rightTop
            + ", rightRight="
            + rightRight
            + ", rightBottom="
            + rightBottom
            + ", rightLeft="
            + rightLeft
            + ", segmentNum="
            + segmentNum
            + ", leftTop="
            + leftTop
            + ", leftRight="
            + leftRight
            + ", leftBottom="
            + leftBottom
            + ", leftLeft="
            + leftLeft
            + '}';
    }
}
