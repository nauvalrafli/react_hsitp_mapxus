package com.mapxushsitp.view.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.view.component.mapxus_compose.MapxusController

object PositionMark: IScreen {
    override val routeName: String = "Position"

    @ExperimentalMaterial3Api
    @Composable
    override fun Screen(
        toiletTitle: String,
        navController: NavHostController,
        mapxusController: MapxusController,
        sheetState: BottomSheetScaffoldState,
        arNavigationViewModel: ARNavigationViewModel
    ) {
        val context = LocalContext.current
        Button(
            onClick = {
                mapxusController.destinationPoint?.let { dest ->
                    val lat = dest.lat
                    val lon = dest.lon
                    if (lat != null && lon != null) {
                        mapxusController.setStartWithCenter(lat, lon)
                    }
                }

                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            )
        ) {
            Text("Set Start Location", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
