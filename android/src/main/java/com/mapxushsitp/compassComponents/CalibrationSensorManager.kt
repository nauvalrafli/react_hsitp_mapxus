package com.mapxushsitp.compassComponents

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RangeTracker(var min: Float = Float.MAX_VALUE, var max: Float = Float.MIN_VALUE) {
    fun update(value: Float) {
        if (value < min) min = value
        if (value > max) max = value
    }

    fun delta(): Float = max - min
}

class CalibrationSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
//    private var progressListener: ((Float) -> Unit)? = null
    private var progressListener: ((Float, List<String>) -> Unit)? = null
    private var rotationSamples = mutableListOf<FloatArray>()
    private var isCalibrated = false

    private var xRange = RangeTracker()
    private var yRange = RangeTracker()
    private var zRange = RangeTracker()


//    fun setProgressListener(listener: (Float) -> Unit) {
//        progressListener = listener
//    }

    fun setProgressListener(listener: (Float, List<String>) -> Unit) {
        progressListener = listener
    }

    fun startListening() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR && !isCalibrated) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            // Save samples
            rotationSamples.add(orientation.copyOf())

            // Track min/max for each axis
            val x = orientation[0]
            val y = orientation[1]
            val z = orientation[2]

            xRange.update(x)
            yRange.update(y)
            zRange.update(z)

            val xRangeValue = xRange.max - xRange.min
            val yRangeValue = yRange.max - yRange.min
            val zRangeValue = zRange.max - zRange.min

//            val progress = computeProgress(rotationSamples)
////            val lackingAxes = getLackingAxes(xRange, yRange, zRange)
//            val lackingAxes = getLackingAxes(xRangeValue, yRangeValue, zRangeValue)
//            progressListener?.invoke(progress, lackingAxes)
//
//            if (progress >= 1f) {
//                isCalibrated = true
//                stopListening()
//            }
            val (progress, lackingAxes) = computeProgress(rotationSamples)
            progressListener?.invoke(progress, lackingAxes)

            if (progress >= 0.95f) {
                isCalibrated = true
                stopListening()
            }

        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Original Code
//    private fun computeProgress(samples: List<FloatArray>): Float {
//        if (samples.isEmpty()) return 0f
//
//        // Calculate range for each axis
//        val xRange = samples.maxOf { it[0] } - samples.minOf { it[0] }
//        val yRange = samples.maxOf { it[1] } - samples.minOf { it[1] }
//        val zRange = samples.maxOf { it[2] } - samples.minOf { it[2] }
//
//        // Normalize ranges (assuming π radians max rotation)
//        val progressX = (xRange / Math.PI).coerceIn(0.0, 1.0)
//        val progressY = (yRange / Math.PI).coerceIn(0.0, 1.0)
//        val progressZ = (zRange / Math.PI).coerceIn(0.0, 1.0)
//
//        // Combine the 3 into one average progress
//        return ((progressX + progressY + progressZ) / 3f).toFloat()
//    }

    private fun computeProgress(samples: List<FloatArray>): Pair<Float, List<String>> {
        if (samples.isEmpty()) return 0f to listOf("x", "z")

        val xRange = samples.maxOf { it[0] } - samples.minOf { it[0] }
        val zRange = samples.maxOf { it[2] } - samples.minOf { it[2] }

        val progressX = (xRange / Math.PI).coerceIn(0.0, 1.0)
        val progressZ = (zRange / Math.PI).coerceIn(0.0, 1.0)

        val averageProgress = ((progressX + progressZ) / 2f).toFloat()

        val neededAxes = mutableListOf<String>()
        if (progressX < 0.9) neededAxes.add("x")
        if (progressZ < 0.9) neededAxes.add("z")

        return averageProgress to neededAxes
    }

    private fun getLackingAxes(xRange: Float, yRange: Float, zRange: Float): List<String> {
        val axes = mutableListOf<String>()
        if (xRange < Math.PI / 4) axes.add("X-axis")
        if (yRange < Math.PI / 4) axes.add("Y-axis")
        if (zRange < Math.PI / 4) axes.add("Z-axis")
        return axes
    }

}


