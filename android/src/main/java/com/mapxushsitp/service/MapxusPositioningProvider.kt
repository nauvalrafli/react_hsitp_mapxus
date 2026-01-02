package com.mapxushsitp.service

import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class MapxusPositioningProvider() : IndoorLocationProvider() {
    private var started = false
    var isInHeadingMode: Boolean = true
    private var routeAdsorber: RouteAdsorber? = null

    private val coroutineScope: CoroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    override fun supportsFloor(): Boolean {
        return true
    }

    fun setRouteAdsorbers(adsorber: RouteAdsorber?) {
        routeAdsorber = adsorber
    }

    //ignore
    override fun start() {
//        positioningClient?.addPositioningListener(mapxusPositioningListener)
//        positioningClient?.start()
//        started = true
    }

    //ignore
    override fun stop() {
//        if (positioningClient != null) {
//            positioningClient!!.stop()
////            positioningClient.removePositioningListener(mapxusPositioningListener);
//        }
//        started = false
    }

    //ignore
    override fun isStarted(): Boolean {
        return started
    }

    companion object {
        private val TAG: String = MapxusPositioningProvider::class.java.simpleName
    }
}
