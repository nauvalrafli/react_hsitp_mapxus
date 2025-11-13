package com.mapxushsitp.view.sheets

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Man
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Woman
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.model.MapPoi
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.view.component.mapxus_compose.MapxusController
import com.mapxus.map.mapxusmap.api.services.PoiSearch
import com.mapxus.map.mapxusmap.api.services.PoiSearch.PoiResponseListener
import com.mapxus.map.mapxusmap.api.services.model.PoiSearchOption
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiResult

object SearchResult : IScreen {
    override val routeName: String = "search_result"

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Screen(
        toiletTitle: String,
        navController: NavHostController,
        mapxusController: MapxusController,
        sheetState: BottomSheetScaffoldState,
        arNavigationViewModel: ARNavigationViewModel
    ) {
        var query by remember { mutableStateOf("") }
        val filteredResults = remember { mutableStateListOf<PoiInfo>() }
        val isInitial = remember { mutableStateOf(true) }
        val isLoading = remember { mutableStateOf(false) }
        val poiSearch = PoiSearch.newInstance()

        LaunchedEffect(true) {
            isInitial.value = false
            isLoading.value = true
            val opts = PoiSearchOption().apply {
                this.setVenueId(mapxusController.selectedVenue?.venueId)
                this.setKeywords(query)
                this.setExcludeCategories("facility.connector.stairs,facility.connector.elevator,facility.steps")
                this.pageCapacity(20)
            }
            poiSearch.searchPoiByOption(opts)
            poiSearch.searchPoiByOption(opts, object :
                PoiResponseListener {
                override fun onGetPoiResult(p0: PoiResult?) {
                    isInitial.value = false
                    filteredResults.clear()
                    filteredResults.addAll(p0?.allPoi ?: listOf())
                    isLoading.value = false
                }
            })
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.background(Color.White).fillMaxSize()
        ) {
            stickyHeader {
                Row(
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = {
                            isLoading.value = true
                            val opt = PoiSearchOption().apply {
                                this.setVenueId(mapxusController.selectedVenue?.venueId)
                                this.setKeywords(query)
                                this.setExcludeCategories("facility.connector.stairs,facility.connector.elevator,facility.steps")
                                this.pageCapacity(20)
                            }
                            poiSearch.searchPoiByOption(opt)
                            poiSearch.searchPoiByOption(opt, object :
                                PoiResponseListener {
                                override fun onGetPoiResult(p0: PoiResult?) {
                                    isInitial.value = false
                                    filteredResults.clear()
                                    filteredResults.addAll(p0?.allPoi ?: listOf())
                                    isLoading.value = false
                                }
                            })
                        }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF4285F4),
                        unfocusedIndicatorColor = Color(0xFF4285F4)
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            isLoading.value = true
                            val opts = PoiSearchOption().apply {
                                this.setVenueId(mapxusController.selectedVenue?.venueId)
                                this.setKeywords(query)
                                this.setExcludeCategories("facility.connector.stairs,facility.connector.elevator,facility.steps")
                                this.pageCapacity(20)
                            }
                            poiSearch.searchPoiByOption(opts)
                            poiSearch.searchPoiByOption(opts, object :
                                PoiResponseListener {
                                override fun onGetPoiResult(p0: PoiResult?) {
                                    isInitial.value = false
                                    filteredResults.clear()
                                    filteredResults.addAll(p0?.allPoi ?: listOf())
                                    isLoading.value = false
                                }
                            })
                        }
                    ),
                )
                Spacer(Modifier.height(12.dp))
            }
            if(isInitial.value) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text("Start searching for your destination", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    }
                }
            } else if (isLoading.value) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text("Loading . . .", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    }
                }
            } else if(filteredResults.size == 0) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text("Location not found", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
            if(filteredResults.size > 0)
                items(filteredResults.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                mapxusController.selectedPoi = MapPoi(
                                    id = filteredResults[index].poiId,
                                    lat = filteredResults[index].location.lat,
                                    lng = filteredResults[index].location.lon,
                                    type = filteredResults[index].nameMap?.getTranslation(mapxusController.locale) ?: "",
                                    floorId = filteredResults[index].floorId ?: "",
                                    floorName = filteredResults[index].floor ?: ""
                                )
                                navController.navigate(PoiDetails.routeName.plus("/${filteredResults[index].category}"))
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pick the correct icon based on toilet type
                        val toiletIcon = when (filteredResults[index].nameMap?.getTranslation(mapxusController.locale) ?: "") {
                            "Male Toilet" -> Icons.Rounded.Man
                            "Female Toilet" -> Icons.Rounded.Woman
                            "Accessible Toilet" -> Icons.Rounded.Accessible // ♿ Accessible icon
                            else -> Icons.Rounded.Place // fallback
                        }
                        Icon(toiletIcon, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(filteredResults[index].nameMap?.getTranslation(mapxusController.locale) ?: "", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(filteredResults[index].floor ?: "", fontSize = 12.sp, fontWeight = FontWeight.Light, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
        }
    }
}
