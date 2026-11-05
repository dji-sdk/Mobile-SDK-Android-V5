package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.model.ProgressStatusType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType

/**
 * 圆形进度条远程视图封装。
 *
 * 对应服务端 ViewType.CIRCULAR_PROGRESS_BAR。
 *
 * 支持的属性：
 * - [setProgress]      设置当前进度（需小于等于通过 [setMax] 设置的最大值，默认 0-100）
 * - [setMax]           设置最大进度值（默认 100）
 * - [setProgressColor] 设置进度弧颜色（ARGB 整型）
 * - [setText]          设置圆心显示文字（NORMAL 状态下生效）
 * - [setTextColor]     设置圆心文字颜色（ARGB 整型）
 * - [setTextSize]      设置圆心文字大小（sp）
 * - [setStatus]        设置进度条状态（NORMAL / SUCCESS / FAIL）
 */
class RemoteCircleProgressBar(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.CIRCULAR_PROGRESS_BAR) {

    /**
     * 设置当前进度，范围 0-100（如已通过 [setMax] 自定义最大值，需调用方自行确保 progress ≤ max）。
     */
    fun setProgress(progress: Int) {
        propertyMap[RemoteViewProperty.PROGRESS] = progress.coerceIn(0, 100).toString()
    }

    /**
     * 设置进度条最大值（默认为 100）。
     * 设置后需同步调整 [setProgress] 传入的值，使其不超过 max。
     *
     * @param max 最大进度值，需大于 0
     */
    fun setMax(max: Int) {
        propertyMap[RemoteViewProperty.PROGRESS_MAX] = max.toString()
    }

    /**
     * 设置进度弧颜色（ARGB 整型）。
     */
    fun setProgressColor(color: Int) {
        propertyMap[RemoteViewProperty.PROGRESS_COLOR] = color.toString()
    }

    /**
     * 设置圆心显示的文字内容，NORMAL 状态下生效。
     */
    override fun setText(text: String) {
        propertyMap[RemoteViewProperty.TEXT] = text
    }

    /**
     * 设置圆心文字颜色（ARGB 整型）。
     */
    fun setTextColor(color: Int) {
        propertyMap[RemoteViewProperty.TEXT_COLOR] = color.toString()
    }

    /**
     * 设置圆心文字大小（单位：sp）。
     */
    fun setTextSize(size: Float) {
        propertyMap[RemoteViewProperty.PROGRESS_TEXT_SIZE] = size.toInt().toString()
    }

    /**
     * 设置进度条状态。
     * - NORMAL：显示文字
     * - SUCCESS：圆心显示成功图标，隐藏文字
     * - FAIL：圆心显示失败图标，隐藏文字
     */
    fun setStatus(status: ProgressStatusType) {
        propertyMap[RemoteViewProperty.PROGRESS_STATUS] = status.value.toString()
    }
}
