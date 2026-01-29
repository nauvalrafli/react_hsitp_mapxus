package com.mapxushsitp.wrapper_button

import android.util.Log
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.mapxushsitp.MapxusHsitpView
import com.mapxushsitp.service.MapxusUtility
import java.util.Locale

@ReactModule(name = MapxusButtonWrapperViewManager.NAME)
class MapxusButtonWrapperViewManager : ViewGroupManager<MapxusButtonWrapperView>() {

  companion object {
    const val NAME = "MapxusButtonWrapperView"
  }

  override fun getName(): String = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): MapxusButtonWrapperView {
    return MapxusButtonWrapperView(reactContext)
  }

  @ReactProp(name = "targetActivity")
  fun setTargetActivity(view: MapxusButtonWrapperView, value: String?) {
    view.setTargetActivity(value)
  }

  @ReactProp(name = "customLocale", customType = "String")
  fun setCustomLocale(view: MapxusButtonWrapperView?, locale: String?) {
    if (locale == null || locale == "none") {
      // Don't set locale
      return
    }

    Log.d("REACT-MAPXUS", locale)
    val parsedLocale = if (locale.contains("-")) {
      val parts = locale.split("-")
      Locale(parts[0], parts[1])
    } else {
      Locale(locale)
    }

    view?.locale = parsedLocale
    MapxusUtility.selectedLocale = parsedLocale
  }

  @ReactProp(name = "name", customType = "String")
  fun setCustomName(view: MapxusButtonWrapperView?, name: String?) {
    if (name == null) {
      return
    }

    view?.name = name
    MapxusUtility.name = name
  }
}
