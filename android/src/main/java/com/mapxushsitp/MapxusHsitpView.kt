package com.mapxushsitp

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.facebook.react.uimanager.ThemedReactContext
import com.mapxushsitp.theme.MaterialThemeUtils
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class MapxusHsitpView : FrameLayout {

  private val fragmentContainerId = View.generateViewId()
  private var fragmentAttached = false
  private var container: FrameLayout? = null
  private var reactContext: ThemedReactContext? = null

  var locale: Locale = Locale.getDefault()
    set(value) {
      if (field == value) return
      field = value
      refreshFragmentForLocaleChange()
    }

  constructor(context: Context) : super(MaterialThemeUtils.ensureMaterialContext(context)) {
    reactContext = context as? ThemedReactContext
    initView()
  }

  constructor(context: Context, attrs: AttributeSet?) : super(MaterialThemeUtils.ensureMaterialContext(context), attrs) {
    reactContext = context as? ThemedReactContext
    initView()
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    MaterialThemeUtils.ensureMaterialContext(context),
    attrs,
    defStyleAttr
  ) {
    reactContext = context as? ThemedReactContext
    initView()
  }

  private fun initView() {
    container = FrameLayout(context).apply {
      id = fragmentContainerId
      layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
      )
      visibility = View.VISIBLE
    }
    addView(container)
    val activity = reactContext?.currentActivity as? FragmentActivity ?: return
    val config = Configuration().apply {
      setLocale(this@MapxusHsitpView.locale)
    }

    val localizedContext = activity.createConfigurationContext(config)
    val resource = localizedContext.resources

  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    post {
      post {
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
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    detachFragmentIfNecessary()
  }

  private fun attachFragmentIfNecessary() {
    if (fragmentAttached) return
    val activity = reactContext?.currentActivity as? FragmentActivity ?: return

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

      val fragment = XmlFragment(
        locale = locale,
      )
      activity.resources.configuration.setLocale(locale)
      activity.supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace(container?.id ?: 0, fragment, fragmentTag())
      }
      viewTreeObserver.addOnGlobalLayoutListener {
        if(fragment.view != null) {
          if(fragment.requireView().height > 0 && fragment.requireView().width > 0) {
            fragmentAttached = true
          }
          val fragView = fragment.view
          val parent = fragView?.parent as? ViewGroup

          val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
          val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

          fragView?.measure(widthSpec, heightSpec)
          fragView?.layout(0, 0, width, height)
          fragView?.invalidate()
        }
      }
    }
  }

  private fun detachFragmentIfNecessary() {
    val activity = reactContext?.currentActivity as? FragmentActivity ?: return
    activity.supportFragmentManager.findFragmentByTag(fragmentTag())?.let { fragment ->
      activity.supportFragmentManager.commit() {
        remove(fragment)
      }
    }
    fragmentAttached = false
  }

  private fun refreshFragmentForLocaleChange() {
    if (!fragmentAttached || !isAttachedToWindow) {
      return
    }

    post {
      detachFragmentIfNecessary()
      fragmentAttached = false
      attachFragmentIfNecessary()
    }
  }

  private fun fragmentTag(): String = "MapxusHsitpView.XmlActivity.${id}"
}
