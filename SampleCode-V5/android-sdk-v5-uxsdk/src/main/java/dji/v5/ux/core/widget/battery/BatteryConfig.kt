package dji.v5.ux.core.widget.battery

/**
 * 电池配置
 */
object BatteryConfig {
    /**
     * 高电位存储红色显示
     */
     const val HIGH_VOLTAGE_SAVE_DAYS_DANGER = 120

    /**
     * 高电位存储黄色显示
     */
     const val HIGH_VOLTAGE_SAVE_DAYS_WARN = 60

    /**
     * 高电位存储接口: 秒->天
     */
     const val SECONDS_IN_DAY = 24 * 60 * 60
}