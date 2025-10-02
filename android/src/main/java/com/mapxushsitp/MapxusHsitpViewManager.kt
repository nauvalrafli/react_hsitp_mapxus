package com.mapxushsitp

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.MapxusHsitpViewManagerInterface
import com.facebook.react.viewmanagers.MapxusHsitpViewManagerDelegate

@ReactModule(name = MapxusHsitpViewManager.NAME)
class MapxusHsitpViewManager : SimpleViewManager<MapxusHsitpView>(),
  MapxusHsitpViewManagerInterface<MapxusHsitpView> {
  private val mDelegate: ViewManagerDelegate<MapxusHsitpView>

  init {
    mDelegate = MapxusHsitpViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<MapxusHsitpView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): MapxusHsitpView {
    return MapxusHsitpView(context)
  }

  @ReactProp(name = "color")
  override fun setColor(view: MapxusHsitpView?, color: String?) {
    view?.setBackgroundColor(Color.parseColor(color))
  }

  companion object {
    const val NAME = "MapxusHsitpView"
  }
}
