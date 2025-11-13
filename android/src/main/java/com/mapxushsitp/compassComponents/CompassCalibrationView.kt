package com.mapxushsitp.compassComponents

import android.util.Log
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

//@Composable
//fun CompassCalibrationScreen(
//    compassViewModel: CompassViewModel,
//    onCalibrated: () -> Unit
//) {
//    val azimuth by compassViewModel.azimuth
//    var lastUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
//    var isCalibrated by remember { mutableStateOf(false) }
//
//    // Track progress seconds (e.g., 0 to 3 seconds)
//    var progressSeconds by remember { mutableIntStateOf(0) }
//
//    // Start compass when this screen appears
//    LaunchedEffect(Unit) {
//        compassViewModel.start()
//    }
//
//    // Check if azimuth has valid value
//    LaunchedEffect(azimuth) {
//        val now = System.currentTimeMillis()
//        if (azimuth != 0f && now - lastUpdateTime > 1000 && !isCalibrated) {
//            progressSeconds += 1
//            lastUpdateTime = now
//
//            if (progressSeconds >= 3) {
//                isCalibrated = true
//                compassViewModel.stop()
//                onCalibrated()
//            }
//        }
//    }
//
//    // UI: Animated rotating phone
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.White),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Example rotating phone animation
//        RotatingPhoneAnimation()
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Text(
//            text = "Please rotate your phone horizontally in all directions",
//            fontSize = 16.sp,
//            textAlign = TextAlign.Center,
//            fontWeight = FontWeight.Normal,
//            modifier = Modifier.padding(horizontal = 32.dp)
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Show progress below instruction
//        if (!isCalibrated) {
//            Text(
//                text = "Calibrating... ${3 - progressSeconds}s",
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Light
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            LinearProgressIndicator(
//                progress = progressSeconds / 3f,
//                modifier = Modifier
//                    .width(150.dp)
//                    .height(6.dp)
//            )
//        }
//
////        Button(onClick = {
////            // maybe after checking some sensor thresholds
////            onCalibrated()
////        }) {
////            Text("Done Calibrating")
////        }
//
//    }
//}

@Composable
fun CompassCalibrationScreen(
    compassViewModel: CompassViewModel,
    onCalibrated: () -> Unit
) {
    val azimuth = compassViewModel.azimuth
    var isCalibrated by remember { mutableStateOf(false) }

    // Track progress seconds (e.g., 0 to 3 seconds)
    var progressSeconds by remember { mutableIntStateOf(0) }

    // Start compass when this screen appears
    LaunchedEffect(Unit) {
        compassViewModel.start()
    }

    // Start a timer to track stable calibration time
    LaunchedEffect(azimuth) {
        if (azimuth.value != 0f && !isCalibrated) {
            while (progressSeconds < 3) {
                delay(1000)
                progressSeconds += 1
            }
            // Once stable for 3 seconds, mark as calibrated
            isCalibrated = true
            compassViewModel.stop()
            onCalibrated()
            Log.e("CalibratingProgress", "${progressSeconds}")
        } else {
            // Reset progress if azimuth is zero again
            progressSeconds = 0
            Log.e("CalibratingProgress", "${progressSeconds}")
        }
    }

    // UI: Animated rotating phone
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RotatingPhoneAnimation()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Please rotate your phone horizontally in all directions",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show progress below instruction
        if (!isCalibrated) {
            Text(
                text = "Calibrating... ${3 - progressSeconds}s",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progressSeconds / 3f,
                modifier = Modifier
                    .width(150.dp)
                    .height(6.dp)
            )
        }

//        Button(onClick = {
//            // maybe after checking some sensor thresholds
//            onCalibrated()
//        }) {
//            Text("Done Calibrating")
//        }
    }
}

@Composable
fun CompassCalibrationCountdown(
    isCalibrated: Boolean,
    isShowingCountdownScreen: Boolean,
    onCalibrationDone: () -> Unit
) {
    var countdown by remember { mutableStateOf(3) }
    var progressSeconds by remember { mutableStateOf(0) }

    // Start countdown only when screen is shown AND not calibrated
    LaunchedEffect(isShowingCountdownScreen, isCalibrated) {
        if (isShowingCountdownScreen && !isCalibrated) {
            for (i in 3 downTo 1) {
                countdown = i
                progressSeconds = 3 - i
                delay(1000)
            }
            progressSeconds = 3
            countdown = 0
            onCalibrationDone()
        }
    }

    if (isShowingCountdownScreen && !isCalibrated) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RotatingPhoneAnimation()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Please rotate your phone horizontally in all directions",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Calibrating... ${countdown}s",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progressSeconds / 3f,
                modifier = Modifier
                    .width(150.dp)
                    .height(6.dp)
            )
        }
    }
}

