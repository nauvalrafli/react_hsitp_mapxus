package com.mapxushsitp.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
// import androidx.viewpager2.widget.ViewPager2  // Commented out - using ViewPager instead
import com.mapxushsitp.adapters.VenueAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.model.BuildingSearchOption
import com.mapxus.map.mapxusmap.api.services.model.VenueSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
import kotlin.math.abs

class VenueScreenFragment : Fragment() {

  private lateinit var venueRecycler: RecyclerView
  private lateinit var paginationIndicators: LinearLayout
  private var venueAdapter: VenueAdapter? = null

  val snapHelper = PagerSnapHelper()

  // Shared ViewModel
  private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_venue_screen, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initializeViews(view)
    setupRecyclerView()

    // Observe shared ViewModel
    observeSharedViewModel()

    // Load real venues data
    loadVenuesData()
  }

  private fun initializeViews(view: View) {
    venueRecycler = view.findViewById<RecyclerView>(R.id.venue_recycler)
    paginationIndicators = view.findViewById(R.id.pagination_indicators)
  }

  private fun setupRecyclerView() {
    venueAdapter = VenueAdapter(sharedViewModel.locale) { venueItem ->
      onVenueSelected(venueItem)
    }
    venueRecycler.apply {
      // Set vertical scrolling
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
      adapter = venueAdapter

      snapHelper.attachToRecyclerView(this)

      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          super.onScrollStateChanged(recyclerView, newState)

          // Only update when the scrolling stops to prevent flickering
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val centerView = snapHelper.findSnapView(layoutManager)
            centerView?.let {
              val pos = layoutManager?.getPosition(it)
              if (pos != null) {
                updatePaginationIndicators(pos)
              }
            }
          }
        }
      })

      // Enable nested scrolling so it plays nice with BottomSheetBehavior
//            isNestedScrollingEnabled = true
    }

    // Optional: Auto-expand sheet when the user interacts with the list
    venueRecycler.setOnTouchListener { _, _ ->
      if (sharedViewModel.bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
        sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
      }
      false
    }
  }

  private fun updatePaginationIndicators(currentPosition: Int) {
    val childCount = paginationIndicators.childCount
    for (i in 0 until childCount) {
      val indicator = paginationIndicators.getChildAt(i)
      // Check if this dot is the one the user is looking at
      val isSelected = (i == currentPosition)

      // Call the smooth function we created in the previous step
      updateIndicatorAppearance(indicator, isSelected)
    }
  }

  private fun setupPaginationIndicatorsForVenues(venueCount: Int) {
    paginationIndicators.removeAllViews()

    // If there is only 1 item, we don't usually need dots
    if (venueCount <= 1) return

    for (i in 0 until venueCount) {
      val indicator = View(requireContext())

      // Use a fixed size for the "box" the dot lives in
      val size = 24 // this is roughly 8dp-10dp depending on density
      val layoutParams = LinearLayout.LayoutParams(size, size)
      layoutParams.setMargins(8, 0, 8, 0)

      indicator.layoutParams = layoutParams
      indicator.background = resources.getDrawable(R.drawable.indicator_background, null)

      // Initial state: dimmed and normal size
      indicator.alpha = 0.3f
      indicator.scaleX = 1.0f
      indicator.scaleY = 1.0f

      paginationIndicators.addView(indicator)
    }

    // Highlight the first one by default
    if (paginationIndicators.childCount > 0) {
      updateIndicatorAppearance(paginationIndicators.getChildAt(0), true)
    }
  }

  private fun updateIndicatorAppearance(indicator: View, isSelected: Boolean) {
    // 1. Change Alpha (Instantly shows which one is active)
    val targetAlpha = if (isSelected) 1.0f else 0.3f


    // Use a short animation for a premium "Material" feel
    indicator.animate()
      .alpha(targetAlpha)
      .setDuration(150) // Fast enough to feel responsive
      .start()

    // 3. Optional: Change Tint
    indicator.backgroundTintList = ColorStateList.valueOf(
      if (isSelected) resources.getColor(R.color.primary_blue, null)
      else resources.getColor(android.R.color.darker_gray, null)
    )
  }


  private fun observeSharedViewModel() {
    sharedViewModel.building.observe(viewLifecycleOwner, Observer {
      if (it.isNotEmpty()) {
        updateBuilding(it)
        setupPaginationIndicatorsForVenues(it.size)
      }
    })
  }

  private fun loadVenuesData() {
    val venueSearch = VenueSearch.newInstance()
    venueSearch.setVenueSearchResultListener { result ->
      if(result != null && result.venueInfoList != null) {
        val venues = result.venueInfoList
        Log.d("Venue", venues.get(0).buildings.size.toString())
        sharedViewModel.updateVenues(venues)
        updateVenues(venues)
      }
    }
    venueSearch.searchVenueByOption(VenueSearchOption())

    val buildingSearch = BuildingSearch.newInstance()
    buildingSearch.searchBuildingByOption(BuildingSearchOption()) {
      if(it != null && it.indoorBuildingList != null) {
        val building = it.indoorBuildingList
        sharedViewModel.updateBuildings(building)
      }
    }
  }

  private fun onVenueSelected(venueItem: IndoorBuildingInfo) {
    // Use shared ViewModel to select venue and show floor selector
//        sharedViewModel.selectVenueAndShowFloorSelector(venueItem.venueId)
    sharedViewModel.setSelectedBuilding(venueItem)

    // Navigate to venue details
    findNavController().navigate(R.id.action_venueScreen_to_venueDetails)
  }

  fun updateVenues(venues: List<VenueInfo>) {
    venueAdapter?.updateVenues(venues)
  }

  fun updateBuilding(buildings: List<IndoorBuildingInfo>) {
    venueAdapter?.updateBuilding(buildings)
  }

}
