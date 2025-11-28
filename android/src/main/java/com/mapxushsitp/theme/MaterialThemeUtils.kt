package com.mapxushsitp.theme

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import com.mapxushsitp.R

object MaterialThemeUtils {

  fun ensureMaterialContext(base: Context): Context {
    val typedValue = TypedValue()
    val hasMaterialFlag = base.theme.resolveAttribute(
      com.google.android.material.R.attr.isMaterialTheme,
      typedValue,
      true
    ) && typedValue.type == TypedValue.TYPE_INT_BOOLEAN && typedValue.data != 0

    return if (hasMaterialFlag) {
      base
    } else {
      ContextThemeWrapper(base, R.style.MapxusHsitpTheme)
    }
  }
}

