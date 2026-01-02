package com.mapxushsitp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapViewProvider
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.planning.InstructionDto
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxus.map.mapxusmap.impl.MapLibreMapViewProvider
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.arComponents.FourthLocalARFragment
import com.mapxushsitp.data.model.ParcelizeRoutePoint
import com.mapxushsitp.data.model.SerializableRouteInstruction
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.mapxushsitp.service.MapxusUtility
import com.mapxushsitp.service.generateSpeakText
import com.mapxushsitp.service.toMeterText
import com.mapxushsitp.view.onboarding.OnboardingPage
import com.mapxushsitp.view.onboarding.OnboardingView
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class XmlActivity : AppCompatActivity(), SensorEventListener {

  lateinit var mapView: MapView
  lateinit var mapViewProvider: MapViewProvider
  lateinit var userLocation: ImageView
  lateinit var bottomSheet: LinearLayout
  lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
  lateinit var fragmentContainer : FragmentContainerView
  private var navController: NavController? = null

  // UI Elements
  private lateinit var gpsFab: FloatingActionButton
  private lateinit var volumeFab: FloatingActionButton
  private lateinit var arNavigationFab: FloatingActionButton
  private lateinit var arFragmentContainer: FragmentContainerView
  private lateinit var navigationRouteCard: CardView
  private lateinit var navTitleText: TextView
  private lateinit var navDistanceText: TextView
  private lateinit var navTimeText: TextView
  private lateinit var loadingOverlay: LinearLayout
  private var arriveAtDestinationDialog: AlertDialog? = null
  private lateinit var navIcon: ImageView
  private lateinit var navPreviousButton: AppCompatImageButton
  private lateinit var navNextButton: AppCompatImageButton
  private lateinit var stepIndicatorsContainer: LinearLayout

  // Shared ViewModel
  private val mapxusSharedViewModel: MapxusSharedViewModel by viewModels()
  private val arNavigationViewModel: ARNavigationViewModel by viewModels()

  // Text-to-Speech
  private lateinit var tts: TextToSpeech
  private var isSpeaking: Boolean = true
  var lastSpoken = -1;

  override fun attachBaseContext(newBase: Context?) {
    var base = newBase

    if (base != null) {
      val config = base.resources.configuration
      Locale.setDefault(MapxusUtility.selectedLocale)
      config.setLocale(MapxusUtility.selectedLocale)

      base = base.createConfigurationContext(config)
    }

    super.attachBaseContext(base)
  }

  // Permission launcher
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val allGranted = permissions.entries.all { it.value }
    if (!allGranted) {
      showPermissionDialog()
    } else {
      // After permissions granted, check for precise location on Android 12+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkPreciseLocation()
      }
    }
  }

  // Location settings launcher
  private val locationSettingsLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    // User returned from location settings
    checkLocationServices()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_xml)
