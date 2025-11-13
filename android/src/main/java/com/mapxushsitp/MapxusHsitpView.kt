package com.mapxushsitp

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.facebook.react.uimanager.ThemedReactContext

@RequiresApi(Build.VERSION_CODES.O)
class MapxusHsitpView : FrameLayout {

  private val fragmentContainerId = View.generateViewId()
  private var fragmentAttached = false

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

  private fun initView(context: Context) {
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    if (findViewById<View>(fragmentContainerId) == null) {
      val container = FrameLayout(context).apply {
        id = fragmentContainerId
        layoutParams = LayoutParams(
          LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT
        )
      }
      addView(container)
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    attachFragmentIfNecessary()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    detachFragmentIfNecessary()
  }

  private fun attachFragmentIfNecessary() {
    if (fragmentAttached) return
    val themedContext = context as? ThemedReactContext ?: return
    val activity = themedContext.currentActivity as? FragmentActivity ?: return

    val tag = fragmentTag()
    val existing = activity.supportFragmentManager.findFragmentByTag(tag)
    if (existing == null) {
//      val fragment = XmlActivity.newInstance()
      activity.supportFragmentManager.commit(allowStateLoss = true) {
        setReorderingAllowed(true)
        replace(fragmentContainerId, fragment, tag)
      }
    }
    fragmentAttached = true
  }

  private fun detachFragmentIfNecessary() {
    val themedContext = context as? ThemedReactContext ?: return
    val activity = themedContext.currentActivity as? FragmentActivity ?: return
    activity.supportFragmentManager.findFragmentByTag(fragmentTag())?.let { fragment ->
      activity.supportFragmentManager.commit(allowStateLoss = true) {
        remove(fragment)
      }
    }
    fragmentAttached = false
  }

  private fun fragmentTag(): String = "MapxusHsitpView.XmlActivity.${id}"
}
