package com.mapxushsitp.view.sheets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxus.map.mapxusmap.api.services.constant.RoutePlanningVehicle
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.model.MapPoi
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.mapxushsitp.gpsComponents.GPSHelper
import com.mapxushsitp.service.DynamicStringResource
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.view.component.mapxus_compose.MapxusController

data class DropdownItem(
    val displayText: String,
    val secondaryText: String = "",
    val poi: MapPoi? = null
)

enum class RouteType {
    SHORTEST_WALK,
    LIFT_ONLY,
    ESCALATOR_ONLY
}

object PrepareNavigation : IScreen {
    override val routeName: String = "prepareNavigation"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Screen(
        toiletTitle: String,
        navController: NavHostController,
        mapxusController: MapxusController,
        sheetState: BottomSheetScaffoldState,
        arNavigationViewModel: ARNavigationViewModel
    ) {
        val gpsHelper = GPSHelper(LocalContext.current)
        var selectedRouteType by remember { mutableStateOf(RouteType.SHORTEST_WALK) }
        var selectedStartPoint by remember { mutableStateOf("") }
        var searchQuery by remember { mutableStateOf("") }
        var isExpanded by remember { mutableStateOf(false) }
        val destinationPoi = mapxusController.selectedPoi

        LaunchedEffect(true) {
            if(mapxusController.startingPoint != null) {
                val text = "${mapxusController.startingPoint!!.lat}, ${mapxusController.startingPoint!!.lon}"
                selectedStartPoint = text
            }
            mapxusController.selectingCenter.value = false
        }

        // Create dropdown items list
        val dropdownItems = remember {
            listOf(
                DropdownItem("Current Location"),
                DropdownItem("Select Location from Map")
            )
//            + toiletList.map { poi ->
//                DropdownItem(
//                    displayText = poi.type,
//                    secondaryText = "Floor ${poi.floorName}, K11 MUSEA",
//                    poi = poi
//                )
//            }
        }

        // Filter items based on search query
        val suggestions = remember(searchQuery) {
            dropdownItems.filter { item ->
                val searchText = "${item.displayText} ${item.secondaryText}"
                searchText.contains(searchQuery, ignoreCase = true)
            }
        }

        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    stringResource(R.string.navigation),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start and Destination Points
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Start Point Dropdown
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = {
                        isExpanded = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    val containerColor = Color(0xFFF5F5F5)

//                    OutlinedTextField(
//                        value = selectedStartPoint,
//                        onValueChange = { selectedStartPoint = it },
//                        readOnly = true,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .menuAnchor(),
//                        placeholder = { Text(stringResource(R.string.select_start_point)) },
//                        leadingIcon = {
//                            Icon(
//                                Icons.Default.LocationOn,
//                                contentDescription = "Start",
//                                tint = Color(0xFF4285F4)
//                            )
//                        },
//                        trailingIcon = {
//                            Icon(
//                                Icons.Default.KeyboardArrowDown,
//                                contentDescription = "Expand"
//                            )
//                        },
//                        containerColor = Color(0xFFF5F5F5),
//                        unfocusedBorderColor = Color.Transparent,
//                        colors = OutlinedTextFieldDefaults.colors(
//                                focusedContainerColor = Color(0xFFF5F5F5),
//                                unfocusedContainerColor = Color(0xFFF5F5F5),
//                                disabledContainerColor = Color(0xFFF5F5F5),
//                                errorContainerColor = Color.Red,
//                                unfocusedBorderColor = Color.Transparent
//                            ),
//                        shape = RoundedCornerShape(8.dp)
//                    )

                    OutlinedTextField(
                        value = selectedStartPoint,
                        readOnly = true,
                        onValueChange = { selectedStartPoint = it },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text(stringResource(R.string.select_start_point)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Start",
                                tint = Color(0xFF4285F4)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
//                        colors = TextFieldDefaults.outlinedTextFieldColors(
//                            focusedContainerColor = Color(0xFFF5F5F5),
//                            unfocusedContainerColor = Color(0xFFF5F5F5),
//                            disabledContainerColor = Color(0xFFF5F5F5),
//                            errorContainerColor = Color.Red,
//                            unfocusedBorderColor = Color.Transparent
//                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = {
                            isExpanded = false
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text= {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.current_location),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                arNavigationViewModel.resetNavigationState()
                                mapxusController.getMapxusMap().setLocationEnabled(true)
                                selectedStartPoint = context.resources.getString(R.string.current_location)

                                mapxusController.isCurrentLocation.value = true
                                isExpanded = false

                                arNavigationViewModel.isSelectingGPSCurrentLocation.value = true

//                                mapxusController.destinationPoint?.let { dest ->
//                                    val lat = dest.lat
//                                    val lon = dest.lon
//                                    if (lat != null && lon != null) {
//                                        mapxusController.setStartWithCenter(lat, lon)
//                                    }
//                                }

                            }
                        )
                        DropdownMenuItem(
                            text= {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.select_location_on_map),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                mapxusController.selectingCenter.value = true
                                mapxusController.isCurrentLocation.value = false
                                navController.navigate(PositionMark.routeName)
                            }
                        )
                    }
                }

                // Destination Field (Non-editable)
                val containerColor1 = Color(0xFFF5F5F5)
                OutlinedTextField(
                    value = "${DynamicStringResource(destinationPoi?.type ?: "")}, ${destinationPoi?.floorName ?: ""}, ${mapxusController.selectedVenue?.venueName?.getTranslation(mapxusController.locale) ?: ""}",
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Destination",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
//                OutlinedTextField(
//                    value = "${DynamicStringResource(destinationPoi?.type ?: "")}, ${destinationPoi?.floorName ?: ""}, ${mapxusController.selectedVenue?.venueName?.getTranslation(mapxusController.locale) ?: ""}",
//                    onValueChange = { },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = false,
//                    leadingIcon = {
//                        Icon(
//                            Icons.Default.LocationOn,
//                            contentDescription = "Destination",
//                            tint = Color(0xFF4285F4)
//                        )
//                    },
//                    unfocusedBorderColor = Color.Transparent,
//                    disabledTextColor = Color.Black,
//                    disabledLeadingIconColor = Color(0xFF4285F4),
////                    colors = OutlinedTextFieldDefaults.colors(
////                            unfocusedTextColor = Color.Transparent,
////                            disabledTextColor = Color.Black,
////                            focusedContainerColor = Color(0xFFF5F5F5),
////                            unfocusedContainerColor = Color(0xFFF5F5F5),
////                            disabledContainerColor = Color(0xFFF5F5F5),
////                            errorContainerColor = Color.Red,
////                            unfocusedBorderColor = Color.Transparent,
////                            disabledLeadingIconColor = Color(0xFF4285F4),
////                        ),
//                    shape = RoundedCornerShape(8.dp)
//                )
            }

            // Route type selection and Go button (shown when start point is selected)
            if (selectedStartPoint.isNotEmpty()) {
                Text(
                    stringResource(R.string.route_type),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RouteType.values().forEach { routeType ->
                        FilterChip(
                            selected = selectedRouteType == routeType,
                            onClick = {
                                selectedRouteType = routeType
                                when(routeType) {
                                    RouteType.SHORTEST_WALK -> mapxusController.selectedVehicle = RoutePlanningVehicle.FOOT
                                    RouteType.LIFT_ONLY -> mapxusController.selectedVehicle = RoutePlanningVehicle.WHEELCHAIR
                                    RouteType.ESCALATOR_ONLY -> mapxusController.selectedVehicle = RoutePlanningVehicle.ESCALATOR
                                }
                            },
                            label = {
                                Text(
                                    when (routeType) {
                                        RouteType.SHORTEST_WALK -> stringResource(R.string.shortest_walk)
                                        RouteType.LIFT_ONLY -> stringResource(R.string.lift_only)
                                        RouteType.ESCALATOR_ONLY -> stringResource(R.string.escalator_only)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        )
                    }
                }

//                AccuracyTextField(mapxusController.accuracyAdsorptionInput.value, {
//                    mapxusController.accuracyAdsorptionInput.value = it
//                }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if(!mapxusController.isLoading.value) {
                            mapxusController.showRoute()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(mapxusController.isLoading.value) Color.DarkGray else Color(0xFF4285F4))
                ) {
                    Text(
                        stringResource(R.string.show_route),
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (!mapxusController.isLoading.value) {
                            if (arNavigationViewModel.isSelectingGPSCurrentLocation.value) {
                                gpsHelper.getLastKnownLocation { lat, lon ->
                                    if (lat != null && lon != null) {
                                        mapxusController.startingPoint = RoutePlanningPoint(lon ?: 0.0, lat ?: 0.0, "")

                                        val calculateHeading = mapxusController.startingPoint?.let {
                                            mapxusController.calculateHeading(it.lat, it.lon, mapxusController.destinationPoint?.lat ?: 0.0, mapxusController.destinationPoint?.lon ?: 0.0)
                                        }

                                        mapxusController.arStartPoint = SerializableRoutePoint(lat, lon, heading = calculateHeading ?: 0.0,"")
                                        Log.d("UserLocation", "Latitude: $lat, Longitude: $lon")
                                        Log.w("ARCoreHeading", "starting point: ${mapxusController.startingPoint}")
                                    } else {
                                        Log.d("UserLocation", "Unable to fetch location")
                                    }
                                }
                            }

                            mapxusController.drawRoute()
                            mapxusController.showSheet.value = false
                            arNavigationViewModel.isShowingOpeningAndClosingARButton.value = true
                            arNavigationViewModel.isActivatingAR.value = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mapxusController.isLoading.value) Color.DarkGray else Color(0xFF4285F4))
                ) {
                    Text(
                        stringResource(R.string.start_navigation),
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    @Composable
    fun AccuracyTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val minValue = 0
        val maxValue = 10
        val increment = 1

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minus button
            IconButton(
                onClick = {
                    val currentValue = value.toIntOrNull() ?: minValue
                    val newValue = (currentValue - increment).coerceAtLeast(minValue)
                    onValueChange(newValue.toString())
                },
                enabled = (value.toIntOrNull() ?: minValue) > minValue
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease"
                )
            }

            // TextField
//            OutlinedTextField(
//                value = value,
//                onValueChange = { newValue ->
//                    // Allow empty string or valid integers only
//                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
//                        onValueChange(newValue)
//                    }
//                },
//                label = { Text(stringResource(R.string.accuracy)) },
//                placeholder = { Text(stringResource(R.string.accuracy)) },
//                suffix = { Text(stringResource(R.string.meter)) },
//                keyboardOptions = KeyboardOptions(
//                    keyboardType = KeyboardType.Number,
//                    imeAction = ImeAction.Done
//                ),
//                keyboardActions = KeyboardActions(
//                    onDone = {
//                        // Auto-correct to range when user finishes editing
//                        val intValue = value.toIntOrNull()
//                        if (intValue != null) {
//                            val correctedValue = intValue.coerceIn(minValue, maxValue)
//                            onValueChange(correctedValue.toString())
//                        } else if (value.isNotEmpty()) {
//                            // If invalid input, set to minimum
//                            onValueChange(minValue.toString())
//                        }
//                    }
//                ),
//                singleLine = true,
//                modifier = Modifier
//                    .weight(1f)
//                    .onFocusChanged { focusState ->
//                        // Auto-correct when field loses focus
//                        if (!focusState.isFocused && value.isNotEmpty()) {
//                            val intValue = value.toIntOrNull()
//                            if (intValue != null) {
//                                val correctedValue = intValue.coerceIn(minValue, maxValue)
//                                onValueChange(correctedValue.toString())
//                            } else {
//                                // If invalid input, set to minimum
//                                onValueChange(minValue.toString())
//                            }
//                        }
//                    }
//            )

            // Plus button
            IconButton(
                onClick = {
                    val currentValue = value.toIntOrNull() ?: minValue
                    val newValue = (currentValue + increment).coerceAtMost(maxValue)
                    onValueChange(newValue.toString())
                },
                enabled = (value.toIntOrNull() ?: minValue) < maxValue
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase"
                )
            }
        }
    }
}
