package dji.sampleV5.moduleaircraft.data

import dji.v5.common.error.IDJIError




data class MissionUploadStateInfo(
    var tips: String = ""
    , var updateProgress: Double = 0.0
    , val error: IDJIError? = null
) {


}