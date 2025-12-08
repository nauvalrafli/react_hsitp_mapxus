package com.mapxushsitp

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.facebook.react.uimanager.ThemedReactContext
import com.mapxushsitp.service.Cleaner
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
    Log.d("React-mapxus", container.toString())
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

  private fun clearMapLibreSingletons() {
    try {
      val controllerClass = Class.forName(
        "org.maplibre.android.plugins.annotation.DraggableAnnotationController"
      )

      val instanceField = controllerClass.getDeclaredField("INSTANCE")
      instanceField.isAccessible = true
      val instance = instanceField.get(null)

      if (instance != null) {
        val mapViewField = controllerClass.getDeclaredField("mapView")
        mapViewField.isAccessible = true
        mapViewField.set(instance, null)
      }

      Log.d("MapxusHsitpView", "Cleared MapLibre singletons")
    } catch (e: Exception) {
      Log.e("MapxusHsitpView", "Failed to clear MapLibre singletons", e)
    }
  }

  fun forceCleanup() {
    Log.d("MapxusHsitpView", "forceCleanup called")

    // Remove ViewTreeObserver listener
    globalLayoutListener?.let {
      try {
        viewTreeObserver.removeOnGlobalLayoutListener(it)
      } catch (e: Exception) {
        Log.e("MapxusHsitpView", "Error removing listener", e)
      }
    }

    // Clear third-party singletons
    clearMapLibreSingletons()
    clearMapxusStaticReference()

    // Detach fragment
    detachFragmentIfNecessary()

    // Clear container
    container?.removeAllViews()
    removeAllViews()

    fragmentAttached = false

    Log.d("MapxusHsitpView", "forceCleanup completed")
  }

  override fun onDetachedFromWindow() {
    clearMapxusStaticReference()
    Cleaner.clearAllStaticReferences()
    forceCleanup()

    super.onDetachedFromWindow()
    detachFragmentIfNecessary()
  }

  private fun clearMapxusStaticReference() {
    try {
      val mapxusClientClass = Class.forName(
        "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
      )

      val lifecycleOwnerField = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
      lifecycleOwnerField.isAccessible = true
      lifecycleOwnerField.set(null, null)

      Log.d("MapxusHsitpView", "Cleared LIFECYCLE_OWNER static field")
    } catch (e: Exception) {
      Log.e("MapxusHsitpView", "Failed to clear static reference", e)
    }
  }

  private fun clearMapxusStaticReferenceForcefully() {
    try {
      val mapxusClientClass = Class.forName(
        "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
      )

      // Clear LIFECYCLE_OWNER
      try {
        val field = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
        field.isAccessible = true
        field.set(null, null)
        Log.d("MapxusView", "Force cleared LIFECYCLE_OWNER")
      } catch (e: NoSuchFieldException) {
        Log.w("MapxusView", "LIFECYCLE_OWNER field doesn't exist")
      }

      // Also try to clear any instance references
      try {
        val instanceField = mapxusClientClass.getDeclaredField("instance")
        instanceField.isAccessible = true
        val instance = instanceField.get(null)
        if (instance != null) {
          // Try to call stop/destroy on the instance
          val stopMethod = mapxusClientClass.getMethod("stop")
          stopMethod.invoke(instance)
          Log.d("MapxusView", "Called stop on singleton instance")
        }
      } catch (e: Exception) {
        // Might not have an instance field or stop method
      }

    } catch (e: ClassNotFoundException) {
      Log.e("MapxusView", "MapxusPositioningClient class not found", e)
    } catch (e: Exception) {
      Log.e("MapxusView", "Error clearing static references", e)
    }
  }

  var fragment : XmlFragment? = null

  private fun attachFragmentIfNecessary() {
    if (fragmentAttached) return
    val activity = reactContext?.currentActivity as? FragmentActivity ?: return

    clearMapxusStaticReferenceForcefully()

    val tag = fragmentTag()
    val existing = activity.supportFragmentManager.findFragmentByTag(tag)
    Log.d("REACT-MAPXUS Ex", "Existing: $existing")

    if (existing != null) {
      // Fragment exists but might be detached - reattach it
      if (existing.isDetached) {
        activity.supportFragmentManager.beginTransaction()
          .remove(existing)
          .commitNow()
      }
      fragmentAttached = true
      return
    }

    if (container != null) {
      container?.visibility = View.VISIBLE
      container?.layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
      )

      // Force layout before attaching fragment
      container?.requestLayout()
      container?.invalidate()

      fragment = XmlFragment(
        locale = locale,
      ).apply {
        arguments = Bundle().apply {
          putInt("id", id)
        }
      }
      activity.resources.configuration.setLocale(locale)
      activity.supportFragmentManager.commit {
        setReorderingAllowed(true)
        if(fragment != null)
          replace(container?.id ?: 0, fragment!!, fragmentTag())
      }
      viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }
  }

  val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
    if(fragment?.view != null) {
      if((fragment?.requireView()?.height ?: 0) > 0 && (fragment?.requireView()?.width ?: 0) > 0) {
        fragmentAttached = true
      }
      val fragView = fragment?.view
      val parent = fragView?.parent as? ViewGroup

      val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
      val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

      fragView?.measure(widthSpec, heightSpec)
      fragView?.layout(0, 0, width, height)
      fragView?.invalidate()
    }
  }

  private fun detachFragmentIfNecessary() {
    val activity = reactContext?.currentActivity as? FragmentActivity ?: return
    activity.supportFragmentManager.findFragmentByTag(fragmentTag())?.let { fragment ->
      activity.supportFragmentManager.commit() {
        remove(fragment)
        System.gc()
      }
    }
    viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
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
