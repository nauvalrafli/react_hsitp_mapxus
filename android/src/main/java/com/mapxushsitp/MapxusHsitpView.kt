package com.mapxushsitp

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.mapxushsitp.view.HomeScreen
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.core.view.doOnAttach
import androidx.core.view.doOnNextLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import androidx.core.view.isEmpty

@RequiresApi(Build.VERSION_CODES.O)
class MapxusHsitpView : FrameLayout {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val expiryDateStr = "2025-11-01 00:00:00"
  val expiryDate = LocalDateTime.parse(expiryDateStr, formatter)

  val now = LocalDateTime.now()

  val homeScreen = HomeScreen.imperativeComposable(context)

  fun createExpiryText(context: Context): TextView {
    return TextView(context).apply {
      text = "Update your package to the latest version."
      textSize = 18f
      typeface = Typeface.DEFAULT_BOLD
      setPadding(32, 32, 32, 32)
      textAlignment = TEXT_ALIGNMENT_CENTER
      layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.CENTER
      }
    }
  }

  constructor(context: Context) : super(context) {
    Log.d("REACT-MAPXUS", "CONSTRUCT")
    initView(context)
  }
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    Log.d("REACT-MAPXUS", "CONSTRUCT")
    initView(context)
  }
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    Log.d("REACT-MAPXUS", "CONSTRUCT")
    initView(context)
  }

  fun initView(context: Context) {
    val expiryText = createExpiryText(context)
    Log.d("REACT-MAPXUS", "INIT")
    if (now.isAfter(expiryDate)) {
      addView(expiryText)
    } else {
      if(isAttachedToWindow) {
        Log.d("REACT-MAPXUS", "ATTACHED")
        homeScreen.apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
        if(isEmpty()) {
          addView(homeScreen)
        }
      } else {
        viewTreeObserver.addOnWindowAttachListener(object: ViewTreeObserver.OnWindowAttachListener {
          override fun onWindowAttached() {
            Log.d("REACT-MAPXUS", "OBSERVER ATTACHED")
            homeScreen.apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
            }
            if(isEmpty()) {
              addView(homeScreen)
            }
            viewTreeObserver.removeOnWindowAttachListener(this)
            invalidate()
          }

          override fun onWindowDetached() {

          }
        })
      }
      invalidate()
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!now.isAfter(expiryDate)) {
      Log.d("REACT-MAPXUS", "ONATTACHED")
      homeScreen.apply {
        layoutParams = LayoutParams(
          LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT
        )
      }

      post {
        if(isEmpty()) {
          addView(homeScreen)
        }
        
        // Defer heavy operations to next frame
        post {
          val parentWidth = width
          val parentHeight = height
          if (parentWidth > 0 && parentHeight > 0) {
            val widthSpec = MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY)

            homeScreen.measure(widthSpec, heightSpec)
            homeScreen.layout(0, 0, parentWidth, parentHeight)
          }

          requestLayout()
          invalidate()

          Log.d("REACT-MAPXUS", "Measured: ${homeScreen.width}x${homeScreen.height}")
        }
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    removeView(homeScreen);
  }
}
