package com.mapxushsitp.view

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.arComponents.ARView
import com.mapxushsitp.compassComponents.CalibrationSensorManager
import com.mapxushsitp.compassComponents.CompassViewModel
import com.mapxushsitp.confettiComponents.ConfettiSource
import com.mapxushsitp.confettiComponents.ConfettiView
import com.mapxushsitp.motionSensor.MotionSensorViewModel
import com.mapxushsitp.ui.theme.MyApplicationTheme
import com.mapxushsitp.view.component.mapxus_compose.MapxusComposable
import com.mapxushsitp.view.component.mapxus_compose.MapxusController
import com.mapxushsitp.view.settingsComponents.SettingsView
import com.mapxushsitp.view.sheets.Navigation
import com.mapxushsitp.view.sheets.PoiDetails
import com.mapxushsitp.view.sheets.PositionMark
import com.mapxushsitp.view.sheets.PrepareNavigation
import com.mapxushsitp.view.sheets.SearchResult
import com.mapxushsitp.view.sheets.ToiletScreen
import com.mapxushsitp.view.sheets.VenueDetails
import com.mapxushsitp.view.sheets.VenueScreen
import java.util.Locale

object HomeScreen {
    fun imperativeComposable(context: Context) : ComposeView {
        return ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                composable(Locale.getDefault())
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    @Composable
    fun composable(locale: Locale) {
        MyApplicationTheme(darkTheme = false) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val arNavigationViewModel: ARNavigationViewModel = viewModel()
            val motionSensorViewModel: MotionSensorViewModel = viewModel()
            val compassViewModel: CompassViewModel = viewModel()
            val calibrationSensorManager = CalibrationSensorManager(context)
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()

            val sheetState = rememberBottomSheetScaffoldState()

            var isCalibrated by remember { mutableStateOf(false) }
            var countdown by remember { mutableStateOf<Int?>(null) }
            var progressSeconds by remember { mutableStateOf(0) }

            val isMotionSensorActiveOrNot by motionSensorViewModel.isFacingUp

            val controller = MapxusController(LocalContext.current, LocalLifecycleOwner.current, locale, navController, arNavigationViewModel)

            val notificationPermission = rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            LaunchedEffect(true) {
                if (!notificationPermission.allPermissionsGranted) {
                    notificationPermission.launchMultiplePermissionRequest()
                }
            }

            // Keep this outside of AnimatedVisibility — never recreate
            val arViewContent = remember {
                mutableStateOf<(@Composable () -> Unit)?>(null)
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                val targetHeight = if (arNavigationViewModel.isShowingAndClosingARNavigation.value) 0.5f else 0f
                val animatedHeightFraction by animateFloatAsState(
                    targetValue = targetHeight,
                    animationSpec = tween(durationMillis = 600),
                    label = "ARHeightFraction"
                )
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val animatedHeight = screenHeight * animatedHeightFraction
                val arHeight = if (arNavigationViewModel.isShowingAndClosingARNavigation.value) animatedHeight else 0.dp
                val isShowing = arNavigationViewModel.isShowingAndClosingARNavigation.value

                LaunchedEffect(true) {
                    sheetState.bottomSheetState.expand()
                    navController.navigate(VenueScreen.routeName)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomSheetScaffold(
                        scaffoldState = sheetState,
                        sheetContainerColor = Color.White,
                        sheetPeekHeight = if (controller?.showSheet?.value != false) 300.dp else 0.dp,
                        sheetContent = {
                            Surface() {
                                if(controller?.showSheet?.value != false) {
                                    NavHost(
                                        navController,
                                        startDestination = VenueScreen.routeName
                                    ) {
                                        composable(VenueScreen.routeName) {
                                            VenueScreen.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(VenueDetails.routeName) {
                                            VenueDetails.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(ToiletScreen.routeName) {
                                            ToiletScreen.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(PoiDetails.routeName.plus("/{type}")) { backStackEntry ->
                                            val type = backStackEntry.arguments?.getString("type")
                                            PoiDetails.Screen(toiletTitle = type ?: "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(PrepareNavigation.routeName) {
                                            PrepareNavigation.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(PositionMark.routeName) {
                                            PositionMark.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(PrepareNavigation.routeName) {
                                            PrepareNavigation.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                        composable(route = Navigation.SettingsView.route) {
                                            SettingsView(arNavigationViewModel, motionSensorViewModel)
                                        }
                                        composable(route = SearchResult.routeName) {
                                            SearchResult.Screen(toiletTitle = "", navController, mapxusController = controller!!, sheetState, arNavigationViewModel)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {

                            Box(modifier = Modifier.height(animatedHeight).fillMaxWidth()) {
                                controller?.let { ctrl ->
                                    val start = ctrl.startingPoint
                                    val end = ctrl.destinationPoint
                                    if (start != null && end != null) {
                                        ARView(
                                            modifier = Modifier.fillMaxSize(),
                                            arModifier = Modifier.fillMaxSize(),
                                            instructionPoints = ctrl.arInstructionPoints,
                                            instructionList = ctrl.arInstructionNavigationList,
                                            yourLocation = start,
                                            destination = end,
                                            instructionIndex = ctrl.instructionIndex.value,
                                            arNavigationViewModel = arNavigationViewModel,
                                            motionSensorViewModel = motionSensorViewModel,
                                            compassViewModel = compassViewModel,
                                            isActivatingAR = arNavigationViewModel.isActivatingAR.value
                                        )
                                    }
                                }
//                                    }
                            }

                            controller?.let { it1 -> MapxusComposable(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(1F),
                                controller = it1,
                                arNavigationViewModel,
                                true
                            ) }
                        }
                    }

                    if (controller?.showSheet?.value == false) {
                        val currentStep = controller?.instructionIndex?.value ?: 0
                        val totalSteps = controller?.instructionList?.size ?: 1
                        if (currentStep == totalSteps - 1 || ((controller?.timeEstimation?.value?.split(" ")?.firstOrNull() ?: "10").ifEmpty { "10" }.toIntOrNull() ?: 5) <= 4) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ConfettiView(source = ConfettiSource.TOP)
                                CustomDialog(
                                    setShowDialog = { it ->
                                        it
                                    },
                                    onPreviousClick = { controller?.previousStep() },
                                    onFinished = { controller?.nextStep() }
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                SwipeableRouteCard(
                                    arNavigationViewModel = arNavigationViewModel,
                                    title = controller?.titleNavigationStep?.value ?: "",
                                    subtitle = controller?.distanceNavigationStep?.value ?: "",
                                    currentStep = controller?.instructionIndex?.value ?: 0,
                                    totalSteps = controller?.instructionList?.size ?: 1,
                                    timeEstimation = controller?.timeEstimation?.value ?: "",
                                    icon = controller?.icon?.value,
                                    onPreviousClick = { controller?.previousStep() },
                                    onNextClick = {
//                                        navController.navigate("venueDetails")
                                        controller?.nextStep()
                                    },
                                    onFinished = {

                                    }
                                )
                            }
                        }
                    }

                }

//                    ArriveAtTheDestination(currentStep = controller?.instructionIndex?.value ?: 0, totalSteps = controller?.instructionList?.size ?: 1, arNavigationViewModel)
            }

            BackHandler() {
                if(controller?.showSheet?.value == false) {
                    controller?.routePainter?.cleanRoute()
                    controller?.showSheet?.value = true
                } else if (navController.currentDestination?.route == VenueScreen.routeName) {
//                    finish()
                }
            }

            if(controller?.isSensorUnreliable?.value == true) CompassUnreliableWarning()

//                ShowWalkthrough()
            if(controller?.isFirst?.value == true) MapxusOnboardingOverlay(onFinish = {
                controller?.sharedPreferences?.edit()?.apply {
                    putBoolean("isFirst", false)
                    apply()
                }
                controller?.isFirst?.value = false
            }, onDismiss = {
                controller?.sharedPreferences?.edit()?.apply {
                    putBoolean("isFirst", false)
                    apply()
                }
                controller?.isFirst?.value = false
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun SwipeableRouteCard(
        arNavigationViewModel: ARNavigationViewModel,
        title: String,
        subtitle: String,
        currentStep: Int,
        totalSteps: Int,
        icon: ImageVector?,
        timeEstimation: String,
        onPreviousClick: () -> Unit,
        onNextClick: () -> Unit,
        onFinished: () -> Unit
    ) {
        val maxVisibleIndicators = 6

        // Calculate start index of visible indicators
        val startStep = when {
            totalSteps <= maxVisibleIndicators -> 0
            currentStep <= 2 -> 0
            currentStep >= totalSteps - 3 -> totalSteps - maxVisibleIndicators
            else -> currentStep - 3
        }

        // Clamp visible indicators
        val visibleSteps = if (totalSteps <= maxVisibleIndicators) {
            0 until totalSteps
        } else {
            startStep until (startStep + maxVisibleIndicators)
        }

        Card(
            colors = CardColors(
                containerColor = Color.White,
                contentColor = Color.Unspecified,
                disabledContainerColor = Color.Unspecified,
                disabledContentColor = Color.Unspecified
            ),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier = Modifier
//                .graphicsLayer(renderEffect = RenderEffect.createBlurEffect(25f,25f, Shader.TileMode.MIRROR).asComposeRenderEffect())
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onPreviousClick()
                        },
                        enabled = currentStep > 0
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = if (currentStep > 0) Color(0xFF4285F4) else Color.Gray
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = subtitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = timeEstimation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                    }

                    if(icon != null) {
                        Icon(imageVector = icon, null, Modifier.size(48.dp))
                    }

                    IconButton(
                        onClick = {
                            onNextClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = Color(0xFF4285F4)
                        )
                    }
                }

                StepIndicators(totalSteps, currentStep)
            }
        }
    }
}
