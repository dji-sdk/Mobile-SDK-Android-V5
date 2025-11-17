package dji.v5.ux.obstacle

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import dji.sdk.keyvalue.utils.ProductUtil
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.utils.common.AndUtil
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.utils.dpad.DpadProductManager
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.ViewUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

/**
 * Description : 用于快捷设置所有避障状态，一键刹停/关闭/绕行

 *
 * @author: Byte.Cai
 *  date : 2023/8/11
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class AvoidanceShortcutWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget<AvoidanceShortcutWidget.ModelState>(context, attrs),
    TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {

    companion object {
        private const val TAG = "AvoidanceShortcutWidget"
    }

    private lateinit var apasTab: TabLayout
    private lateinit var apasDesc: TextView


    private val compositeDisposable = CompositeDisposable()

    private var flightMode = FCFlightMode.UNKNOWN

    private val tabItemResMap = mapOf(
        ObstacleAvoidanceType.BRAKE to R.string.uxsdk_fpv_setting_safe_obstacle_avoidance_behavior_brake_btn,
        ObstacleAvoidanceType.BYPASS to R.string.uxsdk_fpv_setting_safe_obstacle_avoidance_behavior_detour_btn,
        ObstacleAvoidanceType.CLOSE to R.string.uxsdk_fpv_setting_safe_obstacle_avoidance_behavior_off_btn
    )

    private var currentMode = ObstacleAvoidanceType.CLOSE
    private val widgetModel by lazy {
        AvoidanceShortcutWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            widgetModel.setup()
        }

        apasTab = findViewById(R.id.omni_apas_tab)
        apasDesc = findViewById(R.id.omni_apas_desc)

        apasTab.clipToOutline = true
        apasTab.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.apply {
                    val radius = context.resources.getDimension(R.dimen.uxsdk_4_dp)
                    setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, radius);
                }
            }
        }
    }


    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_setting_menu_omni_apas_layout, this, true)

    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productTypeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateTabItems()
        })
        addReaction(widgetModel.flightModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            flightMode = it
        })

        addReaction(widgetModel.obstacleAvoidanceTypeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            currentMode = it
            updateSelectTab(it)
        })


    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        compositeDisposable.clear()
        super.onDetachedFromWindow()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        val value = tab?.tag as ObstacleAvoidanceType
        setObstacleAction(value)

        if (value != ObstacleAvoidanceType.CLOSE) {
            val mode = flightMode.getFlightModeString()
            // 当前档位为A/S时需要提示用户
            if (mode == "A" || mode == "S") {
                var content = if (value == ObstacleAvoidanceType.BRAKE)
                    StringUtils.getResStr(context, R.string.uxsdk_setting_menu_perception_break_s_mode, mode)
                else
                // 切换绕行
                    StringUtils.getResStr(R.string.uxsdk_setting_menu_perception_apas_s_mode, mode)
                showToast(content)
            }
        } else {
            val resStr = StringUtils.getResStr(R.string.uxsdk_setting_menu_perception_apas_off_dialog_content)
            ViewUtil.showToast(context, resStr)
        }
    }

    override fun onTabUnselected(p0: TabLayout.Tab?) {
        //do nothing
    }

    override fun onTabReselected(p0: TabLayout.Tab?) {
        //do nothing
    }

    private fun selectTab(tab: TabLayout.Tab) {
        apasTab.removeOnTabSelectedListener(this)
        tab.select()
        apasTab.addOnTabSelectedListener(this)
    }

    private fun setObstacleAction(type: ObstacleAvoidanceType) {
        widgetModel.setObstacleActionType(type).observeOn(SchedulerProvider.ui())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                    //do nothing
                }

                override fun onComplete() {
                    //do nothing
                }

                override fun onError(e: Throwable) {
                    //do nothing
                    LogUtils.e(TAG, "setObstacleAction onError:$e")
                    revertObstacleAction()
                }

            })

    }

    private fun revertObstacleAction() {
        updateTabByMode()
    }

    private fun updateTabItems() {
        visibility = VISIBLE
        apasTab.removeAllTabs()

        //添加避障行为设置选项
        getAPASModeRange().forEach {
            apasTab.addTab(generateTabItem(it), false)
        }

        // 初始化选中 tab，这时候才会设置观察者
        updateTabByMode()
    }

    private fun updateTabByMode() {
        updateSelectTab(currentMode)
    }

    private fun updateSelectTab(type: ObstacleAvoidanceType) {
        for (i in 0 until apasTab.tabCount) {
            val tab = apasTab.getTabAt(i)
            if (tab?.tag == type) {
                selectTab(tab)
                //更新对应模式的文字描述
                apasDesc.text = when (type) {
                    ObstacleAvoidanceType.BYPASS -> AndUtil.getResString(R.string.uxsdk_setting_menu_perception_apas_description)
                    ObstacleAvoidanceType.BRAKE -> AndUtil.getResString(R.string.uxsdk_setting_menu_perception_break_description)
                    ObstacleAvoidanceType.CLOSE -> AndUtil.getResString(R.string.uxsdk_setting_menu_perception_close_description)
                    else -> ""
                }
                break
            }
        }
    }

    private fun generateTabItem(type: ObstacleAvoidanceType) = apasTab.newTab().apply {
        setText(tabItemResMap[type]!!)
        tag = type
    }

    /**
     * 动态添加APAS子控件
     */
    private fun getAPASModeRange(): List<ObstacleAvoidanceType> {
        //行业机中只有M3支持绕行，其他机型都只支持刹停和关闭
        return if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            listOf(
                ObstacleAvoidanceType.BRAKE,
                ObstacleAvoidanceType.BYPASS,
                ObstacleAvoidanceType.CLOSE
            )
        } else {
            listOf(ObstacleAvoidanceType.BRAKE, ObstacleAvoidanceType.CLOSE)
        }
    }

    private fun FCFlightMode.getFlightModeString(): String {
        return when (this) {
            FCFlightMode.UNKNOWN ->
                ""

            FCFlightMode.ATTI ->
                "A"

            FCFlightMode.GPS_SPORT ->
                "S"

            FCFlightMode.TRIPOD_GPS ->
                "T"

            else -> {
                if (DpadProductManager.getInstance().isDjiRcPlus || DpadProductManager.getInstance().isDjiRcPro || ProductUtil.isM30Product() || ProductUtil.isM350Product() || ProductUtil.isM400Product()) "N" else "P"
            }

        }
    }

    private fun showToast(msg: String) {
        AndroidSchedulers.mainThread().scheduleDirect {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    sealed class ModelState


}