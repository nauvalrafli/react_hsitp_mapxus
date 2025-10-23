package com.mapxushsitp

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.mapbox.mapboxsdk.maps.MapView
import com.mapxushsitp.view.HomeScreen

@ReactModule(name = MapxusHsitpViewManager.NAME)
class MapxusHsitpViewManager : ViewGroupManager<MapxusWrapperView>() {
  override fun getName() = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): MapxusWrapperView {
    return MapxusWrapperView(reactContext)
  }


  companion object {
    const val NAME = "MapxusHsitpView"
  }
}

class MapxusWrapperFragment(private var frame: MapxusHsitpView): Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    return frame
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    HomeScreen.controller?.mapView?.onCreate(savedInstanceState)
  }

  override fun onResume() {
    super.onResume()
    HomeScreen.controller?.mapView?.onResume()
  }

  override fun onStart() {
    super.onStart()
    HomeScreen.controller?.mapView?.onStart()
  }

  override fun onPause() {
    super.onPause()
    HomeScreen.controller?.mapView?.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    HomeScreen.controller?.mapView?.onDestroy()
  }

  override fun onStop() {
    super.onStop()
    HomeScreen.controller?.mapView?.onStop()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    HomeScreen.controller?.mapView?.onLowMemory()
  }
}

class MapxusWrapperView(context: Context) : FrameLayout(context) {
  val mapxusView = MapxusHsitpView(context)

  private var fragmentCreated = false
  val wrapperFragment = MapxusWrapperFragment(mapxusView)

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    // Defer fragment creation to avoid blocking main thread
    post {
      if (!fragmentCreated) {
        addFragment.run()
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    post {
      val activity = (context as? ThemedReactContext)?.currentActivity as FragmentActivity
      activity.supportFragmentManager
        .beginTransaction()
        // the id value here is the react native view id that
        // has been assigned by the view manager system for this view instance
        .remove(wrapperFragment)
        .commit()
    }
  }

  override fun requestLayout() {
    super.requestLayout()

  }

  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private val addFragment = Runnable {
    if (!fragmentCreated) {
      val activity = (context as? ThemedReactContext)?.currentActivity as FragmentActivity
      if(id == View.NO_ID) {
        id = generateViewId()
      }
      if (activity.supportFragmentManager.findFragmentByTag(id.toString()) == null) {
        activity.supportFragmentManager
          .beginTransaction()
          // the id value here is the react native view id that
          // has been assigned by the view manager system for this view instance
          .replace(id, wrapperFragment, id.toString())
          .commitAllowingStateLoss()
      }

      fragmentCreated = true
    }
  }

  private val measureAndLayout = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(left, top, right, bottom)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

    // wait until the fragment has been embedded into the view and the
    // children are ready to measure - else it will give a (0,0) size and
    // not layout correctly
    if (children.count() == 0) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      return
    }

    var maxWidth = 0
    var maxHeight = 0

    children.forEach {
      it.measure(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
      maxWidth = maxWidth.coerceAtLeast(it.measuredWidth)
      maxHeight = maxHeight.coerceAtLeast(it.measuredHeight)
    }

    val finalWidth = maxWidth.coerceAtLeast(suggestedMinimumWidth)
    val finalHeight = maxHeight.coerceAtLeast(suggestedMinimumHeight)
    setMeasuredDimension(finalWidth, finalHeight)
    (context as? ThemedReactContext)?.let { themedReactContext ->
      themedReactContext.runOnNativeModulesQueueThread {
        themedReactContext.getNativeModule(UIManagerModule::class.java)
          ?.updateNodeSize(id, finalWidth, finalHeight)
      }
    }

  }
}
