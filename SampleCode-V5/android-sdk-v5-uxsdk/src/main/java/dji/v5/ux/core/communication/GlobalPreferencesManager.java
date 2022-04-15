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
 * Class used for initializing and getting an instance of the implementation
 * of {@link GlobalPreferencesInterface} being used.
 */
public final class GlobalPreferencesManager {
    //region Fields
    private static GlobalPreferencesInterface instance = null;
    //endregion

    private GlobalPreferencesManager() {
        //Empty
    }

    /**
     * This function is used to get an instance of the implementation of the
     * {@link GlobalPreferencesManager} using the {@link GlobalPreferencesInterface}.
     *
     * @return instance An instance of the class implementing the {@link GlobalPreferencesInterface}.
     */
    public static GlobalPreferencesInterface getInstance() {
        synchronized (GlobalPreferencesManager.class) {
            return instance;
        }
    }

    /**
     * This function is used to initialize the {@link GlobalPreferencesManager} with an
     * implementation of the {@link GlobalPreferencesInterface}. This can be the default built in
     * {@link DefaultGlobalPreferences} or a custom class that implements the
     * {@link GlobalPreferencesInterface}. This must be called before initializing any widgets that
     * use this.
     *
     * @param preferencesManager An instance of the class implementing the
     *                           {@link GlobalPreferencesInterface}.
     */
    public static void initialize(@NonNull GlobalPreferencesInterface preferencesManager) {
        instance = preferencesManager;
    }
}
