package com.mapxushsitp.arComponents

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mapxushsitp.data.model.SerializableRouteInstruction
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class FourthLocalARFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var sceneView: ARSceneView
    private lateinit var modelLoader: ModelLoader
    private lateinit var textToSpeech: TextToSpeech
    private val arNavigationViewModel: ARNavigationViewModel by activityViewModels<ARNavigationViewModel>()
    private var instructionIndex = 0
    private var secondInstructionIndex = mutableIntStateOf(0)
    private lateinit var destination: SerializableRoutePoint
    private lateinit var yourLocation: SerializableRoutePoint
    private val instructionList = mutableListOf<SerializableRouteInstruction>()
    private val instructionPoints = mutableListOf<SerializableRoutePoint>()

    private val arrowNodes = mutableListOf<AnchorNode>()
    private val roadCircleNodes = mutableListOf<AnchorNode>()
    private val drawnRoadSegments = mutableSetOf<Pair<Int, Int>>()

    private lateinit var trackingStatusTextView: TextView
    private lateinit var nextRouteBtn: Button

    private var hasShownFirstArrow: Boolean = false
    private var pendingArrowIndex: Int? = null
    private var lastTrackingState: TrackingState? = null

    private lateinit var ctx: Context

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startARSetup()
        } else {
            Toast.makeText(requireContext(), "Permissions denied. Please allow them to proceed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMissingPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            launch {
                delay(1000)
                startARSetup()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        // ✅ Create main container
        val frameLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // ✅ Initialize AR scene view
        sceneView = ARSceneView(context).apply {
            planeRenderer.isVisible = false
            configureSession { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                session.configure(config)
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        modelLoader = ModelLoader(sceneView.engine, context)

        // ✅ Status TextView (camera tracking)
        trackingStatusTextView = TextView(context).apply {
            text = "Initializing AR..."
            setTextColor(Color.White.hashCode())
            setBackgroundColor(Color.Black.copy(alpha = 0.6F).hashCode()) // semi-transparent black
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
            setShadowLayer(
                16f, 3f, 3f,
                Color.White.hashCode() // semi-transparent yellow
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL or Gravity.TOP
            )
        }

        // For testing
//        trackingStatusTextView = TextView(context).apply {
//            setBackgroundColor(Color.Red.hashCode())
//            text = "TESTING TESTING TESTING TESTING TESTING TESTING"
//            textSize = 50F
//            visibility = View.VISIBLE
//        }

        // ✅ Next Route Button
//        nextRouteBtn = Button(context).apply {
//            text = "Next Route"
//            isEnabled = false
//            visibility = View.INVISIBLE
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//                bottomMargin = 80
//            }
//        }

        frameLayout.addView(sceneView)
        frameLayout.addView(trackingStatusTextView)
//        frameLayout.addView(nextRouteBtn)

        Log.d("ARCoreDebug", "📦 trackingStatusTextView added to layout")

        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestMissingPermissions()
    }

    override fun onPause() {
        super.onPause()
        sceneView.session?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancel()
        textToSpeech.shutdown()
    }

    private fun startARSetup() {
        val frame = sceneView.frame
        val camera = frame?.camera

        arguments?.let { bundle ->
            instructionIndex = bundle.getInt("instructionIndex", 0)
            secondInstructionIndex.intValue = bundle.getInt("secondInstructionIndex", 0)

            yourLocation = bundle.getSerializable("yourLocation") as? SerializableRoutePoint
                ?: throw IllegalArgumentException("Missing yourLocation")

            destination = bundle.getSerializable("destination") as? SerializableRoutePoint
                ?: throw IllegalArgumentException("Missing destination")

            val instructionListSerializable = bundle.getSerializable("instructionList") as? ArrayList<SerializableRouteInstruction>
            val instructionPointsSerializable = bundle.getSerializable("instructionPoints") as? ArrayList<SerializableRoutePoint>

            if (!instructionListSerializable.isNullOrEmpty()) {
                instructionList.clear()
                instructionList.addAll(instructionListSerializable)
            } else {
                Log.w("ARCoreDebug", "⚠️ instructionList is null or empty")
            }

            if (!instructionPointsSerializable.isNullOrEmpty()) {
                instructionPoints.clear()
                instructionPoints.addAll(instructionPointsSerializable)
            } else {
                Log.w("ARCoreDebug", "⚠️ instructionPoints is null or empty")
            }

            Log.d("ARCoreDebug", "instruction index: ${instructionIndex}")
            Log.d("ARCoreDebug", "✅ Loaded AR args — instructions: ${instructionList.size}, points: ${instructionPoints.size}")
        } ?: run {
            Log.e("ARCoreDebug", "❌ arguments is null in Fragment")
        }

        Log.d("ARCoreDebug", "✅ startARSetup: ${instructionPoints.size} points loaded")
        resumeARSession()

        // ViewModel observer (runs once per instruction index update)
        arNavigationViewModel.instructionIndex.observe(viewLifecycleOwner) { index ->
            val trackingState = sceneView.frame?.camera?.trackingState
            if (trackingState == TrackingState.TRACKING) {
                Log.d("ARCoreDebug", "✅ Tracking — showing arrow index $index")
                pendingArrowIndex = null
                isShowingARNavigationArrowOneByOne(index)
            } else {
                Log.w("ARCoreDebug", "🚫 Not tracking — deferring arrow $index")
                pendingArrowIndex = index
            }
        }

        // Coroutine to monitor tracking and process pending arrow if needed
        launch {
            while (isActive) {
                logCameraTrackingStatus()

                val currentState = sceneView.frame?.camera?.trackingState

                if (lastTrackingState != currentState) {
                    Log.d("ARCoreDebug", "📷 Tracking state changed: $currentState")
                }

                if (pendingArrowIndex != null && currentState == TrackingState.TRACKING) {
                    Log.d("ARCoreDebug", "✅ AR tracking restored — showing pending arrow at index $pendingArrowIndex")
                    pendingArrowIndex?.let {
                        isShowingARNavigationArrowOneByOne(it)
                        pendingArrowIndex = null // clear after showing
                    }
                }

                lastTrackingState = currentState
                delay(1000) // or shorter (500ms) if responsiveness is needed
            }
        }

    }

    private fun resumeARSession() {
        try {
            sceneView.session?.resume()
            sceneView.session?.let {
                sceneView.onSessionResumed(it)
            }
            Log.d("ARCoreDebug", "✅ AR session resumed")
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "❌ Failed to resume session: ${e.message}")
        }
    }

    private fun isShowingCameraStatus(): View {
        val frameLayout = FrameLayout(requireContext())

        trackingStatusTextView = TextView(requireContext()).apply {
            text = "Initializing AR..."
            setTextColor(Color.White.hashCode())
            setBackgroundColor(Color.Black.copy(alpha = 0.6F).hashCode()) // semi-transparent black
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
            setShadowLayer(
                16f, 3f, 3f,
                Color.White.hashCode() // semi-transparent yellow
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }

        frameLayout.addView(trackingStatusTextView)

        return frameLayout
    }

    private fun logCameraTrackingStatus() {
        val frame = sceneView.frame
        val camera = frame?.camera

        if (camera == null) {
            Log.w("ARCoreDebug", "⚠️ Camera not ready yet")
            trackingStatusTextView.apply {
                text = "Please move your Android phone slowly!"
                visibility = View.VISIBLE
            }
//            nextRouteBtn.isEnabled = false
//            nextRouteBtn.visibility = View.INVISIBLE
            return
        }

        when (camera.trackingState) {
            TrackingState.TRACKING -> {
                launch {
                    Log.d("ARCoreDebug", "✅ Camera tracking normally")
                    trackingStatusTextView.apply {
                        text = "You are good to go! 👍"
                        visibility = View.VISIBLE
                    }
                    delay(1500)
                    // Only hide if the tracking state is still TRACKING
                    if (camera.trackingState == TrackingState.TRACKING) {
                        trackingStatusTextView.visibility = View.INVISIBLE
                    }
//                    nextRouteBtn.isEnabled = true
//                    nextRouteBtn.visibility = View.VISIBLE
                }
            }
            TrackingState.PAUSED -> {
                val reason = camera.trackingFailureReason
                val message = when (reason) {
                    TrackingFailureReason.BAD_STATE -> "Bad state!"
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> "Low light!.\n\n\nPlease move to a more brightly lit area"
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> "Not enough features!.\n\n\nPlease move to a different area and avoid blank walls and surfaces without detail"
                    TrackingFailureReason.EXCESSIVE_MOTION -> "Too much motion!.\n\n\nPlease move your device more slowly!"
                    else -> "The Tracking is still paused.\n\n\nPlease move your device more slowly and face the back Camera to the right angle with a brightly lit area!"
                }
                Log.w("ARCoreDebug", "🚫 Tracking paused: $message")
                trackingStatusTextView.apply {
                    text = "❗ Tracking Paused ➾ $message"
                    visibility = View.VISIBLE
                }
//                nextRouteBtn.isEnabled = false
//                nextRouteBtn.visibility = View.INVISIBLE
            }
            TrackingState.STOPPED -> {
                Log.e("ARCoreDebug", "🛑 Tracking stopped")
                trackingStatusTextView.apply {
                    text = "❌ Tracking Stopped ➾ Please try again."
                    visibility = View.VISIBLE
                }
//                nextRouteBtn.isEnabled = false
//                nextRouteBtn.visibility = View.INVISIBLE
            }
            else -> {
                trackingStatusTextView.apply {
                    text = "Unknown tracking state"
                    visibility = View.VISIBLE
                }
//                nextRouteBtn.isEnabled = false
//                nextRouteBtn.visibility = View.INVISIBLE
            }
        }
    }

    private fun nextRouteButton() : View {
        // Button
        val button = Button(context).apply {
            text = if (instructionIndex <= 0) { "Show Route \uD83D\uDCCD" } else { "Next Route 📍"}
            setPadding(160, 20, 160, 20)
            setTextColor(Color.Black.hashCode())
            textSize = 18f
            isAllCaps = false

            // ✅ Create a rounded background drawable
            background = GradientDrawable().apply {
                setColor(Color.White.hashCode())
                cornerRadius = 100F  // 🎯 Set the radius in pixels
                setStroke(2, Color.White.copy(alpha = 0.3F).hashCode()) // Optional: border color
            }
            paddingBottom

            setOnClickListener {
                if (instructionIndex < instructionPoints.size) {
                    val session = sceneView.session ?: return@setOnClickListener
                    val frame = sceneView.frame ?: return@setOnClickListener

                    // ✅ Remove previous arrow and road nodes
                    arrowNodes.forEach { sceneView.removeChildNode(it) }
                    arrowNodes.clear()

                    roadCircleNodes.forEach { sceneView.removeChildNode(it) }
                    roadCircleNodes.clear()  // <- remove previously drawn road
                    drawnRoadSegments.clear()

                    val originLat = yourLocation.lat
                    val originLon = yourLocation.lon
                    val baseY = frame.camera.pose.ty() - 1.5f

                    // Calculate all local positions once
                    val localPositions = instructionPoints.map { point ->
                        val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
                        val dz = (point.lat - originLat) * 111_000.0
                        Pair(dx.toFloat(), -dz.toFloat())
                    }

                    val (x, z) = localPositions[instructionIndex]
                    if (!listOf(x, baseY, z).all { it.isFinite() }) {
                        Log.w("ARCoreDebug", "⚠️ Invalid local position for arrow $instructionIndex")
                        return@setOnClickListener
                    }

                    val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
                    val anchor = session.createAnchor(anchorPose)

                    // ✅ Calculate yaw toward next point
                    var yaw = 0f
                    if (instructionIndex < localPositions.size - 1) {
                        val (x1, z1) = localPositions[instructionIndex]
                        val (x2, z2) = localPositions[instructionIndex + 1] // FIXED INDEX
                        val dx = x2 - x1
                        val dz = z2 - z1
                        yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
                    }

                    // 🧭 Add arrow facing next point
                    addArrowWithYaw(anchor, instructionIndex, yaw)

                    // ✅ Draw path from previous to current
                    if (instructionIndex >= 0 && instructionIndex < instructionPoints.size - 1) {
                        val prev = instructionPoints[instructionIndex]
                        val current = instructionPoints[instructionIndex + 1]
                        launch {
                            drawRoadBetween(prev, current, originLat, originLon, baseY)
                        }
                    } else if (instructionIndex == instructionPoints.size - 1) {
                        val point = instructionPoints[instructionIndex]
                        launch {
                            drawRoadBetween(point, point, originLat, originLon, baseY)
                        }
                    } else {
                        Log.w("InstructionPath", "❌ Invalid index: $instructionIndex")
                    }

                    // 🔊 Text-to-speech
                    val currentInstruction = instructionList.getOrNull(instructionIndex)?.instruction
                    val currentInstructionDistance = instructionList.getOrNull(instructionIndex)?.distance
                    if (!currentInstruction.isNullOrBlank()) {
                        Log.d("ARCoreDebug", "🔊 Speaking instruction: $currentInstruction")
                        if (currentInstructionDistance != null && currentInstruction != "Arrive at destination") {
                            textToSpeech.speak(
                                "$currentInstruction, and follow the path for ${if (currentInstructionDistance > 1) "meters" else "meter"}",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        } else if (currentInstruction == "Arrive at destination") {
                            textToSpeech.speak(
                                "Kudos! You have arrived at the destination.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }

                    instructionIndex++
                    updateNextRouteButtonText()
                } else {
                    Toast.makeText(context, "🎉 You’ve reached your destination!", Toast.LENGTH_SHORT).show()
                }
            }

            // ✅ Layout params to stick to bottom-center
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 60
            }

            layoutParams = params
        }

        return button
    }

//    private fun addArrowWithYaw(anchor: Anchor, index: Int, yaw: Float) {
//        val instruction = instructionList.getOrNull(index)?.instruction ?: "Straight"
//        val isDestination = instruction.equals("Arrive at destination", ignoreCase = true)
//        val modelPath = if (isDestination) "models/arrive_at_destination.glb" else "models/direction_arrow.glb"
//
//        modelLoader.loadModelInstanceAsync(modelPath) { modelInstance ->
//            if (modelInstance == null) {
//                Log.e("ARCoreDebug", "❌ Failed to load model at index $index")
//                return@loadModelInstanceAsync
//            }
//
//            val modelNode = ModelNode(modelInstance).apply {
//                scale = if (isDestination) Float3(0.1f) else Float3(3.0f)
//                position = Float3(0f, 0.5f, 0f)
//
//                // 🔄 Adjust yaw because model faces +X by default
//                val correctedYaw = (yaw - 90f + 360f) % 360f
//                rotation = Float3(0f, correctedYaw, 0f)
//
//                Log.d("ARCoreDebug", "🧭 Arrow $index rotation = $correctedYaw°")
//            }
//
//            val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
//                addChildNode(modelNode)
//            }
//
//            sceneView.addChildNode(anchorNode)
//            arrowNodes.add(anchorNode)
//        }
//    }

    private fun addArrowWithYaw(anchor: Anchor?, index: Int, yaw: Float) {
        if (anchor == null) {
            Toast.makeText(requireContext(), "AR is still initializing. Please wait...", Toast.LENGTH_SHORT).show()
            Log.w("ARCoreDebug", "🚫 Arrow skipped at index $index due to null anchor")
            return
        }

        val instruction = instructionList.getOrNull(index)?.instruction ?: "Straight"
        val isDestination = instruction.equals("Arrive at destination", ignoreCase = true)
        val modelPath = if (isDestination) "models/arrive_at_destination.glb" else "models/direction_arrow.glb"

        modelLoader.loadModelInstanceAsync(modelPath) { modelInstance ->
            if (modelInstance == null) {
                Log.e("ARCoreDebug", "❌ Failed to load model at index $index")
                return@loadModelInstanceAsync
            }

            val modelNode = ModelNode(modelInstance).apply {
                scale = if (isDestination) Float3(0.1f) else Float3(3.0f)
                position = Float3(0f, 0.5f, 0f)

                val correctedYaw = (yaw - 90f + 360f) % 360f
                rotation = Float3(0f, correctedYaw, 0f)
            }

            val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
                addChildNode(modelNode)
            }

            sceneView.addChildNode(anchorNode)
            arrowNodes.add(anchorNode)
        }
    }

//    private suspend fun drawRoadBetween(
//        start: SerializableRoutePoint,
//        end: SerializableRoutePoint,
//        originLat: Double,
//        originLon: Double,
//        baseY: Float
//    ) {
//        val session = sceneView.session ?: return
//        val frame = sceneView.frame ?: return
//
//        val deltaLat = end.lat - start.lat
//        val deltaLon = end.lon - start.lon
//        val dist = distanceInMeters(start.lat, start.lon, end.lat, end.lon)
//        val steps = max(1, (dist / 1.0).toInt())
//
//        for (step in 0 until steps) {
//            delay(30)
//
//            try {
//                val currentSession = sceneView.session ?: continue
//                val currentFrame = sceneView.frame ?: continue
//                val pose = currentFrame.camera.pose
//
//                if (pose == null || currentFrame.camera.trackingState != TrackingState.TRACKING) {
//                    Log.w("ARCoreDebug", "⚠️ Skipped drawing road at step $step due to camera not tracking")
//                    continue
//                }
//
//                val t = step.toFloat() / steps
//                val lat = start.lat + deltaLat * t
//                val lon = start.lon + deltaLon * t
//                val dLat = lat - originLat
//                val dLon = lon - originLon
//
//                val localX = dLon * 111000f * cos(Math.toRadians(originLat)).toFloat()
//                val localZ = -dLat * 111000f
//
//                if (!listOf(localX, baseY, localZ).all { true }) {
//                    Log.w("ARCoreDebug", "⚠️ Skipped step $step due to non-finite coordinates")
//                    continue
//                }
//
//                val roadPose = Pose(
//                    floatArrayOf(localX.toFloat(), baseY, localZ.toFloat()),
//                    floatArrayOf(0f, 0f, 0f, 1f)
//                )
//
//                if (currentFrame.camera.trackingState == TrackingState.TRACKING) {
//                    val anchor = currentSession.createAnchor(roadPose)
//
//                    modelLoader.loadModelInstanceAsync("models/road_circle.glb") { model ->
//                        if (model != null) {
//                            val node = ModelNode(model).apply {
//                                scale = Float3(0.1f)
//                                position = Float3(0f, 0f, 0f)
//                            }
//
//                            val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
//                                addChildNode(node)
//                            }
//                            sceneView.addChildNode(anchorNode)
//                            roadCircleNodes.add(anchorNode)
//
//                            // Start blinking animation one by one
//                            lifecycleScope.launch {
//                                delay(step * 120L) // ⏱ Delay increases with step index (sequential)
//                                repeat(3) {
//                                    node.isVisible = false
//                                    delay(200)
//                                    node.isVisible = true
//                                    delay(200)
//                                }
//
//                                // Optional pulse after blinking
//                                val originalScale = node.scale
//                                node.scale = originalScale * 1.2f
//                                delay(100)
//                                node.scale = originalScale
//                            }
//                        } else {
//                            Log.e("ARCoreDebug", "⚠️ Failed to load road circle at step $step")
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("ARCoreDebug", "❌ drawRoadBetween crash at step $step: ${e.localizedMessage}", e)
//                continue
//            }
//        }
//    }

    private suspend fun drawRoadBetween(
        start: SerializableRoutePoint,
        end: SerializableRoutePoint,
        originLat: Double,
        originLon: Double,
        baseY: Float
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
            val pose = currentFrame.camera.pose

            // Skip step if camera isn't tracking
            if (currentFrame.camera.trackingState != TrackingState.TRACKING) {
                if (step == 0) {
                    Toast.makeText(requireContext(), "Waiting for AR tracking...", Toast.LENGTH_SHORT).show()
                    Log.w("ARCoreDebug", "🚫 Road skipped (camera not tracking)")
                }
                continue
            }

            val t = step.toFloat() / steps
            val lat = start.lat + deltaLat * t
            val lon = start.lon + deltaLon * t
            val dLat = lat - originLat
            val dLon = lon - originLon

            val localX = dLon * 111000f * cos(Math.toRadians(originLat)).toFloat()
            val localZ = -dLat * 111000f

            val roadPose = Pose(floatArrayOf(localX.toFloat(), baseY, localZ.toFloat()), floatArrayOf(0f, 0f, 0f, 1f))
            val anchor = try {
                currentSession.createAnchor(roadPose)
            } catch (e: Exception) {
                Log.e("ARCoreDebug", "⚠️ Failed to create road anchor: ${e.message}")
                continue
            }

            modelLoader.loadModelInstanceAsync("models/road_circle.glb") { model ->
                if (model == null) {
                    Log.e("ARCoreDebug", "⚠️ Failed to load road model")
                    return@loadModelInstanceAsync
                }

                val node = ModelNode(model).apply {
                    scale = Float3(0.1f)
                    position = Float3(0f, 0f, 0f)
                }

                val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
                    addChildNode(node)
                }

                sceneView.addChildNode(anchorNode)
                roadCircleNodes.add(anchorNode)

                lifecycleScope.launch {
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

    private suspend fun drawRoadBetweenWithCylinderNode(
        start: SerializableRoutePoint,
        end: SerializableRoutePoint,
        originLat: Double,
        originLon: Double,
        baseY: Float,
        engine: Engine,
        materialLoader: MaterialLoader,
        context: Context
    ) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        val deltaLat = end.lat - start.lat
        val deltaLon = end.lon - start.lon
        val dist = distanceInMeters(start.lat, start.lon, end.lat, end.lon)
        val steps = max(1, (dist / 1.0).toInt())

        for (step in 0 until steps) {
            delay(30)

            try {
                val currentSession = sceneView.session ?: continue
                val currentFrame = sceneView.frame ?: continue
                val pose = currentFrame.camera.pose

                if (pose == null || currentFrame.camera.trackingState != TrackingState.TRACKING) {
                    if (step == 0) {
                        Toast.makeText(context, "Waiting for AR tracking...", Toast.LENGTH_SHORT).show()
                    }
                    Log.w("ARCoreDebug", "⚠️ Skipped drawing road at step $step due to camera not tracking")
                    continue
                }

                val t = step.toFloat() / steps
                val lat = start.lat + deltaLat * t
                val lon = start.lon + deltaLon * t
                val dLat = lat - originLat
                val dLon = lon - originLon

                val localX = dLon * 111000f * cos(Math.toRadians(originLat)).toFloat()
                val localZ = -dLat * 111000f

                // replace the incorrect .all { it.hashCode() } line with:
                if (!localX.isFinite() || !baseY.isFinite() || !localZ.isFinite()) {
                    Log.w("ARCoreDebug", "⚠️ Skipped step $step due to non-finite coordinates")
                    continue
                }

                val roadPose = Pose(
                    floatArrayOf(localX.toFloat(), baseY, localZ.toFloat()),
                    floatArrayOf(0f, 0f, 0f, 1f)
                )

                val anchor = try {
                    currentSession.createAnchor(roadPose)
                } catch (e: Exception) {
                    Log.e("ARCoreDebug", "⚠️ Failed to create road anchor: ${e.message}")
                    continue
                }

                // 🟦 Create a flat blue cylinder as road segment
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
                        rotation = Float3(0f, 0f, 0f)
                    )
                }

                val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
                    addChildNode(cylinderNode)
                }

                sceneView.addChildNode(anchorNode)
                roadCircleNodes.add(anchorNode)

                // Optional: sequential blinking animation (like before)
                lifecycleScope.launch {
                    delay(step * 120L)
                    repeat(3) {
                        cylinderNode.isVisible = false
                        delay(200)
                        cylinderNode.isVisible = true
                        delay(200)
                    }
                }

            } catch (e: Exception) {
                Log.e("ARCoreDebug", "❌ drawRoadBetween crash at step $step: ${e.localizedMessage}", e)
                continue
            }
        }
    }

//    private fun isShowingARNavigationArrowOneByOne(index: Int) {
//        val camera = sceneView.frame
//
//        if (camera?.camera?.trackingState == TrackingState.TRACKING) {
//            Log.e("ARCoreDebug", "$index")
//
//            if (index < instructionPoints.size) {
//                val session = sceneView.session ?: return
//                val frame = sceneView.frame ?: return
//
//                // ✅ Remove previous arrow and road nodes
//                arrowNodes.forEach { sceneView.removeChildNode(it) }
//                arrowNodes.clear()
//
//                roadCircleNodes.forEach { sceneView.removeChildNode(it) }
//                roadCircleNodes.clear()  // <- remove previously drawn road
//                drawnRoadSegments.clear()
//
//                val originLat = yourLocation.lat
//                val originLon = yourLocation.lon
//                val baseY = frame.camera.pose.ty() - 1.5f
//
//                // Calculate all local positions once
//                val localPositions = instructionPoints.map { point ->
//                    val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
//                    val dz = (point.lat - originLat) * 111_000.0
//                    Pair(dx.toFloat(), -dz.toFloat())
//                }
//
//                val (x, z) = localPositions[index]
//                if (!listOf(x, baseY, z).all { it.isFinite() }) {
//                    Log.w("ARCoreDebug", "⚠️ Invalid local position for arrow $index")
//                    return
//                }
//
//                val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
//                val cameraState = frame.camera
//                if (cameraState.trackingState != TrackingState.TRACKING) {
//                    Log.w("ARCoreDebug", "🚫 Cannot create anchor — camera is not tracking!")
//                    return
//                }
//
//                val anchor = session.createAnchor(anchorPose)
//
//                // ✅ Calculate yaw toward next point
//                var yaw = 0f
//                if (index < localPositions.size - 1) {
//                    val (x1, z1) = localPositions[index]
//                    val (x2, z2) = localPositions[index + 1] // FIXED INDEX
//                    val dx = x2 - x1
//                    val dz = z2 - z1
//                    yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
//                }
//
//                // 🧭 Add arrow facing next point
//                addArrowWithYaw(anchor, index, yaw)
//
//                // ✅ Draw path from previous to current
//                if (index >= 0 && index < instructionPoints.size - 1) {
//                    val prev = instructionPoints[index]
//                    val current = instructionPoints[index + 1]
//                    launch {
//                        drawRoadBetween(prev, current, originLat, originLon, baseY)
//                    }
//                } else if (index == instructionPoints.size - 1) {
//                    val point = instructionPoints[index]
//                    launch {
//                        drawRoadBetween(point, point, originLat, originLon, baseY)
//                    }
//                } else {
//                    Log.w("InstructionPath", "❌ Invalid index: $index")
//                }
//
////                updateNextRouteButtonText()
//            } else {
////            Toast.makeText(context, "Start reached your destination!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun isShowingARNavigationArrowOneByOne(index: Int) {
        val session = sceneView.session ?: run {
            Log.w("ARCoreDebug", "❌ Session is null")
            return
        }

        val frame = sceneView.frame ?: run {
            Log.w("ARCoreDebug", "❌ Frame is null")
            return
        }

        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            pendingArrowIndex = index
            Log.w("ARCoreDebug", "🚫 Camera not tracking — arrow $index skipped")
            return
        }

        pendingArrowIndex = null // AR is ready now
        Log.d("ARCoreDebug", "✅ Showing AR arrow for index $index")

        if (index >= instructionPoints.size) {
            Log.w("ARCoreDebug", "❌ Index $index out of bounds for instructionPoints (${instructionPoints.size})")
            return
        }

        // ✅ Remove previous arrow and road nodes
        arrowNodes.forEach { sceneView.removeChildNode(it) }
        arrowNodes.clear()
        roadCircleNodes.forEach { sceneView.removeChildNode(it) }
        roadCircleNodes.clear()
        drawnRoadSegments.clear()

        val originLat = yourLocation.lat
        val originLon = yourLocation.lon
        val baseY = frame.camera.pose.ty() - 1.5f

        // ✅ Calculate local positions from instruction points
        val localPositions = instructionPoints.map { point ->
            val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
            val dz = (point.lat - originLat) * 111_000.0
            Pair(dx.toFloat(), -dz.toFloat())
        }

        val (x, z) = localPositions[index]
        if (!listOf(x, baseY, z).all { it.isFinite() }) {
            Log.w("ARCoreDebug", "⚠️ Invalid position for arrow $index → ($x, $baseY, $z)")
            return
        }

        val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
        val anchor = try {
            session.createAnchor(anchorPose)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "AR not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
            Log.w("ARCoreDebug", "❌ Could not create anchor at index $index: ${e.message}")
            Log.e("ARCoreDebug", "❌ Failed to create anchor for index $index: ${e.message}")
            return
        }

        // ✅ Calculate yaw to next instruction point
        var yaw = 0f
        if (index < localPositions.size - 1) {
            val (x1, z1) = localPositions[index]
            val (x2, z2) = localPositions[index + 1]
            yaw = Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
        }

        addArrowWithYaw(anchor, index, yaw)

        // ✅ Draw connecting path
        if (index in 0 until instructionPoints.size - 1) {
            val prev = instructionPoints[index]
            val next = instructionPoints[index + 1]
            launch {
//                drawRoadBetween(prev, next, originLat, originLon, baseY)
                drawRoadBetweenWithCylinderNode(
                    start = prev,
                    end = next,
                    originLat = originLat,
                    originLon = originLon,
                    baseY = baseY,
                    engine = sceneView.engine,
                    materialLoader = sceneView.materialLoader,
                    context = ctx
                )
            }
        } else if (index == instructionPoints.size - 1) {
            val point = instructionPoints[index]
            launch {
//                drawRoadBetween(point, point, originLat, originLon, baseY)
                drawRoadBetweenWithCylinderNode(
                    start = point,
                    end = point,
                    originLat = originLat,
                    originLon = originLon,
                    baseY = baseY,
                    engine = sceneView.engine,
                    materialLoader = sceneView.materialLoader,
                    context = ctx
                )
            }
        }
    }

    private fun placeArrowsUsingWorldAnchor() {
        Log.d("ARCoreDebug", "📍 Placing arrows facing next point")

        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        val originLat = yourLocation.lat
        val originLon = yourLocation.lon
        val baseY = frame.camera.pose.ty() - 1.5f

        lifecycleScope.launch {
            val localPositions = mutableListOf<Pair<Float, Float>>()  // (X, Z)

            // 1️⃣ Convert GPS to local space
            for (point in instructionPoints) {
                val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
                val dz = (point.lat - originLat) * 111_000.0

                localPositions.add(Pair(dx.toFloat(), -dz.toFloat()))
            }

            // 2️⃣ Place arrows with rotation toward next point
            for (i in 0 until localPositions.size) {
                val (x, z) = localPositions[i]

                if (!listOf(x, baseY, z).all { it.isFinite() }) {
                    Log.w("ARCoreDebug", "⚠️ Skipping arrow $i due to invalid coordinates")
                    continue
                }

                val anchorPose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
                val anchor = session.createAnchor(anchorPose)

                // ➡️ Calculate yaw angle (rotation)
                var yaw = 0f
                if (i < localPositions.size - 1) {
                    val (x1, z1) = localPositions[i]
                    val (x2, z2) = localPositions[i + 1]
                    val dx = x2 - x1
                    val dz = z2 - z1
                    yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
                }

                addArrowWithYaw(anchor, i, yaw)
                delay(100)
            }

            if (instructionIndex >= 0 && instructionIndex < instructionPoints.size - 1) {
                val prev = instructionPoints[instructionIndex]
                val current = instructionPoints[instructionIndex + 1]
                launch {
                    drawRoadBetween(prev, current, originLat, originLon, baseY)
                }
            } else if (instructionIndex == instructionPoints.size - 1) {
                val point = instructionPoints[instructionIndex]
                launch {
                    drawRoadBetween(point, point, originLat, originLon, baseY)
                }
            } else {
                Log.w("InstructionPath", "❌ Invalid index: $instructionIndex")
            }
        }
    }

    private fun updateNextRouteButtonText() {
        nextRouteBtn.text = if (instructionIndex <= 0) "Show Route 📍" else "Next Route 📍"
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

//    companion object {
//        fun newInstance(
//            yourLocation: SerializableRoutePoint,
//            destination: SerializableRoutePoint,
//            instructionIndex: Int,
//            instructionList: ArrayList<SerializableRouteInstruction>,
//            instructionPoints: ArrayList<SerializableRoutePoint?>
//        ): FourthLocalARFragment {
//            return FourthLocalARFragment().apply {
//                arguments = Bundle().apply {
//                    putInt("instructionIndex", instructionIndex)
//                    putSerializable("yourLocation", yourLocation)
//                    putSerializable("destination", destination)
//                    putSerializable("instructionList", ArrayList(instructionList)) // 👈 Convert to ArrayList
//                    putSerializable("instructionPoints", ArrayList(instructionPoints)) // 👈 Convert to ArrayList
//                }
//            }
//        }
//    }

}
