package dji.v5.ux.obstacle

import android.content.Context
import android.util.AttributeSet
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.SwitcherCell
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.uxsdk_widget_precision_landing_widget_layout.view.*

/**
 * Description :该控件不支持M3系列飞机
 *
 * @author: Byte.Cai
 *  date : 2023/8/15
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
private const val TAG = "PrecisionLandingWidget"

class PrecisionLandingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<VisionPositionWidget.ModelState>(context, attrs, defStyleAttr), SwitcherCell.OnCheckedChangedListener {

    private val widgetModel by lazy {
        PrecisionLandingWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun onCheckedChanged(cell: SwitcherCell?, isChecked: Boolean) {
        if (cell?.id == R.id.omni_common_precision_landing) {

            widgetModel.setPrecisionLandingEnabled(isChecked).observeOn(SchedulerProvider.ui())
                .subscribe(object : CompletableObserver {
                    override fun onSubscribe(d: Disposable?) {
                        //do nothing
                    }

                    override fun onComplete() {
                        LogUtils.i(TAG,"setPrecisionLandingEnabled onComplete!")
                    }

                    override fun onError(e: Throwable?) {
                        //do nothing
                        LogUtils.e(TAG, "setPrecisionLandingEnabled onError:$e")
                        updatePrecisionLandingEnable(!isChecked)
                    }

                })
        }

    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_precision_landing_widget_layout, this)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.perceptionInfoProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                updatePrecisionLandingEnable(it.isPrecisionLandingEnabled)
            })

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()

        }
        omni_common_precision_landing.setOnCheckedChangedListener(this)
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    private fun updatePrecisionLandingEnable(enable: Boolean) {
        omni_common_precision_landing.setOnCheckedChangedListener(null)
        omni_common_precision_landing.isChecked = enable
        omni_common_precision_landing.setOnCheckedChangedListener(this)
    }

}