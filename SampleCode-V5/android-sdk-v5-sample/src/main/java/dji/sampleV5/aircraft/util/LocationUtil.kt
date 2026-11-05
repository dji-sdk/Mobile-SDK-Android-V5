package dji.sampleV5.aircraft.util

import dji.sampleV5.aircraft.util.geometry.DoubleMatrix4
import dji.sampleV5.aircraft.util.geometry.DoubleVector3
import kotlin.math.*

/**
 * 坐标系转换工具
 * LLA: [latitude, longitude, altitude](degree)
 *
 * 完全参照 Pilot dji.lidar_map.utils.LocationUtil
 * 保留 lla2ned 完整调用链所需方法：
 *   lla2ned → ecef2ned → lla2ecef / getE2NRotationMatrix / getE2NTranslateMatrix
 */
object LocationUtil {

    /** WGS84 地球几何参数 */
    private const val F = 1.0 / 298.257223563  // 地球扁率
    private const val R = 6378137              // 地球赤道半径(m)
    private const val E = 8.1819190842622e-2   // 地球偏心率

    /**
     * LLA 坐标转 NED 坐标
     * @param locationLLA 待转换的 LLA 坐标 (x=lat, y=lon, z=alt in degrees/meters)
     * @param originLLA   NED 坐标系原点的 LLA 坐标（以飞机位置为原点时传入飞机 LLA）
     * @return 转换后的 NED 坐标 (x=North, y=East, z=Down)，单位：米
     */
    fun lla2ned(locationLLA: DoubleVector3, originLLA: DoubleVector3): DoubleVector3 {
        return ecef2ned(lla2ecef(locationLLA), originLLA)
    }

    /**
     * LLA 坐标转 ECEF 坐标（DoubleVector3 重载）
     * 算法参考：https://ww2.mathworks.cn/help/aeroblks/llatoecefposition.html
     */
    fun lla2ecef(locationLLA: DoubleVector3): DoubleVector3 {
        val ecef = lla2ecef(locationLLA.x, locationLLA.y, locationLLA.z)
        return DoubleVector3(ecef[0], ecef[1], ecef[2])
    }

    /**
     * LLA 坐标转 ECEF 坐标（分量重载）
     * @param latitude  纬度，单位：度
     * @param longitude 经度，单位：度
     * @param altitude  海拔，单位：米
     * @return ECEF 坐标 [x, y, z]，单位：米
     */
    fun lla2ecef(latitude: Double, longitude: Double, altitude: Double): DoubleArray {
        val iota = Math.toRadians(longitude)
        val mu   = Math.toRadians(latitude)
        val h    = altitude

        val lambdaS = atan((1 - F).pow(2) * tan(mu))
        val rS      = sqrt(R.toDouble().pow(2) / (1 + (1 / (1 - F).pow(2) - 1) * sin(lambdaS).pow(2)))

        return doubleArrayOf(
            rS * cos(lambdaS) * cos(iota) + h * cos(mu) * cos(iota),
            rS * cos(lambdaS) * sin(iota) + h * cos(mu) * sin(iota),
            rS * sin(lambdaS) + h * sin(mu)
        )
    }

    /**
     * ECEF 坐标转 NED 坐标
     * p_NED = R * T * P_ECEF
     * @param locationECEF 待转换的 ECEF 坐标
     * @param originLLA    NED 原点的 LLA 坐标
     * @return 转换后的 NED 坐标
     */
    fun ecef2ned(locationECEF: DoubleVector3, originLLA: DoubleVector3): DoubleVector3 {
        val rotation  = getE2NRotationMatrix(originLLA)
        val translate = getE2NTranslateMatrix(originLLA)
        return rotation.multiply(translate).multiply(locationECEF)
    }

    /**
     * 获取 ECEF → NED 旋转矩阵（DCM）
     * 参考：https://ww2.mathworks.cn/help/aeroblks/directioncosinematrixeceftoned.html
     *
     * iota = longitude (radians)
     * mu   = latitude  (radians)
     *
     *       [-sin(mu)cos(iota),  -sin(mu)sin(iota),  cos(mu) ]
     * DCM = [-sin(iota),         cos(iota),          0       ]
     *       [-cos(mu)cos(iota),  -cos(mu)sin(iota),  -sin(mu)]
     *
     * @param originLLA NED 原点的 LLA 坐标 (x=lat, y=lon)
     * @return 4×4 旋转矩阵（列优先）
     */
    fun getE2NRotationMatrix(originLLA: DoubleVector3): DoubleMatrix4 {
        val iota = Math.toRadians(originLLA.y)  // longitude
        val mu   = Math.toRadians(originLLA.x)  // latitude
        val sinU = sin(mu)
        val sinI = sin(iota)
        val cosU = cos(mu)
        val cosI = cos(iota)
        // 行优先传入，内部转为列优先
        val dcm = DoubleMatrix4(
            -sinU * cosI, -sinU * sinI,  cosU, 0.0,
            -sinI,         cosI,         0.0,  0.0,
            -cosU * cosI, -cosU * sinI, -sinU, 0.0,
             0.0,          0.0,          0.0,  1.0
        )
        return dcm.transpose()
    }

    /**
     * 获取 ECEF → NED 平移矩阵
     * @param originLLA NED 原点的 LLA 坐标
     * @return 4×4 平移矩阵（列优先）
     */
    fun getE2NTranslateMatrix(originLLA: DoubleVector3): DoubleMatrix4 {
        val originECEF = lla2ecef(originLLA)
        val translate = DoubleMatrix4(
            1.0, 0.0, 0.0, -originECEF.x,
            0.0, 1.0, 0.0, -originECEF.y,
            0.0, 0.0, 1.0, -originECEF.z,
            0.0, 0.0, 0.0,  1.0
        )
        return translate.transpose()
    }
}
