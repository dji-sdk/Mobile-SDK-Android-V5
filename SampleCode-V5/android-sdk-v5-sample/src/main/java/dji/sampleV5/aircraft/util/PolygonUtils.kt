package dji.sampleV5.aircraft.util

import dji.sampleV5.aircraft.util.geometry.DoubleVector3

/**
 * 几何计算工具类
 * 完全参照 Pilot com.dji.industry.pilot.missionflight.utils.PolygonUtils
 */
object PolygonUtils {

    /**
     * 计算点到线段的最短距离
     *
     * 算法（参照 Pilot）：
     *   1. 计算线段方向向量 lineVec 和起点到目标点的向量 pointVec
     *   2. 若线段退化为点，直接返回点间距离
     *   3. 将 pointVec 向 lineVec 投影，得到标量 projection
     *   4. 根据投影确定线段上最近点：
     *      - projection <= 0    → 最近点为起点 start
     *      - projection >= len  → 最近点为终点 end
     *      - 否则               → 最近点为投影点（线段内部）
     *   5. 返回目标点到最近点的欧几里得距离
     *
     * @param start  线段起点（NED坐标，单位：米）
     * @param end    线段终点（NED坐标，单位：米）
     * @param point  目标点，默认为原点 (0,0,0)，即 NED 原点（飞机位置）
     * @return 目标点到线段的最短距离，单位：米
     */
    fun point2SegmentDist(
        start: DoubleVector3,
        end: DoubleVector3,
        point: DoubleVector3 = DoubleVector3(0.0, 0.0, 0.0)
    ): Double {
        val lineVec    = end.subtract(start)
        val pointVec   = point.subtract(start)
        val lineVecMag = lineVec.getMagnitude()

        if (lineVecMag == 0.0) {
            // 线段退化为点
            return pointVec.getMagnitude()
        }

        val projection = pointVec.dotProduct(lineVec) / lineVecMag
        val closestPoint = when {
            projection <= 0.0      -> start
            projection >= lineVecMag -> end
            else -> start.add(lineVec.scale(projection / lineVecMag))
        }

        return point.subtract(closestPoint).getMagnitude()
    }
}
