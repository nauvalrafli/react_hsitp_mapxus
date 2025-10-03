package com.mapxushsitp.motionSensor

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class MotionSensorViewModel(application: Application) : AndroidViewModel(application) {
    private val _isFacingUp = mutableStateOf(true)
    val isFacingUp: State<Boolean> = _isFacingUp

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val motionSensor = MotionSensorClass(context) { facingUp ->
        _isFacingUp.value = facingUp
    }

    fun start() {
        motionSensor.start()
    }

    fun stop() {
        motionSensor.stop()
    }
}
