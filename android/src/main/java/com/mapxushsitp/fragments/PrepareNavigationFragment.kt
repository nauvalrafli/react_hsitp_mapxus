package com.mapxushsitp.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import kotlin.getValue

class PrepareNavigationFragment : Fragment() {

    private lateinit var backButton: View
    private lateinit var startPointLayout: TextInputLayout
    private lateinit var startPointInput: AutoCompleteTextView
    private lateinit var destinationInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var routeTypeSection: LinearLayout
    private lateinit var shortestWalkChip: Chip
    private lateinit var liftOnlyChip: Chip
    private lateinit var escalatorOnlyChip: Chip
    private lateinit var showRouteButton: MaterialButton
    private lateinit var startNavigationButton: MaterialButton

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
        startPointLayout = view.findViewById(R.id.start_point_layout)
        startPointInput = view.findViewById(R.id.start_point_input)
        destinationInput = view.findViewById(R.id.destination_input)
        routeTypeSection = view.findViewById(R.id.route_type_section)
        shortestWalkChip = view.findViewById(R.id.shortest_walk_chip)
        liftOnlyChip = view.findViewById(R.id.lift_only_chip)
        escalatorOnlyChip = view.findViewById(R.id.escalator_only_chip)
        showRouteButton = view.findViewById(R.id.show_route_button)
        startNavigationButton = view.findViewById(R.id.start_navigation_button)

        destinationInput.setText(sharedViewModel.selectedPoi.value?.nameMap?.en + ", " + sharedViewModel.selectedPoi.value?.floor + ", " + sharedViewModel.selectedVenue.value?.nameMap?.en)
    }

    override fun onResume() {
        super.onResume()
        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        shortestWalkChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.FOOT)
        }

        liftOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.WHEELCHAIR)
        }

        escalatorOnlyChip.setOnClickListener {
            selectRouteType(RoutePlanningVehicle.ESCALATOR)
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
        popupMenu.menu.add("Current Location")
        popupMenu.menu.add("Select Location from Map")

        popupMenu.setOnMenuItemClickListener {
            when (it.title) {
                "Current Location" -> {
                    selectedStartPoint = "Current Location"
                    startPointInput.setText(selectedStartPoint)
                    sharedViewModel.startLatLng = sharedViewModel.userLocation
                    showRouteTypeSection()
                    sharedViewModel.selectedStartText = "Current Location"
                }
                "Select Location from Map" -> {
                    findNavController().navigate(R.id.action_prepareNavigation_to_positionMark)
                }
            }
            true
        }

        startPointLayout.setEndIconOnClickListener {
            popupMenu.show()
        }
        startPointInput.setOnClickListener {
            popupMenu.show()
        }

    }

    private fun selectRouteType(routeType: String) {
        selectedRouteType = routeType
        sharedViewModel.selectVehicle(routeType)

        // Update chip selection
        shortestWalkChip.isChecked = routeType == RoutePlanningVehicle.FOOT
        liftOnlyChip.isChecked = routeType == RoutePlanningVehicle.WHEELCHAIR
        escalatorOnlyChip.isChecked = routeType == RoutePlanningVehicle.ESCALATOR
    }

    private fun showRouteTypeSection() {
        routeTypeSection.visibility = View.VISIBLE
        selectRouteType(sharedViewModel.selectedVehicle)
    }
}
