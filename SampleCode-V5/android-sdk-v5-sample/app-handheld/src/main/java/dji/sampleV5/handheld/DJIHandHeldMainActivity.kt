package dji.sampleV5.handheld

import dji.sampleV5.modulecommon.DJIMainActivity

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIHandHeldMainActivity : DJIMainActivity() {

    override fun prepareUxActivity() {
        //暂不支持ux
    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(HandheldTestingToolsActivity::class.java)
    }
}