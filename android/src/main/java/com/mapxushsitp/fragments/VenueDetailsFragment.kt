package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxushsitp.adapter.CategoryAdapter
import com.mapxushsitp.adapter.CategoryAdapter.CategoryItem
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VenueDetailsFragment : Fragment() {

    private lateinit var venueName: TextView
    private lateinit var venueAddress: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var searchLayout: LinearLayout
    private lateinit var searchField: TextView
    private lateinit var tilSearch: LinearLayout
    private lateinit var categoryList: RecyclerView

    // Shared ViewModel
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_venue_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        observeSharedViewModel()
    }

    private fun initializeViews(view: View) {
        venueName = view.findViewById(R.id.venue_name)
        venueAddress = view.findViewById(R.id.venue_address)
        closeButton = view.findViewById(R.id.close_button)
        searchLayout = view.findViewById(R.id.text_input_layout)
        searchField = view.findViewById(R.id.et_search)
        tilSearch = view.findViewById(R.id.text_input_layout)
        categoryList = view.findViewById(R.id.category_list)

        // Prevent editing by removing key listener and preventing focus
        searchField.keyListener = null
        searchField.isFocusable = false
        searchField.isFocusableInTouchMode = false
        searchField.setText(sharedViewModel.context.resources.getString(R.string.search))
        sharedViewModel.mapxusMap?.selectBuildingById(sharedViewModel.selectedBuilding.value?.buildingId ?: "")
    }

    private fun setupClickListeners() {
        searchLayout.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
        })
        searchField.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
        })
        tilSearch.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
        })

        closeButton.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCategoryRecyclerView()
    }

    private fun observeSharedViewModel() {
        // Observe selected venue from shared ViewModel
        sharedViewModel.selectedBuilding.observe(viewLifecycleOwner, Observer { venue ->
            venue?.let {
                venueName.text = it.buildingNamesMap?.getTranslation(sharedViewModel.locale)
                venueAddress.text = it.addressMap?.getTranslation(sharedViewModel.locale)?.street
            }
        })
//        sharedViewModel.bottomSheet?.post {
//          sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
//        }
    }

    private fun setupCategoryRecyclerView() {
        categoryList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categories = listOf(
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_company),
                iconResId = R.drawable.workplace,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "workplace"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                }
            ),
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_shops),
                iconResId = R.drawable.shopping_bag,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "shopping"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                }
            ),
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_restaurant),
                iconResId = R.drawable.restaurant,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "restaurants"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                }
            ),
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_washroom),
                iconResId = R.drawable.ic_toilet,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "facility.restroom"
                    findNavController().navigate(R.id.action_venueDetails_to_toiletScreen)
                }
            ),
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_transportation),
                iconResId = R.drawable.car,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "transport"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                }
            ),
            CategoryItem(
                name = sharedViewModel.context.getString(R.string.category_utilities),
                iconResId = R.drawable.facility,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "facility"
                    sharedViewModel.excludedCategory = "facility.restroom"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                }
            ),
        )

        categoryList.adapter = CategoryAdapter(categories)
    }
}
