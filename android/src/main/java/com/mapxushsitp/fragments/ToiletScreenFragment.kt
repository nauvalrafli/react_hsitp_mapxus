package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.adapters.ToiletListAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
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
        val buildingSearch = BuildingSearch.newInstance()
        buildingSearch.setBuildingSearchResultListener(object: BuildingSearch.BuildingSearchResultListener {
            override fun onGetBuildingResult(p0: BuildingResult?) {
                toiletListAdapter = ToiletListAdapter(p0?.indoorBuildingList ?: listOf(), sharedViewModel.locale) { toiletItem ->
                    sharedViewModel.setSelectedPoi(toiletItem)
                    findNavController().navigate(R.id.action_toiletScreen_to_poiDetails)
                }

                toiletList.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = toiletListAdapter
                }
            }
            override fun onGetBuildingDetailResult(p0: BuildingDetailResult?) {
                toiletListAdapter = ToiletListAdapter(p0?.indoorBuildingList ?: listOf(), sharedViewModel.locale) { toiletItem ->
                    sharedViewModel.setSelectedPoi(toiletItem)
                    findNavController().navigate(R.id.action_toiletScreen_to_poiDetails)
                }

                toiletList.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = toiletListAdapter
                }
            }
        })
        buildingSearch.searchBuildingByOption(BuildingSearchOption().apply { this.setKeywords("") })
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
        poiSearch.setPoiSearchResultListener(object : PoiSearch.PoiSearchResultListener {
            override fun onGetPoiResult(p0: PoiResult?) {
                updateToiletList(p0?.allPoi ?: listOf())
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

    fun updateToiletList(toilets: List<PoiInfo>) {
        toiletListAdapter?.updateToilets(toilets)
    }
}
