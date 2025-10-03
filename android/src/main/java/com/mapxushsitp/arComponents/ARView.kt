package com.mapxushsitp.arComponents

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.getDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapxushsitp.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.maps.android.SphericalUtil
import com.mapxus.map.mapxusmap.api.services.model.planning.RoutePlanningPoint
import com.mapxushsitp.compassComponents.CompassViewModel
import com.mapxushsitp.data.model.SerializableNavigationInstruction
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.mapxushsitp.motionSensor.MotionSensorViewModel
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.ARSession
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.TreeSet
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.android.gms.maps.model.LatLng as GmsLatLng

@Composable
fun ARFragmentView(
    index: Int,
    startPoint: SerializableRoutePoint,
    endPoint: SerializableRoutePoint,
    instructionPointList: List<SerializableRoutePoint>,
    instructionNavigationList: List<SerializableNavigationInstruction>
) {
    val context = LocalContext.current
    val fragmentManager = (context as? AppCompatActivity)?.supportFragmentManager

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val container = FrameLayout(ctx).apply { id = View.generateViewId() }

            fragmentManager?.let { fm ->
                val tag = "FourthLocalARFragmentTag"
                val existingFragment = fm.findFragmentByTag(tag)
                if (existingFragment == null) {
                    val fragment =
                        FourthLocalARFragment() // ✅ Must extend androidx.fragment.app.Fragment
                    val args = Bundle().apply {
                        putSerializable("yourLocation", startPoint)
                        putSerializable("destination", endPoint)
                        putInt("instructionIndex", index)
                        putInt("secondInstructionIndex", 0)
                        putSerializable("instructionList", ArrayList(instructionNavigationList))
                        putSerializable("instructionPoints", ArrayList(instructionPointList))
                        putParcelableArrayList("secondInstructionPoints", ArrayList(emptyList()))
                    }

                    fragment.arguments = args

                    fm.beginTransaction()
                        .replace(container.id, fragment, tag)
                        .commitAllowingStateLoss()

                }
            }

            container
        }
    )
}

//@Composable
//fun SecondARView(
//    modifier: Modifier,
//    arModifier: Modifier,
//    instructionPoints: List<SerializableRoutePoint>,
//    instructionList: List<SerializableNavigationInstruction>,
//    yourLocation: RoutePlanningPoint,
//    destination: RoutePlanningPoint,
//    instructionIndex: Int,
//    arNavigationViewModel: ARNavigationViewModel,
//    motionSensorViewModel: MotionSensorViewModel
//) {
//    // ✅ Only initialize the AR setup once — not on every recomposition
//    val arViewInitialized = remember { mutableStateOf(false) }
//
//    AndroidView(
//        modifier = modifier,
//        factory = { context ->
//            // Create your ARSceneView (SceneView from Sceneform or SceneView ARCore)
//            ARSceneView(context)
//        },
//        update = { arSceneView ->
//            if (!arViewInitialized.value) {
//                arViewInitialized.value = true
//
//                arSceneView.post {
//                    arSceneView.clearScene() // Optional: if needed to reset before placing new anchors
//
//                    // ✅ Place arrows at instruction points
//                    arSceneView.placeArrowsUsingWorldAnchor(
//                        instructionPoints = instructionPoints,
//                        yourLocation = yourLocation,
//                        instructionList = instructionList
//                    )
//
//                    // ✅ Draw the road path
//                    arSceneView.placeRoadPath(
//                        instructionPoints = instructionPoints
//                    )
//                }
//            }
//        }
//    )
//
//    // Optional: Reset flag when ARView closes
////    LaunchedEffect(arNavigationViewModel.isShowingAndClosingARNavigation.value) {
////        if (!arNavigationViewModel.isShowingAndClosingARNavigation.value) {
////            arViewInitialized.value = false
////        }
////    }
//}


