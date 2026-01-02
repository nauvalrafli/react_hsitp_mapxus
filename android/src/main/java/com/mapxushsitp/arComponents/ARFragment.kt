package com.mapxushsitp.arComponents

import android.Manifest
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mapxushsitp.compassComponents.CompassViewModel
import com.mapxushsitp.data.model.SerializableRouteInstruction
import com.mapxushsitp.data.model.SerializableRoutePoint
import com.mapxushsitp.viewmodel.MapxusSharedViewModel
import com.mapxushsitp.R
import com.google.android.filament.Engine
import com.google.android.filament.utils.Float2
import com.google.android.filament.utils.transform
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
import io.github.sceneview.math.Position
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.SphereNode
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
import io.github.sceneview.node.ShapeNode
import io.github.sceneview.node.LightNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.lang.Math.pow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class FourthLocalARFragment : androidx.fragment.app.Fragment(), CoroutineScope by MainScope() {

    private lateinit var sceneView: ARSceneView
    private lateinit var modelLoader: ModelLoader
    //    private lateinit var textToSpeech: TextToSpeech
    private val mapxusMapViewModel: MapxusSharedViewModel by activityViewModels<MapxusSharedViewModel>()
    private val arNavigationViewModel: ARNavigationViewModel by activityViewModels<ARNavigationViewModel>()
    private val compassViewModel: CompassViewModel by activityViewModels<CompassViewModel>()
    private var instructionIndex = 0
    private var secondInstructionIndex = mutableIntStateOf(0)
    private lateinit var destination: SerializableRoutePoint
    private lateinit var yourLocation: SerializableRoutePoint
    private val instructionList = mutableListOf<SerializableRouteInstruction>()
    private val instructionPoints = mutableListOf<SerializableRoutePoint>()

    private val arrowNodes = mutableListOf<AnchorNode>()
    private val roadCircleNodes = mutableListOf<AnchorNode>()
    private val drawnRoadSegments = mutableSetOf<Pair<Int, Int>>()
    private val junctionNodes = mutableListOf<AnchorNode>()

    private lateinit var trackingStatusTextView: TextView
    private lateinit var compassDegreesStatusTextView: TextView
    private lateinit var directionStatusTextView: TextView
//    private lateinit var degreeToAchieveTextView: TextView
    private lateinit var nextRouteBtn: Button

    private var hasShownFirstArrow: Boolean = false
    private var pendingArrowIndex: Int? = null
    private var lastTrackingState: TrackingState? = null

    private lateinit var ctx: Context

    // For Compass
    private var isCalibrated = false
    private val offsetSamples = mutableListOf<Double>()
    private var arWorldRotationOffset = 0f
    private var isShowingDirectionDegree = "" // Update your UI with this string
    private var isDrawingStarted = false // Prevent duplicate drawing
    private var arDrawingJob: Job? = null
    private var navigationDrawingJob: Job? = null
    private var isCurrentlyDrawing = false
    private var monitorJob: Job? = null
    private var currentlyRenderedFloorId: String? = null // Track the floor currently being rendered
    private var lastRenderedFloorId: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
//            startARSetup()
            startARSetupWithAllArrowNavigationAtOnce()
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
//                startARSetup()
                startARSetupWithAllArrowNavigationAtOnce()
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

        frameLayout.addView(sceneView)

        Log.d("ARCoreDebug", "📦 trackingStatusTextView added to layout")

        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("HideARContainer", "Back button pressed - Closing AR")
                val dialog = AlertDialog.Builder(ctx)
                    .setTitle("Quit Navigation?")
                    .setMessage("Do you really want to quit ongoing navigation?")
                    .setPositiveButton("Yes"
                    ) { p0, p1 -> cleanupAndExit(isManualExit = true) }
                    .setNegativeButton("No") { p0, p1 -> }
                    .setOnDismissListener {

                    }
                    .create()
                dialog.show()
            }
        }
        // viewLifecycleOwner ensures the callback is removed when the fragment is destroyed
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        requestMissingPermissions()
    }

    private fun fullCleanup() {
        // Only attempt to hide arrows if the view is still "alive" enough
        if (view != null && isAdded) {
            hideAllArrowsGemini()
        } else {
            // If view is gone, just clear the local list references
            roadCircleNodes.clear()
        }

        isDrawingStarted = false
        isCalibrated = false
        offsetSamples.clear()
        arWorldRotationOffset = 0f

        navigationDrawingJob?.cancel()
        arDrawingJob?.cancel()
    }

    private fun cleanupAndExit(isManualExit: Boolean = false) {
        Log.d("HideARContainer", "Cleanup initiated. Manual Exit: $isManualExit")

        // 1. PERFORM FULL INTERNAL CLEANUP (Data and Nodes)
        fullCleanup()

        // 2. Capture references safely
        val hostActivity = activity ?: return
        val fm = hostActivity.supportFragmentManager

        // 3. RELEASE CAMERA HARDWARE (Do this while scope is still active)
        sceneView.session?.let { session ->
            try {
                session.pause()
                session.close()
                Log.d("HideARContainer", "ARCore Session closed")
            } catch (e: Exception) {
                Log.e("HideARContainer", "Session close error: ${e.message}")
            }
        }

        // 4. RESET UI STATE
        // We update the ViewModel flag BEFORE cancelling the scope
        arNavigationViewModel.isShowingAndClosingARNavigation.value = false

        // 5. IDENTIFY AND REMOVE FRAGMENT
        if (isManualExit) {
            // Find by ID as we discussed earlier for consistency
            val fragment = fm.findFragmentById(R.id.ar_fragment_container)
            if (fragment != null && !fm.isStateSaved) {
                try {
                    fm.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss() // Using commit instead of commitNow for smoother lifecycle
                    Log.d("HideARContainer", "Fragment removed from Manager")
                } catch (e: Exception) {
                    Log.w("HideARContainer", "Fragment removal error: ${e.message}")
                }
            }
            fm.popBackStack()
        }

        // 6. RESET UI (Map resizing)
        resetMapToFullScreen(hostActivity)

        // 7. STOP SENSORS AND LOOPS (LAST STEP)
        // We cancel the scope at the very end so all previous lines finish executing
        if (this.isActive) {
            this.cancel()
        }
    }

    private fun resetMapToFullScreen(hostActivity: FragmentActivity) {
        // 1. Guard against a dying activity
        if (hostActivity.isFinishing || hostActivity.isDestroyed) return

        hostActivity.runOnUiThread {
            // 2. Safely find views
            val mapView = hostActivity.findViewById<org.maplibre.android.maps.MapView>(R.id.mapView)
            val container = hostActivity.findViewById<View>(R.id.ar_fragment_container)

            // 3. Update Container Visibility
            container?.visibility = View.GONE

            // 4. Update Map Layout Params using 'apply' for readability
            mapView?.apply {
                (layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                    params.height = 0
                    params.weight = 1f
                    layoutParams = params
                    requestLayout()
                }
            }

            // 5. Signal the end of navigation to the Map SDK
            // We wrap this in a try-catch because some SDKs throw errors
            // if endNavigation is called twice or after the map is destroyed.
            try {
                mapxusMapViewModel.endNavigation()
                Log.d("HideARContainer", "Mapxus navigation ended successfully")
            } catch (e: Exception) {
                Log.e("HideARContainer", "Error ending Mapxus navigation: ${e.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sceneView.session?.pause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            // User toggled AR off: Pause camera to save battery, but KEEP anchors
            sceneView.session?.pause()
        } else {
            // User toggled AR on: Resume exactly where we left off
            sceneView.session?.resume()

            // Re-trigger the monitoring loop if it was cancelled
            startARSetupWithAllArrowNavigationAtOnce()
        }
    }

    override fun onDestroyView() {
        try {
            // Pass false so we don't trigger manual fragment transactions
            // while the system is already destroying the view
//            cleanupAndExit(isManualExit = false)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "Cleanup failed during onDestroyView: ${e.message}")
        }
        super.onDestroyView()
    }

    // Start AR with Showing the Arrow one by one
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

        // Showing AR Navigation one by one
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

    private fun startARSetupWithAllArrowNavigationAtOnce() {
        // 1. Load arguments FIRST
        loadArguments()   // <-- wrap your argument loading into a function

        // 2. Start AR session
        resumeARSession()

        setupUIOverlays()

        // 3. Only draw after tracking AND after arguments are valid
        launch {
            waitForTracking()
            waitForInstructionPoints()
            waitUntilTrackingThenShowArrows()
        }
    }

    private suspend fun waitForTracking() {
        while (sceneView.frame?.camera?.trackingState != TrackingState.TRACKING) {
            Log.d("ARCoreDebug", "⏳ Waiting for AR tracking...")
            delay(100)
        }
    }

    private suspend fun waitForInstructionPoints() {
        var attempts = 0
        var lastSize = -1

        // We expect the points to match the number of instructions
        // or at least be greater than 1
        while (coroutineContext.isActive) {
            val currentSize = instructionPoints.size

            // 1. Success Condition: We have points and the list stopped growing
            if (currentSize >= 2 && currentSize == lastSize) {
                Log.d("ARCoreDebugInstructionPoints", "✅ All $currentSize points loaded successfully.")
                return
            }

            // 2. Timeout Condition: 5 seconds total wait
            if (attempts > 50) {
                if (currentSize >= 2) {
                    Log.w("ARCoreDebugInstructionPoints", "⚠️ Timeout reached, but proceeding with $currentSize points.")
                    return
                } else {
                    Log.e("ARCoreDebugInstructionPoints", "❌ Timeout: Not enough points to start AR.")
                    return
                }
            }

            lastSize = currentSize
            delay(100)
            attempts++
        }
    }

    private fun setupUIOverlays() {
        val cameraStatusView = isShowingCameraStatus()
        (sceneView.parent as? ViewGroup)?.addView(cameraStatusView)

        // Compass Degrees Status
        val compassStatusView = isShowingCompassDegreeStatus()
        (sceneView.parent as? ViewGroup)?.addView(compassStatusView)

        val directionText = isShowingDirectionStatus()
        (sceneView.parent as? ViewGroup)?.addView(directionText)
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

    private suspend fun waitUntilTrackingThenShowArrows() {
        Log.d("ARCoreDebug", "🔍 Waiting for stable tracking before showing arrows...")
        Log.d("ARCoreDebug", "🎯 Initial tracking acquired — placing arrows")

        // Showing AR Navigation all at once initially
//        isShowingARNavigationArrowAllAtOnce() // for backup don't delete this

        arNavigationViewModel.instructionIndex.observe(viewLifecycleOwner) { index ->
            isShowingTheARNavigationAllAtOnceGeminiBasedOnCompassDegreesAndFloorId(
                index = index,
                instructionPoints = instructionPoints,
                compassClassDegrees = compassViewModel.deviceHeadingInDegrees
            )
        }

//        // Showing All AR Navigation at once based on Compass
//        isShowingTheARNavigationAllAtOnceGeminiBasedOnCompassDegrees(
//            instructionPoints = instructionPoints,
//            compassClassDegrees = compassViewModel.deviceHeadingInDegrees
//        )

        // Now continue monitoring tracking changes forever
        monitorTrackingAndToggleArrowsWithoutClosingTheAR()
    }

    private fun isShowingDirectionStatus(): View {
        val container = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        directionStatusTextView = TextView(requireContext()).apply {
            text = isShowingDirectionDegree
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setShadowLayer(
                16f, 3f, 3f,
                Color.White.hashCode() // semi-transparent yellow
            )

            // Define the background shape once
            val darkShape = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(180, 0, 0, 0)) // Semi-transparent black
                cornerRadius = 0f // Rounded corners look better
            }
            // Apply background only if there is text initially
            background = if (isShowingDirectionDegree.isNotEmpty()) darkShape else null

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER // Kept your Gravity.CENTER
            ).apply {
                setMargins(40, 0, 40, 40)
            }
        }

        container.addView(directionStatusTextView)
        return container
    }

    private fun isShowingCompassDegreeStatus(): View {
        // Use a FrameLayout that covers the screen to act as a container
        val container = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        compassDegreesStatusTextView = TextView(requireContext()).apply {
            text = "${compassViewModel.deviceHeadingInDegrees.toInt()}°"
            setTextColor(android.graphics.Color.WHITE)
            // Semi-transparent black background
            setBackgroundColor(android.graphics.Color.argb(150, 0, 0, 0))
            textSize = 20f
            gravity = Gravity.CENTER // Centers the text inside its own box
            setPadding(30, 15, 30, 15)
            setShadowLayer(
                16f, 3f, 3f,
                Color.White.hashCode() // semi-transparent yellow
            )

            // Setup the position to Bottom-Right
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END // This pushes it to the bottom right
            ).apply {
                // Add margins so it doesn't touch the screen edges
                setMargins(0, 0, 40, 40)
            }
        }

        container.addView(compassDegreesStatusTextView)
        return container
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

    private suspend fun monitorTrackingAndToggleArrows() {
        var arrowsCurrentlyVisible = true // Tracks the actual UI state
        var trackingLostTimestamp = 0L // To track how long tracking has been gone

        while (true) {
            // EXIT CHECK: If the ViewModel says we are closing, stop the loop immediately
            if (!arNavigationViewModel.isShowingAndClosingARNavigation.value) {
                Log.d("ARCoreDebug", "Loop termination: Navigation flag is false")
                break
            }

            val trackingState = sceneView.frame?.camera?.trackingState

            // 1. Log the status first as requested
            logCameraTrackingStatus()

            withContext(Dispatchers.Main) {
                // 2. Handle UI Overlays and Navigation Logic
                if (::trackingStatusTextView.isInitialized && ::compassDegreesStatusTextView.isInitialized) {
                    val statusContainer = trackingStatusTextView.parent as? View

                    when (trackingState) {
                        TrackingState.TRACKING -> {
                            trackingLostTimestamp = 0L // Reset timer
                            statusContainer?.visibility = View.GONE
                            compassDegreesStatusTextView.visibility = View.VISIBLE
                            directionStatusTextView.visibility = if (isShowingDirectionDegree.isEmpty()) View.GONE else View.VISIBLE

                            // If not drawing, start the calibration/drawing process
                            if (!isDrawingStarted) {
//                                isShowingTheARNavigationOnceAtAllGeminiBasedOnCompassDegrees(
//                                    instructionPoints = instructionPoints,
//                                    compassClassDegrees = compassViewModel.deviceHeadingInDegrees
//                                )
                            }
                            // 1. If we were hidden, show them again (Quick resume)
                            if (!arrowsCurrentlyVisible) {
                                setARNodesVisibility(true)
                                arrowsCurrentlyVisible = true
                                Log.d("ARCoreDebug", "Visibility Restored")
                            }
                        }

                        TrackingState.PAUSED, TrackingState.STOPPED -> {
                            // 3. Just hide them. Do NOT reset isDrawingStarted or isCalibrated
                            if (arrowsCurrentlyVisible) {
                                setARNodesVisibility(false)
                                arrowsCurrentlyVisible = false
                                Log.d("ARCoreDebug", "Visibility Hidden - Waiting for tracking...")
                            }

                            // 🕒 DELAY LOGIC: Only show the container after tracking has been lost for a moment
                            if (trackingLostTimestamp == 0L) {
                                trackingLostTimestamp = System.currentTimeMillis()
                            }

                            // Appear only after ~1 second of continuous lost tracking
                            if (System.currentTimeMillis() - trackingLostTimestamp > 1000) {
                                statusContainer?.visibility = View.VISIBLE
                            }
                        }
                        else -> {}
                    }

                    compassDegreesStatusTextView.text = "${compassViewModel.deviceHeadingInDegrees.toInt()}°"
                }

                // 3. Update Instruction Text (Calibration Status)
                if (::directionStatusTextView.isInitialized) {
                    directionStatusTextView.text = isShowingDirectionDegree

                    if (isShowingDirectionDegree.isEmpty()) {
                        directionStatusTextView.background = null
                        directionStatusTextView.visibility = View.GONE
                    } else {
                        // Re-apply the dark background when text appears
                        val darkShape = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.argb(180, 0, 0, 0))
                            cornerRadius = 0f
                        }
                        directionStatusTextView.background = darkShape
                        directionStatusTextView.visibility = View.VISIBLE
                    }
                }
            }

            delay(250)
        }
    }

    private suspend fun monitorTrackingAndToggleArrowsWithoutClosingTheAR() {
        var arrowsCurrentlyVisible = true
        var trackingLostTimestamp = 0L

        // 1. Change 'while(true)' to check if the coroutine is still valid
        while (coroutineContext.isActive) {

            // 2. GET THE FLAG BUT DON'T BREAK THE LOOP
            val isUserViewingAR = arNavigationViewModel.isShowingAndClosingARNavigation.value ?: false
            val session = sceneView.session
            val frame = sceneView.frame

            // If the session is paused or frame is null, just wait and try again
            if (frame == null || session == null) {
                delay(500)
                continue
            }
            val trackingState = sceneView.frame?.camera?.trackingState

            logCameraTrackingStatus()

            withContext(Dispatchers.Main) {
                // Safety check for Fragment lifecycle
                if (!isAdded || isDetached) return@withContext

                if (!isUserViewingAR) {
                    val statusContainer = trackingStatusTextView.parent as? View
                    // 3. SUSPEND MODE: Hide everything but stay in the loop
                    if (arrowsCurrentlyVisible) {
                        setARNodesVisibility(false)
                        arrowsCurrentlyVisible = false

                        statusContainer?.visibility = View.GONE

                        // Hide UI text views as well so they don't float over the map
                        if (::trackingStatusTextView.isInitialized) {
                            (trackingStatusTextView.parent as? View)?.visibility = View.GONE
                            compassDegreesStatusTextView.visibility = View.GONE
                            directionStatusTextView.visibility = View.GONE
                        }
                        Log.d("ARCoreDebug", "AR Backgrounded: Nodes hidden, session keeping state.")
                    }
                } else {
                    // 4. ACTIVE MODE: Handle UI and Tracking
                    if (::trackingStatusTextView.isInitialized && ::compassDegreesStatusTextView.isInitialized) {
                        val statusContainer = trackingStatusTextView.parent as? View

                        when (trackingState) {
                            TrackingState.TRACKING -> {
                                trackingLostTimestamp = 0L
                                statusContainer?.visibility = View.GONE
                                compassDegreesStatusTextView.visibility = View.VISIBLE

                                // Only triggers the VERY first time (or after a fullCleanup)
                                if (!isDrawingStarted) {
//                                    isShowingTheARNavigationOnceAtAllGeminiBasedOnCompassDegrees(
//                                        instructionPoints = instructionPoints,
//                                        compassClassDegrees = compassViewModel.deviceHeadingInDegrees
//                                    )

                                    arNavigationViewModel.instructionIndex.observe(viewLifecycleOwner) { index ->
                                        Log.d("ARFloorLogic", "Instruction index 1: ${index}")
                                        isShowingTheARNavigationAllAtOnceGeminiBasedOnCompassDegreesAndFloorId(
                                            index = index,
                                            instructionPoints = instructionPoints,
                                            compassClassDegrees = compassViewModel.deviceHeadingInDegrees
                                        )
                                    }
                                }

                                // RESUME VIEWING: Show nodes again without recalibrating
                                if (!arrowsCurrentlyVisible) {
                                    setARNodesVisibility(true)
                                    arrowsCurrentlyVisible = true
                                    Log.d("ARCoreDebug", "AR Resumed: Nodes visible again.")
                                }
                            }

                            TrackingState.PAUSED, TrackingState.STOPPED -> {
                                if (arrowsCurrentlyVisible) {
                                    setARNodesVisibility(false)
                                    arrowsCurrentlyVisible = false
                                }
                                if (trackingLostTimestamp == 0L) trackingLostTimestamp = System.currentTimeMillis()
                                if (System.currentTimeMillis() - trackingLostTimestamp > 1000) {
                                    statusContainer?.visibility = View.VISIBLE
                                }
                            }
                            else -> {}
                        }
                        compassDegreesStatusTextView.text = "${compassViewModel.deviceHeadingInDegrees.toInt()}°"
                    }

                    // Handle Direction Text Visibility
                    if (::directionStatusTextView.isInitialized) {
                        if (isShowingDirectionDegree.isNotEmpty()) {
                            // Re-apply the dark background when text appears
                            val darkShape = android.graphics.drawable.GradientDrawable().apply {
                                setColor(android.graphics.Color.argb(180, 0, 0, 0))
                                cornerRadius = 0f
                            }
                            directionStatusTextView.background = darkShape
                            directionStatusTextView.text = isShowingDirectionDegree
                            directionStatusTextView.visibility = View.VISIBLE
                        } else {
                            directionStatusTextView.background = null
                            directionStatusTextView.visibility = View.GONE
                        }
                    }
                }
            }
            delay(250)
        }
    }

    private fun logCameraTrackingStatus() {
        // 🛡️ Add this check at the very beginning of the function
        if (!::trackingStatusTextView.isInitialized) {
            Log.w("ARCoreDebug", "⚠️ UI not initialized yet, skipping log update")
            return
        }

        // 2. 🛡️ Guard against empty navigation points (CRITICAL FIX)
        if (instructionPoints.size < 2) {
            Log.w("ARCoreDebug", "⚠️ Waiting for navigation data... (Current size: ${instructionPoints.size})")
            trackingStatusTextView.text = "Loading navigation path..."
            return
        }

        val frame = sceneView.frame
        val camera = frame?.camera

        val targetBearing = normalizedBearingDegrees(instructionPoints[0], instructionPoints[1]).toFloat()
        val compassTrackingStatus = directionStatusTextView.parent as? View

        if (camera == null) {
            Log.w("ARCoreDebug", "⚠️ Camera not ready yet")
            trackingStatusTextView.apply {
                text = "Please move your Android phone slowly!"
                visibility = View.VISIBLE
            }
            return
        }

        when (camera.trackingState) {
            TrackingState.TRACKING -> {
                launch {
                    Log.d("ARCoreDebug", "✅ Camera tracking normally")
                    trackingStatusTextView.apply {
                        text = "You are good to go! 👍 ${targetBearing}"
                        visibility = View.VISIBLE
                    }
                    delay(1500)
                    // Only hide if the tracking state is still TRACKING
                    if (camera.trackingState == TrackingState.TRACKING) {
                        trackingStatusTextView.visibility = View.GONE
                        compassTrackingStatus?.visibility = View.VISIBLE
                    }
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
                compassTrackingStatus?.visibility = View.GONE
            }
            TrackingState.STOPPED -> {
                Log.e("ARCoreDebug", "🛑 Tracking stopped")
                trackingStatusTextView.apply {
                    text = "❌ Tracking Stopped ➾ Please try again."
                    visibility = View.VISIBLE
                }
                compassTrackingStatus?.visibility = View.GONE
            }
            else -> {
                trackingStatusTextView.apply {
                    text = "Unknown tracking state"
                    visibility = View.VISIBLE
                }
                compassTrackingStatus?.visibility = View.GONE
            }
        }
    }

    /**
     * Helper to toggle visibility of all navigation nodes
     */
    private fun setARNodesVisibility(visible: Boolean) {
        // Use a copy or check for empty to avoid ConcurrentModificationException
        roadCircleNodes.toList().forEach { node ->
            try {
                node.isVisible = visible
            } catch (e: Exception) {
                Log.e("ARCoreDebug", "Node visibility error: ${e.message}")
            }
        }
    }

    private fun loadArguments() {
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
//                        if (currentInstructionDistance != null && currentInstruction != "Arrive at destination") {
//                            textToSpeech.speak(
//                                "$currentInstruction, and follow the path for ${if (currentInstructionDistance > 1) "meters" else "meter"}",
//                                TextToSpeech.QUEUE_FLUSH,
//                                null,
//                                null
//                            )
//                        } else if (currentInstruction == "Arrive at destination") {
//                            textToSpeech.speak(
//                                "Kudos! You have arrived at the destination.",
//                                TextToSpeech.QUEUE_FLUSH,
//                                null,
//                                null
//                            )
//                        }
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
                position = Float3(0f, 1.5f, 0f)

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
//        val steps = max(1, (dist / 1.0).toInt())
        val spacing = 0.4   // 30 cm per cylinder
        val steps = max(1, (dist / spacing).toInt())

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

    private fun isShowingARNavigationArrowAllAtOnce() {
        val session = sceneView.session ?: run {
            Log.w("ARCoreDebug", "❌ Session null")
            return
        }

        val frame = sceneView.frame ?: run {
            Log.w("ARCoreDebug", "❌ Frame null")
            return
        }

        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            Log.w("ARCoreDebug", "🚫 Camera not tracking — cannot draw")
            return
        }

        // Clear previous AR content once
        arrowNodes.forEach { sceneView.removeChildNode(it) }
        arrowNodes.clear()

        roadCircleNodes.forEach { sceneView.removeChildNode(it) }
        roadCircleNodes.clear()

        drawnRoadSegments.clear()

        val originLat = yourLocation.lat
        val originLon = yourLocation.lon
        val baseY = frame.camera.pose.ty() - 1.5f

        // Pre-compute all local positions
        val localPositions = instructionPoints.map { point ->
            val dx = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
            val dz = (point.lat - originLat) * 111_000.0
            Pair(dx.toFloat(), -dz.toFloat())
        }

        // 🔥 Launch all work together (arrows + road)
        lifecycleScope.launch {
            // Draw ALL arrows first
            instructionPoints.indices.forEach { i ->
                val (x, z) = localPositions[i]

                if (!listOf(x, baseY, z).all { it.isFinite() }) {
                    Log.w("ARCoreDebug", "⚠️ Invalid coordinate for arrow $i")
                    return@forEach
                }

                val pose = Pose(
                    floatArrayOf(x, baseY, z),
                    floatArrayOf(0f, 0f, 0f, 1f)
                )

                val anchor = try {
                    session.createAnchor(pose)
                } catch (e: Exception) {
                    Log.e("ARCoreDebug", "❌ Failed to create anchor: ${e.message}")
                    return@forEach
                }

                // Compute yaw toward next arrow
                val yaw = if (i < localPositions.size - 1) {
                    val (x1, z1) = localPositions[i]
                    val (x2, z2) = localPositions[i + 1]
                    Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
                } else 0f

                addArrowWithYaw(anchor, i, yaw)
            }

            // Draw ALL road segments
            // For backup from me - don't delete this below code!
            instructionPoints.indices.forEach { i ->
                if (i >= instructionPoints.size - 1) return@forEach

                val start = instructionPoints[i]
                val end   = instructionPoints[i + 1]

                // Run cylinder drawing without delay
//                drawRoadBetweenWithCylinderNode(
//                    start = start,
//                    end = end,
//                    originLat = originLat,
//                    originLon = originLon,
//                    baseY = baseY,
//                    engine = sceneView.engine,
//                    materialLoader = sceneView.materialLoader,
//                    context = ctx
//                )

                isShowingARNavigationRectanglePathAllAtOnceGemini(
                    start = start,
                    end = end,
                    originLat = originLat,
                    originLon = originLon,
                    baseY = baseY,
                    engine = sceneView.engine,
                    materialLoader = sceneView.materialLoader,
                    context = ctx
                )
            }

            Log.d("ARCoreDebug", "🎉 All arrows + road loaded instantly!")
        }
    }

    private suspend fun isShowingARNavigationRectanglePathAllAtOnce(
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

        // 1. Calculate the total distance of this segment
        val dist = distanceInMeters(start.lat, start.lon, end.lat, end.lon).toFloat()
        if (dist < 0.1f) return // Too short to draw

        // 2. Precompute world-local positions
        val startX = (start.lon - originLon) * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val startZ = -(start.lat - originLat) * 111000f

        val endX = (end.lon - originLon) * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val endZ = -(end.lat - originLat) * 111000f

        // Calculate midpoint for the anchor (placing it in the middle of the segment)
        val midX = (startX + endX) / 2f
        val midZ = (startZ + endZ) / 2f

        // 3. Compute rotation (Yaw) to align the rectangle with the direction of travel
        val yawDegrees = Math.toDegrees(
            atan2((endX - startX).toDouble(), (endZ - startZ).toDouble())
        ).toFloat()

        val materialInstance = materialLoader.createColorInstance(
            color = Color(0f, 0.47f, 0.83f, 1.0f)
        ).apply {
            // If your material loader supports it, set culling to None
            // Otherwise, ensure your rotation is exactly 90f or -90f
            setParameter("roughness", 0.8f)
            setParameter("metallic", 0.0f)
        }

        // 4. Create the Geometry
        // We define a rectangle where the height (Y-axis in 2D) is the total distance
        val roadWidth = 1.2f
        val polygonPath = listOf(
            dev.romainguy.kotlin.math.Float2(-roadWidth / 2, -dist / 2),
            dev.romainguy.kotlin.math.Float2(roadWidth / 2, -dist / 2),
            dev.romainguy.kotlin.math.Float2(roadWidth / 2, dist / 2),
            dev.romainguy.kotlin.math.Float2(-roadWidth / 2, dist / 2)
        )

        // 5. Create Anchor and Node
        val roadPose = Pose(
            floatArrayOf(midX.toFloat(), baseY, midZ.toFloat()),
            floatArrayOf(0f, 0f, 0f, 1f)
        )

        val anchor = try {
            session.createAnchor(roadPose)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "Anchor creation failed: ${e.message}")
            return
        }

        val shapeNode = ShapeNode(
            engine = engine,
            polygonPath = polygonPath,
            materialInstance = materialLoader.createColorInstance(
                color = Color(0f, 0.47f, 0.83f, 1.0f),
                metallic = 0.0f,
                roughness = 0.8f
            )
        ).apply {
            transform(
                position = Float3(0f, 0.01f, 0f), // Lift slightly (1cm) to prevent floor flickering
                // 90f on X lays it flat on the XZ plane (the ground)
                // yawDegrees on Y points it toward the destination
                rotation = Float3(90f, yawDegrees, 0f)
            )
        }

        val anchorNode = AnchorNode(engine, anchor).apply {
            addChildNode(shapeNode)
        }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)
    }

    private suspend fun isShowingARNavigationRectanglePathAllAtOnceGemini(
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

        val spacing = 0.50 // 50 cm segments for a smoother road look
        val steps = max(1, (dist / spacing).toInt())

        // 1. Calculate rotation (Yaw) so the rectangle points toward the destination
        val startX = (start.lon - originLon) * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val startZ = -(start.lat - originLat) * 111000f
        val endX = (end.lon - originLon) * 111000f * cos(Math.toRadians(originLat)).toFloat()
        val endZ = -(end.lat - originLat) * 111000f

        val yawDegrees = Math.toDegrees(
            atan2((endX - startX).toDouble(), (endZ - startZ).toDouble())
        ).toFloat()

        for (step in 0 until steps) {
            delay(20) // Slightly faster delay for smoother appearance

            try {
                val currentSession = sceneView.session ?: continue
                val currentFrame = sceneView.frame ?: continue

                if (currentFrame.camera.trackingState != TrackingState.TRACKING) continue

                val t = step.toFloat() / steps
                val lat = start.lat + deltaLat * t
                val lon = start.lon + deltaLon * t

                val localX = (lon - originLon) * 111000f * cos(Math.toRadians(originLat)).toFloat()
                val localZ = -(lat - originLat) * 111000f

                if (!localX.isFinite() || !localZ.isFinite()) continue

                val roadPose = Pose(
                    floatArrayOf(localX.toFloat(), baseY, localZ.toFloat()),
                    floatArrayOf(0f, 0f, 0f, 1f)
                )

                val anchor = currentSession.createAnchor(roadPose) ?: continue

                // 🟦 Use CubeNode instead of CylinderNode for a rectangular road segment
                val roadSegmentNode = CubeNode(
                    engine = engine,
                    // size = Float3(Width, Thickness, Length)
                    size = Float3(0.6f, 0.01f, 0.55f),
                    materialInstance = materialLoader.createColorInstance(
                        color = Color(0f, 0.47f, 0.83f, 1.0f), // Professional Blue
                        metallic = 0.0f,
                        roughness = 0.8f
                    )
                ).apply {
                    // Apply the calculated rotation to align with the path
                    rotation = Float3(0f, yawDegrees, 0f)
                }

                val anchorNode = AnchorNode(engine, anchor).apply {
                    addChildNode(roadSegmentNode)
                }

                sceneView.addChildNode(anchorNode)
                roadCircleNodes.add(anchorNode)

            } catch (e: Exception) {
                Log.e("ARCoreDebug", "❌ Error at step $step: ${e.message}")
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

    private fun addJunctionPlate(
        position: Float3,
        roadWidth: Float,
        thickness: Float,
        color: Color,
        engine: Engine,
        materialLoader: MaterialLoader
    ) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        // ✅ MUST be tracking
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            Log.w("ARCoreDebug", "🚫 Skipping junction plate — camera not tracking")
            return
        }

        // ✅ Safety: validate coordinates
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) {
            Log.w("ARCoreDebug", "🚫 Invalid junction position: $position")
            return
        }

        val radius = roadWidth / 2f
        val segments = 16

        val circlePath = (0 until segments).map {
            val a = 2.0 * Math.PI * it / segments
            dev.romainguy.kotlin.math.Float2(
                (cos(a) * radius).toFloat(),
                (sin(a) * radius).toFloat()
            )
        }

        val shapeNode = ShapeNode(
            engine = engine,
            polygonPath = circlePath,
            materialInstance = materialLoader.createColorInstance(
                color,
                metallic = 0.1f,
                roughness = 0.6f
            )
        ).apply {
            transform(
                position = Float3(0f, 0.002f, 0f), // ⬅️ slightly lifted
                rotation = Float3(0f, 0f, 0f)
            )
        }

        val pose = Pose(
            floatArrayOf(
                position.x,
                max(position.y, frame.camera.pose.ty() - 1.6f), // ⬅️ safe Y
                position.z
            ),
            floatArrayOf(0f, 0f, 0f, 1f)
        )

        val anchor = try {
            session.createAnchor(pose)
        } catch (e: Exception) {
            Log.e("ARCoreDebug", "❌ Failed to create junction anchor: ${e.message}")
            return
        }

        val anchorNode = AnchorNode(engine, anchor).apply {
            addChildNode(shapeNode)
        }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)
    }

    private fun addStraightRoad(
        start: Float3,
        end: Float3,
        width: Float,
        thickness: Float, // Included for logic, though ShapeNode is flat
        color: Color,
        engine: Engine,
        materialLoader: MaterialLoader
    ) {
        val session = sceneView.session ?: return

        val dx = end.x - start.x
        val dz = end.z - start.z
        val length = sqrt(dx * dx + dz * dz)

        if (length < 0.01f) return

        // Calculate rotation to face the next point
        val yawDegrees = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

        val midPoint = Float3(
            (start.x + end.x) / 2f,
            start.y,
            (start.z + end.z) / 2f
        )

        // The polygon is defined in 2D space.
        // We make 'length' the vertical (Y) dimension of the 2D shape.
        val rectPath = listOf(
            dev.romainguy.kotlin.math.Float2(-width / 2f, -length / 2f),
            dev.romainguy.kotlin.math.Float2( width / 2f, -length / 2f),
            dev.romainguy.kotlin.math.Float2( width / 2f,  length / 2f),
            dev.romainguy.kotlin.math.Float2(-width / 2f,  length / 2f)
        )

        val shapeNode = ShapeNode(
            engine = engine,
            polygonPath = rectPath,
            materialInstance = materialLoader.createColorInstance(
                color,
                metallic = 0.1f,
                roughness = 0.7f
            )
        ).apply {
            transform(
                position = Float3(0f, 0f, 0f),
                // ✅ CRITICAL FIX: Rotate 90 degrees on X to lay it flat on the ground.
                // Then rotate on Y (up axis) to point the road toward the end point.
                rotation = Float3(90f, yawDegrees, 0f)
            )
        }

        val pose = Pose(floatArrayOf(midPoint.x, midPoint.y, midPoint.z), floatArrayOf(0f, 0f, 0f, 1f))
        val anchorNode = try {
            AnchorNode(engine, session.createAnchor(pose)).apply { addChildNode(shapeNode) }
        } catch (e: Exception) { return }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)
    }

    private fun addSharpTurn(
        p0: Float3,
        p1: Float3,
        p2: Float3,
        width: Float,
        thickness: Float,
        color: Color,
        engine: Engine,
        materialLoader: MaterialLoader
    ) {
        addStraightRoad(p0, p1, width, thickness, color, engine, materialLoader)
        addStraightRoad(p1, p2, width, thickness, color, engine, materialLoader)
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

    // For backup from me - Works but the road segment overlapping the circle of the junction
//    private suspend fun isShowingARNavigationRectanglePathAllAtOnceGeminiBasedOnCompass(
//        startX: Float,
//        startZ: Float,
//        endX: Float,
//        endZ: Float,
//        baseY: Float,
//        engine: Engine,
//        materialLoader: MaterialLoader
//    ) {
//        val session = sceneView.session ?: return
//        val frame = sceneView.frame ?: return
//
//        // 🛡️ IMPORTANT: Only create anchors if ARCore is actually tracking the environment
//        if (frame.camera.trackingState != TrackingState.TRACKING) {
//            Log.w("ARCoreDebug", "Skipping segment: Camera not tracking")
//            return
//        }
//
//        val dx = endX - startX
//        val dz = endZ - startZ
//        val dist = sqrt(dx * dx + dz * dz)
//
//        // If the distance is too small, we skip to avoid overlapping anchors
//        if (dist < 0.2f) return
//
//        val yawDegrees = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
//        val midX = (startX + endX) / 2f
//        val midZ = (startZ + endZ) / 2f
//
//        // FIX: Use a slightly higher baseY (+0.1f) to ensure it stays above the floor
//        val pose = Pose(floatArrayOf(midX, baseY + 0.1f, midZ), floatArrayOf(0f, 0f, 0f, 1f))
//
//        val anchor = try {
//            session.createAnchor(pose)
//        } catch (e: Exception) {
//            Log.e("ARCoreDebug", "Anchor failed for segment")
//            null
//        } ?: return
//
//        val material = materialLoader.createColorInstance(
//            color = Color(0f, 0.47f, 0.83f, 0.8f),
//            metallic = 0.0f,
//            roughness = 0.8f
//        )
//
//        // FIX: Increased height (0.05f) and width (1.0f) for better visibility
//        val roadSegmentNode = CubeNode(
//            engine = engine,
//            size = Float3(1.0f, 0.05f, dist),
//            materialInstance = material
//        ).apply {
//            rotation = Float3(0f, yawDegrees, 0f)
//        }
//
//        val anchorNode = AnchorNode(engine, anchor).apply {
//            addChildNode(roadSegmentNode)
//        }
//
//        sceneView.addChildNode(anchorNode)
//        roadCircleNodes.add(anchorNode)
//        yield()
//    }

    private suspend fun isShowingARNavigationRectanglePathAllAtOnceGeminiBasedOnCompass(
        startX: Float,
        startZ: Float,
        endX: Float,
        endZ: Float,
        baseY: Float,
        engine: Engine,
        materialLoader: MaterialLoader
    ) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        if (frame.camera.trackingState != TrackingState.TRACKING) return

        val dx = endX - startX
        val dz = endZ - startZ
        val fullDist = sqrt(dx * dx + dz * dz)

        // 🛡️ OFFSET LOGIC:
        // We want to shrink the road so it doesn't overlap the 0.5m radius circles.
        val junctionRadius = 0.0f
        val newDist = fullDist - (junctionRadius * 2)

        // If points are closer than 1 meter, the circles already touch. No road needed.
        if (newDist < 0.1f) return

        val yawDegrees = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

        // 🎯 NEW MIDPOINT: Still the middle of the original points
        val midX = (startX + endX) / 2f
        val midZ = (startZ + endZ) / 2f

        val pose = Pose(floatArrayOf(midX, baseY + 0.1f, midZ), floatArrayOf(0f, 0f, 0f, 1f))

        val anchor = try {
            session.createAnchor(pose)
        } catch (e: Exception) {
            null
        } ?: return

        val material = materialLoader.createColorInstance(
            color = Color(0f, 0.47f, 0.83f, 0.8f),
            metallic = 0.0f,
            roughness = 0.8f
        )

        // Use 'newDist' instead of 'dist' to avoid crossing into the cylinders
        val roadSegmentNode = CubeNode(
            engine = engine,
            size = Float3(1.0f, 0.05f, newDist),
            materialInstance = material
        ).apply {
            rotation = Float3(0f, yawDegrees, 0f)
        }

        val anchorNode = AnchorNode(engine, anchor).apply {
            addChildNode(roadSegmentNode)
        }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)
        yield()
    }

    // 🆕 Changed to 'suspend' and removed internal 'launch'
    private suspend fun isShowingARNavigationArrowAllAtOnceBasedOnCompass(arWorldRotation: Float) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) return

        // 1. Clean up old anchors from the ARCore Session first
        hideAllArrowsGemini()

        val originLat = yourLocation.lat
        val originLon = yourLocation.lon
        val baseY = frame.camera.pose.ty() - 1.5f

        val cosTheta = cos(arWorldRotation.toDouble())
        val sinTheta = sin(arWorldRotation.toDouble())

        val localPositions = instructionPoints.map { point ->
            val worldX = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
            val worldZ = (originLat - point.lat) * 111_000.0

            // For backup from me - don't delete this one below
//            val rotatedX = (worldX * cosTheta + worldZ * sinTheta).toFloat()
//            val rotatedZ = (-worldX * sinTheta + worldZ * cosTheta).toFloat()

            // Inside your drawing function:
            val rotatedX = (worldX * cos(arWorldRotation) - worldZ * sin(arWorldRotation)).toFloat()
            val rotatedZ = (worldX * sin(arWorldRotation) + worldZ * cos(arWorldRotation)).toFloat()
            Pair(rotatedX, rotatedZ)
        }

        // 2. Draw Arrows
        instructionPoints.indices.forEach { i ->
            // 🛡️ VERY IMPORTANT: If tracking is lost, this suspend function is cancelled
            if (!coroutineContext.isActive) return@forEach

            val (x, z) = localPositions[i]
            val pose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
            val anchor = try { session.createAnchor(pose) } catch (e: Exception) { return@forEach }

            val yaw = if (i < localPositions.size - 1) {
                val (x1, z1) = localPositions[i]
                val (x2, z2) = localPositions[i + 1]
                Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
            } else 0f

            addArrowWithYaw(anchor, i, yaw)
            yield() // Give the AR engine a millisecond to breathe
        }

        // --- Draw Road Segments ---
        lifecycleScope.launch {
            instructionPoints.indices.forEach { i ->
                if (!coroutineContext.isActive) return@forEach

                val (currentX, currentZ) = localPositions[i]

                // 1. 🟢 ADD CIRCLE (Junction Sphere) at every point
                addJunctionSphere(currentX, baseY, currentZ)

                // 2. 🛣️ DRAW ROAD to the next point
                if (i < instructionPoints.size - 1) {
                    val (nextX, nextZ) = localPositions[i + 1]

                    isShowingARNavigationRectanglePathAllAtOnceGeminiBasedOnCompass(
                        startX = currentX,
                        startZ = currentZ,
                        endX = nextX,
                        endZ = nextZ,
                        baseY = baseY,
                        engine = sceneView.engine,
                        materialLoader = sceneView.materialLoader
                    )
                }

                // Slightly longer delay for the first 5 segments to ensure stability
                delay(if (i < 5) 100L else 50L)
            }
        }

    }

    private fun isShowingTheARNavigationOnceAtAllGeminiBasedOnCompassDegrees(
        index: Int,
        instructionPoints: List<SerializableRoutePoint>,
        compassClassDegrees: Float
    ) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        if (isDrawingStarted) return
        if (instructionPoints.size < 2) return

        val targetBearing = normalizedBearingDegrees(instructionPoints[0], instructionPoints[1]).toFloat()
        val getCompassDirectionString = getDirectionString(start = instructionPoints[0], end = instructionPoints[1])
        val currentPhoneHeading = compassClassDegrees

        isShowingDirectionDegree = "Face your phone to:\n$getCompassDirectionString\n(Target: ${targetBearing.toInt()}°)"

        if (!isCalibrated) {
            val diff = abs(currentPhoneHeading - targetBearing)
            val normalizedDiff = min(diff, 360f - diff)

            if (normalizedDiff < 20.0f) {
                isShowingDirectionDegree = "Locking to True North... ⏳ (${offsetSamples.size}/20)"

                // 1. Get the current AR Camera Yaw
                val cameraQuaternion = frame.camera.displayOrientedPose.rotationQuaternion
                val arKitYaw = atan2(
                    2.0 * (cameraQuaternion[3] * cameraQuaternion[1] + cameraQuaternion[0] * cameraQuaternion[2]),
                    1.0 - 2.0 * (cameraQuaternion[1] * cameraQuaternion[1] + cameraQuaternion[2] * cameraQuaternion[2])
                )
                val arKitDegrees = Math.toDegrees(arKitYaw).toFloat()

                // 2. 🧭 TRUE NORTH CALCULATION
                // We want to find the offset that turns AR -Z into Compass North.
                // The formula: Offset = (CompassHeading + ARYaw)
                // We use '+' because as you turn Right (Compass increases),
                // AR Yaw increases in the opposite direction (Internal engine math).
                val currentStableNorth = (currentPhoneHeading + arKitDegrees + 360f) % 360f
                offsetSamples.add(currentStableNorth.toDouble())

                if (offsetSamples.size >= 20) {
                    val averageNorth = offsetSamples.average().toFloat()

                    // 3. FINAL OFFSET
                    // We don't subtract 180 or 90 here; we let the Sin/Cos math handle the direction.
                    this.arWorldRotationOffset = averageNorth

                    isCalibrated = true
                    isShowingDirectionDegree = ""
                    Log.d("ARCalibration", "🎯 TRUE NORTH LOCKED: $arWorldRotationOffset")
                } else {
                    return
                }
            } else {
                val targetDirectionStr = getDirectionString(instructionPoints[0], instructionPoints[1])
                isShowingDirectionDegree = "Face your phone to:\n$targetDirectionStr\n(Target: ${targetBearing.toInt()}°)"
                offsetSamples.clear()
                return
            }
        }

        if (isCalibrated && !isDrawingStarted) {
            isDrawingStarted = true
            // We use Negative rotation because we are rotating the WORLD back to North
            val rotationAngleRad = Math.toRadians((-arWorldRotationOffset).toDouble()).toFloat()

            navigationDrawingJob?.cancel()
            navigationDrawingJob = lifecycleScope.launch {
                isShowingARNavigationArrowAllAtOnceBasedOnCompass(rotationAngleRad)
            }
        }
    }

    private suspend fun isShowingARNavigationArrowAllAtOnceBasedOnCompassAndFloorId(index: Int, arWorldRotation: Float) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) return

        // 1. Get the floor ID of the current instruction step
        val currentStepPoint = instructionPoints.getOrNull(index) ?: return
        val currentTargetFloor = currentStepPoint.floorId

        // Double check: if someone manually reset isDrawingStarted but floor is same
        if (isDrawingStarted && currentTargetFloor == lastRenderedFloorId) return

        isDrawingStarted = true // Mark as drawing so monitor loop doesn't restart this

        val originLat = yourLocation.lat
        val originLon = yourLocation.lon
        val baseY = frame.camera.pose.ty() - 1.5f

        // 4. FILTER: Only process points belonging to the current floor
        // We use forEachIndexed to keep track of the original sequence if needed
        val pointsOnThisFloor = mutableListOf<Pair<Int, SerializableRoutePoint>>()
        instructionPoints.forEachIndexed { idx, point ->
            if (point.floorId == currentTargetFloor) {
                pointsOnThisFloor.add(idx to point)
            }
        }

        if (pointsOnThisFloor.size < 2) return
        isDrawingStarted = true // Set this early to prevent overlapping launches

        // 5. CALCULATE & DRAW
        val localPositions = pointsOnThisFloor.map { (_, point) ->
            val worldX = (point.lon - originLon) * 111_000.0 * cos(Math.toRadians(originLat))
            val worldZ = (originLat - point.lat) * 111_000.0

            val rotatedX = (worldX * cos(arWorldRotation) - worldZ * sin(arWorldRotation)).toFloat()
            val rotatedZ = (worldX * sin(arWorldRotation) + worldZ * cos(arWorldRotation)).toFloat()
            Pair(rotatedX, rotatedZ)
        }

        pointsOnThisFloor.indices.forEach { i ->
            if (!coroutineContext.isActive) return@forEach

            val (originalIndex, _) = pointsOnThisFloor[i]
            val (x, z) = localPositions[i]

            val pose = Pose(floatArrayOf(x, baseY, z), floatArrayOf(0f, 0f, 0f, 1f))
            val anchor = try { session.createAnchor(pose) } catch (e: Exception) { return@forEach }

            // Determine Yaw (Direction) based on the filtered sub-list
            val yaw = if (i < localPositions.size - 1) {
                val (x1, z1) = localPositions[i]
                val (x2, z2) = localPositions[i + 1]
                Math.toDegrees(atan2((x2 - x1).toDouble(), (z2 - z1).toDouble())).toFloat()
            } else 0f

            // Add the AR assets
            addArrowWithYaw(anchor, originalIndex, yaw)
            addJunctionSphere(x, baseY, z)

            if (i < pointsOnThisFloor.size - 1) {
                val (nextX, nextZ) = localPositions[i+1]
                isShowingARNavigationRectanglePathAllAtOnceGeminiBasedOnCompass(
                    startX = x, startZ = z,
                    endX = nextX, endZ = nextZ,
                    baseY = baseY, engine = sceneView.engine,
                    materialLoader = sceneView.materialLoader
                )
            }
            yield()
        }
    }

    private fun isShowingTheARNavigationAllAtOnceGeminiBasedOnCompassDegreesAndFloorId(
        index: Int,
        instructionPoints: List<SerializableRoutePoint>,
        compassClassDegrees: Float
    ) {
        val session = sceneView.session ?: return
        val frame = sceneView.frame ?: return

        // 1. GET THE FLOOR ID OF THE CURRENT INDEX
        val currentStepPoint = instructionPoints.getOrNull(index) ?: return
        val currentTargetFloor = currentStepPoint.floorId

        // 2. FLOOR CHANGE TRIGGER
        if (lastRenderedFloorId != null && currentTargetFloor != lastRenderedFloorId) {
            Log.d("ARFloorLogic", "Floor change: $lastRenderedFloorId -> $currentTargetFloor")
            navigationDrawingJob?.cancel()
            hideAllArrowsGeminiBasedOnFloorId()

            // Reset calibration for the new floor so the user is prompted to face the right way
            isCalibrated = false
            offsetSamples.clear()

            isDrawingStarted = false
            lastRenderedFloorId = currentTargetFloor
        }

        if (lastRenderedFloorId == null) lastRenderedFloorId = currentTargetFloor

        // 3. FIND POINTS FOR THE CURRENT FLOOR ONLY
        // We need the first two points of THIS floor to set the initial bearing
        val floorPoints = instructionPoints.filter { it.floorId == currentTargetFloor }
        if (floorPoints.size < 2) return

        // Use the first two points of the CURRENT floor for calibration
        val startPoint = floorPoints[0]
        val endPoint = floorPoints[1]

        val targetBearing = normalizedBearingDegrees(startPoint, endPoint).toFloat()
        val getCompassDirectionString = getDirectionString(start = startPoint, end = endPoint)
        val currentPhoneHeading = compassClassDegrees

        if (isDrawingStarted) return

        // 4. CALIBRATION LOGIC
        if (!isCalibrated) {
            val diff = abs(currentPhoneHeading - targetBearing)
            val normalizedDiff = min(diff, 360f - diff)

            if (normalizedDiff < 20.0f) {
                isShowingDirectionDegree = "Locking to True North... ⏳ (${offsetSamples.size}/20)"

                val cameraQuaternion = frame.camera.displayOrientedPose.rotationQuaternion
                val arKitYaw = atan2(
                    2.0 * (cameraQuaternion[3] * cameraQuaternion[1] + cameraQuaternion[0] * cameraQuaternion[2]),
                    1.0 - 2.0 * (cameraQuaternion[1] * cameraQuaternion[1] + cameraQuaternion[2] * cameraQuaternion[2])
                )
                val arKitDegrees = Math.toDegrees(arKitYaw).toFloat()

                val currentStableNorth = (currentPhoneHeading + arKitDegrees + 360f) % 360f
                offsetSamples.add(currentStableNorth.toDouble())

                if (offsetSamples.size >= 20) {
                    val averageNorth = offsetSamples.average().toFloat()
                    this.arWorldRotationOffset = averageNorth
                    isCalibrated = true
                    isShowingDirectionDegree = ""
                    Log.d("ARCalibration", "🎯 TRUE NORTH LOCKED: $arWorldRotationOffset")
                }
            } else {
                isShowingDirectionDegree = "Face your phone to:\n$getCompassDirectionString\n(Target: ${targetBearing.toInt()}°)"
                offsetSamples.clear()
            }
            return // Stop here if not calibrated
        }

        // 5. DRAWING TRIGGER
        if (isCalibrated && !isDrawingStarted) {
            val rotationAngleRad = Math.toRadians((-arWorldRotationOffset).toDouble()).toFloat()
            navigationDrawingJob?.cancel()
            navigationDrawingJob = lifecycleScope.launch {
                isShowingARNavigationArrowAllAtOnceBasedOnCompassAndFloorId(index, rotationAngleRad)
            }
        }
    }

    private fun addJunctionSphere(x: Float, y: Float, z: Float) {
        val session = sceneView.session ?: return

        // Create an anchor at the junction
        // Lifting it to y + 0.1f to match the road's increased elevation
        val pose = Pose(floatArrayOf(x, y + 0.1f, z), floatArrayOf(0f, 0f, 0f, 1f))
        val anchor = try { session.createAnchor(pose) } catch (e: Exception) { return }

        val cylinderNode = CylinderNode(
            engine = sceneView.engine,
            radius = 0.5f,  // 0.5f radius = 1.0f width (matching road width)
            height = 0.051f, // matching road thickness
            materialInstance = sceneView.materialLoader.createColorInstance(
                color = Color(0f, 0.47f, 0.83f, 0.8f), // Matching road blue
                metallic = 0.0f,
                roughness = 0.8f
            )
        ).apply {
            // Positioned at the center of the anchor
            position = Float3(0f, 0f, 0f)
        }

        val anchorNode = AnchorNode(sceneView.engine, anchor).apply {
            addChildNode(cylinderNode)
        }

        sceneView.addChildNode(anchorNode)
        roadCircleNodes.add(anchorNode)
    }

    // In your ViewModel observer or wherever instructionIndex is updated
    fun onInstructionStepChanged(newIndex: Int) {
        this.instructionIndex = newIndex

        // Reset drawing state so the loop triggers a fresh draw for the new floor
        isDrawingStarted = false

        // This will cause isShowingTheARNavigationOnceAtAllGemini... to
        // run again in your monitor loop and call the updated drawing function.
    }

    private fun resetARCalibrationState() {
        isDrawingStarted = false
        isCalibrated = false
        offsetSamples.clear()
        navigationDrawingJob?.cancel()
        hideAllArrowsGemini()
        Log.d("ARCalibration", "♻️ State Reset for new attempt")
    }

    private fun hideAllArrowsGeminiBasedOnFloorId() {
        Log.d("ARFloorLogic", "🗑️ Full Scene Cleanup: Removing all floor assets")

        // Combine all lists to clean them up in one go
        val allNodes = arrowNodes + roadCircleNodes + junctionNodes

        allNodes.forEach { node ->
            node.detachAnchor() // Stops ARCore from tracking this point
            node.parent = null  // Removes from scene
            node.destroy()      // Frees memory
        }

        // Clear the lists for the next floor
        arrowNodes.clear()
        roadCircleNodes.clear()
        junctionNodes.clear()
    }

    private fun hideAllArrowsGemini() {
        // 1. Check if sceneView is still attached and session exists
        val session = sceneView.session ?: return

        try {
            // 2. Use a copy (toList) to avoid ConcurrentModificationException
            // while iterating and removing
            val nodesCopy = roadCircleNodes.toList()

            for (node in nodesCopy) {
                // Remove the node from the scene first
                node.parent = null

                // Detach the anchor from the ARCore session
                node.anchor.let { anchor ->
                    if (session.allAnchors.contains(anchor)) {
                        anchor.detach()
                    }
                }
            }

            // 3. Clear the original list only after successful removal
            roadCircleNodes.clear()

        } catch (e: Exception) {
            Log.e("ARCoreDebug", "Error during hideAllArrowsGemini: ${e.message}")
        }
    }

    private fun hideAllArrows() {
        Log.w("ARCoreDebug", "🧹 Removing all AR arrows due to tracking loss...")

        try {
            // Example cleanup
            arrowNodes.forEach { node ->
                sceneView.removeChildNode(node)
            }
            arrowNodes.clear()

        } catch (e: Exception) {
            Log.e("ARCoreDebug", "Error while hiding arrows: ${e.message}")
        }
    }

    private fun normalizedBearingDegrees(start: SerializableRoutePoint, end: SerializableRoutePoint): Double {
        val lat1 = Math.toRadians(start.lat)
        val lon1 = Math.toRadians(start.lon)
        val lat2 = Math.toRadians(end.lat)
        val lon2 = Math.toRadians(end.lon)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun getDirectionString(start: SerializableRoutePoint, end: SerializableRoutePoint): String {
        val b = normalizedBearingDegrees(start, end)
        return when {
            b < 22.5 || b >= 337.5 -> "North"
            b < 67.5 -> "North-East"
            b < 112.5 -> "East"
            b < 157.5 -> "South-East"
            b < 202.5 -> "South"
            b < 247.5 -> "South-West"
            b < 292.5 -> "West"
            else -> "North-West"
        }
    }

}
