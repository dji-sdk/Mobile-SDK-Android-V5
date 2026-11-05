package com.dji.open.pilot.plugin.sampleitem.mapmanager

import dji.open.pilot.base.manager.OpenLocation

/**
 * 地图 Demo 几何与配色工具。
 *
 * 所有图元以「飞机当前位置」为中心展开，因此在任何起飞地点都能在飞机周围看到完整演示。
 * 方位角约定：弧度制，0 = 正北，顺时针增大（与 AR 模块 offsetLocation 一致）。
 */
object MapGeo {

    // ── 配色（WIDGET / BATCH 用；MapElement 只能用枚举色，不走这里） ──────────

    /** 默认填充：半透明蓝 */
    const val FILL_BLUE = 0x662D8CF0
    /** 默认描边：深蓝 */
    const val STROKE_BLUE = 0xFF0050B3.toInt()
    /** 更新后填充：半透明绿 */
    const val FILL_GREEN = 0x6619BE6B
    /** 更新后描边：深绿 */
    const val STROKE_GREEN = 0xFF0A7A3D.toInt()

    // ── 半径 / 尺寸常量（米） ────────────────────────────────────────────────

    /** WIDGET/BATCH：以飞机为中心的图元外接半径 */
    const val WIDGET_RADIUS_M = 40.0
    /** WIDGET：圆半径 */
    const val CIRCLE_RADIUS_M = 40.0
    /** LAYER：四方位组距飞机的距离 */
    const val LAYER_DIST_M = 60.0
    /** LAYER：每个方位三角形的外接半径 */
    const val LAYER_TRI_RADIUS_M = 20.0
    /** ELEMENT：抬高高度（便于 AR 视图观察悬空元素） */
    const val ELEMENT_ALT_OFFSET_M = 50.0

    /** 四方位角（北/东/南/西），弧度 */
    val FOUR_DIRECTIONS = doubleArrayOf(
        0.0,                 // 北
        Math.PI / 2,         // 东
        Math.PI,             // 南
        Math.PI * 3 / 2      // 西
    )

    /**
     * 以 (baseLat, baseLng) 为原点，向 angleRad 方向偏移 distanceM 米。
     * @param altitude 目标点绝对高度（米）
     */
    fun offset(
        baseLat: Double, baseLng: Double, altitude: Double,
        distanceM: Double, angleRad: Double
    ): OpenLocation {
        val latOffset = distanceM * Math.cos(angleRad) / 111320.0
        val lngOffset = distanceM * Math.sin(angleRad) / (111320.0 * Math.cos(Math.toRadians(baseLat)))
        return OpenLocation(baseLat + latOffset, baseLng + lngOffset, altitude)
    }

    /**
     * 生成以 (centerLat, centerLng) 为中心、外接半径 radiusM 的等边三角形三个顶点。
     * 顶点方位 = startAngleRad, +120°, +240°。
     */
    fun equilateralTriangle(
        centerLat: Double, centerLng: Double, altitude: Double,
        radiusM: Double, startAngleRad: Double = 0.0
    ): List<OpenLocation> {
        val step = Math.toRadians(120.0)
        return listOf(0, 1, 2).map { i ->
            offset(centerLat, centerLng, altitude, radiusM, startAngleRad + step * i)
        }
    }

    /**
     * “底朝飞机”的等边三角形：三角形位于飞机某方位 bearingRad 处、距飞机 distM，
     * 且底边中点朝向飞机（即顶点背离飞机、指向外侧）。
     *
     * 实现：三角形中心在飞机方位 bearingRad、距 distM 处；令其中一个顶点朝外
     * （= bearingRad 方向），另两个顶点构成的底边自然朝向飞机。
     *
     * @return Pair(三角形三顶点, 三角形中心点)
     */
    fun triangleFacingAircraft(
        aircraftLat: Double, aircraftLng: Double, altitude: Double,
        bearingRad: Double, distM: Double, triRadiusM: Double
    ): Pair<List<OpenLocation>, OpenLocation> {
        val center = offset(aircraftLat, aircraftLng, altitude, distM, bearingRad)
        // 顶点朝外（bearingRad 方向），底边两顶点在 +120° / +240°，底边中点朝向飞机
        val verts = equilateralTriangle(
            center.latitude, center.longitude, altitude, triRadiusM, startAngleRad = bearingRad
        )
        return verts to center
    }

    /**
     * 等边三角形（外接半径 R）的内切圆半径 = R / 2。
     */
    fun incircleRadius(triRadiusM: Double): Double = triRadiusM / 2.0

    /** 闭合折线：首点追加到末尾，形成闭环 */
    fun closed(points: List<OpenLocation>): List<OpenLocation> =
        if (points.isEmpty()) points else points + points.first()
}