@Composable
fun ARView(
    modifier: Modifier,
    arModifier: Modifier,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    destination: RoutePlanningPoint,
    instructionIndex: Int,
    arNavigationViewModel: ARNavigationViewModel,
    motionSensorViewModel: MotionSensorViewModel,
    compassViewModel: CompassViewModel,
    isActivatingAR: Boolean
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = remember { ModelLoader(engine, context) }
    val materialLoader = rememberMaterialLoader(engine)

    val motionSensorClass: MotionSensorViewModel = viewModel()

    val trackingMessage = remember { mutableStateOf("Initializing AR...") }
    var isShowingTrackingMessage by remember { mutableStateOf(false) }
    val arrowNodes = remember { mutableStateListOf<io.github.sceneview.node.Node>() }
    val roadCircleNodes = remember { mutableStateListOf<io.github.sceneview.node.Node>() }
    val sceneViewRef = remember { mutableStateOf<ARSceneView?>(null) }

    var pendingArrowIndex by remember { mutableStateOf<Int?>(null) }
    var lastTrackingState by remember { mutableStateOf<TrackingState?>(null) }
    var lastTrackingStateState = remember { mutableStateOf<TrackingState?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val systemUiController = rememberSystemUiController()
    systemUiController.isStatusBarVisible = false
    systemUiController.setStatusBarColor(Color.Transparent)

    var arViewInitialized = remember { mutableStateOf(false) }

    var isARActive by remember { mutableStateOf(arNavigationViewModel.isShowingAndClosingARNavigation.value) }
    var isShowingARNavigationButton by remember { mutableStateOf(arNavigationViewModel.isShowingOpeningAndClosingARButton.value) }
    val cameraMaterialStream = rememberARCameraStream(materialLoader)
    val cameraStreamEnabled = arNavigationViewModel.isShowingAndClosingARNavigation.value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isARActive = true // Resume AR when the app is in the foreground
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isARActive = false // Pause AR when the app goes to the background
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(trackingMessage) {
        while (isActive) {
            logCameraTrackingStatus(
                sceneView = sceneViewRef.value,
                trackingMessage = trackingMessage,
                coroutineScope = coroutineScope,
                lastTrackingState = lastTrackingStateState
            )
            delay(500) // Check tracking status periodically
        }
    }

    LaunchedEffect(instructionIndex) {
        val sceneView = sceneViewRef.value ?: return@LaunchedEffect
        var currentPendingIndex: Int? = instructionIndex
        var currentLastTrackingState: TrackingState? = null

        while (currentPendingIndex != null && isActive) {
            val trackingState = sceneView.frame?.camera?.trackingState

            if (currentLastTrackingState != trackingState) {
                Log.d("ARCoreDebug", "📷 Tracking state changed: $trackingState")
            }

            if (trackingState == TrackingState.TRACKING) {
                Log.d("ARCoreDebug", "✅ Tracking — showing arrow index $currentPendingIndex")

//                isShowingARNavigationArrowOneByOne(
//                    index = currentPendingIndex,
//                    sceneView = sceneView,
//                    instructionPoints = instructionPoints,
//                    instructionList = instructionList,
//                    yourLocation = yourLocation,
//                    yourDestination = destination,
//                    modelLoader = modelLoader,
//                    arrowNodes = arrowNodes,
//                    roadCircleNodes = roadCircleNodes,
//                    context = context,
//                    coroutineScope = coroutineScope,
//                    compassViewModel = compassViewModel,
//                    engine = engine,
//                    materialLoader = materialLoader
//                )

//                secondIsShowingARNavigationArrowOneByOne(
//                    index = currentPendingIndex,
//                    sceneView = sceneView,
//                    instructionPoints = instructionPoints,
//                    instructionList = instructionList,
//                    yourLocation = yourLocation,
//                    yourDestination = destination,
//                    modelLoader = modelLoader,
//                    arrowNodes = arrowNodes,
//                    roadCircleNodes = roadCircleNodes,
//                    context = context,
//                    coroutineScope = coroutineScope,
//                    compassViewModel = compassViewModel
//                )

//                thirdIsShowingARNavigationArrowOneByOne(
//                    index = currentPendingIndex,
//                    sceneView = sceneView,
//                    instructionPoints = instructionPoints,
//                    instructionList = instructionList,
//                    yourLocation = yourLocation,
//                    yourDestination = destination,
//                    modelLoader = modelLoader,
//                    arrowNodes = arrowNodes,
//                    roadCircleNodes = roadCircleNodes,
//                    context = context,
//                    coroutineScope = coroutineScope,
//                    compassViewModel = compassViewModel,
//                    arNavigationViewModel = arNavigationViewModel,
//                    engine = engine,
//                    materialLoader = materialLoader
//                )

//                fourthIsShowingARNavigationArrowOneByOne(
//                    index = currentPendingIndex,
//                    sceneView = sceneView,
//                    instructionPoints = instructionPoints,
//                    instructionList = instructionList,
//                    yourLocation = yourLocation,
//                    yourDestination = destination,
//                    modelLoader = modelLoader,
//                    arrowNodes = arrowNodes,
//                    roadCircleNodes = roadCircleNodes,
//                    context = context,
//                    coroutineScope = coroutineScope,
//                    compassViewModel = compassViewModel,
//                    arNavigationViewModel = arNavigationViewModel,
//                    engine = engine,
//                    materialLoader = materialLoader
//                )

                isShowingARNavigationArrowOneByOneBasedOnCompass(
                    index = currentPendingIndex,
                    sceneView = sceneView,
                    instructionPoints = instructionPoints,
                    instructionList = instructionList,
                    yourLocation = yourLocation,
                    yourDestination = destination,
                    modelLoader = modelLoader,
                    arrowNodes = arrowNodes,
                    roadCircleNodes = roadCircleNodes,
                    context = context,
                    coroutineScope = coroutineScope,
                    compassViewModel = compassViewModel,
                    arNavigationViewModel = arNavigationViewModel,
                    engine = engine,
                    materialLoader = materialLoader
                )

                // Show All Arrow at once
//                isShowingARNavigationArrowAllAtOnce(
//                    sceneView = sceneView,
//                    instructionPoints = instructionPoints,
//                    instructionList = instructionList,
//                    yourLocation = yourLocation,
//                    yourDestination = destination,
//                    modelLoader = modelLoader,
//                    arrowNodes = arrowNodes,
//                    roadCircleNodes = roadCircleNodes,
//                    context = context,
//                    coroutineScope = coroutineScope,
//                    compassViewModel = compassViewModel,
//                    engine = engine,
//                    materialLoader = materialLoader
//                )

                currentPendingIndex = null // done
            } else {
                delay(500) // retry every 0.5 seconds
                pendingArrowIndex = instructionIndex
                currentPendingIndex = instructionIndex
//                Log.w("ARCoreDebug", "🚫 Not tracking — waiting to show arrow $currentPendingIndex")
            }

            currentLastTrackingState = trackingState
        }
    }

    Box(modifier = modifier) {
        ARScene(
            modifier = arModifier,
            engine = engine,
            sessionFeatures = setOf(),
            planeRenderer = false,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onSessionUpdated = { session, frame ->
                val camera = frame.camera
                if (camera.trackingState == TrackingState.TRACKING) {
//                    trackingMessage.value = "Tracking OK"
                    isShowingTrackingMessage = false
                    if (pendingArrowIndex != null) {
                        pendingArrowIndex = null
                    }
                } else {
                    isShowingTrackingMessage = true
//                    trackingMessage.value = "Tracking lost, move slowly..."
                    pendingArrowIndex = instructionIndex
                }

                lastTrackingState = camera.trackingState
            },
            onViewCreated = {
                val sceneView = this
                sceneViewRef.value = sceneView

//                if (!arViewInitialized.value) {
//                    arViewInitialized.value = true
//                }
            },
            cameraStream = rememberARCameraStream(materialLoader),
//            cameraStream = if (cameraStreamEnabled) cameraMaterialStream else null,
            onSessionCreated = { session ->
                // Handle session creation
            },
            onSessionResumed = { session ->
                // Handle session resume

            },
            onSessionPaused = { session ->
                // Handle session pause
//                session.pause()
//                if (arNavigationViewModel.isShowingAndClosingARNavigation.value) {
//                    session.resume()
//                } else {
//                    session.pause()
//                }
            },
            // Error handling
            onSessionFailed = { exception ->
                // Handle ARCore session errors
            },

            // Track camera tracking state changes
            onTrackingFailureChanged = { trackingFailureReason ->
                // Handle tracking failures
                trackingFailureReason.apply {

                }
            }
        )

        if (arNavigationViewModel.isShowingRotatingPhoneMessage.value) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
//                SmallRotatingPhoneAnimation()
                Image(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.Transparent),   //crops the image to circle shape
                    painter = rememberDrawablePainter(
                        drawable = getDrawable(
                            LocalContext.current,
                            R.drawable.scan_direction_all_around
                        )
                    ),
                    contentDescription = "Loading animation",
                    contentScale = ContentScale.FillHeight,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please rotate your phone horizontally in all direction and slowly until the Arrow appears!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp)
                        .zIndex(1f)
                )
            }
        } else if (isShowingTrackingMessage) {
            Text(
                text = trackingMessage.value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(3f, 3f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .zIndex(1f)
            )
        }

//        if (isShowingTrackingMessage) {
//            Text(
//                text = trackingMessage.value,
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Light,
//                color = Color.White,
//                textAlign = TextAlign.Center,
//                style = LocalTextStyle.current.copy(
//                    shadow = Shadow(
//                        color = Color.White,
//                        offset = Offset(3f, 3f),
//                        blurRadius = 3f
//                    )
//                ),
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(horizontal = 16.dp)
//                    .zIndex(1f)
//            )
//        }
    }
}

private fun logCameraTrackingStatus(
    sceneView: ARSceneView?,
    trackingMessage: MutableState<String>,
    coroutineScope: CoroutineScope,
    lastTrackingState: MutableState<TrackingState?>
) {
    val frame = sceneView?.frame
    val camera = frame?.camera

    if (camera == null) {
        Log.w("ARCoreDebug", "⚠️ Camera not ready yet")
        trackingMessage.value = "Please move your Android phone slowly!"
        return
    }

    when (camera.trackingState) {
        TrackingState.TRACKING -> {
            coroutineScope.launch {
                if (lastTrackingState.value != TrackingState.TRACKING) {
                    Log.d("ARCoreDebug", "✅ Camera tracking normally")
                    trackingMessage.value = "You are good to go! 👍"
                    delay(1500)
                    if (sceneView.frame?.camera?.trackingState == TrackingState.TRACKING) {
                        trackingMessage.value = ""
                    }
                }
                lastTrackingState.value = TrackingState.TRACKING
            }
        }

        TrackingState.PAUSED -> {
            val reason = camera.trackingFailureReason
            val message = when (reason) {
                TrackingFailureReason.BAD_STATE -> "Bad state!"
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Low light!\n\nPlease move to a more brightly lit area"
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Not enough features!\n\nPlease move to a different area and avoid blank walls"
                TrackingFailureReason.EXCESSIVE_MOTION -> "Too much motion!\n\nPlease move your device more slowly!"
                else -> "The tracking is paused.\n\nPlease move slowly in a well-lit area!"
            }
            Log.w("ARCoreDebug", "🚫 Tracking paused: $message")
            trackingMessage.value = "❗ Tracking Paused ➾ $message"
            lastTrackingState.value = TrackingState.PAUSED
        }

        TrackingState.STOPPED -> {
            Log.e("ARCoreDebug", "🛑 Tracking stopped")
            trackingMessage.value = "❌ Tracking Stopped ➾ Please try again."
            lastTrackingState.value = TrackingState.STOPPED
        }

        else -> {
            trackingMessage.value = "Unknown tracking state"
        }
    }
}

