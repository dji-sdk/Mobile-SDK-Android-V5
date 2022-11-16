package dji.v5.ux.mapkit.core.maps

/**
 *
 *  @author joe.yang@dji.com
 *  @date 2019-10-17 15:36
 */
interface DJIInfoWindow {
    fun setOnViewChangedListener(listener: () -> Unit)

    fun onCreate()

    fun onDestroy()
}