package dji.v5.ux.remotecontroller

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.Toast
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.ViewUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.uxsdk_widget_rc_check_frequency_layout.view.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/8/16
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
const val TAG = "RcCheckFrequencyWidget"

class RCPairingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<RCPairingWidget.ModelState>(context, attrs, defStyleAttr) {
    private var connect = false
    private var isMotorOn = false
    private var pairingState = PairingState.UNKNOWN

    private val widgetModel by lazy {
        RcCheckFrequencyWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    sealed class ModelState

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rc_check_frequency_layout, this)
        updateCheckFrequencyText(StringUtils.getResStr(R.string.uxsdk_setting_menu_title_rc_check_frequency))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.connectionProcessor.toFlowable()
            .subscribe { connect = it })

        addReaction(widgetModel.isMotorOnProcessor.toFlowable()
            .subscribe { isMotorOn = it })

        addReaction(widgetModel.pairingStateProcessor.toFlowable()
            .subscribe {
                if (pairingState == it) {
                    return@subscribe
                }
                LogUtils.i(TAG, "pairingState=$it")
                if (pairingState == PairingState.UNPAIRED && PairingState.PAIRING == it) {
                    updateCheckFrequencyText(StringUtils.getResStr(R.string.uxsdk_setting_ui_rc_stop_pair))
                    ViewUtil.showToast(context, StringUtils.getResStr(R.string.uxsdk_setting_ui_rc_start_pair))
                } else if (pairingState == PairingState.PAIRING && PairingState.UNPAIRED == it) {
                    updateCheckFrequencyText(StringUtils.getResStr(R.string.uxsdk_setting_menu_title_rc_check_frequency))
                    ViewUtil.showToast(context, StringUtils.getResStr(R.string.uxsdk_setting_ui_rc_stop_pair))
                }
                pairingState = it

            })


        setting_menu_rc_check_frequency.setOnClickListener {
            if (isMotorOn && connect) {
                ViewUtil.showToast(context, R.string.uxsdk_dialog_message_rc_cannot_frequency_motorup, Toast.LENGTH_SHORT)
            } else if (pairingState == PairingState.PAIRING) {
                widgetModel.stopPairing().subscribe(object : CompletableObserver {
                    override fun onSubscribe(d: Disposable?) {
                        //do nothing
                    }

                    override fun onComplete() {
                        //do nothing
                    }

                    override fun onError(e: Throwable?) {
                        //do nothing
                        ViewUtil.showToast(context, e?.message, Toast.LENGTH_SHORT)
                    }

                })
            } else {
                widgetModel.startPairing().subscribe(object : CompletableObserver {
                    override fun onSubscribe(d: Disposable?) {
                        //do nothing
                    }

                    override fun onComplete() {
                        //do nothing
                    }

                    override fun onError(e: Throwable?) {
                        //do nothing
                        ViewUtil.showToast(context, e?.message, Toast.LENGTH_SHORT)
                    }

                })
            }
        }


    }

    private fun updateCheckFrequencyText(content: String) {
        if (TextUtils.isEmpty(content)) {
            return
        }

        AndroidSchedulers.mainThread().scheduleDirect {
            setting_menu_rc_check_frequency.text = content
        }
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

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

}