package com.mapxushsitp

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.mapxus.map.mapxusmap.api.map.MapxusMapContext
import java.util.ArrayList
import com.facebook.react.bridge.UiThreadUtil
import com.mapxushsitp.MapxusHsitpViewManager

class MapxusHsitpViewPackage : ReactPackage {
  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    val viewManagers: MutableList<ViewManager<*, *>> = ArrayList()
    viewManagers.add(MapxusHsitpViewManager())
    return viewManagers
  }

  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    // Initialize MapxusMapContext asynchronously to avoid blocking
    UiThreadUtil.runOnUiThread {
      try {
        MapxusMapContext.init(reactContext.applicationContext)
      } catch (e: Exception) {
        // Log error but don't crash the app
        android.util.Log.e("MapxusHsitp", "Failed to initialize MapxusMapContext", e)
      }
    }
    return emptyList()
  }
}
