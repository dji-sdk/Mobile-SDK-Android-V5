package dji.v5.ux.remotecontroller;


import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;
import dji.v5.common.utils.RxUtil;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.remotecontroller.calibration.SmartControllerCalibrationInfo;
import dji.v5.ux.remotecontroller.calibration.stick.StickState;
import dji.v5.ux.remotecontroller.calibration.stick.StickValue;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2023/8/17
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class RcCalibrationWidgetModel extends WidgetModel {
    protected boolean isStart = false;
    public DataProcessor<RcCalibrateState> rcCalibrateStateDataProcessor = DataProcessor.create(RcCalibrateState.UNKNOWN);
    public DataProcessor<SmartControllerCalibrationInfo> calibrationInfoDataProcessor = DataProcessor.create(new SmartControllerCalibrationInfo());
    public DataProcessor<Boolean> connectDataProcessor = DataProcessor.create(false);
    public DataProcessor<Boolean> isCalibrateStartProcessor = DataProcessor.create(false);
    public DataProcessor<StickState> stickStateDataProcessor = DataProcessor.create(new StickState());
    public DataProcessor<StickValue> leftStickValueDataProcessor = DataProcessor.create(new StickValue());
    public DataProcessor<StickValue> rightStickValueDataProcessor = DataProcessor.create(new StickValue());
    protected RcCalibrateState rcMode = RcCalibrateState.UNKNOWN;


    public RcCalibrationWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {

        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyConnection, ComponentIndexType.LEFT_OR_MAIN), connectDataProcessor);

        //波轮
        addDisposable(Flowable.combineLatest(RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyConnection,
                ComponentIndexType.LEFT_OR_MAIN), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyLeftDial), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRightDial), this),
                (connect, leftGyroValue, rightGyroValue) -> new SmartControllerCalibrationInfo(connect, rcCalibrateStateDataProcessor.getValue(),
                        leftGyroValue, rightGyroValue)).subscribe(smartControllerCalibrationInfo -> {
            if (smartControllerCalibrationInfo != null) {
                calibrationInfoDataProcessor.onNext(smartControllerCalibrationInfo);
            }
        }));

        addDisposable(Flowable.combineLatest(
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateNumberOfSegment), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateAAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateBAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateCAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateDAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateEAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateFAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateGAxisStatus), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateHAxisStatus), this),
                (segmentNum, aAxis, bAxis, cAxis, dAxis, eAxis, fAxis, gAxis, hAxis) -> {
                    StickState state = new StickState();
                    state.calibrationState = rcCalibrateStateDataProcessor.getValue();
                    state.isConnection = connectDataProcessor.getValue();
                    state.segmentNum = segmentNum;
                    state.rightTop = aAxis;
                    state.rightBottom = bAxis;
                    state.rightRight = cAxis;
                    state.rightLeft = dAxis;
                    state.leftTop = eAxis;
                    state.leftBottom = fAxis;
                    state.leftRight = gAxis;
                    state.leftLeft = hAxis;
                    return state;

                }).

                subscribe(stickState -> {
                    if (stickState != null) {
                        stickStateDataProcessor.onNext(stickState);
                    }
                }));


        //摇杆
        addDisposable(Flowable.combineLatest(
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal), this),
                (StickValue::new))
                .subscribe(stickValue -> {
                    leftStickValueDataProcessor.onNext(stickValue);
                }));

        addDisposable(Flowable.combineLatest(
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyStickRightVertical), this),
                RxUtil.addListener(KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal), this),
                (StickValue::new))
                .subscribe(stickValue -> rightStickValueDataProcessor.onNext(stickValue)));


    }

    @Override
    protected void inCleanup() {
        KeyManager.getInstance().cancelListen(this);
    }

    public void setRcCalibrateChannels(RcCalibrateState rcCalibrateState, boolean isDelay) {
        addDisposable(RxUtil.performActionSingWithResult(KeyTools.createKey(RemoteControllerKey.KeyRcCalibrateChannels), rcCalibrateState)
                .delay(isDelay ? 500 : 0, TimeUnit.MILLISECONDS).subscribe(rcCalibrateState1 -> {
                    if (rcCalibrateState1 != null) {
                        updateRcMode(rcCalibrateState1);
                    } else {
                        doNext(true);
                    }
                }));

    }

    public void startCalibration() {
        doNext(false);
    }

    public void finishCalibration() {
        if (rcMode == RcCalibrateState.EXIT) {
            doNext(false);
        }
    }


    private void doNext(boolean isAuto) {
        LogUtils.d(tag, "doNext with Mode = " + rcMode + "，isAuto=" + isAuto);
        if (rcMode == RcCalibrateState.UNKNOWN) {
            if (!isAuto) {
                isStart = true;
                setRcCalibrateChannels(RcCalibrateState.NORMAL, false);
            }
        } else if (rcMode == RcCalibrateState.NORMAL) {
            if (isStart) {
                setRcCalibrateChannels(RcCalibrateState.RECORDCENTER, false);
            } else {
                // 完成一次完整校准后，发送完Quit，会回到Normbal，这时重置所有状态
                rcMode = RcCalibrateState.UNKNOWN;
            }
        } else if (rcMode == RcCalibrateState.RECORDCENTER) {
            isStart = false;
            setRcCalibrateChannels(RcCalibrateState.LIMITVALUE, true);
        } else if (rcMode == RcCalibrateState.LIMITVALUE) {
            setRcCalibrateChannels(RcCalibrateState.LIMITVALUE, true);
        } else if (rcMode == RcCalibrateState.EXIT) {
            if (!isAuto) {
                setRcCalibrateChannels(RcCalibrateState.EXIT, false);
            } else {
                setRcCalibrateChannels(RcCalibrateState.EXIT, true);
            }
        }
        LogUtils.d(tag, "doNext finish， Mode = " + rcMode);

        if (isCalibrateStartProcessor.getValue() != isStart) {
            isCalibrateStartProcessor.onNext(isStart);
        }
    }

    protected void updateRcMode(RcCalibrateState mode) {
        RcCalibrateState prevMode = rcMode;
        rcMode = mode;
        if (prevMode == rcMode || rcMode == RcCalibrateState.LIMITVALUE
                || (prevMode == RcCalibrateState.NORMAL && rcMode == RcCalibrateState.RECORDCENTER)
                || (prevMode == RcCalibrateState.EXIT && rcMode == RcCalibrateState.NORMAL)
                || isStart) {
            doNext(true);
        }
        if (rcMode != prevMode) {
            rcCalibrateStateDataProcessor.onNext(rcMode);
        }
    }


}