// Works - 1
//fun addArrowWithYaw(
//    anchor: Anchor?,
//    index: Int,
//    yaw: Float,
//    modelLoader: ModelLoader,
//    instructionList: List<SerializableNavigationInstruction>,
//    sceneView: ARSceneView,
//    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
//    context: Context
//) {
//    if (anchor == null) {
//        Toast.makeText(context, "AR is still initializing. Please wait...", Toast.LENGTH_SHORT).show()
//        Log.w("ARCoreDebug", "🚫 Arrow skipped at index $index due to null anchor")
//        return
//    }
//
//    val instruction = instructionList.getOrNull(index)?.instruction ?: "Straight"
//    val isDestination = instruction.equals("Arrive at destination", ignoreCase = true)
//    val modelPath = if (isDestination) "models/arrive_at_destination_3.glb" else "models/direction_arrow.glb"
//
//    modelLoader.loadModelInstanceAsync(modelPath) { modelInstance ->
//        if (modelInstance == null) {
//            Log.e("ARCoreDebug", "❌ Failed to load model at index $index")
//            return@loadModelInstanceAsync
//        }
//
//        val modelNode = ModelNode(modelInstance).apply {
//            scale = if (isDestination) Float3(0.1f) else Float3(3.0f)
//            position = Float3(0f, 0.5f, 0f)
//            rotation = Float3(0f, (yaw - 90f + 360f) % 360f, 0f)
//        }
//
//        val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
//            addChildNode(modelNode)
//        }
//
//        sceneView.addChildNode(anchorNode)
//        arrowNodes.add(anchorNode)
//    }
//}

// Works and the main code
fun addArrowWithYaw(
    anchor: Anchor?,
    index: Int,
    yaw: Float,
    modelLoader: ModelLoader,
    instructionList: List<SerializableNavigationInstruction>,
    sceneView: ARSceneView,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context
) {
    if (anchor == null) {
        Toast.makeText(context, "AR is still initializing. Please wait...", Toast.LENGTH_SHORT).show()
        Log.w("ARCoreDebug", "🚫 Arrow skipped at index $index due to null anchor")
        return
    }

    val text = instructionList.getOrNull(index)?.instruction?.lowercase() ?: "straight"
    val isDestination = text.lowercase().contains("arrive at destination")

    val arrowName = when {
        text.contains("take stairs up") -> "arrow_up_and_down"
        text.contains("take stairs down") -> "arrow_up_and_down_copy"
        text.contains("take elevator up") -> "arrow_up_and_down"
        text.contains("take elevator down") -> "arrow_up_and_down_copy"
        text.contains("arrive at destination") -> "arrive_at_destination_3"
        else -> "direction_arrow"
    }

    val modelPath = "models/$arrowName.glb"

    modelLoader.loadModelInstanceAsync(modelPath) { modelInstance ->
        if (modelInstance == null) {
            Log.e("ARCoreDebug", "❌ Failed to load model at index $index")
            return@loadModelInstanceAsync
        }

        val modelNode = ModelNode(modelInstance).apply {
            // Temporarily set position to compute rotation correctly
            position = Float3(0f, 1.2f, 0f)

            val cameraPosition = sceneView.cameraNode.worldPosition
            val arrowPosition = this.worldPosition

            val dx = cameraPosition.x - arrowPosition.x
            val dz = cameraPosition.z - arrowPosition.z
            val angleY = Math.toDegrees(atan2(dz.toDouble(), dx.toDouble())).toFloat() // For backup from me (Upside down direction)
//            val angleY = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

            scale = when (arrowName) {
                "arrow_up_and_down", "arrow_up_and_down_copy" -> Float3(1.0f)
                else -> if (isDestination) Float3(0.3f) else Float3(3.0f)
            }

            rotation = when (arrowName) {
                "arrow_up_and_down_copy" -> Float3(180f, 90 - angleY, 0f) // Flip vertically (X) + face camera (Y)
                "arrow_up_and_down" -> Float3(0f, 90 - angleY, 0f) // Face camera
                "arrive_at_destination_3" -> Float3(0f, 0f, 0f)
                else -> Float3(0f, (yaw - 90f + 360f) % 360f, 0f)
//                else -> Float3(0f, yaw, 0f)
            }
        }

        val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
            addChildNode(modelNode)
        }

        sceneView.addChildNode(anchorNode)
        arrowNodes.add(anchorNode)
    }
}

// Works and the main code
suspend fun drawRoadBetweenWithGLBImage(
    heading: Float,
    start: SerializableRoutePoint,
    end: SerializableRoutePoint,
    originLat: Double,
    originLon: Double,
    baseY: Float,
    sceneView: ARSceneView,
    modelLoader: ModelLoader,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return

    val deltaLat = end.lat - start.lat
    val deltaLon = end.lon - start.lon
    val dist = distanceInMeters(start.lat, start.lon, end.lat, end.lon)
    val steps = max(1, (dist / 1.0).toInt())

    for (step in 0 until steps) {
        delay(30)

        val currentSession = sceneView.session ?: continue
        val currentFrame = sceneView.frame ?: continue

        if (currentFrame.camera.trackingState != TrackingState.TRACKING) {
            if (step == 0) {
                Toast.makeText(context, "Waiting for AR tracking...", Toast.LENGTH_SHORT).show()
                Log.w("ARCoreDebug", "🚫 Road skipped (camera not tracking)")
            }
            continue
        }

        val t = step.toFloat() / steps
        val lat = start.lat + deltaLat * t
        val lon = start.lon + deltaLon * t
        val dLat = lat - originLat
        val dLon = lon - originLon

        // For backup from me - works 1
//        val localX = -dLon * 111000f * cos(Math.toRadians(originLat)).toFloat() // Fixed on the Flipped direction of Turn Right and Turn Left (it's going to the Left instead of to the Right)
////        val localX = dLon * 111000f * cos(Math.toRadians(originLat)).toFloat() // For backup from me
////        val localZ = -dLat * 111000f  // For backup from me (Upside down direction)
//        val localZ = dLat * 111000f
//        val roadPose = Pose(floatArrayOf(localX.toFloat(), baseY, localZ.toFloat()), floatArrayOf(0f, 0f, 0f, 1f))

//        val headingRad = Math.toRadians(heading.toDouble())
//        val rotatedX = localX * cos(headingRad) - localZ * sin(headingRad)
//        val rotatedZ = localX * sin(headingRad) + localZ * cos(headingRad)
//        val roadPose = Pose(floatArrayOf(rotatedX.toFloat(), baseY, rotatedZ.toFloat()), floatArrayOf(0f, 0f, 0f, 1f))

        val rawX = -dLon * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val rawZ = dLat * 111000f
        val (localX, localZ) = rotateXZByHeading(rawX.toFloat(), rawZ.toFloat(), heading)

        val roadPose = Pose(floatArrayOf(localX, baseY, localZ), floatArrayOf(0f, 0f, 0f, 1f))

        val anchor = try {
            currentSession.createAnchor(roadPose)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "⚠️ Failed to create road anchor: ${e.message}")
            continue
        }

        modelLoader.loadModelInstanceAsync("models/road_circle.glb") { model ->
            if (model == null) return@loadModelInstanceAsync

//            val node = ModelNode(model).apply {
//                scale = Float3(0.1f)
//                position = Float3(0f, 0f, 0f)
//            }
            val node = ModelNode(model).apply {
                scale = Float3(0.1f)
                position = Float3(0f, 0f, 0f)

                // Change all materials to blue
                model.materialInstances.forEach { materialInstance ->
                    materialInstance.setParameter(
                        "baseColorFactor",
                        Color(0xFF00004d).hashCode()
                    )
                    materialInstance.setColorWrite(true)
                }
            }

            val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
                addChildNode(node)
            }

            sceneView.addChildNode(anchorNode)
            roadCircleNodes.add(anchorNode)

            coroutineScope.launch {
                delay(step * 120L)
                repeat(3) {
                    node.isVisible = false
                    delay(200)
                    node.isVisible = true
                    delay(200)
                }
                val originalScale = node.scale
                node.scale = originalScale * 1.2f
                delay(100)
                node.scale = originalScale
            }
        }
    }
}

