package com.mapxushsitp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.mapxushsitp.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import java.util.Locale


class HomeActivity : ComponentActivity(), SensorEventListener {

    var controller : MapxusController? = null
    private lateinit var compassViewModel: CompassViewModel
    private lateinit var calibrationSensorManager: CalibrationSensorManager
    private lateinit var mapxusSensorManager: SensorManager
    private var mAccelerometer : Sensor ?= null
    var locale = Locale.getDefault()

    fun updateLocale(context: Context?): Context? {
        val systemLocale = Locale.getDefault()

        val newLocale = when {
            systemLocale.language == "zh" && systemLocale.country.equals("TW", ignoreCase = true) ->
                Locale("zh", "TW")
            systemLocale.language == "zh" && systemLocale.country.equals("HK", ignoreCase = true) ->
                Locale("zh", "HK")
            systemLocale.language == "zh" && systemLocale.country.equals("CN", ignoreCase = true) ->
                Locale("zh", "CN")
            // zh but unknown region → default to zh-SG (unknown) to use generic zh as fallback
            systemLocale.language == "zh" && systemLocale.country.isNullOrEmpty() ->
                Locale("zh", "SG")
            else -> systemLocale
        }

        Locale.setDefault(newLocale)
        val config = Configuration(context?.resources?.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(newLocale))
        } else {
            config.setLocale(newLocale)
        }

