package com.mapxushsitp

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.facebook.react.uimanager.ThemedReactContext

@RequiresApi(Build.VERSION_CODES.O)
class MapxusHsitpView : FrameLayout {

  private val fragmentContainerId = View.generateViewId()
  private var fragmentAttached = false
  private var container: FrameLayout? = null

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
    container = FrameLayout(context).apply {
      id = fragmentContainerId
      layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
      )
      visibility = View.VISIBLE
    }
    addView(container)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    Log.d("REACT-MAPXUS", "onAttachedToWindow - width=$width, height=$height")
    post {
      post {
        Log.d("REACT-MAPXUS", "After post - width=$width, height=$height, container=${container?.width}*${container?.height}")
        attachFragmentIfNecessary()
        requestLayout()
        invalidate()
      }
    }
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    // Ensure container fills the parent after layout
    container?.let { container ->
      val newWidth = right - left
      val newHeight = bottom - top
      if (container.width != newWidth || container.height != newHeight) {
        container.layoutParams = LayoutParams(newWidth, newHeight)
        container.layout(0, 0, newWidth, newHeight)
        container.visibility = View.VISIBLE

        Log.d("REACT-MAPXUS", "Layout: Parent=$newWidth*$newHeight, Container=${container.width}*${container.height}")
      }
    }
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
    if (existing == null && container != null) {
      // CRITICAL: Ensure container is visible and has proper dimensions
      container?.visibility = View.VISIBLE
      container?.layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
      )

      // Force layout before attaching fragment
      container?.requestLayout()
      container?.invalidate()

      val fragment = XmlFragment()
      activity.supportFragmentManager.commit {
        replace(container?.id ?: 0, fragment, fragmentTag())
      }
      viewTreeObserver.addOnGlobalLayoutListener {
        if(fragment.view != null) {
          if(fragment.view!!.height > 0 && fragment.view!!.width > 0) {
            fragmentAttached = true
          }
          val fragView = fragment.view
          val parent = fragView?.parent as? ViewGroup

          val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
          val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

          fragView?.measure(widthSpec, heightSpec)
          fragView?.layout(0, 0, width, height)
          fragView?.invalidate()

          Log.d("REACT-MAPXUS", "h: ${fragment.view?.height} w: ${fragment.view?.width}")
          Log.d("REACT-MAPXUS", "ch: ${container?.height} cw: ${container?.width}")
          Log.d("REACT-MAPXUS", "ph: $height pw: $width")
        }
      }
    }
  }

  private fun detachFragmentIfNecessary() {
    val themedContext = context as? ThemedReactContext ?: return
    val activity = themedContext.currentActivity as? FragmentActivity ?: return
//    activity.supportFragmentManager.findFragmentByTag(fragmentTag())?.let { fragment ->
//      activity.supportFragmentManager.commit(allowStateLoss = true) {
//        remove(fragment)
//      }
//    }
    fragmentAttached = false
  }

  private fun fragmentTag(): String = "MapxusHsitpView.XmlActivity.${id}"
}
