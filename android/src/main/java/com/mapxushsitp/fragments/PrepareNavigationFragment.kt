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
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel

class PrepareNavigationFragment : Fragment() {

    private lateinit var backButton: View
    private lateinit var startPointLayout: LinearLayout
    private lateinit var startPointInput: AutoCompleteTextView
    private lateinit var destinationInput: TextView
    private lateinit var routeTypeSection: LinearLayout
    private lateinit var shortestWalkChip: TextView
    private lateinit var liftOnlyChip: TextView
    private lateinit var escalatorOnlyChip: TextView
    private lateinit var showRouteButton: Button
    private lateinit var startNavigationButton: Button

    private var selectedRouteType : String = RoutePlanningVehicle.FOOT
    private var selectedStartPoint = ""
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prepare_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupDropdown()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        startPointLayout = view.findViewById(R.id.start_point_container)
        startPointInput = view.findViewById(R.id.start_point_input)
        destinationInput = view.findViewById(R.id.destination_input)
        routeTypeSection = view.findViewById(R.id.route_type_section)
        shortestWalkChip = view.findViewById(R.id.shortest_walk_chip)
        liftOnlyChip = view.findViewById(R.id.lift_only_chip)
        escalatorOnlyChip = view.findViewById(R.id.escalator_only_chip)
        showRouteButton = view.findViewById(R.id.show_route_button)
        startNavigationButton = view.findViewById(R.id.start_navigation_button)
        chips = listOf(shortestWalkChip, liftOnlyChip, escalatorOnlyChip)

        view.findViewById<TextView>(R.id.tvHeader).setText(sharedViewModel.context.resources.getString(R.string.navigation))
        view.findViewById<TextView>(R.id.tvRouteType).setText(sharedViewModel.context.resources.getString(R.string.route_type))
        view.findViewById<TextView>(R.id.shortest_walk_chip).setText(sharedViewModel.context.resources.getString(R.string.shortest_walk))
        view.findViewById<TextView>(R.id.lift_only_chip).setText(sharedViewModel.context.resources.getString(R.string.lift_only))
        view.findViewById<TextView>(R.id.escalator_only_chip).setText(sharedViewModel.context.resources.getString(R.string.escalator_only))
        showRouteButton.setText(sharedViewModel.context.resources.getString(R.string.show_route))
        startNavigationButton.setText(sharedViewModel.context.resources.getString(R.string.start_navigation))
        startPointInput.hint = sharedViewModel.context.resources.getString(R.string.select_start_point)

        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
        destinationInput.setText(sharedViewModel.selectedPoi.value?.nameMap?.getTranslation(sharedViewModel.locale) + ", " + sharedViewModel.selectedPoi.value?.floor + ", " + sharedViewModel.selectedBuilding.value?.buildingNamesMap?.getTranslation(sharedViewModel.locale))
        when(sharedViewModel.selectedVehicle) {
            RoutePlanningVehicle.FOOT -> {
                selectChip(shortestWalkChip)
            }
            RoutePlanningVehicle.WHEELCHAIR -> {
                selectChip(liftOnlyChip)
            }
            RoutePlanningVehicle.ESCALATOR -> {
                selectChip(escalatorOnlyChip)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.routePainter?.cleanRoute()
        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
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
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        shortestWalkChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.FOOT)
            selectChip(shortestWalkChip)
        }

        liftOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.WHEELCHAIR)
            selectChip(liftOnlyChip)
        }

        escalatorOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.ESCALATOR)
            selectChip(escalatorOnlyChip)
        }

        showRouteButton.setOnClickListener {
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

        startNavigationButton.setOnClickListener {
            sharedViewModel.requestRoutePlanning(true, selectedRouteType)
        }
    }

    private fun setupDropdown() {
        val popupMenu = PopupMenu(requireContext(), startPointInput)
        popupMenu.menu.add(sharedViewModel.context.resources.getString(R.string.current_location))
        popupMenu.menu.add(sharedViewModel.context.resources.getString(R.string.select_location_on_map))

        popupMenu.setOnMenuItemClickListener {
            when (it.title) {
                sharedViewModel.context.resources.getString(R.string.current_location) -> {
                    selectedStartPoint = sharedViewModel.context.resources.getString(R.string.current_location)
                    startPointInput.setText(selectedStartPoint)
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
        startPointInput.setOnClickListener {
            popupMenu.show()
        }

    }

    private fun selectRouteType(routeType: String) {
        selectedRouteType = routeType
        sharedViewModel.selectVehicle(routeType)
    }

    private fun showRouteTypeSection() {
        routeTypeSection.visibility = View.VISIBLE
        selectRouteType(sharedViewModel.selectedVehicle)
    }
}
