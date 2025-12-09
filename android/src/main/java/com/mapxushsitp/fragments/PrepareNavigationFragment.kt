package com.mapxushsitp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxushsitp.R
import com.mapxushsitp.databinding.FragmentPrepareNavigationBinding
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel

class PrepareNavigationFragment : Fragment() {


    var _binding : FragmentPrepareNavigationBinding? = null
    val binding: FragmentPrepareNavigationBinding get() = _binding!!

    private var selectedRouteType : String = RoutePlanningVehicle.FOOT
    private var selectedStartPoint = ""
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrepareNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupDropdown()
    }

    private fun initializeViews(view: View) {
        chips = listOf(binding.shortestWalkChip, binding.liftOnlyChip, binding.escalatorOnlyChip)

        binding.tvHeader.setText(sharedViewModel.context.resources.getString(R.string.navigation))
        binding.tvRouteType.setText(sharedViewModel.context.resources.getString(R.string.route_type))
        binding.showRouteButton.setText(sharedViewModel.context.resources.getString(R.string.shortest_walk))
        binding.liftOnlyChip.setText(sharedViewModel.context.resources.getString(R.string.lift_only))
        binding.escalatorOnlyChip.setText(sharedViewModel.context.resources.getString(R.string.escalator_only))
        binding.showRouteButton.setText(sharedViewModel.context.resources.getString(R.string.show_route))
        binding.startNavigationButton.setText(sharedViewModel.context.resources.getString(R.string.start_navigation))
        binding.startPointInput.hint = sharedViewModel.context.resources.getString(R.string.select_start_point)

        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            binding.startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
        binding.destinationInput.setText(sharedViewModel.selectedPoi.value?.nameMap?.getTranslation(sharedViewModel.locale) + ", " + sharedViewModel.selectedPoi.value?.floor + ", " + sharedViewModel.selectedBuilding.value?.buildingNamesMap?.getTranslation(sharedViewModel.locale))
        when(sharedViewModel.selectedVehicle) {
            RoutePlanningVehicle.FOOT -> {
                selectChip(binding.shortestWalkChip)
            }
            RoutePlanningVehicle.WHEELCHAIR -> {
                selectChip(binding.liftOnlyChip)
            }
            RoutePlanningVehicle.ESCALATOR -> {
                selectChip(binding.escalatorOnlyChip)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.routePainter?.cleanRoute()
        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            binding.startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    var chips = listOf<TextView>()

    fun selectChip(target: TextView) {
        chips.forEach { chip ->
            if (chip == target) {
                chip.background = getDrawable(requireContext(), R.drawable.bg_chip_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.background = getDrawable(requireContext(), R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.shortestWalkChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.FOOT)
            selectChip(binding.shortestWalkChip)
        }

        binding.liftOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.WHEELCHAIR)
            selectChip(binding.liftOnlyChip)
        }

        binding.escalatorOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.ESCALATOR)
            selectChip(binding.escalatorOnlyChip)
        }

        binding.showRouteButton.setOnClickListener {
            // Request route planning first
            sharedViewModel.requestRoutePlanning(false, selectedRouteType)

            // Set bottom sheet to half height before navigation
            sharedViewModel.bottomSheetBehavior?.let { behavior ->
                // Calculate half screen height
                val displayMetrics = resources.displayMetrics
                val halfScreenHeight = displayMetrics.heightPixels / 2

                // Set peek height to half screen height
                behavior.peekHeight = halfScreenHeight

                // Ensure it's draggable
                behavior.isDraggable = true

                // Set to half expanded state
                behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            // Navigate to ShowRouteFragment
            findNavController().navigate(R.id.action_prepareNavigation_to_showRoute)
        }

        binding.startNavigationButton.setOnClickListener {
            sharedViewModel.requestRoutePlanning(true, selectedRouteType)
        }
    }

    private fun setupDropdown() {
        val popupMenu = PopupMenu(requireContext(), binding.startPointInput)
        popupMenu.menu.add(sharedViewModel.context.resources.getString(R.string.current_location))
        popupMenu.menu.add(sharedViewModel.context.resources.getString(R.string.select_location_on_map))

        popupMenu.setOnMenuItemClickListener {
            when (it.title) {
                sharedViewModel.context.resources.getString(R.string.current_location) -> {
                    selectedStartPoint = sharedViewModel.context.resources.getString(R.string.current_location)
                    binding.startPointInput.setText(selectedStartPoint)
                    sharedViewModel.startLatLng = sharedViewModel.userLocation
                    showRouteTypeSection()
                    sharedViewModel.selectedStartText = sharedViewModel.context.resources.getString(R.string.current_location)
                }
              sharedViewModel.context.resources.getString(R.string.select_location_on_map) -> {
                    findNavController().navigate(R.id.action_prepareNavigation_to_positionMark)
                }
            }
            true
        }
        binding.startPointInput.setOnClickListener {
            popupMenu.show()
        }

    }

    private fun selectRouteType(routeType: String) {
        selectedRouteType = routeType
        sharedViewModel.selectVehicle(routeType)
    }

    private fun showRouteTypeSection() {
        binding.routeTypeSection.visibility = View.VISIBLE
        selectRouteType(sharedViewModel.selectedVehicle)
    }
}
