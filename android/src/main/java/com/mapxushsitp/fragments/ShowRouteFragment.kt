package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R

class ShowRouteFragment : Fragment() {

    private lateinit var backButton: View
    private val sharedViewModel: MapxusSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_show_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            sharedViewModel.routePainter?.cleanRoute()
            findNavController().navigateUp()
        }
    }
}