//    setupVersion()

    mapxusSharedViewModel.locale =
      intent.getStringExtra("locale")?.let { loc ->
        if (loc.contains("-")) {
          val p = loc.split("-")
          Locale(p[0], p[1])
        } else {
          Locale(loc)
        }
      } ?: resources.configuration.locale
    Log.d("REACT-MAPXUS", intent.getStringExtra("locale") ?: "null")
    Log.d("REACT-MAPXUS", mapxusSharedViewModel.locale.toLanguageTag())
    mapxusSharedViewModel.selectVehicle(mapxusSharedViewModel.sharedPreferences.getString("vehicle", RoutePlanningVehicle.FOOT) ?: RoutePlanningVehicle.FOOT)
    isSpeaking = mapxusSharedViewModel.sharedPreferences.getBoolean("isSpeaking", true)

    initializeTTS()

    // Check all required permissions (location fine, coarse, camera) and precise location
    checkAllPermissions()

    setupMap()
    mapView.onCreate(savedInstanceState)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
    setupFloatingActionButtons()


    mapViewProvider.getMapxusMapAsync {
      setupBottomSheet()
      setupNavigation()
      setupNavigationRouteCard()
      mapView.post {
        it.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
      }
    }

    findViewById<TextView>(R.id.version).setText("0.1.16")

    val boarded = mapxusSharedViewModel.sharedPreferences.getBoolean("onboardingDone", false)
    if(!boarded) {
      setupWalkthroughOverlay()
    }
  }

  fun setupMap() {
    mapView = findViewById<MapView>(R.id.mapView)
    mapxusSharedViewModel.setMapView(mapView)
    try {
      val surface = mapView.getChildAt(0) as SurfaceView?
      surface?.setZOrderOnTop(false)
      surface?.setZOrderMediaOverlay(true)
      surface?.z = 0f
    } catch (e: Exception) {
      Log.d("Mapxus", "Unable to set map always bottom")
    }
    val mapOptions = MapxusMapOptions().apply {
      floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
      zoomLevel = 19.0
    }
    mapViewProvider = MapLibreMapViewProvider(this, mapView, mapOptions)
    mapxusSharedViewModel.setMapViewProvider(mapViewProvider)
    userLocation = findViewById(R.id.select_location)

    // Initialize shared ViewModel with map components
    mapxusSharedViewModel.setMapViewProvider(mapViewProvider)
    mapxusSharedViewModel.initPositioning(this, this)

    loadingOverlay = findViewById(R.id.loading_overlay)
    mapxusSharedViewModel.isLoadingRoute.observe(this) {
      loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
    }
  }

  private fun setupWalkthroughOverlay() {
    val onboarding = findViewById<OnboardingView>(R.id.containerOnboarding)
    val pages = listOf(
      OnboardingPage(
        imageRes = R.drawable.baseline_my_location_24,
        title = "Welcome to Mapxus!",
        subtitle = "Take a look and discover what we can do for you.",
        description = "We help you navigate... Outdoor location will be using GPS."
      ),
      OnboardingPage(
        imageRes = R.drawable.figure_8_compass_calibration,
        title = "Device Calibration",
        subtitle = "Keep your compass accurate",
        description = "Make sure your device compass accurate by doing calibration.",
        isGif = true
      ),
      OnboardingPage(
        imageRes = R.drawable.baseline_location_on_24,
        title = "Explore the Map",
        subtitle = "Browse any nearby places",
        description = "Explore the locations around you...",
      )
    )

    onboarding.setup(pages)
    onboarding.visibility = View.VISIBLE
  }

