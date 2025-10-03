package com.mapxushsitp.view.component.mapxus_compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.static.floorList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapxusComposable(
    modifier: Modifier,
    controller: MapxusController,
    arNavigationViewModel: ARNavigationViewModel,
    isShowingARToolTipMessage: Boolean
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isShowingARToolTipMessage) {
        if (isShowingARToolTipMessage) {
            coroutineScope.launch {
                tooltipState.show(MutatePriority.PreventUserInput)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {

        Box(
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    controller.mapView
                },
                modifier = Modifier.fillMaxSize()
            )

            if (controller.selectingCenter.value) {
                Image(
                    painter = painterResource(R.drawable.ic_venue_location),
                    contentDescription = "Selecting User Location",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        if(controller.isFloorSelectorShown.value)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {

                FloorSelector(controller = controller, floorList = floorList.map { it -> Floor(it.id, it.code) }, onSelectedFloorChanged = {
                    controller.getMapxusMap().selectFloorById(it)
                }, userCurrentFloor = controller.userCurrentFloor.value)
            }

        Column(modifier = Modifier.padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(
                onClick = {
                    val zoomLevel = controller.mapboxMap?.cameraPosition?.zoom
                    controller.getMapxusMap().followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
                    coroutineScope.launch {
                        delay(500)
                        controller.mapboxMap?.cameraPosition = com.mapbox.mapboxsdk.camera.CameraPosition.Builder().zoom(zoomLevel ?: 19.0).build()
                    }
                },
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.End)
                    .background(color = Color.White, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GpsFixed,
                    contentDescription = null,
                    tint = Color(0xFF4285F4)
                )
            }

            if (controller.isNavigating) {
                IconButton(
                    onClick = {
                        controller.isSpeaking.value = !controller.isSpeaking.value
                        controller.sharedPreferences.edit().apply {
                            putBoolean("isSpeaking", controller.isSpeaking.value)
                            apply()
                        }
                    },
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.End)
                        .background(color = Color.White, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if(controller.isSpeaking.value) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = null,
                        tint = if(controller.isSpeaking.value) Color(0xFF4285F4) else Color.LightGray
                    )
                }
            }

            if (arNavigationViewModel.isShowingOpeningAndClosingARButton.value) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(8.dp),
                    tooltip = {
                        RichTooltip(
                            modifier = Modifier.padding(end = 16.dp),
                            shape = RichTooltipWithPointerShape(),
                            title = { Text("AR Navigation", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                            action = {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            tooltipState.dismiss()
                                        }
                                    }
                                ) {
                                    Text("Got it!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Click this Button to show and hide the AR Navigation.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
//                                Text(
//                                    "And before starting the Navigation, click the 'Show Route' button and make sure that the Arrow of the GPS aligned with the Blue Line (Road path) on the Map. Then you can start the Navigation by clicking the 'Start Navigation' button.",
//                                    fontSize = 12.sp,
//                                    fontWeight = FontWeight.Normal,
//                                    fontStyle = FontStyle.Italic
//                                )
                            }
                        }
                    },
                    state = tooltipState
                ) {
                    IconButton(
                        onClick = {
                            arNavigationViewModel.isShowingAndClosingARNavigation.value = !arNavigationViewModel.isShowingAndClosingARNavigation.value
//                            arNavigationViewModel.isShowingCountdownScreen.value = !arNavigationViewModel.isShowingCountdownScreen.value
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.End)
                            .background(color = Color.White, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NearMe,
                            contentDescription = "Toggle AR Navigation",
                            tint = if (arNavigationViewModel.isShowingAndClosingARNavigation.value) Color(0xFF4285F4) else Color.LightGray
                        )
                    }
                }
            }

//            if (arNavigationViewModel.isShowingARNavigation.value) {
//
//                IconButton(
//                    onClick = {
//                        arNavigationViewModel.isShowingAndClosingARNavigation.value = !arNavigationViewModel.isShowingAndClosingARNavigation.value
//                    },
//                    modifier = Modifier
//                        .padding(6.dp)
//                        .align(Alignment.End)
//                        .background(color = Color.White, shape = CircleShape)
//                ) {
//                    Icon(
//                        imageVector = Icons.Rounded.Navigation,
//                        contentDescription = null,
//                        tint = Color(0xFF7E57C2)
//                    )
//                }
//            }
        }
    }

}

fun RichTooltipWithPointerShape(): GenericShape {
    return GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val pointerWidth = 20f
        val pointerHeight = 10f
        val radius = 16f

        // Start from top-left corner
        moveTo(radius, 0f)
        lineTo(width - radius, 0f)
        arcTo(
            rect = Rect(width - 2 * radius, 0f, width, 2 * radius),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        lineTo(width, height - radius - pointerHeight)
        arcTo(
            rect = Rect(width - 2 * radius, height - radius * 2 - pointerHeight, width, height - pointerHeight),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Draw pointer (triangle) on bottom-end
        lineTo(width / 2 + pointerWidth / 2, height - pointerHeight)
        lineTo(width / 2f, height)
        lineTo(width / 2 - pointerWidth / 2, height - pointerHeight)

        lineTo(radius, height - pointerHeight)
        arcTo(
            rect = Rect(0f, height - radius * 2 - pointerHeight, 2 * radius, height - pointerHeight),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        lineTo(0f, radius)
        arcTo(
            rect = Rect(0f, 0f, 2 * radius, 2 * radius),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        close()
    }
}

data class Floor(
    val id: String,
    val floorName: String
)

@Composable
fun FloorSelector(
    controller: MapxusController,
    floorList: List<Floor>,
    onSelectedFloorChanged: (String) -> Unit,
    userCurrentFloor: String?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Toggle button
        if(expanded)
            IconButton(onClick = { expanded = false }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Toggle",
                    tint = Color.Gray
                )
            }

        if (expanded) {
            // Collapsed Sidebar Mode
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp)
                    .clickable {
                        expanded = true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Up arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Up",
                    tint = Color.Gray
                )

                // Floor list
                floorList.forEach { floor ->
                    BadgedBox(
                        badge = { if(floor.id == userCurrentFloor) Badge(containerColor = Color.Red) },
                                modifier = Modifier
                                .padding(vertical = 4.dp)
                            .size(width = 48.dp, height = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (floor.id == controller.selectedFloor) Color(0xFF4285F4) else Color.Transparent
                            )
                            .clickable {
                                controller.selectedFloor = floor.id
                                onSelectedFloorChanged(floor.id)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = floor.floorName,
                                fontWeight = FontWeight.Bold,
                                color = if (floor.id == controller.selectedFloor) Color.White else Color.Gray
                            )
                        }
                    }
                }

                // Down arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Down",
                    tint = Color.Gray
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clickable {
                        expanded = true
                    }
                    .background(Color(0xFF4285F4), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = floorList.firstOrNull { it.id == controller.selectedFloor }?.floorName ?: "MF",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
