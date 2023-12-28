package dji.sampleV5.aircraft.keyvalue;

/**
 * @author feel.feng
 * @time 2022/03/11 9:55 上午
 * @description:
 */

import org.jetbrains.annotations.Nullable;

/**
 * @author feel.feng
 */
public interface KeyItemActionListener<T> {

    void actionChange(@Nullable T t);
}