fun drawRoadBetweenWithCylinderNode(
    heading: Float,
    start: SerializableRoutePoint,
    end: SerializableRoutePoint,
    originLat: Double,
    originLon: Double,
    baseY: Float,
    sceneView: ARSceneView,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return

    val deltaLat = end.lat - start.lat
    val deltaLon = end.lon - start.lon
    val dist = distanceInMeters(start.lat, start.lon, end.lat, end.lon)
    val steps = max(1, (dist / 1.0).toInt())

    for (step in 0 until steps) {
//        delay(30)

        val currentSession = sceneView.session ?: continue
        val currentFrame = sceneView.frame ?: continue

        if (currentFrame.camera.trackingState != TrackingState.TRACKING) {
            if (step == 0) {
                Toast.makeText(context, "Waiting for AR tracking...", Toast.LENGTH_SHORT).show()
                Log.w("ARCoreDebug", "🚫 Road skipped (camera not tracking)")
            }
            continue
        }

        val t = step.toFloat() / steps
        val lat = start.lat + deltaLat * t
        val lon = start.lon + deltaLon * t
        val dLat = lat - originLat
        val dLon = lon - originLon
        val rawX = -dLon * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val rawZ = dLat * 111000f
        val (localX, localZ) = rotateXZByHeading(rawX.toFloat(), rawZ.toFloat(), heading) // for backup from me - don't delete this
//        val (localX, localZ) = projectToXZ(rawX.toFloat(), rawZ.toFloat()) // for backup from me - don't delete this
//        val (localX, localZ) = Pair(rawX.toFloat(), rawZ.toFloat()) // for backup from me - don't delete this

        val roadPose = Pose(floatArrayOf(localX, baseY, localZ), floatArrayOf(0f, 0f, 0f, 1f))

        val anchor = try {
            currentSession.createAnchor(roadPose)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "⚠️ Failed to create road anchor: ${e.message}")
            continue
        }

        val cylinderNode = CylinderNode(
            engine = engine,
            radius = 0.20f, // thinner than 0.2 for cleaner road
            height = 0.01f, // very flat like a circle
            materialInstance = materialLoader.createColorInstance(
                color = Color(0f, 0f, 1f, 1f), // Blue RGBA
                metallic = 0.5f,
                roughness = 0.4f,
                reflectance = 0.5f
            )
        ).apply {
            transform(
                position = Float3(0f, 0f, 0f),
                rotation = Float3(0f, 0f, 0f) // Lay flat on the ground
            )
        }

        val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
            addChildNode(cylinderNode)
        }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)

//        coroutineScope.launch {
//            delay(step * 120L)
//            repeat(3) {
//                cylinderNode.isVisible = false
//                delay(200)
//                cylinderNode.isVisible = true
//                delay(200)
//            }
//            val originalScale = cylinderNode.scale
//            cylinderNode.scale = originalScale * 1.2f
//            delay(100)
//            cylinderNode.scale = originalScale
//        }

        // with infinite blinking animation - 1
//        coroutineScope.launch {
//            delay(step * 200L) // start offset for sequential effect
//            while (true) {
//                cylinderNode.isVisible = false
//                delay(300)
//                cylinderNode.isVisible = true
//                delay(300)
//            }
//        }

        // with infinite blinking animation - 2
        // After creating and adding each node:
//        coroutineScope.launch {
//            // Delay start based on position in the sequence (step index)
//            delay(step * 800L) // bigger delay = slower "traveling blink" effect
//            while (true) {
//                cylinderNode.isVisible = true
//                delay(300) // visible duration
//                cylinderNode.isVisible = false
////                delay((steps - 1) * 400L) // wait until the "wave" comes back around
//                delay((steps - 1) * 100L) // wait until the "wave" comes back around
//            }
//        }

    }
}


fun isShowingARNavigationArrowOneByOne(
    index: Int,
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "❌ Index $index out of bounds")
        return
    }

    arrowNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

    // for backup from me - works 1
//    val localPositions = instructionPoints.map { point ->
//        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
//        val dz = (point.lat - originLat) * 111_000.0
////        Pair(dx.toFloat(), -dz.toFloat()) // For backup from me (Upside down direction (Go to the South instead of to the North))
//        Pair(-dx.toFloat(), dz.toFloat()) // For backup from me (for fixing the Flipping direction (go to the Left instead of to the Right))
////        Pair(dx.toFloat(), dz.toFloat()) // ✅ FIXED: no negative sign
//    }

//    val compassHeading = SphericalUtil.computeHeading(
//        GmsLatLng(yourLocation.lat, yourLocation.lon),
//        GmsLatLng(yourDestination.lat, yourDestination.lon)
//    )
//    val heading = (compassHeading + 360) % 360

    // For backup from me - with latest Compass
//    val heading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
//    val heading = computeHeading(yourLocation.lat, yourLocation.lon, yourDestination.lat, yourDestination.lon)
//    val heading = SphericalUtil.computeHeading(
//        GmsLatLng(yourLocation.lat, yourLocation.lon),
//        GmsLatLng(yourDestination.lat, yourDestination.lon)
//    )

    // for backup from me - for version 1 of the latest Compass
    val mapHeading = SphericalUtil.computeHeading(
        GmsLatLng(yourLocation.lat, yourLocation.lon),
        GmsLatLng(yourDestination.lat, yourDestination.lon)
    )

    // For backup from me - for version 2 of the latest Compass
//    val mapHeading = if (index < instructionPoints.size - 1) {
//        SphericalUtil.computeHeading(
//            GmsLatLng(instructionPoints[index].lat, instructionPoints[index].lon),
//            GmsLatLng(instructionPoints[index + 1].lat, instructionPoints[index + 1].lon)
//        )
//    } else {
//        SphericalUtil.computeHeading(
//            GmsLatLng(yourLocation.lat, yourLocation.lon),
//            GmsLatLng(yourDestination.lat, yourDestination.lon)
//        )
//    }

    // For backup from me - for version 3 of the latest Compass
