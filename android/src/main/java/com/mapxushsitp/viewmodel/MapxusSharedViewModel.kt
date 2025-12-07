package com.mapxushsitp.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
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
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.RoutePlanning
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningLocale
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.DetailSearchOption
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
import com.mapxushsitp.R
import com.mapxushsitp.service.toMeterText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.collections.get
import kotlin.math.abs
import kotlin.math.roundToInt

class MapxusSharedViewModel(application: Application) : AndroidViewModel(application) {

    // Context access helper
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

    val sharedPreferences = context.getSharedPreferences("Mapxus", Context.MODE_PRIVATE)

    var selectedVehicle: String = RoutePlanningVehicle.FOOT
    fun selectVehicle(value: String) {
        selectedVehicle = value
        sharedPreferences.edit {
            putString("vehicle", value)
            commit()
        }
    }

    val positioningListener = object : MapxusPositioningListener {
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
          errorInfo.errorMessage.contains("Current provider disabled", true)
        ) {

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
            mapxusPositioningProvider.dispatchCompassChange(
              orientation,
              sensorAccuracy
            )
          }
        } else {
          mapxusPositioningProvider.dispatchCompassChange(
            orientation,
            sensorAccuracy
          )
        }
      }

      override fun onLocationChange(mapxusLocation: MapxusLocation) {
        val location = Location("MapxusPositioning")
        viewModelScope.launch {
          location.latitude = mapxusLocation.latitude
          location.longitude = mapxusLocation.longitude
          location.time = System.currentTimeMillis()
          val building = mapxusLocation.buildingId
          val floorInfo =
            if (mapxusLocation.mapxusFloor == null) null else FloorInfo(
              mapxusLocation.mapxusFloor.id,
              mapxusLocation.mapxusFloor.code,
              mapxusLocation.mapxusFloor.ordinal
            )

          val indoorLocation = IndoorLocation(building, floorInfo, location)
          indoorLocation.accuracy = mapxusLocation.accuracy

          if (mapxusLocation.mapxusFloor != null) {
            _currentFloor.value = mapxusLocation.mapxusFloor.id
            mapxusMap?.selectFloorById(mapxusLocation.mapxusFloor.id)
            mapxusPositioningClient.changeFloor(mapxusLocation.mapxusFloor)
          }

          if (routeAdsorber != null) {
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
              mapxusPositioningProvider.dispatchIndoorLocationChange(
                indoorLocation
              )
              if (routeShortener != null) {
                routeShortener?.cutFromTheLocationProjection(
                  newLocation,
                  maplibreMap
                )
              }
            }
          } else {
            userLocation = RoutePlanningPoint(
              indoorLocation?.longitude ?: 0.0,
              indoorLocation?.latitude ?: 0.0,
              indoorLocation?.floor?.id
            )
            withContext(Dispatchers.Main) {
              mapxusPositioningProvider.dispatchIndoorLocationChange(
                indoorLocation
              )
            }
          }
        }
      }
    }

    fun initPositioning(lifecycleOwner: LifecycleOwner, context: Context) {
        mapxusPositioningClient = MapxusPositioningClient.getInstance(lifecycleOwner, context)
        mapxusPositioningProvider = MapxusPositioningProvider(mapxusPositioningClient)
        lifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                mapxusPositioningClient.addPositioningListener(positioningListener)
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

    private fun clearPositioningProviderListeners() {
      // Ensure mapxusPositioningProvider is initialized before proceeding
      if (!::mapxusPositioningProvider.isInitialized) return

      try {
        // The class name of the abstract base class that holds the listener lists
        val providerClass = Class.forName("com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider")

        // 1. Target: IndoorLocationProvider.compassListeners
        // This list holds the specific internal listener (r0$e) causing the leak.
        val compassListenersField = providerClass.getDeclaredField("compassListeners")
        compassListenersField.isAccessible = true

        // Get the actual list instance from your mapxusPositioningProvider object
        val compassListeners = compassListenersField.get(mapxusPositioningProvider) as? java.util.concurrent.CopyOnWriteArrayList<*>

        // CRITICAL: Clear the list to remove all retained listeners
        compassListeners?.clear()
        Log.d("ViewModel", "Cleared IndoorLocationProvider.compassListeners.")


        // 2. Target: IndoorLocationProvider.listeners
        // Clearing this list removes general location listeners (although the compass list was the direct leak source, clearing this is safer).
        val listenersField = providerClass.getDeclaredField("listeners")
        listenersField.isAccessible = true

        val listeners = listenersField.get(mapxusPositioningProvider) as? java.util.concurrent.CopyOnWriteArrayList<*>
        listeners?.clear()
        Log.d("ViewModel", "Cleared IndoorLocationProvider.listeners.")

        Log.d("ViewModel", "Cleared all internal MapxusPositioningProvider listener lists.")

      } catch (e: Exception) {
        // Log the failure in case the SDK updates and field names change
        Log.e("ViewModel", "Failed to clear positioning provider listeners via reflection. Error: ${e.message}", e)
      }
    }

    /**
     * Clear view references when Fragment is destroyed.
     * This should be called from onDestroyView() to allow Fragment recreation.
     */
    private fun clearAllMapLibreViewReferences() {
      try {
        val dacClass = Class.forName("org.maplibre.android.plugins.annotation.DraggableAnnotationController")
        val dacInstanceField = dacClass.getDeclaredField("INSTANCE")
        dacInstanceField.isAccessible = true
        val dacInstance = dacInstanceField.get(null) // Should be the static instance

        if (dacInstance != null) {

          // --- 1. Fix 1: Clear the direct maplibreMap field (for safety) ---
          try {
            val maplibreMapField = dacClass.getDeclaredField("maplibreMap")
            maplibreMapField.isAccessible = true
            maplibreMapField.set(dacInstance, null)
          } catch (e: Exception) {
            // Ignore if field doesn't exist or is already cleared
          }

          // --- 2. Fix 2: CRITICAL - Clear the annotationManagersById HashMap ---
          // This targets the new leak path: DraggableAnnotationController.annotationManagersById
          val managersByIdField = dacClass.getDeclaredField("annotationManagersById")
          managersByIdField.isAccessible = true
          val managersById = managersByIdField.get(dacInstance) as? java.util.HashMap<*, *>

          managersById?.apply {
            // Manually iterate and clear internal map references before clearing the list
            for (manager in values) {
              try {
                // All MapLibre Annotation Managers inherit from AnnotationManager
                val managerSuperclass = Class.forName("org.maplibre.android.maps.AnnotationManager")
                val mapViewField = managerSuperclass.getDeclaredField("mapView")
                mapViewField.isAccessible = true
                mapViewField.set(manager, null) // Null the MapView reference
              } catch (e: Exception) {
                Log.w("ViewModel", "Failed to clear mapView in manager: ${e.message}")
              }
            }

            // Clear the HashMap itself, removing the SymbolManager instances
            clear()
            Log.d("ViewModel", "Cleared annotationManagersById HashMap.")
          }


          // --- 3. Fix 3: Clear the old annotationManagers list (if it still exists) ---
          try {
            val managersField = dacClass.getDeclaredField("annotationManagers")
            managersField.isAccessible = true
            val managers = managersField.get(dacInstance) as? MutableList<*>
            managers?.clear()
          } catch (e: Exception) {
            // Ignore if field doesn't exist
          }

          Log.d("ViewModel", "Comprehensive MapLibre cleanup successful.")
        }

      } catch (e: Exception) {
        Log.e("ViewModel", "Comprehensive MapLibre cleanup failed.", e)
      }
    }

    private fun clearMapLibreAnnotationManagers() {
      try {
        val controllerClass = Class.forName(
          "org.maplibre.android.plugins.annotation.DraggableAnnotationController"
        )

        val instanceField = controllerClass.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        val instance = instanceField.get(null)

        if (instance != null) {
          // Clear annotationManagers list
          val managersField = controllerClass.getDeclaredField("annotationManagers")
          managersField.isAccessible = true
          val managers = managersField.get(instance) as? MutableList<*>

          managers?.forEach { manager ->
            try {
              // Clear mapView from each manager
              val managerClass = manager?.javaClass?.superclass
              val mapViewField = managerClass?.getDeclaredField("mapView")
              mapViewField?.isAccessible = true
              mapViewField?.set(manager, null)
            } catch (e: Exception) {
              // Ignore
            }
          }

          managers?.clear()

          // Also clear the mapView field
          val mapViewField = controllerClass.getDeclaredField("mapView")
          mapViewField.isAccessible = true
          mapViewField.set(instance, null)

          Log.d("ViewModel", "Cleared MapLibre annotation managers")
        }
      } catch (e: Exception) {
        Log.e("ViewModel", "Failed to clear annotation managers", e)
      }
    }

    fun clearViewReferences() {
      mapxusMap?.removeOnMapClickedListener(onMapClicked)
      mapxusMap?.removeOnIndoorPoiClickListener(onIndoorClick)
      mapxusPositioningProvider.removeListener(indoorLocationProviderListener)
      routeShortener?.setOnPathChangeListener(null)

      routePainter?.cleanRoute()
      routePainter = null

      try {
        mapxusMap?.removeMapxusPointAnnotations()
      } catch (e: Exception) {
        Log.e("ViewModel", "Error removing annotations", e)
      }

      clearPositioningProviderListeners()
      maplibreMap?.annotations?.clear()
      maplibreMap?.removeAnnotations()
      clearAllMapLibreViewReferences()
//      clearMapLibreAnnotationManagers()

      mapxusMap = null
      maplibreMap = null
      _mapView.value?.onDestroy()


      _mapView.value = null
      _mapViewProvider.value = null

      clearPositioningProviderListeners()

      bottomSheet = null
      bottomSheetBehavior = null
      selectionMark = null
      navController = null
      routeAdsorber = null
    }

    /**
     * Full cleanup - stops services and clears all references.
     * Should only be called when the Activity is being destroyed.
     */
    fun destroy() {
      routePainter?.cleanRoute()
      routePainter = null
      if (::mapxusPositioningProvider.isInitialized) {
        mapxusPositioningProvider.stop()
      }
      if (::mapxusPositioningClient.isInitialized) {
        mapxusPositioningClient.removePositioningListener(positioningListener)
        mapxusPositioningClient.stop()
      }

      mapxusMap?.removeMapxusPointAnnotations()
      mapxusMap = null
      maplibreMap = null
      _mapView.value?.onDestroy()
      _mapView.value = null
      _mapViewProvider.value = null

      clearViewReferences()
      clearStaticLifecycleOwner()
    }

    private fun clearStaticLifecycleOwner() {
        try {
            val mapxusClientClass = Class.forName(
              "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
            )

            // Clear LIFECYCLE_OWNER
            val lifecycleOwnerField = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
            lifecycleOwnerField.isAccessible = true
            lifecycleOwnerField.set(null, null)

            Log.d("ViewModel", "Cleared MapxusPositioningClient static references")
        } catch (e: Exception) {
            Log.e("ViewModel", "Failed to clear static references", e)
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
            if(mapxusMap != null && routePlanning == null) {
                routePlanning = RoutePlanning.newInstance()
            }
        }
    }
    fun setMapViewProvider(provider: MapViewProvider) {
        _mapViewProvider.value = provider
        _mapViewProvider.value?.setLanguage(locale.language)
        _mapViewProvider.value?.getMapxusMapAsync {
            mapxusMap = it
            if(maplibreMap != null && routePainter == null) {
                routePainter = RoutePainter(context, maplibreMap, mapxusMap)
            }
//            mapxusMap?.mapxusUiSettings?.isSelectorEnabled = false
            mapxusMap?.mapxusUiSettings?.isBuildingSelectorEnabled = false
            mapxusMap?.switchFloorScope = SwitchFloorScope.GLOBAL
            mapxusMap?.mapxusUiSettings?.setSelectorPosition(SelectorPosition.TOP_LEFT)
            mapxusMap?.mapxusUiSettings?.setSelectFontColor(Color.White.hashCode())
            mapxusMap?.mapxusUiSettings?.setSelectBoxColor(Color(0xFF4285F4).hashCode())
//            mapViewProvider.value.setLanguage()

            mapxusMap?.addOnMapClickedListener(onMapClicked)
            mapxusMap?.addOnIndoorPoiClickListener(onIndoorClick)
            mapxusPositioningProvider.addListener(indoorLocationProviderListener)
            it?.setLocationEnabled(true)
            mapxusMap?.setLocationEnabled(true)
            mapxusMap?.setLocationProvider(mapxusPositioningProvider)
            mapxusPositioningProvider.start()
        }
    }

    val indoorLocationProviderListener = object: IndoorLocationProviderListener {
      override fun onCompassChanged(angle: Float, sensorAccuracy: Int) {
//                    mapxusMap?.setLocationProvider(mapxusPositioningProvider)
      }

      override fun onIndoorLocationChange(indoorLocation: IndoorLocation?) {
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
        Log.d("REACT-MAPXUS", "Provider Error: ${errorInfo.errorMessage}")
      }

      override fun onProviderStarted() {
        Log.d("REACT-MAPXUS", "Started")
        mapxusMap?.setLocationProvider(mapxusPositioningProvider)
      }

      override fun onProviderStopped() {
        Log.d("REACT-MAPXUS", "Stopped")
      }

    }
    val onIndoorClick = MapxusMap.OnIndoorPoiClickListener { poi ->
      if(navController?.currentDestination?.id != R.id.poiDetailsFragment) {
        navController?.navigate(R.id.action_searchResult_to_poiDetails)
      }
      setSelectedPoi(PoiInfo(poiId = poi.id, nameMap = poi.nameMap, buildingId =  poi.buildingId, location = com.mapxus.map.mapxusmap.api.services.model.LatLng().apply { lat = poi.latitude; lon = poi.longitude }, floor = poi.floorName, floorId = poi.floor, sharedFloorId = poi.sharedFloorId))
      bottomSheet?.post {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
    val onMapClicked = OnMapClickedListener { p0, p1 ->
      if(mapxusMap?.selectedVenueId != null) {
        if(venues.value?.isEmpty() == true) {
          val vs = VenueSearch.newInstance()
          vs.setVenueSearchResultListener { updateVenues(it.venueInfoList) }
          vs.searchVenueByOption(VenueSearchOption())
        }

        setSelectedVenue(venues.value?.find { it.id == p1.venue?.id })
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
                    Toast.makeText(context, context.resources.getString(R.string.no_route), Toast.LENGTH_LONG).show();
                    _isNavigationActive.value = false
                    _instructionList.value = emptyList()
                    _instructionIndex.value = 0
                    _isLoadingroute.value = false
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
                updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].distance.toMeterText(
                    Locale.getDefault()), p0.routeResponseDto.paths.map { it.distance }.reduce { a,b -> a + b })

                if(isNavigating && instructionList.value?.isNotEmpty() == true) {
                    routeAdsorber = RouteAdsorber(NavigationPathDto(p0.routeResponseDto.paths.get(0)))
                    routeShortener = RouteShortener(NavigationPathDto(p0.routeResponseDto.paths.get(0)), p0.routeResponseDto.paths.get(0), p0.routeResponseDto.paths[0].indoorPoints)
                    routeShortener?.setOnPathChangeListener(object: RouteShortener.OnPathChangeListener {
                      override fun onPathChange(pathDto: PathDto?) {
                        if(pathDto != null) {
                          if(routePainter == null) {
                            routePainter = RoutePainter(context, maplibreMap, mapxusMap)
                          }
                          mapxusMap?.selectFloorById(p0.routeResponseDto.paths[0].indoorPoints[0].floorId ?: "")
                          routePainter?.paintRouteUsingResult(pathDto, pathDto?.indoorPoints ?: listOf(), isAutoZoom = false)
                          routePainter?.setRoutePainterResource(RoutePainterResource().setHiddenTranslucentPaths(true).setIndoorLineColor(android.graphics.Color.BLUE));
                          updateNavigationText(p0.routeResponseDto.paths[0].instructions[0].text, p0.routeResponseDto.paths[0].distance.toMeterText(
                            locale ?: Locale.getDefault()), p0.routeResponseDto.paths.map { it.distance }.reduce { a,b -> a + b })
                        } else {
                          Log.d("REACT-MAPXUS", "Route not found")
                          return
                        }
                      }
                    })
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
                if(p0 == null || p0.status != 0 || p0.routeResponseDto == null) {
                    Log.d("REACT-MAPXUS", "Route not found")
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
    var _isLoadingroute = MutableLiveData<Boolean>(false)
    var isLoadingRoute : LiveData<Boolean> = _isLoadingroute

    fun requestRoutePlanning(isForNavigating: Boolean, routeType: String, callback: () -> Unit = {}) {
        if(startLatLng?.floorId != null) {
            mapxusMap?.selectFloorById(startLatLng?.floorId ?: userLocation?.floorId ?: selectedBuilding.value?.floors[0]?.id ?: "")
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
        points.add(RoutePlanningPoint(selectedPoi.value?.location?.lon ?: 0.0, selectedPoi.value?.location?.lat  ?: 0.0, selectedPoi.value?.floorId ?: selectedPoi.value?.sharedFloorId ?: ""))
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
        isNavigating = isForNavigating
    }

    fun updateRoutePlanning() {
        routePlanning.setRoutePlanningListener(routePlanningStepListener)
        val request = RoutePlanningQueryRequest().apply {
            val points = mutableListOf<RoutePlanningPoint>()

            points.add(RoutePlanningPoint(instructionList.value?.get(instructionIndex.value ?: 0)?.indoorPoints?.get(0)?.lon ?: 0.0, instructionList.value?.get(instructionIndex?.value  ?: 0)?.indoorPoints?.get(0)?.lat ?: 0.0, instructionList.value?.get(instructionIndex?.value ?: 0)?.indoorPoints?.get(0)?.floorId))
            points.add(RoutePlanningPoint(instructionList.value?.get(instructionList.value?.size?.minus(1) ?: 0)?.indoorPoints?.get(0)?.lon ?: 0.0, instructionList.value?.get(instructionList.value?.size?.minus(1) ?: 0)?.indoorPoints?.get(0)?.lat ?: 0.0, instructionList.value?.get(instructionList.value?.size?.minus(1) ?: 0)?.indoorPoints?.get(0)?.floorId))
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
    }

    fun setInstructionIndex(index: Int) {
        _instructionIndex.value = index
    }

    fun nextInstruction() {
        _instructionIndex.value = (_instructionIndex.value ?: 0) + 1
    }
}