        return context?.createConfigurationContext(config)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(updateLocale(newBase))
    }

    lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("Range", "UseCompatLoadingForDrawables")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        ))
        mapxusSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mapxusSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        // Initialize the ViewModel safely here
        compassViewModel = ViewModelProvider(this)[CompassViewModel::class.java]

        // Initialize SensorManager
        calibrationSensorManager = CalibrationSensorManager(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HomeScreen.composable(Locale.getDefault())
        }
    }

    override fun onResume() {
        super.onResume()
        controller?.mapView?.onResume()
        compassViewModel.start()
        mapxusSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        controller?.mapView?.onPause()
        mapxusSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        controller?.mapView?.onDestroy()
//        compassClass.stop()
        compassViewModel.stop()
//        calibrationSensorManager.stopListening()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        controller?.mapView?.onLowMemory()
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

    override fun onSensorChanged(p0: SensorEvent?) {}

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        if(p1 == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            controller?.isSensorUnreliable?.value = true
        } else {
            controller?.isSensorUnreliable?.value = false
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StepIndicators(
    totalSteps: Int,
    currentStep: Int
) {
    val maxVisibleIndicators = 6

    val startStep = when {
        totalSteps <= maxVisibleIndicators -> 0
        currentStep <= 2 -> 0
        currentStep >= totalSteps - 3 -> totalSteps - maxVisibleIndicators
        else -> currentStep - 3
    }.coerceAtLeast(0)

    val endStep = (startStep + maxVisibleIndicators).coerceAtMost(totalSteps)
    val visibleSteps = (startStep until endStep).toList()

    // Remember last start step for direction
    var previousStart by remember { mutableIntStateOf(startStep) }

    AnimatedContent(
        targetState = visibleSteps,
        transitionSpec = {
            val slideDirection = if ((targetState.firstOrNull() ?: 0) > previousStart) {
                // slide left
                slideInHorizontally { fullWidth -> fullWidth } + fadeIn() with
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
            } else {
                // slide right
                slideInHorizontally { fullWidth -> -fullWidth } + fadeIn() with
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
            }
                .using(SizeTransform(clip = false))
            slideDirection
        },
        label = "StepIndicatorAnimation"
    ) { stepsToShow ->
        previousStart = stepsToShow.firstOrNull() ?: 0

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (step in stepsToShow) {
                Canvas(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                ) {
                    drawCircle(
                        color = when {
                            step < currentStep -> Color(0xFF0a47a9)
                            step == currentStep -> Color(0xFF4285F4)
                            else -> Color.LightGray
                        },
                        radius = size.minDimension / 2
                    )
                }
            }
        }
    }
}

@Composable
fun ArriveAtDestination(
    onPreviousClick: (() -> Unit)? = null,
    onFinished: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier
            .clip(
                CircleShape
            )
            .background(Color.White)
            .padding(16.dp)
        ) {
            Image(
                modifier = Modifier.size(140.dp).clip(CircleShape),   //crops the image to circle shape
                painter = rememberDrawablePainter(
                    drawable = getDrawable(
                        LocalContext.current,
                        R.drawable.arrive_at_destination_gif
                    )
                ),
                contentDescription = "Loading animation",
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kudos!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF4285F4),
                textAlign = TextAlign.Center
            )
            Text(
                text = "You've arrived at the destination!.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF4285F4),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onPreviousClick?.invoke()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFF4285F4)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Go back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    onFinished?.invoke()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFF4285F4)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Finish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ArriveAtDestinationAlertDialog(controller: MapxusController) {
    androidx.compose.material.AlertDialog(
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Kudos!.", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("You've arrived at the destination!.", fontSize = 14.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
            }
        },
        onDismissRequest = {

        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    controller.previousStep()
                }) {
                    Text("↺ Go previous", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0a47a9))
                }

                Spacer(modifier = Modifier.width(16.dp))

                TextButton(onClick = {
                    controller.nextStep()
                }) {
                    Text("Finish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4285F4))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        contentColor = Color.Unspecified,
        properties = DialogProperties()
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun CustomDialog(setShowDialog: (Boolean) -> Unit, onPreviousClick: (() -> Unit)? = null, onFinished: (() -> Unit)? = null) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = Color.White.copy(alpha = 0.9F)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.size(100.dp).background(Color.Transparent),   //crops the image to circle shape
                            painter = rememberDrawablePainter(
                                drawable = getDrawable(
                                    LocalContext.current,
                                    R.drawable.arrive_at_destination_gif
                                )
                            ),
                            contentDescription = "Loading animation",
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Kudos!.", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Text("You have arrived at the destination!.", fontSize = 16.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Button(
//                            onClick = {
//                                onPreviousClick?.invoke()
//                                setShowDialog(false)
//                            },
//                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFF0a47a9)),
//                            shape = RoundedCornerShape(8.dp)
//                        ) {
//                            Text("↺ Go previous", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
//                        }

                        TextButton(onClick = {
                            onPreviousClick?.invoke()
                            setShowDialog(false)
                        }) {
                            Text("↺ Go previous", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0a47a9))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                onFinished?.invoke()
                                setShowDialog(false)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFF4285F4)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Finish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompassUnreliableWarning() {
    val infiniteTransition = rememberInfiniteTransition(label = "figure8")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.padding(top = 36.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color.Red,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Warning title
            Text(
                text = "Compass Needs Calibration",
                style = TextStyle(fontSize = 16.sp),
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Warning description
            Text(
                text = "Your device's compass sensor needs calibration for accurate readings.",
                style = TextStyle(fontSize = 12.sp),
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Figure-8 animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Color.LightGray.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(100.dp)
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.width * 0.3f

                    // Draw figure-8 path
                    val path = Path().apply {
                        // Left loop
                        addOval(
                            Rect(
                                offset = Offset(centerX - radius * 1.5f, centerY - radius),
                                size = Size(radius * 2, radius * 2)
                            )
                        )
                        // Right loop
                        addOval(
                            Rect(
                                offset = Offset(centerX - radius * 0.5f, centerY - radius),
                                size = Size(radius * 2, radius * 2)
                            )
                        )
                    }

                    // Draw the path
                    drawPath(
                        path = path,
                        color = Color.Gray.copy(alpha = 0.3f),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Calculate phone position along figure-8
                    val t = animationProgress * 2 * Math.PI
                    val x = centerX + radius * Math.sin(t).toFloat()
                    val y = centerY + radius * Math.sin(t).toFloat() * Math.cos(t).toFloat()

                    // Draw phone icon at animated position
                    drawRoundRect(
                        color = Color.Blue,
                        topLeft = Offset(x - 8.dp.toPx(), y - 12.dp.toPx()),
                        size = Size(16.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            Text(
                text = "To calibrate:",
                style = TextStyle(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Hold your device away from your body\n2. Move it in a figure-8 pattern as shown above\n3. Repeat the motion several times until calibrated",
                style = TextStyle(fontSize = 12.sp),
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.LightGray.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tip",
                        tint = Color.Blue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Move away from metal objects and electronic devices while calibrating",
                        style = TextStyle(fontSize = 12.sp),
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun MapxusOnboardingOverlay(
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardColors(containerColor = Color.White, contentColor = Color.White, disabledContainerColor = Color.White, disabledContentColor = Color.White)
        ) {
            Box {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(modifier = Modifier.align(Alignment.Top), onClick = {
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "\uD83C\uDF0E Welcome to Mapxus!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0057D9),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Take a look and discover what we can do for you.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swipeable Pager
                    HorizontalPager(
                        state = pagerState,
                    ) { page ->
                        when (page) {
                            0 -> PageWayfinding()
                            1 -> PageCalibrate()
                            2 -> PageBrowsing(onFinish = onFinish)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Indicator
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pagerState.pageCount) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (pagerState.currentPage == index) Color(0xFF0057D9) else Color.LightGray,
                                        CircleShape
                                    )
                            )
                            if (index < pagerState.pageCount - 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }

                // Close button
//                Text(
//                    text = "✕",
//                    fontSize = 18.sp,
//                    color = Color.Gray,
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .padding(12.dp)
//                        .clickable { onDismiss() }
//                )

            }
        }
    }
}

@Composable
private fun PageWayfinding() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.GpsFixed,
            contentDescription = "Wayfinding",
            tint = Color(0xFF4285F4),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Wayfinding", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "We help you navigate through building to reach your destination faster. \nIndoor location accuracy within 5 meters. \nOutdoor location will be using GPS.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageCalibrate() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = rememberDrawablePainter(
                getDrawable(LocalContext.current, R.drawable.figure_8_compass_calibration)
            ),
            contentDescription = "Calibration",
            modifier = Modifier.height(200.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Calibration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Make sure your device compass accurate by doing calibration as depicted on the picture. You can try to do this whenever the location is less accurate.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageBrowsing(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Browsing",
            tint = Color(0xFF4285F4),
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Browsing", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Explore the locations around you by selecting building and explore their point of interest",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
        ) {
            Text("Get started", color = Color.White)
        }
    }
}

@Composable
fun ShowWalkthrough() {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4

    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.padding(top = 36.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / totalSteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = Color.Blue
            )

            // Step indicator
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700),
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (currentStep) {
                0 -> WelcomeStep(
                    onNext = { currentStep++ }
                )
                1 -> PositioningStep(
                    onEnableLocation = {
                        // Handle location permission
                        currentStep++
                    }
                )
                2 -> NavigationOverviewStep(
                    onNext = { currentStep++ }
                )
                3 -> BrowsingStep(
                    onNext = { currentStep++ }
                )
                4 -> SummaryStep(
                    onStartExploring = {
                        // Handle start exploring - close walkthrough
                    }
                )
            }

            // Navigation buttons
            if (currentStep < 3) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- }
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = {
                            if (currentStep < totalSteps - 1) currentStep++
                        }
                    ) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.WavingHand,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.Blue
        )

        Text(
            text = "Welcome!",
            style = TextStyle(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Let's get you set up for the best navigation experience.",
            style = TextStyle(fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "We'll guide you through a few quick steps to enable location services and show you how to navigate.",
            style = TextStyle(fontSize = 16.sp),
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun PositioningStep(onEnableLocation: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.Blue
        )

        Text(
            text = "Positioning",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Enable location services for accurate positioning.",
            style = TextStyle(fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Indoor/Outdoor info cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Blue.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Blue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Indoor",
                        style = TextStyle(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "5m accuracy",
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.LightGray.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Blue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Outdoor GPS",
                        style = TextStyle(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Precise navigation",
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }

        Button(
            onClick = onEnableLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Location")
        }
    }
}

@Composable
fun NavigationOverviewStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.Blue
        )

        Text(
            text = "Navigation Overview",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Navigate easily between areas.",
            style = TextStyle(fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Use the map to explore indoor & outdoor routes.",
            style = TextStyle(fontSize = 14.sp),
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Mini map preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.LightGray.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Blue
                    )
                    Text(
                        text = "Interactive Map Preview",
                        style = TextStyle(fontSize = 12.sp),
                        color = Color.LightGray
                    )
                }
            }
        }

        Text(
            text = "💡 Tip: Tap on a destination to see the route",
            style = TextStyle(fontSize = 12.sp),
            color = Color.Blue,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Got It")
        }
    }
}

@Composable
fun BrowsingStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.Blue
        )

        Text(
            text = "Browsing the Map",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Browse places by categories or search directly.",
            style = TextStyle(fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Tap on a location to view details and directions.",
            style = TextStyle(fontSize = 14.sp),
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search bar preview
//        OutlinedTextField(
//            value = "",
//            onValueChange = { },
//            placeholder = { Text("Search locations...") },
//            leadingIcon = {
//                Icon(Icons.Default.Search, contentDescription = null)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp),
//            enabled = false
//        )

//        // Category filters preview
//        LazyRow(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.padding(bottom = 24.dp)
//        ) {
//            val list = listOf("🏪 Shops", "🍕 Food", "🚻 Restrooms", "ℹ️ Info")
//            items(list.size) { index ->
//                FilterChip(
//                    onClick = { },
//                    label = { Text(list[index]) },
//                    selected = false
//                )
//            }
//        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun SummaryStep(onStartExploring: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color(0xFF4CAF50)
        )

        Text(
            text = "Ready to Start!",
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "You're all set up! Here's what we've covered:",
            style = TextStyle(fontSize = 14.sp),
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Checklist
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            ChecklistItem("Location set (Indoor + GPS)")
            ChecklistItem("Navigation explained")
            ChecklistItem("Browsing explained")
        }

        Button(
            onClick = onStartExploring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Exploring")
        }
    }
}

@Composable
fun ChecklistItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp)
        )
    }
}