//  fun setupVersion() {
//    val versionText = findViewById<TextView>(R.id.version)
//    try {
//      val pInfo = this.packageManager.getPackageInfo(this.getPackageName(), 0);
//      val versionName = pInfo.versionName;
//      val versionCode = pInfo.versionCode;
//      val version = "${versionName} +${versionCode}"
//      versionText.setText(version)
//    } catch (e: PackageManager.NameNotFoundException) {
//      versionText.setText("")
//      e.printStackTrace();
//    }
//  }

  fun setupBottomSheet() {
    bottomSheet = findViewById<LinearLayout>(R.id.bottomSheet)
    mapxusSharedViewModel.bottomSheet = bottomSheet

    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
    mapxusSharedViewModel.bottomSheetBehavior = bottomSheetBehavior

    bottomSheetBehavior.isHideable = false
    bottomSheetBehavior.isDraggable = true
    bottomSheetBehavior.skipCollapsed = false
    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

    bottomSheet.visibility = View.VISIBLE

    mapView.addOnDidFinishRenderingFrameListener { _,_,_ ->
      bottomSheetBehavior.isHideable = mapxusSharedViewModel.isNavigating
      if(mapxusSharedViewModel.isNavigating) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        arNavigationFab.visibility = View.VISIBLE
      } else if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        arNavigationFab.visibility = View.GONE
      }
    }
    bottomSheet.viewTreeObserver.addOnGlobalLayoutListener {
      val newHeight = bottomSheet.measuredHeight
      // Only update peekHeight if not in half-expanded state (to preserve half-height setting)
      if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED) {
        if (bottomSheetBehavior.peekHeight != newHeight) {
          bottomSheetBehavior.peekHeight = newHeight
        } else if(!mapxusSharedViewModel.isNavigating) {
          bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
      }

      if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }

    bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
      override fun onStateChanged(bottomSheet: View, newState: Int) {
      }

      override fun onSlide(bottomSheet: View, slideOffset: Float) {
      }
    })

    onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if(mapxusSharedViewModel.isNavigating) {
          Log.d("REACT-MAPXUS", "AA")
          mapxusSharedViewModel.routePainter?.cleanRoute()
          mapxusSharedViewModel.clearInstructions()
          mapxusSharedViewModel.setInstructionIndex(0)
          mapxusSharedViewModel.isNavigating = false
        } else if(navController?.currentDestination?.id != R.id.venueScreenFragment) {
          Log.d("REACT-MAPXUS", "BB ${navController?.currentDestination?.id}")
          if(navController?.currentDestination?.id == R.id.poiDetailsFragment) {
            mapxusSharedViewModel.mapxusMap?.removeMapxusPointAnnotations()
          }
          navController?.navigateUp()
          mapxusSharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
          Log.d("REACT-MAPXUS", "CC ${navController?.currentDestination?.route}")
          finish()
        }
      }
    })
  }

  fun setupNavigation() {
    fragmentContainer = findViewById(R.id.fragment_container)

    lifecycleScope.launch {
      withContext(Dispatchers.IO) {
        delay(300)
        fragmentContainer.visibility = View.VISIBLE
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
        withContext(Dispatchers.Main) {
          navController = navHostFragment?.navController
          mapxusSharedViewModel.navController = navController
          navController?.addOnDestinationChangedListener { _, destination, _ ->
            // Don't expand if navigating to ShowRouteFragment (keep half-expanded)
            bottomSheet.postDelayed({
              if (destination.id != R.id.showRouteFragment) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
              }
            }, 200)
          }
        }
      }
    }

    mapxusSharedViewModel.selectionMark = findViewById(R.id.select_location)
  }

  private fun setupFloatingActionButtons() {
    gpsFab = findViewById(R.id.gps_fab)
    volumeFab = findViewById(R.id.volume_fab)
    arNavigationFab = findViewById(R.id.ar_navigation_fab)
    arFragmentContainer = findViewById(R.id.ar_fragment_container)

    // GPS button - center on user location
    gpsFab.setOnClickListener {
      if (hasLocationPermissions()) {
        if (isLocationEnabled()) {
          mapxusSharedViewModel.mapxusMap?.let { mapxusMap ->
            val zoomLevel = mapxusSharedViewModel.maplibreMap?.cameraPosition?.zoom ?: 19.0
            mapxusMap.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
          }
        } else {
          showLocationSettingsDialog()
        }
      } else {
        requestLocationPermissions()
      }
    }

    // Volume button - toggle voice navigation (show during navigation)
    volumeFab.setOnClickListener {
      isSpeaking = !isSpeaking
      mapxusSharedViewModel.sharedPreferences.edit().putBoolean("isSpeaking", isSpeaking).apply()
      updateVolumeButtonIcon()
    }
    updateVolumeButtonIcon()

    // AR Navigation button - toggle AR view
    arNavigationFab.setOnClickListener {
      val isARActive = arNavigationViewModel.isShowingAndClosingARNavigation.value ?: false
      arNavigationViewModel.isShowingAndClosingARNavigation.value = !isARActive

      if (!isARActive) {
        showARFragment()
      } else {
        hideARFragment()
      }
    }

    mapxusSharedViewModel.instructionList.observe(this) { instructions ->
      val isNavigating = instructions.isNotEmpty()

      volumeFab.visibility = if (isNavigating) View.VISIBLE else View.GONE

      if (isNavigating) {
        arNavigationViewModel.isShowingOpeningAndClosingARButton.value = true
      }
    }

  }

  private fun setupNavigationRouteCard() {
    navigationRouteCard = findViewById(R.id.navigation_route_card)
    navTitleText = findViewById(R.id.nav_title_text)
    navDistanceText = findViewById(R.id.nav_distance_text)
    navTimeText = findViewById(R.id.nav_time_text)
    navIcon = findViewById(R.id.nav_icon)
    navPreviousButton = findViewById(R.id.nav_previous_button)
    navNextButton = findViewById(R.id.nav_next_button)
    stepIndicatorsContainer = findViewById(R.id.step_indicators_container)

    // Previous button click
    navPreviousButton.setOnClickListener {
      mapxusSharedViewModel.previousStep()
      arNavigationViewModel.prevInstruction()
    }

    // Next button click
    navNextButton.setOnClickListener {
      mapxusSharedViewModel.nextStep()
      arNavigationViewModel.nextInstruction(mapxusSharedViewModel.instructionList.value?.size ?: 1)
    }

    // Observe instruction index and update card

    // Observe instruction list and index reactively
    mapxusSharedViewModel.instructionList.observe(this) { instructionList ->
      val instructionIndex = mapxusSharedViewModel.instructionIndex.value ?: 0
      val isNavigating = mapxusSharedViewModel.isNavigating

      updateNavigationUI(instructionList, instructionIndex, isNavigating)
    }

    mapxusSharedViewModel.instructionIndex.observe(this) { instructionIndex ->
      val instructionList = mapxusSharedViewModel.instructionList.value.orEmpty()
      val isNavigating = mapxusSharedViewModel.isNavigating

      updateNavigationUI(instructionList, instructionIndex, isNavigating)

      // Speak instruction when index changes during navigation
      if (lastSpoken != instructionIndex && isNavigating && instructionList.isNotEmpty() && instructionIndex in instructionList.indices) {
        lastSpoken = instructionIndex
        speakInstruction(instructionList[instructionIndex])
      }
    }
  }

  private fun updateStepIndicators(totalSteps: Int, currentStep: Int) {
    stepIndicatorsContainer.removeAllViews()

    val maxVisibleIndicators = 6
    val startStep = when {
      totalSteps <= maxVisibleIndicators -> 0
      currentStep <= 2 -> 0
      currentStep >= totalSteps - 3 -> totalSteps - maxVisibleIndicators
      else -> currentStep - 3
    }.coerceAtLeast(0)

    val endStep = (startStep + maxVisibleIndicators).coerceAtMost(totalSteps)

    for (i in startStep until endStep) {
      val indicator = View(this).apply {
        val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
          setMargins(4, 4, 4, 4)
        }
        background = GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.WHITE) // base color (required for tint to apply)
        }

        // Apply tint
        backgroundTintList = ColorStateList.valueOf(
          ContextCompat.getColor(
            this@XmlActivity,
            if (i == currentStep) android.R.color.holo_blue_light else android.R.color.darker_gray
          )
        )
      }
      stepIndicatorsContainer.addView(indicator)
    }
  }

  private fun updateNavigationUI(
    instructionList: List<InstructionDto>,
    instructionIndex: Int,
    isNavigating: Boolean
  ) {
    if (instructionList.isNotEmpty() && instructionIndex in instructionList.indices && isNavigating) {
      val instruction = instructionList[instructionIndex]
      navTitleText.text = instruction.text ?: ""
      navDistanceText.text = "${instruction.distance.toMeterText(Locale.getDefault())}"
      val totalDistanceMeters = (instructionList.subList(instructionIndex.absoluteValue, instructionList.size).map { instructionDto -> instructionDto.distance }.reduce { a,b -> a + b } / 1.2).roundToInt()
      val estimatedSeconds = (totalDistanceMeters/1.2).roundToInt()
      if(estimatedSeconds > 60)
        navTimeText.text = getString(R.string.minute, (estimatedSeconds/60).toInt())
      else
        navTimeText.text = getString(R.string.second, estimatedSeconds)

      // Check if we should show arrival dialog
      if (isLastStepOrLowTimeEstimation(instructionIndex, instructionList.size, estimatedSeconds)) {
        showArriveAtDestinationDialog()
      } else {
        // Hide dialog if it's showing and we're not at destination
        arriveAtDestinationDialog?.dismiss()
        arriveAtDestinationDialog = null
      }

      // Update buttons and card visibility
      navPreviousButton.isEnabled = instructionIndex > 0
      navigationRouteCard.visibility = View.VISIBLE

      val icon = getStepIcon(instructionList.getOrNull(instructionIndex)?.sign ?: 0)
      navIcon.setImageDrawable(resources.getDrawable(icon, null))

      // Update step indicators
      updateStepIndicators(instructionList.size, instructionIndex)
    } else {
      navigationRouteCard.visibility = View.GONE
      // Hide dialog when navigation ends
      arriveAtDestinationDialog?.dismiss()
      arriveAtDestinationDialog = null
    }
  }

  fun getStepIcon(sign: Int): Int {
    return when (sign) {
      -98 -> R.drawable.u_turn_left // U_TURN_UNKNOWN
      -8  -> R.drawable.u_turn_left // U_TURN_LEFT
      -7  -> R.drawable.turn_left // KEEP_LEFT
      -3  -> R.drawable.turn_sharp_left // TURN_SHARP_LEFT
      -2  -> R.drawable.turn_left // TURN_LEFT
      -1  -> R.drawable.turn_slight_left // TURN_SLIGHT_LEFT
      0   -> R.drawable.straight // CONTINUE_ON_STREET
      1   -> R.drawable.turn_slight_right // TURN_SLIGHT_RIGHT
      2   -> R.drawable.turn_right // TURN_RIGHT
      3   -> R.drawable.turn_sharp_right // TURN_SHARP_RIGHT
      4   -> R.drawable.flag // FINISH
      5   -> R.drawable.flag // REACHED_VIA
      6   -> R.drawable.change_circle // USE_ROUNDABOUT
      7   -> R.drawable.turn_right // KEEP_RIGHT
      8   -> R.drawable.u_turn_right // U_TURN_RIGHT
      100 -> R.drawable.keyboard_arrow_up // UP (elevator up)
      -100 -> R.drawable.keyboard_arrow_down // DOWN (elevator down)
      200 -> R.drawable.door_front // CROSS_DOOR
      -200 -> R.drawable.meeting_room // CROSS_ROOM_DOOR
      else -> R.drawable.help // fallback for unexpected signs
    }
  }

  private fun isLastStepOrLowTimeEstimation(
    currentStep: Int,
    totalSteps: Int,
    estimatedSeconds: Int
  ): Boolean {
    // First condition: check if we're at the last step
    if (currentStep >= totalSteps - 1) {
      return true
    }

    // Second condition: check if time estimation is <= 4 seconds
    return estimatedSeconds <= 4
  }

  private fun showArriveAtDestinationDialog() {
    // Don't show if already showing
    lastSpoken = -1;
    mapxusSharedViewModel.routeShortener = null
    if (arriveAtDestinationDialog?.isShowing == true) {
      return
    }

    val dialogView = layoutInflater.inflate(R.layout.dialog_arrive_at_destination, null)

    val btnGoPrevious = dialogView.findViewById<TextView>(R.id.btn_go_previous)
    val btnFinished = dialogView.findViewById<TextView>(R.id.btn_finished)

    arriveAtDestinationDialog = AlertDialog.Builder(this)
      .setView(dialogView)
      .setCancelable(false)
      .create()

    // Set transparent background for rounded corners
    arriveAtDestinationDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

    // Go Previous button
    btnGoPrevious.setOnClickListener {
      mapxusSharedViewModel.previousStep()
      arriveAtDestinationDialog?.dismiss()
      arriveAtDestinationDialog = null
    }

    // Finished button
    btnFinished.setOnClickListener {
      // End navigation
      endNavigation()
      arriveAtDestinationDialog?.dismiss()
      arriveAtDestinationDialog = null
    }

    arriveAtDestinationDialog?.show()
  }

  private fun endNavigation() {
    mapxusSharedViewModel.clearInstructions()
    mapxusSharedViewModel.setInstructionIndex(0)
    mapxusSharedViewModel.isNavigating = false
    navigationRouteCard.visibility = View.GONE

    // Hide AR if showing
    if (arNavigationViewModel.isShowingAndClosingARNavigation.value == true) {
      hideARFragment()
    }

    // Clear route painter if exists
    mapxusSharedViewModel.routePainter?.cleanRoute()
  }


  private fun showARFragment() {
    val startLocationSerializable = SerializableRoutePoint(
      mapxusSharedViewModel.startLatLng?.lat ?: 0.0,
      mapxusSharedViewModel.startLatLng?.lon ?: 0.0,
      mapxusSharedViewModel.startLatLng?.floorId ?: ""
    )
    val destinationLocationSerializable = SerializableRoutePoint(
      mapxusSharedViewModel.selectedPoi?.value?.location?.lat ?: 0.0,
      mapxusSharedViewModel.selectedPoi?.value?.location?.lon ?: 0.0,
      mapxusSharedViewModel.selectedPoi?.value?.floorId ?: mapxusSharedViewModel.selectedPoi?.value?.sharedFloorId ?: ""
    )

    val instructionListSerializable = (mapxusSharedViewModel.instructionList.value ?: listOf()).mapNotNull {
      it.floorId?.let { floorId ->
        SerializableRouteInstruction(it.text, it.distance, floorId)
      }
    }

    val instructionPointList : MutableList<RoutePlanningPoint> = mutableListOf()
    (mapxusSharedViewModel.instructionList.value ?: listOf()).forEachIndexed { index, instruction ->
      instruction.indoorPoints.firstOrNull()?.let { point ->
        instructionPointList.add(
          RoutePlanningPoint(
            lat = point.lat,
            lon = point.lon,
            floorId = point.floorId
          )
        )
        Log.e("InstructionCoord", "Instruction $index → (${point.lat}, ${point.lon})")
      }
    }

    val instructionPointSerializable = instructionPointList.map {
      it.floorId?.let { it1 -> SerializableRoutePoint(it.lat, it.lon, it1) }
    }

    val secondInstructionPointSerializable = instructionPointList.map {
      it.floorId?.let { it1 -> ParcelizeRoutePoint(it.lat, it.lon, it1) }
    }

    val fragment = FourthLocalARFragment()

    val args = Bundle().apply {
      putSerializable("yourLocation", startLocationSerializable!!)
      putSerializable("destination", destinationLocationSerializable!!)
      putInt("instructionIndex", mapxusSharedViewModel.instructionIndex.value ?: 0)
      putInt("secondInstructionIndex", mapxusSharedViewModel.instructionIndex.value ?: 0)
      putSerializable("instructionList", ArrayList(instructionListSerializable))
      putSerializable("instructionPoints", ArrayList(instructionPointSerializable))
      putParcelableArrayList("secondInstructionPoints", ArrayList(secondInstructionPointSerializable))
    }

    fragment.arguments = args

    supportFragmentManager.beginTransaction()
      .replace(R.id.ar_fragment_container, fragment)
      .commit()

    arFragmentContainer.visibility = View.VISIBLE
  }

  private fun hideARFragment() {
    arFragmentContainer.visibility = View.GONE
  }

  fun getNavController(): NavController? {
    return navController ?: run {
      val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
      navHostFragment?.navController
    }
  }

  fun getSharedViewModel(): MapxusSharedViewModel {
    return mapxusSharedViewModel
  }

  override fun onStart() {
    super.onStart()
    mapxusSharedViewModel.mapView.value?.onStart()
  }

  override fun onResume() {
    super.onResume()
    mapxusSharedViewModel.mapView.value?.onResume()
  }

  override fun onPause() {
    mapxusSharedViewModel.mapView.value?.onPause()
    super.onPause()
  }

  override fun onStop() {
    mapxusSharedViewModel.mapView.value?.onStop()
    super.onStop()
  }

  private fun initializeTTS() {
    tts = TextToSpeech(this) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val locale = Locale.getDefault()
        val language = if (locale.language.contains("zh")) locale else Locale("en-US")
        Handler(Looper.getMainLooper()).postDelayed({
          tts.setLanguage(language)
        }, 1000)
      }
    }
  }

  private fun speakInstruction(instruction: InstructionDto) {
    if (isSpeaking && ::tts.isInitialized) {
      val locale = Locale.getDefault()
      val words = generateSpeakText(instruction.text ?: "", instruction.distance, locale)
      tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, null)
    }
  }

  private fun updateVolumeButtonIcon() {
    if (::volumeFab.isInitialized) {
      if (isSpeaking) {
        volumeFab.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
      } else {
        volumeFab.setImageResource(android.R.drawable.ic_lock_silent_mode)
      }
    }
  }

  override fun onDestroy() {
    mapxusSharedViewModel.mapView.value?.onDestroy()
    // Clean up TTS
    if (::tts.isInitialized) {
      tts.stop()
      tts.shutdown()
    }
    super.onDestroy()
  }

  override fun onLowMemory() {
    mapxusSharedViewModel.mapView.value?.onLowMemory()
    super.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    mapxusSharedViewModel.mapView.value?.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onSensorChanged(p0: SensorEvent?) {}

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
//        controller?.isSensorUnreliable?.value = p1 == SensorManager.SENSOR_STATUS_UNRELIABLE
  }

  // Permission & Settings Methods
  private fun checkAllPermissions() {
    if (!hasAllRequiredPermissions()) {
      requestAllPermissions()
    } else {
      // Check location services
      checkLocationServices()
      // Check precise location on Android 12+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkPreciseLocation()
      }
    }
  }

  private fun hasAllRequiredPermissions(): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCamera = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    return hasFineLocation && hasCoarseLocation && hasCamera
  }

  private fun hasLocationPermissions(): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return hasFineLocation && hasCoarseLocation
  }

  private fun requestAllPermissions() {
    val permissionsToRequest = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
      != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.CAMERA)
    }

    if (permissionsToRequest.isNotEmpty()) {
      requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
  }

  private fun requestLocationPermissions() {
    requestPermissionLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
  }

  private fun isLocationEnabled(): Boolean {
    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }

  private fun checkLocationServices() {
    if (!isLocationEnabled()) {
      showLocationSettingsDialog()
    }
  }

  /**
   * Check if precise location is enabled (Android 12+)
   * On Android 12+, even with ACCESS_FINE_LOCATION permission,
   * the user may have chosen "approximate location" instead of "precise location".
   */
  private fun isPreciseLocationEnabled(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

      // Check if location services are enabled
      val isLocationServiceEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

      if (!isLocationServiceEnabled) {
        return false
      }

      // On Android 12+, check if we have ACCESS_FINE_LOCATION permission
      // This is required for precise location. By default, if ACCESS_FINE_LOCATION
      // is granted, precise location is enabled unless user explicitly disabled it.
      val hasFineLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

      // If ACCESS_FINE_LOCATION is granted and location services are enabled,
      // we assume precise location is enabled (default behavior)
      return hasFineLocation
    }
    // For Android versions below 12, if location permission is granted, it's fine/precise
    return hasLocationPermissions() && isLocationEnabled()
  }

  /**
   * Check if precise location is enabled and prompt if not (Android 12+)
   * Only opens settings if we can confirm precise location is NOT enabled
   */
  private fun checkPreciseLocation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!isPreciseLocationEnabled()) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
          // Request fine location permission for precise location
          requestAllPermissions()
        } else if (!isLocationEnabled()) {
          // Location services not enabled - open location settings
          openLocationSettings()
        }
        // If ACCESS_FINE_LOCATION is granted and location is enabled,
        // we assume precise location is enabled (default behavior)
        // and don't prompt the user
      } else {
        Log.d("XmlActivity", "Precise location is enabled")
      }
    }
  }

  private fun showPermissionDialog() {
    val missingPermissions = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Fine Location")
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Coarse Location")
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Camera")
    }

    val message = if (missingPermissions.isEmpty()) {
      "Some required permissions are missing."
    } else {
      "This app needs the following permissions to work properly:\n\n" +
        missingPermissions.joinToString("\n• ", "• ")
    }

    AlertDialog.Builder(this)
      .setTitle("Permissions Required")
      .setMessage(message)
      .setPositiveButton("Grant Permissions") { _, _ ->
        requestAllPermissions()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun showPreciseLocationDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      AlertDialog.Builder(this)
        .setTitle("Precise Location Required")
        .setMessage("For best accuracy with Mapxus Positioning SDK 2.0.0+, please ensure precise location is enabled.\n\n" +
          "This app requires precise location to avoid ERROR_LOCATION_SERVICE_DISABLED errors.\n\n" +
          "Please go to Settings > Location > App permissions, and ensure \"Precise\" is selected for this app.")
        .setPositiveButton("Open Settings") { _, _ ->
          openAppLocationSettings()
        }
        .setNegativeButton("Later", null)
        .show()
    }
  }

  private fun showLocationSettingsDialog() {
    AlertDialog.Builder(this)
      .setTitle("Location Services Disabled")
      .setMessage("Please enable location services to use this feature.")
      .setPositiveButton("Open Settings") { _, _ ->
        openLocationSettings()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun openLocationSettings() {
    val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    locationSettingsLauncher.launch(locationIntent)
  }

  /**
   * Open app-specific location settings (Android 12+)
   * This allows users to toggle between precise and approximate location
   */
  private fun openAppLocationSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Open app details settings where user can configure location permissions
      // This is the correct way to open app-specific location settings on Android 12+
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
      }
      locationSettingsLauncher.launch(intent)
    } else {
      // Fallback to general location settings for older versions
      openLocationSettings()
    }
  }
}