//    val mapHeading = if (index < instructionPoints.size - 1) {
//        var heading = SphericalUtil.computeHeading(
//            GmsLatLng(instructionPoints[index].lat, instructionPoints[index].lon),
//            GmsLatLng(instructionPoints[index + 1].lat, instructionPoints[index + 1].lon)
//        )
//        heading = (heading + 360) % 360 // ✅ normalize here
//
//        Log.d(
//            "ArrowPlaced",
//            "Google Map Heading: $heading | Next Point: ${GmsLatLng(instructionPoints[index + 1].lat, instructionPoints[index + 1].lon)}"
//        )
//        heading
//    } else {
//        var heading = SphericalUtil.computeHeading(
//            GmsLatLng(yourLocation.lat, yourLocation.lon),
//            GmsLatLng(yourDestination.lat, yourDestination.lon)
//        )
//        heading = (heading + 360) % 360 // ✅ normalize here
//
//        Log.d(
//            "ArrowPlaced",
//            "Google Map Heading (Final Leg): $heading"
//        )
//        heading
//    }

    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
    val correctedHeading = (mapHeading - deviceHeading + 360) % 360

//    Log.e("ARCompass1", "${heading}")
    Log.e("ARCompass1", "${correctedHeading}")

    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        val (rotatedX, rotatedZ) = rotateXZByHeading(-dx.toFloat(), dz.toFloat(), correctedHeading.toFloat())
        Pair(rotatedX, rotatedZ)
    }

    val (x, z) = localPositions[index]
    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
    val anchor = try {
        session.createAnchor(anchorPose)
    } catch (e: Exception) {
        Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        return
    }

    // for backup from me - works 1
    var yaw = 0f
    if (index < localPositions.size - 1) {
        val (x1, z1) = localPositions[index]
        val (x2, z2) = localPositions[index + 1]
        yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
//        yaw = Math.toDegrees(atan2((z2 - z1).toDouble(), (x2 - x1).toDouble())).toFloat()
    }

//    var yaw = 0f
//    if (index < localPositions.size - 1) {
//        val (x1, z1) = localPositions[index]
//        val (x2, z2) = localPositions[index + 1]
//
//        // Angle between current and next point in local AR space
//        val localAngle = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
//
//        // Get device compass heading
//        val heading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
//
//        // Adjust the local angle to align with the real compass
//        // Subtract heading to cancel out phone orientation
//        yaw = (localAngle - heading + 360) % 360
//    }

    addArrowWithYaw(
        anchor, index, yaw, modelLoader, instructionList, sceneView, arrowNodes, context
    )

    if (index in 0 until instructionPoints.size - 1) {
        val prev = instructionPoints[index]
        val next = instructionPoints[index + 1]
//        val heading = (compassViewModel.deviceHeadingInDegrees + 360) % 360 // for backup from me - with latest Compass
        coroutineScope.launch {
//            drawRoadBetweenWithGLBImage(correctedHeading.toFloat(), prev, next, originLat, originLon, baseY, sceneView, modelLoader, roadCircleNodes, context, coroutineScope)
            drawRoadBetweenWithCylinderNode(
                correctedHeading.toFloat(),
                prev,
                next,
                originLat,
                originLon,
                baseY,
                sceneView,
                roadCircleNodes,
                context = context,
                coroutineScope = coroutineScope,
                engine = engine,
                materialLoader = materialLoader,
            )

            Log.d("ArrowPlaced", "arrow placed at: ${next}")
        }
    }
}

fun secondIsShowingARNavigationArrowOneByOne(
    index: Int,
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "❌ Index $index out of bounds")
        return
    }

    // Clear previous arrows & circles
    arrowNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

    // Compute global correction factor based on device heading
    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360

    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        Pair(dx.toFloat(), dz.toFloat())
    }

    // Anchor position for this step
    val (x, z) = localPositions[index]
    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
    val anchor = try {
        session.createAnchor(anchorPose)
    } catch (e: Exception) {
        Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        return
    }

    // --- Per-step heading calculation ---
    var yaw = 0f
    if (index < instructionPoints.size - 1) {
        val start = instructionPoints[index]
        val end = instructionPoints[index + 1]

        // Heading from start → next point
        val stepHeading = SphericalUtil.computeHeading(
            GmsLatLng(start.lat, start.lon),
            GmsLatLng(end.lat, end.lon)
        )

        // Adjust yaw so it’s relative to device orientation
        yaw = ((stepHeading - deviceHeading + 360) % 360).toFloat()
    }

    // Place arrow with correct yaw
    addArrowWithYaw(
        anchor, index, yaw, modelLoader, instructionList, sceneView, arrowNodes, context
    )

    // Draw the road between current and next point
    if (index in 0 until instructionPoints.size - 1) {
        val prev = instructionPoints[index]
        val next = instructionPoints[index + 1]
        coroutineScope.launch {
            drawRoadBetweenWithGLBImage(
                yaw, prev, next, originLat, originLon, baseY,
                sceneView, modelLoader, roadCircleNodes, context, coroutineScope
            )
        }
    }
}

fun thirdIsShowingARNavigationArrowOneByOne(
    index: Int,
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel,
    arNavigationViewModel: ARNavigationViewModel,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "❌ Index $index out of bounds")
        return
    }

    arrowNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

    val mapHeadingWithGoogleComputeHeading = SphericalUtil.computeHeading(
        GmsLatLng(yourLocation.lat, yourLocation.lon),
        GmsLatLng(yourDestination.lat, yourDestination.lon)
    )

    val mapHeadingWithBearingBetween = bearingBetween(yourLocation.lat, yourLocation.lon, yourDestination.lat, yourDestination.lon)

    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
    val correctedHeadingWithGoogleComputeHeading = (mapHeadingWithGoogleComputeHeading - deviceHeading + 360) % 360
    val correctedHeadingWithBearingBetween = (mapHeadingWithBearingBetween - deviceHeading + 360) % 360
    val correctedHeadingWithInstructionPointHeading = (instructionPoints[0].heading - deviceHeading + 360) % 360

    if (arNavigationViewModel.initialCorrectedHeading == null) {
        arNavigationViewModel.initialCorrectedHeading = correctedHeadingWithBearingBetween
    }
    // Always use the first captured heading
    val lockedHeading = arNavigationViewModel.initialCorrectedHeading ?: correctedHeadingWithBearingBetween

    // Grab the first heading only once
    val firstHeading = (instructionPoints.firstOrNull()?.heading ?: (0.0 + 360)) % 360

    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        val (rotatedX, rotatedZ) = rotateXZByHeading(-dx.toFloat(), dz.toFloat(), lockedHeading.toFloat())
        Pair(rotatedX, rotatedZ)
    }

    val (x, z) = localPositions[index]
    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
    val anchor = try {
        session.createAnchor(anchorPose)
    } catch (e: Exception) {
        Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        return
    }

    // for backup from me - works 1
    var yaw = 0f
    if (index < localPositions.size - 1) {
        val (x1, z1) = localPositions[index]
        val (x2, z2) = localPositions[index + 1]
        yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
    }

    addArrowWithYaw(
        anchor, index, yaw, modelLoader, instructionList, sceneView, arrowNodes, context
    )

    if (index in 0 until instructionPoints.size - 1) {
        val prev = instructionPoints[index]
        val next = instructionPoints[index + 1]

        coroutineScope.launch {
            drawRoadBetweenWithCylinderNode(
                lockedHeading.toFloat(),
                prev,
                next,
                originLat,
                originLon,
                baseY,
                sceneView,
                roadCircleNodes,
                context = context,
                coroutineScope = coroutineScope,
                engine = engine,
                materialLoader = materialLoader,
            )

            Log.d("ArrowPlaced", "arrow placed at: ${next}, heading: ${prev.heading}")
            Log.d("ARCoreHeading", "corrected heading bearing between: $correctedHeadingWithBearingBetween")
            Log.d("ARCoreHeading", "corrected heading google compute: $correctedHeadingWithBearingBetween")
            Log.d("ARCoreHeading", "compute heading with google map heading no normalized: $mapHeadingWithGoogleComputeHeading")
            Log.w("ARCoreHeading", "instruction point heading 1: ${instructionPoints[0].heading}")
//            Log.e("ARCoreHeading", "heading isShowing testing heading: $testingHeading")
//            Log.e("ARCoreHeading", "heading isShowing second testing heading: $secondTestingHeading")
        }
    }
}

