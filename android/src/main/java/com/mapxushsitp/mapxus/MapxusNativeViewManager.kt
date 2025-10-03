package com.mapxushsitp.mapxus

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class MapxusNativeViewManager : SimpleViewManager<MapxusNativeView>() {

  override fun getName(): String = REACT_CLASS

  override fun createViewInstance(reactContext: ThemedReactContext): MapxusNativeView {
    return MapxusNativeView(reactContext)
  }

  companion object {
    const val REACT_CLASS = "MapxusNativeView"
  }
}


