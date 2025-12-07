package com.mapxushsitp.service

import android.util.Log

object Cleaner {

    fun clearAllStaticReferences() {
        clearMapxusStaticReferences()
        clearMapLibreStaticReferences()
    }

    private fun clearMapxusStaticReferences() {
        try {
            val mapxusClientClass = Class.forName(
              "com.mapxus.positioning.positioning.api.MapxusPositioningClient"
            )

            val lifecycleOwnerField = mapxusClientClass.getDeclaredField("LIFECYCLE_OWNER")
            lifecycleOwnerField.isAccessible = true
            lifecycleOwnerField.set(null, null)

            Log.d("Cleanup", "Cleared MapxusPositioningClient")
        } catch (e: Exception) {
            Log.w("Cleanup", "Could not clear MapxusPositioningClient", e)
        }
    }

  private fun clearMapLibreStaticReferences() {
    try {
      val controllerClass = Class.forName(
        "org.maplibre.android.plugins.annotation.DraggableAnnotationController"
      )

      val instanceField = controllerClass.getDeclaredField("INSTANCE")
      instanceField.isAccessible = true
      val instance = instanceField.get(null)

      if (instance != null) {
        // Clear mapView field
        try {
          val mapViewField = controllerClass.getDeclaredField("mapView")
          mapViewField.isAccessible = true
          mapViewField.set(instance, null)
          Log.d("Cleanup", "Cleared DraggableAnnotationController.mapView")
        } catch (e: Exception) {
          Log.w("Cleanup", "Could not clear mapView", e)
        }

        // CRITICAL: Clear annotationManagers list
        try {
          val managersField = controllerClass.getDeclaredField("annotationManagers")
          managersField.isAccessible = true
          val managers = managersField.get(instance) as? MutableList<*>

          if (managers != null) {
            // Clear MapView reference from each manager
            managers.forEach { manager ->
              if (manager != null) {
                try {
                  val managerClass = manager.javaClass.superclass // AnnotationManager
                  val managerMapViewField = managerClass?.getDeclaredField("mapView")
                  managerMapViewField?.isAccessible = true
                  managerMapViewField?.set(manager, null)
                } catch (e: Exception) {
                  Log.w("Cleanup", "Could not clear manager mapView", e)
                }
              }
            }

            // Clear the list
            managers.clear()
            Log.d("Cleanup", "Cleared DraggableAnnotationController.annotationManagers")
          }
        } catch (e: Exception) {
          Log.w("Cleanup", "Could not clear annotationManagers", e)
        }
      }

      Log.d("Cleanup", "Cleared DraggableAnnotationController")
    } catch (e: Exception) {
      Log.w("Cleanup", "Could not clear DraggableAnnotationController", e)
    }
  }
}
