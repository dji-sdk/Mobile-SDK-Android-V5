package dji.v5.ux.flight.flightparam

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import dji.sdk.keyvalue.value.flightcontroller.GoHomePathMode
import dji.v5.utils.common.AndUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.TabSelectCell
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import io.reactivex.rxjava3.disposables.CompositeDisposable

import io.reactivex.rxjava3.kotlin.addTo

class ReturnHomeModeWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), TabSelectCell.OnTabChangeListener {

    companion object {
        private const val TAG = "GoHomeModeWidgetModel"
    }

    private val goHomeModeTab: TabSelectCell
    private val goHomeModeTv: TextView

    private val goHomeHeightNearGroundDrawable: Drawable?
    private val goHomeHeightFixedDrawable: Drawable?

    private val compositeDisposable = CompositeDisposable()

    private val widgetModel = GoHomeModeWidgetModel(
        DJISDKModel.getInstance(),
        ObservableInMemoryKeyedStore.getInstance())
    init {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_setting_menu_aircraft_go_home_mode_layout, this)
        orientation = VERTICAL

        goHomeModeTab = findViewById(R.id.setting_menu_aircraft_go_home_mode)
        goHomeModeTv = findViewById(R.id.setting_menu_aircraft_go_home_mode_desc)

        val width = context.resources.getDimensionPixelSize(R.dimen.uxsdk_setting_menu_perception_pic_cac_width)
        val height = context.resources.getDimensionPixelSize(R.dimen.uxsdk_setting_menu_perception_pic_cac_height)
        goHomeHeightNearGroundDrawable = context.getDrawable(R.drawable.uxsdk_setting_ui_go_home_mode_height_near_ground)
        goHomeHeightFixedDrawable = context.getDrawable(R.drawable.uxsdk_setting_ui_go_home_mode_height_fixed)

        goHomeHeightNearGroundDrawable?.setBounds(0, 0, width, height)
        goHomeHeightFixedDrawable?.setBounds(0, 0, width, height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        registerGoHomeMode()
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        unregisterGoHomeMode()
        super.onDetachedFromWindow()
    }

    private fun registerGoHomeMode() {
        goHomeModeTab.setOnTabChangeListener(this)
        widgetModel.goHomePathModeFlowable().observeOn(SchedulerProvider.ui())

            .subscribe {
                val isSupport = widgetModel.isSupportGoHomeMode
                if (isSupport) {
                    visibility = VISIBLE
                } else {
                    visibility = GONE
                    return@subscribe
                }
                val currentDrawable: Drawable?
                val value: Int
                LogUtils.i(TAG, "goHomeMode >> $it")
                if (it == GoHomePathMode.HEIGHT_NEAR_GROUND) {
                    visibility = VISIBLE
                    goHomeModeTv.text = AndUtil.getResString(R.string.uxsdk_setting_menu_flyc_smart_rth_smart_altitude_des)
                    currentDrawable = goHomeHeightNearGroundDrawable
                    value = 1
                } else {
                    visibility = VISIBLE
                    goHomeModeTv.text = AndUtil.getResString(R.string.uxsdk_setting_menu_flyc_smart_rth_preset_altitude_des)
                    currentDrawable = goHomeHeightFixedDrawable
                    value = 0
                }
                goHomeModeTv.setCompoundDrawables(currentDrawable, null, null, null)
                goHomeModeTab.currentTab = value
            }.addTo(compositeDisposable)
    }

    private fun unregisterGoHomeMode() {
        goHomeModeTab.setOnTabChangeListener(null)
        compositeDisposable.clear()
    }

    override fun onTabChanged(cell: TabSelectCell?, oldIndex: Int, newIndex: Int) {
        val goHomePathMode = if (newIndex == 1) GoHomePathMode.HEIGHT_NEAR_GROUND else GoHomePathMode.HEIGHT_FIXED
        widgetModel.setGoHomePathMode(goHomePathMode)
            .subscribe {
                LogUtils.i(TAG, "onTabChanged >> $it")
                goHomeModeTab.currentTab = oldIndex
            }
            .addTo(compositeDisposable)
    }

}