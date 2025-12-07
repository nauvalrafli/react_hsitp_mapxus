package com.mapxushsitp.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.mapxushsitp.adapters.VenueAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.model.BuildingSearchOption
import com.mapxus.map.mapxusmap.api.services.model.VenueSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
import kotlinx.coroutines.launch

class VenueScreenFragment : Fragment() {

    private lateinit var venuePager: ViewPager2
    private lateinit var paginationIndicators: LinearLayout
    private var venueAdapter: VenueAdapter? = null

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
        setupViewPager()
        setupPaginationIndicators()

        // Observe shared ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
          observeSharedViewModel()
        }

        // Load real venues data
        loadVenuesData()
    }

    override fun onDestroyView() {

        super.onDestroyView()
    }

    private fun initializeViews(view: View) {
        venuePager = view.findViewById(R.id.venue_pager)
        paginationIndicators = view.findViewById(R.id.pagination_indicators)
        view.findViewById<TextView>(R.id.tvExplore).setText(sharedViewModel.context.resources.getString(R.string.explore_by_building))
    }

    private fun setupViewPager() {
        venueAdapter = VenueAdapter(sharedViewModel.locale) { venueItem ->
            onVenueSelected(venueItem)
        }

        venuePager.adapter = venueAdapter

        // Add page change listener for pagination indicators
        venuePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePaginationIndicators(position)
            }
        })
    }

    private fun setupPaginationIndicators() {
        // This will be called when venue data is loaded
    }

    private fun observeSharedViewModel() {
        // Observe venues data from shared ViewModel
        sharedViewModel.venues.observe(viewLifecycleOwner, Observer { venues ->
            if (venues.isNotEmpty()) {
                updateVenues(venues)
            }
        })
        sharedViewModel.building.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                updateBuilding(it)
            }
        })
    }

    private fun loadVenuesData() {
        val venueSearch = VenueSearch.newInstance()
        venueSearch.setVenueSearchResultListener { result ->
            if(result != null && result.venueInfoList != null) {
                val venues = result.venueInfoList
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

    private fun updatePaginationIndicators(currentPage: Int) {
        val childCount = paginationIndicators.childCount
        for (i in 0 until childCount) {
            val indicator = paginationIndicators.getChildAt(i)
            val isSelected = i == currentPage
            updateIndicatorAppearance(indicator, isSelected)
        }
    }

    private fun updateIndicatorAppearance(indicator: View, isSelected: Boolean) {
        val layoutParams = indicator.layoutParams
        val size = if (isSelected) 32 else 24
        layoutParams.width = size
        layoutParams.height = size
        indicator.layoutParams = layoutParams

        indicator.backgroundTintList = ColorStateList.valueOf(
            if (isSelected) resources.getColor(R.color.primary_blue, null)
            else resources.getColor(android.R.color.darker_gray, null)
        )
    }

    private fun onVenueSelected(venueItem: IndoorBuildingInfo) {
        // Use shared ViewModel to select venue and show floor selector
        sharedViewModel.selectVenueAndShowFloorSelector(venueItem.buildingId)
        sharedViewModel.setSelectedBuilding(venueItem)

        // Navigate to venue details
        findNavController().navigate(R.id.action_venueScreen_to_venueDetails)
    }

    fun updateVenues(venues: List<VenueInfo>) {
        venueAdapter?.updateVenues(venues)
        setupPaginationIndicatorsForVenues(venues.size)
    }

    fun updateBuilding(buildings: List<IndoorBuildingInfo>) {
        venueAdapter?.updateBuilding(buildings)
        setupPaginationIndicatorsForVenues(buildings.size)
    }

    private fun setupPaginationIndicatorsForVenues(venueCount: Int) {
        paginationIndicators.removeAllViews()

        for (i in 0 until venueCount) {
            if(context == null) {
              break;
            }
            val indicator = View(context)
            val size = 24 // 6dp in pixels
            val layoutParams = LinearLayout.LayoutParams(size, size)
            layoutParams.setMargins(8, 0, 8, 0) // 2dp margin
            indicator.layoutParams = layoutParams
            indicator.background = resources.getDrawable(R.drawable.indicator_background, null)
            indicator.backgroundTintList = ColorStateList.valueOf(resources.getColor(android.R.color.darker_gray, null))
            paginationIndicators.addView(indicator)
        }
    }

}
