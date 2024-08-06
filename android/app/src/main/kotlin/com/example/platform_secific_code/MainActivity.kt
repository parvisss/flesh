package com.example.platform_secific_code

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlin.math.sqrt

class MainActivity : FlutterActivity(), SensorEventListener {
    private val CHANNEL = "com.example.flashlight_app/flashlight"
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private var flashlightOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "turnOnFlashlight") {
                try {
                    turnOnFlashlight()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Flashlight not available.", null)
                }
            } else if (call.method == "turnOffFlashlight") {
                try {
                    turnOffFlashlight()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Flashlight not available.", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun turnOnFlashlight() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId, true)
        flashlightOn = true
    }

    private fun turnOffFlashlight() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId, false)
        flashlightOn = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()

            if ((currentTime - lastUpdate) > 200) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                val speed = sqrt((x * x + y * y + z * z).toDouble()) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    if (flashlightOn) {
                        turnOffFlashlight()
                    } else {
                        turnOnFlashlight()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // We can ignore this for now
    }

    companion object {
        private const val SHAKE_THRESHOLD = 800 // Adjust threshold as needed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
