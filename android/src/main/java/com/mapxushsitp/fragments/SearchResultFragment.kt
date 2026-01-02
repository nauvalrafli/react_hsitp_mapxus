package com.mapxushsitp.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.adapters.SearchResultsAdapter
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mapxus.map.mapxusmap.api.services.PoiSearch
import com.mapxus.map.mapxusmap.api.services.model.PoiSearchOption
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiDetailResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiOrientationResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiCategoryResult

class SearchResultFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var searchInputLayout: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchResultsList: RecyclerView
    private lateinit var loadingState: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var notFoundState: LinearLayout

    val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    private var searchResultsAdapter: SearchResultsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupRecyclerView()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        searchInputLayout = view.findViewById(R.id.text_input_layout)
        searchInput = view.findViewById(R.id.et_search)
        searchResultsList = view.findViewById(R.id.search_results_list)
        loadingState = view.findViewById(R.id.loading_state)
        emptyState = view.findViewById(R.id.empty_state)
        notFoundState = view.findViewById(R.id.not_found_state)

        // Show empty state initially instead of performing empty search
        showEmptyState()
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        searchInput.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(
                p0: TextView?,
                p1: Int,
                p2: KeyEvent?
            ): Boolean {
                if(p2?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    performSearch()
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                } else if(p1 == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch()
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter(locale = sharedViewModel.locale) { poiInfo ->
            sharedViewModel.setSelectedPoi(poiInfo){
                findNavController().navigate(R.id.action_searchResult_to_poiDetails)
                sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        searchResultsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultsAdapter
        }
    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()

        // If query is empty, show empty state
//        if (query.isEmpty()) {
//            showEmptyState()
//            return
//        }

        showLoadingState()
        val poiSearch = PoiSearch.newInstance()
        poiSearch.setPoiSearchResultListener(object : PoiSearch.PoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                val results = result?.allPoi ?: listOf()
                searchResultsAdapter?.updateResults(results)
                searchResultsList.requestLayout()

                // Show appropriate state based on results
                if (results.isEmpty()) {
                    showNotFoundState()
                } else {
                    showResults()
                }
            }

            override fun onGetPoiDetailResult(p0: PoiDetailResult?) {
                // Not used in search
            }

            override fun onGetPoiByOrientationResult(p0: PoiOrientationResult?) {
                // Not used in search
            }

            override fun onPoiCategoriesResult(p0: PoiCategoryResult?) {
                // Not used in search
            }
        })

        val searchOption = PoiSearchOption().apply {
            setKeywords(query)
            setExcludeCategories("facility.steps,facility.elevator")
            pageCapacity(30)
            // Add venue and building filter if available
            sharedViewModel.selectedBuilding.value?.let {
                setBuildingId(it.buildingId)
                setVenueId(it.venueId)
            }
        }
        Log.d("SEARCH", "Search query: ${query} with building: ${sharedViewModel.selectedBuilding.value?.buildingNamesMap?.en}")
        poiSearch.searchPoiByOption(searchOption)
    }

    private fun showLoadingState() {
        loadingState.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        notFoundState.visibility = View.GONE
        searchResultsList.visibility = View.GONE
    }

    private fun showEmptyState() {
        loadingState.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        notFoundState.visibility = View.GONE
        searchResultsList.visibility = View.GONE
    }

    private fun showNotFoundState() {
        loadingState.visibility = View.GONE
        emptyState.visibility = View.GONE
        notFoundState.visibility = View.VISIBLE
        searchResultsList.visibility = View.GONE
    }

    private fun showResults() {
        loadingState.visibility = View.GONE
        emptyState.visibility = View.GONE
        notFoundState.visibility = View.GONE
        searchResultsList.visibility = View.VISIBLE
    }

}
