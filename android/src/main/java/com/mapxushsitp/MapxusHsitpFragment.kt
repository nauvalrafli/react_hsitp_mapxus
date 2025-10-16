package com.mapxushsitp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class MapxusHsitpFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val color: String? = arguments?.getString(ARG_COLOR)
    return MapxusHsitpView(requireContext()).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      if (color != null) {
        setBackgroundColor(android.graphics.Color.parseColor(color))
      }
    }
  }

  companion object {
    private const val ARG_COLOR = "color"
    fun newInstance(color: String? = null): MapxusHsitpFragment {
      return MapxusHsitpFragment().apply {
        arguments = Bundle().apply { putString(ARG_COLOR, color) }
      }
    }
  }
}


