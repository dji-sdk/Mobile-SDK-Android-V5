/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.util;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This class contains enums for UX SDK settings.
 */
public final class SettingDefinitions {

    private SettingDefinitions() {
        //Empty Constructor
    }

    /**
     * The gimbal index to determine control over aircraft with multiple gimbals.
     */
    public enum GimbalIndex {
        /**
         * The gimbal index corresponds to the port side of the aircraft.
         */
        PORT(0),
        /**
         * The gimbal index corresponds to the starboard side of the aircraft.
         */
        STARBOARD(1),
        /**
         * The gimbal index corresponds to the top of the aircraft.
         */
        TOP(2),

        UNKONWN(3);

        private int index;

        GimbalIndex(int index) {
            this.index = index;
        }

        private static GimbalIndex[] values;

        public static GimbalIndex[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        @Nullable
        public static GimbalIndex find(@IntRange(from = 0, to = 2) int index) {
            for (GimbalIndex gimbalIndex : GimbalIndex.getValues()) {
                if (gimbalIndex.getIndex() == index) {
                    return gimbalIndex;
                }
            }
            return null;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * The camera's location on the aircraft.
     */
    public enum CameraSide {
        /**
         * The camera is on the port side of the aircraft.
         */
        PORT("Port-side"),
        /**
         * The camera is on the starboard side of the aircraft.
         */
        STARBOARD("Starboard-side"),
        /**
         * The camera is on top of the aircraft.
         */
        TOP("Top-side"),
        /**
         * The camera is on an unknown side of the aircraft.
         */
        UNKNOWN("Unknown");

        private String side;

        CameraSide(String side) {
            this.side = side;
        }

        private static CameraSide[] values;

        public static CameraSide[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static CameraSide find(String side) {
            CameraSide result = UNKNOWN;
            for (int i = 0; i < getValues().length; i++) {
                if (getValues()[i].side.equals(side)) {
                    result = getValues()[i];
                    break;
                }
            }
            return result;
        }

        @Override
        @NonNull
        public String toString() {
            return side;
        }
    }

    /**
     * The mode which will be controlled by tapping the
     * FPVInteractionWidget.
     */
    public enum ControlMode {
        /**
         * The camera will perform spot metering when the
         * FPVInteractionWidget is tapped.
         */
        SPOT_METER(0),
        /**
         * The camera will perform center metering.
         */
        CENTER_METER(1),
        /**
         * The camera will set an auto focus target
         * FPVInteractionWidget is tapped.
         */
        AUTO_FOCUS(2),
        /**
         * The camera will set a manual focus target when the
         * FPVInteractionWidget is tapped.
         */
        MANUAL_FOCUS(3),
        /**
         * The camera will set an auto focus continuous target when the
         * FPVInteractionWidget is tapped.
         */
        AUTO_FOCUS_CONTINUE(4);

        private int value;

        ControlMode(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        private boolean _equals(int b) {
            return value == b;
        }

        private static ControlMode[] values;

        public static ControlMode[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static ControlMode find(int value) {
            ControlMode result = SPOT_METER;
            for (int i = 0; i < getValues().length; i++) {
                if (getValues()[i]._equals(value)) {
                    result = getValues()[i];
                    break;
                }
            }
            return result;
        }

    }

    /**
     * The enum defines the various Map Providers
     * to be used with the MapWidget
     */
    public enum MapProvider {
        /**
         * Unknown Provider
         */
        UNKNOWN(-1),
        /**
         * Google Maps Provider
         */
        GOOGLE(0),
        /**
         * Here Maps Provider
         */
        HERE(1),
        /**
         * AMap Provider
         */
        AMAP(2),
        /**
         * MapLibre Provider
         */
        MAPLIBRE(3);

        private int index;

        MapProvider(int index) {
            this.index = index;
        }

        private static MapProvider[] values;

        public static MapProvider[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        @NonNull
        public static MapProvider find(@IntRange(from = -1, to = 3) int index) {
            for (MapProvider mapProvider : MapProvider.getValues()) {
                if (mapProvider.getIndex() == index) {
                    return mapProvider;
                }
            }
            return UNKNOWN;
        }

        public int getIndex() {
            return index;
        }
    }


}
