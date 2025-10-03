package com.mapxushsitp.compassComponents

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompassViewModel(application: Application) : AndroidViewModel(application) {
    private val _isFacingUp = mutableStateOf(true)
    val isFacingUp: State<Boolean> = _isFacingUp

    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth

    val deviceHeadingInDegrees: Float
        get() = ((_azimuth.value % 360) + 360) % 360

//    val deviceHeadingInDegrees: Float
//        get() = ((_azimuth.value % 360) + 360) % 360   // Always 0..360

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val motionSensor = CompassClass(context) { facingUp, azimuthDegrees ->
        _isFacingUp.value = facingUp
//        _azimuth.value = ((azimuthDegrees % 360) + 360) % 360
        _azimuth.value = azimuthDegrees

        Log.e("ARCompass", "compassViewModel: ${deviceHeadingInDegrees}")
    }

    fun start() = motionSensor.start()
    fun stop() = motionSensor.stop()
}
