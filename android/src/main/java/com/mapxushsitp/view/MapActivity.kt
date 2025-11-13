package com.mapxushsitp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mapxushsitp.R
import com.mapxushsitp.data.static.floorList
import com.mapxushsitp.service.TextToVoice
import com.mapxushsitp.service.limitDecimal
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnMapClickedListener
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnMapxusPointAnnotationClickListener
import com.mapxus.map.mapxusmap.api.map.interfaces.OnMapxusMapReadyCallback
import com.mapxus.map.mapxusmap.api.map.model.IndoorBuilding
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxus.map.mapxusmap.api.map.model.MapxusPointAnnotationOptions
import com.mapxus.map.mapxusmap.api.map.model.MapxusSite
import com.mapxus.map.mapxusmap.api.map.model.Poi
import com.mapxus.map.mapxusmap.api.map.model.Venue
import com.mapxus.map.mapxusmap.api.map.model.overlay.MapxusPointAnnotation
import com.mapxus.map.mapxusmap.api.services.RoutePlanning
import com.mapxus.map.mapxusmap.api.services.RoutePlanning.RoutePlanningResultListener
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningLocale
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.planning.InstructionDto
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningQueryRequest
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningResult
import com.mapxus.map.mapxusmap.impl.MapLibreMapViewProvider
import com.mapxus.map.mapxusmap.overlay.model.RoutePainterResource
import com.mapxus.map.mapxusmap.overlay.navi.NavigationPathDto
import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.overlay.route.RoutePainter
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProviderListener
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.MapxusPositioningOption
import com.mapxus.positioning.positioning.api.PositioningMode
import com.mapxus.positioning.positioning.api.PositioningState
import org.maplibre.android.maps.MapView
import roundToNearestHalfString

class MapActivity : AppCompatActivity() {

    enum class MapState {
        Initial,
        ShowingDetail,
        SelectingLocation,
        CurrentLocation,
        ShowingRoute,
        Navigating
    }

    lateinit var detail: ScrollView
    lateinit var titleDetail: TextView
    lateinit var floorDetail: TextView
    lateinit var descriptionDetail: TextView
    lateinit var showRouteButton: Button
    lateinit var startNavigationButton: Button
    lateinit var locationButton: ImageView

    lateinit var navigationConfig: ScrollView
    lateinit var startLocation: TextView
    lateinit var stopLocation: TextView
    lateinit var showRouteNavigationButton: Button
    lateinit var startNavigationNavigationButton: Button

    lateinit var mapView : MapView
    lateinit var mapViewProvider: MapLibreMapViewProvider
    lateinit var mapOptions: MapxusMapOptions
    lateinit var routePlanning: RoutePlanning
    lateinit var routePainter: RoutePainter

    lateinit var cardInstruction : CardView
    lateinit var instructionContent : LinearLayout
    lateinit var btnPrev : ImageView
    lateinit var btnNext : ImageView
    lateinit var tvInstruction : TextView
    lateinit var tvBuilding : TextView

    lateinit var startIntent: Intent

    var routeAdsorber : RouteAdsorber? = null

    var instructionIndex = 0
        set(value) {
            field = value
            if(value <= instructionList.size - 1) {
                tvInstruction.text = instructionList[value].text
                tts.speakInstruction(instructionList[value].text, instructionList[value].distance.roundToNearestHalfString().toDouble())
                redrawRoute()
            }
            btnPrev.isEnabled = value > 0
            btnNext.isEnabled = value < instructionList.size
            tvBuilding.text = instructionList[value].distance.roundToNearestHalfString() + " m"
        }
    var instructionList = mutableListOf<InstructionDto>()

