package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewOnItemSelectedListener
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.DividerConfig
import dji.open.pilot.base.ui.model.ListItemData
import dji.open.pilot.base.ui.model.ListItemOperation
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.utils.common.JsonUtil

/**
 * 远程列表视图，支持4种预定义Item类型：
 * - [ListItemData.text]: 纯文本
 * - [ListItemData.iconText]: 图标+标题
 * - [ListItemData.iconTwoLine]: 图标+双行文本
 * - [ListItemData.iconTextValue]: 图标+标题+右侧值
 *
 * 使用示例:
 * ```kotlin
 * val listView = helper.createListView()
 * listView.setWidth(300)
 * listView.setHeight(400)
 * listView.addToRoot()
 *
 * listView.setDividerConfig(DividerConfig(1, "#33FFFFFF", 56, 0, false))
 *
 * listView.setItems(listOf(
 *     ListItemData.text("id1", "纯文本项"),
 *     ListItemData.iconText("id2", "file.png", "文件A.txt"),
 *     ListItemData.iconTwoLine("id3", "photo.png", "照片B.jpg", "5.1MB"),
 *     ListItemData.iconTextValue("id4", "setting.png", "分辨率", "4K")
 * ))
 *
 * listView.setOnItemClickListener { position, itemId ->
 *     // handle click
 * }
 * ```
 *
 * 多语言支持（使用 setItemsByI18nBuilder）：
 * ```kotlin
 * listView.setItemsByI18nBuilder {
 *     listOf(
 *         ListItemData.text("id1", RemoteLangUtils.getString(R.string.item_text)),
 *         ListItemData.iconText("id2", "file.png", RemoteLangUtils.getString(R.string.item_file))
 *     )
 * }
 * ```
 */

class RemoteListView(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.VERTICAL_LIST_VIEW) {

    private var onItemClickListener: OnItemClickListener? = null

    /**
     * 列表数据构建器，语言切换时由 updateTextByResId() 重新执行以刷新文案。
     * null 表示未使用多语言模式（通过 setItems 直接设置）。
     */
    private var itemsBuilder: (() -> List<ListItemData>)? = null

    init {
        // 使用 IOnItemSelectedListener 通道接收 item 点击事件
        // index = position, item = itemId
        remoteView.onItemSelectedListener = IRemoteViewOnItemSelectedListener { index, item ->
            onItemClickListener?.onItemClick(index, item)
        }
    }

    /**
     * 添加列表到根布局
     * @param params 布局参数（可选）
     */
    fun addToRoot(params: WidgetLayoutParam? = null) {
        helper.addViewToParent(remoteView, RemoteView.PARENT_ID, params)
        syncToRemote()
    }

    // region 数据操作

    /**
     * 全量设置列表数据
     * @param items 列表数据，最多支持200条
     */
    fun setItems(items: List<ListItemData>): OperationResult {
        require(items.size <= MAX_ITEM_COUNT) {
            "List item count exceeds maximum limit of $MAX_ITEM_COUNT"
        }
        return helper.updateView(remoteView.id, RemoteViewProperty.LIST_DATA, JsonUtil.toJson(items))
    }

    /**
     * 通过构建器设置列表数据，支持语言切换时自动刷新。
     *
     * 与 [setItems] 的区别：语言切换时，[RemoteViewHelper] 会自动重新调用 builder
     * 获取最新语言的文案并刷新列表，无需业务层手动处理。
     *
     * builder 中应使用 RemoteLangUtils.getString(resId) 获取当前语言文案：
     * ```kotlin
     * listView.setItemsBuilder {
     *     listOf(
     *         ListItemData.text("id1", RemoteLangUtils.getString(R.string.item_label)),
     *         ListItemData.iconTwoLine("id2", "icon.png",
     *             RemoteLangUtils.getString(R.string.item_title),
     *             RemoteLangUtils.getString(R.string.item_subtitle))
     *     )
     * }
     * ```
     *
     * @param builder 构建列表数据的 lambda，每次语言切换时重新执行
     */
    fun setItemsBuilder(builder: () -> List<ListItemData>) {
        itemsBuilder = builder
        setItems(builder())
    }

    /**
     * 在指定位置插入一条数据
     * @param position 插入位置
     * @param item 数据
     */
    fun insertItem(position: Int, item: ListItemData): OperationResult {
        return helper.updateView(
            remoteView.id,
            RemoteViewProperty.LIST_INSERT_ITEM,
            JsonUtil.toJson(ListItemOperation(position, item))
        )
    }

    /**
     * 删除指定位置的数据
     * @param position 删除位置
     */
    fun removeItem(position: Int): OperationResult {
        return helper.updateView(remoteView.id, RemoteViewProperty.LIST_REMOVE_ITEM, position.toString())
    }

    /**
     * 更新指定位置的数据（整体替换）
     * @param position 更新位置
     * @param item 新数据
     */
    fun updateItem(position: Int, item: ListItemData): OperationResult {
        return helper.updateView(
            remoteView.id,
            RemoteViewProperty.LIST_UPDATE_ITEM,
            JsonUtil.toJson(ListItemOperation(position, item))
        )
    }

    // endregion

    // region 配置

    /**
     * 设置分割线配置
     * @param config 分割线配置
     */
    fun setDividerConfig(config: DividerConfig): OperationResult {
        return helper.updateView(remoteView.id, RemoteViewProperty.LIST_DIVIDER_CONFIG, JsonUtil.toJson(config))
    }

    /**
     * 设置Item点击事件是否启用
     * @param enabled true=启用，false=禁用
     */
    fun setItemClickEnabled(enabled: Boolean): OperationResult {
        return helper.updateView(remoteView.id, RemoteViewProperty.LIST_ITEM_CLICK_ENABLED, enabled.toString())
    }

    // endregion

    // region 事件订阅

    /**
     * 设置Item点击订阅器
     * @param listener 点击订阅器，回调 position 和 itemId
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    /**
     * 设置Item点击订阅器（lambda 方式）
     * @param listener 点击订阅器，参数为 (position, itemId)
     */
    fun setOnItemClickListener(listener: (position: Int, itemId: String) -> Unit) {
        onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, itemId: String) {
                listener(position, itemId)
            }
        }
    }

    override fun updateTextByResId() {
        itemsBuilder?.let { builder ->
            setItems(builder())
            return
        }
        super.updateTextByResId()
    }

    override fun clearResIds() {
        super.clearResIds()
        itemsBuilder = null
    }

    // endregion

    /**
     * Item点击事件订阅器
     */
    interface OnItemClickListener {
        /**
         * Item被点击
         * @param position 点击的位置
         * @param itemId 点击的Item唯一标识
         */
        fun onItemClick(position: Int, itemId: String)
    }

    companion object {
        /** 列表最大Item数量 */
        const val MAX_ITEM_COUNT = 100
    }
}
