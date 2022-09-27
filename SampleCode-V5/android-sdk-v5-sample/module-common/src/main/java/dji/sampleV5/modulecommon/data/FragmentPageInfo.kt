package dji.sampleV5.modulecommon.data

data class FragmentPageInfo(
        val vavGraphId: Int = DEFAULT_RES_ID,
        val items: LinkedHashSet<FragmentPageInfoItem> = LinkedHashSet<FragmentPageInfoItem>()
)

data class FragmentPageInfoItem(
        val id: Int = DEFAULT_RES_ID,
        val title: Int = DEFAULT_RES_ID,
        val description: Int = DEFAULT_RES_ID
)