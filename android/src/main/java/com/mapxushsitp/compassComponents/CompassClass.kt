package com.mapxushsitp.compassComponents

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class CompassClass(
    private val context: Context,
    private val onUpdate: (facingUp: Boolean, azimuth: Float) -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // 🎯 Use TYPE_ROTATION_VECTOR for the smoothest, non-shaking experience.
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d("ARCompass", "Rotation Vector sensor started")
        } else {
            Log.e("ARCompass", "Rotation Vector sensor not available on this device")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // 1. Safety check
        if (event == null) return

        // 2. Logic belongs HERE, where 'event' is defined
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

            // Convert the rotation vector to a rotation matrix
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Get the orientation (Azimuth, Pitch, Roll) from the matrix
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert Azimuth from Radians to Degrees
            val azimuthRadians = orientationAngles[0]
            val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()

            // For Rotation Vector, we can assume facingUp is true or calculate
            // it from the matrix if needed, but usually true is fine for AR.
            onUpdate(true, azimuthDegrees)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stop() {
        sensorManager?.unregisterListener(this)
    }
}
