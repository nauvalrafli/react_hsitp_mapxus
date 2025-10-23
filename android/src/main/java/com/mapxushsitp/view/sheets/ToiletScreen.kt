package com.mapxushsitp.view.sheets

import android.content.Context
import android.util.Log
import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Boy
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Man
import androidx.compose.material.icons.rounded.Woman
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.static.toiletList
import com.mapxushsitp.service.DynamicStringResource
import com.mapxushsitp.view.component.mapxus_compose.MapxusController
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusPointAnnotationOptions
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class ToiletItem(
    val id: String,
    val type: String,
    val location: String,
    val status: ToiletStatus
)

enum class ToiletStatus(val color: Color, val text: String) {
    AVAILABLE(Color(0xFF4285F4), "Available"),
    ALMOST_FULL(Color(0xFFFFC107), "Almost full"),
    FULLY_ENGAGED(Color(0xFFF44336), "Fully engaged")
}

object ToiletScreen : IScreen {
    override val routeName: String = "Toilet list"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Screen(toiletTitle: String, navController: NavHostController, mapxusController: MapxusController, sheetState: BottomSheetScaffoldState, arNavigationViewModel: ARNavigationViewModel
    ) {
        var selectedFilter by remember { mutableStateOf("All") }
        val toiletType = "female_toilet" // or "male_toilet"
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.White)
                .navigationBarsPadding()
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.restroom),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Filter chips
            ScrollableTabRow(
                selectedTabIndex = when(selectedFilter) {
                    "All" -> 0
                    "accessible_toilet" -> 1
                    "female_toilet" -> 2
                    "male_toilet" -> 3
                    else -> 0
                },
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                listOf("All", "accessible_toilet", "female_toilet", "male_toilet").forEachIndexed { index, filter ->
                    val label = when(filter) {
                        "All" -> "All"
                        "accessible_toilet" -> "Accessible Toilet"
                        "female_toilet" -> "Female Toilet"
                        "male_toilet" -> "Male Toilet"
                        else -> {
                            ""
                        }
                    }
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(label) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toilet list
            val coroutine = rememberCoroutineScope()
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(toiletList.size) { index ->
                    val random = remember { mutableStateOf(Random.nextInt(3)) }
                    val toilet = toiletList[index]
                    val toiletIcon = when (toilet.type) {
                        "female_toilet" -> Icons.Rounded.Woman
                        "male_toilet" -> Icons.Rounded.Man
                        else -> Icons.Rounded.Accessible // Fallback icon
                    }

                    if (selectedFilter == "All" || selectedFilter == toilet.type) {
                        Card(
                            modifier = Modifier.align(Alignment.Start),
                            colors = CardColors(
                                containerColor = Color(0xFFF5F5F5),
                                contentColor = Color.Unspecified,
                                disabledContainerColor = Color.Unspecified,
                                disabledContentColor = Color.Unspecified
                            ),
                            onClick = {
                                coroutine.launch {
                                    mapxusController.selectedPoi = toilet
                                    delay(200)
                                    mapxusController.getMapxusMap()?.apply {
                                        var point = MapxusPointAnnotationOptions().apply {
                                            setFloorId(toilet.floorId)
                                            setPosition(LatLng(toilet.lat, toilet.lng))
                                        }
                                        if(mapxusPointAnnotations.isNotEmpty()) {
                                            removeMapxusPointAnnotations()
                                        }
                                        addMapxusPointAnnotation(point)
                                        selectFloorById(toilet.floorId)

                                        mapxusController.destinationPoint = RoutePlanningPoint(
                                            toilet.lng, toilet.lat,
                                            toilet.floorId
                                        )
                                    }
                                    delay(200)
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("poiDetails/${toilet.type}")
                                    }
                                }
                        }) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = toiletIcon,
                                    contentDescription = "Accessible",
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(40.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Toilet info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = DynamicStringResource(toilet.type),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = toilet.floorName,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1F)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getToiletStatusColor(random.value))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = getToiletStatus(random.value, context = context),
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    fun getToiletStatus(number: Int, context: Context) : String {
        return when(number) {
            1 -> context.getString(R.string.almost_full)
            2 -> context.getString(R.string.full)
            else -> context.getString(R.string.available)
        }
    }

    fun getToiletStatusColor(number: Int) : Color {
        return when(number) {
            1 -> Color.Yellow
            2 -> Color.Red
            else -> Color(0xFF4285F4)
        }
    }
}
