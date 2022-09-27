package dji.v5.ux.accessory

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.Switch
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.util.RxUtil
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.Disposable

/**
 * Description : This widget displays a switch that will enable or disable RTK Keep Status.

 *
 * @author: Byte.Cai
 *  date : 2022/7/11
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
private const val TAG = "RTKKeepStatusWidget"

class RTKKeepStatusWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<Boolean>(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {


    private val rtkKeepStatusSwitch: Switch = findViewById(R.id.rtk_keep_status_switch)

    private val widgetModel by lazy {
        RTKKeepStatusWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            RTKCenter.getInstance()
        )
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_keep_status_enable_layout, this)
    }

    init {
        rtkKeepStatusSwitch.setOnCheckedChangeListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.rtkKeepStatusEnable
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                setRTKSwitch(it)
            }
        )
    }

    private fun setRTKSwitch(isChecked: Boolean) {
        rtkKeepStatusSwitch.setOnCheckedChangeListener(null)
        rtkKeepStatusSwitch.isChecked = isChecked
        rtkKeepStatusSwitch.setOnCheckedChangeListener(this)
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_rtk_keep_status_ratio)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        widgetModel.setRTKKeepStatusEnable(isChecked)
            .observeOn(SchedulerProvider.ui())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable?) {
                    //Do nothing
                }

                override fun onComplete() {
                    //Do nothing
                }

                override fun onError(e: Throwable?) {
                    setRTKSwitch(!isChecked)
                    RxUtil.logErrorConsumer(TAG, "canEnableRTK: ")
                }
            })
    }

}