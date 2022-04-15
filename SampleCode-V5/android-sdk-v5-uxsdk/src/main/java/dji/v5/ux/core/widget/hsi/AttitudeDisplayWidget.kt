package dji.v5.ux.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.GpsUtils
import dji.v5.ux.core.util.UnitUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.uxsdk_liveview_pfd_attitude_display_widget.view.*
import java.lang.Double.NaN
import java.util.*
import kotlin.math.abs

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/26
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class AttitudeDisplayWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<AttitudeDisplayWidget.ModelState>(context, attrs, defStyleAttr) {

    private val widgetModel by lazy {
        AttitudeDisplayModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * 飞行器相对home点的高度
     */
    private var mAltitude = 0F

    /**
     * home点的高度
     */
    private var mHomePointAltitude = 0.0

    /**
     * 飞行器垂直速度
     */
    private var mSpeedZ = 0F

    /**
     * 飞行器坐标
     */
    private var mAircraftLocation = LocationCoordinate2D(Double.NaN, Double.NaN)

    open fun loadLayout() {
        View.inflate(context, R.layout.uxsdk_liveview_pfd_attitude_display_widget, this)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        loadLayout()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            pfd_attitude_dash_board.setModel(widgetModel)
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
        addDisposable((widgetModel.velocityProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe { velocity ->
            mSpeedZ = velocity.z.toFloat()
            updateSpeed()
        }))
        addDisposable(widgetModel.altitudeProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe { altitude ->
            mAltitude = altitude.toFloat()
            updateAltitude()
        })

        //RTK起飞高度信息
        addDisposable(widgetModel.rtkTakeoffAltitudeInfoProcessor.toFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .filter { info -> abs(mHomePointAltitude - info.altitude) >= 0.1 }
            .subscribe { info ->
                mHomePointAltitude = info.altitude
                updateAltitude()
            }
        )

        addDisposable(widgetModel.aircraftLocationDataProcessor.toFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { location ->
                mAircraftLocation = location
                if (mAircraftLocation != null) {
                    updateAltitude()
                }
            })
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    private fun updateAltitude() {
        addDisposable(Flowable.create({ emitter: FlowableEmitter<Float> ->
            val lat: Double = mAircraftLocation?.latitude ?: NaN
            val lon: Double = mAircraftLocation?.longitude ?: NaN
            val aslValue: Double = GpsUtils.egm96Altitude((mHomePointAltitude + mAltitude).toDouble(), lat, lon)
            val value: Float = UnitUtils.getValueFromMetricByLength(aslValue.toFloat(), if (UnitUtils.isMetricUnits()) UnitUtils.UnitType.METRIC else UnitUtils.UnitType.IMPERIAL)
            emitter.onNext(value)
        }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe { aValue ->
                pfd_asl_value.text = String.format(Locale.US, "%06.1f", aValue as Float)
            })
    }

    private fun updateSpeed() {
        var showSpeedZ: Float = mSpeedZ
        if (!java.lang.Float.isNaN(mSpeedZ) && mSpeedZ != 0f) {
            showSpeedZ = -mSpeedZ
        }
        val value: Float = UnitUtils.transFormSpeedIntoDifferentUnit(showSpeedZ)
        pfd_vs_value.text = String.format(Locale.US, "%03.1f", value)
    }

    sealed class ModelState
}