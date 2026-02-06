package com.mapxushsitp.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.adapters.ToiletListAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.PoiSearch
import com.mapxus.map.mapxusmap.api.services.model.BuildingSearchOption
import com.mapxus.map.mapxusmap.api.services.model.DetailSearchOption
import com.mapxus.map.mapxusmap.api.services.model.PoiSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.BuildingDetailResult
import com.mapxus.map.mapxusmap.api.services.model.building.BuildingResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiCategoryResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiDetailResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiOrientationResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiResult
import com.mapxushsitp.adapters.ToiletPoi
import com.mapxushsitp.data.api.DeviceTelemetryResponse
import kotlin.getValue

class ToiletScreenFragment : Fragment() {

  private lateinit var refreshButton: ImageButton
  private lateinit var backButton: ImageButton
  private lateinit var filterTabs: TabLayout
  private lateinit var toiletList: RecyclerView

  private var toiletListAdapter: ToiletListAdapter? = null
  private var selectedFilter : ToiletType = ToiletType.ALL
  private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

  // Saved bottom sheet state so we can restore it when leaving this fragment
  private var prevPeekHeight: Int? = null
  private var prevIsDraggable: Boolean? = null
  private var prevIsHideable: Boolean? = null
  private var prevSheetHeight: Int? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_toilet_screen, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initializeViews(view)
    setupClickListeners()
    setupRecyclerView()
    setupTabs()
    setupData()

