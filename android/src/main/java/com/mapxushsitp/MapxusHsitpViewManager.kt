package com.mapxushsitp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = MapxusHsitpViewManager.NAME)
class MapxusHsitpViewManager : ViewGroupManager<MapxusHsitpView>() {
  override fun getName() = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): MapxusHsitpView {
    return MapxusHsitpView(reactContext)
  }

  @ReactProp(name = "color")
  fun setColor(view: MapxusHsitpView?, color: String?) {
    view?.setBackgroundColor(Color.parseColor(color))
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
    return super.onCreateView(inflater, container, savedInstanceState)
    return frame
  }
}

class MapxusWrapperView(context: Context) : FrameLayout(context) {
  val mapxusView = MapxusHsitpView(context)

  private var fragmentCreated = false

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
//    post(addFragment)
  }

  override fun requestLayout() {
    super.requestLayout()

  }

  private val addFragment = Runnable {
    if (!fragmentCreated) {
      val wrapperFragment = MapxusWrapperFragment(mapxusView)
      val activity = (context as? ThemedReactContext)?.currentActivity as FragmentActivity
      activity.supportFragmentManager
        .beginTransaction()
        // the id value here is the react native view id that
        // has been assigned by the view manager system for this view instance
        .replace(id, wrapperFragment, id.toString())
        .commit()

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
