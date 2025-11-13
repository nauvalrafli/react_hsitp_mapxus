package com.mapxushsitp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VenueDetailsFragment : Fragment() {

    private lateinit var venueName: TextView
    private lateinit var venueAddress: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var searchLayout: ConstraintLayout
    private lateinit var searchField: TextInputEditText
    private lateinit var tilSearch: TextInputLayout
    private lateinit var restroomCategory: View

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
        searchLayout = view.findViewById(R.id.search_layout)
        searchField = view.findViewById(R.id.et_search)
        tilSearch = view.findViewById(R.id.text_input_layout)
        restroomCategory = view.findViewById(R.id.restroom_category)

        // Prevent editing by removing key listener and preventing focus
        searchField.keyListener = null
        searchField.isFocusable = false
        searchField.isFocusableInTouchMode = false
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

        restroomCategory.setOnClickListener {
            findNavController().navigate(R.id.action_venueDetails_to_toiletScreen)
        }
    }

    private fun observeSharedViewModel() {
        // Observe selected venue from shared ViewModel
        sharedViewModel.selectedVenue.observe(viewLifecycleOwner, Observer { venue ->
            venue?.let {
                venueName.text = it.nameMap.getTranslation(sharedViewModel.locale)
                venueAddress.text = it.addressMap.getTranslation(sharedViewModel.locale).street
            }
        })
    }
}
