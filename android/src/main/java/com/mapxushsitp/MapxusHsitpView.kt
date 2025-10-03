package com.mapxushsitp

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.mapxushsitp.view.HomeScreen
import android.view.ViewGroup

class MapxusHsitpView : FrameLayout {

  lateinit var homeScreen: ComposeView

  constructor(context: Context) : super(context) {
    homeScreen = HomeScreen.imperativeComposable(context).apply {
      layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT 
      )
    }
    addView(homeScreen)
  }
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    homeScreen = HomeScreen.imperativeComposable(context).apply {
      layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT 
      )
    }
    addView(homeScreen)
  }
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    homeScreen = HomeScreen.imperativeComposable(context).apply {
      layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT 
      )
    }
    addView(homeScreen)
  }
}