    // Force bottom sheet to full screen while this fragment is visible
    enforceFullScreenBottomSheet()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    // Restore bottom sheet behavior when leaving this fragment
    restoreBottomSheet()
  }

  private fun enforceFullScreenBottomSheet() {
    val behavior = sharedViewModel.bottomSheetBehavior
    val sheet = sharedViewModel.bottomSheet
    if (behavior == null || sheet == null) return

    val displayMetrics = resources.displayMetrics
    val screenHeight = displayMetrics.heightPixels

    // Save previous settings
    prevPeekHeight = behavior.peekHeight
    prevIsDraggable = behavior.isDraggable
    prevIsHideable = behavior.isHideable
    prevSheetHeight = (sheet.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT)

    // Apply full-screen
    sheet.layoutParams = sheet.layoutParams.apply { height = screenHeight }
    sheet.requestLayout()

    behavior.isDraggable = false
    behavior.isHideable = false
    behavior.peekHeight = screenHeight
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
  }

  private fun restoreBottomSheet() {
    val behavior = sharedViewModel.bottomSheetBehavior
    val sheet = sharedViewModel.bottomSheet
    if (behavior == null || sheet == null) return

    // Restore height
    prevSheetHeight?.let { sheet.layoutParams = sheet.layoutParams.apply { height = it } }
    sheet.requestLayout()

    // Restore behavior
    prevPeekHeight?.let { behavior.peekHeight = it }
    prevIsDraggable?.let { behavior.isDraggable = it }
    prevIsHideable?.let { behavior.isHideable = it }

    // Clear saved values
    prevPeekHeight = null
    prevIsDraggable = null
    prevIsHideable = null
    prevSheetHeight = null
  }

  private fun initializeViews(view: View) {
    refreshButton = view.findViewById(R.id.refresh_button)
    backButton = view.findViewById(R.id.back_button)
    filterTabs = view.findViewById(R.id.filter_tabs)
    toiletList = view.findViewById(R.id.toilet_list)
  }

  private fun setupClickListeners() {
    backButton.setOnClickListener {
      findNavController().navigateUp()
    }
    refreshButton.setOnClickListener {
      setupData(isUpdate = true)
    }
  }

  private fun setupRecyclerView() {
    toiletListAdapter = ToiletListAdapter(sharedViewModel.building.value ?: emptyList(), sharedViewModel.locale) { toiletItem ->
      sharedViewModel.setSelectedPoi(toiletItem.poiInfo) {
        findNavController().navigate(R.id.action_toiletScreen_to_poiDetails)
        sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }

    toiletList.apply {
      layoutManager = SafeLayoutManager(requireContext())
      adapter = toiletListAdapter
      setHasFixedSize(true)
    }
  }

  private fun setupTabs() {
    filterTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.position) {
          0 -> selectedFilter = ToiletType.ALL
          1 -> selectedFilter = ToiletType.ACCESSIBLE_TOILET
          2 -> selectedFilter = ToiletType.FEMALE_TOILET
          3 -> selectedFilter = ToiletType.MALE_TOILET
        }

        setupData(selectedFilter)
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) {}
      override fun onTabReselected(tab: TabLayout.Tab?) {}
    })
  }

  enum class ToiletType {
    ALL,
    ACCESSIBLE_TOILET,
    FEMALE_TOILET,
    MALE_TOILET
  }

  private fun setupData(type: ToiletType = ToiletType.ALL, isUpdate: Boolean = false) {
    val poiSearch = PoiSearch.newInstance()
    if(!isUpdate) {
      updateToiletList(emptyList())
      sharedViewModel.bottomSheet?.postDelayed({
        sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
      }, 100)
    }
    poiSearch.setPoiSearchResultListener(object : PoiSearch.PoiSearchResultListener {
      override fun onGetPoiResult(p0: PoiResult?) {
        if((System.currentTimeMillis() - sharedViewModel.lastUpdateTime) < 30000 && sharedViewModel.selectedBuilding.value?.buildingId == sharedViewModel.lastUpdateBuilding) {
          val toiletPoiList = (p0?.allPoi ?: listOf()).map { poi ->
            val occupancyRate = calculateOccupancyForDevices((sharedViewModel.deviceStatusBatch.value ?: mapOf())[poi.poiId] ?: emptyList())
            ToiletPoi(
              poiInfo = poi,
              occupancy = occupancyRate // This is your Double (0.0 to 100.0)
            )
          }

          if(selectedFilter == type) {
            updateToiletList(toiletPoiList)
          }
        } else {
          val toiletPoiList = (p0?.allPoi ?: listOf()).map { poi ->
            val occupancyRate = calculateOccupancyForDevices((sharedViewModel.deviceStatusBatch.value ?: mapOf())[poi.poiId] ?: emptyList())
            ToiletPoi(
              poiInfo = poi,
              occupancy = occupancyRate // This is your Double (0.0 to 100.0)
            )
          }

          if(selectedFilter == type) {
            updateToiletList(toiletPoiList)
          }

          sharedViewModel.getToiletStatus(
            sharedViewModel.selectedBuilding.value?.buildingId,
            { toiletStatus ->
              val toiletPoiList = (p0?.allPoi ?: listOf()).map { poi ->
                val occupancyRate = calculateOccupancyForDevices((sharedViewModel.deviceStatusBatch.value ?: mapOf())[poi.poiId] ?: emptyList())
                ToiletPoi(
                  poiInfo = poi,
                  occupancy = occupancyRate // This is your Double (0.0 to 100.0)
                )
              }

              if(selectedFilter == type) {
                updateToiletList(toiletPoiList)
              }
            }, onFail = {
              val toiletPoiList = (p0?.allPoi ?: listOf()).map { poi ->
                ToiletPoi(
                  poiInfo = poi,
                  occupancy = 0.0 // This is your Double (0.0 to 100.0)
                )
              }

              updateToiletList(toiletPoiList)
            })
        }
        sharedViewModel.bottomSheet?.postDelayed({
          sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }, 200)
      }

      fun calculateOccupancyForDevices(
        response: List<DeviceTelemetryResponse>
      ): Double {
        if (response.isEmpty()) return 0.0

        // Count how many individual telemetry entries indicate "Occupied"
        val occupiedCount = response.count { it.isVacant() == 1 }

        return (occupiedCount.toDouble() / response.size) * 100.0
      }

      override fun onGetPoiDetailResult(p0: PoiDetailResult?) {
        TODO("Not yet implemented")
      }

      override fun onGetPoiByOrientationResult(p0: PoiOrientationResult?) {
        TODO("Not yet implemented")
      }

      override fun onPoiCategoriesResult(p0: PoiCategoryResult?) {
        TODO("Not yet implemented")
      }

    })
    val opt = PoiSearchOption().apply {
      mPageCapacity = 100
      if(sharedViewModel.selectedBuilding.value != null && sharedViewModel.selectedBuilding.value?.buildingId != null) {
        setBuildingId(sharedViewModel.selectedBuilding.value?.buildingId)
      }
    }
    opt.setVenueId(sharedViewModel.selectedVenue.value?.id)
    val type = when (type) {
      ToiletType.ALL -> "restroom"
      ToiletType.ACCESSIBLE_TOILET -> "restroom.disable"
      ToiletType.FEMALE_TOILET -> "restroom.female"
      ToiletType.MALE_TOILET -> "restroom.male"
    }
    opt.setCategory(type)
    poiSearch.searchPoiByOption(opt)
  }

  // Updated to suppress layout and disable change animations while updating the adapter
  fun updateToiletList(toilets: List<ToiletPoi>) {
    // Ensure this runs on the UI thread and prevents intermediate layout passes that move the BottomSheet
    toiletList.post {
      // Temporarily stop layout and animations
      toiletList.suppressLayout(true)
      val animator = toiletList.itemAnimator
      val simpleAnimator = animator as? androidx.recyclerview.widget.SimpleItemAnimator
      val prevSupportsChange = simpleAnimator?.supportsChangeAnimations
      simpleAnimator?.supportsChangeAnimations = false

      try {
        toiletListAdapter?.updateToilets(toilets)
      } finally {
        // Restore previous animator setting and re-enable layout
        simpleAnimator?.supportsChangeAnimations = prevSupportsChange ?: true
        toiletList.suppressLayout(false)
      }
    }
  }
}

class SafeLayoutManager(context: Context): LinearLayoutManager(context) {
  override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
    try {
      super.onLayoutChildren(recycler, state)
    } catch (e: IndexOutOfBoundsException) {
      Log.e("RecyclerView", "Inconsistency detected")
    }
  }
}
