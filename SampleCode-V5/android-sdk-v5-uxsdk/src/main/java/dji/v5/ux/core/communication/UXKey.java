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

package dji.v5.ux.core.communication;

import androidx.annotation.NonNull;

/**
 * This is a class to define a UXKey.
 * <p>
 * String key: String value of UXParamKey as defined in its parent class
 * Class valueType: Type of value this key will take for setting or return
 * to getters and observers.
 * String keyString: The full path of this key used for storage which includes
 * the index of the component.
 * UpdateType updateType: The update type of this key.
 */
public class UXKey {
    private final String key;
    private final Class<?> valueType;
    private final String keyPath;
    private final UXKeys.UpdateType updateType;

    public UXKey(@NonNull String key, @NonNull Class<?> valueType, @NonNull String keyPath, @NonNull UXKeys.UpdateType updateType) {
        this.key = key;
        this.valueType = valueType;
        this.keyPath = keyPath;
        this.updateType = updateType;
    }

    public String getKey() {
        return key;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public UXKeys.UpdateType getUpdateType() {
        return updateType;
    }
}
