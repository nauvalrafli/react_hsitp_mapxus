package com.mapxushsitp.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.mapxushsitp.view.sheets.VenueDetails
import com.mapxushsitp.view.sheets.VenueScreen
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapViewProvider
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnMapClickedListener
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusPointAnnotationOptions
import com.mapxus.map.mapxusmap.api.map.model.MapxusSite
import com.mapxus.map.mapxusmap.api.map.model.SelectorPosition
import com.mapxus.map.mapxusmap.api.services.RoutePlanning
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningLocale
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.IndoorLatLng
import com.mapxus.map.mapxusmap.api.services.model.VenueSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.planning.InstructionDto
import com.mapxus.map.mapxusmap.api.services.model.planning.PathDto
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningQueryRequest
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import toMeterText
import java.util.Locale
import kotlin.collections.find
import kotlin.math.abs
import kotlin.math.roundToInt

class MapxusSharedViewModel(application: Application) : AndroidViewModel(application) {

    // Context access helper
    val context: Context get() = getApplication<Application>()

    // Map-related data
    private val _mapView = MutableLiveData<MapView?>()
    val mapView: LiveData<MapView?> = _mapView

    private val _mapViewProvider = MutableLiveData<MapViewProvider?>()
    val mapViewProvider: LiveData<MapViewProvider?> = _mapViewProvider

    private val _selectedVenue = MutableLiveData<VenueInfo?>()
    val selectedVenue: LiveData<VenueInfo?> = _selectedVenue

    private val _selectedBuilding = MutableLiveData<IndoorBuildingInfo?>()
    val selectedBuilding: LiveData<IndoorBuildingInfo?> = _selectedBuilding

    private val _selectedPoi = MutableLiveData<PoiInfo?>()
    var selectedPoi: LiveData<PoiInfo?> = _selectedPoi

    private val _isFloorSelectorShown = MutableLiveData<Boolean>()
    val isFloorSelectorShown: LiveData<Boolean> = _isFloorSelectorShown

    // Navigation state
    private val _currentFloor = MutableLiveData<String?>()
    val currentFloor: LiveData<String?> = _currentFloor

    private val _isNavigationActive = MutableLiveData<Boolean>()
    val isNavigationActive: LiveData<Boolean> = _isNavigationActive

    private val _navTitleText = MutableLiveData<String>()
    val navTitleText: LiveData<String> = _navTitleText

    private val _navDistanceText = MutableLiveData<String>()
    val navDistanceText: LiveData<String> = _navDistanceText

    private val _navTimeText = MutableLiveData<String>()
    val navTimeText: LiveData<String> = _navTimeText

    // Venue data
    private val _venues = MutableLiveData<List<VenueInfo>>()
    val venues: LiveData<List<VenueInfo>> = _venues

    private val _building = MutableLiveData<List<IndoorBuildingInfo>>()
    val building: LiveData<List<IndoorBuildingInfo>> = _building

    var locale = Locale.getDefault()
    var selectionMark : ImageView? = null
    var bottomSheet: LinearLayout? = null
    var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    var maplibreMap : MapLibreMap? = null
    var mapxusMap: MapxusMap? = null
    var startLatLng: RoutePlanningPoint? = null
    var selectedStartText: String = ""
    var lastGpsTime = 0L
    var userLocation : RoutePlanningPoint? = null

    var routePlanning = RoutePlanning.newInstance()

    var routePainter : RoutePainter? = null
    var routeAdsorber: RouteAdsorber? = null
    var routeShortener: RouteShortener? = null

    private val _instructionList = MutableLiveData<List<InstructionDto>>(listOf<InstructionDto>())
    val instructionList: LiveData<List<InstructionDto>> = _instructionList
    private val _instructionPointList = MutableLiveData<List<IndoorLatLng>>(listOf())
    val instructionPointList: LiveData<List<IndoorLatLng>> = _instructionPointList

    private val _instructionIndex = MutableLiveData(0)
    val instructionIndex: LiveData<Int> = _instructionIndex

    lateinit var mapxusPositioningClient: MapxusPositioningClient
    lateinit var mapxusPositioningProvider: MapxusPositioningProvider

    val sharedPreferences = context.getSharedPreferences("Mapxus", android.content.Context.MODE_PRIVATE)

    var selectedVehicle: String = RoutePlanningVehicle.FOOT
    fun selectVehicle(value: String) {
        selectedVehicle = value
        sharedPreferences.edit {
            putString("vehicle", value)
            commit()
        }
    }

