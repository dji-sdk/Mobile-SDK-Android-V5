package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.ar.ARCircleInfo
import dji.open.pilot.base.manager.ar.ARDistAlpha
import dji.open.pilot.base.manager.ar.ARDistLineWidth
import dji.open.pilot.base.manager.ar.ARFaceStroke
import dji.open.pilot.base.manager.ar.ARFontGravity
import dji.open.pilot.base.manager.ar.ARIconOrthographicConfig
import dji.open.pilot.base.manager.ar.ARIconPerspectiveConfig
import dji.open.pilot.base.manager.ar.ARIconProjectionType
import dji.open.pilot.base.manager.ar.ARLineInfo
import dji.open.pilot.base.manager.ar.ARPointConfig
import dji.open.pilot.base.manager.ar.ARPointInfo
import dji.open.pilot.base.manager.ar.ARPolygonFace
import dji.open.pilot.base.manager.ar.ARPolygonInfo
import dji.open.pilot.base.manager.ar.ARRadarConfig
import dji.open.pilot.base.manager.ar.ARStrokeInfo
import dji.open.pilot.base.manager.ar.ARTextConfig
import dji.open.pilot.client.manager.ui.ARManager
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get

class ARProjectionVM : DJIViewModel() {
    val TAG = "ARProjectionVM"
    companion object {
        val colorBlue = 0xFF2D8CF0.toInt()
        val colorRed = 0xFFE23C39.toInt()
        val colorGreen = 0xFF19BE6B.toInt()
        val colorYellow = 0xFFFFBB00.toInt()
        val colorMagenta = 0xFFB620E0.toInt()
        val colorBackground = 0x55141414
        val colorOrange = 0xFFFFA500.toInt()

        val colorLightRed    = 0xFFFF6B6B.toInt()
        val colorLightOrange = 0xFFFFA94D.toInt()
        val colorLightYellow = 0xFFFFD43B.toInt()
        val colorLightGreen  = 0x3369DB7C.toInt()
        val colorLightCyan   = 0xFF4DD0E1.toInt()
        val colorLightBlue   = 0x3374C0FC.toInt()
        val colorLightPurple = 0x33DA77F2.toInt()
        val colorLightWhite       = 0x33FFFFFF.toInt()
        val textColorBlack       = 0x88000000.toInt()
        val colorBlack       = 0x33000000.toInt()

        /**SVG内容限制：
         * 单个需要由<g></g>包围。
         * 必须要有id，且包含“normal”。
         * 如果要外部设置颜色，id则包含“_normal_color”。
         */
        const val AR_ORTHOGRAPHIC_ICON_SVG = "ar_point_select.svg"
        const val AR_ORTHOGRAPHIC_NEXT_ICON_SVG = "ar_point_next.svg"
        const val AR_PERSPECTIVE_MAX_ICON_SVG = "ar_point_max_waypoint.svg"
        const val AR_PERSPECTIVE_MIN_ICON_SVG = "ar_point_min_waypoint.svg"

        const val AR_POINT_BUS_ICON_SVG = "ar_point_bus_icon.svg"
        const val AR_POINT_CAR_ICON_SVG = "ar_point_car_icon.svg"
        const val AR_POINT_PERSON_ICON_SVG = "ar_point_person_icon.svg"
        const val AR_POINT_STAR_MAX_ICON_SVG = "ar_point_star_max_icon.svg"
        const val AR_POINT_STAR_MIN_ICON_SVG = "ar_point_star_min_icon.svg"

        //近大远小，瞄准图标
        private val arIconPerspectiveConfig01 = ARIconPerspectiveConfig.Builder.builder()
            .withRealSize(1.6f)                 // 真实世界大小（米）
            .withMinSize(60f)                    // 最小像素
            .withMaxSize(180f)                   // 最大像素
            .withStrokeWidthRatio(0.8f)          // 线宽比例
            .withMaxIconFileName(AR_PERSPECTIVE_MAX_ICON_SVG)   // 大图标
            .withMinIconFileName(AR_PERSPECTIVE_MIN_ICON_SVG)   // 小图标
            .build()

        //近大远小，星星图标
        private val arIconPerspectiveConfig02 = ARIconPerspectiveConfig.Builder.builder()
            .withRealSize(3f)
            .withMinSize(60f)
            .withMaxSize(180f)
            .withStrokeWidthRatio(1f)
            .withMaxIconFileName(AR_POINT_STAR_MAX_ICON_SVG)
            .withMinIconFileName(AR_POINT_STAR_MIN_ICON_SVG)
            .build()

        //近大远小，人头图标
        private val arIconPersonConfig02 = ARIconPerspectiveConfig.Builder.builder()
            .withRealSize(3f)
            .withMinSize(60f)
            .withMaxSize(180f)
            .withStrokeWidthRatio(1f)
            .withMaxIconFileName(AR_POINT_PERSON_ICON_SVG)
            .withMinIconFileName(AR_POINT_PERSON_ICON_SVG)
            .build()

        //固定大小，货车🚚图标
        private val arIconOrthographicConfig01: ARIconOrthographicConfig = ARIconOrthographicConfig.Builder.builder()
            .withSize(80f)
            .withIconFileName(AR_POINT_CAR_ICON_SVG)
            .build()

        //固定大小，巴士🚌图标
        private val arIconOrthographicConfig02: ARIconOrthographicConfig = ARIconOrthographicConfig.Builder.builder()
            .withSize(80f)
            .withIconFileName(AR_POINT_BUS_ICON_SVG)
            .build()

        //固定大小，人像图标
        private val arIconOrthographicConfig04: ARIconOrthographicConfig = ARIconOrthographicConfig.Builder.builder()
            .withSize(80f)
            .withIconFileName(AR_POINT_PERSON_ICON_SVG)
            .build()

        //文本在图标顶部，无阴影
        private val arTextConfig01: ARTextConfig = ARTextConfig.Builder.builder()
            .withTextSize(40f)
            .withFontGravity(ARFontGravity.TOP)
            .withShadow(false)
            .withBackgroundColor(colorBackground)
            .build()

        //文本在图标底部，无阴影
        private val arTextConfig02: ARTextConfig = ARTextConfig.Builder.builder()
            .withTextSize(40f)
            .withFontGravity(ARFontGravity.BOTTOM)
            .withShadow(false)
            .withBackgroundColor(colorBackground)
            .build()

        //文本在图标顶部，有阴影，背景颜色填充
        private val arTextConfig03: ARTextConfig = ARTextConfig.Builder.builder()
            .withTextSize(40f)
            .withFontGravity(ARFontGravity.TOP)
            .withShadow(true)
            .withBackgroundColor(colorBackground)
            .build()

        //近大远小，星星图标
        val arPointConfigImp01: ARPointConfig = ARPointConfig.Builder.builder()
            .withName("AR Point Config 01")
            .withUseText(true)
            .withTextConfig(arTextConfig01)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(arIconPerspectiveConfig02)
            .withIconOrthographicConfig(arIconOrthographicConfig01)
            .build()

        val arPointConfigImp02: ARPointConfig = ARPointConfig.Builder.builder()
            .withName("AR Point Config 02")
            .withUseText(true)
            .withTextConfig(arTextConfig02)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconPerspectiveConfig(arIconPerspectiveConfig02)
            .withIconOrthographicConfig(arIconOrthographicConfig01)
            .build()

        val arPointConfigImp03: ARPointConfig = ARPointConfig.Builder.builder()
            .withName("AR Point Config 03")
            .withUseText(true)
            .withTextConfig(arTextConfig03)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconPerspectiveConfig(arIconPerspectiveConfig02)
            .withIconOrthographicConfig(arIconOrthographicConfig02)
            .build()

        val arPointConfigImp04: ARPointConfig = ARPointConfig.Builder.builder()
            .withName("AR Point Config 04")
            .withUseText(true)
            .withTextConfig(arTextConfig02)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconPerspectiveConfig(arIconPerspectiveConfig02)
            .withIconOrthographicConfig(arIconOrthographicConfig04)
            .build()

        val arPointConfigImp05: ARPointConfig = ARPointConfig.Builder.builder()
            .withName("AR Point Config 05")
            .withUseText(true)
            .withTextConfig(arTextConfig02)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(arIconPersonConfig02)
            .withIconOrthographicConfig(arIconOrthographicConfig04)
            .build()

    }

