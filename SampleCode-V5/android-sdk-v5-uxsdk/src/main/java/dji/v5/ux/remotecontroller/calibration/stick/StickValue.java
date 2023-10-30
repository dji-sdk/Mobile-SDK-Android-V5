package dji.v5.ux.remotecontroller.calibration.stick;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2023/8/18
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class StickValue {
    public int vertical;
    public int horizontal;

    public StickValue() {

    }

    public StickValue(int vertical, int horizontal) {
        this.vertical = vertical;
        this.horizontal = horizontal;
    }

    @Override
    public String toString() {
        return "StickValue{" +
                "vertical=" + vertical +
                ", horizontal=" + horizontal +
                '}';
    }
}
