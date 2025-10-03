package com.mapxushsitp.view.sheets

import androidx.compose.material.Surface
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.view.component.mapxus_compose.MapxusController

interface IScreen {
    abstract val routeName : String

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    abstract fun Screen(toiletTitle: String, navController: NavHostController, mapxusController: MapxusController, sheetState: BottomSheetScaffoldState, arNavigationViewModel: ARNavigationViewModel)
}
