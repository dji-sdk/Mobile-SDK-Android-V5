package dji.sampleV5.moduleaircraft.models

import dji.sampleV5.modulecommon.models.DJIViewModel

import dji.v5.manager.aircraft.upgrade.UpgradeManager
import dji.v5.manager.aircraft.upgrade.UpgradeableComponent
import dji.v5.manager.aircraft.upgrade.UpgradeableComponentListener

/**
 * @author feel.feng
 * @time 2022/01/26 10:56 上午
 * @description:
 */
class UpgradeVM : DJIViewModel() {

    fun addUpgradeableComponentListener(listener: UpgradeableComponentListener) {
        UpgradeManager.getInstance().addUpgradeableComponentListener(listener)
    }

    fun getUpgradeableComponents(): List<UpgradeableComponent> {
        return UpgradeManager.getInstance().upgradeableComponents
    }
}