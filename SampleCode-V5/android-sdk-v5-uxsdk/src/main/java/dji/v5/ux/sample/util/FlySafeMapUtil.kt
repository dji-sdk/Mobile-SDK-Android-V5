package dji.v5.ux.sample.util

import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.common.utils.GpsUtils
import dji.v5.manager.aircraft.flysafe.info.FlyZoneInformation

object FlySafeMapUtil {

    fun sortFlyZonesByDistanceFromAircraft(aircraftLocation: LocationCoordinate3D, flyZones: MutableList<FlyZoneInformation>) {
        val c = java.util.Comparator<FlyZoneInformation> { o1, o2 ->
            val distanceO1: Double = GpsUtils.distanceBetween(
                aircraftLocation.latitude, aircraftLocation.longitude,
                o1.circleCenter.latitude, o1.circleCenter.longitude
            ) - o1.circleRadius
            val distanceO2: Double = GpsUtils.distanceBetween(
                aircraftLocation.latitude, aircraftLocation.longitude,
                o2.circleCenter.latitude, o2.circleCenter.longitude
            ) - o2.circleRadius
            distanceO1.compareTo(distanceO2)
        }
        flyZones.sortWith(c)
    }
}