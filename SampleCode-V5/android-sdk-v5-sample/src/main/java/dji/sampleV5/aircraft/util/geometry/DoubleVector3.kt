package dji.sampleV5.aircraft.util.geometry

import kotlin.math.sqrt

/**
 * 三维双精度向量
 * 完全参照 Pilot dji.lidar_map.utils.geometry.DoubleVector3
 *
 * 坐标系约定（与 LocationUtil 配合时）：
 *   LLA 语境：x = latitude, y = longitude, z = altitude
 *   NED 语境：x = North, y = East, z = Down
 */
class DoubleVector3(
    var x: Double,
    var y: Double,
    var z: Double
) {

    fun zero(): DoubleVector3 = DoubleVector3(0.0, 0.0, 0.0)

    fun add(other: DoubleVector3): DoubleVector3 =
        DoubleVector3(x + other.x, y + other.y, z + other.z)

    fun subtract(other: DoubleVector3): DoubleVector3 =
        DoubleVector3(x - other.x, y - other.y, z - other.z)

    fun invert(): DoubleVector3 = DoubleVector3(-x, -y, -z)

    fun dotProduct(other: DoubleVector3): Double =
        x * other.x + y * other.y + z * other.z

    fun normalize(): DoubleVector3 {
        val mag = getMagnitude()
        return DoubleVector3(x / mag, y / mag, z / mag)
    }

    fun scale(factor: Double): DoubleVector3 =
        DoubleVector3(x * factor, y * factor, z * factor)

    fun getMagnitudeSquared(): Double = x * x + y * y + z * z

    fun getMagnitude(): Double = sqrt(getMagnitudeSquared())
}
