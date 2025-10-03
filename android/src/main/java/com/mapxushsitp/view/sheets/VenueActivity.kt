package com.mapxushsitp.view.sheets

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.data.static.venues
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.view.component.mapxus_compose.MapxusController

data class VenueItem(
    val name: String,
    val address: String,
    val imageResId: Int
)

object VenueScreen : IScreen {
    override val routeName: String = "venue"

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Screen(toiletTitle: String, navController: NavHostController, mapxusController: MapxusController, sheetState: BottomSheetScaffoldState, arNavigationViewModel: ARNavigationViewModel) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_venue_location),
                        contentDescription = "Location",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.explore_by_venue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carousel
            val pagerState = rememberPagerState(pageCount = { venues.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { page ->
                val venue = venues[page]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable {
                            mapxusController.mapViewProvider.getMapxusMapAsync {
                                it.selectVenueById(venue.venueId)
                                mapxusController.isFloorSelectorShown.value = true
                            }
                            mapxusController.selectedVenue = venue
                            navController.navigate("venueDetails")
                        },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Venue Information
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = venue.name.getTranslation(mapxusController.locale) ?: "",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = venue.address.getTranslation(mapxusController.locale),
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Pagination Indicators
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(venues.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        Color(0xFF4285F4)
                    } else {
                        Color.LightGray
                    }
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                    )
                }
            }
        }
    }
}
