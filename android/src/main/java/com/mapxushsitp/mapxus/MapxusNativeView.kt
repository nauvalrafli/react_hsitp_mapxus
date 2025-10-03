package com.mapxushsitp.mapxus

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapxus.map.mapxusmap.impl.MapboxMapViewProvider
import com.mapxus.map.mapxusmap.api.services.RoutePlanning
import com.mapxus.map.mapxusmap.api.services.RoutePlanning.RoutePlanningResultListener
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxushsitp.view.HomeScreen
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.S)
class MapxusNativeView(context: Context) : FrameLayout(context) {
  private val label: TextView = TextView(context)

  @RequiresApi(Build.VERSION_CODES.S)
  val homeScreen = HomeScreen.imperativeComposable(context)
  val mapView = MapView(context)
  val mapOptions = MapxusMapOptions().apply {
    floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
    zoomLevel = 19.0
  }
  val mapViewProvider = MapboxMapViewProvider(context, mapView, mapOptions)
  val routePlanning = RoutePlanning.newInstance()

  init {
    setBackgroundColor(Color.parseColor("#0b1120"))
    val padding = (16 * resources.displayMetrics.density).toInt()
    setPadding(0, padding, 0, 0)
    addView(homeScreen, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
  }
}


