package com.mapxushsitp.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapViewProvider
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnMapClickedListener
import com.mapxus.map.mapxusmap.api.map.SwitchFloorScope
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
import com.mapxus.map.mapxusmap.api.services.model.floor.SharedFloor
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
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.issuereport.MapxusIssueReportClient
import com.mapxus.positioning.api.issuereport.MapxusIssueReportListener
import com.mapxus.positioning.api.issuereport.Record
import com.mapxus.positioning.api.positioning.MapxusFloor
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import com.mapxus.positioning.api.positioning.MapxusPositioningListener
import com.mapxus.positioning.api.positioning.PositioningMode
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxushsitp.service.MapxusPositioningProvider
import com.mapxushsitp.service.Preference
import com.mapxushsitp.service.toMeterText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.math.roundToInt

class MapxusSharedViewModel(application: Application) : AndroidViewModel(application), MapxusPositioningListener {

    var context: Context = getApplication<Application>()
    var navController : NavController? = null

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

    var selectedCategory = ""
    var excludedCategory = ""

    fun resetCategory() {
        selectedCategory = ""
        excludedCategory = ""
    }

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

    private val _signIcon = MutableLiveData<Int>()
    val signIcon: LiveData<Int> = _signIcon

    private val _timeInSeconds = MutableLiveData<Int>()
    val timeInSeconds: LiveData<Int> = _timeInSeconds

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
    var userLocation : MapxusLocation? = null

    var routePlanning: RoutePlanning = RoutePlanning.newInstance()

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
    lateinit var mapxusIssueReportClient : MapxusIssueReportClient

    val sharedPreferences : SharedPreferences? = null

    init {
        Preference.init(context)
    }

    var selectedVehicle: String = RoutePlanningVehicle.FOOT
    fun selectVehicle(value: String) {
        selectedVehicle = value
        Preference.editVehicle(value)
    }

    var lastUpdateTime = 0L
    var counter = 0
    var isOnceFinished = false
    var onLocationChangedOnceListener : () -> Unit = {}
        set(value) {
            isOnceFinished = false
            field = value
        }

    fun initPositioning() {
        viewModelScope.launch {
            mapxusPositioningClient = MapxusPositioningClient.getInstance(application.applicationContext)
            mapxusIssueReportClient = MapxusIssueReportClient.getInstance(application.applicationContext)
            mapxusIssueReportClient.addIssueReportListener(reportListener)
            mapxusPositioningProvider = MapxusPositioningProvider()
            mapxusPositioningClient.addPositioningListener(this@MapxusSharedViewModel)
            mapxusMap?.setLocationEnabled(true)
            mapxusPositioningClient.setUserMode(UserMode.PEDESTRIAN)
            mapxusPositioningClient.start()

            locationFlow
                .conflate()
                .collect {
                    onLocationFlowUpdated(it)
                    if(counter < 2) {
                        counter++
                    } else {
                      maplibreMap?.getStyle { p0 ->
                        if(p0.isFullyLoaded) {
                          mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
                        }
                      }
                    }
                    if(!isOnceFinished) {
                        isOnceFinished = true
                        onLocationChangedOnceListener()
                    }
                }
        }
    }

