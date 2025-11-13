package com.mapxus.mapxusmapandroiddemo.examples.indoorpositioning

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.PositioningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs

class MapxusPositioningProvider(
    private val positioningClient: MapxusPositioningClient
) : IndoorLocationProvider() {
    private var started = false
    var isInHeadingMode: Boolean = false
    private var routeAdsorber: RouteAdsorber? = null

    private val coroutineScope: CoroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    override fun supportsFloor(): Boolean {
        return true
    }

    fun setRouteAdsorbers(adsorber: RouteAdsorber?) {
        routeAdsorber = adsorber
    }

    override fun start() {
//        positioningClient?.addPositioningListener(mapxusPositioningListener)
        positioningClient?.start()
        started = true
    }

    override fun stop() {
        if (positioningClient != null) {
            positioningClient!!.stop()
//            positioningClient.removePositioningListener(mapxusPositioningListener);
        }
        started = false
    }

    override fun isStarted(): Boolean {
        return started
    }


    private val mapxusPositioningListener: MapxusPositioningListener =
        object : MapxusPositioningListener {
            override fun onStateChange(positionerState: PositioningState) {
                when (positionerState) {
                    PositioningState.STOPPED -> {
                        dispatchOnProviderStopped()
                    }

                    PositioningState.RUNNING -> {
                        dispatchOnProviderStarted()
                    }

                    else -> {}
                }
            }

            override fun onError(errorInfo: ErrorInfo) {
                Log.e(TAG, errorInfo.errorMessage)
                dispatchOnProviderError(
                    com.mapxus.map.mapxusmap.positioning.ErrorInfo(
                        errorInfo.errorCode,
                        errorInfo.errorMessage
                    )
                )
            }

            override fun onOrientationChange(orientation: Float, sensorAccuracy: Int) {
                if (isInHeadingMode) {
                    if (abs((orientation - lastCompass).toDouble()) > 10) {
                        dispatchCompassChange(orientation, sensorAccuracy)
                    }
                } else {
                    dispatchCompassChange(orientation, sensorAccuracy)
                }
            }

            override fun onLocationChange(mapxusLocation: MapxusLocation) {
                val location = Location("MapxusPositioning")
                coroutineScope.launch {
                    location.latitude = mapxusLocation.latitude
                    location.longitude = mapxusLocation.longitude
                    location.time = System.currentTimeMillis()
                    val building = mapxusLocation.buildingId
                    val floorInfo = if (mapxusLocation.mapxusFloor == null) null else FloorInfo(
                        mapxusLocation.mapxusFloor.id,
                        mapxusLocation.mapxusFloor.code,
                        mapxusLocation.mapxusFloor.ordinal
                    )

                    val indoorLocation = IndoorLocation(building, floorInfo, location)
                    indoorLocation.accuracy = mapxusLocation.accuracy

                    if(routeAdsorber != null) {
                        val newLocation = routeAdsorber?.calculateTheAdsorptionLocation(indoorLocation)
                        if(indoorLocation.latitude != newLocation?.latitude || indoorLocation.longitude != newLocation?.longitude) {
                            indoorLocation.latitude = newLocation?.latitude ?: 0.0
                            indoorLocation.longitude = newLocation?.longitude ?: 0.0
                        }
                    }
                }
            }
        }

    companion object {
        private val TAG: String = MapxusPositioningProvider::class.java.simpleName
    }
}