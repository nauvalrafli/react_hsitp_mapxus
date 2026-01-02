package com.mapxushsitp.compassComponents

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.mapxushsitp.motionSensor.MotionSensorClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth.asStateFlow()

    private var lastAzimuth = 0f

    // ⚙️ Dynamic Smoothing Config
    private val MIN_SMOOTHING = 0.05f // Very smooth for standing still
    private val MAX_SMOOTHING = 0.40f // Very snappy for fast turns
    private val THRESHOLD_FAST_TURN = 10f // Degrees of change to trigger "Fast Mode"

    val deviceHeadingInDegrees: Float
        get() = ((_azimuth.value % 360) + 360) % 360

    private val context = getApplication<Application>().applicationContext

    private val motionSensor = CompassClass(context) { _, azimuthDegrees ->

        // 1. Calculate the shortest distance between new and old angle
        var delta = azimuthDegrees - lastAzimuth
        while (delta < -180) delta += 360
        while (delta > 180) delta -= 360

        // 2. Determine how much to smooth based on the size of the delta
        // If delta is large (turning fast), use a higher factor (less smoothing)
        val absDelta = kotlin.math.abs(delta)
        val dynamicFactor = if (absDelta > THRESHOLD_FAST_TURN) {
            MAX_SMOOTHING
        } else {
            // Gradually transition between Min and Max for a premium feel
            MIN_SMOOTHING + (absDelta / THRESHOLD_FAST_TURN) * (MAX_SMOOTHING - MIN_SMOOTHING)
        }

        // 3. Apply the dynamic filter
        val smoothedAzimuth = lastAzimuth + (dynamicFactor * delta)

        lastAzimuth = smoothedAzimuth
        _azimuth.value = smoothedAzimuth
    }

    fun start() = motionSensor.start()
    fun stop() = motionSensor.stop()
}

//class CompassViewModel(application: Application) : AndroidViewModel(application) {
//    private val _isFacingUp = mutableStateOf(true)
//    val isFacingUp: State<Boolean> = _isFacingUp
//
//    private val _azimuth = MutableStateFlow(0f)
//    val azimuth: StateFlow<Float> = _azimuth
//
//    val deviceHeadingInDegrees: Float
//        get() = ((_azimuth.value % 360) + 360) % 360
//
////    val deviceHeadingInDegrees: Float
////        get() = ((_azimuth.value % 360) + 360) % 360   // Always 0..360
//
//    @SuppressLint("StaticFieldLeak")
//    private val context = getApplication<Application>().applicationContext
//
//    private val motionSensor = CompassClass(context) { facingUp, azimuthDegrees ->
//        _isFacingUp.value = facingUp
////        _azimuth.value = ((azimuthDegrees % 360) + 360) % 360
//        _azimuth.value = azimuthDegrees
//
//        Log.e("ARCompass", "compassViewModel: ${deviceHeadingInDegrees}")
//    }
//
//    fun start() = motionSensor.start()
//    fun stop() = motionSensor.stop()
//}
