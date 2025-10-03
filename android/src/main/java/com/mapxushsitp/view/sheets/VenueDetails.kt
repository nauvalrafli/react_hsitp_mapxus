package com.mapxushsitp.view.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapxushsitp.R
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.view.component.mapxus_compose.MapxusController

data class VenueCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object VenueDetails : IScreen {
    override val routeName: String = "venueDetails"

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Screen(toiletTitle: String, navController: NavHostController, mapxusController: MapxusController, sheetState: BottomSheetScaffoldState, arNavigationViewModel: ARNavigationViewModel) {
        val context = LocalContext.current;
        val categories = listOf(
//            VenueCategory("Restaurant", R.drawable.ic_restaurant, Color(0xFFFFA726)),
//            VenueCategory("Shopping", R.drawable.ic_shopping, Color(0xFF66BB6A)),
//            VenueCategory("Health and\nMedical", R.drawable.ic_health, Color(0xFF42A5F5)),
//            VenueCategory("Restroom", Icons.Default.Wc, Color(0xFF7E57C2)), // For backup the Purple Color
            VenueCategory(context.getString(R.string.restroom), Icons.Default.Wc, Color(0xFF4285F4)),
//            VenueCategory("Arts and\nEntertainment", R.drawable.ic_arts, Color(0xFFEC407A)),
//            VenueCategory("Services", R.drawable.ic_services, Color(0xFF26A69A))
        )

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
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
//                        text = "Hong Kong-Shenzhen Innovation And Technology Park",
                        text = mapxusController.selectedVenue?.name?.getTranslation(mapxusController.locale) ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = mapxusController.selectedVenue?.address?.getTranslation(mapxusController.locale) ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        navController.navigate(VenueScreen.routeName)
                    }
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

//            OutlinedTextField(
//                value = "",
//                onValueChange = {},
//                placeholder = { Text("Search") },
//                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
//                enabled = false,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp)
//                    .clickable { navController.navigate(SearchResult.routeName) },
//                shape = CircleShape,
//                colors = TextFieldDefaults.outlinedTextFieldColors(
//                    disabledTextColor = Color.Transparent,
//                    disabledBorderColor = Color.DarkGray,
//                    disabledPlaceholderColor = Color.Gray,
//                    disabledLeadingIconColor = Color.Gray,
//                    disabledLabelColor = Color.Transparent,
//                    unfocusedBorderColor = Color(0xFF4285F4),
//                    focusedBorderColor = Color(0xFF4285F4),
//                )
//            )

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                enabled = false, // make it non-editable
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { navController.navigate(SearchResult.routeName) }, // open search page
                shape = CircleShape,
//                colors = TextFieldDefaults.outlinedTextFieldColors(
//                    disabledTextColor = Color.Transparent,
//                    disabledBorderColor = Color.DarkGray,
//                    disabledPlaceholderColor = Color.Gray,
//                    disabledLeadingIconColor = Color.Gray,
//                    disabledLabelColor = Color.Transparent,
//                    unfocusedBorderColor = Color(0xFF4285F4),
//                    focusedBorderColor = Color(0xFF4285F4),
//                )
            )

            // Categories Grid
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                categories.forEach { category ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                navController.navigate(ToiletScreen.routeName)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(category.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
//                            Image(painter = painterResource(id = category.icon), contentDescription = null, modifier = Modifier.size(40.dp))
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                tint = category.color,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            color = Color.Black,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    modifier = Modifier
//                        .padding(horizontal = 8.dp, vertical = 8.dp)
//                        .clip(RoundedCornerShape(16.dp))
//                        .clickable {
//                            navController.navigate(Navigation.SettingsView.route)
//                        }
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(60.dp)
//                            .clip(CircleShape)
//                            .background(Color(0xFF7E57C2).copy(alpha = 0.1f)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Settings,
//                            contentDescription = "Settings View",
//                            tint = Color.Black,
//                            modifier = Modifier.size(40.dp)
//                        )
//                    }
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = "Settings",
//                        fontSize = 12.sp,
//                        color = Color.Black,
//                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
//                    )
//                }
            }
        }
    }
}

