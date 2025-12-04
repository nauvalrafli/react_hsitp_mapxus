package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.mapxus.map.mapxusmap.api.services.PoiSearch
import com.mapxus.map.mapxusmap.api.services.model.DetailSearchOption
import com.mapxus.map.mapxusmap.api.services.model.PoiSearchOption
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiCategoryResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiDetailResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiOrientationResult
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiResult
import kotlin.getValue

class PoiDetailsFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var poiTitle: TextView
    private lateinit var poiSubtitle: TextView
    private lateinit var shareButton: ImageButton
    private lateinit var facilitiesChip: Chip
    private lateinit var directionButton: Button
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_poi_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        updatePoiInfo()
        sharedViewModel.bottomSheet?.post {
            sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        poiTitle = view.findViewById(R.id.poi_title)
        poiSubtitle = view.findViewById(R.id.poi_subtitle)
        shareButton = view.findViewById(R.id.share_button)
//        facilitiesChip = view.findViewById(R.id.facilities_chip)
        directionButton = view.findViewById(R.id.direction_button)
        directionButton.text = sharedViewModel.context.resources.getString(R.string.direction)

        sharedViewModel.startLatLng = null
        sharedViewModel.selectedStartText = ""
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            sharedViewModel.mapxusMap?.removeMapxusPointAnnotations()
            findNavController().navigateUp()
        }

        shareButton.setOnClickListener {
            // TODO: Implement share functionality
        }

        directionButton.setOnClickListener {
            findNavController().navigate(R.id.action_poiDetails_to_prepareNavigation)
            sharedViewModel.bottomSheet?.post {
                sharedViewModel.bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    fun updatePoiInfo() {
        sharedViewModel.selectedPoi.observe(viewLifecycleOwner) {
            poiTitle.text = it?.nameMap?.getTranslation(sharedViewModel.locale)
            val subtitle = it?.floor + " - " + (sharedViewModel.selectedBuilding.value?.buildingNamesMap?.getTranslation(sharedViewModel.locale) ?: sharedViewModel.selectedVenue.value?.nameMap?.getTranslation(sharedViewModel.locale))
            poiSubtitle.text = subtitle
        }
    }
}
