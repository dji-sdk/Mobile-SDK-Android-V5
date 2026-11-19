package dji.sampleV5.aircraft

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import kotlin.math.*

class TargetHudActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvLockedEasting: TextView
    private lateinit var tvLockedNorthing: TextView
    private lateinit var tvLockedHeight: TextView
    private lateinit var tvShotCoords: TextView
    private lateinit var tvCorrectionResult: TextView
    private lateinit var tvLiveTarget: TextView

    private var currentEasting = 0
    private var currentNorthing = 0
    private var currentHeight = 0
    private var gimbalPitch = 0
    private var droneHeading = 0
    private var droneBat = 0
    private var rcBat = 0
    private var isMicOn = true

    private var lockedEasting: Int? = null
    private var lockedNorthing: Int? = null
    private var shotCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_hud)

        initViews()
        setupVideoStream()
        listenToTelemetry()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tv_connection_status)
        tvDistance = findViewById(R.id.tv_distance)
        tvBattery = findViewById(R.id.tv_battery)
        tvLockedEasting = findViewById(R.id.tv_locked_easting)
        tvLockedNorthing = findViewById(R.id.tv_locked_northing)
        tvLockedHeight = findViewById(R.id.tv_locked_height)
        tvShotCoords = findViewById(R.id.tv_shot_coords)
        tvCorrectionResult = findViewById(R.id.tv_correction_result)
        tvLiveTarget = findViewById(R.id.tv_live_target)

        val btnMic = findViewById<Button>(R.id.btn_mic)
        btnMic.setOnClickListener {
            isMicOn = !isMicOn
            btnMic.text = if (isMicOn) "MIC: ON" else "MIC: OFF"
            btnMic.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isMicOn) 0xFF2E7D32.toInt() else 0xFF616161.toInt()
            )
        }

        findViewById<Button>(R.id.btn_lock_target).setOnClickListener {
            lockedEasting = currentEasting
            lockedNorthing = currentNorthing
            tvLockedEasting.text = "Easting  : $lockedEasting"
            tvLockedNorthing.text = "Northing: $lockedNorthing"
            tvLockedHeight.text = "Height   : ${currentHeight} m (${gimbalPitch}°)"
            Toast.makeText(this, "TARGET LOCKED", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_record_shot).setOnClickListener {
            if (lockedEasting == null || lockedNorthing == null) {
                Toast.makeText(this, "पहले LOCK TARGET दबाएं!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shotCounter++
            val diffE = currentEasting - lockedEasting!!
            val diffN = currentNorthing - lockedNorthing!!

            val lrText = if (diffE >= 0) "LEFT ${abs(diffE)}" else "RIGHT ${abs(diffE)}"
            val adText = if (diffN >= 0) "ADD ${abs(diffN)}" else "DROP ${abs(diffN)}"

            tvShotCoords.text = "S$shotCounter E:$currentEasting N:$currentNorthing"
            tvCorrectionResult.text = "$lrText | $adText"
        }

        findViewById<Button>(R.id.btn_rth).setOnClickListener {
            val rthKey = KeyTools.createKey(FlightControllerKey.KeyStartGoHome)
            KeyManager.getInstance().performAction(rthKey, null)
            Toast.makeText(this, "RTH Activated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupVideoStream() {
        val surfaceView = findViewById<SurfaceView>(R.id.video_surface_view)
        val streamManager = MediaDataCenter.getInstance().cameraStreamManager

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                streamManager.putCameraStreamSurface(
                    ComponentIndexType.LEFT_OR_MAIN,
                    holder.surface,
                    surfaceView.width,
                    surfaceView.height,
                    ICameraStreamManager.ScaleType.CENTER_CROP
                )
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                streamManager.removeCameraStreamSurface(holder.surface)
            }
        })
    }

    private fun listenToTelemetry() {
        KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent), this) { _, v ->
            v?.let { droneBat = it; updateTopBar() }
        }
        KeyManager.getInstance().listen(KeyTools.createKey(RemoteControllerKey.KeyBatteryInfo), this) { _, info ->
            info?.let { rcBat = it.batteryPercent; updateTopBar() }
        }
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyCompassHeading), this) { _, heading ->
            heading?.let {
                droneHeading = ((it.toInt() + 360) % 360)
                updateTopBar()
            }
        }
        KeyManager.getInstance().listen(KeyTools.createKey(GimbalKey.KeyGimbalAttitude), this) { _, att ->
            att?.let { gimbalPitch = it.pitch.toInt() }
        }
        KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.KeyLaserMeasureInformation), this) { _, laserInfo ->
            laserInfo?.let {
                val dist = it.distance.toInt()
                val utm = latLonToUTM(it.location3D.latitude, it.location3D.longitude)
                currentEasting = utm.first
                currentNorthing = utm.second
                currentHeight = it.location3D.altitude.toInt()

                runOnUiThread {
                    tvDistance.text = "DIST: ${dist}m"
                    tvLiveTarget.text = "TGT E: $currentEasting | N: $currentNorthing | H: ${currentHeight}m (${gimbalPitch}°)"
                }
            }
        }
    }

    private fun updateTopBar() {
        runOnUiThread {
            val formattedHeading = String.format("%03d", droneHeading)
            tvStatus.text = "EDS | DRONE: CONNECTED | OT: ${formattedHeading}°"
            tvBattery.text = "DRONE: ${droneBat}%  RC: ${rcBat}%"
        }
    }

    private fun latLonToUTM(lat: Double, lon: Double): Pair<Int, Int> {
        val zone = floor((lon + 180.0) / 6.0).toInt() + 1
        val a = 6378137.0
        val f = 1 / 298.257223563
        val k0 = 0.9996
        val e = sqrt(f * (2 - f))
        val e2 = e * e
        val ep2 = e2 / (1 - e2)

        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val lonOrigin = (zone - 1) * 6.0 - 180.0 + 3.0
        val lonOriginRad = Math.toRadians(lonOrigin)

        val N = a / sqrt(1 - e2 * sin(latRad).pow(2))
        val T = tan(latRad).pow(2)
        val C = ep2 * cos(latRad).pow(2)
        val A = cos(latRad) * (lonRad - lonOriginRad)

        val M = a * ((1 - e2 / 4 - 3 * e2.pow(2) / 64 - 5 * e2.pow(3) / 256) * latRad
                - (3 * e2 / 8 + 3 * e2.pow(2) / 32 + 45 * e2.pow(3) / 1024) * sin(2 * latRad)
                + (15 * e2.pow(2) / 256 + 45 * e2.pow(3) / 1024) * sin(4 * latRad)
                - (35 * e2.pow(3) / 3072) * sin(6 * latRad))

        val easting = (k0 * N * (A + (1 - T + C) * A.pow(3) / 6
                + (5 - 18 * T + T.pow(2) + 72 * C - 58 * ep2) * A.pow(5) / 120) + 500000.0).toInt()

        var northing = (k0 * (M + N * tan(latRad) * (A.pow(2) / 2
                + (5 - T + 9 * C + 4 * C.pow(2)) * A.pow(4) / 24
                + (61 - 58 * T + T.pow(2) + 600 * C - 330 * ep2) * A.pow(6) / 720))).toInt()

        if (lat < 0) northing += 10000000

        return Pair(easting, northing)
    }
}
