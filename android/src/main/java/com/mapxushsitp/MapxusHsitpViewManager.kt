package com.mapxushsitp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import java.util.Locale

@ReactModule(name = MapxusHsitpViewManager.NAME)
class MapxusHsitpViewManager : SimpleViewManager<MapxusHsitpView>()  {
  override fun getName() = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): MapxusHsitpView {
    return MapxusHsitpView(reactContext)
  }

  override fun onDropViewInstance(view: MapxusHsitpView) {
    view.forceCleanup()
    super.onDropViewInstance(view)
  }

  override fun getCommandsMap(): Map<String, Int> {
    return mapOf("cleanup" to 1)
  }

  override fun receiveCommand(view: MapxusHsitpView, commandId: String?, args: ReadableArray?) {
    when (commandId) {
      "cleanup" -> view.forceCleanup()
    }
  }

  @ReactProp(name = "color")
  fun setColor(view: MapxusHsitpView?, color: String?) {
    Log.d("REACT-MAPXUS", color.toString())
    view?.setBackgroundColor(Color.parseColor(color))
  }

  @ReactProp(name = "customLocale", customType = "String")
  fun setCustomLocale(view: MapxusHsitpView?, locale: String?) {
    if (locale == null || locale == "none") {
      // Don't set locale
      return
    }

    Log.d("REACT-MAPXUS", locale)
    if (locale.contains("-")) {
      val split = locale.split("-")
      view?.locale = Locale(split[0], split[1])
    } else {
      view?.locale = Locale(locale)
    }
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
    return super.onCreateView(inflater, container, null)
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
