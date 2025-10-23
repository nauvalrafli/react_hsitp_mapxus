package com.mapxushsitp.view.component.mapxus_compose

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.UTurnRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnMapClickedListener
import com.mapxus.map.mapxusmap.api.map.interfaces.OnMapxusMapReadyCallback
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxus.map.mapxusmap.api.map.model.MapxusSite
import com.mapxus.map.mapxusmap.api.map.model.SelectorPosition
import com.mapxus.map.mapxusmap.api.services.RoutePlanning
import com.mapxus.map.mapxusmap.api.services.RoutePlanning.RoutePlanningResultListener
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningLocale
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.planning.InstructionDto
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningQueryRequest
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningResult
import com.mapxus.map.mapxusmap.impl.MapboxMapViewProvider
import com.mapxus.map.mapxusmap.overlay.model.RoutePainterResource
import com.mapxus.map.mapxusmap.overlay.navi.NavigationPathDto
import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.overlay.navi.RouteShortener
import com.mapxus.map.mapxusmap.overlay.route.RoutePainter
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProviderListener
import com.mapxus.mapxusmapandroiddemo.examples.indoorpositioning.MapxusPositioningProvider
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.PositioningState
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.model.MapPoi
import com.mapxushsitp.data.model.SerializableNavigationInstruction
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.mapxushsitp.data.model.Venue
import com.mapxushsitp.data.static.venues
import com.mapxushsitp.service.generateSpeakText
import com.mapxushsitp.service.toMeterText
import com.mapxushsitp.view.sheets.VenueDetails
import com.mapxushsitp.view.sheets.VenueScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MapxusController(
  val context: Context,
  val lifecycleOwner: LifecycleOwner,
  val locale: Locale,
  private val navController: NavController,
  val arNavigationViewModel: ARNavigationViewModel
) : OnMapxusMapReadyCallback {
    val mapView = MapView(context)
    val mapOptions = MapxusMapOptions().apply {
        floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
        zoomLevel = 19.0
    }
    val mapViewProvider = MapboxMapViewProvider(context, mapView, mapOptions)
    val routePlanning = RoutePlanning.newInstance()

    val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    val mapxusPositioningProvider : MapxusPositioningProvider = MapxusPositioningProvider(lifecycleOwner, context)

    var routePainter : RoutePainter? = null
    val isCurrentLocation = mutableStateOf(false)

    private var mapxusMap : MapxusMap? = null
    var mapboxMap : MapboxMap? = null

    var selectedPoi: MapPoi? = null
        set(value) {
            if (value != null) {
                selectedFloor = value.floorId
            }
            field = value
        }
    var selectedVenue by mutableStateOf<Venue?>(null)
    var selectedFloor by mutableStateOf<String?>(null)

    var startingPoint by mutableStateOf<RoutePlanningPoint?>(null)
    var currentLocation = RoutePlanningPoint(0.0,0.0)
    var destinationPoint : RoutePlanningPoint? = null
    var lastGpsTime = 0L

    var instructionList = mutableListOf<InstructionDto>()
    var instructionIndex = mutableStateOf(0)
    var showSheet = mutableStateOf(true)
    var selectingCenter = mutableStateOf(false)

    var titleNavigationStep = mutableStateOf("")
    var distanceNavigationStep = mutableStateOf("")
    var timeEstimation = mutableStateOf("")
    var distance = mutableStateOf(0.0)
    var icon = mutableStateOf<ImageVector?>(null)

    var isSensorUnreliable = mutableStateOf(false)
    var isFirst = mutableStateOf(false)

    var routeAdsorber : RouteAdsorber? = null
    var routeShortener: RouteShortener? = null
    var isNavigating = false
    var isSpeaking = mutableStateOf(true)

    var selectedVehicle : String = RoutePlanningVehicle.FOOT
    var isLoading = mutableStateOf(false)

    var isFloorSelectorShown = mutableStateOf(false)
    var userCurrentFloor = mutableStateOf<String?>(null)

    lateinit var tts: TextToSpeech

    val sharedPreferences = context.getSharedPreferences("Mapxus", Context.MODE_PRIVATE)

    var arStartPoint : SerializableRoutePoint? = null
    var arEndPoint : SerializableRoutePoint? = null
    var arInstructionPoints = mutableListOf<SerializableRoutePoint>()
    var arInstructionNavigationList = mutableListOf<SerializableNavigationInstruction>()

    var checkStartLocation = true
    val accuracyAdsorptionInput = mutableStateOf("5")

    init {
        val speakingConfig = sharedPreferences.getBoolean("isSpeaking", true)
        isFirst.value = sharedPreferences.getBoolean("isFirst", true)
//        accuracyAdsorptionInput.value = sharedPreferences.getInt("adsorption", 3).toString()
        isSpeaking.value = speakingConfig
        tts = TextToSpeech(context) { }
        Handler(Looper.getMainLooper()).postDelayed(Runnable({
            tts.setLanguage(if(locale.language.contains("zh")) locale else Locale("en-US"))
        }), 1000)
        mapxusPositioningProvider.updatePositioningListener(object : MapxusPositioningListener {
            override fun onStateChange(positionerState: PositioningState) {
                when (positionerState) {
                    PositioningState.STOPPED -> {
                        mapxusPositioningProvider.dispatchOnProviderStopped()
                    }

                    PositioningState.RUNNING -> {
                        mapxusPositioningProvider.dispatchOnProviderStarted()
                    }

                    else -> {}
                }
            }

            override fun onError(errorInfo: ErrorInfo) {
                Log.e("ERROR: ", errorInfo.errorMessage)

                if (errorInfo.errorMessage.contains("Out of Mapxus indoor service", true) ||
                    errorInfo.errorMessage.contains("Current provider disabled", true)) {

                    // 1. Stop indoor provider
//                    mapxusPositioningProvider.dispatchOnProviderStopped()

                    // 2. Switch to outdoor GPS
//                    switchToOutdoor()
                } else {
                    // Default error propagation
                    mapxusPositioningProvider.dispatchOnProviderError(
                        com.mapxus.map.mapxusmap.positioning.ErrorInfo(
                            errorInfo.errorCode,
                            errorInfo.errorMessage
                        )
                    )
                }
            }

            override fun onOrientationChange(orientation: Float, sensorAccuracy: Int) {
                if (mapxusPositioningProvider.isInHeadingMode) {
                    if (abs((orientation - mapxusPositioningProvider.lastCompass).toDouble()) > 10) {
                        mapxusPositioningProvider.dispatchCompassChange(orientation, sensorAccuracy)
                    }
                } else {
                    mapxusPositioningProvider.dispatchCompassChange(orientation, sensorAccuracy)
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

                    if(checkStartLocation) {
                        mapxusMap?.selectFloorById(indoorLocation.floor?.id ?: "")
                        userCurrentFloor.value = indoorLocation.floor?.id
                        isFloorSelectorShown.value = true
                        checkStartLocation = false
                    }

                    if(mapxusLocation.mapxusFloor != null) {
                        mapxusPositioningProvider.updateFloor(mapxusLocation.mapxusFloor)
                    }

                    if(routeAdsorber != null && isNavigating) {
                        val newLocation =
                            routeAdsorber?.calculateTheAdsorptionLocation(indoorLocation)
                                ?: indoorLocation
                        if (indoorLocation.latitude != newLocation?.latitude || indoorLocation.longitude != newLocation?.longitude) {
                            indoorLocation.latitude = newLocation?.latitude ?: 0.0
                            indoorLocation.longitude = newLocation?.longitude ?: 0.0
                            currentLocation = RoutePlanningPoint(
                                newLocation?.longitude ?: 0.0,
                                newLocation?.latitude ?: 0.0,
                                newLocation?.floor?.id
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if(indoorLocation.time > lastGpsTime) {
                                lastGpsTime = indoorLocation.time
                                mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                            }
                            if (routeShortener != null) {
                                routeShortener?.cutFromTheLocationProjection(newLocation, mapboxMap)
                            }
                        }
                    } else {
                        currentLocation = RoutePlanningPoint(
                            indoorLocation?.longitude ?: 0.0,
                            indoorLocation?.latitude ?: 0.0,
                            indoorLocation?.floor?.id
                        )
                        withContext(Dispatchers.Main) {
                            if(indoorLocation.time > lastGpsTime) {
                                lastGpsTime = indoorLocation.time
                                mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                            }
                        }
                    }
                }
            }
        })
        mapViewProvider.getMapxusMapAsync(this)
        mapxusPositioningProvider.start()
        mapxusMap?.setLocationEnabled(true)
        mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
    }

    fun getMapxusMap(): MapxusMap {
        if (mapxusMap != null) {
            return mapxusMap!!
        }

        var result: MapxusMap? = null
        val latch = CountDownLatch(1)

        mapViewProvider.getMapxusMapAsync {
            result = it
            latch.countDown()
        }

        latch.await() // blocks current thread until latch is released
        return result!!
    }

    fun showRoute() {
        if(isLoading.value) return
        try {
            isLoading.value = true
            if(isCurrentLocation.value) {
                startingPoint = currentLocation
            }
            if(startingPoint == null) {
                throw Error()
            }
            val points = listOf(startingPoint, destinationPoint)
            val request = RoutePlanningQueryRequest()
            request.points = points
            request.vehicle = selectedVehicle
            request.locale = getRoutePlanningLocale()

            routePlanning.route(request)
            routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
                    try {
                        if(result == null) {
                            Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                            return
                        }
                        if(result.status != 0) {
                            Log.d("Location", "Error: ${result}")
                            Toast.makeText(context, "Error: ${result}", Toast.LENGTH_SHORT).show()
                            return
                        }
                        if(result.routeResponseDto == null) {
                            Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                            return
                        }
                        instructionList.clear()
                        instructionList.addAll(result.routeResponseDto.paths.get(0).instructions)
                        instructionIndex.value = 0
                        routePainter?.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                        routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(context.resources.getColor(R.color.indoor_line, null)));
                        titleNavigationStep.value = instructionList[instructionIndex.value].text ?: ""
                        distanceNavigationStep.value = instructionList[instructionIndex.value].distance.toMeterText(locale)
                        distance.value = instructionList[instructionIndex.value].distance
                        isLoading.value = false
                    } catch(e: Error) {
                        isLoading.value = false
                        Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading.value = false
                    }
                }
            })
        } catch (e: Error) {
            isLoading.value = false
            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
        } finally {
            isLoading.value = false
        }
    }

    fun drawRoute() {
        Log.d("DrawRoute", "${isLoading.value}")
        if(isLoading.value) return
        isLoading.value = true
        isNavigating = true
        mapxusMap?.mapxusUiSettings?.setSelectorCollapse(true)
        try {
            if(isCurrentLocation.value) {
                startingPoint = currentLocation
                isCurrentLocation.value = false
            }
            if(startingPoint == null || destinationPoint == null) {
                throw Error()
            }
            instructionIndex.value = 0
            val points = listOf(startingPoint!!, destinationPoint!!)
            val request = RoutePlanningQueryRequest()
            request.points = points
            request.vehicle = selectedVehicle
            request.locale = getRoutePlanningLocale()
            routePlanning.route(request)
            routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
                    try {
                        if(result == null) {
                            Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                            return
                        }
                        if(result.status != 0) {
                            Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            return
                        }
                        if(result.routeResponseDto == null) {
                            Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                            return
                        }
                        instructionList.clear()

                        arInstructionPoints.clear()
                        arInstructionNavigationList.clear()

                        val pathDto = result.routeResponseDto.paths.get(0)

//                        instructionList.addAll(pathDto.instructions)

                        // New by me for Coordinates
//                        pathDto.points.coordinates.forEachIndexed { index, doubles ->
//                            Log.d("ARCoreDebugCoordinates", "coordinates: ${doubles.size}")
//                        }

//                        pathDto.points.coordinates.forEachIndexed { index, coord ->
//                            if (coord.size >= 2) {
//                                val longitude = coord[0]
//                                val latitude = coord[1]
//                                val floorIndex = if (coord.size >= 3) coord[2] else null
//
//                                Log.d("ARCoreDebugCoordinates", "Point $index: lat=$latitude, lon=$longitude, floor=$floorIndex")
//                            } else {
//                                Log.w("ARCoreDebugCoordinates", "Point $index has invalid coordinate size: ${coord.size}")
//                            }
//                        }

                        pathDto.points.coordinates.forEachIndexed { index, coord ->
                            if (coord.size >= 2 && index < pathDto.points.coordinates.size - 1) {
                                val (lon1, lat1) = coord
                                val (lon2, lat2) = pathDto.points.coordinates[index + 1]
                                val heading = calculateHeading(lat1, lon1, lat2, lon2)
                                Log.d("ARCoreDebugHeading", "Heading from point $index to ${index + 1} = $heading°")
                            }
                        }



                        pathDto.instructions.forEachIndexed { index, instruction ->
                            // For backup from me - AR Coordinates
//                            instruction.indoorPoints.firstOrNull()?.let { point ->
//                                arInstructionPoints.add(
//                                    SerializableRoutePoint(
//                                        lat = point.lat,
//                                        lon = point.lon,
//                                        heading = 0.0,
//                                        floorId = point.floorId ?: ""
//                                    )
//                                )
//                                Log.e("ARCoreDebug", "Instruction $index → (${point.lat}, ${point.lon})")
//                            }

                            instruction.indoorPoints.firstOrNull()?.let { point ->
                                // Calculate heading to the next point if available
                                val heading = if (index < pathDto.instructions.size - 1) {
                                    val nextPoint = pathDto.instructions[index + 1].indoorPoints.firstOrNull()
                                    if (nextPoint != null) {
                                        calculateHeading(point.lat, point.lon, nextPoint.lat, nextPoint.lon)
                                    } else {
                                        0.0
                                    }
                                } else {
                                    0.0
                                }

                                // for backup from me - version 1
//                                arInstructionPoints.add(
//                                    SerializableRoutePoint(
//                                        lat = point.lat,
//                                        lon = point.lon,
//                                        heading = (heading + 360) % 360,
//                                        floorId = point.floorId ?: ""
//                                    )
//                                )

                                // for backup from me - version 2
                                arInstructionPoints.add(
                                    SerializableRoutePoint(
                                        lat = point.lat,
                                        lon = point.lon,
                                        heading = (heading + 360) % 360,
                                        floorId = point.floorId ?: ""
                                    )
                                )

                                // for backup from me - version 3
//                                arInstructionPoints.add(
//                                    SerializableRoutePoint(
//                                        lat = point.lat,
//                                        lon = point.lon,
//                                        heading = (instruction.heading + 360) % 360,
//                                        floorId = point.floorId ?: ""
//                                    )
//                                )

                                arInstructionPoints.forEachIndexed { index, p ->
                                    Log.w("ARCoreHeading", "Instruction Point with new Heading $index: lat: ${p.lat}, lon: ${p.lon}, heading: ${p.heading}, headingCompass: ${bearingToDirection(p.heading)}")
                                }

                                Log.e("ARCoreHeading", "heading main: $heading, compass: ${bearingToDirection(heading)}")
                                Log.d("ARCoreHeadingMapxusMap", "instruction heading: ${instruction.heading}, compass: ${bearingToDirection(instruction.heading)}")
                            }

                            instruction.floorId.let { floorId ->
                                arInstructionNavigationList.add(
                                    SerializableNavigationInstruction(
                                        instruction = instruction.text,
                                        distance = instruction.distance,
                                        floorId = floorId ?: ""
                                    )
                                )
                            }
                            isLoading.value = false
                        }

                        arEndPoint = arInstructionPoints.lastOrNull()

                        if((accuracyAdsorptionInput.value.toIntOrNull() ?: 0) > 0) {
                            routeAdsorber = RouteAdsorber(NavigationPathDto(pathDto), accuracyAdsorptionInput.value.toDouble(), 5)
                        } else {
                            routeAdsorber = null
                            mapxusPositioningProvider.setRouteAdsorbers(null)
                        }
                        mapboxMap?.cameraPosition = CameraPosition.Builder().bearing(pathDto.instructions[0].heading).zoom(19.0).build()
                        routeShortener = RouteShortener(NavigationPathDto(pathDto), pathDto, pathDto.indoorPoints)
                        routeShortener?.setOnPathChangeListener(object: RouteShortener.OnPathChangeListener {
                            override fun onPathChange(pathDto: com.mapxus.map.mapxusmap.api.services.model.planning.PathDto?) {
                                if(pathDto != null) {
                                    val pathCount = pathDto.instructions.size
                                    if(pathDto.instructions[0].distance <= 2 && pathDto.instructions.size > 1) {
                                        titleNavigationStep.value = pathDto.instructions[1].text ?: ""
                                        distanceNavigationStep.value = pathDto.instructions[1].distance.toMeterText(locale)
                                        distance.value = pathDto.instructions[1].distance
                                        icon.value = getStepIcon(pathDto.instructions[1].sign)
                                    } else {
                                        titleNavigationStep.value = pathDto.instructions[0].text ?: ""
                                        distanceNavigationStep.value = pathDto.instructions[0].distance.toMeterText(locale)
                                        distance.value = pathDto.instructions[0].distance
                                        icon.value = getStepIcon(pathDto.instructions[0].sign)
                                    }
                                    if(pathCount < instructionList.size && instructionIndex.value != (instructionList.size - pathCount) && instructionList.size - pathCount > 0) {
                                        instructionIndex.value = instructionList.size - pathCount
                                        redrawRoute()
//                                        mapboxMap?.cameraPosition = CameraPosition.Builder().bearing(pathDto.instructions[0].heading).build()
                                        mapboxMap?.animateCamera(CameraUpdateFactory.bearingTo(pathDto.instructions[0].heading))
                                        if(instructionIndex.value == instructionList.size - 1) {
                                            showSheet.value = true
                                            navController.navigate(VenueScreen.routeName)
                                            routeShortener = null
                                            routeAdsorber = null
                                            mapxusPositioningProvider.setRouteAdsorbers(null)
                                            isNavigating = false
                                        }
                                    }
                                    val totalDistanceMeters = pathDto.instructions.map { it -> it.distance }.reduce { a, b -> a + b }
                                    val estimatedSeconds = (totalDistanceMeters/1.2).roundToInt()
                                    if(estimatedSeconds > 60)
                                        timeEstimation.value = context.resources.getString(R.string.minute, (estimatedSeconds/60).toInt())
                                    else
                                        timeEstimation.value = context.resources.getString(R.string.second, estimatedSeconds)
                                    if(estimatedSeconds < 3 && instructionList.size > 0) endNavigation() // end navigation when distance to destination is less than 3 seconds
                                }
                            }
                        })
                        if((accuracyAdsorptionInput.value.toIntOrNull() ?: 0) > 0) {
                            mapxusPositioningProvider.setRouteAdsorbers(routeAdsorber)
                        } else {
                            routeAdsorber = null
                            mapxusPositioningProvider.setRouteAdsorbers(null)
                        }
                        icon.value = Icons.Default.Straight
                        instructionList.addAll(result.routeResponseDto.paths.get(0).instructions)
                        instructionIndex.value = 0
                        routePainter?.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                        routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(context.resources.getColor(R.color.indoor_line, null)));
                        if(mapxusMap?.selectedFloor?.id != instructionList[instructionIndex.value].floorId)
                            mapxusMap?.selectFloorById(instructionList[instructionIndex.value].floorId ?: "")
                        updateNavigationText()
                        val words = generateSpeakText(titleNavigationStep.value, instructionList[instructionIndex.value].distance, locale)
                        if(isSpeaking.value) tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, null)
                        showSheet.value = false

                        Log.e("ARCoreHeading", "Navigation Start Point: $arStartPoint")
                        Log.e("ARCoreDebug", "Navigation Start Point: $arStartPoint")
                        Log.e("ARCoreDebug", "Navigation End Point: $arEndPoint")

                        arInstructionPoints.forEachIndexed { index, p ->
                            Log.e("ARCoreDebug", "Navigation Point $index: lat: ${p.lat}, Lon: ${p.lon}, heading: ${p.heading}, headingCompass: ${bearingToDirection(p.heading)}")
                            Log.e("ArrowPlaced", "Navigation Point $index: ${p.lat}, ${p.lon}")
                        }
                        arInstructionNavigationList.forEachIndexed { index, p ->
                            Log.e("ARCoreDebug", "Instruction $index: ${p.instruction}, ${p.distance}, ${p.floorId}")
                        }
                    } catch (e: Error) {
                        isLoading.value = false
                        Log.d("DrawRoute", "${isLoading.value} $e")
                        Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading.value = false
                    }
                }
            })
        } catch (e: Error) {
            isLoading.value = false
            isNavigating = false
            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
        } finally {
            isLoading.value = false
        }
        isCurrentLocation.value = false
    }

    private fun redrawRoute() {
        if(isLoading.value) return
        isLoading.value = true
        try {
            if(startingPoint == null) {
                throw Error()
            }
            val startPoint = RoutePlanningPoint(
                lat = instructionList[instructionIndex.value].indoorPoints[0].lat,
                lon = instructionList[instructionIndex.value].indoorPoints[0].lon,
                floorId = instructionList[instructionIndex.value].indoorPoints[0].floorId
            )
            val points = listOf(startPoint, destinationPoint)
            val request = RoutePlanningQueryRequest(points)
            request.vehicle = selectedVehicle
            request.locale = getRoutePlanningLocale()
            routePlanning.route(request)
            routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
                    if(result == null) {
                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                        return
                    }
                    if(result.status != 0) {
                        Toast.makeText(context, "Error: ${result}", Toast.LENGTH_SHORT).show()
                        Log.d("Location", "Error: ${result}")
                        return
                    }
                    if(result.routeResponseDto == null) {
                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
                        return
                    }
                    if((accuracyAdsorptionInput.value.toIntOrNull() ?: 0) > 0) {
                        routeAdsorber = RouteAdsorber(
                            NavigationPathDto(result.routeResponseDto.paths.get(0)),
                            accuracyAdsorptionInput.value.toDouble(),
                            5
                        )
                    } else {
                        routeAdsorber = null
                        mapxusPositioningProvider.setRouteAdsorbers(null)
                    }
                    mapxusPositioningProvider.setRouteAdsorbers(routeAdsorber)
                    routePainter?.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                    routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(context.resources.getColor(R.color.indoor_line, null)));
                    if(instructionIndex.value < instructionList.size - 1) {
                        if(mapxusMap?.selectedFloor?.id != instructionList[instructionIndex.value].floorId)
                            mapxusMap?.selectFloorById(instructionList[instructionIndex.value].floorId ?: "")
                        updateNavigationText()
                        val words = generateSpeakText(titleNavigationStep.value, instructionList[instructionIndex.value].distance, locale)
                        if(isSpeaking.value) tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, null)
                    } else if(instructionIndex.value >= instructionList.size - 1) {
                        instructionIndex.value = instructionList.size - 1
                    } else {
                        instructionIndex.value = 0
                    }
                    icon.value = getStepIcon(instructionList[instructionIndex.value].sign)
                    mapboxMap?.animateCamera(CameraUpdateFactory.bearingTo(result.routeResponseDto.paths[0].instructions[0].heading))
