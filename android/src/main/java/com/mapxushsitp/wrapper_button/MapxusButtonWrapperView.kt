package com.mapxushsitp.wrapper_button

import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.views.view.ReactViewGroup
import com.mapxushsitp.XmlActivity

class MapxusButtonWrapperView(context: ThemedReactContext) : ReactViewGroup(context) {

  private var targetActivityClass: Class<*>? = null

  private var lastClick = 0L

  init {
    isClickable = true
    isFocusable = true

    // This ensures touch goes to parent before children
    setOnTouchListener { _, event ->
      Log.d("REACT-MAPXUS", "Launching activity: $event")
//      performClick()
      if (event?.action == MotionEvent.ACTION_UP) {
        Log.d("REACT-MAPXUS", "Single click detected")
        launchActivity()
      }
//      val now = System.currentTimeMillis()
//      if (now - lastClick > 1000) {
//        lastClick = now
//        launchActivity()
//      }
      return@setOnTouchListener true  // <--- CONSUME TOUCH
    }

    setOnClickListener {
      Log.d("REACT-MAPXUS", "Launching activity click")
      launchActivity()
    }
  }

  fun setTargetActivity(activityClassName: String?) {
    if (activityClassName != null) {
      try {
        targetActivityClass = Class.forName(activityClassName)
      } catch (e: ClassNotFoundException) {
        e.printStackTrace()
      }
    }
  }

  private fun launchActivity() {
    Log.d("REACT-MAPXUS", "Launching activity: $targetActivityClass")
    val intent = Intent(context, XmlActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
  }

  // Allow children to be added (the customizable content)
  override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
    super.addView(child, index, params)
  }
}
