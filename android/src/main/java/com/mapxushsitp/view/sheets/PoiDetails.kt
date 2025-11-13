@file:OptIn(ExperimentalMaterial3Api::class)

package com.mapxushsitp.view.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.service.DynamicStringResource
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.view.component.mapxus_compose.MapxusController
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint

object PoiDetails : IScreen {
    override val routeName: String = "poiDetails"

    @Composable
    override fun Screen(
        toiletTitle: String,
        navController: NavHostController,
        mapxusController: MapxusController,
        sheetState: BottomSheetScaffoldState,
        arNavigationViewModel: ARNavigationViewModel
    ) {
        val selectedPoi = mapxusController.selectedPoi

        LaunchedEffect(true) {
            mapxusController.destinationPoint = RoutePlanningPoint(selectedPoi?.lng ?: 0.0, selectedPoi?.lat ?: 0.0, selectedPoi?.floorId)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Header with back button and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = {
                        navController.navigateUp()
                        mapxusController.getMapxusMap().removeMapxusPointAnnotations()
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                // Title and subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = DynamicStringResource(selectedPoi?.type ?: "", selectedPoi?.type ?: ""),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedPoi?.floorName}, ${mapxusController.selectedVenue?.name?.getTranslation(mapxusController.locale) ?: ""}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Share button
                IconButton(
                    onClick = { /* Handle share */ },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags/Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
//                AssistChip(
//                    onClick = { },
//                    label = { Text(stringResource(R.string.restroom), fontSize = 12.sp, fontWeight = FontWeight.Normal) },
//                    leadingIcon = {
//                        Box(
//                            modifier = Modifier
//                                .size(8.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFF7E57C2))
//                        )
//                    }
//                )
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.facilities), fontSize = 12.sp, fontWeight = FontWeight.Normal) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7E57C2))
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Direction button
            Button(
                onClick = { navController.navigate(PrepareNavigation.routeName) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4)
                )
            ) {
                Text(
                    stringResource(R.string.direction),
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