//                    mapboxMap?.cameraPosition = CameraPosition.Builder().bearing(result.routeResponseDto.paths[0].instructions[0].heading).zoom(19.0).build()
                    isLoading.value = false
                }
            })
        } catch (e: Error) {
            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
            isLoading.value = false
        } finally {
            isLoading.value = false
        }
    }

    fun nextStep() {
        if (instructionList.isEmpty()) {
            Log.w("Navigation", "Instruction list is empty, cannot go to next step.")
            return
        }

        val current = instructionIndex.value
        val totalInstruction = instructionList.size
        val isLastInstruction = instructionIndex.value >= instructionList.lastIndex

        if (current == totalInstruction - 2) {
            instructionIndex.value += 1
            redrawRoute()
            arNavigationViewModel.isShowingAndClosingARNavigation.value = false
            icon.value = getStepIcon(instructionList[instructionIndex.value].sign)

            Log.e("ArriveAtDestination", "Arrive at destination - 2")
        } else if (isLastInstruction) {
            endNavigation()
        } else {
            instructionIndex.value += 1
            redrawRoute()
        }
    }

    fun previousStep() {
        if(instructionIndex.value > 0) {
            instructionIndex.value -= 1;
            arNavigationViewModel.removeAlignedIndex(instructionIndex.value)
            icon.value = getStepIcon(instructionList[instructionIndex.value].sign)
            redrawRoute()
        }
    }

    // for backup from me - original with mofication
    fun setStartWithCenter(destinationLat: Double, destinationLon: Double) {
        isCurrentLocation.value = false
        val point = mapboxMap?.cameraPosition?.target
        startingPoint = RoutePlanningPoint(point?.longitude ?: 0.0, point?.latitude ?: 0.0, mapxusMap?.selectedFloor?.id)

        val calculateHeading = startingPoint?.let {
            calculateHeading(it.lat, it.lon, destinationLat, destinationLon)
        }

        arStartPoint = SerializableRoutePoint(point?.longitude ?: 0.0, point?.latitude ?: 0.0, heading = calculateHeading ?: 0.0,mapxusMap?.selectedFloor?.id ?: "")
        selectingCenter.value = false
        Log.e("ARCoreHeading", "heading: ${arStartPoint}")
    }

    fun updateNavigationText() {
        titleNavigationStep.value = instructionList[instructionIndex.value].text ?: ""
        distanceNavigationStep.value = instructionList[instructionIndex.value].distance.toMeterText(locale)
        distance.value = instructionList[instructionIndex.value].distance
        val totalDistanceMeters = instructionList.subList(instructionIndex.value, instructionList.size - 1).map { it -> it.distance }.reduce { a, b -> a + b }
        val estimatedSeconds = (totalDistanceMeters/1.2).roundToInt()
        if(estimatedSeconds > 60)
            timeEstimation.value = context.resources.getString(R.string.minute, (estimatedSeconds/60).toInt())
        else
            timeEstimation.value = context.resources.getString(R.string.second, estimatedSeconds)
    }

    fun getStepIcon(sign: Int): ImageVector {
        return when (sign) {
            -98 -> Icons.Default.UTurnLeft // U_TURN_UNKNOWN
            -8  -> Icons.Default.UTurnLeft // U_TURN_LEFT
            -7  -> Icons.Default.TurnLeft // KEEP_LEFT
            -3  -> Icons.Default.TurnSharpLeft // TURN_SHARP_LEFT
            -2  -> Icons.Default.TurnLeft // TURN_LEFT
            -1  -> Icons.Default.TurnSlightLeft // TURN_SLIGHT_LEFT
            0   -> Icons.Default.Straight // CONTINUE_ON_STREET
            1   -> Icons.Default.TurnSlightRight // TURN_SLIGHT_RIGHT
            2   -> Icons.Default.TurnRight // TURN_RIGHT
            3   -> Icons.Default.TurnSharpRight // TURN_SHARP_RIGHT
            4   -> Icons.Default.Flag // FINISH
            5   -> Icons.Default.Flag // REACHED_VIA
            6   -> Icons.Default.ChangeCircle // USE_ROUNDABOUT
            7   -> Icons.Default.TurnRight // KEEP_RIGHT
            8   -> Icons.Default.UTurnRight // U_TURN_RIGHT
            100 -> Icons.Default.KeyboardArrowUp // UP (elevator up)
            -100 -> Icons.Default.KeyboardArrowDown // DOWN (elevator down)
            200 -> Icons.Default.DoorFront // CROSS_DOOR
            -200 -> Icons.Default.MeetingRoom // CROSS_ROOM_DOOR
            else -> Icons.Default.Help // fallback for unexpected signs
        }
    }


    fun getManeuverIcon(instructions: List<InstructionDto>, index: Int): ImageVector {
        if (index == 0) {
            return Icons.Default.Straight
        }

        val prevHeading = instructions[index - 1].heading
        val currHeading = instructions[index].heading

        // delta in range [-180, 180]
        var delta = currHeading - prevHeading
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        return when {
            delta in -20.0..20.0 -> Icons.Default.Straight

            delta in 21.0..45.0 -> Icons.Default.TurnSlightRight
            delta in 46.0..135.0 -> Icons.Default.TurnRight
            delta > 135.0 -> Icons.Default.UTurnRight

            delta in -45.0..-21.0 -> Icons.Default.TurnSlightLeft
            delta in -135.0..-46.0 -> Icons.Default.TurnLeft
            delta < -135.0 -> Icons.Default.UTurnLeft

            else -> Icons.Default.Help
        }
    }



