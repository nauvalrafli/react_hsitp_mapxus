package com.mapxushsitp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.adapters.CategoryListAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxus.map.mapxusmap.api.services.PoiSearch
import com.mapxus.map.mapxusmap.api.services.model.PoiSearchOption
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import java.util.Locale

/**
 * Presents a vertically scrolling list that mirrors the Toilet screen UI without filters.
 * Logic for fetching and mapping categories can be plugged in through the provided APIs.
 */
class CategoryListFragment : Fragment() {

    private lateinit var closeButton: ImageButton
    private lateinit var headerTitle: TextView
    private lateinit var categoryRecycler: RecyclerView
    private lateinit var categoryHeaderTitle: TextView
    private lateinit var loadingStateText : LinearLayout
    private lateinit var emptyStateText : LinearLayout
    private lateinit var notFoundStateText : LinearLayout

    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()
    private var categoryAdapter: CategoryListAdapter? = null
    private var onCategorySelected: ((PoiInfo) -> Unit)? = null
    private var pendingItems: List<PoiInfo>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_category_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecycler()
        setupClickListeners()
        applyArguments()
        sharedViewModel.bottomSheet?.post {
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initializeViews(root: View) {
        closeButton = root.findViewById(R.id.category_close_button)
        headerTitle = root.findViewById(R.id.category_header_title)
        categoryRecycler = root.findViewById(R.id.category_recycler_view)
        categoryHeaderTitle = root.findViewById(R.id.category_header_title)
        notFoundStateText = root.findViewById(R.id.not_found_state)
        emptyStateText = root.findViewById(R.id.empty_state)
        loadingStateText = root.findViewById(R.id.loading_state)

        val text = when(sharedViewModel.selectedCategory) {
            "workplace" -> sharedViewModel.context.getString(R.string.category_company)
            "shopping" -> sharedViewModel.context.getString(R.string.category_shops)
            "restaurants" -> sharedViewModel.context.getString(R.string.category_restaurant)
            "facility.restroom" -> sharedViewModel.context.getString(R.string.category_washroom)
            "transport" -> sharedViewModel.context.getString(R.string.category_transportation)
            "facility" -> sharedViewModel.context.getString(R.string.category_utilities)
            else -> "General"
        }
        showEmptyState()
        categoryHeaderTitle.setText(text)
    }

    private fun setupRecycler() {
        categoryAdapter = CategoryListAdapter(
            buildingList = sharedViewModel.building.value ?: emptyList(),
            locale = sharedViewModel.locale
        ) { item ->
            sharedViewModel.setSelectedPoi(item) {
                if(sharedViewModel.navController?.currentDestination?.id != R.id.poiDetailsFragment) {
                    sharedViewModel.navController?.navigate(R.id.action_global_to_poiDetails)
                }
                sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
            onCategorySelected?.invoke(item)
        }

        categoryRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        pendingItems?.let {
            categoryAdapter?.submitItems(it)
            pendingItems = null
        }

        fetchCategoryData()
    }

    private fun fetchCategoryData() {
        showLoading()
        val category = sharedViewModel.selectedCategory.takeIf { it.isNotBlank() }
        val poiSearch = PoiSearch.newInstance()
        poiSearch.searchPoiByOption(
            PoiSearchOption().apply {
                if(category != "facility") {
                    setCategory(category)
                } else {
                    setExcludeCategories("facility.steps,facility.connector,facility.restroom,workplace,shopping,restaurants,transport")
                }
                Log.d("Mapxus Category", category.toString())
                pageCapacity(30)
                sharedViewModel.selectedBuilding.value?.buildingId?.let { setBuildingId(it) }
                sharedViewModel.selectedVenue.value?.id?.let { setVenueId(it) }
            }
        ) { result ->
            val pois = result.allPoi ?: emptyList()
            if(pois.size > 0) {
                renderCategories(pois)
                showResult()
            } else {
                showNotFound()
            }
        }
    }

    private fun showLoading() {
        notFoundStateText.visibility = View.GONE
        loadingStateText.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
        categoryRecycler.visibility = View.GONE
    }

    private fun showNotFound() {
        notFoundStateText.visibility = View.VISIBLE
        loadingStateText.visibility = View.GONE
        emptyStateText.visibility = View.GONE
        categoryRecycler.visibility = View.GONE
    }

    private fun showEmptyState() {
        notFoundStateText.visibility = View.GONE
        loadingStateText.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
        categoryRecycler.visibility = View.GONE
    }

    private fun showResult() {
        notFoundStateText.visibility = View.GONE
        loadingStateText.visibility = View.GONE
        emptyStateText.visibility = View.GONE
        categoryRecycler.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun applyArguments() {
        val title = arguments?.getString(ARG_HEADER_TITLE)
        if (!title.isNullOrBlank()) {
            headerTitle.text = title
        }
    }

    fun setOnCategorySelectedListener(listener: (PoiInfo) -> Unit) {
        onCategorySelected = listener
    }

    fun renderCategories(items: List<PoiInfo>) {
        if (categoryAdapter == null) {
            pendingItems = items
        } else {
            categoryAdapter?.submitItems(items)
        }
    }

    companion object {
        private const val ARG_HEADER_TITLE = "category_header_title"

        fun newInstance(headerTitle: String? = null): CategoryListFragment {
            return CategoryListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HEADER_TITLE, headerTitle)
                }
            }
        }
    }
}