fun fourthIsShowingARNavigationArrowOneByOne(
    index: Int,
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel,
    arNavigationViewModel: ARNavigationViewModel,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "❌ Index $index out of bounds")
        return
    }

    // clear old nodes
    arrowNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

    // ✅ Purely use map heading (bearing between origin and destination)
    val mapHeading = bearingBetween(
        yourLocation.lat, yourLocation.lon,
        yourDestination.lat, yourDestination.lon
    )

    // Lock heading once
    if (arNavigationViewModel.initialCorrectedHeading == null) {
        arNavigationViewModel.initialCorrectedHeading = instructionPoints[0].heading
    }
    val lockedHeading = arNavigationViewModel.initialCorrectedHeading ?: instructionPoints[0].heading

    // compute local ENU positions relative to origin, rotated by lockedHeading
    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        val (rotatedX, rotatedZ) = rotateXZByHeading(
            -dx.toFloat(),
            dz.toFloat(),
            lockedHeading.toFloat()
        )
        rotatedX to rotatedZ
    }

    // place arrow at current instruction point
    val (x, z) = localPositions[index]
    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
    val anchor = try {
        session.createAnchor(anchorPose)
    } catch (e: Exception) {
        Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        return
    }

    // compute yaw between current and next point
    var yaw = 0f
    if (index < localPositions.size - 1) {
        val (x1, z1) = localPositions[index]
        val (x2, z2) = localPositions[index + 1]
        yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
    }

    // add arrow
    addArrowWithYaw(
        anchor, index, yaw,
        modelLoader, instructionList,
        sceneView, arrowNodes, context
    )

    // draw connecting road
    if (index in 0 until instructionPoints.size - 1) {
        val prev = instructionPoints[index]
        val next = instructionPoints[index + 1]

        coroutineScope.launch {
            drawRoadBetweenWithCylinderNode(
                lockedHeading.toFloat(),
                prev, next,
                originLat, originLon,
                baseY,
                sceneView, roadCircleNodes,
                context, coroutineScope,
                engine, materialLoader
            )

            Log.d("ArrowPlaced", "arrow placed at: $next, heading: ${prev.heading}")
            Log.d("ARCoreHeading", "locked map heading: $lockedHeading")
        }
    }
}

fun isShowingARNavigationArrowOneByOneBasedOnCompass(
    index: Int,
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel,
    arNavigationViewModel: ARNavigationViewModel,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "❌ Index $index out of bounds")
        return
    }

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

//    val mapHeadingWithBearingBetween = bearingBetween(yourLocation.lat, yourLocation.lon, yourDestination.lat, yourDestination.lon)
//
//    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
//    val correctedHeadingWithBearingBetween = (mapHeadingWithBearingBetween - deviceHeading + 360) % 360

//    Log.d("ARCoreHeadingDebug", "✅ Corrected heading bearing between: $correctedHeadingWithBearingBetween")

    // Lock heading from first leg if not already locked
//    if (arNavigationViewModel.initialCorrectedHeading == null) {
//        arNavigationViewModel.initialCorrectedHeading = correctedHeadingWithBearingBetween
//    }
//    val lockedHeading = arNavigationViewModel.initialCorrectedHeading
//        ?: correctedHeadingWithBearingBetween.toFloat()

    // Ensure alignedIndexes exists
    if (arNavigationViewModel.alignedIndexes.isEmpty()) {
        arNavigationViewModel.alignedIndexes = TreeSet()
    }

    coroutineScope.launch {
        compassViewModel.azimuth.collect { deviceHeadingLive ->
            fun normalize(deg: Float) = ((deg % 360) + 360) % 360
            fun angularDifference(a: Float, b: Float): Float {
                val diff = (a - b + 540) % 360 - 180
                return abs(diff)
            }

//            Log.e("ARCoreHeadingDebug", "Compass: ${deviceHeadingLive}")

            if (instructionPoints.isNotEmpty()) {
                val device = normalize(deviceHeadingLive)
                val locked = normalize(instructionPoints[0].heading.toFloat())
                val diff = angularDifference(device, locked)

                val mapHeadingWithBearingBetween = bearingBetween(instructionPoints[0].lat, instructionPoints[0].lon, yourDestination.lat, yourDestination.lon)
                val correctedHeadingWithBearingBetweenLive = (mapHeadingWithBearingBetween - deviceHeadingLive + 360) % 360
                if (arNavigationViewModel.initialCorrectedHeading == null) {
                    arNavigationViewModel.initialCorrectedHeading = correctedHeadingWithBearingBetweenLive
                }
                val lockedHeading = arNavigationViewModel.initialCorrectedHeading
                    ?: correctedHeadingWithBearingBetweenLive.toFloat()

                val hasAlignedOnce = arNavigationViewModel.hasAlignedOnce
                val waitingLogShown = arNavigationViewModel.waitingLogShown
                val isShowingRotatingPhoneMessage = arNavigationViewModel.isShowingRotatingPhoneMessage
                val alignedIndexes = arNavigationViewModel.alignedIndexes

                fun placeAtIndex() {
                    placeArrowAndRoad(
                        index, lockedHeading.toFloat(), originLat, originLon, baseY,
                        instructionPoints, instructionList, session, sceneView,
                        modelLoader, arrowNodes, roadCircleNodes, context, engine, materialLoader
                    )
                    arNavigationViewModel.addAlignedIndex(index)
                }

                if (index == 0) {
                    // ✅ First index requires alignment
                    if (!hasAlignedOnce.value) {
                        if (!waitingLogShown.value) {
                            Log.d("ARCoreHeadingDebug", "⏸ Waiting for alignment… index=$index locked=$locked device=$device diff=$diff")
                            waitingLogShown.value = true
                            isShowingRotatingPhoneMessage.value = true
                        }
                        // 🔑 Only place when the angular difference matches the "diff" itself (epsilon check)
                        val epsilon = 0.5f // tolerance, adjust as needed
                        if (diff < 0.1f) {
//                        if (abs(diff - 0f) < epsilon) {
                            hasAlignedOnce.value = true
                            isShowingRotatingPhoneMessage.value = false
                            placeAtIndex()

//                            placeArrowAndRoad(
//                                index,
//                                locked.toFloat(),  // 🔑 use locked heading, not corrected+diff
//                                originLat,
//                                originLon,
//                                baseY,
//                                instructionPoints,
//                                instructionList,
//                                session,
//                                sceneView,
//                                modelLoader,
//                                arrowNodes,
//                                roadCircleNodes,
//                                context,
//                                engine,
//                                materialLoader
//                            )
//                            arNavigationViewModel.addAlignedIndex(index)

                            Log.d("ARCoreHeadingDebug", "display heading: $lockedHeading")
                            Log.d("ARCoreHeadingDebug", "✅ Aligned! Showing arrow at index=$index")
                            Log.d("ARCoreHeadingDebug", "✅ Mapxus heading 0: ${instructionPoints[0].heading.toFloat()}")
                            Log.d("ARCoreHeadingDebug", "✅ Corrected heading bearing between live: $correctedHeadingWithBearingBetweenLive")
                        }
                    } else {
//                    if (!alignedIndexes.contains(0)) {
//                        Log.d("ARCoreHeadingDebug", "🔄 Re-showing arrow at index=0")
//                        placeAtIndex()
//                        alignedIndexes.add(0)
//                    }
                    }
                } else {
                    // ✅ Later indexes skip alignment
                    if (!alignedIndexes.contains(index)) {
                        // 🧹 Remove all indexes >= current, so we can "go back" cleanly
                        placeAtIndex()
//                        arNavigationViewModel.addAlignedIndex(index)

//                        placeArrowAndRoad(
//                            index,
//                            lockedHeading.toFloat(),  // 🔑 use locked heading, not corrected+diff
//                            originLat,
//                            originLon,
//                            baseY,
//                            instructionPoints,
//                            instructionList,
//                            session,
//                            sceneView,
//                            modelLoader,
//                            arrowNodes,
//                            roadCircleNodes,
//                            context,
//                            engine,
//                            materialLoader
//                        )
//                        arNavigationViewModel.addAlignedIndex(index)
                    }
                }
            } else {
                Log.d("ARCoreHeadingDebug", "Instruction point is empty!.")
            }
        }
    }

}

