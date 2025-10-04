package com.mapxushsitp

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.mapxushsitp.view.HomeScreen
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class MapxusHsitpView : FrameLayout {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val expiryDateStr = "2025-11-01 00:00:00"
  val expiryDate = LocalDateTime.parse(expiryDateStr, formatter)

  val now = LocalDateTime.now()

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
    val homeScreen: ComposeView = HomeScreen.imperativeComposable(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }
    val expiryText = createExpiryText(context)
    if (now.isAfter(expiryDate)) {
      addView(expiryText)
    } else {
      addView(homeScreen)
    }
  }
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    val expiryText = createExpiryText(context)
    if (now.isAfter(expiryDate)) {
      addView(expiryText)
    } else {
      val homeScreen: ComposeView = HomeScreen.imperativeComposable(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
      addView(homeScreen)
    }
  }
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    val expiryText = createExpiryText(context)
    if (now.isAfter(expiryDate)) {
      addView(expiryText)
    } else {
      val homeScreen: ComposeView = HomeScreen.imperativeComposable(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
      addView(homeScreen)
    }
  }
}
