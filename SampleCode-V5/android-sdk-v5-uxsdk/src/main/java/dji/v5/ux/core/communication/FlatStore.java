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

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is responsible for the underlying storage for UXKeys
 */
public class FlatStore {
    private static final int INITIAL_CAPACITY = 100;
    private final ConcurrentHashMap<String, ModelValue> store;

    private FlatStore() {
        store = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    }

    public static FlatStore getInstance() {
        return FlatStore.SingletonHolder.instance;
    }

    /**
     * Update and store the given value for the given key
     *
     * @param value   ModelValue to be stored for key
     * @param keyPath UXKey path to be used for storing this value
     */
    public void setModelValue(@NonNull ModelValue value, @NonNull String keyPath) {
        store.put(keyPath, value);
    }

    /**
     * Get the current value for the given key
     *
     * @param keyPath UXKey path to be used to retrieve value
     * @return ModelValue store for the given key
     */
    public ModelValue getModelValue(@NonNull String keyPath) {
        return store.get(keyPath);
    }

    private static class SingletonHolder {
        private static FlatStore instance = new FlatStore();
    }
}
