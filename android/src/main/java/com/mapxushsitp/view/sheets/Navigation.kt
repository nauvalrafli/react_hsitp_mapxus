package com.mapxushsitp.view.sheets

sealed class Navigation(val route: String) {
    object SettingsView: Navigation("settings_view")
}
