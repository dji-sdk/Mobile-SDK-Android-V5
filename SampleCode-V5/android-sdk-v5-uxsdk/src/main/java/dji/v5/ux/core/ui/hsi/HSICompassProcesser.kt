package dji.v5.ux.core.ui.hsi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.LogUtils

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference

internal class HSICompassProcesser(context: Context, listener: CompassListener) {
    private val mSensorMgr: SensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var mAccelerometerStatus: Int = SensorManager.SENSOR_STATUS_NO_CONTACT
    private var mMagneticStatus: Int = SensorManager.SENSOR_STATUS_NO_CONTACT
    private val mDefaultDisplay: Display
    private val mRegisterManager: RegisterManager
    private var mAccelerometerData: FloatArray? = null
    private var mMagneticValues: FloatArray? = null
    var mListener: CompassListener

    private val sampleTimeInMs = 100L
    private var lastUpdateTime = 0L

    private fun calcOrientation() {
        if (mAccelerometerData == null || mMagneticValues == null) {
            return
        }
        val values = FloatArray(3)
        val matrix = FloatArray(9)
        SensorManager.getRotationMatrix(matrix, null, mAccelerometerData, mMagneticValues)
        SensorManager.getOrientation(matrix, values)
        values[0] = Math.toDegrees(values[0].toDouble()).toFloat()
        var orientation = values[0]
        if (orientation < 0) {
            orientation += 360
        }
        orientation += screenOrientation.toFloat()
        if (orientation > 360) {
            orientation -= 360f
        }

        val strength = mAccelerometerStatus.coerceAtMost(mMagneticStatus)
        notifyDisposable?.dispose()
        notifyDisposable = AndroidSchedulers.mainThread().scheduleDirect {
            mListener.onOrientationChange(strength, orientation)
        }
    }

    @Volatile
    private var screenOrientation: Int = 0


    private var notifyDisposable: Disposable? = null

    fun start() {
        mRegisterManager.registerSensorListener()
        updateScreenOrientation()
    }


    fun stop() {
        mRegisterManager.unRegisterSensorListener()
        notifyDisposable?.dispose()
    }

    fun updateScreenOrientation() {
        screenOrientation = mDefaultDisplay.rotation * 90
    }

    fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerData = event.values
        }
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagneticValues = event.values
        }
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime >= sampleTimeInMs) {
            lastUpdateTime = now
            calcOrientation()
        }
    }

    fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        LogUtils.d(TAG, "onAccuracyChanged: type: ${sensor?.type}, accuracy: $accuracy")
        when (sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> mAccelerometerStatus = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> mMagneticStatus = accuracy
        }
    }

    /**
     * 监测设备朝向
     */
    internal interface CompassListener {
        /**
         * @param strength 信号强度，one of SensorManager.SENSOR_STATUS_*
         * @param degree 朝向
         */
        fun onOrientationChange(strength: Int, degree: Float)
    }

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDefaultDisplay = wm.defaultDisplay
        val mAccelerometerSensor: Sensor? = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mMagneticSensor: Sensor? = mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mRegisterManager = RegisterManager(
            mSensorMgr,
            mAccelerometerSensor,
            mMagneticSensor,
            HSICompassSensorEventListener(WeakReference(this)),
            HSICompassSensorOrientationEventListener(WeakReference(this), context.applicationContext),
        )
        mListener = listener
    }

    private class RegisterManager(
        private val sensorManager: SensorManager,
        private val mAccelerometerSensor: Sensor?,
        private val mMagneticSensor: Sensor?,
        private val mRcOriListener: SensorEventListener,
        private val orientationEventListener: OrientationEventListener,
    ) {
        // 在自己的线程上进行计算
        private val handler: Handler = Handler(DJIExecutor.getLooper())

        fun registerSensorListener() {
            mAccelerometerSensor?.let {
                mMagneticSensor?.let {
                    handler.post {
                        //耗时操作，需要放在子线程处理
                        sensorManager.registerListener(mRcOriListener,
                            mAccelerometerSensor,
                            SAMPLING_PERIOD_US,
                            SensorManager.SENSOR_DELAY_NORMAL,
                            handler)
                        sensorManager.registerListener(mRcOriListener,
                            mMagneticSensor,
                            SAMPLING_PERIOD_US,
                            SensorManager.SENSOR_DELAY_NORMAL,
                            handler)
                        if (orientationEventListener.canDetectOrientation()) {
                            orientationEventListener.enable()
                        }
                    }
                }
            }
        }

        fun unRegisterSensorListener() {
            handler.post {
                sensorManager.unregisterListener(mRcOriListener)
                if (orientationEventListener.canDetectOrientation()) {
                    orientationEventListener.disable()
                }
            }
        }
    }

    private class HSICompassSensorEventListener(private val weakRefHSICompassProcesser: WeakReference<HSICompassProcesser>) : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent?) {
            weakRefHSICompassProcesser.get()?.onSensorChanged(event)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            weakRefHSICompassProcesser.get()?.onAccuracyChanged(sensor, accuracy)
        }
    }

    private class HSICompassSensorOrientationEventListener(
        private val weakRefHSICompassProcesser: WeakReference<HSICompassProcesser>,
        context: Context,
    ) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            weakRefHSICompassProcesser.get()?.updateScreenOrientation()
        }

    }

    companion object {
        private const val SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_NORMAL
        private const val TAG = "HSICompassProcesser"
    }
}