package com.mapxushsitp.service

import android.app.Activity
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TextToVoice(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun speakInstruction(direction: String, distance: Double) {
        if (isReady && direction.isNotBlank()) {
            var text = "$direction"
            if(distance != 0.0) text += " for ${distance.toMeterText(Locale.getDefault())}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun speakArrival() {
        if (isReady) {
            tts?.speak("You have arrived at your destination", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        isReady = false
    }

    companion object {
        @Volatile
        private var INSTANCE: TextToVoice? = null

        fun getInstance(context: Activity): TextToVoice {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TextToVoice(context).also { INSTANCE = it }
            }
        }
    }
}
