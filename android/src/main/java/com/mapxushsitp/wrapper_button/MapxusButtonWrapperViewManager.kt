package com.mapxushsitp.wrapper_button

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp

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
}