    // Methods to update data
    fun setMapView(mapView: MapView) {
        _mapView.value = mapView
        mapView.isNestedScrollingEnabled = false
        _mapView.value?.getMapAsync {
            maplibreMap = it
            if(mapxusMap != null && routePainter == null) {
                routePainter = RoutePainter(context, maplibreMap, mapxusMap)
            }
            if(mapxusMap != null && routePlanning == null) {
                routePlanning = RoutePlanning.newInstance()
            }
        }
    }
    fun setMapViewProvider(provider: MapViewProvider) {
        _mapViewProvider.value = provider
        _mapViewProvider.value?.getMapxusMapAsync {
            mapxusMap = it
            if(maplibreMap != null && routePainter == null) {
                routePainter = RoutePainter(context, maplibreMap, it)
            }
            it?.mapxusUiSettings?.isBuildingSelectorEnabled = false
            it?.switchFloorScope = SwitchFloorScope.GLOBAL
            it?.mapxusUiSettings?.setSelectorPosition(SelectorPosition.TOP_LEFT)
            it?.mapxusUiSettings?.setSelectFontColor(Color.White.hashCode())
            it?.mapxusUiSettings?.setSelectBoxColor(Color(0xFF4285F4).hashCode())

            it?.addOnMapClickedListener(object: OnMapClickedListener {
                override fun onMapClick(
                    p0: LatLng,
                    p1: MapxusSite
                ) {
                    if(mapxusMap?.selectedVenueId != null) {
                        if((venues.value ?: listOf()).isEmpty()) {
                            val vs = VenueSearch.newInstance()
                            vs.setVenueSearchResultListener { updateVenues(it.venueInfoList) }
                            vs.searchVenueByOption(VenueSearchOption())
                        }

                        setSelectedVenue((venues.value ?: listOf()).find { it.id == p1.venue?.id })
                    }
                }
            })
            it?.addOnIndoorPoiClickListener { poi ->
                if(navController?.currentDestination?.id != R.id.poiDetailsFragment) {
                    navController?.navigate(R.id.action_searchResult_to_poiDetails)
                }
                setSelectedPoi(PoiInfo(poiId = poi.id, nameMap = poi.nameMap, buildingId =  poi.buildingId, location = com.mapxus.map.mapxusmap.api.services.model.LatLng().apply { lat = poi.latitude; lon = poi.longitude }, floor = poi.floorName, floorId = poi.floor, sharedFloorId = poi.sharedFloorId))
                bottomSheet?.post {
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            it?.setLocationEnabled(true)
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

    fun setSelectedPoi(poi: PoiInfo, callback: () -> Unit = {}) {
        _selectedPoi.value = poi
        if(_selectedBuilding.value?.buildingId != poi.buildingId) {
            try {
                _selectedBuilding.value = building.value?.find { it.buildingId == poi.buildingId }
            } catch(e: Exception) {
                mapxusMap?.removeMapxusPointAnnotations()
            }
        } else {
            mapxusMap?.removeMapxusPointAnnotations()
            mapxusMap?.addMapxusPointAnnotation(
                MapxusPointAnnotationOptions().apply {
                    position = LatLng(
                        poi.location.lat,
                        poi.location.lon
                    )
                    floorId = poi.floorId ?: poi.sharedFloorId ?: ""
                }
            )
            callback()
        }
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

    fun updateNavigationText(title: String, distance: String, totalDistance: Double, sign: Int) {
        _navTitleText.value = title
        _navDistanceText.value = distance
        val estimatedSeconds = (totalDistance/1.2).roundToInt()
        _timeInSeconds.value = estimatedSeconds
        if(estimatedSeconds > 60)
            _navTimeText.value = context.resources.getString(R.string.minute, (estimatedSeconds/60).toInt())
        else if(estimatedSeconds > 3)
            _navTimeText.value = context.resources.getString(R.string.second, estimatedSeconds)
    }

    val shortenerListener = object: RouteShortener.OnPathChangeListener {
        override fun onPathChange(pathDto: PathDto?) {
            try {
                if(pathDto != null) {
                    if(routePainter == null) {
                        routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                    }
                    if(pathDto.instructions[0].distance <= 1 && instructionIndex.value == instructionList.value?.size?.minus(pathDto.instructions.size)) {
                        nextStep()
                        return;
                    } else if((_instructionList.value ?: listOf()).size >= pathDto.instructions.size) {
                        val value = _instructionList.value?.size?.minus(pathDto.instructions.size)
                        if(_instructionIndex.value != value) {
                            _instructionIndex.value = value
                            updateRoutePlanning()
                            updateNavigationText(pathDto.instructions[0]?.text ?: "", pathDto.instructions[0].distance?.toMeterText(
                                Locale.getDefault()) ?: "", pathDto.instructions.map { it.distance }.reduce { a,b -> a + b } ?: 0.0, pathDto.instructions[0].sign)
                        }
                    }
                    //prevent auto zoom everytime
                    if(pathDto.indoorPoints[(_instructionIndex.value ?: 0)].floorId != null && mapxusMap?.selectedFloor?.id != pathDto.indoorPoints[(_instructionIndex.value ?: 0)].floorId) {
                        val camera = maplibreMap?.cameraPosition
                        mapxusMap?.selectFloorById(pathDto.indoorPoints[(_instructionIndex.value ?: 0)].floorId ?: "")
                        if(camera != null) {
                            maplibreMap?.cameraPosition = camera
                        }
                    }
                    try {
                        val start = pathDto.instructions[0].indoorPoints[0]
                        pathDto.requestPoints = listOf(
                            RoutePlanningPoint(start.lon, start.lat, start.floorId),
                            RoutePlanningPoint(selectedPoi.value?.location?.lon ?: 0.0, selectedPoi.value?.location?.lat  ?: 0.0, selectedPoi.value?.floorId)
                        )
                        routePainter?.paintRouteUsingResult(pathDto!!, pathDto!!.indoorPoints,false)
//                                        routePainter?.paintRouteUsingResult( p0.routeResponseDto, isAutoZoom = false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));
                    updateNavigationText(pathDto.instructions[0].text, pathDto.instructions[0].distance.toMeterText(
                        Locale.getDefault()), pathDto.instructions.map { it.distance }.reduce { a,b -> a + b }, pathDto.instructions[0].sign)
                } else {
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val routePlanningListener = object : RoutePlanning.RoutePlanningResultListener {
        override fun onGetRoutePlanningResult(p0: RoutePlanningResult?) {
            try {
                if(p0 == null || p0.status != 0 || p0.routeResponseDto == null) {
                    Toast.makeText(context, context.resources.getString(R.string.no_route), Toast.LENGTH_LONG).show();
                    _isNavigationActive.value = false
                    _instructionList.value = emptyList()
                    _instructionIndex.value = 0
                    _isLoadingroute.value = false
                    isNavigating = false
                    if(navController?.currentDestination?.id == R.id.showRouteFragment) {
                        navController?.navigateUp()
                    }
                    return
                }
                if(p0.status!=0){
                    Toast.makeText(context, context.resources.getString(R.string.no_route), Toast.LENGTH_LONG).show();
                    _isLoadingroute.value = false
                    return;
                }
                if(p0.routeResponseDto == null){
                    Toast.makeText(context, context.resources.getString(R.string.no_route),Toast.LENGTH_LONG).show();
                    _isLoadingroute.value = false
                    return;
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
                updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].instructions[0].distance.toMeterText(
                    Locale.getDefault()), p0.routeResponseDto.paths[0].instructions.map { it.distance }.reduce { a,b -> a + b }, p0.routeResponseDto.paths[0].instructions[0].sign)

                if(isNavigating && (instructionList.value ?: listOf()).isNotEmpty()) {
                    routeShortener = RouteShortener(NavigationPathDto(p0.routeResponseDto.paths.get(0)), p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths.get(0).indoorPoints)
                    routeAdsorber = RouteAdsorber(NavigationPathDto(p0.routeResponseDto.paths.get(0)))
                    routeShortener?.setOnPathChangeListener(shortenerListener)
                }
                _isLoadingroute.value = false
            } catch(e: Error) {
                e.printStackTrace()
            }
        }
    }

    val routePlanningStepListener = object : RoutePlanning.RoutePlanningResultListener {
        override fun onGetRoutePlanningResult(p0: RoutePlanningResult?) {
            try {
                _isLoadingroute.value = true
                if(p0 == null || p0.status != 0 || p0.routeResponseDto == null) {
                    Log.d("Mapxus", "Route not found")
                    return
                }
                if(routePainter == null) {
                    routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                }
                routePainter?.paintRouteUsingResult(p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths.get(0).indoorPoints, isAutoZoom = false)
                routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));

                if(p0.routeResponseDto.paths[0].indoorPoints[0].floorId != null && _currentFloor.value != p0.routeResponseDto.paths[0].indoorPoints[0].floorId) {
                    mapxusMap?.selectFloorById(p0.routeResponseDto.paths[0].indoorPoints[0].floorId ?: "")
                }

                routeAdsorber = RouteAdsorber(NavigationPathDto(p0.routeResponseDto.paths[0]), 2.0, 3)
                routeShortener = RouteShortener(NavigationPathDto(p0.routeResponseDto.paths[0]), p0.routeResponseDto.paths[0], p0.routeResponseDto.paths[0].indoorPoints)
                routeShortener?.setOnPathChangeListener(shortenerListener)
                try {
                    updateNavigationText(instructionList.value?.get((instructionIndex.value ?: 0))?.text ?: "", instructionList.value?.get(instructionIndex.value ?: 0)?.distance?.toMeterText(
                        Locale.getDefault()) ?: "", instructionList.value?.map { it.distance }?.reduce { a,b -> a + b } ?: 0.0, instructionList.value?.get(instructionIndex.value ?: 0)?.sign ?: 0)
                } catch (e: Exception) {
                }
                _isLoadingroute.value = false
            } catch(e: Error) {
                e.printStackTrace()
                _isLoadingroute.value = false
            }
        }
    }

    var isNavigating = false
        set(value) {
            field = value
            if(!value) {
                routeShortener = null
                routeAdsorber = null
            }
        }
    var _isLoadingroute = MutableLiveData<Boolean>(false)
    var isLoadingRoute : LiveData<Boolean> = _isLoadingroute

    fun requestRoutePlanning(isForNavigating: Boolean, routeType: String, callback: () -> Unit = {}) {
        isNavigating = isForNavigating
        if(startLatLng?.floorId != null) {
            mapxusMap?.selectFloorById(startLatLng?.floorId ?: userLocation?.mapxusFloor?.id ?: selectedBuilding.value?.floors?.get(0)?.id ?: "")
        }
        mapxusMap?.removeMapxusPointAnnotations()
        if(routePlanning == null) {
            routePlanning = RoutePlanning.newInstance()
        }
//        if(startLatLng?.floorId != null) {
//            mapxusMap?.selectFloorById(startLatLng?.floorId ?: "")
//        }
        val points = mutableListOf<RoutePlanningPoint>()
        if(startLatLng == null) {
            Toast.makeText(context, "Start location not set", Toast.LENGTH_SHORT).show()
            return
        }
        points.add(startLatLng!!)
        val destination = RoutePlanningPoint(selectedPoi.value?.location?.lon ?: 0.0, selectedPoi.value?.location?.lat  ?: 0.0, selectedPoi.value?.floorId ?: selectedPoi.value?.sharedFloorId ?: "")
        points.add(destination)

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
        _isLoadingroute.value = true
    }

    fun updateRoutePlanning() {
        routePlanning.setRoutePlanningListener(routePlanningStepListener)
        val request = RoutePlanningQueryRequest().apply {
            val points = mutableListOf<RoutePlanningPoint>()

            points.add(RoutePlanningPoint((instructionList.value ?: listOf()).get(instructionIndex.value ?: 0).indoorPoints.get(0).lon, (instructionList.value ?: listOf()).get(instructionIndex.value ?: 0).indoorPoints.get(0).lat, (instructionList.value ?: listOf()).get(instructionIndex.value ?: 0).indoorPoints.get(0).floorId))
            points.add(RoutePlanningPoint((instructionList.value ?: listOf()).get((instructionList.value ?: listOf()).size - 1).indoorPoints.get(0).lon, (instructionList.value ?: listOf()).get((instructionList.value ?: listOf()).size - 1).indoorPoints.get(0).lat, (instructionList.value ?: listOf()).get((instructionList.value ?: listOf()).size - 1).indoorPoints.get(0).floorId))
            this.points = points
        }
        routePlanning.route(request)
    }

    fun nextStep() {
        if((instructionIndex.value ?: 0) < (instructionList.value ?: listOf()).size - 1) {
          _instructionIndex.value = _instructionIndex.value?.plus(1)
          updateRoutePlanning()
        }
    }

    fun previousStep() {
        if((instructionIndex.value ?: 0) > 0) {
          _instructionIndex.value = _instructionIndex.value?.minus(1)
          updateRoutePlanning()
        }
    }

    fun setInstructions(list: List<InstructionDto>) {
        _instructionList.value = list
    }

    fun clearInstructions() {
        _instructionList.value = emptyList()
        routeShortener = null
    }

    fun setInstructionIndex(index: Int) {
        _instructionIndex.value = index
    }

    fun nextInstruction() {
        _instructionIndex.value = (_instructionIndex.value ?: 0) + 1
    }

    fun startPositioning() {
      if(::mapxusPositioningClient.isInitialized) {
        mapxusPositioningClient.start()
      }
    }

    fun endNavigation() {
        clearInstructions()
        setInstructionIndex(0)
        isNavigating = false

        // Clear route painter if exists
        routePainter?.cleanRoute()
    }

    private val locationFlow = MutableSharedFlow<MapxusLocation>(
        0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private suspend fun onLocationFlowUpdated(mapxusLocation: MapxusLocation) {
        userLocation = mapxusLocation

        val location = Location("MapxusPositioning").apply {
            latitude = mapxusLocation.latitude
            longitude = mapxusLocation.longitude
            time = System.currentTimeMillis()
        }

        try {
            val building = mapxusLocation.buildingId
            val floorInfo = mapxusLocation.mapxusFloor?.run {
                when (type) {
                    MapxusFloor.Type.FLOOR -> FloorInfo(id, code, ordinal)
                    MapxusFloor.Type.SHARED_FLOOR -> SharedFloor(id, code, ordinal)
                }
            }
            _currentFloor.value = floorInfo?.id

            val indoorLocation = IndoorLocation(building, floorInfo, location)
            indoorLocation.accuracy = mapxusLocation.accuracy

//            mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)

            if(mapxusLocation.mapxusFloor != null && mapxusLocation.mapxusFloor?.id != null && mapxusLocation.mapxusFloor?.id != mapxusMap?.selectedFloor?.id && isNavigating) {
                mapxusMap?.selectFloorById(mapxusLocation.mapxusFloor!!.id)
            }

            if(routeAdsorber != null) {
                val newLocation = withContext(Dispatchers.Default) {
                    routeAdsorber?.calculateTheAdsorptionLocation(indoorLocation)
                        ?: indoorLocation
                }
                if (indoorLocation.latitude != newLocation?.latitude || indoorLocation.longitude != newLocation?.longitude) {
                    indoorLocation.latitude = newLocation?.latitude ?: indoorLocation.latitude
                    indoorLocation.longitude = newLocation?.longitude ?: indoorLocation.longitude
                }
                mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                if (routeShortener != null) {
                    try {
                        routeShortener?.cutFromTheLocationProjection(indoorLocation, maplibreMap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStateChange(positionerState: PositioningState) {
        when (positionerState) {
            PositioningState.STOPPED -> {
                mapxusMap?.setLocationEnabled(false)
                mapxusPositioningProvider.dispatchOnProviderStopped()
            }

            PositioningState.RUNNING -> {
                mapxusMap?.setLocationProvider(mapxusPositioningProvider)
                mapxusMap?.setLocationEnabled(true)
                mapxusPositioningProvider.dispatchOnProviderStarted()
            }

            else -> {}
        }
    }

    override fun onBearingChange(bearing: Float) {
        mapxusPositioningProvider.dispatchCompassChange(bearing, 0)
    }

    override fun onLocationChange(mapxusLocation: MapxusLocation) {
        locationFlow.tryEmit(mapxusLocation)
    }

    override fun onWheelchairSpeedChange(speed: Float) {
    }

    override fun onPositioningModeChange(mode: PositioningMode) {

    }

    override fun onFeedback(userFeedbackInfo: UserFeedbackInfo) {
        Log.d("Feedback", "${userFeedbackInfo.code} ${userFeedbackInfo.type} ${userFeedbackInfo.message}")
        for(item in mapxusIssueReportClient.listLocalRecords()) {
            mapxusIssueReportClient.uploadRecord(item)
        }
    }

    val reportListener = object : MapxusIssueReportListener {
        override fun onRecordUploadSuccess(record: Record) {
            val logId = mapxusIssueReportClient.loggingId()
            val deviceName = Build.MODEL
        }

        override fun onRecordUploadFailed(
            record: Record,
            errorMessage: String
        ) {
            Log.d("REACT-MAPXUS", errorMessage)
        }

    }

  fun remeasureBottomSheet() {
    val newHeight = bottomSheet?.measuredHeight
    if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HALF_EXPANDED) {
      if (bottomSheetBehavior?.peekHeight != newHeight && newHeight != null) {
        bottomSheetBehavior?.peekHeight = newHeight
      }
      bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }
  }
}