private fun placeArrowAndRoad(
    index: Int,
    lockedHeading: Float,
    originLat: Double,
    originLon: Double,
    baseY: Float,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    session: Session,
    sceneView: ARSceneView,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    // Compute local ENU position
    if (instructionPoints.isNotEmpty() && index in instructionPoints.indices) {
        val dx = (instructionPoints[index].lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (instructionPoints[index].lat - originLat) * 111_000.0
        val (rotatedX, rotatedZ) = rotateXZByHeading(-dx.toFloat(), dz.toFloat(), lockedHeading) // for backup from me

        val anchorPose = Pose(floatArrayOf(rotatedX, baseY, rotatedZ), floatArrayOf(0f, 0f, 0f, 1f))
        val anchor = try {
            session.createAnchor(anchorPose)
        } catch (e: Exception) {
            Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        var yaw = 0f
        if (index < instructionPoints.size - 1) {
            val dx1 = (instructionPoints[index + 1].lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
            val dz1 = (instructionPoints[index + 1].lat - originLat) * 111_000.0
            val (rx1, rz1) = rotateXZByHeading(-dx1.toFloat(), dz1.toFloat(), lockedHeading)
            yaw = Math.toDegrees(atan2((rx1 - rotatedX).toDouble(), (rz1 - rotatedZ).toDouble())).toFloat()
        }

        addArrowWithYaw(anchor, index, yaw, modelLoader, instructionList, sceneView, arrowNodes, context)

        if (index in 0 until instructionPoints.size - 1) {
            drawRoadBetweenWithCylinderNode(
                lockedHeading,
                instructionPoints[index],
                instructionPoints[index + 1],
                originLat,
                originLon,
                baseY,
                sceneView,
                roadCircleNodes,
                context,
                CoroutineScope(Dispatchers.Main),
                engine,
                materialLoader
            )
        }
    } else {
        Log.w("ARCoreDebug", "⚠ instructionPoints empty or index=$index out of range. Size=${instructionPoints.size}")
    }
}

private fun placeArrowInFrontOfCamera(
    sceneView: ARSceneView,
    session: Session,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context
) {
    val cameraPose = sceneView.frame?.camera?.pose ?: return

    // Place the arrow 1.5m in front of the camera
    val forwardDistance = 1.5f
    val forwardPose = cameraPose
        .compose(Pose.makeTranslation(0f, 0f, -forwardDistance))

    val anchor = try {
        session.createAnchor(forwardPose)
    } catch (e: Exception) {
        Toast.makeText(context, "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        return
    }

    // Default yaw = 0 since it's locked to camera orientation
    addArrowWithYaw(anchor, -1, 0f, modelLoader, emptyList(), sceneView, arrowNodes, context)
}


fun isShowingARNavigationArrowAllAtOnce(
    sceneView: ARSceneView,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    yourLocation: RoutePlanningPoint,
    yourDestination: RoutePlanningPoint,
    modelLoader: ModelLoader,
    arrowNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    roadCircleNodes: SnapshotStateList<io.github.sceneview.node.Node>,
    context: Context,
    coroutineScope: CoroutineScope,
    compassViewModel: CompassViewModel,
    engine: com.google.android.filament.Engine,
    materialLoader: MaterialLoader
) {
    val session = sceneView.session ?: return
    val frame = sceneView.frame ?: return
    val camera = frame.camera

    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "🚫 Camera not tracking — arrows skipped")
        return
    }

    if (instructionPoints.isEmpty()) {
        Log.w("ARCoreDebug", "❌ No instruction points provided")
        return
    }

    // clear all existing nodes first
    arrowNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon
    val baseY = frame.camera.pose.ty() - 1.5f

//    val mapHeading = SphericalUtil.computeHeading(
//        GmsLatLng(yourLocation.lat, yourLocation.lon),
//        GmsLatLng(yourDestination.lat, yourDestination.lon)
//    )
//    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
//    val correctedHeading = (mapHeading - deviceHeading + 360) % 360

    val mapHeading = SphericalUtil.computeHeading(
        GmsLatLng(yourLocation.lat, yourLocation.lon),
        GmsLatLng(yourDestination.lat, yourDestination.lon)
    ).let { (it + 360) % 360 } // normalize

    val deviceHeading = (compassViewModel.deviceHeadingInDegrees + 360) % 360
    val correctedHeading = (mapHeading - deviceHeading + 360) % 360

    // compute local positions
    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        val (rotatedX, rotatedZ) = rotateXZByHeading(-dx.toFloat(), dz.toFloat(), correctedHeading.toFloat())
        Pair(rotatedX, rotatedZ)
    }

    // --- Place ALL arrows ---
    instructionPoints.forEachIndexed { idx, point ->
        val (x, z) = localPositions[idx]
        val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
        val anchor = try {
            session.createAnchor(anchorPose)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "⚠️ Failed to create anchor: ${e.message}")
            return@forEachIndexed
        }

        // compute yaw from this point to the next
        var yaw = 0f
        if (idx < localPositions.size - 1) {
            val (x1, z1) = localPositions[idx]
            val (x2, z2) = localPositions[idx + 1]
            yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
        }

        addArrowWithYaw(
            anchor, idx, yaw, modelLoader, instructionList, sceneView, arrowNodes, context
        )
    }

    // --- Draw ALL roads ---
    for (idx in 0 until instructionPoints.size - 1) {
        val prev = instructionPoints[idx]
        val next = instructionPoints[idx + 1]
        coroutineScope.launch {
            drawRoadBetweenWithCylinderNode(
                correctedHeading.toFloat(),
                prev,
                next,
                originLat,
                originLon,
                baseY,
                sceneView,
                roadCircleNodes,
                context = context,
                coroutineScope = coroutineScope,
                engine = engine,
                materialLoader = materialLoader,
            )
            Log.d("ArrowPlaced", "road placed between: $prev -> $next")
        }
    }
}

private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

