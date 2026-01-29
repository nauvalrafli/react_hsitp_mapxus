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

    private lateinit var closeButton: ImageButton
    private lateinit var filterTabs: TabLayout
    private lateinit var toiletList: RecyclerView

    private var toiletListAdapter: ToiletListAdapter? = null
    private var selectedFilter : ToiletType = ToiletType.ALL
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

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
    }

    private fun initializeViews(view: View) {
        closeButton = view.findViewById(R.id.close_button)
        filterTabs = view.findViewById(R.id.filter_tabs)
        toiletList = view.findViewById(R.id.toilet_list)
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        toiletListAdapter = ToiletListAdapter((sharedViewModel.building.value ?: listOf()), sharedViewModel.locale) { toiletItem ->
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

    private fun setupData(type: ToiletType = ToiletType.ALL) {
        val poiSearch = PoiSearch.newInstance()
        updateToiletList(emptyList())
        sharedViewModel.bottomSheet?.postDelayed({
          sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }, 100)
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
            mPageCapacity = 30
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

    fun updateToiletList(toilets: List<ToiletPoi>) {
        toiletListAdapter?.updateToilets(toilets)
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