    var mapState : MapState = MapState.Initial
        set(value) {
            field = value
            when(value) {
                MapState.Initial -> {
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    navigationConfig.visibility = View.GONE
                    detail.visibility = View.GONE
                    startPoint = null
                    startLocation.text = "Please Select a Location"
                    routePainter.cleanRoute()
                    instructionList.clear()
                    cardInstruction.visibility = View.GONE
                }
                MapState.ShowingDetail -> {
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    navigationConfig.visibility = View.GONE
                    detail.visibility = View.VISIBLE
                    routePainter.cleanRoute()
                    instructionList.clear()
                    cardInstruction.visibility = View.GONE
                }
                MapState.SelectingLocation -> {
                    startLocation.setTextColor(resources.getColor(R.color.blue))
                    navigationConfig.visibility = View.VISIBLE
                    detail.visibility = View.GONE
                    routePainter.cleanRoute()
                    cardInstruction.visibility = View.GONE
                }
                MapState.CurrentLocation -> {
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    navigationConfig.visibility = View.VISIBLE
                    detail.visibility = View.GONE
                    routePainter.cleanRoute()
                    cardInstruction.visibility = View.GONE
                }
                MapState.ShowingRoute -> {
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    navigationConfig.visibility = View.VISIBLE
                    detail.visibility = View.GONE
                    cardInstruction.visibility = View.GONE
                }
                MapState.Navigating -> {
                    startLocation.setTextColor(resources.getColor(R.color.black))
                    navigationConfig.visibility = View.GONE
                    detail.visibility = View.GONE
                    cardInstruction.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

    var mapxusMap: MapxusMap? = null
    var startPoint : RoutePlanningPoint? = null
    var endPoint : RoutePlanningPoint? = null

    val mapxusPositioningListener = object: MapxusPositioningListener {
        override fun onStateChange(p0: PositioningState?) {

        }

        override fun onError(p0: ErrorInfo?) {
            Log.d("Cupcake", p0?.errorMessage ?: "")
        }

        override fun onOrientationChange(p0: Float, p1: Int) {

        }

        override fun onLocationChange(p0: MapxusLocation?) {
            if(mapState == MapState.CurrentLocation) {
                startPoint = RoutePlanningPoint(
                    p0?.longitude ?: 0.0,
                    p0?.latitude ?: 0.0,
                    p0?.mapxusFloor?.id ?: ""
                )
                startLocation.text = "(${floorList.firstOrNull { item -> item.id == startPoint?.floorId }?.code}) - ${startPoint?.lat?.limitDecimal()}...,${startPoint?.lon?.limitDecimal()}..."
                mapxusMap?.selectFloorById(p0?.mapxusFloor?.id ?: "")
            }
        }
    }

    lateinit var mapxusPositioningClient: MapxusPositioningClient
    lateinit var mapxusPositioningProvider: IndoorLocationProvider

    lateinit var tts : TextToVoice

//    var venueSheets : VenueSheets = VenueSheets()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)
//        venueSheets.show(supportFragmentManager, "")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToVoice.getInstance(this)

        mapOptions = MapxusMapOptions()
        if (intent.extras != null) {
            mapOptions.setFloorId(intent.getStringExtra("floorId") ?: "").setZoomLevel(19.0)
        } else {
            mapOptions.setFloorId("ad24bdcb0698422f8c8ab53ad6bb2665").setZoomLevel(19.0)
        }

        detail = findViewById(R.id.sv_detail)
        titleDetail = findViewById(R.id.tv_title)
        floorDetail = findViewById(R.id.tv_floor)
        descriptionDetail = findViewById(R.id.tv_description)
        showRouteButton = findViewById(R.id.btn_show_route)
        startNavigationButton = findViewById(R.id.btn_start_navigation)
        locationButton = findViewById(R.id.btn_location)

        navigationConfig = findViewById(R.id.sv_navigation_point)
        startLocation = findViewById(R.id.tv_start_location)
        stopLocation = findViewById(R.id.tv_stop_location)
        showRouteNavigationButton = findViewById(R.id.btn_show_route_navigation)
        startNavigationNavigationButton = findViewById(R.id.btn_start_navigation_navigation)

        cardInstruction = findViewById(R.id.card_instruction)
        instructionContent = findViewById(R.id.instruction_content)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        tvInstruction = findViewById(R.id.tv_instruction)
        tvBuilding = findViewById(R.id.tv_distance)


        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapViewProvider = MapLibreMapViewProvider(this, mapView, mapOptions)

        mapViewProvider.getMapxusMapAsync(onMapxusMapReadyCallback())

        routePlanning = RoutePlanning.newInstance()

        showRouteButton.setOnClickListener {
            detail.visibility = View.GONE
            drawRoute()
        }
        startNavigationButton.setOnClickListener {
            drawRoute()
            detail.visibility = View.GONE
        }
        showRouteNavigationButton.setOnClickListener {
            detail.visibility = View.GONE
            drawRoute()
        }
        startNavigationNavigationButton.setOnClickListener {
            drawRoute()
            detail.visibility = View.GONE
            navigationConfig.visibility = View.GONE
            mapState = MapState.Navigating
        }
        locationButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    123
                )
                mapState = MapState.CurrentLocation
                mapxusPositioningClient.start()
                mapxusPositioningProvider.start()
            } else {
                mapState = MapState.CurrentLocation
                mapxusPositioningProvider.start()
//                mapxusPositioningClient.start()
            }
        }