    fun initPositioning(lifecycleOwner: LifecycleOwner, context: Context) {
        mapxusPositioningClient = MapxusPositioningClient.getInstance(lifecycleOwner, context)
        mapxusPositioningProvider = MapxusPositioningProvider(mapxusPositioningClient)

        lifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                mapxusPositioningClient.addPositioningListener(object : MapxusPositioningListener {
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
                        lifecycleOwner.lifecycleScope.launch {
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

                            if(mapxusLocation.mapxusFloor != null) {
                                _currentFloor.value = mapxusLocation.mapxusFloor.id
                                mapxusMap?.selectFloorById(mapxusLocation.mapxusFloor.id)
                                mapxusPositioningClient.changeFloor(mapxusLocation.mapxusFloor)
                            }

                            if(routeAdsorber != null) {
                                val newLocation =
                                    routeAdsorber?.calculateTheAdsorptionLocation(indoorLocation)
                                        ?: indoorLocation
                                if (indoorLocation.latitude != newLocation?.latitude || indoorLocation.longitude != newLocation?.longitude) {
                                    indoorLocation.latitude = newLocation?.latitude ?: 0.0
                                    indoorLocation.longitude = newLocation?.longitude ?: 0.0
                                    userLocation = RoutePlanningPoint(
                                        newLocation?.longitude ?: 0.0,
                                        newLocation?.latitude ?: 0.0,
                                        newLocation?.floor?.id
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                                    if (routeShortener != null) {
                                        routeShortener?.cutFromTheLocationProjection(newLocation, maplibreMap)
                                    }
                                }
                            } else {
                                userLocation = RoutePlanningPoint(
                                    indoorLocation?.longitude ?: 0.0,
                                    indoorLocation?.latitude ?: 0.0,
                                    indoorLocation?.floor?.id
                                )
                                withContext(Dispatchers.Main) {
                                    mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                                }
                            }
                        }
                    }
                })
                mapxusPositioningClient.setDebugEnabled(true)
                mapxusPositioningProvider.start()
                mapxusPositioningClient.start()
                mapxusMap?.setLocationEnabled(true)
                withContext(Dispatchers.Main) {
                    mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
                }
            }
        }
    }

    // Methods to update data
    fun setMapView(mapView: MapView) {
        _mapView.value = mapView
        _mapView.value?.getMapAsync {
            maplibreMap = it
            if(mapxusMap != null && routePainter == null) {
                routePainter = RoutePainter(context, maplibreMap, mapxusMap)
            }
        }
    }
    fun setMapViewProvider(provider: MapViewProvider) {
        _mapViewProvider.value = provider
        _mapViewProvider.value?.getMapxusMapAsync {
            mapxusMap = it
            if(maplibreMap != null && routePainter == null) {
                routePainter = RoutePainter(context, maplibreMap, mapxusMap)
            }
//            mapxusMap?.mapxusUiSettings?.isSelectorEnabled = false
            mapxusMap?.mapxusUiSettings?.isBuildingSelectorEnabled = false
            mapxusMap?.mapxusUiSettings?.setSelectorPosition(SelectorPosition.TOP_LEFT)
            mapxusMap?.mapxusUiSettings?.setSelectFontColor(androidx.compose.ui.graphics.Color.White.hashCode())
            mapxusMap?.mapxusUiSettings?.setSelectBoxColor(Color(0xFF4285F4).hashCode())
//            mapViewProvider.value.setLanguage()

            mapxusMap?.addOnMapClickedListener(object: OnMapClickedListener {
                override fun onMapClick(
                    p0: LatLng,
                    p1: MapxusSite
                ) {
                    if(mapxusMap?.selectedVenueId != null) {
                        if(venues.value.isEmpty()) {
                            val vs = VenueSearch.newInstance()
                            vs.setVenueSearchResultListener { updateVenues(it.venueInfoList) }
                            vs.searchVenueByOption(VenueSearchOption())
                        }

                        setSelectedVenue(venues.value.find { it.id == p1.venue?.id })
                    }
                }
            })
            mapxusMap?.addOnIndoorPoiClickListener { poi ->
                setSelectedPoi(PoiInfo(poiId = poi.id, nameMap = poi.nameMap, buildingId =  poi.buildingId, location = com.mapxus.map.mapxusmap.api.services.model.LatLng().apply { lat = poi.latitude; lon = poi.longitude }))
            }
            mapxusPositioningProvider.addListener(object: IndoorLocationProviderListener {
                override fun onCompassChanged(angle: Float, sensorAccuracy: Int) {
//                    mapxusMap?.setLocationProvider(mapxusPositioningProvider)
                }

                override fun onIndoorLocationChange(indoorLocation: IndoorLocation?) {
                    Log.d("Location", "Indoor Location Change")
                    userLocation = RoutePlanningPoint(
                        indoorLocation?.longitude ?: 0.0,
                        indoorLocation?.latitude ?: 0.0,
                        indoorLocation?.floor?.id
                    )
                    if(indoorLocation?.floor?.id != null) {
                        userLocation = userLocation?.copy(floorId = indoorLocation.floor?.id)
                    }
                }

                override fun onProviderError(errorInfo: com.mapxus.map.mapxusmap.positioning.ErrorInfo) {
                    Log.d("Location", "Provider Error: ${errorInfo.errorMessage}")
                }

                override fun onProviderStarted() {
                    Log.d("Location", "Started")
                    mapxusMap?.setLocationProvider(mapxusPositioningProvider)
                }

                override fun onProviderStopped() {
                    Log.d("Location", "Stopped")
                }

            })
            it?.setLocationEnabled(true)
            mapxusMap?.setLocationEnabled(true)
            mapxusMap?.setLocationProvider(mapxusPositioningProvider)
            mapxusPositioningProvider.start()
        }
    }

    fun selectVenue(venueId: String) {
        _mapViewProvider.value?.getMapxusMapAsync { mapxusMap ->
            mapxusMap.selectVenueById(venueId)
            _isFloorSelectorShown.value = true
        }
    }

    fun setSelectedVenue(venue: VenueInfo?) {
        _selectedVenue.value = venue
    }

    fun setSelectedBuilding(building: IndoorBuildingInfo) {
        _selectedBuilding.value = building
    }

    fun setSelectedPoi(poi: PoiInfo) {
        _selectedPoi.value = poi
        mapxusMap?.removeMapxusPointAnnotations()
        mapxusMap?.selectFloorById(poi.floorId ?: "")
        mapxusMap?.addMapxusPointAnnotation(
            MapxusPointAnnotationOptions().apply {
                position = LatLng(
                    poi.location.lat,
                    poi.location.lon
                )
            }
        )
    }

    fun setFloorSelectorShown(shown: Boolean) {
        _isFloorSelectorShown.value = shown
    }

    fun setCurrentFloor(floorId: String?) {
        _currentFloor.value = floorId
    }

    fun setNavigationActive(active: Boolean) {
        _isNavigationActive.value = active
    }

    fun updateVenues(venues: List<VenueInfo>) {
        _venues.value = venues
    }

    fun updateBuildings(venues: List<IndoorBuildingInfo>) {
        _building.value = venues
    }

    // Convenience methods for common operations
    fun selectVenueAndShowFloorSelector(venueId: String) {
        selectVenue(venueId)
        setFloorSelectorShown(true)
    }

    fun updateNavigationText(title: String, distance: String, totalDistance: Double) {
        _navTitleText.value = title
        _navDistanceText.value = distance
        val estimatedSeconds = (totalDistance/1.2).roundToInt()
        if(estimatedSeconds > 60)
            _navTimeText.value = context.resources.getString(R.string.minute, (estimatedSeconds/60).toInt())
        else
            _navTimeText.value = context.resources.getString(R.string.second, estimatedSeconds)
    }

    val routePlanningListener = object : RoutePlanning.RoutePlanningResultListener {
        override fun onGetRoutePlanningResult(p0: RoutePlanningResult?) {
            try {
                if(p0 == null || p0.status != 0 || p0.routeResponseDto == null) {
                    Log.d("Mapxus", "Route not found")
                    return
                }
                _instructionIndex.value = 0
                clearInstructions()
                _instructionList.value = p0.routeResponseDto.paths.get(0).instructions
                _instructionPointList.value = p0.routeResponseDto.paths.get(0).indoorPoints
                if(routePainter == null) {
                    routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                }
                routePainter?.paintRouteUsingResult(p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));

                if(p0.routeResponseDto.paths[0].indoorPoints[0].floorId != null) {
                    mapxusMap?.selectFloorById(p0.routeResponseDto.paths[0].indoorPoints[0].floorId ?: "")
                }
                updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].distance.toMeterText(
                    Locale.getDefault()), p0.routeResponseDto.paths.map { it.distance }.reduce { a,b -> a + b })

                if(isNavigating) {
                    routeAdsorber = RouteAdsorber(NavigationPathDto(p0.routeResponseDto.paths.get(0)))
                    routeShortener = RouteShortener(NavigationPathDto(p0.routeResponseDto.paths.get(0)), p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths[0].indoorPoints)
                    routeShortener?.setOnPathChangeListener(object: RouteShortener.OnPathChangeListener {
                        override fun onPathChange(pathDto: PathDto?) {
                            if(pathDto == null) {
                                Log.d("Mapxus", "Route not found")
                                return
                            }
                            if(routePainter == null) {
                                routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                            }
                            mapxusMap?.selectFloorById(p0.routeResponseDto.paths.get(0).indoorPoints.get(0).floorId ?: "")
                            routePainter?.paintRouteUsingResult(pathDto, pathDto.indoorPoints, isAutoZoom = false)
                            routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));
                            updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].distance.toMeterText(
                                Locale.getDefault()), p0.routeResponseDto.paths.map { it.distance }.reduce { a,b -> a + b })
                        }
                    })
                }
            } catch(e: Error) {
                e.printStackTrace()
            }
        }
    }

    val routePlanningStepListener = object : RoutePlanning.RoutePlanningResultListener {
        override fun onGetRoutePlanningResult(p0: RoutePlanningResult?) {
            try {
                if(p0 == null || p0.status != 0 || p0.routeResponseDto == null) {
                    Log.d("Mapxus", "Route not found")
                    return
                }
                if(routePainter == null) {
                    routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                }
                routePainter?.paintRouteUsingResult(p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));

                if(p0.routeResponseDto.paths[0].indoorPoints[0].floorId != null) {
                    mapxusMap?.selectFloorById(p0.routeResponseDto.paths[0].indoorPoints[0].floorId ?: "")
                }

                updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].distance.toMeterText(
                    Locale.getDefault()), p0.routeResponseDto.paths.map { it.distance }.reduce { a,b -> a + b })

                if(isNavigating) {
                    routeAdsorber = RouteAdsorber(NavigationPathDto(p0.routeResponseDto.paths.get(0)))
                    routeShortener = RouteShortener(NavigationPathDto(p0.routeResponseDto.paths.get(0)), p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths[0].indoorPoints)
                }
            } catch(e: Error) {
                e.printStackTrace()
            }
        }
    }

    var isNavigating = false

    fun requestRoutePlanning(isForNavigating: Boolean, routeType: String) {
        mapxusMap?.removeMapxusPointAnnotations()
        if(startLatLng?.floorId != null) {
            mapxusMap?.selectFloorById(startLatLng?.floorId ?: "")
        }
        val points = mutableListOf<RoutePlanningPoint>()
        if(startLatLng == null) {
            Toast.makeText(context, "Start location not set", Toast.LENGTH_SHORT).show()
            return
        }
        points.add(startLatLng!!)
        points.add(RoutePlanningPoint(selectedPoi.value?.location?.lon ?: 0.0, selectedPoi.value?.location?.lat  ?: 0.0, selectedPoi.value?.floorId))
        val request = RoutePlanningQueryRequest().apply {
            this.points = points.toList()
        }
        request.vehicle = routeType
        request.locale =
            if(locale.language == "en") RoutePlanningLocale.EN
            else if (locale.country == "hk") RoutePlanningLocale.ZH_HK
            else if(locale.country == "tw") RoutePlanningLocale.ZH_TW
            else RoutePlanningLocale.ZH_CN

        routePlanning.setRoutePlanningListener(routePlanningListener)
        routePlanning.route(request)
        isNavigating = isForNavigating
    }

    fun updateRoutePlanning() {
        routePlanning.setRoutePlanningListener(routePlanningStepListener)
        val request = RoutePlanningQueryRequest().apply {
            val points = mutableListOf<RoutePlanningPoint>()

            points.add(RoutePlanningPoint(instructionList.value.get(instructionIndex.value).indoorPoints.get(0).lon, instructionList.value.get(instructionIndex.value).indoorPoints.get(0).lat, instructionList.value.get(instructionIndex.value).indoorPoints.get(0).floorId))
            points.add(RoutePlanningPoint(instructionList.value.get(instructionList.value.size - 1).indoorPoints.get(0).lon, instructionList.value.get(instructionList.value.size - 1).indoorPoints.get(0).lat, instructionList.value.get(instructionList.value.size - 1).indoorPoints.get(0).floorId))
            this.points = points
        }
        routePlanning.route(request)
    }

    fun nextStep() {
        if((instructionIndex.value ?: 0) < (instructionList.value ?: listOf()).size - 1) {
            _instructionIndex.value += 1
            updateRoutePlanning()
        }
    }

    fun previousStep() {
        if((instructionIndex.value ?: 0) > 0) {
            _instructionIndex.value -= 1
            updateRoutePlanning()
        }
    }

    fun setInstructions(list: List<InstructionDto>) {
        _instructionList.value = list
    }

    fun clearInstructions() {
        _instructionList.value = emptyList()
    }

    fun setInstructionIndex(index: Int) {
        _instructionIndex.value = index
    }

    fun nextInstruction() {
        _instructionIndex.value = (_instructionIndex.value ?: 0) + 1
    }
}

