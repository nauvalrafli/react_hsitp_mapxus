package com.mapxushsitp.compassComponents

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class CompassClass(
    private val context: Context,
    private val onUpdate: (facingUp: Boolean, azimuth: Float) -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager?.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
        }

        if (gravity.isNotEmpty() && geomagnetic.isNotEmpty()) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRadians = orientationAngles[0]
                val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
                val facingUp = gravity[2] > 0
                onUpdate(facingUp, azimuthDegrees)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    fun stop() {
        sensorManager?.unregisterListener(this)
    }
}
