package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.button.MaterialButton
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusPointAnnotationOptions
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint

class PositionMarkFragment : Fragment() {

    private lateinit var setStartLocationButton: Button
    val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_position_mark, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        sharedViewModel.selectionMark?.visibility = View.VISIBLE
    }

    private fun initializeViews(view: View) {
        setStartLocationButton = view.findViewById(R.id.set_start_location_button)
        setStartLocationButton.text = getString(R.string.set_start_location)
    }

    private fun setupClickListeners() {
        setStartLocationButton.setOnClickListener {
            val latlng = sharedViewModel.mapxusMap?.cameraPosition?.target
            sharedViewModel.selectedStartText = "${latlng?.latitude}, ${latlng?.longitude}"
            sharedViewModel.startLatLng = RoutePlanningPoint(
                latlng?.longitude ?: 0.0,
                latlng?.latitude ?: 0.0,
                sharedViewModel.mapxusMap?.selectedFloor?.id
            )
            sharedViewModel.mapxusMap?.addMapxusPointAnnotation(MapxusPointAnnotationOptions().apply {
              this.position = latlng
            })
            sharedViewModel.selectionMark?.visibility = View.GONE
            findNavController().navigateUp()
        }


    }
}