//    fun setStartWithCenter() {
//        val point = mapboxMap?.cameraPosition?.target
//
//        val startLat = point?.latitude ?: 0.0
//        val startLon = point?.longitude ?: 0.0
//
//        val fromPoint = RoutePlanningPoint(startLat, startLon, mapxusMap?.selectedFloor?.id)
//        val destination = RoutePlanningPoint()
//        val heading = calculateFirstHeading(fromPoint, destination)
//
//        startingPoint = fromPoint
//        arStartPoint = SerializableRoutePoint(
//            lat = startLat,
//            lon = startLon,
//            heading = heading,
//            floorId = mapxusMap?.selectedFloor?.id ?: ""
//        )
//
//        selectingCenter.value = false
//    }


    fun endNavigation() {
        instructionList.clear()
        arInstructionPoints.clear()
        arInstructionNavigationList.clear()

        startingPoint = null
        destinationPoint = null

        routePainter?.cleanRoute()
        useDefaultDrawableBearingIcon()
        getMapxusMap()?.removeMapxusPointAnnotations()
        getMapxusMap()?.selectFloorById(getMapxusMap()?.selectedFloor?.id ?: "")
        routePlanning.destroy()

        mapxusMap?.mapxusUiSettings?.setSelectorCollapse(false)

        navController.navigate(VenueScreen.routeName)
        arNavigationViewModel.isShowingOpeningAndClosingARButton.value = false
        arNavigationViewModel.isShowingAndClosingARNavigation.value = false
        arNavigationViewModel.isSelectingGPSCurrentLocation.value = false
        routeShortener = null
        routeAdsorber = null
        mapxusPositioningProvider.setRouteAdsorbers(null)
        isNavigating = false
        showSheet.value = true
        isLoading.value = false
        arNavigationViewModel.resetNavigationState()
    }

    fun calculateHeading(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private fun calculateFirstHeading(from: RoutePlanningPoint, to: RoutePlanningPoint): Double {
        val fromLat = Math.toRadians(from.lat)
        val fromLng = Math.toRadians(from.lon)
        val toLat = Math.toRadians(to.lat)
        val toLng = Math.toRadians(to.lon)

        val dLng = toLng - fromLng
        val y = sin(dLng) * cos(toLat)
        val x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng)
        val heading = Math.toDegrees(atan2(y, x))

        return (heading + 360) % 360 // normalize to 0–360°
    }

    fun getRoutePlanningLocale() : String {
        return if(locale.language.contains("zh") && locale.country == "TW")
            RoutePlanningLocale.ZH_TW
        else if(locale.language.contains("zh") && locale.country == "HK")
            RoutePlanningLocale.ZH_HK
        else if(locale.language.contains("zh"))
            RoutePlanningLocale.ZH_CN
        else  RoutePlanningLocale.EN
    }

    fun useDefaultDrawableBearingIcon() {
        coroutineScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                mapboxMap?.locationComponent?.applyStyle(
                    LocationComponentOptions.builder(context)
                        .bearingDrawable(R.drawable.user_location_2)
                        .build()
                )
                mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
            }
        }
    }

    fun bearingToDirection(bearing: Double, use16: Boolean = false): String {
        // 8 directions (full words)
        val dirs8 = arrayOf(
            "North",
            "North East",
            "East",
            "South East",
            "South",
            "South West",
            "West",
            "North West"
        )

        // 16 directions (full words)
        val dirs16 = arrayOf(
            "North",
            "North North East",
            "North East",
            "East North East",
            "East",
            "East South East",
            "South East",
            "South South East",
            "South",
            "South South West",
            "South West",
            "West South West",
            "West",
            "West North West",
            "North West",
            "North North West"
        )

        val dirs = if (use16) dirs16 else dirs8

        // ✅ Normalize bearing to [0, 360)
        val normalizedBearing = ((bearing % 360) + 360) % 360

        val segmentSize = 360.0 / dirs.size
        val index = ((normalizedBearing + segmentSize / 2) / segmentSize).toInt() % dirs.size

        return dirs[index]
    }

    @SuppressLint("MissingPermission")
    private fun switchToOutdoor() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val request: LocationRequest =
            LocationRequest.create().apply {
                interval = 2000L
                fastestInterval = 1000L
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

        fusedClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { gpsLoc ->
                    val location = IndoorLocation(
                        null, // no building
                        null, // no floor
                        Location("OutdoorGPS").apply {
                            latitude = gpsLoc.latitude
                            longitude = gpsLoc.longitude
                            accuracy = gpsLoc.accuracy
                            time = System.currentTimeMillis()
                        }
                    )

                    coroutineScope.launch {
                        if(routeAdsorber != null && isNavigating) {
                            val newLocation =
                                routeAdsorber?.calculateTheAdsorptionLocation(location)
                                    ?: location
                            if (location.latitude != newLocation?.latitude || location.longitude != newLocation?.longitude) {
                                location.latitude = newLocation?.latitude ?: 0.0
                                location.longitude = newLocation?.longitude ?: 0.0
                                currentLocation = RoutePlanningPoint(
                                    newLocation?.longitude ?: 0.0,
                                    newLocation?.latitude ?: 0.0,
                                    null
                                )
                            }
                            withContext(Dispatchers.Main) {
                                if(location.time > lastGpsTime) {
                                    lastGpsTime = location.time
                                    mapxusPositioningProvider.dispatchIndoorLocationChange(location)
                                }
                                if (routeShortener != null) {
                                    routeShortener?.cutFromTheLocationProjection(newLocation, mapboxMap)
                                }
                            }
                        } else {
                            currentLocation = RoutePlanningPoint(
                                location?.longitude ?: 0.0,
                                location?.latitude ?: 0.0,
                                null
                            )
                            withContext(Dispatchers.Main) {
                                if(location.time > lastGpsTime) {
                                    lastGpsTime = location.time
                                    mapxusPositioningProvider.dispatchIndoorLocationChange(location)
                                }
                            }
                        }


                    }
                }
            }
        }, Looper.getMainLooper())
    }

    override fun onMapxusMapReady(p0: MapxusMap?) {
      Log.d("Location", "Mapxus is ready")
      mapxusMap = p0
      mapxusMap?.mapxusUiSettings?.isSelectorEnabled = false
      mapxusMap?.mapxusUiSettings?.isBuildingSelectorEnabled = false
      mapxusMap?.mapxusUiSettings?.setSelectorPosition(SelectorPosition.TOP_LEFT)
      mapxusMap?.mapxusUiSettings?.setSelectFontColor(Color.White.hashCode())
      mapxusMap?.mapxusUiSettings?.setSelectBoxColor(Color(0xFF4285F4).hashCode())
      mapViewProvider.setLanguage(locale.language)

      mapxusMap?.addOnMapClickedListener(object: OnMapClickedListener {
        override fun onMapClick(
          p0: LatLng,
          p1: MapxusSite
        ) {
          if(mapxusMap?.selectedVenueId != null) {
            selectedVenue = venues.find { it.venueId == p1.venue?.id }
            if(navController.currentDestination?.route == VenueScreen.routeName || selectedVenue?.venueId != p1.venue?.id) {
              selectedPoi = null
              navController.navigate(VenueDetails.routeName)
            }
            isFloorSelectorShown.value = true
          } else {
            isFloorSelectorShown.value = false
          }
        }

      })
      mapView.getMapAsync(object: OnMapReadyCallback {
        override fun onMapReady(mMap: MapboxMap) {
          Log.d("Location", "MapboxMap is ready")
          routePainter = RoutePainter(context, mMap, p0)
          coroutineScope.launch {
            delay(3000)
            withContext(Dispatchers.Main) {
//                        useDefaultDrawableBearingIcon()
              mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
            }
          }
          mMap.setMinZoomPreference(18.0)
          mapboxMap = mMap
        }
      })
      mapxusPositioningProvider.addListener(object: IndoorLocationProviderListener {
        override fun onCompassChanged(angle: Float, sensorAccuracy: Int) {

        }

        override fun onIndoorLocationChange(indoorLocation: IndoorLocation?) {
          Log.d("Location", "Indoor Location Change")
          currentLocation = RoutePlanningPoint(
            indoorLocation?.longitude ?: 0.0,
            indoorLocation?.latitude ?: 0.0,
            indoorLocation?.floor?.id
          )
          if(indoorLocation?.floor?.id != null) {
            userCurrentFloor.value = indoorLocation?.floor?.id
          }
          useDefaultDrawableBearingIcon()
        }

        override fun onProviderError(errorInfo: com.mapxus.map.mapxusmap.positioning.ErrorInfo) {
          Log.d("Location", "Provider Error: ${errorInfo.errorMessage}")
        }

        override fun onProviderStarted() {
          Log.d("Location", "Started")
        }

        override fun onProviderStopped() {
          Log.d("Location", "Stopped")
        }

      })
      mapxusPositioningProvider.start()
      mapxusMap?.setLocationEnabled(true)
      mapxusMap?.setLocationProvider(mapxusPositioningProvider)
    }


}

