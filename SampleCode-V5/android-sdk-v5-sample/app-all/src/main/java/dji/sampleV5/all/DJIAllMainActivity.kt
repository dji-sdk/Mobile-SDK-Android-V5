package dji.sampleV5.all

import dji.sampleV5.modulecommon.DJIMainActivity
import dji.v5.ux.core.util.UxSharedPreferencesUtil
import dji.v5.ux.sample.showcase.defaultlayout.DefaultLayoutActivity
import dji.v5.ux.sample.showcase.widgetlist.WidgetsActivity

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIAllMainActivity : DJIMainActivity() {

    override fun prepareUxActivity() {
        UxSharedPreferencesUtil.initialize(this)
        enableDefaultLayout(DefaultLayoutActivity::class.java)
        enableWidgetList(WidgetsActivity::class.java)
    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(AllTestingToolsActivity::class.java)
    }
}