package com.mapxushsitp
//
//import android.Manifest
//import android.content.Context
//import android.content.Context.LOCATION_SERVICE
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.content.res.ColorStateList
//import android.content.res.Configuration
//import android.graphics.Color
//import android.graphics.drawable.GradientDrawable
//import android.location.LocationManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.LocaleList
//import android.os.Looper
//import android.provider.Settings
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.view.ContextThemeWrapper
//import android.view.LayoutInflater
//import android.view.SurfaceView
//import android.view.View
//import android.view.ViewGroup
//import android.view.ViewTreeObserver
//import android.widget.LinearLayout
//import android.widget.TextView
//import androidx.activity.OnBackPressedCallback
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AlertDialog
//import androidx.core.content.ContextCompat
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentManager
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavController
//import androidx.navigation.fragment.NavHostFragment
//import com.google.android.material.bottomsheet.BottomSheetBehavior
//import com.mapxus.map.mapxusmap.api.map.FollowUserMode
//import com.mapxus.map.mapxusmap.api.map.MapViewProvider
//import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
//import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
//import com.mapxus.map.mapxusmap.api.services.model.planning.InstructionDto
//import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
//import com.mapxus.map.mapxusmap.impl.MapLibreMapViewProvider
//import com.mapxushsitp.arComponents.ARNavigationViewModel
//import com.mapxushsitp.arComponents.FourthLocalARFragment
//import com.mapxushsitp.data.model.ParcelizeRoutePoint
//import com.mapxushsitp.data.model.SerializableRouteInstruction
//import com.mapxushsitp.data.model.SerializableRoutePoint
//import com.mapxushsitp.databinding.ActivityXmlBinding
//import com.mapxushsitp.service.Cleaner
//import com.mapxushsitp.service.generateSpeakText
//import com.mapxushsitp.service.toMeterText
//import com.mapxushsitp.theme.MaterialThemeUtils
//import com.mapxushsitp.view.onboarding.OnboardingPage
//import com.mapxushsitp.view.onboarding.OnboardingView
//import com.mapxushsitp.viewmodel.MapxusSharedViewModel
//import kotlinx.coroutines.cancelChildren
//import org.maplibre.android.maps.MapView
//import java.util.Locale
//import kotlin.math.absoluteValue
//import kotlin.math.roundToInt
//
//class XmlFragment(
//  private var locale: Locale? = null,
//) : Fragment() {
//  var _binding : ActivityXmlBinding? = null
//  val binding get() = _binding!!
//
//  var mapViewProvider: MapViewProvider? = null
//  var bottomSheetBehavior = BottomSheetBehavior<LinearLayout>()
//
//  private var localizedContext: Context? = null
//  var navHostFragment : NavHostFragment? = null
//  private var navController: NavController? = null
//
//  // UI Elements
//
//  private var arriveAtDestinationDialog: AlertDialog? = null
//
//  // Store adapter observers to avoid leaks
//  private val adapterObservers = mutableMapOf<androidx.recyclerview.widget.RecyclerView, androidx.recyclerview.widget.RecyclerView.AdapterDataObserver>()
//  private val scrollListeners = mutableMapOf<androidx.recyclerview.widget.RecyclerView, androidx.recyclerview.widget.RecyclerView.OnScrollListener>()
//
//  // Shared ViewModel
//  private val mapxusSharedViewModel: MapxusSharedViewModel by activityViewModels()
//  private val arNavigationViewModel: ARNavigationViewModel by activityViewModels()
//
//  // Text-to-Speech
//  private lateinit var tts: TextToSpeech
//  private var isSpeaking: Boolean = true
//
//  // Permission launcher
//  private val requestPermissionLauncher = registerForActivityResult(
//    ActivityResultContracts.RequestMultiplePermissions()
//  ) { permissions ->
//    val allGranted = permissions.entries.all { it.value }
//    if (!allGranted) {
//      showPermissionDialog()
//    } else {
//      // After permissions granted, check for precise location on Android 12+
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        checkPreciseLocation()
//      }
//    }
//  }
//
//  private val locationSettingsLauncher = registerForActivityResult(
//    ActivityResultContracts.StartActivityForResult()
//  ) {
//    // User returned from location settings
//    checkLocationServices()
//  }
//
//  private fun resolveChineseLocale(sourceLocale: Locale): Locale {
//    val systemLocale = sourceLocale
//    return when {
//      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("TW", ignoreCase = true) ->
//        Locale("zh", "TW")
//
//      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("HK", ignoreCase = true) ->
//        Locale("zh", "HK")
//
//      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.equals("CN", ignoreCase = true) ->
//        Locale("zh", "CN")
//
//      systemLocale.language.equals("zh", ignoreCase = true) && systemLocale.country.isNullOrEmpty() ->
//        Locale("zh", "SG") // fallback
//
//      else -> systemLocale
//    }
//  }
//
//  private fun updateLocaleContext(base: Context): Context {
//    val desiredLocale = locale ?: Locale.getDefault()
//    val newLocale = resolveChineseLocale(desiredLocale)
//
//    // Apply locale globally for this context
//    Locale.setDefault(newLocale)
//
//    // Clone configuration with new locale
//    val config = Configuration(base.resources.configuration)
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//      config.setLocales(LocaleList(newLocale))
//    } else {
//      config.setLocale(newLocale)
//    }
//
//    // Create a locale-updated context
//    val localizedContext = base.createConfigurationContext(config)
//
//    // ⭐ CRITICAL: Re-apply the Activity theme or MaterialComponents will crash
//    val themedContext = ContextThemeWrapper(localizedContext, base.theme)
//    return MaterialThemeUtils.ensureMaterialContext(themedContext)
//  }
//
//
//  override fun onAttach(context: Context) {
//    // Create localized context after attachment so we can safely access theme
//    localizedContext = updateLocaleContext(context)
//    mapxusSharedViewModel.context = localizedContext ?: requireContext()
//    navController = getNavController()
//    super.onAttach(localizedContext ?: context)
//  }
//
//  override fun getContext(): Context? {
//    return localizedContext ?: super.getContext()
//  }
//
//  override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
//    val baseInflater = super.onGetLayoutInflater(savedInstanceState)
//    val ctx = localizedContext ?: updateLocaleContext(baseInflater.context)
//    return baseInflater.cloneInContext(ctx)
//  }
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(null)
//    if (savedInstanceState != null) {
//      childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
//    }
//  }
//
//  override fun onCreateView(
//    inflater: LayoutInflater,
//    container: ViewGroup?,
//    savedInstanceState: Bundle?
//  ): View? {
//    // Use the localized context which has the theme preserved
//    val ctx = localizedContext ?: updateLocaleContext(requireContext())
//    val localizedInflater = inflater.cloneInContext(ctx)
//    _binding = ActivityXmlBinding.inflate(localizedInflater, container, false)
//    return binding.root
//  }
//
//  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, null)
//
//    mapxusSharedViewModel.selectVehicle(mapxusSharedViewModel.sharedPreferences?.getString("vehicle", RoutePlanningVehicle.FOOT) ?: RoutePlanningVehicle.FOOT)
//    isSpeaking = mapxusSharedViewModel.sharedPreferences?.getBoolean("isSpeaking", true) ?: true
//
//    mapxusSharedViewModel.locale = locale ?: Locale.getDefault()
//
//    mapxusSharedViewModel.selectedVehicle = mapxusSharedViewModel.sharedPreferences?.getString("vehicle", "") ?: RoutePlanningVehicle.FOOT
//    isSpeaking = mapxusSharedViewModel.sharedPreferences?.getBoolean("isSpeaking", true) ?: true
//    initializeTTS()
//
//    // Check all required permissions (location fine, coarse, camera) and precise location
//    checkAllPermissions()
//
//    setupMap()
//    setupBottomSheet()
//    setupNavigationRouteCard()
//    binding.mapView.onCreate(null)
//    ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
//      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//      insets
//    }
//    setupFloatingActionButtons()
//
//    mapViewProvider?.getMapxusMapAsync {
//      mapxusSharedViewModel.mapxusMap = it
//      binding.mapView.post {
//        it.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
//      }
//    }
//
//    val boarded = mapxusSharedViewModel.sharedPreferences?.getBoolean("onboardingDone", false) ?: false
//    if(!boarded) {
//      setupWalkthroughOverlay()
//    }
//    view.post {
//      setupNavigation()
//    }
//  }
//
//  private fun setupNavHostIfNeeded() {
//    val fragmentManager = childFragmentManager
//    val containerId = R.id.fragment_container
//
//    // Check if a NavHost already exists (from previous attach)
//    val existingHost = fragmentManager.findFragmentById(containerId)
//
//    if (existingHost is NavHostFragment) {
//      // Already correct host: ensure navController still valid
//      if (existingHost.navController.graph == null) {
//        existingHost.navController.setGraph(R.navigation.nav_graph)
//      }
//      return
//    }
//
//    // Remove any stray fragments in the container
//    if (existingHost != null) {
//      fragmentManager.beginTransaction()
//        .remove(existingHost)
//        .commitNow()
//    }
//
//    // Create the NavHostFragment programmatically
//    val navHost = NavHostFragment.create(R.navigation.nav_graph)
//
//    fragmentManager.beginTransaction()
//      .replace(containerId, navHost, "NavHost_${id}")
//      .setPrimaryNavigationFragment(navHost)
//      .commitNow()
//
//    // Optional: apply locale to NavHost’s context if needed
////    applyLocaleToHost(navHost)
//  }
//
//  fun setupMap() {
//    mapxusSharedViewModel.setMapView(binding.mapView)
//    try {
//      val surface = binding.mapView.getChildAt(0) as SurfaceView?
//      surface?.setZOrderOnTop(false)
//      surface?.setZOrderMediaOverlay(true)
//      surface?.z = 0f
//    } catch (e: Exception) {
//      Log.d("REACT-MAPXUS", "Unable to set map always bottom")
//    }
//    val mapOptions = MapxusMapOptions().apply {
//      floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
//      zoomLevel = 19.0
//    }
//    mapViewProvider = MapLibreMapViewProvider(requireContext(), binding.mapView, mapOptions)
//    binding.mapView.getMapAsync {
//      mapxusSharedViewModel.maplibreMap = it
//    }
//    if(mapViewProvider != null) {
//      mapxusSharedViewModel.setMapViewProvider(mapViewProvider!!)
//    }
//
//    // Initialize positioning only if not already initialized
//    // This allows positioning to persist across Fragment recreations
//    try {
//      mapxusSharedViewModel.initPositioning()
//    } catch (e: Exception) {
//      Log.e("REACT-MAPXUS", "Error initializing positioning", e)
//      mapxusSharedViewModel.initPositioning()
//    }
//
//    mapxusSharedViewModel.isLoadingRoute.observe(viewLifecycleOwner) {
//      binding.loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
//    }
//  }
//
//  private fun setupWalkthroughOverlay() {
//    val onboarding = view?.findViewById<OnboardingView>(R.id.containerOnboarding)
//    val pages = listOf(
//      OnboardingPage(
//        imageRes = R.drawable.baseline_my_location_24,
//        title = "Welcome to Mapxus!",
//        subtitle = "Take a look and discover what we can do for you.",
//        description = "We help you navigate... Outdoor location will be using GPS."
//      ),
//      OnboardingPage(
//        imageRes = R.drawable.figure_8_compass_calibration,
//        title = "Device Calibration",
//        subtitle = "Keep your compass accurate",
//        description = "Make sure your device compass accurate by doing calibration.",
//        isGif = true
//      ),
//      OnboardingPage(
//        imageRes = R.drawable.baseline_location_on_24,
//        title = "Explore the Map",
//        subtitle = "Browse any nearby places",
//        description = "Explore the locations around you...",
//      )
//    )
//
//    onboarding?.setup(pages)
//    onboarding?.visibility = View.VISIBLE
//  }
//
//  val didFinishRenderingFrameListener = MapView.OnDidFinishRenderingFrameListener { _,_,_ ->
//    bottomSheetBehavior.isHideable = mapxusSharedViewModel.isNavigating
//    if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
//      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//    }
//    if(mapxusSharedViewModel.isNavigating) {
//      bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//    }
//  }
//
//  val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
//    val newHeight = binding.bottomSheet.measuredHeight
//    // Only update peekHeight if not in half-expanded state (to preserve half-height setting)
//
//    if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED) {
//      if (bottomSheetBehavior.peekHeight != newHeight) {
//        bottomSheetBehavior.peekHeight = newHeight
//        binding.arNavigationFab.visibility = View.GONE
//      } else if(!mapxusSharedViewModel.isNavigating) {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        binding.arNavigationFab.visibility = View.GONE
//      } else {
//        binding.arNavigationFab.visibility = View.VISIBLE
//      }
//    } else {
//      // When in half-expanded state, keep it draggable and maintain visibility
//      binding.arNavigationFab.visibility = View.GONE
//    }
//
//    if(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED && bottomSheetBehavior.state != BottomSheetBehavior.STATE_DRAGGING && !mapxusSharedViewModel.isNavigating) {
//      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//    }
//  }
//
//  val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
//    override fun onStateChanged(bottomSheet: View, newState: Int) {
//    }
//
//    override fun onSlide(bottomSheet: View, slideOffset: Float) {
//    }
//  }
//
//  /**
//   * Re-measures the fragment container and adjusts its size to match content.
//   * This should be called when content changes (e.g., RecyclerView data updates).
//   */
//  private fun remeasureFragmentContainer() {
//    binding.fragmentContainer.post {
//      binding.fragmentContainer.requestLayout()
//      binding.fragmentContainer.invalidate()
//
//      // Wait for layout to complete, then measure content
//      binding.fragmentContainer.post {
//        remeasureFragmentContainerInternal()
//      }
//    }
//  }
//
//  /**
//   * Internal method that performs the actual measurement.
//   */
//  private fun remeasureFragmentContainerInternal() {
//    navHostFragment?.view?.let { navView ->
//      val parentWidth = binding.fragmentContainer.width
//      if (parentWidth > 0) {
//        // Measure NavHostFragment with UNSPECIFIED height to get actual content height
//        val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
//        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        navView.measure(widthSpec, heightSpec)
//
//        // Measure current destination fragment to get its actual content size
//        var contentHeight = 0
//        navHostFragment?.childFragmentManager?.fragments?.forEach { childFragment ->
//          childFragment.view?.let { childView ->
//            val childWidthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
//            val childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//
//            // Check if this view contains a RecyclerView
//            val recyclerView = findRecyclerView(childView)
//            if (recyclerView != null && recyclerView.adapter != null) {
//              // Measure RecyclerView with its actual content
//              val recyclerWidthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
//              val recyclerHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//              recyclerView.measure(recyclerWidthSpec, recyclerHeightSpec)
//
//              // Calculate RecyclerView content height
//              var recyclerContentHeight = recyclerView.measuredHeight
//              val layoutManager = recyclerView.layoutManager
//              val adapter = recyclerView.adapter
//
//              if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager && adapter != null) {
//                // For LinearLayoutManager, calculate total height of all items
//                if (adapter.itemCount > 0) {
//                  var totalItemHeight = 0
//
//                  // Calculate height from visible items
//                  if (layoutManager.childCount > 0) {
//                    for (i in 0 until layoutManager.childCount) {
//                      val child = layoutManager.getChildAt(i)
//                      if (child != null) {
//                        totalItemHeight += child.height
//                      }
//                    }
//                  }
//
//                  // If we have more items than visible, estimate total height
//                  if (adapter.itemCount > layoutManager.childCount && layoutManager.childCount > 0) {
//                    val avgItemHeight = if (layoutManager.childCount > 0) {
//                      totalItemHeight / layoutManager.childCount
//                    } else {
//                      0
//                    }
//                    // Estimate total height based on average item height
//                    val estimatedTotalHeight = avgItemHeight * adapter.itemCount
//                    totalItemHeight = maxOf(totalItemHeight, estimatedTotalHeight)
//                  }
//
//                  // Add padding and margins
//                  totalItemHeight += recyclerView.paddingTop + recyclerView.paddingBottom
//
//                  // Use the larger of measured height or calculated item height
//                  recyclerContentHeight = maxOf(recyclerContentHeight, totalItemHeight)
//                }
//              }
//
//              // Measure the parent view (excluding RecyclerView) to get other views' height
//              // We'll measure the child view first, then adjust for RecyclerView
//              childView.measure(childWidthSpec, childHeightSpec)
//
//              // Find RecyclerView's position in the parent to calculate other views' height
//              var otherViewsHeight = 0
//              if (childView is ViewGroup) {
//                for (i in 0 until childView.childCount) {
//                  val child = childView.getChildAt(i)
//                  if (child != recyclerView && child.visibility != View.GONE) {
//                    val childViewWidthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
//                    val childViewHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//                    child.measure(childViewWidthSpec, childViewHeightSpec)
//                    otherViewsHeight += child.measuredHeight
//                  }
//                }
//                // Add parent padding
//                otherViewsHeight += childView.paddingTop + childView.paddingBottom
//              } else {
//                // If not a ViewGroup, use measured height minus RecyclerView height
//                otherViewsHeight = childView.measuredHeight - recyclerContentHeight
//              }
//
//              contentHeight = maxOf(contentHeight, otherViewsHeight + recyclerContentHeight)
//            } else {
//              // Regular view measurement
//              childView.measure(childWidthSpec, childHeightSpec)
//              contentHeight = maxOf(contentHeight, childView.measuredHeight)
//            }
//
//            // Layout with measured dimensions
//            childView.layout(0, 0, childView.measuredWidth, contentHeight)
//
//            Log.d("REACT-MAPXUS", "Destination fragment ${childFragment.javaClass.simpleName} measured size: ${childView.measuredWidth}x${contentHeight}")
//          }
//        }
//
//        // Use content height if available, otherwise use measured height
//        val finalHeight = if (contentHeight > 0) contentHeight else navView.measuredHeight
//
//        // Layout with the measured dimensions
//        navView.layout(0, 0, navView.measuredWidth, finalHeight)
//
//        // Update fragment container layout params to match content size
//        val containerParams = binding.fragmentContainer.layoutParams
//        if (containerParams != null && finalHeight > 0) {
//          val oldHeight = containerParams.height
//          containerParams.height = finalHeight
//          binding.fragmentContainer.layoutParams = containerParams
//
//          // Only request layout if height actually changed
//          if (oldHeight != finalHeight) {
//            binding.fragmentContainer.requestLayout()
//            // Also update bottom sheet peek height after a delay to ensure layout is complete
//            binding.bottomSheet.postDelayed({
//              val newPeekHeight = binding.fragmentContainer.height
//              if (newPeekHeight > 0 && bottomSheetBehavior.peekHeight != newPeekHeight) {
//                bottomSheetBehavior.peekHeight = newPeekHeight
//              }
//            }, 50)
//          }
//        }
//
//        Log.d("REACT-MAPXUS", "NavHostFragment final size: ${navView.width}x${navView.height}, container height: ${containerParams?.height}")
//      }
//    }
//  }
//
//  /**
//   * Recursively finds a RecyclerView in the view hierarchy.
//   */
//  private fun findRecyclerView(view: View): androidx.recyclerview.widget.RecyclerView? {
//    if (view is androidx.recyclerview.widget.RecyclerView) {
//      return view
//    }
//    if (view is ViewGroup) {
//      for (i in 0 until view.childCount) {
//        val child = view.getChildAt(i)
//        val recyclerView = findRecyclerView(child)
//        if (recyclerView != null) {
//          return recyclerView
//        }
//      }
//    }
//    return null
//  }
//
//  /**
//   * Sets up listeners on RecyclerViews to detect when content changes and trigger re-measurement.
//   */
//  private fun setupRecyclerViewContentListeners() {
//    // Use a delayed post to ensure fragments are created
//    binding.fragmentContainer.postDelayed({
//      // Find all RecyclerViews in current child fragments
//      navHostFragment?.childFragmentManager?.fragments?.forEach { childFragment ->
//        childFragment.view?.let { childView ->
//          val recyclerView = findRecyclerView(childView)
//          recyclerView?.let { rv ->
//            // Remove old observer if exists
//            adapterObservers[rv]?.let { oldObserver ->
//              rv.adapter?.unregisterAdapterDataObserver(oldObserver)
//            }
//            scrollListeners[rv]?.let { oldListener ->
//              rv.removeOnScrollListener(oldListener)
//            }
//
//            // Create and register new adapter data observer to detect data changes
//            val dataObserver = object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
//              override fun onChanged() {
//                super.onChanged()
//                // Data changed, re-measure after layout
//                binding.fragmentContainer.postDelayed({
//                  remeasureFragmentContainer()
//                }, 200)
//              }
//
//              override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                super.onItemRangeInserted(positionStart, itemCount)
//                binding.fragmentContainer.postDelayed({
//                  remeasureFragmentContainer()
//                }, 200)
//              }
//
//              override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
//                super.onItemRangeRemoved(positionStart, itemCount)
//                binding.fragmentContainer.postDelayed({
//                  remeasureFragmentContainer()
//                }, 200)
//              }
//
//              override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
//                super.onItemRangeChanged(positionStart, itemCount)
//                binding.fragmentContainer.postDelayed({
//                  remeasureFragmentContainer()
//                }, 200)
//              }
//            }
//
//            rv.adapter?.registerAdapterDataObserver(dataObserver)
//            adapterObservers[rv] = dataObserver
//
//            // Also add scroll listener to detect when content is fully laid out
//            val scrollListener = object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
//              override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                // When scrolling stops, re-measure to account for any layout changes
//                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
//                  binding.fragmentContainer.postDelayed({
//                    remeasureFragmentContainer()
//                  }, 100)
//                }
//              }
//            }
//
//            rv.addOnScrollListener(scrollListener)
//            scrollListeners[rv] = scrollListener
//          }
//        }
//      }
//    }, 300) // Delay to ensure fragments are created
//  }
//
//  val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
//    Log.d("REACT-MAPXUS", "Destination changed to: ${destination.id}")
//    Log.d("REACT-MAPXUS", "Size: ${navHostFragment?.view?.width} ${navHostFragment?.view?.height}")
//
//    // Ensure fragment container is visible first
//    binding.fragmentContainer.visibility = View.VISIBLE
//
//    // Re-setup RecyclerView listeners for the new destination fragment
//    binding.fragmentContainer.postDelayed({
//      setupRecyclerViewContentListeners()
//    }, 200)
//
//    // Measure content after destination changes
//    binding.fragmentContainer.postDelayed({
//      remeasureFragmentContainer()
//    }, 300)
//
//    binding.bottomSheet.post {
//      if (destination.id == R.id.showRouteFragment) {
//        // Hide bottom sheet for showRouteFragment
//        bottomSheetBehavior.isHideable = true
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//      } else {
//        // Show bottom sheet for other destinations
//        bottomSheetBehavior.isHideable = false
//        // Use a shorter delay to ensure NavHostFragment content is ready
//        binding.bottomSheet.postDelayed({
//          if (navHostFragment?.view != null && !mapxusSharedViewModel.isNavigating) {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            // Re-measure after bottom sheet is shown to ensure correct size
//            binding.fragmentContainer.postDelayed({
//              remeasureFragmentContainer()
//            }, 200)
//          }
//        }, 100) // Reduced from 2000ms to 100ms
//      }
//    }
//  }
//
//  fun setupBottomSheet() {
//    mapxusSharedViewModel.bottomSheet = binding.bottomSheet
//
//    bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
//    mapxusSharedViewModel.bottomSheetBehavior = bottomSheetBehavior
//
//    bottomSheetBehavior.isHideable = false
//    bottomSheetBehavior.isDraggable = true
//    bottomSheetBehavior.skipCollapsed = false
//
//    // Ensure bottomSheet is visible and in correct state
//    binding.bottomSheet.visibility = View.VISIBLE
//    binding.bottomSheet.post {
//      // Set state after layout to ensure proper initialization
//      if (!mapxusSharedViewModel.isNavigating) {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//      } else {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//      }
//    }
//
//    binding.mapView.addOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)
//
//    binding.bottomSheet.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
//
//    bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
//
//    requireActivity().onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
//      override fun handleOnBackPressed() {
//        if(mapxusSharedViewModel.isNavigating) {
//          mapxusSharedViewModel.routePainter?.cleanRoute()
//          mapxusSharedViewModel.clearInstructions()
//          mapxusSharedViewModel.setInstructionIndex(0)
//          mapxusSharedViewModel.isNavigating = false
//        } else if(navController?.currentDestination?.route != "venue_screen") {
//          if(navController?.currentDestination?.id == R.id.poiDetailsFragment) {
//            mapxusSharedViewModel.mapxusMap?.removeMapxusPointAnnotations()
//          }
//          navController?.navigateUp()
//          binding.bottomSheet.post {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//          }
//        } else {
//          val containerView = requireView().parent as? ViewGroup
////          containerView?.removeAllViews()   // remove fragment UI
//          parentFragmentManager.beginTransaction()
//            .remove(this@XmlFragment)
//            .commitAllowingStateLoss()
//        }
//      }
//    })
//  }
//
//  fun setupNavigation() {
//    // NUCLEAR OPTION: Remove ALL fragments completely
//    childFragmentManager.fragments.toList().forEach { fragment ->
//      // Clear the fragment's view reference
//      fragment.view?.let { view ->
//        // Remove from parent
//        (view.parent as? ViewGroup)?.removeView(view)
//
//        // Clear fragment's internal view reference
//        try {
//          val mViewField = Fragment::class.java.getDeclaredField("mView")
//          mViewField.isAccessible = true
//          mViewField.set(fragment, null)  // ← CRITICAL!
//        } catch (e: Exception) {
//          Log.e("REACT-MAPXUS", "Error clearing fragment view", e)
//        }
//      }
//
//      // Remove fragment from FragmentManager
//      childFragmentManager.beginTransaction()
//        .remove(fragment)
//        .commitNowAllowingStateLoss()
//    }
//
//    // Always ensure fragmentContainer is visible first
//    binding.fragmentContainer.visibility = View.VISIBLE
//
//    // Ensure fragment container has proper layout params
//    val containerParams = binding.fragmentContainer.layoutParams
//    if (containerParams != null) {
//      containerParams.width = ViewGroup.LayoutParams.MATCH_PARENT
//      containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
//      binding.fragmentContainer.layoutParams = containerParams
//    }
//
//    // Get existing NavHostFragment if it exists
//    navHostFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
//
//    if (navHostFragment == null) {
//      // Create new NavHostFragment only if it doesn't exist
//      navHostFragment = NavHostFragment.create(R.navigation.nav_graph)
//
//      childFragmentManager.beginTransaction()
//        .replace(R.id.fragment_container, navHostFragment!!, "nav_host_fragment")
//        .setPrimaryNavigationFragment(navHostFragment!!)
//        .commitNow()
//    }
//
//    navController = navHostFragment?.navController
//
//    // Only set graph if it hasn't been set yet
//    if (navController?.graph == null) {
//      navController?.setGraph(R.navigation.nav_graph)
//    }
//
//    // Set up RecyclerView listeners to detect content changes
//    setupRecyclerViewContentListeners()
//
//    // Initial measurement
//    remeasureFragmentContainer()
//
//    // Listen for destination changes
//    navController?.addOnDestinationChangedListener(destinationChangedListener)
//
//    mapxusSharedViewModel.navController = navController
//  }
//
//  private fun setupFloatingActionButtons() {
//    // GPS button - center on user location
//    binding.gpsFab.setOnClickListener {
//      if (hasLocationPermissions()) {
//        if (isLocationEnabled()) {
//          mapxusSharedViewModel.mapxusMap?.let { mapxusMap ->
//            mapxusMap.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
//          }
//        } else {
//          showLocationSettingsDialog()
//        }
//      } else {
//        requestLocationPermissions()
//      }
//    }
//
//    // Volume button - toggle voice navigation (show during navigation)
//    binding.volumeFab.setOnClickListener {
//      isSpeaking = !isSpeaking
//      mapxusSharedViewModel.sharedPreferences?.edit()?.putBoolean("isSpeaking", isSpeaking)?.apply()
//      updateVolumeButtonIcon()
//    }
//    updateVolumeButtonIcon()
//
//    // AR Navigation button - toggle AR view
//    binding.arNavigationFab.setOnClickListener {
//      val isARActive = arNavigationViewModel.isShowingAndClosingARNavigation.value ?: false
//      arNavigationViewModel.isShowingAndClosingARNavigation.value = !isARActive
//
//      if (!isARActive) {
//        showARFragment()
//      } else {
//        hideARFragment()
//      }
//    }
//
//    mapxusSharedViewModel.instructionList.observe(viewLifecycleOwner) { instructions ->
//      val isNavigating = instructions.isNotEmpty()
//
//      binding.volumeFab.visibility = if (isNavigating) View.VISIBLE else View.GONE
//
//      if (isNavigating) {
//        arNavigationViewModel.isShowingOpeningAndClosingARButton.value = true
//      }
//    }
//
//  }
//
//  private fun setupNavigationRouteCard() {
//    // Previous button click
//    binding.navPreviousButton.setOnClickListener {
//      mapxusSharedViewModel.previousStep()
//      arNavigationViewModel.prevInstruction()
//    }
//
//    // Next button click
//    binding.navNextButton.setOnClickListener {
//      mapxusSharedViewModel.nextStep()
//      arNavigationViewModel.nextInstruction(mapxusSharedViewModel.instructionList.value?.size ?: 0)
//    }
//
//    // Observe instruction index and update card
//
//    // Observe instruction list and index reactively
//    mapxusSharedViewModel.instructionList.observe(viewLifecycleOwner) { instructionList ->
//      val instructionIndex = mapxusSharedViewModel.instructionIndex.value ?: 0
//      val isNavigating = mapxusSharedViewModel.isNavigating
//
//      updateNavigationUI(instructionList, instructionIndex, isNavigating)
//    }
//
//    mapxusSharedViewModel.instructionIndex.observe(viewLifecycleOwner) { instructionIndex ->
//      val instructionList = mapxusSharedViewModel.instructionList.value.orEmpty()
//      val isNavigating = mapxusSharedViewModel.isNavigating
//
//      updateNavigationUI(instructionList, instructionIndex, isNavigating)
//
//      // Speak instruction when index changes during navigation
//      if (isNavigating && instructionList.isNotEmpty() && instructionIndex in instructionList.indices) {
//        speakInstruction(instructionList[instructionIndex])
//      }
//    }
//  }
//
//  private fun updateStepIndicators(totalSteps: Int, currentStep: Int) {
//    binding.stepIndicatorsContainer.removeAllViews()
//
//    val maxVisibleIndicators = 6
//    val startStep = when {
//      totalSteps <= maxVisibleIndicators -> 0
//      currentStep <= 2 -> 0
//      currentStep >= totalSteps - 3 -> totalSteps - maxVisibleIndicators
//      else -> currentStep - 3
//    }.coerceAtLeast(0)
//
//    val endStep = (startStep + maxVisibleIndicators).coerceAtMost(totalSteps)
//
//    for (i in startStep until endStep) {
//      val indicator = View(getContext() ?: requireContext()).apply {
//        val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
//        layoutParams = LinearLayout.LayoutParams(size, size).apply {
//          setMargins(4, 4, 4, 4)
//        }
//        background = GradientDrawable().apply {
//          shape = GradientDrawable.OVAL
//          setColor(Color.WHITE) // base color (required for tint to apply)
//        }
//
//        // Apply tint
//        backgroundTintList = ColorStateList.valueOf(
//          ContextCompat.getColor(
//            getContext() ?: requireContext(),
//            if (i == currentStep) android.R.color.holo_blue_light else android.R.color.darker_gray
//          )
//        )
//      }
//      binding.stepIndicatorsContainer.addView(indicator)
//    }
//  }
//
//  private fun updateNavigationUI(
//    instructionList: List<InstructionDto>,
//    instructionIndex: Int,
//    isNavigating: Boolean
//  ) {
//    if (instructionList.isNotEmpty() && instructionIndex in instructionList.indices && isNavigating) {
//      val instruction = instructionList[instructionIndex]
//      binding.navTitleText.text = instruction.text ?: ""
//      binding.navDistanceText.text = "${instruction.distance.toMeterText(Locale.getDefault())}"
//      val totalDistanceMeters = (instructionList.subList(instructionIndex.absoluteValue, instructionList.size).map { instructionDto -> instructionDto.distance }.reduce { a,b -> a + b } / 1.2).roundToInt()
//      val estimatedSeconds = (totalDistanceMeters/1.2).roundToInt()
//      if(estimatedSeconds > 60)
//        binding.navTimeText.text = getString(R.string.minute, (estimatedSeconds/60).toInt())
//      else
//        binding.navTimeText.text = getString(R.string.second, estimatedSeconds)
//
//      // Check if we should show arrival dialog
//      if (isLastStepOrLowTimeEstimation(instructionIndex, instructionList.size, estimatedSeconds)) {
//        showArriveAtDestinationDialog()
//      } else {
//        // Hide dialog if it's showing and we're not at destination
//        arriveAtDestinationDialog?.dismiss()
//        arriveAtDestinationDialog = null
//      }
//
//      // Update buttons and card visibility
//      binding.navPreviousButton.isEnabled = instructionIndex > 0
//      binding.navigationRouteCard.visibility = View.VISIBLE
//
//      val icon = getStepIcon(instructionList.getOrNull(instructionIndex)?.sign ?: 0)
//      binding.navIcon.setImageDrawable(resources.getDrawable(icon, null))
//
//      // Update step indicators
//      updateStepIndicators(instructionList.size, instructionIndex)
//    } else {
//      binding.navigationRouteCard.visibility = View.GONE
//      // Hide dialog when navigation ends
//      arriveAtDestinationDialog?.dismiss()
//      arriveAtDestinationDialog = null
//    }
//  }
//
//  fun getStepIcon(sign: Int): Int {
//    return when (sign) {
//      -98 -> R.drawable.u_turn_left // U_TURN_UNKNOWN
//      -8  -> R.drawable.u_turn_left // U_TURN_LEFT
//      -7  -> R.drawable.turn_left // KEEP_LEFT
//      -3  -> R.drawable.turn_sharp_left // TURN_SHARP_LEFT
//      -2  -> R.drawable.turn_left // TURN_LEFT
//      -1  -> R.drawable.turn_slight_left // TURN_SLIGHT_LEFT
//      0   -> R.drawable.straight // CONTINUE_ON_STREET
//      1   -> R.drawable.turn_slight_right // TURN_SLIGHT_RIGHT
//      2   -> R.drawable.turn_right // TURN_RIGHT
//      3   -> R.drawable.turn_sharp_right // TURN_SHARP_RIGHT
//      4   -> R.drawable.flag // FINISH
//      5   -> R.drawable.flag // REACHED_VIA
//      6   -> R.drawable.change_circle // USE_ROUNDABOUT
//      7   -> R.drawable.turn_right // KEEP_RIGHT
//      8   -> R.drawable.u_turn_right // U_TURN_RIGHT
//      100 -> R.drawable.keyboard_arrow_up // UP (elevator up)
//      -100 -> R.drawable.keyboard_arrow_down // DOWN (elevator down)
//      200 -> R.drawable.door_front // CROSS_DOOR
//      -200 -> R.drawable.meeting_room // CROSS_ROOM_DOOR
//      else -> R.drawable.help // fallback for unexpected signs
//    }
//  }
//
//  private fun isLastStepOrLowTimeEstimation(
//    currentStep: Int,
//    totalSteps: Int,
//    estimatedSeconds: Int
//  ): Boolean {
//    // First condition: check if we're at the last step
//    if (currentStep >= totalSteps - 1) {
//      return true
//    }
//
//    // Second condition: check if time estimation is <= 4 seconds
//    return estimatedSeconds <= 4
//  }
//
//  private fun showArriveAtDestinationDialog() {
//    // Don't show if already showing
//    if (arriveAtDestinationDialog?.isShowing == true) {
//      return
//    }
//
//    val dialogView = layoutInflater.inflate(R.layout.dialog_arrive_at_destination, null)
//
//    val btnGoPrevious = dialogView.findViewById<TextView>(R.id.btn_go_previous)
//    val btnFinished = dialogView.findViewById<TextView>(R.id.btn_finished)
//
//    arriveAtDestinationDialog = AlertDialog.Builder(requireActivity())
//      .setView(dialogView)
//      .setCancelable(false)
//      .create()
//
//    // Set transparent background for rounded corners
//    arriveAtDestinationDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
//
//    // Go Previous button
//    btnGoPrevious.setOnClickListener {
//      mapxusSharedViewModel.previousStep()
//      arriveAtDestinationDialog?.dismiss()
//      arriveAtDestinationDialog = null
//    }
//
//    // Finished button
//    btnFinished.setOnClickListener {
//      // End navigation
//      endNavigation()
//      arriveAtDestinationDialog?.dismiss()
//      arriveAtDestinationDialog = null
//    }
//
//    if (isAdded && !requireActivity().isFinishing && !requireActivity().isDestroyed) {
//      arriveAtDestinationDialog?.show()
//    }
//  }
//
//  private fun endNavigation() {
//    mapxusSharedViewModel.clearInstructions()
//    mapxusSharedViewModel.setInstructionIndex(0)
//    mapxusSharedViewModel.isNavigating = false
//    binding.navigationRouteCard.visibility = View.GONE
//
//    // Hide AR if showing
//    if (arNavigationViewModel.isShowingAndClosingARNavigation.value == true) {
//      hideARFragment()
//    }
//
//    // Clear route painter if exists
//    mapxusSharedViewModel.routePainter?.cleanRoute()
//  }
//
//  private fun showARFragment() {
//    val startLocationSerializable = SerializableRoutePoint(
//      mapxusSharedViewModel.startLatLng?.lat ?: 0.0,
//      mapxusSharedViewModel.startLatLng?.lon ?: 0.0,
//      mapxusSharedViewModel.startLatLng?.floorId ?: ""
//    )
//    val destinationLocationSerializable = SerializableRoutePoint(
//      mapxusSharedViewModel.selectedPoi?.value?.location?.lat ?: 0.0,
//      mapxusSharedViewModel.selectedPoi?.value?.location?.lon ?: 0.0,
//      mapxusSharedViewModel.selectedPoi?.value?.floorId ?: mapxusSharedViewModel.selectedPoi?.value?.sharedFloorId ?: ""
//    )
//
//    val instructionListSerializable = mapxusSharedViewModel.instructionList.value?.mapNotNull {
//      it.floorId?.let { floorId ->
//        SerializableRouteInstruction(it.text, it.distance, floorId)
//      }
//    }
//
//    val instructionPointList : MutableList<RoutePlanningPoint> = mutableListOf()
//    mapxusSharedViewModel.instructionList.value?.forEachIndexed { index, instruction ->
//      instruction.indoorPoints.firstOrNull()?.let { point ->
//        instructionPointList.add(
//          RoutePlanningPoint(
//            lat = point.lat,
//            lon = point.lon,
//            floorId = point.floorId
//          )
//        )
//        Log.e("InstructionCoord", "Instruction $index → (${point.lat}, ${point.lon})")
//      }
//    }
//
//    val instructionPointSerializable = instructionPointList.map {
//      it.floorId?.let { it1 -> SerializableRoutePoint(it.lat, it.lon, it1) }
//    }
//
//    val secondInstructionPointSerializable = instructionPointList.map {
//      it.floorId?.let { it1 -> ParcelizeRoutePoint(it.lat, it.lon, it1) }
//    }
//
//    val fragment = FourthLocalARFragment()
//
//    val args = Bundle().apply {
//      putSerializable("yourLocation", startLocationSerializable!!)
//      putSerializable("destination", destinationLocationSerializable!!)
//      putInt("instructionIndex", mapxusSharedViewModel.instructionIndex.value ?: 0)
//      putInt("secondInstructionIndex", mapxusSharedViewModel.instructionIndex.value ?: 0)
//      putSerializable("instructionList", ArrayList(instructionListSerializable))
//      putSerializable("instructionPoints", ArrayList(instructionPointSerializable))
//      putParcelableArrayList("secondInstructionPoints", ArrayList(secondInstructionPointSerializable))
//    }
//
//    fragment.arguments = args
//
//    childFragmentManager.beginTransaction()
//      .replace(R.id.ar_fragment_container, fragment)
//      .commit()
//
//    binding.arFragmentContainer.visibility = View.VISIBLE
//  }
//
//  private fun hideARFragment() {
//    binding.arFragmentContainer.visibility = View.GONE
//  }
//
//  fun getNavController(): NavController? {
//    return navController ?: run {
//      val navHostFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
//      return navHostFragment?.navController
//    }
//  }
//
//  fun getSharedViewModel(): MapxusSharedViewModel {
//    return mapxusSharedViewModel
//  }
//
//  override fun onStart() {
//    Log.d("REACT-MAPXUS", "On Start")
//    super.onStart()
//    mapxusSharedViewModel.mapView.value?.onStart()
//  }
//
//  override fun onResume() {
//    Log.d("REACT-MAPXUS", "On Resume")
//    super.onResume()
//    mapxusSharedViewModel.mapView.value?.onResume()
//  }
//
//  override fun onPause() {
//    Log.d("REACT-MAPXUS", "On Pause")
//    mapxusSharedViewModel.mapView.value?.onPause()
//    super.onPause()
//  }
//
//  override fun onStop() {
//    Log.d("REACT-MAPXUS", "On Stop")
//    mapxusSharedViewModel.mapView.value?.onStop()
//    super.onStop()
//  }
//
//  private fun initializeTTS() {
//    tts = TextToSpeech(getContext() ?: requireContext()) { status ->
//      if (status == TextToSpeech.SUCCESS) {
//        val locale = mapxusSharedViewModel.locale
//        val language = if (locale.language.contains("zh")) locale else Locale("en-US")
//        Handler(Looper.getMainLooper()).postDelayed({
//          tts.setLanguage(language)
//        }, 1000)
//      }
//    }
//  }
//
//  private fun speakInstruction(instruction: InstructionDto) {
//    if (isSpeaking && ::tts.isInitialized) {
//      val locale = Locale.getDefault()
//      val words = generateSpeakText(instruction.text ?: "", instruction.distance, locale)
//      tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, null)
//    }
//  }
//
//  private fun updateVolumeButtonIcon() {
//    if (isSpeaking) {
//      binding.volumeFab.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
//    } else {
//      binding.volumeFab.setImageResource(android.R.drawable.ic_lock_silent_mode)
//    }
//  }
//
//  override fun onDestroyView() {
//    binding.mapView.removeOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)
//    mapxusSharedViewModel.mapView.value?.removeOnDidFinishRenderingFrameListener(didFinishRenderingFrameListener)
//
//    Cleaner.clearAllStaticReferences()
//
//    // Clean up RecyclerView listeners to prevent memory leaks
//    adapterObservers.forEach { entry ->
//      entry.key.adapter?.unregisterAdapterDataObserver(entry.value)
//    }
//    adapterObservers.clear()
//
//    scrollListeners.forEach { entry ->
//      entry.key.removeOnScrollListener(entry.value)
//    }
//    scrollListeners.clear()
//
//    binding.bottomSheet.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
//    // ALSO remove from view's observer (safety net)
//    view?.viewTreeObserver?.let { observer ->
//      if (observer.isAlive) {
//        observer.removeOnGlobalLayoutListener(onGlobalLayoutListener)
//      }
//    }
//
//    // ALSO remove from root view's observer (this is where the leak is!)
//    activity?.window?.decorView?.viewTreeObserver?.let { observer ->
//      if (observer.isAlive) {
//        observer.removeOnGlobalLayoutListener(onGlobalLayoutListener)
//      }
//    }
//
//    bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
//
//    // Clean up child fragments (including NavHostFragment)
//    childFragmentManager.fragments.toList().forEach { fragment ->
//      childFragmentManager.beginTransaction()
//        .remove(fragment)
//        .commitNowAllowingStateLoss()
//    }
//
//    navController?.removeOnDestinationChangedListener(destinationChangedListener)
//
//    viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
//    localizedContext = null
//
//    Log.d("REACT-MAPXUS", "On Destroy View " + navController)
//    super.onDestroyView()
//  }
//
//  override fun onDestroy() {
//    Log.d("REACT-MAPXUS", "On Destroy")
//    clearMapxusStaticReference()
//    Cleaner.clearAllStaticReferences()
////    mapxusSharedViewModel.destroy()
//
//    navHostFragment = null
//    navController = null
//
//    // Clean up TTS
//    if (::tts.isInitialized) {
//      tts.stop()
//      tts.shutdown()
//    }
//
//    binding.mapView.onDestroy()
//    Log.d("HostFragmentCheck", "onDestroy: $this hash=${this.hashCode()}")
//    super.onDestroy()
//  }
//
//  override fun onLowMemory() {
//    Log.d("REACT-MAPXUS", "On LowMem")
//    mapxusSharedViewModel.mapView.value?.onLowMemory()
//    super.onLowMemory()
//  }
//
//  override fun onSaveInstanceState(outState: Bundle) {
//    Log.d("REACT-MAPXUS", "On Save Instance")
//    mapxusSharedViewModel.mapView.value?.onSaveInstanceState(outState)
//    super.onSaveInstanceState(outState)
//  }
//
//  override fun onViewStateRestored(savedInstanceState: Bundle?) {
//    super.onViewStateRestored(savedInstanceState)
//  }
//
//  private fun clearMapxusStaticReference() {
//    try {
//      Log.d("XmlFragment", "Clearing static LIFECYCLE_OWNER")
//      val mapxusClientClass = Class.forName(
//        "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
//      )
//      val lifecycleOwnerField = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
//      lifecycleOwnerField.isAccessible = true
//
//      val currentOwner = lifecycleOwnerField.get(null)
//      if (currentOwner == this) {
//        lifecycleOwnerField.set(null, null)
//        Log.d("XmlFragment", "Cleared static LIFECYCLE_OWNER")
//      }
//    } catch (e: Exception) {
//      Log.e("XmlFragment", "Failed to clear static reference", e)
//    }
//  }
//
//  // Permission & Settings Methods
//  private fun checkAllPermissions() {
//    if (!hasAllRequiredPermissions()) {
//      requestAllPermissions()
//    } else {
//      // Check location services
//      checkLocationServices()
//      // Check precise location on Android 12+
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        checkPreciseLocation()
//      }
//    }
//  }
//
//  private fun hasAllRequiredPermissions(): Boolean {
//    val hasFineLocation = ContextCompat.checkSelfPermission(
//      requireContext(),
//      Manifest.permission.ACCESS_FINE_LOCATION
//    ) == PackageManager.PERMISSION_GRANTED
//
//    val hasCoarseLocation = ContextCompat.checkSelfPermission(
//      requireContext(),
//      Manifest.permission.ACCESS_COARSE_LOCATION
//    ) == PackageManager.PERMISSION_GRANTED
//
//    val hasCamera = ContextCompat.checkSelfPermission(
//      requireContext(),
//      Manifest.permission.CAMERA
//    ) == PackageManager.PERMISSION_GRANTED
//
//    return hasFineLocation && hasCoarseLocation && hasCamera
//  }
//
//  private fun hasLocationPermissions(): Boolean {
//    val hasFineLocation = ContextCompat.checkSelfPermission(
//      requireContext(),
//      Manifest.permission.ACCESS_FINE_LOCATION
//    ) == PackageManager.PERMISSION_GRANTED
//
//    val hasCoarseLocation = ContextCompat.checkSelfPermission(
//      requireContext(),
//      Manifest.permission.ACCESS_COARSE_LOCATION
//    ) == PackageManager.PERMISSION_GRANTED
//
//    return hasFineLocation && hasCoarseLocation
//  }
//
//  private fun requestAllPermissions() {
//    val permissionsToRequest = mutableListOf<String>()
//
//    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//      != PackageManager.PERMISSION_GRANTED) {
//      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
//    }
//
//    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
//      != PackageManager.PERMISSION_GRANTED) {
//      permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//    }
//
//    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//      != PackageManager.PERMISSION_GRANTED) {
//      permissionsToRequest.add(Manifest.permission.CAMERA)
//    }
//
//    if (permissionsToRequest.isNotEmpty()) {
//      requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
//    }
//  }
//
//  private fun requestLocationPermissions() {
//    requestPermissionLauncher.launch(
//      arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_COARSE_LOCATION
//      )
//    )
//  }
//
//  private fun isLocationEnabled(): Boolean {
//    val locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
//    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
//      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//  }
//
//  private fun checkLocationServices() {
//    if (!isLocationEnabled()) {
//      showLocationSettingsDialog()
//    }
//  }
//
//  /**
//   * Check if precise location is enabled (Android 12+)
//   * On Android 12+, even with ACCESS_FINE_LOCATION permission,
//   * the user may have chosen "approximate location" instead of "precise location".
//   */
//  private fun isPreciseLocationEnabled(): Boolean {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//      val locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
//
//      // Check if location services are enabled
//      val isLocationServiceEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
//        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//
//      if (!isLocationServiceEnabled) {
//        return false
//      }
//
//      // On Android 12+, check if we have ACCESS_FINE_LOCATION permission
//      // This is required for precise location. By default, if ACCESS_FINE_LOCATION
//      // is granted, precise location is enabled unless user explicitly disabled it.
//      val hasFineLocation = ContextCompat.checkSelfPermission(
//        requireContext(),
//        Manifest.permission.ACCESS_FINE_LOCATION
//      ) == PackageManager.PERMISSION_GRANTED
//
//      // If ACCESS_FINE_LOCATION is granted and location services are enabled,
//      // we assume precise location is enabled (default behavior)
//      return hasFineLocation
//    }
//    // For Android versions below 12, if location permission is granted, it's fine/precise
//    return hasLocationPermissions() && isLocationEnabled()
//  }
//
//  /**
//   * Check if precise location is enabled and prompt if not (Android 12+)
//   * Only opens settings if we can confirm precise location is NOT enabled
//   */
//  private fun checkPreciseLocation() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//      if (!isPreciseLocationEnabled()) {
//        val hasFineLocation = ContextCompat.checkSelfPermission(
//          requireContext(),
//          Manifest.permission.ACCESS_FINE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//
//        if (!hasFineLocation) {
//          // Request fine location permission for precise location
//          requestAllPermissions()
//        } else if (!isLocationEnabled()) {
//          // Location services not enabled - open location settings
//          openLocationSettings()
//        }
//        // If ACCESS_FINE_LOCATION is granted and location is enabled,
//        // we assume precise location is enabled (default behavior)
//        // and don't prompt the user
//      } else {
//        Log.d("REACT-MAPXUS", "Precise location is enabled")
//      }
//    }
//  }
//
//  private fun showPermissionDialog() {
//    val missingPermissions = mutableListOf<String>()
//
//    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//      != PackageManager.PERMISSION_GRANTED) {
//      missingPermissions.add("Fine Location")
//    }
//
//    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
//      != PackageManager.PERMISSION_GRANTED) {
//      missingPermissions.add("Coarse Location")
//    }
//
//    if (ContextCompat.checkSelfPermission(getContext() ?: requireContext(), Manifest.permission.CAMERA)
//      != PackageManager.PERMISSION_GRANTED) {
//      missingPermissions.add("Camera")
//    }
//
//    val message = if (missingPermissions.isEmpty()) {
//      "Some required permissions are missing."
//    } else {
//      "This app needs the following permissions to work properly:\n\n" +
//        missingPermissions.joinToString("\n• ", "• ")
//    }
//
//    AlertDialog.Builder(getContext() ?: requireContext())
//      .setTitle("Permissions Required")
//      .setMessage(message)
//      .setPositiveButton("Grant Permissions") { _, _ ->
//        requestAllPermissions()
//      }
//      .setNegativeButton("Cancel", null)
//      .show()
//  }
//
//  private fun showPreciseLocationDialog() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//      AlertDialog.Builder(getContext() ?: requireContext())
//        .setTitle("Precise Location Required")
//        .setMessage("For best accuracy with Mapxus Positioning SDK 2.0.0+, please ensure precise location is enabled.\n\n" +
//          "This app requires precise location to avoid ERROR_LOCATION_SERVICE_DISABLED errors.\n\n" +
//          "Please go to Settings > Location > App permissions, and ensure \"Precise\" is selected for this app.")
//        .setPositiveButton("Open Settings") { _, _ ->
//          openAppLocationSettings()
//        }
//        .setNegativeButton("Later", null)
//        .show()
//    }
//  }
//
//  private fun showLocationSettingsDialog() {
//    AlertDialog.Builder(getContext() ?: requireContext())
//      .setTitle("Location Services Disabled")
//      .setMessage("Please enable location services to use this feature.")
//      .setPositiveButton("Open Settings") { _, _ ->
//        openLocationSettings()
//      }
//      .setNegativeButton("Cancel", null)
//      .show()
//  }
//
//  private fun openLocationSettings() {
//    val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//    locationSettingsLauncher.launch(locationIntent)
//  }
//
//  /**
//   * Open app-specific location settings (Android 12+)
//   * This allows users to toggle between precise and approximate location
//   */
//  private fun openAppLocationSettings() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//      // Open app details settings where user can configure location permissions
//      // This is the correct way to open app-specific location settings on Android 12+
//      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//        data = android.net.Uri.fromParts("package", (getContext() ?: requireContext()).packageName, null)
//      }
//      locationSettingsLauncher.launch(intent)
//    } else {
//      // Fallback to general location settings for older versions
//      openLocationSettings()
//    }
//  }
//
//}
