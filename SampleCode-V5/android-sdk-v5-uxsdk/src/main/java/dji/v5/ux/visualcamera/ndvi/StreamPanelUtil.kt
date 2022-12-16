package dji.v5.ux.visualcamera.ndvi

import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionType
import dji.v5.ux.R

/**
 * 码流窄带及植被指数工具类
 */
object StreamPanelUtil {

    //实际支持的所有包含窄带的列表
    val ALL_STREAM_LIST: List<CameraVideoStreamSourceType> = arrayListOf(
        CameraVideoStreamSourceType.NDVI_CAMERA, CameraVideoStreamSourceType.MS_G_CAMERA,
        CameraVideoStreamSourceType.MS_R_CAMERA, CameraVideoStreamSourceType.MS_RE_CAMERA,
        CameraVideoStreamSourceType.MS_NIR_CAMERA
    )

    //要显示的窄带列表
    val NARROW_BAND_MODEL_LIST: List<NarrowBandModel> by lazy {
        mutableListOf(
            NarrowBandModel(CameraVideoStreamSourceType.MS_G_CAMERA, "G", R.drawable.uxsdk_stream_narrow_band_g),
            NarrowBandModel(CameraVideoStreamSourceType.MS_R_CAMERA, "R", R.drawable.uxsdk_stream_narrow_band_r),
            NarrowBandModel(CameraVideoStreamSourceType.MS_RE_CAMERA, "RE", R.drawable.uxsdk_stream_narrow_band_re),
            NarrowBandModel(CameraVideoStreamSourceType.MS_NIR_CAMERA, "NIR", R.drawable.uxsdk_stream_narrow_band_nir),
        )
    }

    //植被指数
    val VEGETATION_MODEL_LIST: List<VegetationModel> by lazy {
        mutableListOf(
            VegetationModel(MultiSpectralFusionType.NDVI, "NDVI", R.drawable.uxsdk_stream_vegatation_ndvi),
            VegetationModel(MultiSpectralFusionType.GNDVI, "GNDVI", R.drawable.uxsdk_stream_vegatation_gndvi),
            VegetationModel(MultiSpectralFusionType.NDRE, "NDRE", R.drawable.uxsdk_stream_vegatation_ndre),
        )
    }

    class NarrowBandModel(
        val sourceType: CameraVideoStreamSourceType,
        val name: String,
        val image: Int = 0
    ) {
        val nameRes = name
        val imageRes = image
    }

    class VegetationModel(
        val sourceType: MultiSpectralFusionType,
        val name: String,
        val image: Int = 0
    ) {
        val nameRes = name
        val imageRes = image
    }
}