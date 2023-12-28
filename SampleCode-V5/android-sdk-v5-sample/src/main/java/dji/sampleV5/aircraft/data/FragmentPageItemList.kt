package dji.sampleV5.aircraft.data

data class FragmentPageItemList(
    val vavGraphId: Int = DEFAULT_RES_ID,
    val items: LinkedHashSet<FragmentPageItem> = LinkedHashSet()
)

data class FragmentPageItem(
    val id: Int = DEFAULT_RES_ID,
    val title: Int = DEFAULT_RES_ID,
    val description: Int = DEFAULT_RES_ID,
    val isStrike: Boolean = false
)