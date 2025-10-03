package com.mapxushsitp.gpsComponents

import android.content.Context
import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class GPSHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Get last known location (fast, but might be null if no location yet)
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(onResult: (lat: Double?, lon: Double?) -> Unit) {
        val locationTask: Task<Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                onResult(null, null)
            }
        }
        locationTask.addOnFailureListener {
            onResult(null, null)
        }
    }
}
