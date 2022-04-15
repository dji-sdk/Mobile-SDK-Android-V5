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

package dji.v5.ux.core.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * The class represents warning messages.
 * The messages will guide the user through the flight.
 */
public class WarningMessage {

    private static final int DEFAULT_DISPLAY_DURATION = 5;

    /**
     * Warning levels for warning message
     */
    public enum Level {
        /**
         * Message is a notification
         */
        NOTIFY(0),

        /**
         * Message is a warning
         */
        WARNING(1),

        /**
         * Message implies dangerous condition
         */
        DANGEROUS(2);

        private int value;

        Level(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static Level[] values;

        public static Level[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static Level find(int value) {
            for (Level item : getValues()) {
                if (item.getValue() == value) {
                    return item;
                }
            }
            return NOTIFY;
        }
    }

    /**
     * Type of message behavior
     */
    public enum Type {

        /**
         * Message will auto disappear from the screen
         * after the number of seconds specified by showDuration.
         */
        AUTO_DISAPPEAR(0),

        /**
         * Message stays until a removal message is sent.
         */
        PUSH(1),

        /**
         * Message will be pinned above other messages and will
         * have a close button to dismiss it.
         */
        PINNED(2),

        /**
         * Message will be pinned above other messages and cannot
         * be dismissed by user.
         */
        PINNED_NOT_CLOSE(3);

        private int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static Type[] values;

        public static Type[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static Type find(int value) {
            for (Type item : getValues()) {
                if (item.getValue() == value) {
                    return item;
                }
            }
            return AUTO_DISAPPEAR;
        }
    }

    /**
     * Action for warning message
     */
    public enum Action {
        /**
         * Insert warning message
         */
        INSERT,

        /**
         * Remove warning message
         */
        REMOVE
    }

    /**
     *  The type of the warning message component
     */
    public enum WarningType {
        /**
         * Air 1860
         */
        AIR1860(0),

        /**
         * Battery
         */
        BATTERY(1),

        /**
         * Camera
         */
        CAMERA(2),

        /**
         * Center board
         */
        CENTER_BOARD(3),

        /**
         * OSD
         */
        OSD(4),

        /**
         * Flight Controller
         */
        FLIGHT_CONTROLLER(5),

        /**
         * Gimbal
         */
        GIMBAL(6),

        /**
         * Lightbridge
         */
        LIGHT_BRIDGE(7),

        /**
         * Remote controller
         */
        REMOTE_CONTROLLER(8),

        /**
         * Vision
         */
        VISION(9),

        /**
         * Flight record
         */
        FLIGHT_RECORD(10),

        /**
         * Fly safe
         */
        FLY_SAFE(11),

        /**
         * RTK
         */
        RTK(12),

        /**
         * LTE
         */
        LTE(13),

        /**
         * Other
         */
        OTHER(100);

        private int value;

        WarningType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private static WarningType[] values;

        public static WarningType[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static WarningType find(int value) {
            for (WarningType item : getValues()) {
                if (item.getValue() == value) {
                    return item;
                }
            }
            return OTHER;
        }
    }

    private WarningType warningType;
    private int code;
    private int subCode;
    private int componentIndex;
    private String reason;
    private String solution;
    private Level level = Level.WARNING;
    private Type type = Type.AUTO_DISAPPEAR;
    private Action action = Action.INSERT;
    private int showDuration = DEFAULT_DISPLAY_DURATION;
    private int iconRes;

    private WarningMessage(@Nullable WarningType type, @Nullable String reason, @Nullable String solution) {
        this.reason = reason;
        this.solution = solution;
        this.code = WarningType.OTHER.getValue();
        this.subCode = WarningMessageError.CUSTOMER_USE_ERROR.value();
        this.warningType = type;
        if (this.reason != null) {
            this.code = this.reason.hashCode();
        }
        if (this.solution != null) {
            this.code = this.code + this.solution.hashCode();
        }
    }

    public WarningMessage(@Nullable WarningType warningType, int code, int subCode, @Nullable String reason, @Nullable String solution) {
        this(warningType, code, subCode, 0, reason, solution);
    }

    public WarningMessage(@Nullable WarningType warningType, int code, int subCode, int componentIndex, @Nullable String reason, @Nullable String solution) {
        this.warningType = warningType;
        this.code = code;
        this.subCode = subCode;
        this.componentIndex = componentIndex;
        this.reason = reason;
        this.solution = solution;
    }

    @Nullable
    public WarningType getWarningType() {
        return warningType;
    }

    public void setWarningType(@Nullable WarningType type) {
        this.warningType = type;
    }

    public int getCode() {
        return code;
    }

    public void setSubCode(int subCode) {
        this.subCode = subCode;
    }

    public int getSubCode() {
        return subCode;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getComponentIndex() {
        return componentIndex;
    }

    public void setComponentIndex(int componentIndex) {
        this.componentIndex = componentIndex;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    public void setReason(@Nullable String reason) {
        this.reason = reason;
    }

    @Nullable
    public String getSolution() {
        return solution;
    }

    public void setSolution(@Nullable String solution) {
        this.solution = solution;
    }

    @Nullable
    public Level getLevel() {
        return level;
    }

    public void setLevel(@Nullable Level level) {
        this.level = level;
    }

    @Nullable
    public Type getType() {
        return type;
    }

    public void setType(@Nullable Type type) {
        this.type = type;
    }

    public int getShowDuration() {
        return showDuration;
    }

    public void setShowDuration(int showDuration) {
        this.showDuration = showDuration;
    }

    @Nullable
    public Action getAction() {
        return action;
    }

    public void setAction(@Nullable Action action) {
        this.action = action;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    @Override
    @NonNull
    public String toString() {
        return "WarningMessage{"
                + "WarningType="
                + (warningType == null ? "is null" : warningType.name())
                + ", code="
                + code
                + ", subCode="
                + subCode
                + ", componentIndex="
                + componentIndex
                + ", reason='"
                + reason
                + '\''
                + ", solution='"
                + solution
                + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarningMessage that = (WarningMessage) o;

        if (warningType != that.warningType) return false;
        if (code != that.code) return false;
        if (subCode != that.subCode) return false;
        if (componentIndex != that.componentIndex) return false;
        if (!Objects.equals(reason, that.reason)) return false;
        return Objects.equals(solution, that.solution);
    }

    @Override
    public int hashCode() {
        int result = code;
        result = 31 * result + subCode;
        result = 31 * result + (warningType == null ? 0 : warningType.getValue());
        result = 31 * result + componentIndex;
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (solution != null ? solution.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private WarningType warningType;
        private int code;
        private int subCode;
        private int componentIndex;
        private String reason;
        private String solution;
        private Level level = Level.WARNING;
        private Type type = Type.AUTO_DISAPPEAR;
        private Action action = Action.INSERT;
        private int showDuration = DEFAULT_DISPLAY_DURATION;
        private int iconRes;

        public Builder(@Nullable WarningType warningType) {
            this.warningType = warningType;
        }

        @NonNull
        public Builder code(int code) {
            this.code = code;
            return this;
        }

        @NonNull
        public Builder warningType(@Nullable WarningType warningType) {
            this.warningType = warningType;
            return this;
        }

        @NonNull
        public Builder subCode(int subCode) {
            this.subCode = subCode;
            return this;
        }

        @NonNull
        public Builder componentIndex(int componentIndex) {
            this.componentIndex = componentIndex;
            return this;
        }

        @NonNull
        public Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        @NonNull
        public Builder solution(@Nullable String solution) {
            this.solution = solution;
            return this;
        }

        @NonNull
        public Builder level(@Nullable Level level) {
            this.level = level;
            return this;
        }

        @NonNull
        public Builder type(@Nullable Type type) {
            this.type = type;
            return this;
        }

        @NonNull
        public Builder showDuration(int showDuration) {
            this.showDuration = showDuration;
            return this;
        }

        @NonNull
        public Builder action(@Nullable Action action) {
            this.action = action;
            return this;
        }

        @NonNull
        public Builder iconRes(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        @NonNull
        public WarningMessage build() {
            WarningMessage warningMessage = new WarningMessage(warningType, reason, solution);
            if (this.code != -1) {
                warningMessage.setCode(this.code);
            }
            warningMessage.setSubCode(this.subCode);
            warningMessage.setLevel(this.level);
            warningMessage.setType(this.type);
            warningMessage.setComponentIndex(this.componentIndex);
            warningMessage.setShowDuration(this.showDuration);
            warningMessage.setAction(this.action);
            warningMessage.setIconRes(iconRes);

            return warningMessage;
        }

    }
}
