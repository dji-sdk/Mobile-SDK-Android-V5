package dji.sampleV5.modulecommon.data

import androidx.fragment.app.Fragment
import dji.sampleV5.modulecommon.data.DEFAULT_RES_ID
import dji.sampleV5.modulecommon.data.DEFAULT_STR
import java.util.LinkedHashSet

data class FragmentPageInfo(
        val vavGraphId: Int = DEFAULT_RES_ID,
        val items: LinkedHashSet<FragmentPageInfoItem> = LinkedHashSet<FragmentPageInfoItem>()
)

data class FragmentPageInfoItem(
        val id: Int = DEFAULT_RES_ID,
        val title: Int = DEFAULT_RES_ID,
        val description: Int = DEFAULT_RES_ID
)