// For backup from me
//class MainMapxusController(
//    context: Context,
//    lifecycleOwner: LifecycleOwner,
//    val navigationController: NavHostController,
//    val arNavigationViewModel: ARNavigationViewModel
//) {
//    val context = context
//    val mapView = MapView(context)
//    val mapOptions = MapxusMapOptions().apply {
//        floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
//        zoomLevel = 19.0
//    }
//    val mapViewProvider = MapboxMapViewProvider(context, mapView, mapOptions)
//    val routePlanning = RoutePlanning.newInstance()
//    val mapxusPositioningClient = MapxusPositioningClient.getInstance(lifecycleOwner, context)
////    val mapxusPositioningProvider = MapxusPositioningProvider(lifecycleOwner, context)
//    val mapxusPositioningProvider : MapxusPositioningProvider = MapxusPositioningProvider(lifecycleOwner, context, mapxusPositioningClient)
//
//    var routePainter : RoutePainter? = null
//    val isCurrentLocation = mutableStateOf(false)
//
//    private val job = Job()
//    val coroutineScope = CoroutineScope(Dispatchers.Main + job)
//
//    private var mapxusMap : MapxusMap? = null
//    var mapboxMap : MapboxMap? = null
//
//    var selectedVenue : Venue? = null
//    var selectedFloor : Venue.Floor? = null
//    var selectedPoi: MapPoi? = null
//
//    var startingPoint : RoutePlanningPoint? = null
//    var destinationPoint : RoutePlanningPoint? = null
//
//    var instructionList = mutableListOf<InstructionDto>()
//    var instructionIndex = mutableStateOf(0)
//    var showSheet = mutableStateOf(true)
//    var selectingCenter = mutableStateOf(false)
//
//    var titleNavigationStep = mutableStateOf("")
//    var distanceNavigationStep = mutableStateOf("")
//    var distance = mutableStateOf(0.0)
//    var currentLocation = RoutePlanningPoint(0.0,0.0)
//
//    val tts = TextToSpeech(context) { }
//
//    var arStartPoint : SerializableRoutePoint? = null
//    var arEndPoint : SerializableRoutePoint? = null
//    var arInstructionPoints = mutableListOf<SerializableRoutePoint>()
//    var arInstructionNavigationList = mutableListOf<SerializableNavigationInstruction>()
//
//    init {
//        mapViewProvider.getMapxusMapAsync(object : OnMapxusMapReadyCallback {
//            override fun onMapxusMapReady(p0: MapxusMap?) {
//                mapxusMap = p0
//                mapView.getMapAsync(object: OnMapReadyCallback {
//                    override fun onMapReady(mMap: MapboxMap) {
//                        routePainter = RoutePainter(context, mMap, p0)
//                        mapboxMap = mMap
//                    }
//                })
//            }
//        })
//        mapxusPositioningClient.addPositioningListener(object: MapxusPositioningListener {
//            override fun onStateChange(p0: PositioningState?) {
//
//            }
//
//            override fun onError(p0: ErrorInfo?) {
//
//            }
//
//            override fun onOrientationChange(p0: Float, p1: Int) {
//
//            }
//
//            override fun onLocationChange(p0: MapxusLocation?) {
//                currentLocation = RoutePlanningPoint(
//                    p0?.longitude ?: 0.0,
//                    p0?.latitude ?: 0.0,
//                    p0?.mapxusFloor?.id
//                )
//                if (isCurrentLocation.value) {
//                    startingPoint = currentLocation
//                }
//            }
//
//        })
//        mapxusPositioningClient.start()
//        mapxusMap?.setLocationEnabled(true)
//    }
//
//    fun getMapxusMap(): MapxusMap {
//        if (mapxusMap != null) {
//            return mapxusMap!!
//        }
//
//        var result: MapxusMap? = null
//        val latch = java.util.concurrent.CountDownLatch(1)
//
//        mapViewProvider.getMapxusMapAsync {
//            result = it
//            latch.countDown()
//        }
//
//        latch.await() // blocks current thread until latch is released
//        return result!!
//    }
//
//    fun showRoute() {
//        try {
////            if(startingPoint == null) {
////                throw e
////            }
//            val points = listOf(startingPoint!!, destinationPoint!!)
//            val request = RoutePlanningQueryRequest()
//            request.points = points
//            request.vehicle = "foot"
//            request.locale = RoutePlanningLocale.EN
//            routePlanning.route(request)
//            routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
//                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
//                    if(result == null) {
//                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
//                        return
//                    }
//                    if(result.status != 0) {
//                        Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
//                        return
//                    }
//                    if(result.routeResponseDto == null) {
//                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
//                        return
//                    }
//                    instructionList.clear()
//                    instructionList.addAll(result.routeResponseDto.paths.get(0).instructions)
//                    instructionIndex.value = 0
//                    routePainter?.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints)
//                    routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true));
//                    titleNavigationStep.value = instructionList[instructionIndex.value].text ?: ""
//                    distanceNavigationStep.value = "${instructionList[instructionIndex.value].distance.roundToNearestHalfString()} m"
//                    distance.value = instructionList[instructionIndex.value].distance
//                    tts.speak(if (instructionIndex.value == instructionList.size) "Kudos!. You have arrived at the Destination!." else "${titleNavigationStep.value}. And follow the steps for ${distance.value.toMeterText()}", TextToSpeech.QUEUE_FLUSH, null)
//                }
//            })
//        } catch (e: Error) {
//            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    fun drawRoute() {
//        try {
//            if (startingPoint == null || destinationPoint == null) {
//                Toast.makeText(context, "Please select both starting and destination points.", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            val start = startingPoint
//            val end = destinationPoint
//            val points = listOf(start, end)
//
//            val request = RoutePlanningQueryRequest().apply {
//                this.points = points
//                this.vehicle = "foot"
//                this.locale = RoutePlanningLocale.EN
//            }
//
//            // Set listener BEFORE triggering route planning
//            routePlanning.setRoutePlanningListener(object : RoutePlanningResultListener {
//                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
//                    if (result == null || result.status != 0 || result.routeResponseDto == null) {
//                        val errorMessage = result?.errorMessage ?: "No route found"
//                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
//                        return
//                    }
//
//                    val path = result.routeResponseDto.paths.getOrNull(0)
//                    if (path == null || path.instructions.isEmpty()) {
//                        Toast.makeText(context, "Error: Route instructions missing", Toast.LENGTH_LONG).show()
//                        return
//                    }
//
//                    // ✅ Clear and repopulate instruction and point lists
//                    instructionList.clear()
//                    arInstructionPoints.clear()
//                    arInstructionNavigationList.clear()
//
//                    instructionList.addAll(path.instructions)
//
//                    path.instructions.forEachIndexed { index, instruction ->
//                        instruction.indoorPoints.firstOrNull()?.let { point ->
//                            arInstructionPoints.add(
//                                SerializableRoutePoint(
//                                    lat = point.lat,
//                                    lon = point.lon,
//                                    floorId = point.floorId ?: ""
//                                )
//                            )
//                            Log.e("ARCoreDebug", "Instruction $index → (${point.lat}, ${point.lon})")
//                        }
//
//                        instruction.floorId?.let { floorId ->
//                            arInstructionNavigationList.add(
//                                SerializableNavigationInstruction(
//                                    instruction = instruction.text,
//                                    distance = instruction.distance,
//                                    floorId = floorId
//                                )
//                            )
//                        }
//                    }
//
//                    arEndPoint = arInstructionPoints.lastOrNull()
//
//                    instructionIndex.value = 0
//                    routePainter?.apply {
//                        paintRouteUsingResult(path, path.indoorPoints)
//                        setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true))
//                    }
//
//                    val currentInstruction = instructionList.getOrNull(instructionIndex.value)
//                    if (currentInstruction != null) {
//                        titleNavigationStep.value = currentInstruction.text ?: ""
//                        distanceNavigationStep.value = "${currentInstruction.distance.roundToNearestHalfString()} m"
//                        distance.value = currentInstruction.distance
//
//                        val isLastInstruction = instructionIndex.value == instructionList.lastIndex
//                        val message = if (isLastInstruction) {
//                            "Kudos! You have arrived at the Destination!"
//                        } else {
//                            "${titleNavigationStep.value}. And follow the steps for ${distance.value.toMeterText()}"
//                        }
//                        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null)
//                    }
//
//                    Log.e("ARCoreDebug", "Navigation Start Point: $arStartPoint")
//                    Log.e("ARCoreDebug", "Navigation End Point: $arEndPoint")
//
//                    arInstructionPoints.forEachIndexed { index, p ->
//                        Log.e("ARCoreDebug", "Navigation Point $index: ${p.lat}, ${p.lon}")
//                    }
//                    arInstructionNavigationList.forEachIndexed { index, p ->
//                        Log.e("ARCoreDebug", "Instruction $index: ${p.instruction}, ${p.distance}, ${p.floorId}")
//                    }
//                }
//            })
//
//            // 🔁 Finally, trigger the route request
//            routePlanning.route(request)
//
//        } catch (e: Exception) {
//            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
//            Log.e("RouteError", "Exception: ${e.message}", e)
//        }
//    }
//
//    fun redrawRoute() {
//        try {
//            val startPoint = RoutePlanningPoint(
//                lat = instructionList[instructionIndex.value].indoorPoints[0].lat,
//                lon = instructionList[instructionIndex.value].indoorPoints[0].lon,
//                floorId = instructionList[instructionIndex.value].indoorPoints[0].floorId
//            )
//            val points = listOf(startPoint, destinationPoint)
//            val request = RoutePlanningQueryRequest(points)
//            request.vehicle = "foot"
//            request.locale = RoutePlanningLocale.EN
//            routePlanning.route(request)
//            routePlanning.setRoutePlanningListener(object: RoutePlanningResultListener {
//                override fun onGetRoutePlanningResult(result: RoutePlanningResult?) {
//                    if(result == null) {
//                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
//                        return
//                    }
//                    if(result.status != 0) {
//                        Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
//                        return
//                    }
//                    if(result.routeResponseDto == null) {
//                        Toast.makeText(context, "Error: No route found", Toast.LENGTH_LONG).show()
//                        return
//                    }
//                    routePainter?.paintRouteUsingResult(result.routeResponseDto.paths.get(0), result.routeResponseDto.paths.get(0).indoorPoints)
//                    routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true));
//                    mapxusMap?.selectFloorById(instructionList[instructionIndex.value].floorId ?: "")
//                    titleNavigationStep.value = instructionList[instructionIndex.value].text ?: ""
//                    distanceNavigationStep.value = "${instructionList[instructionIndex.value].distance.roundToNearestHalfString()} m"
//                    distance.value = instructionList[instructionIndex.value].distance
//
//                    val isLastInstruction = instructionIndex.value == instructionList.lastIndex
//                    val message = if (isLastInstruction) {
//                        "Kudos! You have arrived at the Destination!"
//                    } else {
//                        "${titleNavigationStep.value}. And follow the steps for ${distance.value.toMeterText()}"
//                    }
//
//                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null)
//
//                }
//            })
//        } catch (e: Error) {
//            Toast.makeText(context, "Unable to get route, please check locations", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    fun nextStep() {
//        if (instructionList.isEmpty()) {
//            Log.w("Navigation", "Instruction list is empty, cannot go to next step.")
//            return
//        }
//
//        val isLastInstruction = instructionIndex.value >= instructionList.lastIndex
//        if (isLastInstruction) {
//            endNavigation()
//            navigationController.navigate("venueDetails")
//            arNavigationViewModel.isShowingARNavigation.value = false
//            showSheet.value = true
//        } else {
//            instructionIndex.value += 1
//            redrawRoute()
//        }
//    }
//
//    fun previousStep() {
//        instructionIndex.value -= 1;
//        redrawRoute()
//    }
//
//    fun setStartWithCenter() {
//        val point = mapboxMap?.cameraPosition?.target
//        startingPoint = RoutePlanningPoint(point?.longitude ?: 0.0, point?.latitude ?: 0.0, mapxusMap?.selectedFloor?.id)
//        arStartPoint = SerializableRoutePoint(point?.longitude ?: 0.0, point?.latitude ?: 0.0, mapxusMap?.selectedFloor?.id ?: "")
//        selectingCenter.value = false
//    }
//
//    fun endNavigation() {
//        instructionList.clear()
//        arInstructionPoints.clear()
//        arInstructionNavigationList.clear()
//
//        startingPoint = null
//        destinationPoint = null
//
//        routePainter?.cleanRoute()
//        getMapxusMap().removeMapxusPointAnnotations()
//        getMapxusMap().selectFloorById(getMapxusMap().selectedFloor?.id ?: "")
//        routePlanning.destroy()
//    }
//
//    fun onDestroy() {
//        job.cancel() // Clean up coroutine when no longer needed
//    }
//}
