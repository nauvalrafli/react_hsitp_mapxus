package com.mapxushsitp.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle

object Preference {
    private var instance : SharedPreferences? = null

    fun init(context: Context) {
        instance = context.getSharedPreferences("Mapxus", Context.MODE_PRIVATE)
    }

    fun editVehicle(vehicle: String) {
        instance?.edit {
            putString("vehicle", vehicle)
            commit()
        }
    }

    fun getVehicle() : String {
        return instance?.getString("vehicle", RoutePlanningVehicle.FOOT) ?: "car"
    }

    fun getOnboardingDone() : Boolean {
        return instance?.getBoolean("onboardingDone", false) ?: false
    }

    fun setOnboardingDone() {
        instance?.edit {
            putBoolean("onboardingDone", true)
            commit()
        }
    }

    fun setARWalkthroughDone() {
        instance?.edit {
            putBoolean("arWalkthroughDone", true)
            commit()
        }
    }

    fun getARWalkthroughDone() : Boolean {
        return instance?.getBoolean("arWalkthroughDone", false) ?: false
    }

    fun setGpsWalkthroughDone() {
        instance?.edit {
            putBoolean("gpsWalkthroughDone", true)
            commit()
        }
    }

    fun getGpsWalkthroughDone() : Boolean {
        return instance?.getBoolean("gpsWalkthroughDone", false) ?: false
    }

    fun setIsSpeaking(value: Boolean) {
        instance?.edit {
            putBoolean("isSpeaking", value)
            commit()
        }
    }

    fun getIsSpeaking() : Boolean {
        return instance?.getBoolean("isSpeaking", true) ?: false
    }
}
