package com.mapxushsitp

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapViewProvider
import com.mapxus.map.mapxusmap.api.map.model.CameraPosition
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
import com.mapxushsitp.service.Cleaner
import com.mapxushsitp.service.generateSpeakText
import com.mapxushsitp.service.toMeterText
import com.mapxushsitp.theme.MaterialThemeUtils
import com.mapxushsitp.view.onboarding.OnboardingPage
import com.mapxushsitp.view.onboarding.OnboardingView
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class XmlFragment(
  private var locale: Locale? = null,
) : Fragment() {
  private var localizedContext: Context? = null
  lateinit var mapView: MapView
  lateinit var mapViewProvider: MapViewProvider
  lateinit var userLocation: ImageView
  lateinit var bottomSheet: LinearLayout
  lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
  lateinit var fragmentContainer : FragmentContainerView
  var navHostFragment : NavHostFragment? = null
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
  private val mapxusSharedViewModel: MapxusSharedViewModel by activityViewModels()
  private val arNavigationViewModel: ARNavigationViewModel by activityViewModels()

  // Text-to-Speech
  private lateinit var tts: TextToSpeech
  private var isSpeaking: Boolean = true

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

  private val locationSettingsLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    // User returned from location settings
    checkLocationServices()
  }

  private fun resolveChineseLocale(sourceLocale: Locale): Locale {
    val systemLocale = sourceLocale
    return when {
      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("TW", ignoreCase = true) ->
        Locale("zh", "TW")

      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("HK", ignoreCase = true) ->
        Locale("zh", "HK")

      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("CN", ignoreCase = true) ->
        Locale("zh", "CN")

      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.isNullOrEmpty() ->
        Locale("zh", "SG") // fallback

      else -> systemLocale
    }
  }

  private fun updateLocaleContext(base: Context): Context {
    val desiredLocale = locale ?: Locale.getDefault()
    val newLocale = resolveChineseLocale(desiredLocale)

    // Apply locale globally for this context
    Locale.setDefault(newLocale)

    // Clone configuration with new locale
    val config = Configuration(base.resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      config.setLocales(LocaleList(newLocale))
    } else {
      config.setLocale(newLocale)
    }

    // Create a locale-updated context
    val localizedContext = base.createConfigurationContext(config)

    // ⭐ CRITICAL: Re-apply the Activity theme or MaterialComponents will crash
    val themedContext = ContextThemeWrapper(localizedContext, base.theme)
    return MaterialThemeUtils.ensureMaterialContext(themedContext)
  }


  override fun onAttach(context: Context) {
    // Create localized context after attachment so we can safely access theme
    localizedContext = updateLocaleContext(context)
    mapxusSharedViewModel.context = localizedContext ?: requireContext()
    navController = getNavController()
    super.onAttach(localizedContext ?: context)
  }

  override fun getContext(): Context? {
    return localizedContext ?: super.getContext()
  }

  override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
    val baseInflater = super.onGetLayoutInflater(savedInstanceState)
    val ctx = localizedContext ?: updateLocaleContext(baseInflater.context)
    return baseInflater.cloneInContext(ctx)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d("REACT-MAPXUS", "On Create ${::mapView.isInitialized} ${::bottomSheet.isInitialized} ${::fragmentContainer.isInitialized}")
    super.onCreate(null)
    if (savedInstanceState != null) {
      childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Log.d("REACT-MAPXUS", "On Create View")
    Log.d("REACT-MAPXUS", "${::mapView.isInitialized} ${::bottomSheet.isInitialized} ${::fragmentContainer.isInitialized}")
    // Use the localized context which has the theme preserved
    val ctx = localizedContext ?: updateLocaleContext(requireContext())
    val localizedInflater = inflater.cloneInContext(ctx)
    return localizedInflater.inflate(R.layout.activity_xml, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    Log.d("REACT-MAPXUS", "On View Created")
    super.onViewCreated(view, savedInstanceState)

    mapxusSharedViewModel.selectVehicle(mapxusSharedViewModel.sharedPreferences.getString("vehicle", RoutePlanningVehicle.FOOT) ?: RoutePlanningVehicle.FOOT)
    isSpeaking = mapxusSharedViewModel.sharedPreferences.getBoolean("isSpeaking", true)

    mapxusSharedViewModel.locale = locale ?: Locale.getDefault()

    mapxusSharedViewModel.selectedVehicle = mapxusSharedViewModel.sharedPreferences.getString("vehicle", "") ?: RoutePlanningVehicle.FOOT
    isSpeaking = mapxusSharedViewModel.sharedPreferences.getBoolean("isSpeaking", true)
    initializeTTS()

    // Check all required permissions (location fine, coarse, camera) and precise location
    checkAllPermissions()

    setupMap()
    setupBottomSheet()
    setupNavigationRouteCard()
    mapView.onCreate(null)
    ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
    setupFloatingActionButtons()

    mapViewProvider.getMapxusMapAsync {
      mapxusSharedViewModel.mapxusMap = it
      mapView.post {
        it.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
      }
    }

    val boarded = mapxusSharedViewModel.sharedPreferences.getBoolean("onboardingDone", false)
    if(!boarded) {
      setupWalkthroughOverlay()
    }
    view.post {
      setupNavigation()
      Log.d("REACT-MAPXUS", "On Create ${mapView} ${bottomSheet} ${fragmentContainer}")
    }
  }

  private fun setupNavHostIfNeeded() {
    val fragmentManager = childFragmentManager
    val containerId = R.id.fragment_container

    // Check if a NavHost already exists (from previous attach)
    val existingHost = fragmentManager.findFragmentById(containerId)

    if (existingHost is NavHostFragment) {
      // Already correct host: ensure navController still valid
      if (existingHost.navController.graph == null) {
        existingHost.navController.setGraph(R.navigation.nav_graph)
      }
      return
    }

    // Remove any stray fragments in the container
    if (existingHost != null) {
      fragmentManager.beginTransaction()
        .remove(existingHost)
        .commitNow()
    }

    // Create the NavHostFragment programmatically
    val navHost = NavHostFragment.create(R.navigation.nav_graph)

    fragmentManager.beginTransaction()
      .replace(containerId, navHost, "NavHost_${id}")
      .setPrimaryNavigationFragment(navHost)
      .commitNow()

    // Optional: apply locale to NavHost’s context if needed
//    applyLocaleToHost(navHost)
  }

  fun setupMap() {
    mapView = requireView().findViewById<MapView>(R.id.mapView)
    mapxusSharedViewModel.setMapView(mapView)
    try {
      val surface = mapView.getChildAt(0) as SurfaceView?
      surface?.setZOrderOnTop(false)
      surface?.setZOrderMediaOverlay(true)
      surface?.z = 0f
    } catch (e: Exception) {
      Log.d("REACT-MAPXUS", "Unable to set map always bottom")
    }
    val mapOptions = MapxusMapOptions().apply {
      floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
      zoomLevel = 19.0
    }
    mapViewProvider = MapLibreMapViewProvider(requireContext(), mapView, mapOptions)
    mapView.getMapAsync {
      mapxusSharedViewModel.maplibreMap = it
    }
    mapxusSharedViewModel.setMapViewProvider(mapViewProvider)
    userLocation = requireView().findViewById(R.id.select_location)

    // Initialize positioning only if not already initialized
    // This allows positioning to persist across Fragment recreations
    try {
      mapxusSharedViewModel.initPositioning(viewLifecycleOwner, requireContext())
    } catch (e: Exception) {
      Log.e("REACT-MAPXUS", "Error initializing positioning", e)
      mapxusSharedViewModel.initPositioning(viewLifecycleOwner, requireContext())
    }

    loadingOverlay = requireView().findViewById(R.id.loading_overlay)
    mapxusSharedViewModel.isLoadingRoute.observe(viewLifecycleOwner) {
      loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
    }
  }

  private fun setupWalkthroughOverlay() {
    val onboarding = view?.findViewById<OnboardingView>(R.id.containerOnboarding)
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

    onboarding?.setup(pages)
    onboarding?.visibility = View.VISIBLE
  }

  val didFinishRenderingFrameListener = MapView.OnDidFinishRenderingFrameListener { _,_,_ ->
    bottomSheetBehavior.isHideable = mapxusSharedViewModel.isNavigating
    if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
    if(mapxusSharedViewModel.isNavigating) {
      bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
  }

  val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
    val newHeight = bottomSheet.measuredHeight
    // Only update peekHeight if not in half-expanded state (to preserve half-height setting)

    if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED) {
      if (bottomSheetBehavior.peekHeight != newHeight) {
        bottomSheetBehavior.peekHeight = newHeight
        arNavigationFab.visibility = View.GONE
      } else if(!mapxusSharedViewModel.isNavigating) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        arNavigationFab.visibility = View.GONE
      } else {
        arNavigationFab.visibility = View.VISIBLE
      }
    } else {
      // When in half-expanded state, keep it draggable and maintain visibility
      arNavigationFab.visibility = View.GONE
    }

    if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
  }

  val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
    }
  }

  val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
    Log.d("REACT-MAPXUS", "Destination changed to: ${destination.id}")
    Log.d("REACT-MAPXUS", "Size: ${navHostFragment?.view?.width} ${navHostFragment?.view?.height}")

    // Ensure fragment container is visible first
    fragmentContainer.visibility = View.VISIBLE

    // Measure content and adjust size to match content
    fragmentContainer.post {
      fragmentContainer.requestLayout()
      fragmentContainer.invalidate()

      // Wait for layout to complete, then measure content
      fragmentContainer.post {
        // Force size on NavHostFragment and its children based on content
        navHostFragment?.view?.let { navView ->
          val parentWidth = fragmentContainer.width
          if (parentWidth > 0) {
            // Measure NavHostFragment with UNSPECIFIED height to get actual content height
            val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            navView.measure(widthSpec, heightSpec)

            // Measure current destination fragment to get its actual content size
            var contentHeight = 0
            navHostFragment?.childFragmentManager?.fragments?.forEach { childFragment ->
              childFragment.view?.let { childView ->
                val childWidthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
                val childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                childView.measure(childWidthSpec, childHeightSpec)

                // Use the measured height as the content height
                contentHeight = minOf(contentHeight, childView.measuredHeight)

                Log.d("REACT-MAPXUS", "Destination fragment ${childFragment.javaClass.simpleName} measured size: ${childView.measuredWidth}x${childView.measuredHeight}")
              }
            }

            // If we got a content height, use it; otherwise use measured height
            var finalHeight = if (contentHeight > 0) contentHeight else navView.measuredHeight
            finalHeight = navView.measuredHeight

            // Layout with the measured dimensions
            navView.layout(0, 0, navView.measuredWidth, finalHeight)

            // Update fragment container layout params to match content size
            val containerParams = fragmentContainer.layoutParams
            if (containerParams != null && finalHeight > 0) {
              containerParams.height = finalHeight
              fragmentContainer.layoutParams = containerParams
              fragmentContainer.requestLayout()
            }

            Log.d("REACT-MAPXUS", "NavHostFragment final size: ${navView.width}x${navView.height}, container height: ${containerParams?.height}")
          }
        }
      }
    }

    bottomSheet.post {
      if (destination.id == R.id.showRouteFragment) {
        // Hide bottom sheet for showRouteFragment
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      } else {
        // Show bottom sheet for other destinations
        bottomSheetBehavior.isHideable = false
        // Use a shorter delay to ensure NavHostFragment content is ready
        bottomSheet.postDelayed({
          if (navHostFragment?.view != null && !mapxusSharedViewModel.isNavigating) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
          }
        }, 100) // Reduced from 2000ms to 100ms
      }
    }
  }

  fun setupBottomSheet() {
    bottomSheet = requireView().findViewById<LinearLayout>(R.id.bottomSheet)
    mapxusSharedViewModel.bottomSheet = bottomSheet

    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
    mapxusSharedViewModel.bottomSheetBehavior = bottomSheetBehavior

    bottomSheetBehavior.isHideable = false
    bottomSheetBehavior.isDraggable = true
    bottomSheetBehavior.skipCollapsed = false

    // Ensure bottomSheet is visible and in correct state
    bottomSheet.visibility = View.VISIBLE
    bottomSheet.post {
      // Set state after layout to ensure proper initialization
      if (!mapxusSharedViewModel.isNavigating) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      } else {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      }
    }

    mapView.addOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)

    bottomSheet.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

    bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

    requireActivity().onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if(mapxusSharedViewModel.isNavigating) {
          mapxusSharedViewModel.routePainter?.cleanRoute()
          mapxusSharedViewModel.clearInstructions()
          mapxusSharedViewModel.setInstructionIndex(0)
          mapxusSharedViewModel.isNavigating = false
        } else if(navController?.currentDestination?.route != "venue_screen") {
          if(navController?.currentDestination?.id == R.id.poiDetailsFragment) {
            mapxusSharedViewModel.mapxusMap?.removeMapxusPointAnnotations()
          }
          navController?.navigateUp()
          bottomSheet.post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
          }
        } else {
          val containerView = requireView().parent as? ViewGroup
//          containerView?.removeAllViews()   // remove fragment UI
          parentFragmentManager.beginTransaction()
            .remove(this@XmlFragment)
            .commitAllowingStateLoss()
        }
      }
    })
  }

  fun setupNavigation() {
    fragmentContainer = requireView().findViewById(R.id.fragment_container)

    // NUCLEAR OPTION: Remove ALL fragments completely
    childFragmentManager.fragments.toList().forEach { fragment ->
      // Clear the fragment's view reference
      fragment.view?.let { view ->
        // Remove from parent
        (view.parent as? ViewGroup)?.removeView(view)

        // Clear fragment's internal view reference
        try {
          val mViewField = Fragment::class.java.getDeclaredField("mView")
          mViewField.isAccessible = true
          mViewField.set(fragment, null)  // ← CRITICAL!
        } catch (e: Exception) {
          Log.e("REACT-MAPXUS", "Error clearing fragment view", e)
        }
      }

      // Remove fragment from FragmentManager
      childFragmentManager.beginTransaction()
        .remove(fragment)
        .commitNowAllowingStateLoss()
    }

    // Always ensure fragmentContainer is visible first
    fragmentContainer.visibility = View.VISIBLE

    // Ensure fragment container has proper layout params
    val containerParams = fragmentContainer.layoutParams
    if (containerParams != null) {
      containerParams.width = ViewGroup.LayoutParams.MATCH_PARENT
      containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
      fragmentContainer.layoutParams = containerParams
    }

    // Get existing NavHostFragment if it exists
    navHostFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment

    if (navHostFragment == null) {
      // Create new NavHostFragment only if it doesn't exist
      navHostFragment = NavHostFragment.create(R.navigation.nav_graph)

      childFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, navHostFragment!!, "nav_host_fragment")
        .setPrimaryNavigationFragment(navHostFragment!!)
        .commitNow()
    }

    navController = navHostFragment?.navController

    // Only set graph if it hasn't been set yet
    if (navController?.graph == null) {
      navController?.setGraph(R.navigation.nav_graph)
    }

    // Ensure NavHostFragment and its children have proper size based on content
    fragmentContainer.post {
      // Wait for layout to complete
      fragmentContainer.post {
        // Force layout pass
        fragmentContainer.requestLayout()
        fragmentContainer.invalidate()

        // Measure content to get actual size
        navHostFragment?.view?.let { navView ->
          val parentWidth = fragmentContainer.width

          if (parentWidth > 0) {
            // Measure with UNSPECIFIED height to get actual content height
            val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

            navView.measure(widthSpec, heightSpec)

            // Measure child fragments to get their actual content size
            var contentHeight = 0
            navHostFragment?.childFragmentManager?.fragments?.forEach { childFragment ->
              childFragment.view?.let { childView ->
                val childWidthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
                val childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

                childView.measure(childWidthSpec, childHeightSpec)
                contentHeight = minOf(contentHeight, childView.measuredHeight)

                // Layout child with measured size
                childView.layout(0, 0, childView.measuredWidth, childView.measuredHeight)

                Log.d("REACT-MAPXUS", "Child fragment ${childFragment.javaClass.simpleName} measured size: ${childView.measuredWidth}x${childView.measuredHeight}")
              }
            }

            // Use content height if available, otherwise use measured height
            val finalHeight = if (contentHeight > 0) contentHeight else navView.measuredHeight

            // Layout NavHostFragment with measured dimensions
            navView.layout(0, 0, navView.measuredWidth, finalHeight)

            // Update fragment container to match content size
            val containerParams = fragmentContainer.layoutParams
            if (containerParams != null && finalHeight > 0) {
              containerParams.height = finalHeight
              fragmentContainer.layoutParams = containerParams
              fragmentContainer.requestLayout()
            }

            Log.d("REACT-MAPXUS", "NavHostFragment final size: ${navView.width}x${navView.height}, container height: ${containerParams?.height}")
          }
        }
      }
    }

    // Listen for destination changes
    navController?.addOnDestinationChangedListener(destinationChangedListener)

    mapxusSharedViewModel.navController = navController
  }

  fun setupNavigation3() {
    fragmentContainer = requireView().findViewById(R.id.fragment_container)

    // 1. Force-remove old nav host if RN left the screen earlier
    val existing = childFragmentManager.findFragmentById(R.id.fragment_container)
    if (existing != null) {
      childFragmentManager.beginTransaction().setReorderingAllowed(true)
        .detach(existing)
        .remove(existing)
        .commitNow()
    }

    // 2. Create a brand-new NavHostFragment
    val newNavHost = NavHostFragment.create(R.navigation.nav_graph)
    childFragmentManager.beginTransaction()
      .replace(R.id.fragment_container, newNavHost)
      .setPrimaryNavigationFragment(newNavHost)
      .show(newNavHost)
      .commitNow()

    // 3. Assign controller
    navHostFragment = childFragmentManager
      .findFragmentById(R.id.fragment_container) as? NavHostFragment

    navController = navHostFragment?.navController

    // Don't reset the graph if it's already set - this clears the current destination
    navController?.let { controller ->
      if (controller.graph == null) {
        controller.setGraph(R.navigation.nav_graph)
      }

      // Navigate to start destination if not already there
      val currentDestination = controller.currentDestination
      val startDestination = controller.graph?.startDestinationId

      if (currentDestination == null || (startDestination != null && currentDestination.id != startDestination)) {
        try {
          controller.navigate(startDestination ?: R.id.venueScreenFragment)
        } catch (e: Exception) {
          Log.e("REACT-MAPXUS", "Error navigating to start destination", e)
        }
      }
    }

    fragmentContainer.visibility = View.VISIBLE

    // 4. Register listener after navigation is set up
    navController?.addOnDestinationChangedListener(destinationChangedListener)

    mapxusSharedViewModel.navController = navController
  }



  fun setupNavigation2() {
    fragmentContainer = requireView().findViewById(R.id.fragment_container)

    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {

      // 1. Remove any existing fragment completely
      val old = childFragmentManager.findFragmentById(R.id.fragment_container)
      if (old != null) {
        childFragmentManager.beginTransaction()
          .remove(old)
          .commitNow()
      }

      // 2. Recreate new host
      val newHost = NavHostFragment.create(R.navigation.nav_graph)

      childFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, newHost)
        .commitNow()

      val host = childFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
      val controller = host.navController

      // 3. Show
      fragmentContainer.visibility = View.VISIBLE

      controller.navigate(R.id.action_global_to_venue)
      mapxusSharedViewModel.navController = controller
      controller.addOnDestinationChangedListener(destinationChangedListener)
    }
  }

  private fun setupFloatingActionButtons() {
    gpsFab = requireView().findViewById(R.id.gps_fab)
    volumeFab = requireView().findViewById(R.id.volume_fab)
    arNavigationFab = requireView().findViewById(R.id.ar_navigation_fab)
    arFragmentContainer = requireView().findViewById(R.id.ar_fragment_container)

    // GPS button - center on user location
    gpsFab.setOnClickListener {
      if (hasLocationPermissions()) {
        if (isLocationEnabled()) {
          mapxusSharedViewModel.mapxusMap?.let { mapxusMap ->
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

    mapxusSharedViewModel.instructionList.observe(viewLifecycleOwner) { instructions ->
      val isNavigating = instructions.isNotEmpty()

      volumeFab.visibility = if (isNavigating) View.VISIBLE else View.GONE

      if (isNavigating) {
        arNavigationViewModel.isShowingOpeningAndClosingARButton.value = true
      }
    }

  }

  private fun setupNavigationRouteCard() {
    navigationRouteCard = requireView().findViewById(R.id.navigation_route_card)
    navTitleText = requireView().findViewById(R.id.nav_title_text)
    navDistanceText = requireView().findViewById(R.id.nav_distance_text)
    navTimeText = requireView().findViewById(R.id.nav_time_text)
    navIcon = requireView().findViewById(R.id.nav_icon)
    navPreviousButton = requireView().findViewById(R.id.nav_previous_button)
    navNextButton = requireView().findViewById(R.id.nav_next_button)
    stepIndicatorsContainer = requireView().findViewById(R.id.step_indicators_container)

    // Previous button click
    navPreviousButton.setOnClickListener {
      mapxusSharedViewModel.previousStep()
      arNavigationViewModel.prevInstruction()
    }

    // Next button click
    navNextButton.setOnClickListener {
      mapxusSharedViewModel.nextStep()
      arNavigationViewModel.nextInstruction(mapxusSharedViewModel.instructionList.value?.size ?: 0)
    }

    // Observe instruction index and update card

    // Observe instruction list and index reactively
    mapxusSharedViewModel.instructionList.observe(viewLifecycleOwner) { instructionList ->
      val instructionIndex = mapxusSharedViewModel.instructionIndex.value ?: 0
      val isNavigating = mapxusSharedViewModel.isNavigating

      updateNavigationUI(instructionList, instructionIndex, isNavigating)
    }

    mapxusSharedViewModel.instructionIndex.observe(viewLifecycleOwner) { instructionIndex ->
      val instructionList = mapxusSharedViewModel.instructionList.value.orEmpty()
      val isNavigating = mapxusSharedViewModel.isNavigating

      updateNavigationUI(instructionList, instructionIndex, isNavigating)

      // Speak instruction when index changes during navigation
      if (isNavigating && instructionList.isNotEmpty() && instructionIndex in instructionList.indices) {
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
      val indicator = View(getContext() ?: requireContext()).apply {
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
            getContext() ?: requireContext(),
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
        navTimeText.text = resources.getString(R.string.minute, (estimatedSeconds/60).toInt())
      else
        navTimeText.text = resources.getString(R.string.second, estimatedSeconds)

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
    if (arriveAtDestinationDialog?.isShowing == true) {
      return
    }

    val dialogView = layoutInflater.inflate(R.layout.dialog_arrive_at_destination, null)

    val btnGoPrevious = dialogView.findViewById<TextView>(R.id.btn_go_previous)
    val btnFinished = dialogView.findViewById<TextView>(R.id.btn_finished)

    arriveAtDestinationDialog = AlertDialog.Builder(requireActivity())
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

    if (isAdded && !requireActivity().isFinishing && !requireActivity().isDestroyed) {
      arriveAtDestinationDialog?.show()
    }
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

    val instructionListSerializable = mapxusSharedViewModel.instructionList.value?.mapNotNull {
      it.floorId?.let { floorId ->
        SerializableRouteInstruction(it.text, it.distance, floorId)
      }
    }

    val instructionPointList : MutableList<RoutePlanningPoint> = mutableListOf()
    mapxusSharedViewModel.instructionList.value?.forEachIndexed { index, instruction ->
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

    childFragmentManager.beginTransaction()
      .replace(R.id.ar_fragment_container, fragment)
      .commit()

    arFragmentContainer.visibility = View.VISIBLE
  }

  private fun hideARFragment() {
    arFragmentContainer.visibility = View.GONE
  }

  fun getNavController(): NavController? {
    return navController ?: run {
      val navHostFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
      return navHostFragment?.navController
    }
  }

  fun getSharedViewModel(): MapxusSharedViewModel {
    return mapxusSharedViewModel
  }

  override fun onStart() {
    Log.d("REACT-MAPXUS", "On Start")
    super.onStart()
    mapxusSharedViewModel.mapView.value?.onStart()
  }

  override fun onResume() {
    Log.d("REACT-MAPXUS", "On Resume")
    super.onResume()
    mapxusSharedViewModel.mapView.value?.onResume()
  }

  override fun onPause() {
    Log.d("REACT-MAPXUS", "On Pause")
    mapxusSharedViewModel.mapView.value?.onPause()
    super.onPause()
  }

  override fun onStop() {
    Log.d("REACT-MAPXUS", "On Stop")
    mapxusSharedViewModel.mapView.value?.onStop()
    super.onStop()
  }

  private fun initializeTTS() {
    tts = TextToSpeech(getContext() ?: requireContext()) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val locale = mapxusSharedViewModel.locale
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

  override fun onDestroyView() {
    mapView.removeOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)
    mapxusSharedViewModel.mapView.value?.removeOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)

    Cleaner.clearAllStaticReferences()

    bottomSheet.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    // ALSO remove from view's observer (safety net)
    view?.viewTreeObserver?.let { observer ->
      if (observer.isAlive) {
        observer.removeOnGlobalLayoutListener(onGlobalLayoutListener)
      }
    }

    // ALSO remove from root view's observer (this is where the leak is!)
    activity?.window?.decorView?.viewTreeObserver?.let { observer ->
      if (observer.isAlive) {
        observer.removeOnGlobalLayoutListener(onGlobalLayoutListener)
      }
    }

    if (::bottomSheetBehavior.isInitialized) {
      bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    // Clean up child fragments (including NavHostFragment)
    childFragmentManager.fragments.toList().forEach { fragment ->
      childFragmentManager.beginTransaction()
        .remove(fragment)
        .commitNowAllowingStateLoss()
    }

    navController?.removeOnDestinationChangedListener(destinationChangedListener)

    viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
    localizedContext = null

    // Clear view references in ViewModel to allow proper reinitialization
    // when Fragment is recreated. Don't call destroy() as it stops services.
    mapxusSharedViewModel.clearViewReferences()

    Log.d("REACT-MAPXUS", "On Destroy View " + navController)
    super.onDestroyView()
  }

  override fun onDestroy() {
    Log.d("REACT-MAPXUS", "On Destroy")
    clearMapxusStaticReference()
    Cleaner.clearAllStaticReferences()
//    mapxusSharedViewModel.destroy()

    navHostFragment = null
    navController = null

    // Clean up TTS
    if (::tts.isInitialized) {
      tts.stop()
      tts.shutdown()
    }

    mapView.onDestroy()
    Log.d("HostFragmentCheck", "onDestroy: $this hash=${this.hashCode()}")
    super.onDestroy()
  }

  override fun onLowMemory() {
    Log.d("REACT-MAPXUS", "On LowMem")
    mapxusSharedViewModel.mapView.value?.onLowMemory()
    super.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    Log.d("REACT-MAPXUS", "On Save Instance")
    mapxusSharedViewModel.mapView.value?.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
  }

  private fun clearMapxusStaticReference() {
    try {
      Log.d("XmlFragment", "Clearing static LIFECYCLE_OWNER")
      val mapxusClientClass = Class.forName(
        "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
      )
      val lifecycleOwnerField = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
      lifecycleOwnerField.isAccessible = true

      val currentOwner = lifecycleOwnerField.get(null)
      if (currentOwner == this) {
        lifecycleOwnerField.set(null, null)
        Log.d("XmlFragment", "Cleared static LIFECYCLE_OWNER")
      }
    } catch (e: Exception) {
      Log.e("XmlFragment", "Failed to clear static reference", e)
    }
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
      requireContext(),
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCamera = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    return hasFineLocation && hasCoarseLocation && hasCamera
  }

  private fun hasLocationPermissions(): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return hasFineLocation && hasCoarseLocation
  }

  private fun requestAllPermissions() {
    val permissionsToRequest = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
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
    val locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
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
      val locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager

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
        requireContext(),
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
          requireContext(),
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
        Log.d("REACT-MAPXUS", "Precise location is enabled")
      }
    }
  }

  private fun showPermissionDialog() {
    val missingPermissions = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Fine Location")
    }

    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Coarse Location")
    }

    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.CAMERA)
      != PackageManager.PERMISSION_GRANTED) {
      missingPermissions.add("Camera")
    }

    val message = if (missingPermissions.isEmpty()) {
      "Some required permissions are missing."
    } else {
      "This app needs the following permissions to work properly:\n\n" +
        missingPermissions.joinToString("\n• ", "• ")
    }

    AlertDialog.Builder(getContext() ?: requireContext())
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
      AlertDialog.Builder(getContext() ?: requireContext())
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
    AlertDialog.Builder(getContext() ?: requireContext())
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
        data = android.net.Uri.fromParts("package", (getContext() ?: requireContext()).packageName, null)
      }
      locationSettingsLauncher.launch(intent)
    } else {
      // Fallback to general location settings for older versions
      openLocationSettings()
    }
  }

}
