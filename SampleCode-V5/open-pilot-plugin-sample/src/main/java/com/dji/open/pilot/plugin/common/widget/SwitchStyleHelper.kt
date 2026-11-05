package com.dji.open.pilot.plugin.common.widget

import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.dji.open.pilot.plugin.R

/**
 * 通用胶囊开关样式扩展。
 *
 * 与 XML 样式 `@style/AppSwitch` 等价，用于代码动态创建的 [SwitchCompat] 复用统一外观：
 * 亮蓝胶囊轨道 + 白色圆形滑块。
 *
 * 两种复用方式：
 * - XML：`style="@style/AppSwitch"`
 * - 代码：`switch.applyAppSwitchStyle()`
 */
fun SwitchCompat.applyAppSwitchStyle() {
    trackDrawable = ContextCompat.getDrawable(context, R.drawable.switch_track)
    thumbDrawable = ContextCompat.getDrawable(context, R.drawable.switch_thumb)
    minimumHeight = 0
    minimumWidth = 0
    setPadding(0, 0, 0, 0)
    switchMinWidth = (44 * resources.displayMetrics.density).toInt()
    thumbTextPadding = 0
    background = null
    // 关闭 AppCompat 默认 tint，否则 track/thumb 会被主题染成青绿色
    thumbTintList = null
    trackTintList = null
}