@Composable
fun RotatingPhoneAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Icon(
        imageVector = Icons.Rounded.ScreenRotation,
        contentDescription = "Rotating phone",
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                rotationY = rotation
            },
        tint = Color.Gray
    )
}

@Composable
fun SmallRotatingPhoneAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Icon(
        imageVector = Icons.Rounded.ScreenRotation,
        contentDescription = "Rotating phone",
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                rotationY = rotation
            },
        tint = Color.White
    )
}

@Composable
fun CalibrateScreen(
    calibrationSensorManager: CalibrationSensorManager,
    isShowingCalibratingScreen: Boolean,
    onCalibrated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }

//    LaunchedEffect(isShowingCalibratingScreen) {
//        calibrationSensorManager.setProgressListener { newProgress ->
//            progress = newProgress
//            if (progress >= 1f) {
//                onCalibrated()
//            }
//            Log.e("CalibratingProgress", "$newProgress")
//        }
//
//        // 🔥 Start the calibration process!
//        calibrationSensorManager.startListening()
//    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        RotatingPhoneAnimation()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Please rotate your phone horizontally and vertically in all directions",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calibrating sensors...",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF4285F4),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LinearProgressIndicator(
            progress = progress,
            color = Color(0xFF4285F4),
            modifier = Modifier
                .width(200.dp)
                .height(8.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun SecondCalibrateScreen(
    calibrationSensorManager: CalibrationSensorManager,
    isShowingCalibratingScreen: Boolean,
    onCalibrated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }
    var neededAxes by remember { mutableStateOf(listOf<String>()) }

    val directionHint = if (neededAxes.contains("x") || neededAxes.contains("z")) {
        "horizontally"
    } else {
        ""
    }

    LaunchedEffect(isShowingCalibratingScreen) {
        calibrationSensorManager.setProgressListener { newProgress, lackingAxes ->
            progress = newProgress
            neededAxes = lackingAxes
            if (progress >= 0.80f) {
                onCalibrated()
            }
            Log.e("CalibratingProgress", "Progress: $newProgress, Needed: $lackingAxes")
        }

        calibrationSensorManager.startListening()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        RotatingPhoneAnimation()

        Spacer(modifier = Modifier.height(24.dp))

//        Text(
//            text = "Please rotate your phone horizontally and vertically in all directions",
//            fontSize = 16.sp,
//            textAlign = TextAlign.Center,
//            fontWeight = FontWeight.SemiBold,
//            modifier = Modifier.padding(horizontal = 32.dp)
//        )

        Text(
            text = "Please rotate your phone left, right, and spin it flat like a compass (${directionHint}).",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calibrating sensors...",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF4285F4),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LinearProgressIndicator(
            progress = progress.coerceAtMost(0.80F),
            color = Color(0xFF4285F4),
            modifier = Modifier
                .width(200.dp)
                .height(8.dp)
                .clip(CircleShape)
        )

        // Show missing directions if any
        // Full orientation sensor (horizontal & vertical)
//        if (neededAxes.isNotEmpty()) {
//            val directionHint = when {
//                neededAxes.contains("x") || neededAxes.contains("z") -> "horizontally"
//                neededAxes.contains("y") -> "vertically"
//                else -> ""
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//            Text(
//                text = "Almost there! Try rotating a bit more around: $directionHint",
//                fontSize = 10.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Red,
//                modifier = Modifier.padding(horizontal = 16.dp)
//            )
//        }

    }
}