        btnNext.setOnClickListener {
            if(instructionList.size - 1 == instructionIndex) {
                mapState = MapState.Initial
            } else {
                instructionIndex++
            }
        }
        btnPrev.setOnClickListener {
            instructionIndex--
        }

        mapxusPositioningClient = MapxusPositioningClient.getInstance(this, this)
        val mapxusPositioningOption = MapxusPositioningOption()
        mapxusPositioningOption.positioningMode = PositioningMode.NORMAL
        mapxusPositioningClient.setPositioningOption(mapxusPositioningOption)
        mapxusPositioningClient.addPositioningListener(mapxusPositioningListener)
//        mapxusPositioningProvider = MapxusPositioningProvider(this, this)
        mapxusPositioningProvider.addListener(object: IndoorLocationProviderListener{
            override fun onCompassChanged(angle: Float, sensorAccuracy: Int) {

            }

            override fun onIndoorLocationChange(indoorLocation: IndoorLocation?) {
                if(mapState == MapState.CurrentLocation) {
                    startPoint = RoutePlanningPoint(
                        indoorLocation?.longitude ?: 0.0,
                        indoorLocation?.latitude ?: 0.0,
                        indoorLocation?.floor?.id ?: ""
                    )
                    startLocation.text = "(${floorList.firstOrNull { item -> item.id == startPoint?.floorId }?.code}) - ${startPoint?.lat?.limitDecimal()}...,${startPoint?.lon?.limitDecimal()}..."
                    mapxusMap?.selectFloorById(indoorLocation?.floor?.id ?: "")
                }
            }

            override fun onProviderError(errorInfo: com.mapxus.map.mapxusmap.positioning.ErrorInfo) {

            }

            override fun onProviderStarted() {

            }

            override fun onProviderStopped() {

            }

        })
    }

    fun drawRoute() {
        if(startPoint == null) {
            AlertDialog.Builder(this)
                .setTitle("Select Location")
                .setMessage("Please select a starting location first.")
                .setPositiveButton("OK", null)
                .show()
            mapState = MapState.SelectingLocation
            stopLocation.text = "(${floorList.first { item -> item.id == endPoint?.floorId }.code}) - ${endPoint?.lat?.limitDecimal()}...,${endPoint?.lon?.limitDecimal()}..."
            stopLocation.isEnabled = false;
            startLocation.setOnClickListener {
                if(mapState == MapState.SelectingLocation) mapState = MapState.Initial
                else {
                    mapState = MapState.SelectingLocation
                    startLocation.setTextColor(resources.getColor(R.color.blue))
                }
            }
            return
        }
        mapState = MapState.ShowingRoute
        val points = listOf(startPoint, endPoint)
        val request = RoutePlanningQueryRequest(points)
        request.vehicle = "foot"
        request.locale = RoutePlanningLocale.EN
        routePlanning.route(request)
        routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
            override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
                if(result == null) {
                    Toast.makeText(this@MapActivity, "Error: No route found", Toast.LENGTH_LONG).show()
                    return
                }
                if(result.status != 0) {
                    Toast.makeText(this@MapActivity, "Error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                    return
                }
                if(result.routeResponseDto == null) {
                    Toast.makeText(this@MapActivity, "Error: No route found", Toast.LENGTH_LONG).show()
                    return
                }
                instructionList.clear()
                routeAdsorber = RouteAdsorber(NavigationPathDto(result.routeResponseDto.paths.get(0)), 1.0, 5)
                instructionList.addAll(result.routeResponseDto.paths.get(0).instructions)
                instructionIndex = 0
                routePainter.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints)
                routePainter.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true));
            }
        })
    }

    fun redrawRoute() {
        startPoint = RoutePlanningPoint(
            lat = instructionList[instructionIndex].indoorPoints[0].lat,
            lon = instructionList[instructionIndex].indoorPoints[0].lon,
            floorId = instructionList[instructionIndex].indoorPoints[0].floorId
        )
        mapState = MapState.Navigating
        val points = listOf(startPoint, endPoint)
        val request = RoutePlanningQueryRequest(points)
        request.vehicle = RoutePlanningVehicle.FOOT
        request.locale = RoutePlanningLocale.EN
        routePlanning.route(request)
        routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
            override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
                if(result == null) {
                    Toast.makeText(this@MapActivity, "Error: No route found", Toast.LENGTH_LONG).show()
                    return
                }
                if(result.status != 0) {
                    Toast.makeText(this@MapActivity, "Error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                    return
                }
                if(result.routeResponseDto == null) {
                    Toast.makeText(this@MapActivity, "Error: No route found", Toast.LENGTH_LONG).show()
                    return
                }
                routePainter.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                routePainter.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true));
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapxusPositioningClient.removePositioningListener(mapxusPositioningListener)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    fun showPointDetails(
        title: String,
        floor: String,
        description: String
    ) {
        detail.visibility = View.VISIBLE
        titleDetail.text = title
        floorDetail.text = floor
        descriptionDetail.text = description
        if(description.isEmpty()) descriptionDetail.visibility = View.GONE
        else descriptionDetail.visibility = View.VISIBLE

        if(mapState == MapState.Navigating) {

            showRouteButton.visibility = View.GONE
            startNavigationButton.visibility = View.GONE
        } else {
            routePainter.cleanRoute()
            showRouteButton.visibility = View.VISIBLE
            startNavigationButton.visibility = View.VISIBLE
        }
    }

    fun hidePointDetails() {
        detail.visibility = View.GONE
    }

    fun onMapxusMapReadyCallback(): OnMapxusMapReadyCallback {
        return object : OnMapxusMapReadyCallback {
            override fun onMapxusMapReady(mapxusMap: MapxusMap?) {
                this@MapActivity.mapxusMap = mapxusMap

                mapxusMap?.setLocationProvider(mapxusPositioningProvider)

                if(intent.extras != null) {
                    val lat = intent.getDoubleExtra("lat", 0.0)
                    val lon = intent.getDoubleExtra("lon", 0.0)
                    val floorId = intent.getStringExtra("floorId")
                    val marker = MapxusPointAnnotationOptions()
                    marker.setFloorId(floorId)
                    marker.position = LatLng(lat, lon)
                    mapxusMap?.addMapxusPointAnnotation(marker)
                }

                mapView.getMapAsync { mapboxMap ->
                    routePainter = RoutePainter(this@MapActivity, mapboxMap, mapxusMap)
                }

                mapxusMap?.addOnMapClickedListener(object: OnMapClickedListener {
                    override fun onMapClick(
                        p0: LatLng,
                        p1: MapxusSite
                    ) {
                        if(mapState == MapState.Navigating) {
                            return;
                        }
                        if(mapState == MapState.SelectingLocation) {
                            startPoint = RoutePlanningPoint(
                                lat = p0.latitude,
                                lon = p0.longitude,
                                floorId = p1.floor?.id
                            )
                            val marker = MapxusPointAnnotationOptions()
                            marker.position = LatLng(p0.latitude, p0.longitude)
                            mapxusMap.addMapxusPointAnnotation(
                                marker
                            )
                            startLocation.text = "(${floorList.first { item -> item.id == startPoint?.floorId }.code}) - ${startPoint?.lat?.limitDecimal()}...,${startPoint?.lon?.limitDecimal()}..."
                        } else {
                            endPoint = RoutePlanningPoint(
                                lat = p0.latitude,
                                lon = p0.longitude,
                                floorId = p1.floor?.id
                            )
                            if(mapxusMap.mapxusPointAnnotations.isEmpty()) {
                                val pointOption = MapxusPointAnnotationOptions()
                                pointOption.position = LatLng(LatLng(p0.latitude ?: 0.0, p0.longitude ?: 0.0))
                                pointOption.setFloorId(p1.floor?.id)
                                mapxusMap.addMapxusPointAnnotation(pointOption)
                            } else {
                                mapxusMap.removeMapxusPointAnnotations()
                                hidePointDetails()
                            }
                        }
                    }
                })

                mapxusMap?.addOnIndoorPoiClickListener(object: MapxusMap.OnIndoorPoiClickListener {
                    override fun onIndoorPoiClick(poi: Poi?) {
                        if(mapState == MapState.Navigating) {
                            return;
                        }
                        if(mapState == MapState.SelectingLocation) {
                            startPoint = RoutePlanningPoint(
                                lat = poi?.latitude ?: 0.0,
                                lon = poi?.longitude ?: 0.0,
                                floorId = poi?.floor ?: ""
                            )
                            val marker = MapxusPointAnnotationOptions()
                            marker.position = LatLng(poi?.latitude ?: 0.0, poi?.longitude ?: 0.0)
                            mapxusMap.addMapxusPointAnnotation(
                                marker
                            )
                            startLocation.text = "(${floorList.first { item -> item.id == startPoint?.floorId }.code}) - ${startPoint?.lat?.limitDecimal()}...,${startPoint?.lon?.limitDecimal()}..."
                        } else {
                            if(mapxusMap.mapxusPointAnnotations.isNotEmpty()) {
                                mapxusMap.removeMapxusPointAnnotations()
                            }
                            startPoint = null
                            startLocation.text = "Please Select a Location"
                            endPoint = RoutePlanningPoint(
                                lat = poi?.latitude ?: 0.0,
                                lon = poi?.longitude ?: 0.0,
                                floorId = poi?.floor ?: ""
                            )
                            val pointOption = MapxusPointAnnotationOptions()
                            pointOption.position = LatLng(LatLng(poi?.latitude ?: 0.0, poi?.longitude ?: 0.0))
                            pointOption.setFloorId(poi?.floor)
                            mapxusMap.addMapxusPointAnnotation(pointOption)
                            showPointDetails(
                                title = poi?.nameMap?.en ?: "",
                                floor = poi?.floorName ?: "",
                                description = poi?.accessibilityDetailMap?.en ?: ""
                            )
                        }
                    }
                })

                mapxusMap?.addOnMapxusPointAnnotationClickListener(object: OnMapxusPointAnnotationClickListener {
                    override fun OnMapxusPointAnnotationClick(p0: MapxusPointAnnotation?) {
                        mapxusMap.removeMapxusPointAnnotation(p0)
                    }
                })
            }
        }
    }
}
