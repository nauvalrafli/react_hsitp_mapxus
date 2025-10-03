package com.mapxushsitp.motionSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.atan2
import kotlin.math.sqrt

class MotionSensorClass(
    private val context: Context,
    private val onTiltChanged: (Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isFacingUp = true
    private var lastSensorUpdateTime = 0L

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

//    override fun onSensorChanged(event: SensorEvent?) {
//        val z = event?.values?.get(2) ?: return
//        val facingUp = z > 0
//
//        if (facingUp != isFacingUp) {
//            isFacingUp = facingUp
//            onTiltChanged(facingUp)
//        }
//    }

    override fun onSensorChanged(event: SensorEvent?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSensorUpdateTime < 200) return
        lastSensorUpdateTime = currentTime

        event?.values?.let { accelerometerValues ->
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                getRotationVectorFromAccelerometer(accelerometerValues)
            )
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val pitch = Math.toDegrees(orientationAngles[1].toDouble())

            // Hide ARView if pitch is less than or equal to -80°
//            val isInHideRange = pitch in -20.0..0.0 // hide between -20.0 until 0.0
            val isInHideRange = pitch >= 0.0
            val facingUp = !isInHideRange

            if (facingUp != isFacingUp) {
                isFacingUp = facingUp
                onTiltChanged(facingUp)
            }

            Log.d("MotionSensor", "Pitch: %.2f°, isFacingUp: %b".format(pitch, isFacingUp))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun getRotationVectorFromAccelerometer(acc: FloatArray): FloatArray {
        val norm = sqrt(acc[0]*acc[0] + acc[1]*acc[1] + acc[2]*acc[2])
        return floatArrayOf(
            acc[0] / norm,
            acc[1] / norm,
            acc[2] / norm
        )
    }

}
