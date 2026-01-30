package com.mapxushsitp.fragments

import android.content.SharedPreferences
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
import androidx.core.app.NotificationCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapxushsitp.XmlActivity
import com.mapxushsitp.data.model.RoutePlanningPoint
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle

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
    private lateinit var adsorberSeekBar: android.widget.SeekBar
    private lateinit var adsorberValue: TextView

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
        adsorberSeekBar = view.findViewById(R.id.adsorber_seekbar)
        adsorberValue = view.findViewById(R.id.adsorber_value)
        chips = listOf(shortestWalkChip, liftOnlyChip, escalatorOnlyChip)

        if(sharedViewModel.selectedStartText.isNotEmpty()) {
            startPointInput.setText(sharedViewModel.selectedStartText)
            showRouteTypeSection()
        }
        destinationInput.setText(sharedViewModel.selectedPoi.value?.nameMap?.en + ", " + sharedViewModel.selectedPoi.value?.floor + ", " + sharedViewModel.selectedBuilding.value?.buildingNamesMap?.en)
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
        // initialize adsorber seekbar: range 5.0..25.0 with step 0.5 -> seekbar max 40
        val initial = sharedViewModel.adsorberDistance.value ?: 10.0
        val progress = ((initial - 5.0) * 2).toInt().coerceIn(0,40)
        adsorberSeekBar.max = 40
        adsorberSeekBar.progress = progress
        adsorberValue.text = String.format("%.1f", 5.0 + progress * 0.5)
        adsorberSeekBar.setOnSeekBarChangeListener(object: android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                val v = 5.0 + p * 0.5
                adsorberValue.text = String.format("%.1f", v)
                if (fromUser) {
                    sharedViewModel.setAdsorberDistance(v)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
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
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
                    sharedViewModel.startLatLng = com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint(
                        sharedViewModel.userLocation?.longitude ?: 0.0,
                        sharedViewModel.userLocation?.latitude ?: 0.0,
                        sharedViewModel.userLocation?.mapxusFloor?.id
                    )
                    showRouteTypeSection()
                    sharedViewModel.selectedStartText = "Current Location"
                    sharedViewModel.bottomSheet?.postDelayed({
                      sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    }, 200)
                }
                "Select Location from Map" -> {
                    findNavController().navigate(R.id.action_prepareNavigation_to_positionMark)
                    sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
