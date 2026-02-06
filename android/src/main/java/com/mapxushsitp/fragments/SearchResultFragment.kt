package com.mapxushsitp.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
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
    private lateinit var checkboxAllBuilding: CheckBox

    val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    private var searchResultsAdapter: SearchResultsAdapter? = null

    // Pagination state
    private var currentPage = 1
    private val pageSize = 30
    private var isLoading = false
    private var hasMore = true

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
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupRecyclerView()

        // Force bottom sheet to full screen while this fragment is visible to avoid remeasure jitter
        enforceFullScreenBottomSheet()
    }

    override fun onDestroyView() {
      super.onDestroyView()
      // Restore bottom sheet behavior when leaving this fragment
      restoreBottomSheet()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        searchInputLayout = view.findViewById(R.id.text_input_layout)
        searchInput = view.findViewById(R.id.et_search)
        searchResultsList = view.findViewById(R.id.search_results_list)
        loadingState = view.findViewById(R.id.loading_state)
        emptyState = view.findViewById(R.id.empty_state)
        notFoundState = view.findViewById(R.id.not_found_state)
        checkboxAllBuilding = view.findViewById(R.id.checkboxAllBuilding)

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
                  // start new search from page 1
                    currentPage = 1
                    hasMore = true
                    performSearch(page = currentPage)
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                } else if(p1 == EditorInfo.IME_ACTION_SEARCH) {
                    currentPage = 1
                    hasMore = true
                    performSearch(page = currentPage)
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                }
                return true
            }
        })

        // When checkbox toggles, re-run the search so results reflect the chosen scope
        checkboxAllBuilding.setOnCheckedChangeListener { _, _ ->
          // Re-run search from first page with updated scope (will skip building filter when checked)
          currentPage = 1
          hasMore = true
          performSearch(page = currentPage)
        }
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

            // Add scroll listener to implement infinite scroll / pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
              @Deprecated("Overrides deprecated API")
              override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // Trigger load when we're within 5 items from the end
                if (!isLoading && hasMore && lastVisibleItemPosition >= (totalItemCount - 5)) {
                  loadNextPage()
                }
              }
            })
        }
    }

    private fun loadNextPage() {
      if (isLoading || !hasMore) return
      isLoading = true
      currentPage += 1
      performSearch(page = currentPage)
    }

    private fun performSearch(page: Int = 1) {
        val query = searchInput.text.toString().trim()

        // If it's the first page, show loading state; for subsequent pages keep showing existing list
        if (page == 1) showLoadingState()
        val poiSearch = PoiSearch.newInstance()
        isLoading = true
        poiSearch.setPoiSearchResultListener(object : PoiSearch.PoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                val results = result?.allPoi ?: listOf()
                searchResultsAdapter?.updateResults(results)
                searchResultsList.requestLayout()

                // If first page, replace; otherwise append
                if (page == 1) {
                  searchResultsAdapter?.updateResults(results)
                } else {
                  searchResultsAdapter?.addResults(results)
                }

                // Determine if there are more pages - if returned results less than pageSize assume no more
                hasMore = results.size >= pageSize

                // Update UI states
                if (searchResultsAdapter?.itemCount == 0) {
                    showNotFoundState()
                } else {
                    showResults()
                }

                sharedViewModel.bottomSheet?.postDelayed({
                  sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }, 200)

                isLoading = false
            }

            override fun onGetPoiDetailResult(p0: PoiDetailResult?) {
                // Not used in search
                isLoading = false
            }

            override fun onGetPoiByOrientationResult(p0: PoiOrientationResult?) {
                // Not used in search
                isLoading = false
            }

            override fun onPoiCategoriesResult(p0: PoiCategoryResult?) {
                // Not used in search
                isLoading = false
            }
        })

        val searchOption = PoiSearchOption().apply {
            setKeywords(query)
            setExcludeCategories("facility.steps,facility.elevator")
            pageCapacity(pageSize)
            pageNum(page)
            // Add venue and building filter if available
            sharedViewModel.selectedBuilding.value?.let {
                setVenueId(it.venueId)
            }
            if (!checkboxAllBuilding.isChecked) {
              sharedViewModel.selectedBuilding.value?.let {
                setBuildingId(it.buildingId)
              }
            }
        }
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

    // Enforce full-screen bottom sheet while this fragment is visible
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

    // Restore bottom sheet to previous settings
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
}