    val markerIconList = listOf(AR_ORTHOGRAPHIC_ICON_SVG, AR_ORTHOGRAPHIC_NEXT_ICON_SVG,
        AR_PERSPECTIVE_MAX_ICON_SVG, AR_PERSPECTIVE_MIN_ICON_SVG,
        AR_POINT_BUS_ICON_SVG, AR_POINT_CAR_ICON_SVG,
        AR_POINT_PERSON_ICON_SVG, AR_POINT_STAR_MAX_ICON_SVG,
        AR_POINT_STAR_MIN_ICON_SVG)

    val arPointConfigs = MutableLiveData<MutableSet<ARPointConfig>>()
    val arRemotePointInfos = MutableLiveData<List<ARPointInfo>>()
    val arRemoteLineInfos = MutableLiveData<List<ARLineInfo>>()
    val arRemotePolygonInfos = MutableLiveData<List<ARPolygonInfo>>()
    val arRemoteCircleInfos = MutableLiveData<List<ARCircleInfo>>()

    //AR Point 查询配置 + 将服务器中的配置表拉到本地
    fun queryARPointConfig() {
        ARManager.getInstance().queryARPointConfig(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPointConfig>> {
            override fun onSuccess(result: List<ARPointConfig>) {
                arPointConfigs.postValue(result.toMutableSet())
                notifyUser("queryARPointConfig onSuccess, config size: ${result.size}")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryARPointConfig onFailure ${error.errorCode()}")
            }
        })
    }

