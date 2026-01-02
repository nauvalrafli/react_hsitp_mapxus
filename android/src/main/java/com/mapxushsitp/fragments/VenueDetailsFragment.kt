package com.mapxushsitp.fragments

import android.os.Bundle
import android.util.Log
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
import com.mapxushsitp.adapter.CategoryAdapter
import com.mapxushsitp.adapter.CategoryAdapter.CategoryItem
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.mapxushsitp.databinding.FragmentVenueDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VenueDetailsFragment : Fragment() {

    var _binding : FragmentVenueDetailsBinding? = null
    val binding get() = _binding!!

    // Shared ViewModel
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVenueDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        observeSharedViewModel()
    }

    private fun initializeViews(view: View) {
        // Prevent editing by removing key listener and preventing focus
        binding.etSearch.keyListener = null
        binding.etSearch.isFocusable = false
        binding.etSearch.isFocusableInTouchMode = false
    }

    private fun setupClickListeners() {
        binding.textInputLayout.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        })
        binding.etSearch.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        })
        binding.textInputLayout.setOnClickListener({
            findNavController().navigate(R.id.action_venueDetails_to_searchResult)
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        })

        binding.closeButton.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCategoryRecyclerView()
    }

    private fun observeSharedViewModel() {
        sharedViewModel.selectedBuilding.observe(viewLifecycleOwner, Observer { venue ->
            venue?.let {
                sharedViewModel.mapxusMap?.selectBuildingById(it.buildingId)
                binding.venueName.text = it.buildingNamesMap?.getTranslation(sharedViewModel.locale)
                binding.venueAddress.text = it.addressMap?.getTranslation(sharedViewModel.locale)?.street
            }
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
//            sharedViewModel.bottomSheet?.post {
//                sharedViewModel.mapxusMap?.selectBuildingById(venue?.buildingId ?: "")
//            }
        })
    }

    private fun setupCategoryRecyclerView() {
        binding.categoryList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categories = listOf(
            CategoryItem(
                name = getString(R.string.category_company),
                iconResId = R.drawable.workplace,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "workplace"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
            CategoryItem(
                name = getString(R.string.category_shops),
                iconResId = R.drawable.shopping_bag,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "shopping"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
            CategoryItem(
                name = getString(R.string.category_restaurant),
                iconResId = R.drawable.restaurant,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "restaurants"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
            CategoryItem(
                name = getString(R.string.category_washroom),
                iconResId = R.drawable.ic_toilet,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "facility.restroom"
                    findNavController().navigate(R.id.action_venueDetails_to_toiletScreen)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
            CategoryItem(
                name = getString(R.string.category_transportation),
                iconResId = R.drawable.car,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "transport"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
            CategoryItem(
                name = getString(R.string.category_utilities),
                iconResId = R.drawable.facility,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.soft_blue),
                onItemClick = {
                    sharedViewModel.selectedCategory = "facility"
                    sharedViewModel.excludedCategory = "facility.restroom"
                    findNavController().navigate(R.id.action_venueDetails_to_categoryList)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            ),
        )

        binding.categoryList.adapter = CategoryAdapter(categories)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