// For backup from me - don't delete this one
fun rotateXZByHeading(e: Float, n: Float, headingDeg: Float): Pair<Float, Float> {
    // Convert heading to radians
    val rad = Math.toRadians(headingDeg.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()

    // ENU → AR world convention
    // East → +X
    // North → +Z
    // Apply rotation so heading 0° = North (+Z)
    val x = e * c - n * s
    val z = e * s + n * c

    return x to z
}

// For backup from me - don't delete this one
//fun rotateXZByHeading(x: Float, z: Float, heading: Float): Pair<Float, Float> {
//    val headingRad = Math.toRadians(heading.toDouble())
//    val cosHeading = cos(headingRad).toFloat()
//    val sinHeading = sin(headingRad).toFloat()
//
//    val rotatedX = x * cosHeading - z * sinHeading
//    val rotatedZ = x * sinHeading + z * cosHeading
//
//    return Pair(rotatedX, rotatedZ)
//}

// Static placement, no phone heading applied
fun projectToXZ(x: Float, z: Float): Pair<Float, Float> {
    return Pair(x, z)
}

fun computeHeading(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
    val startLatRad = Math.toRadians(startLat)
    val startLngRad = Math.toRadians(startLng)
    val endLatRad = Math.toRadians(endLat)
    val endLngRad = Math.toRadians(endLng)

    val dLng = endLngRad - startLngRad
    val y = Math.sin(dLng) * Math.cos(endLatRad)
    val x = Math.cos(startLatRad) * Math.sin(endLatRad) -
            Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(dLng)
    return Math.toDegrees(Math.atan2(y, x))
}

fun bearingBetween(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
    val lat1 = Math.toRadians(startLat)
    val lon1 = Math.toRadians(startLng)
    val lat2 = Math.toRadians(endLat)
    val lon2 = Math.toRadians(endLng)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    val bearing = Math.toDegrees(atan2(y, x))
    return (bearing + 360) % 360
}

fun calculateHeading(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

// 🔹 1. Place arrow in the world (no heading rotation)
fun placeArrowAtCoordinate(
    userLat: Double,
    userLon: Double,
    targetLat: Double,
    targetLon: Double
): Pair<Float, Float> {
    val metersPerDegreeLat = 111_320f
    val metersPerDegreeLon = 111_320f * cos(Math.toRadians(userLat))

    val dx = ((targetLon - userLon) * metersPerDegreeLon).toFloat()
    val dz = ((targetLat - userLat) * metersPerDegreeLat).toFloat()

    return Pair(dx, dz)
}

// 🔹 2. Compute facing direction of the arrow
fun getArrowRotation(userLat: Double, userLon: Double, targetLat: Double, targetLon: Double): Float {
    val deltaLon = targetLon - userLon
    val deltaLat = targetLat - userLat
    val angleRad = atan2(deltaLon, deltaLat) // relative to North
    return Math.toDegrees(angleRad).toFloat()
}

// --- helpers ---------------------------------------------------------------
private fun normalizeDeg(d: Double) = ((d % 360) + 360) % 360

/** Rotate EN (east, north) into AR world XZ using a single correction angle.
 *  0° keeps North->+Z, East->+X. Positive = clockwise from North.
 */
private fun enuToWorld(
    e: Double,  // East offset in meters
    n: Double,  // North offset in meters
    mapHeadingDeg: Double // from route (bearing), not device
): Pair<Float, Float> {
    val rad = Math.toRadians(mapHeadingDeg)
    val c = cos(rad)
    val s = sin(rad)

    // Rotate ENU by map heading so arrows align with route
    val x = e * c - n * s
    val z = e * s + n * c

    return x.toFloat() to z.toFloat()
}

fun bearingToDirection(bearing: Double, use16: Boolean = false): String {
    // 8 directions (full words)
    val dirs8 = arrayOf(
        "North",
        "North East",
        "East",
        "South East",
        "South",
        "South West",
        "West",
        "North West"
    )

    // 16 directions (full words)
    val dirs16 = arrayOf(
        "North",
        "North North East",
        "North East",
        "East North East",
        "East",
        "East South East",
        "South East",
        "South South East",
        "South",
        "South South West",
        "South West",
        "West South West",
        "West",
        "West North West",
        "North West",
        "North North West"
    )

    val dirs = if (use16) dirs16 else dirs8

    // ✅ Normalize bearing to [0, 360)
    val normalizedBearing = ((bearing % 360) + 360) % 360

    val segmentSize = 360.0 / dirs.size
    val index = ((normalizedBearing + segmentSize / 2) / segmentSize).toInt() % dirs.size

    return dirs[index]
}


fun drawArrowAndRoad(
    modelLoader: ModelLoader,
    session: ARSession,
    frame: Frame,
    yourLocation: RoutePlanningPoint,
    instructionPoints: List<SerializableRoutePoint>,
    instructionList: List<SerializableNavigationInstruction>,
    arrowNodes: MutableList<io.github.sceneview.node.Node>,
    roadCircleNodes: MutableList<io.github.sceneview.node.Node>,
    index: Int,
    sceneView: ARSceneView
) {
    val camera = frame.camera
    if (camera.trackingState != TrackingState.TRACKING) {
        Log.w("ARCoreDebug", "⚠️ Tracking not active. Skipping arrow.")
        return
    }

    if (index >= instructionPoints.size) {
        Log.w("ARCoreDebug", "⚠️ Index $index out of bounds (${instructionPoints.size})")
        return
    }

    // ❌ Clear previous nodes
    arrowNodes.forEach { sceneView.removeChildNode(it) }
    arrowNodes.clear()
    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
    roadCircleNodes.clear()

    val originLat = yourLocation.lat
    val originLon = yourLocation.lon

    // Convert GPS to local X/Z positions
    val localPositions = instructionPoints.map { point ->
        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
        val dz = (point.lat - originLat) * 111_000.0
        Pair(dx.toFloat(), -dz.toFloat())
    }

    val (x, z) = localPositions[index]

    // 🚫 Skip if too far from camera
    if (abs(x) > 20f || abs(z) > 20f) {
        Log.w("ARCoreDebug", "❌ Anchor too far from camera: (x=$x, z=$z)")
        return
    }

    val cameraY = frame.camera.pose.ty()
    val baseY = if (cameraY.isFinite() && cameraY > 0.1f) cameraY - 1.5f else 0f

    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
    val anchor = try {
        session.createAnchor(anchorPose)
    } catch (e: Exception) {
        Log.e("ARCoreDebug", "❌ Failed to create anchor: ${e.message}", e)
        return
    }

    // 🔄 Calculate Yaw rotation toward next point
    var yaw = 0f
    if (index < localPositions.size - 1) {
        val (x1, z1) = localPositions[index]
        val (x2, z2) = localPositions[index + 1]
        yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
    }

    // 📍 Choose model type based on instruction
    val modelPath = when (instructionList.getOrNull(index)?.instruction?.lowercase()) {
        "arrive at destination" -> "models/arrive_at_destination_3.glb"
        else -> "models/direction_arrow.glb"
    }

    modelLoader.loadModelInstanceAsync(modelPath) { model ->
        if (model != null) {
            val node = ModelNode(model).apply {
                scale = Float3(3.0f)
                position = Float3(0f, 0.5f, 0f)
                rotation = Float3(0f, (yaw - 90 + 360) % 360, 0f)
            }

            val anchorNode = AnchorNode(modelLoader.engine, anchor).apply {
                addChildNode(node)
            }

            sceneView.addChildNode(anchorNode)
            arrowNodes.add(anchorNode)

            Log.d("ARCoreDebug", "✅ Arrow placed at index=$index x=$x z=$z yaw=$yaw")
        } else {
            Log.e("ARCoreDebug", "❌ Model not loaded: $modelPath")
        }
    }

    // TODO: Draw connecting road circles if needed
}