    fun uploadARPointConfigs() {
        uploadARPointConfig(arPointConfigImp01)
        uploadARPointConfig(arPointConfigImp02)
        uploadARPointConfig(arPointConfigImp03)
        uploadARPointConfig(arPointConfigImp04)
        uploadARPointConfig(arPointConfigImp05)
    }

    //上传配置
    private fun uploadARPointConfig(config: ARPointConfig) {
        ARManager.getInstance().uploadARPointConfig(config,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String?) {
                    notifyUser("uploadARPointConfig onSuccess, return id is: ${result.toString()}")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARPointConfig failed ${error.errorCode()}")
                }
            })
    }

    fun uploadARPointInfoList() {
        val location_test = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location_test == null) {
            notifyUser("location is null!")
            return
        }

        val arPointInfo01 = ARPointInfo.Builder.builder()
            .withName("First")
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("DJI--Pilot--Star")
            .withTextColor(colorBlue)
            .withIconColor(colorOrange)
            .withLocation(
                OpenLocation(
                    location_test.latitude,
                    location_test.longitude - 0.0001,
                    60.0
                )
            )
            .build()

        val arPointInfo02 = ARPointInfo.Builder.builder()
            .withName("Second")
            .withConfigId(arPointConfigImp02.id)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("Car")
            .withTextColor(colorBlue)
            .withIconColor(colorGreen)
            .withLocation(
                OpenLocation(
                    location_test.latitude,
                    location_test.longitude,
                    60.0
                )
            )
            .build()

        val arPointInfo03 = ARPointInfo.Builder.builder()
            .withName("Third")
            .withConfigId(arPointConfigImp04.id)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("Person(固定大小)")
            .withTextColor(colorBlue)
            .withIconColor(colorGreen)
            .withLocation(
                OpenLocation(
                    location_test.latitude,
                    location_test.longitude + 0.0001,
                    60.0
                )
            )
            .build()

        uploadARPointInfo(arPointInfo01)
        uploadARPointInfo(arPointInfo02)
        uploadARPointInfo(arPointInfo03)
    }

    //查询投影属性
    fun queryARPointInfo(){
        ARManager.getInstance().queryARPointInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPointInfo>> {
            override fun onSuccess(result: List<ARPointInfo>) {
                arRemotePointInfos.postValue(result)
                notifyUser("queryARPointInfo onSuccess ${result.size}")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryARPointInfo onFailure ${error.errorCode()}")
            }
        })
    }

    private fun uploadARPointInfo(arPointInfo: ARPointInfo) {
        ARManager.getInstance().uploadARPointInfo(
            arPointInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARPointInfo onSuccess $result")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARPointInfo onFailure ${error.errorCode()}")
                }
            })
    }

    fun deleteARPointInfo(arPointInfo: ARPointInfo) {
        ARManager.getInstance()
            .deleteARPointInfo(arPointInfo, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    notifyUser("removeARPointInfo onSuccess")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("removeARPointInfo onFailure ${error.errorCode()}")
                }
            })
    }

    fun updateARPointInfo(arPointInfo: ARPointInfo) {
        ARManager.getInstance()
            .updateARPointInfo(arPointInfo, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    notifyUser("updateARPointInfo onSuccess")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("updateARPointInfo onFailure")
                }
            })
    }

    // ==================== AR Line ====================

    fun uploadARLineInfoList() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        val colorWhite       = 0xFFFFFFFF.toInt()
        val colorBlack       = 0xFF000000.toInt()

        // 北斗七星坐标布局（按真实赤道坐标比例还原，相对飞机位置偏移，高度 50m）
        // 连线顺序：1(天枢)→2(天璇)→3(天玑)→4(天权)→5(玉衡)→6(开阳)→7(摇光)
        //
        // 真实坐标参考（赤经/赤纬）：
        //   天枢 α: RA 11h03m  Dec +61°45'
        //   天璇 β: RA 11h02m  Dec +56°22'
        //   天玑 γ: RA 11h54m  Dec +53°41'
        //   天权 δ: RA 12h15m  Dec +57°01'
        //   玉衡 ε: RA 12h54m  Dec +55°57'
        //   开阳 ζ: RA 13h24m  Dec +54°55'
        //   摇光 η: RA 13h48m  Dec +49°18'
        //
        // 以天枢为锚点(0,0)，单位 0.0001°，X=经度(东)，Y=纬度(北)
        // 赤经差 × 0.004 → lon偏移；赤纬差 × 0.0002 → lat偏移（等比缩放）
        //
        // 俯视示意（↑北/纬度增大，→东/经度增大）：
        //
        //  1(天枢) ·
        //                                  · 4(天权)
        //       · 2(天璇)   · 3(天玑)             · 5(玉衡) · 6(开阳)
        //                                                            · 7(摇光)

        // 以飞机位置为锚点，把整体图案居中偏前方
        val baseLat = location.latitude
        val baseLon = location.longitude

        // 1 天枢 (0.0, 0.0) — 纬度最高，最北
        val p1 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("α").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat + 0.0000, baseLon + 0.0000, 50.0))
            .build()

        // 2 天璇 (RA≈+0.5min→lon+0.0000, Dec-5.4°→lat-0.0011)
        val p2 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("β").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00011, baseLon + 0.00002, 50.0))
            .build()

        // 3 天玑 (RA≈+51min→lon+0.0020, Dec-8.1°→lat-0.0016)
        val p3 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("γ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00016, baseLon + 0.00020, 50.0))
            .build()

        // 4 天权 (RA≈+72min→lon+0.0029, Dec-4.7°→lat-0.0009)
        val p4 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("δ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00009, baseLon + 0.00029, 50.0))
            .build()

        // 5 玉衡 (RA≈+111min→lon+0.0044, Dec-5.8°→lat-0.0012)
        val p5 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("ε").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00012, baseLon + 0.00044, 50.0))
            .build()

        // 6 开阳 (RA≈+141min→lon+0.0056, Dec-6.8°→lat-0.0014)
        val p6 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("ζ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00014, baseLon + 0.00056, 50.0))
            .build()

        // 7 摇光 (RA≈+165min→lon+0.0066, Dec-12.5°→lat-0.0025) — 纬度最低，最南
        val p7 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("η").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00025, baseLon + 0.00066, 50.0))
            .build()

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorBlack)
            .withDistLineWidth(distLineWidth)
            .withIsDash(false)
            .build()

        val arLineInfo = ARLineInfo.Builder.builder()
            .withName("AR Line 01")
            .withPoints(listOf(p1, p2, p3, p4, p5, p6, p7))
            .withStrokeInfo(strokeInfo)
            .withUseDepth(false)
            .build()

        uploadARLineInfo(arLineInfo)
    }

    private fun uploadARLineInfo(arLineInfo: ARLineInfo) {
        ARManager.getInstance().uploadARLineInfo(arLineInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARLineInfo onSuccess, id: $result")
                }
                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARLineInfo onFailure: ${error.errorCode()}")
                }
            })
    }

    fun queryARLineInfo() {
        ARManager.getInstance().queryARLineInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARLineInfo>> {
            override fun onSuccess(result: List<ARLineInfo>) {
                arRemoteLineInfos.postValue(result)
                notifyUser("queryARLineInfo onSuccess, size: ${result.size}")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("queryARLineInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun updateARLineInfo(arLineInfo: ARLineInfo) {
        // 北斗七星各点更新：图标颜色改为浅色系红橙黄绿青蓝紫，文案改为星名
        val colorLightRed    = 0xFFFF6B6B.toInt()
        val colorLightOrange = 0xFFFFA94D.toInt()
        val colorLightYellow = 0xFFFFD43B.toInt()
        val colorLightGreen  = 0xFF69DB7C.toInt()
        val colorLightCyan   = 0xFF4DD0E1.toInt()
        val colorLightBlue   = 0xFF74C0FC.toInt()
        val colorLightPurple = 0xFFDA77F2.toInt()
        val colorWhite       = 0xFFFFFFFF.toInt()


        val starNames  = listOf("天枢", "天璇", "天玑", "天权", "玉衡", "开阳", "摇光")
        val starColors = listOf(
            colorLightRed, colorLightOrange, colorLightYellow, colorLightGreen,
            colorLightCyan, colorLightBlue, colorLightPurple
        )

        arLineInfo.points?.forEachIndexed { index, point ->
            if (index < starNames.size) {
                point.text      = starNames[index]
                point.iconColor = starColors[index]
                point.textColor = starColors[index]
            }
        }

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(20f)
            .withMinLineWidth(5f).withMinWidthDist(40f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorWhite)
            .withDistLineWidth(distLineWidth)
            .withIsDash(true)
            .build()

        arLineInfo.isUseDepth = true
        arLineInfo.strokeInfo = strokeInfo

        ARManager.getInstance().updateARLineInfo(arLineInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("updateARLineInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("updateARLineInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun deleteARLineInfo(arLineInfo: ARLineInfo) {
        ARManager.getInstance().deleteARLineInfo(arLineInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("deleteARLineInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("deleteARLineInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    // ==================== AR Polygon ====================
    fun uploadARPolygonInfoList() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        // 多边形端点（支持嵌入 AR 点）
        val p1 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("A").withTextColor(colorBlue).withIconColor(colorBlue)
            .withLocation(OpenLocation(location.latitude - 0.0002, location.longitude - 0.0002, 50.0))
            .build()

        val p2 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("B").withTextColor(colorGreen).withIconColor(colorGreen)
            .withLocation(OpenLocation(location.latitude - 0.0002, location.longitude + 0.0002, 50.0))
            .build()

        val p3 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("C").withTextColor(colorRed).withIconColor(colorRed)
            .withLocation(OpenLocation(location.latitude + 0.0002, location.longitude, 50.0))
            .build()

        // 面颜色配置（半透明蓝色正面，半透明绿色背面）
        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightGreen)
            .withBackFaceColor(colorLightWhite)
            .build()

        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightBlue)
            .withBackFaceColor(colorLightGreen)
            .build()

        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightWhite)
            .withBackFaceColor(colorLightGreen)
            .build()

        // 边线配置
        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(15f).withMaxWidthDist(25f)
            .withMinLineWidth(3f).withMinWidthDist(50f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(textColorBlack).withDistLineWidth(distLineWidth).withIsDash(false).build()

        val sideStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        // 拉伸锚点（柱体底部位置，与 p1 相同高度做参考）
        val extrusionAnchor = OpenLocation(0.0, 0.0, 0.0)

        val arPolygonInfo = ARPolygonInfo.Builder.builder()
            .withName("AR Polygon 01")
            .withPoints(listOf(p1, p2, p3))
            .withExtrusionAnchor(extrusionAnchor)
            .withTopFace(topFace)
            .withSideFace(sideFace)
            .withBottomFace(bottomFace)
            .withTopFaceStroke(sideStroke)
            .withSideFaceStroke(sideStroke)
            .withBottomFaceStroke(sideStroke)
            .withFaceDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withFaceStrokeDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withIs3dMesh(false)
            .build()

        uploadARPolygonInfo(arPolygonInfo)
    }

    private fun uploadARPolygonInfo(arPolygonInfo: ARPolygonInfo) {
        ARManager.getInstance().uploadARPolygonInfo(arPolygonInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARPolygonInfo onSuccess, id: $result")
                }
                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARPolygonInfo onFailure: ${error.errorCode()}")
                }
            })
    }

    fun queryARPolygonInfo() {
        ARManager.getInstance().queryARPolygonInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPolygonInfo>> {
            override fun onSuccess(result: List<ARPolygonInfo>) {
                arRemotePolygonInfos.postValue(result)
                notifyUser("queryARPolygonInfo onSuccess, size: ${result.size}")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("queryARPolygonInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun updateARPolygonInfo(arPolygonInfo: ARPolygonInfo) {
        // 演示更新：将侧面颜色改为红色半透明
        val extrusionAnchor = OpenLocation(0.0, 0.0, 50.0)

        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightWhite)
            .withBackFaceColor(colorLightGreen)
            .build()

        arPolygonInfo.extrusionAnchor = extrusionAnchor
        arPolygonInfo.bottomFace = bottomFace

        ARManager.getInstance().updateARPolygonInfo(arPolygonInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("updateARPolygonInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("updateARPolygonInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun deleteARPolygonInfo(arPolygonInfo: ARPolygonInfo) {
        ARManager.getInstance().deleteARPolygonInfo(arPolygonInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("deleteARPolygonInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("deleteARPolygonInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    // ==================== AR Circle ====================

    fun uploadARCircleInfoList() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        // 圆心（飞机正前方）
        val center = OpenLocation(location.latitude, location.longitude, 30.0)
        // 拉伸锚点（底面位置）
        val extrusionAnchor = OpenLocation(0.0, 0.0, 0.0)

        // 顶面：半透明绿色
        val circleFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightBlue)
            .withBackFaceColor(colorLightWhite)
            .build()

        // 边线
        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(12f).withMaxWidthDist(50f)
            .withMinLineWidth(3f).withMinWidthDist(100f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorBlue).withDistLineWidth(distLineWidth).withIsDash(false).build()
        val edgeStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        val arDistAlpha = ARDistAlpha.Builder.builder().withEnable(true).withMinDist(30f).withMaxDist(60f).withMinAlpha(0.3f).withMaxAlpha(0.8f).build()

        val arCircleInfo = ARCircleInfo.Builder.builder()
            .withName("AR Circle 01")
            .withCenter(center)
            .withRadius(20f)
            .withExtrusionAnchor(extrusionAnchor)
            .withTopFace(circleFace)
            .withBottomFace(circleFace)
            .withSideFace(circleFace)
            .withTopFaceStroke(edgeStroke)
            .withSideFaceStroke(edgeStroke)
            .withBottomFaceStroke(edgeStroke)
            .withFaceDistAlpha(arDistAlpha)
            .withFaceStrokeDistAlpha(arDistAlpha)
            .build()

        uploadARCircleInfo(arCircleInfo)
    }

    private fun uploadARCircleInfo(arCircleInfo: ARCircleInfo) {
        ARManager.getInstance().uploadARCircleInfo(arCircleInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARCircleInfo onSuccess, id: $result")
                }
                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARCircleInfo onFailure: ${error.errorCode()}")
                }
            })
    }

    fun queryARCircleInfo() {
        ARManager.getInstance().queryARCircleInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARCircleInfo>> {
            override fun onSuccess(result: List<ARCircleInfo>) {
                arRemoteCircleInfos.postValue(result)
                notifyUser("queryARCircleInfo onSuccess, size: ${result.size}")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("queryARCircleInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun updateARCircleInfo(arCircleInfo: ARCircleInfo) {
        // 演示更新：将侧面颜色改为蓝色，半径扩大
        val circleFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightGreen)
            .withBackFaceColor(colorLightWhite)
            .build()

        val extrusionAnchor = OpenLocation(0.0, 0.0, 30.0)

        // 边线
        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(12f).withMaxWidthDist(50f)
            .withMinLineWidth(3f).withMinWidthDist(100f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorGreen).withDistLineWidth(distLineWidth).withIsDash(false).build()
        val edgeStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()


        arCircleInfo.topFace = circleFace
        arCircleInfo.bottomFace = circleFace
        arCircleInfo.sideFace = circleFace
        arCircleInfo.sideFaceStroke = edgeStroke
        arCircleInfo.topFaceStroke = edgeStroke
        arCircleInfo.bottomFaceStroke = edgeStroke
        arCircleInfo.radius = 20f
        arCircleInfo.tRadius = 20f
        arCircleInfo.extrusionAnchor = extrusionAnchor

        ARManager.getInstance().updateARCircleInfo(arCircleInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("updateARCircleInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("updateARCircleInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun deleteARCircleInfo(arCircleInfo: ARCircleInfo) {
        ARManager.getInstance().deleteARCircleInfo(arCircleInfo, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("deleteARCircleInfo onSuccess")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("deleteARCircleInfo onFailure: ${error.errorCode()}")
            }
        })
    }

    fun enableARRadar(label: String) {
        val config = ARRadarConfig.Builder.builder()
            .withEnable(true)
            .withLabel(label)
            .build()
        ARManager.getInstance().enableARRadar(config, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("enableARRadar onSuccess")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("enableARRadar onFailure ${error.errorCode()}")
            }
        })
    }

    fun disableARRadar() {
        val config = ARRadarConfig.Builder.builder()
            .withEnable(false)
            .build()
        ARManager.getInstance().enableARRadar(config, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("disableARRadar onSuccess")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("disableARRadar onFailure ${error.errorCode()}")
            }
        })
    }

    // ============================== AR Line ==============================
    val arLineInfos = MutableLiveData<List<ARLineInfo>>()
    val arPolygonInfos = MutableLiveData<List<ARPolygonInfo>>()
    val arCircleInfos = MutableLiveData<List<ARCircleInfo>>()

    private fun buildDistLineWidth(): ARDistLineWidth {
        return ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(10f)
            .withMaxWidthDist(10f)
            .withMinLineWidth(5f)
            .withMinWidthDist(100f)
            .build()
    }

    private fun buildStroke(color: Int): ARStrokeInfo {
        return ARStrokeInfo.Builder.builder()
            .withColor(color)
            .withDistLineWidth(buildDistLineWidth())
            .withIsDash(false)
            .build()
    }

    private fun buildFace(frontColor: Int, backColor: Int): ARPolygonFace {
        return ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(frontColor)
            .withBackFaceColor(backColor)
            .build()
    }

    private fun buildFaceStroke(color: Int): ARFaceStroke {
        return ARFaceStroke.Builder.builder()
            .withVisible(true)
            .withStroke(buildStroke(color))
            .build()
    }

    private fun buildDistAlpha(): ARDistAlpha {
        return ARDistAlpha.Builder.builder()
            .withEnable(true)
            .withMinDist(10f)
            .withMinAlpha(1f)
            .withMaxDist(200f)
            .withMaxAlpha(0.2f)
            .build()
    }

    fun uploadARLine() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        val colorWhite = 0xFFFFFFFF.toInt()
        val colorBlack = 0xFF000000.toInt()

        // 北斗七星七点连线：以飞机位置为锢点相对偏移，高度 50m
        val baseLat = location.latitude
        val baseLon = location.longitude

        val p1 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("α").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat + 0.0000, baseLon + 0.0000, 50.0))
            .build()
        val p2 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("β").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00011, baseLon + 0.00002, 50.0))
            .build()
        val p3 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("γ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00016, baseLon + 0.00020, 50.0))
            .build()
        val p4 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("δ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00009, baseLon + 0.00029, 50.0))
            .build()
        val p5 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("ε").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00012, baseLon + 0.00044, 50.0))
            .build()
        val p6 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(false)
            .withText("ζ").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00014, baseLon + 0.00056, 50.0))
            .build()
        val p7 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(true).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("η").withTextColor(colorWhite).withIconColor(colorWhite)
            .withLocation(OpenLocation(baseLat - 0.00025, baseLon + 0.00066, 50.0))
            .build()

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()
        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorBlack)
            .withDistLineWidth(distLineWidth)
            .withIsDash(false)
            .build()

        val lineInfo = ARLineInfo.Builder.builder()
            .withName("AR Line 01")
            .withPoints(listOf(p1, p2, p3, p4, p5, p6, p7))
            .withStrokeInfo(strokeInfo)
            .withUseDepth(false)
            .build()

        ARManager.getInstance().uploadARLineInfo(lineInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARLineInfo onSuccess $result")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARLineInfo onFailure ${error.errorCode()}")
                }
            })
    }

    fun queryARLine() {
        ARManager.getInstance().queryARLineInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARLineInfo>> {
            override fun onSuccess(result: List<ARLineInfo>) {
                arLineInfos.postValue(result)
                notifyUser("queryARLineInfo onSuccess ${result.size}")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryARLineInfo onFailure ${error.errorCode()}")
            }
        })
    }

    fun updateARLine() {
        val list = arLineInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARLineInfo为空，请先查询/上传！")
            return
        }
        for (info in list) {
            info.strokeInfo = buildStroke(colorRed)
            ARManager.getInstance().updateARLineInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("updateARLineInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("updateARLineInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }

    fun deleteARLine() {
        val list = arLineInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARLineInfo为空，请先查询！")
            return
        }
        for (info in list) {
            ARManager.getInstance().deleteARLineInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("deleteARLineInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("deleteARLineInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }

    // ============================== AR Polygon ==============================
    fun uploadARPolygon() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        // 三角面端点：以飞机位置相对偏移，高度 50m
        val p1 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("A").withTextColor(colorBlue).withIconColor(colorBlue)
            .withLocation(OpenLocation(location.latitude - 0.0002, location.longitude - 0.0002, 50.0))
            .build()
        val p2 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("B").withTextColor(colorGreen).withIconColor(colorGreen)
            .withLocation(OpenLocation(location.latitude - 0.0002, location.longitude + 0.0002, 50.0))
            .build()
        val p3 = ARPointInfo.Builder.builder()
            .withConfigId(arPointConfigImp01.id)
            .withVisible(true).withIconVisible(false).withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText("C").withTextColor(colorRed).withIconColor(colorRed)
            .withLocation(OpenLocation(location.latitude + 0.0002, location.longitude, 50.0))
            .build()

        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightGreen)
            .withBackFaceColor(colorLightWhite)
            .build()
        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightBlue)
            .withBackFaceColor(colorLightGreen)
            .build()
        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightWhite)
            .withBackFaceColor(colorLightGreen)
            .build()

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(15f).withMaxWidthDist(25f)
            .withMinLineWidth(3f).withMinWidthDist(50f)
            .build()
        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(textColorBlack).withDistLineWidth(distLineWidth).withIsDash(false).build()
        val faceStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        val extrusionAnchor = OpenLocation(0.0, 0.0, 0.0)

        val polygonInfo = ARPolygonInfo.Builder.builder()
            .withName("AR Polygon 01")
            .withPoints(listOf(p1, p2, p3))
            .withExtrusionAnchor(extrusionAnchor)
            .withTopFace(topFace)
            .withSideFace(sideFace)
            .withBottomFace(bottomFace)
            .withTopFaceStroke(faceStroke)
            .withSideFaceStroke(faceStroke)
            .withBottomFaceStroke(faceStroke)
            .withFaceDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withFaceStrokeDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withIs3dMesh(false)
            .build()

        ARManager.getInstance().uploadARPolygonInfo(polygonInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARPolygonInfo onSuccess $result")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARPolygonInfo onFailure ${error.errorCode()}")
                }
            })
    }

    fun queryARPolygon() {
        ARManager.getInstance().queryARPolygonInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPolygonInfo>> {
            override fun onSuccess(result: List<ARPolygonInfo>) {
                arPolygonInfos.postValue(result)
                notifyUser("queryARPolygonInfo onSuccess ${result.size}")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryARPolygonInfo onFailure ${error.errorCode()}")
            }
        })
    }

    fun updateARPolygon() {
        val list = arPolygonInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARPolygonInfo为空，请先查询/上传！")
            return
        }
        val newColor = 0x55E23C39
        for (info in list) {
            info.topFace = buildFace(newColor, newColor)
            info.sideFace = buildFace(newColor, newColor)
            info.bottomFace = buildFace(newColor, newColor)
            ARManager.getInstance().updateARPolygonInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("updateARPolygonInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("updateARPolygonInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }

    fun deleteARPolygon() {
        val list = arPolygonInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARPolygonInfo为空，请先查询！")
            return
        }
        for (info in list) {
            ARManager.getInstance().deleteARPolygonInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("deleteARPolygonInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("deleteARPolygonInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }

    // ============================== AR Circle ==============================
    fun uploadARCircle() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("location is null!")
            return
        }

        // 圆：圆心为飞机正前方高度 30m
        val center = OpenLocation(location.latitude, location.longitude, 30.0)
        val extrusionAnchor = OpenLocation(0.0, 0.0, 0.0)

        val circleFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(colorLightBlue)
            .withBackFaceColor(colorLightWhite)
            .build()

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(12f).withMaxWidthDist(50f)
            .withMinLineWidth(3f).withMinWidthDist(100f)
            .build()
        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(colorBlue).withDistLineWidth(distLineWidth).withIsDash(false).build()
        val edgeStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        val arDistAlpha = ARDistAlpha.Builder.builder()
            .withEnable(true).withMinDist(30f).withMaxDist(60f)
            .withMinAlpha(0.3f).withMaxAlpha(0.8f).build()

        val circleInfo = ARCircleInfo.Builder.builder()
            .withName("AR Circle 01")
            .withCenter(center)
            .withRadius(20f)
            .withExtrusionAnchor(extrusionAnchor)
            .withTopFace(circleFace)
            .withBottomFace(circleFace)
            .withSideFace(circleFace)
            .withTopFaceStroke(edgeStroke)
            .withSideFaceStroke(edgeStroke)
            .withBottomFaceStroke(edgeStroke)
            .withFaceDistAlpha(arDistAlpha)
            .withFaceStrokeDistAlpha(arDistAlpha)
            .build()

        ARManager.getInstance().uploadARCircleInfo(circleInfo,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String) {
                    notifyUser("uploadARCircleInfo onSuccess $result")
                }

                override fun onFailure(error: IDJIError) {
                    notifyUser("uploadARCircleInfo onFailure ${error.errorCode()}")
                }
            })
    }

    fun queryARCircle() {
        ARManager.getInstance().queryARCircleInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARCircleInfo>> {
            override fun onSuccess(result: List<ARCircleInfo>) {
                arCircleInfos.postValue(result)
                notifyUser("queryARCircleInfo onSuccess ${result.size}")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryARCircleInfo onFailure ${error.errorCode()}")
            }
        })
    }

    fun updateARCircle() {
        val list = arCircleInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARCircleInfo为空，请先查询/上传！")
            return
        }
        val newColor = 0x55E23C39
        for (info in list) {
            info.topFace = buildFace(newColor, newColor)
            info.sideFace = buildFace(newColor, newColor)
            info.bottomFace = buildFace(newColor, newColor)
            ARManager.getInstance().updateARCircleInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("updateARCircleInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("updateARCircleInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }

    fun deleteARCircle() {
        val list = arCircleInfos.value
        if (list.isNullOrEmpty()) {
            notifyUser("ARCircleInfo为空，请先查询！")
            return
        }
        for (info in list) {
            ARManager.getInstance().deleteARCircleInfo(info,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        notifyUser("deleteARCircleInfo onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        notifyUser("deleteARCircleInfo onFailure ${error.errorCode()}")
                    }
                })
        }
    }
}