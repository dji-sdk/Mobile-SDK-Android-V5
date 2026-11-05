package dji.sampleV5.aircraft.data

import dji.v5.common.register.SDKLinKMode

data class FragmentPageItemList(
    val vavGraphId: Int = DEFAULT_RES_ID,
    val items: LinkedHashSet<FragmentPageItem> = LinkedHashSet()
)

data class FragmentPageItem(
    val id: Int = DEFAULT_RES_ID,
    val title: Int = DEFAULT_RES_ID,
    val description: Int = DEFAULT_RES_ID,
    val sdkLinKMode: List<SDKLinKMode> = listOf(),
    val isStrike: Boolean = false
)