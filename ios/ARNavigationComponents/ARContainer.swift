//
//  ARContainer.swift
//  mapxus-hsitp-ios
//
//  Created by Boxyguild on 6/12/25.
//

import SwiftUI
import ARKit
import RealityKit
import Foundation
import CoreLocation
import Combine
import CoreLocation

import MapxusMapSDK
import MapxusBaseSDK
import MapxusComponentKit
import MapxusVisualSDK

private enum ARNavigationTransition {
    case none, floorChange, buildingChange
}

@available(iOS 18.0, *)
struct ARNavigationArrowPointDirectionViewContainer: UIViewRepresentable {
    var instructionList: [MXMInstruction]
    var instructionPointList: [MXMGeoPoint]
    var currentInstructionIndex: Int
    var compassClassDegrees: Double
    var languageCode: String

    func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        let config = ARWorldTrackingConfiguration()
        config.planeDetection = []
        config.isLightEstimationEnabled = true
        arView.session.run(config)

        context.coordinator.arView = arView

        return arView
    }
    
    func updateUIView(_ uiView: ARView, context: Context) {
        context.coordinator.isShowingTheARNavigationBasedOnCompassDegreesOnceAtAll(currentIndex: currentInstructionIndex, instructionList: instructionList, instructionPoints: instructionPointList, compassClassDegrees: compassClassDegrees)
    }
    
//    func updateUIView(_ uiView: ARView, context: Context) {
//        let isAllAtOnce = isShowingARNavigationAllAtOnceOr
//
//        /// Only re-render when mode changes
//        if context.coordinator.lastModeIsAllAtOnce != isAllAtOnce {
//            context.coordinator.lastModeIsAllAtOnce = isAllAtOnce
//        }
//
//        if isAllAtOnce {
//            /// Showing AR Navigation one by one based on User Phone Compass
//            context.coordinator.isShowingTheARNavigationBasedOnCompassDegreesOneByOne(instructionList: instructionList, instructionPoints: instructionPointList, currentIndex: currentInstructionIndex, compassClassDegrees: compassClassDegrees)
//
//            /// For backup from me - Show AR Navigation one by one - Works
////            context.coordinator.isShowingTurnArrowsOneByOne(instructions: instructionList, points: instructionPointList, currentIndex: currentInstructionIndex)
////            context.coordinator.isShowingCircleDirectionPathOneByOne(from: instructionPointList, instruction: instructionList, currentIndex: currentInstructionIndex)
//        } else {
//            /// Show the All AR Navigation all at once based on User Phone Compass
//            context.coordinator.isShowingTheARNavigationBasedOnCompassDegreesOnceAtAll(currentIndex: currentInstructionIndex, instructionList: instructionList, instructionPoints: instructionPointList, compassClassDegrees: compassClassDegrees)
//
//            /// For backup from me - Show AR Navigation all at once without compass - Works
////            context.coordinator.isShowingArrowAllAtOnce(instructions: instructionList, points: instructionPointList)
////            context.coordinator.isShowingSmoothRectangleRoadAllAtOnce(instructions: instructionList, points: instructionPointList)
//        }
//    }
    
    func makeCoordinator() -> Coordinator {
        let coordinator = Coordinator(
            currentInstructionIndex: currentInstructionIndex
        )
        coordinator.instructionList = instructionList
        coordinator.instructionPointList = instructionPointList
        coordinator.compassClassDegrees = compassClassDegrees
        coordinator.languageCode = languageCode
        return coordinator
    }
    
    static func dismantleUIView(_ uiView: ARView, coordinator: Coordinator) {
        uiView.session.pause()
        coordinator.endARNavigation() // This clears the arrows/roads
        uiView.removeFromSuperview()
    }

  // Coordinator is used by SwiftUI's UIViewRepresentable makeCoordinator().
  // Avoid marking the entire class as iOS 18+ so SwiftUI can see the type on older SDKs.
  // Guard iOS 18+ APIs inside methods where needed with `if #available(iOS 18.0, *)` checks.
  class Coordinator: NSObject, ARSessionDelegate {
        weak var arView: ARView?
        var instructionList: [MXMInstruction] = []
        var instructionPointList: [MXMGeoPoint] = []
        var compassClassDegrees: Double = 0.0

        // Property to track the road drawing task
        private var roadDrawingTask: Task<Void, Never>?
        private var arrowDrawingTask: Task<Void, Never>? // Add this

        var arrowAnchor: AnchorEntity?
        var instructionArrows: [AnchorEntity] = []
        var instructionArrowsByFloor: [String: [AnchorEntity]] = [:]

        var cancellables = Set<AnyCancellable>()

        /// For Animations
        var blinkingCancellables: [Cancellable] = []
        var animationCancellables: [Cancellable] = []
        var jumpCancellables: [Cancellable] = []

        var currentInstructionIndex: Int
        var currentFloorId: String? = nil
        var currentRenderedFloorId: String? = nil

        var lastTurnIndex: Int = -1 // put this in Coordinator to persist between calls

        var hasStartedARNavigation = false /// For showing the AR Navigation arrow based on Compass
        var hasCompassMatchedFirstSegment = false

        var hasRenderedAllAtOnce = false
        var lastModeIsAllAtOnce = false

        var arWorldRotationOffset: Float = 0.0
        /// Add this property to your class
        var calibrationAngleCovered: Double = 0.0
        var lastCalibrationDegrees: Double?
        var isCalibrated: Bool = false
        var offsetSamples: [Double] = []
        var lastProcessedIndex: Int = -1

        var lastRenderedFloorId: String? = nil
        var isWaitingForFloorConfirmation: Bool = false
        var isDrawingStarted: Bool = false
        var isStopping: Bool = false // Add this property

        var alertDialogBuildingTitle: String = ""
        var alertDialogBuildingMessage: String = ""

        var languageCode: String = ""

        @AppStorage("ARNavigation-App-Enabling-TTS") private var isEnablingTTS: Bool = true
        @AppStorage("ARNavigation-App-Enabling-Motion-Sensor") private var isEnablingMotionSensor: Bool = false
        @AppStorage("ARNavigation-App-Showing-Direction") private var isShowingDirectionDegree: String = ""
        @AppStorage("ARNavigation-App-Stopping-AR-Navigation") private var isStoppingARNavigation: Bool = false

        init(currentInstructionIndex: Int) {
            self.currentInstructionIndex = currentInstructionIndex
        }
        
        let translationClass: TranslationClass = TranslationClass()
        
//        func session(_ session: ARSession, didUpdate frame: ARFrame) {
//            isShowingTheARNavigationBasedOnCompassDegreesOnceAtAll(currentIndex: currentInstructionIndex, instructionList: instructionList, instructionPoints: instructionPointList, compassClassDegrees: compassClassDegrees)
//        }
        
        func isShowingTurnArrowsOneByOneWithoutRemovingThePreviousOneGemini(
            instructions: [MXMInstruction],
            points: [MXMGeoPoint],
            currentIndex: Int,
            arWorldRotation: Float // Rotation angle in RADIANS
        ) {
            guard let arView else { return }

            /// 1. Wait for ARKit frame
            guard arView.session.currentFrame != nil else {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    self.isShowingTurnArrowsOneByOneWithoutRemovingThePreviousOneGemini(
                        instructions: instructions,
                        points: points,
                        currentIndex: currentIndex,
                        arWorldRotation: arWorldRotation
                    )
                }
                return
            }

            // 2. Find next instruction index
            var nextIndex = currentIndex
            while nextIndex < instructions.count {
                if let text = instructions[nextIndex].text?.lowercased(),
                    text.contains("straight") || text.contains("steps ahead") ||
                    text.contains("turn") || text.contains("keep") ||
                    text.contains("arrive") || text.contains("stairs") || text.contains("elevator") {
                    break
                }
                nextIndex += 1
            }

            guard instructions.indices.contains(nextIndex), points.indices.contains(nextIndex) else { return }

            // 3. Clear previous arrows
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()

            Task {
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)

                let current = points[nextIndex]
                let futureIndex = min(nextIndex + 1, points.count - 1)
                let future = points[futureIndex]

                // 4. COORDINATE CALCULATION (Matching Circles)
                let worldX = (Float(current.longitude) - baseLon) * 100_000
                let worldZ = (baseLat - Float(current.latitude)) * 100_000

                let cosTheta = cos(arWorldRotation)
                let sinTheta = sin(arWorldRotation)

                // Matrix alignment
                let rotatedX = worldX * cosTheta + worldZ * sinTheta
                let rotatedZ = -worldX * sinTheta + worldZ * cosTheta
                
                let finalArrowPosition = SIMD3<Float>(rotatedX, -0.5, rotatedZ)

                // 5. ROTATION LOGIC (The Fix)
                // Calculate the local angle of the path segment
                let dx = Float(future.longitude - current.longitude)
                let dz = Float(current.latitude - future.latitude)
                
                // This is the angle relative to "Map North"
                let segmentAngle = atan2(dx, dz) - .pi / 2
                
                // The final rotation of the arrow must be the segment angle PLUS the world offset
                // We subtract .pi / 2 if your 3D model's "forward" is pointing Right instead of Forward
                let finalModelRotation = segmentAngle + arWorldRotation

                // 6. Model Loading
                let text = instructions[nextIndex].text?.lowercased() ?? ""
                var arrowName = "direction_arrow_horizontal"
                if text.contains("arrive") { arrowName = "arrive_at_destination" }
                else if text.contains("up") { arrowName = "go_upstair_arrow" }
                else if text.contains("down") { arrowName = "go_downstair_arrow_copy" }

                do {
                    let entity = try await ModelEntity(named: arrowName)

                    await MainActor.run {
                        entity.scale = (arrowName == "direction_arrow_horizontal") ? [0.1, 0.1, 0.1] : [0.009, 0.009, 0.009]
                        
                        // Set model rotation relative to its parent anchor
                        entity.orientation = simd_quatf(angle: finalModelRotation, axis: [0, 1, 0])
                        
                        // Create Anchor at the correctly rotated world position
                        let anchor = AnchorEntity(world: finalArrowPosition)
                        // Anchor orientation stays 0 so the coordinate system is stable
                        anchor.orientation = simd_quatf(angle: 0, axis: [0, 1, 0])
                        
                        anchor.addChild(entity)
                        arView.scene.addAnchor(anchor)
                        instructionArrows = [anchor]
                        
                        if arrowName == "arrive_at_destination" {
                            startInfiniteRotation(for: entity, in: arView)
                        }
                    }
                } catch {
                    print("❌ Failed to load arrow: \(error)")
                }
            }
        }
        
        func isShowingTurnArrowsOneByOneAndRemovedThePreviousOneGemini(
            instructions: [MXMInstruction],
            points: [MXMGeoPoint],
            currentIndex: Int,
            arWorldRotation: Float
        ) {
            guard let arView else { return }

            // 1. Find next valid instruction index
            var nextIndex = currentIndex
            while nextIndex < instructions.count {
                if let text = instructions[nextIndex].text?.lowercased(),
                    text.contains("straight") || text.contains("steps ahead") ||
                    text.contains("turn") || text.contains("keep") ||
                    text.contains("arrive") || text.contains("stairs") || text.contains("elevator") {
                    break
                }
                nextIndex += 1
            }

            // --- KEY FIX: Prevent reloading if we are already showing this arrow ---
            if nextIndex == self.lastProcessedIndex && !instructionArrows.isEmpty {
                return // Already showing the correct arrow, do nothing!
            }
            
            guard instructions.indices.contains(nextIndex), points.indices.contains(nextIndex) else { return }
            
            // Update the tracker
            self.lastProcessedIndex = nextIndex

            // 2. Clear previous arrows
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()

            Task {
                // ... (Coordinate and rotation math remains the same) ...
                let baseLat = Float(points[0].latitude); let baseLon = Float(points[0].longitude)
                let current = points[nextIndex]; let future = points[min(nextIndex + 1, points.count - 1)]
                let worldX = (Float(current.longitude) - baseLon) * 100_000
                let worldZ = (baseLat - Float(current.latitude)) * 100_000
                let cosTheta = cos(arWorldRotation); let sinTheta = sin(arWorldRotation)
                let rotatedX = worldX * cosTheta + worldZ * sinTheta
                let rotatedZ = -worldX * sinTheta + worldZ * cosTheta
                let finalArrowPosition = SIMD3<Float>(rotatedX, -0.5, rotatedZ)
                let dx = Float(future.longitude - current.longitude); let dz = Float(current.latitude - future.latitude)
                let segmentAngle = atan2(dx, dz) - .pi / 2
                let finalModelRotation = segmentAngle + arWorldRotation

                let text = instructions[nextIndex].text?.lowercased() ?? ""
                var arrowName = "direction_arrow_horizontal"
                if text.contains("arrive") { arrowName = "arrive_at_destination" }

                do {
                    let entity = try await ModelEntity(named: arrowName)

                    await MainActor.run {
                        // Double check we haven't moved to a new index while loading
                        if nextIndex != self.lastProcessedIndex { return }

                        entity.scale = (arrowName == "direction_arrow_horizontal") ? [0.1, 0.1, 0.1] : [0.009, 0.009, 0.009]
//                        entity.orientation = simd_quatf(angle: finalModelRotation, axis: [0, 1, 0])
                        
                        let headingRotation = simd_quatf(angle: finalModelRotation, axis: [0, 1, 0])
                        
                        var tiltRotation = simd_quatf(angle: 0, axis: [1, 0, 0])
                        let spinAngle: Float = .pi / 2
                        var spinRotation = simd_quatf(angle: 0, axis: [0, 1, 0])

                        if text.contains("up") {
                            tiltRotation = simd_quatf(angle: .pi / 2, axis: [1, 0, 0])
                            spinRotation = simd_quatf(angle: spinAngle, axis: [0, 1, 0])
                            startInfiniteRotationForArrowUpAndDown(for: entity, in: arView)
                        } else if text.contains("down") {
                            tiltRotation = simd_quatf(angle: -.pi / 2, axis: [1, 0, 0])
                            spinRotation = simd_quatf(angle: spinAngle, axis: [0, 1, 0])
                            startInfiniteRotationForArrowUpAndDown(for: entity, in: arView)
                        } else if arrowName == "arrive_at_destination" {
                            startInfiniteRotation(for: entity, in: arView)
                        }

                        entity.orientation = headingRotation * tiltRotation * spinRotation
                        
                        let anchor = AnchorEntity(world: finalArrowPosition)
                        anchor.addChild(entity)
                        arView.scene.addAnchor(anchor)
                        
                        self.instructionArrows = [anchor]
                    }
                } catch {
                    print("❌ Failed to load arrow: \(error)")
                }
            }
        }
        
        func isShowingTheARNavigationBasedOnCompassDegreesOneByOne(
            instructionList: [MXMInstruction],
            instructionPoints: [MXMGeoPoint],
            currentIndex: Int,
            compassClassDegrees: Double
        ) {
            guard instructionPoints.count >= 2,
                  let frame = arView?.session.currentFrame else { return }

            let targetBearing = Float(normalizedBearingDegrees(from: instructionPoints[0], to: instructionPoints[1])) // 308.7°
            let targetBearingDirection = getDirectionString(from: instructionPoints[0], to: instructionPoints[1])
            let currentPhoneHeading = compassClassDegrees

            if !isCalibrated {
                let diff = abs(Float(currentPhoneHeading) - targetBearing)
                let normalizedDiff = min(diff, 360 - diff)

                // 1. Check if user is facing the path
                if normalizedDiff < 15.0 {
                    isShowingDirectionDegree = "Almost there, keep going!. (\(offsetSamples.count)/20)"
                    
                    let cameraTransform = frame.camera.transform
                    let arKitYaw = atan2(-cameraTransform.columns.2.x, -cameraTransform.columns.2.z)
                    let arKitDegrees = Double(arKitYaw) * 180 / .pi
                    
                    // Calculate current stable North
                    let currentStableNorth = (currentPhoneHeading + arKitDegrees + 360).truncatingRemainder(dividingBy: 360)
                    
                    // 2. Collect samples to smooth out the 24° error
                    offsetSamples.append(currentStableNorth)
                    
                    print("⏳ Calibrating... Samples: \(offsetSamples.count)/20")

                    if offsetSamples.count >= 20 {
                        let averageNorth = offsetSamples.reduce(0, +) / Double(offsetSamples.count)
                        
                        // 3. APPLY THE FINAL LOCK
                        // We use -180 because your math treats +Z as South.
                        // If it's still slightly off, try changing -180 to -175 or -185 to account for local declination.
                        self.arWorldRotationOffset = Float(averageNorth - 180.0)
                        // Clear the instructional text once finished
                        isShowingDirectionDegree = ""
                        
                        print("🎯 FINAL LOCK! North: \(Int(averageNorth))° | Path should point to: \(targetBearing)°")
                        self.isCalibrated = true
                    }
                } else {
                    // Reset samples if they look away before finishing
                    isShowingDirectionDegree = "Face your camera to:\n\(targetBearingDirection) (\(Int(targetBearing))°)"
                    offsetSamples.removeAll()
                    print("🔄 Please point your phone at the path (\(Int(targetBearing))°)")
                    return
                }
                return
            }

            // --- 2. EXECUTION ---
            let rotationAngleRad = Float(self.arWorldRotationOffset) * .pi / 180
            
            /// Smooth rectangle direction path
            isShowingRectangleDirectionPathOneByOneGemini(
                from: instructionPoints,
                instruction: instructionList,
                currentIndex: currentIndex,
                arWorldRotation: rotationAngleRad
            )
            
            isShowingTurnArrowsOneByOneAndRemovedThePreviousOneGemini(
                instructions: instructionList,
                points: instructionPoints,
                currentIndex: currentIndex,
                arWorldRotation: rotationAngleRad
            )
            
            /// Ensure text stays empty after calibration
            if isShowingDirectionDegree != "" {
                isShowingDirectionDegree = ""
            }
        }

        /// Start of without View Model
        func isShowingArrowAllAtOnceGemini(instructions: [MXMInstruction], points: [MXMGeoPoint], arWorldRotation: Float) {
            guard let arView = arView else { return }
            
            // Clear existing immediate sync references
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()
            
            // 1. Cancel any existing arrow task
            arrowDrawingTask?.cancel()

            arrowDrawingTask = Task {
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)
                let cosTheta = cos(arWorldRotation)
                let sinTheta = sin(arWorldRotation)
                var tempAnchors: [AnchorEntity] = []
                
                // 2. Initial check
                if Task.isCancelled { return }

                for i in 0..<instructions.count {
                    // 3. Middle loop check
                    if Task.isCancelled || !isDrawingStarted { break }
                    guard points.indices.contains(i) else { continue }
                    
                    let text = instructions[i].text?.lowercased() ?? ""
                    let keywords = ["straight", "steps ahead", "turn", "keep", "arrive", "stairs", "elevator"]
                    if !keywords.contains(where: { text.contains($0) }) { continue }

                    let worldX = (Float(points[i].longitude) - baseLon) * 100_000
                    let worldZ = (baseLat - Float(points[i].latitude)) * 100_000
                    
                    let rotatedX = worldX * cosTheta + worldZ * sinTheta
                    let rotatedZ = -worldX * sinTheta + worldZ * cosTheta
                    let arrowPos = SIMD3<Float>(rotatedX, 0.2, rotatedZ)
                    
                    let futureIndex = min(i + 1, points.count - 1)
                    let dx = Float(points[futureIndex].longitude - points[i].longitude)
                    let dz = Float(points[i].latitude - points[futureIndex].latitude)
                    
                    let segmentAngle = atan2(dx, dz) - .pi / 2
                    let finalModelRotation = segmentAngle + arWorldRotation
                    let arrowName = text.contains("arrive") ? "arrive_at_destination" : "direction_arrow_horizontal"

                    do {
                        // 4. Async loading check
                        let entity = try await ModelEntity(named: arrowName)
                        
                        // CRITICAL: Check if navigation ended while the model was loading!
                        if Task.isCancelled || !isDrawingStarted { return }

                        await MainActor.run {
                            // 5. Final UI thread check
                            guard !Task.isCancelled && isDrawingStarted else { return }

                            entity.scale = (arrowName == "direction_arrow_horizontal") ? [0.03, 0.03, 0.03] : [0.004, 0.004, 0.004]
                            
                            let headingRotation = simd_quatf(angle: finalModelRotation, axis: [0, 1, 0])
                            var tiltRotation = simd_quatf(angle: 0, axis: [1, 0, 0])
                            let spinAngle: Float = .pi / 2
                            var spinRotation = simd_quatf(angle: 0, axis: [0, 1, 0])

                            if text.contains("up") {
                                tiltRotation = simd_quatf(angle: .pi / 2, axis: [1, 0, 0])
                                spinRotation = simd_quatf(angle: spinAngle, axis: [0, 1, 0])
                                startInfiniteRotationForArrowUpAndDown(for: entity, in: arView)
                            } else if text.contains("down") {
                                tiltRotation = simd_quatf(angle: -.pi / 2, axis: [1, 0, 0])
                                spinRotation = simd_quatf(angle: spinAngle, axis: [0, 1, 0])
                                startInfiniteRotationForArrowUpAndDown(for: entity, in: arView)
                            } else if arrowName == "arrive_at_destination" {
                                startInfiniteRotation(for: entity, in: arView)
                            }

                            entity.orientation = headingRotation * tiltRotation * spinRotation
                            
                            let anchor = AnchorEntity(world: arrowPos)
                            anchor.addChild(entity)
                            arView.scene.addAnchor(anchor)
                            tempAnchors.append(anchor)
                        }
                    } catch { print("❌ Error loading \(arrowName)") }
                }
                
                // Final sync of the reference list
                if !Task.isCancelled {
                    await MainActor.run { self.instructionArrows = tempAnchors }
                }
            }
        }

        func isShowingSmoothRectangleRoadAllAtOnceGemini(instructions: [MXMInstruction], points: [MXMGeoPoint], arWorldRotation: Float) {
            guard let arView = arView, points.count >= 2 else { return }
            
            // CANCEL any existing road task before starting a new one
            roadDrawingTask?.cancel()

            roadDrawingTask = Task {
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)
                let cosTheta = cos(arWorldRotation)
                let sinTheta = sin(arWorldRotation)
                
                func getRotatedPos(for point: MXMGeoPoint) -> SIMD3<Float> {
                    // MATH FROM OneByOne
                    let worldX = (Float(point.longitude) - baseLon) * 100_000
                    let worldZ = (baseLat - Float(point.latitude)) * 100_000
                    
                    let rX = worldX * cosTheta + worldZ * sinTheta
                    let rZ = -worldX * sinTheta + worldZ * cosTheta
                    return SIMD3<Float>(rX, -0.6, rZ)
                }
                
                // Check for cancellation before entering the MainActor
                if Task.isCancelled { return }

                await MainActor.run {
                    // Final check before UI updates start
                    guard !Task.isCancelled && isDrawingStarted else { return }
                    
                    var pStart = getRotatedPos(for: points[0])
                    addJunctionPlate(at: pStart, roadWidth: 0.3, roadThickness: 0.01, roadColor: .systemBlue, to: arView)

                    for i in 1..<points.count {
                        // CRITICAL: Check if navigation ended while we were looping
                        if Task.isCancelled || !isDrawingStarted {
                            print("🛑 Road drawing cancelled mid-loop")
                            break
                        }
                        
                        guard isDrawingStarted else { break }
                        let pEnd = getRotatedPos(for: points[i])
                        addStraightRoad(start: pStart, end: pEnd, roadWidth: 0.3, roadThickness: 0.01, roadColor: .systemBlue, to: arView)
                        addJunctionPlate(at: pEnd, roadWidth: 0.3, roadThickness: 0.01, roadColor: .systemBlue, to: arView)
                        pStart = pEnd
                    }
                }
            }
        }

        func isShowingTheARNavigationBasedOnCompassDegreesOnceAtAll(
            currentIndex: Int,
            instructionList: [MXMInstruction],
            instructionPoints: [MXMGeoPoint],
            compassClassDegrees: Double
        ) {
            guard let arView = arView, let frame = arView.session.currentFrame else { return }
            guard instructionList.indices.contains(currentIndex) else { return }
            
            let currentTargetFloor = instructionList[currentIndex].floorId ?? ""
            
            let (hasLeave, hasEnter) = instructionList.reduce((false, false)) { (result, instruction) in
                
                let text = instruction.text?.lowercased() ?? ""
                
                // Check for the keywords "leave" and "into" independently
                let foundLeave = text.contains("leave the building")
                let foundEnter = text.contains("and go into the")
                
                return (
                    result.0 || foundLeave,
                    result.1 || foundEnter
                )
            }
            
            // Use the list-wide check to decide the title
            let alertTitle = (hasLeave || hasEnter) ? "Building Changed" : "Floor Changed"
            let floorChangedTitle = "Floor Changed"
            var alertMessage = "Have you arrived at the next floor?"
            
            if instructionList.indices.contains(currentIndex) {
                let currentInstruction = instructionList[currentIndex].text?.lowercased() ?? ""
                
                // 1. Check both conditions
                let mentionsEntry = currentInstruction.contains("and go into the building")
                let mentionsLeave = currentInstruction.contains("leave the building")

                // 2. Logic Priority: If it mentions "go into", that is the MOST important message.
                if mentionsEntry {
                    // Splits by "go into the " and takes the last part
                    if let destination = currentInstruction.lowercased().components(separatedBy: "go into the ").last,
                       destination != currentInstruction.lowercased() {
                        let name = destination.trimmingCharacters(in: .punctuationCharacters).capitalized
                        self.alertDialogBuildingMessage = "Have you entered the \(name)?"
                        self.alertDialogBuildingTitle = "Building Changed"
                    } else {
                        self.alertDialogBuildingMessage = "Have you entered the next building?"
                        self.alertDialogBuildingTitle = "Building Changed"
                    }
                } else if mentionsLeave {
                    if let buildingPart = currentInstruction.lowercased().components(separatedBy: "leave the ").last {
                        // 1. Split again to remove everything after "and"
                        let buildingNameOnly = buildingPart.components(separatedBy: " and").first ?? buildingPart
                        
                        // 2. Clean and Capitalize
                        let name = buildingNameOnly.trimmingCharacters(in: .whitespacesAndNewlines)
                                                   .trimmingCharacters(in: .punctuationCharacters)
                                                   .capitalized
                        
                        self.alertDialogBuildingMessage = "Have you left the \(name)?"
                        self.alertDialogBuildingTitle = "Building Changed"
                    } else {
                        self.alertDialogBuildingMessage = "Have you left the building?"
                        self.alertDialogBuildingTitle = "Building Changed"
                    }
                }
            }
            
            // --- Floor Logic ---
            if let lastFloor = lastRenderedFloorId, currentTargetFloor != lastFloor, !isWaitingForFloorConfirmation {
                // FIX: If the new floor is empty (outdoor), don't show alert, just update and continue
                if currentTargetFloor.isEmpty {
                    self.lastRenderedFloorId = currentTargetFloor
                    // Optionally reset calibration if you want a fresh start outdoors
                    resetARDrawing()
                } else {
                    isWaitingForFloorConfirmation = true
                    resetARDrawing()
                    
                    let alert = UIAlertController(title: self.alertDialogBuildingTitle.isEmpty ? floorChangedTitle : self.alertDialogBuildingTitle, message: self.alertDialogBuildingMessage.isEmpty ? alertMessage : self.alertDialogBuildingMessage, preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "Yes", style: .default) { _ in
                        self.isWaitingForFloorConfirmation = false
                        self.lastRenderedFloorId = currentTargetFloor
                        self.resetARDrawing()
                        self.alertDialogBuildingTitle = ""
                        self.alertDialogBuildingMessage = ""
                    })
                    DispatchQueue.main.async {
                        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                           let rootVC = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController {
                            rootVC.present(alert, animated: true)
                        }
                    }
                    return
                }
            }
            
            // --- 2. STOP HERE IF DRAWING IS ALREADY DONE ---
            // Now that we've checked for floor changes, we can safely exit if
            // the current floor's path is already rendered.
            if isDrawingStarted { return }

            if isWaitingForFloorConfirmation { return }
            if lastRenderedFloorId == nil { lastRenderedFloorId = currentTargetFloor }
            
            // 1. Find the bounds of the current contiguous floor sequence
            var startIndex = currentIndex
            var endIndex = currentIndex
            
            while startIndex > 0 && instructionList[startIndex - 1].floorId ?? "" == currentTargetFloor {
                startIndex -= 1
            }
            
            while endIndex < instructionList.count - 1 && instructionList[endIndex + 1].floorId ?? "" == currentTargetFloor {
                endIndex += 1
            }
            
//            let displayEndIndex = currentTargetFloor.isEmpty ? min(endIndex + 1, instructionList.count - 1) : endIndex
            let displayEndIndex = min(endIndex + 1, instructionList.count - 1)
            let floorIndices = Array(startIndex...displayEndIndex)

//            let floorIndices = instructionList.enumerated().compactMap { $1.floorId == currentTargetFloor ? $0 : nil }
//            let floorIndices = instructionList.enumerated().compactMap { (idx, inst) in
//                let instFloor = inst.floorId ?? ""
//                return instFloor == currentTargetFloor ? idx : nil
//            }
            
            let floorPoints = floorIndices.compactMap { instructionPoints.indices.contains($0) ? instructionPoints[$0] : nil }
//            let floorInstructions = floorIndices.compactMap { instructionList.indices.contains($0) ? instructionList[$0] : nil }
            let floorInstructions = Array(startIndex...endIndex).compactMap { instructionList.indices.contains($0) ? instructionList[$0] : nil }
            
            guard floorPoints.count >= 2 else { return }

            let targetBearing = Float(normalizedBearingDegrees(from: floorPoints[0], to: floorPoints[1]))
            let targetDirection = getDirectionStringInMultipleLanguages(from: floorPoints[0], to: floorPoints[1], languageCode: languageCode)

            if !isCalibrated && !currentTargetFloor.isEmpty {
                let diff = abs(Float(compassClassDegrees) - targetBearing)
                let normalizedDiff = min(diff, 360 - diff)

                // Using 15.0 tolerance from your OneByOne code
                if normalizedDiff < 15.0 {
                    isShowingDirectionDegree = "\(translationClass.almostThere(code: languageCode)).\n⏳ (\(offsetSamples.count)/20)"

                    let cameraTransform = frame.camera.transform
                    let arKitYaw = atan2(-cameraTransform.columns.2.x, -cameraTransform.columns.2.z)
                    let arKitDegrees = Double(arKitYaw) * 180 / .pi
                    
                    // CALCULATION FROM OneByOne: (Heading + ARKitDegrees)
                    let currentStableNorth = (compassClassDegrees + arKitDegrees + 360).truncatingRemainder(dividingBy: 360)
                    offsetSamples.append(currentStableNorth)

                    if offsetSamples.count >= 20 {
                        let averageNorth = offsetSamples.reduce(0, +) / Double(offsetSamples.count)
                        
                        // OFFSET FROM OneByOne: (Average - 180.0)
                        self.arWorldRotationOffset = Float(averageNorth - 180.0)
                        let rotationAngleRad = Float(self.arWorldRotationOffset) * .pi / 180
                        
                        isCalibrated = true
                        isDrawingStarted = true
                        isShowingDirectionDegree = ""
                        
                        isShowingArrowAllAtOnceGemini(instructions: floorInstructions, points: floorPoints, arWorldRotation: rotationAngleRad)
                        isShowingSmoothRectangleRoadAllAtOnceGemini(instructions: floorInstructions, points: floorPoints, arWorldRotation: rotationAngleRad)
                    }
                } else {
                    if !currentTargetFloor.isEmpty {
                        isShowingDirectionDegree = "\(translationClass.faceYouriPhone(code: languageCode))\n\(targetDirection) (\(Int(targetBearing))°)"
                    }
                    offsetSamples.removeAll()
                }
            }
        }
        
        func resetARDrawing() {
            guard let arView = arView else { return }
            
            // Stop any current drawing processes
            isDrawingStarted = false
            isCalibrated = false
            offsetSamples.removeAll()
            animationCancellables.removeAll()
            
            // Remove all existing 3D content to prevent overlapping
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()
            arView.scene.anchors.removeAll()
            
            isShowingDirectionDegree = ""
            
            print("🧹 AR Scene cleared for new floor/segment.")
        }
        
        @MainActor // Use this attribute to ensure the whole function runs on the UI thread
        func endARNavigation() {
            // 1. Kill the background task immediately
            roadDrawingTask?.cancel()
            arrowDrawingTask?.cancel()
            roadDrawingTask = nil
            arrowDrawingTask = nil
            
            // 1. Stop Logic (Sync)
            self.isDrawingStarted = false
            self.isCalibrated = false
            self.isWaitingForFloorConfirmation = false
            self.offsetSamples.removeAll()
            self.animationCancellables.removeAll()
            self.lastRenderedFloorId = nil
            self.isShowingDirectionDegree = ""
            self.arWorldRotationOffset = 0
            
            // 2. Clear Scene
            guard let arView = self.arView else {
                print("⚠️ Failed to clear: arView is nil")
                return
            }
            
            // Clear the delegate to stop session updates
            arView.session.delegate = nil

            // This is the "Nuclear Option" that removes EVERYTHING
            arView.scene.anchors.removeAll()
            
            // Clear tracked references
            self.instructionArrows.removeAll()
            
            print("🛑 AR Navigation: Scene Wiped and Logic Stopped.")
        }
        /// End of without View Model
        
        /// Works - Don't delete this one - second version
        func isShowingSmoothRectangleRoadAllAtOnce(
            instructions: [MXMInstruction],
            points: [MXMGeoPoint]
        ) {
            guard let arView else { return }
            guard points.count >= 2 else { return } // Need at least two points for a direction

            // Remove old roads
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()

            let baseLat = Float(points[0].latitude)
            let baseLon = Float(points[0].longitude)

            let roadWidth: Float = 1.6
            let roadThickness: Float = 0.02
            let roadColor = UIColor.systemBlue.withAlphaComponent(0.95)

            // --- NEW: Calculate and Log Initial Bearing Direction ---
            if points.count >= 2 {
                let segmentDeg = normalizedBearingDegrees(from: points[0], to: points[1])
                let initialDirection = getDirectionString(from: points[0], to: points[1])
                print("➡️ Initial AR Road Direction: \(initialDirection), with degrees of: \(segmentDeg)")
                // You can use this 'initialDirection' to display a message to the user here.
            }
            // --------------------------------------------------------

            Task {
                // P_start is the starting point of the current segment.
                // Initialize P_start to the AR World Origin (User's initial position).
                var P_start = SIMD3<Float>(0, -5, 0)
                
                // Loop runs for every point in the path, drawing a segment leading TO that point.
                for i in 0 ..< points.count {
                    
                    // P_end is the destination point of the current segment (points[i]'s local position).
                    let P_end = localPosition(from: points[i], baseLat: baseLat, baseLon: baseLon)
                    
                    // ... (Rest of the road drawing logic remains the same) ...
                    
                    // If i > 0, we draw the segment from P_start (points[i-1]) to P_end (points[i]).
                    if i > 0 {
                        let instructionIndex = i - 1
                        
                        let isTurn = (instructionIndex >= 0 && instructionIndex < instructions.count) ?
                            (instructions[instructionIndex].text?.lowercased().contains("turn left") ?? false ||
                             instructions[instructionIndex].text?.lowercased().contains("turn right") ?? false ||
                             instructions[instructionIndex].text?.lowercased().contains("keep") ?? false ||
                             instructions[instructionIndex].text?.lowercased().contains("arrive") ?? false)
                            : false

                        if isTurn {
                            let mid = SIMD3<Float>((P_start.x + P_end.x)/2, -5, (P_start.z + P_end.z)/2)
                            await MainActor.run {
                                addSharpTurn(from: P_start, via: mid, to: P_end, roadWidth: roadWidth, roadThickness: roadThickness, roadColor: roadColor, to: arView)
                            }
                        } else {
                            await MainActor.run {
                                addStraightRoad(start: P_start, end: P_end, roadWidth: roadWidth, roadThickness: roadThickness, roadColor: roadColor, to: arView)
                            }
                        }
                    } else {
                         // First segment (i=0): User origin (0, -5, 0) to points[0]
                        await MainActor.run {
                            addStraightRoad(start: P_start, end: P_end, roadWidth: roadWidth, roadThickness: roadThickness, roadColor: roadColor, to: arView)
                        }
                    }
                    
                    // Add Junction Plate
                    await MainActor.run {
                        addJunctionPlate(at: P_end, roadWidth: roadWidth, roadThickness: roadThickness, roadColor: roadColor, to: arView)
                    }
                    
                    // Update the starting point for the next iteration
                    P_start = P_end
                }
                
                print("✨ Roads (straight + curve) successfully drawn")
            }
        }
        
        @MainActor
        func addStraightRoad(
            start: SIMD3<Float>,
            end: SIMD3<Float>,
            roadWidth: Float,
            roadThickness: Float,
            roadColor: UIColor,
            to arView: ARView
        ) {
            let dx = end.x - start.x
            let dz = end.z - start.z
            let length = sqrt(dx*dx + dz*dz)

            let angle = atan2(dx, dz)
            let mid = SIMD3<Float>(
                (start.x + end.x) / 2,
                start.y,
                (start.z + end.z) / 2
            )

            let material = SimpleMaterial(color: roadColor, roughness: 0.5, isMetallic: false)

            let road = ModelEntity(
                mesh: .generateBox(size: [roadWidth, roadThickness, length]),
                materials: [material]
            )

            road.position = [0, -0.01, 0]
            road.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])

            let anchor = AnchorEntity(world: mid)
            anchor.addChild(road)
            arView.scene.addAnchor(anchor)
        }
        
        @MainActor
        func addSharpTurn(
            from p0: SIMD3<Float>,
            via p1: SIMD3<Float>,
            to p2: SIMD3<Float>,
            roadWidth: Float,
            roadThickness: Float,
            roadColor: UIColor,
            to arView: ARView
        ) {
            // 1️⃣ Segment before the turn (p0 → p1)
            Task(operation: {
                await MainActor.run {
                    addStraightRoad(
                        start: p0,
                        end: p1,
                        roadWidth: roadWidth,
                        roadThickness: roadThickness,
                        roadColor: roadColor,
                        to: arView
                    )

                    // 2️⃣ Segment after the turn (p1 → p2)
                    addStraightRoad(
                        start: p1,
                        end: p2,
                        roadWidth: roadWidth,
                        roadThickness: roadThickness,
                        roadColor: roadColor,
                        to: arView
                    )
                }
            })
        }

        func addCornerPlate(
            at p: SIMD3<Float>,
            roadWidth: Float,
            roadThickness: Float,
            roadColor: UIColor,
            to arView: ARView
        ) {
            let size: Float = roadWidth * 0.9  // small square tile
            let material = SimpleMaterial(color: roadColor, roughness: 0.5, isMetallic: false)

            let tile = ModelEntity(
                mesh: .generateBox(size: [size, roadThickness, size]),
                materials: [material]
            )

            tile.position = [0, -0.01, 0]

            let anchor = AnchorEntity(world: p)
            anchor.addChild(tile)
            arView.scene.addAnchor(anchor)
        }
        
        @MainActor
        func addJunctionPlate(
            at p_junction: SIMD3<Float>,
            roadWidth: Float,
            roadThickness: Float,
            roadColor: UIColor,
            to arView: ARView
        ) {
            // 1. Create a high-quality material
            // Increasing roughness slightly prevents distracting mirror-like reflections in AR
            let material = SimpleMaterial(color: roadColor, roughness: 0.6, isMetallic: false)

            // 2. Generate a Cylinder (Disk)
            // The radius should be exactly half the road width to perfectly flush with the edges
            let junctionMesh = MeshResource.generateCylinder(
                height: roadThickness,
                radius: roadWidth / 2
            )

            let junctionEntity = ModelEntity(mesh: junctionMesh, materials: [material])

            // 3. Offset and Placement
            // We lower it slightly (just like your road) to avoid z-fighting with the ground,
            // and we ensure it is at the exact same Y-level as your road segments.
            junctionEntity.position = [0, -0.01, 0]

            // 4. Anchor to the world
            let anchor = AnchorEntity(world: p_junction)
            anchor.addChild(junctionEntity)
            arView.scene.addAnchor(anchor)
        }
        
        func addStraightRoadForIsShowingOneByOneARNavigation(
            start: SIMD3<Float>,
            end: SIMD3<Float>,
            roadWidth: Float,
            roadThickness: Float,
            roadColor: UIColor,
            to arView: ARView
        ) {
            let dx = end.x - start.x
            let dz = end.z - start.z
            let length = sqrt(dx*dx + dz*dz)

            let angle = atan2(dx, dz)
            
            // FIX: Use the Y from the start/end points
            let mid = SIMD3<Float>(
                (start.x + end.x) / 2,
                start.y, // Changed from -5 to start.y
                (start.z + end.z) / 2
            )

            let material = SimpleMaterial(color: roadColor, roughness: 0.5, isMetallic: false)
            let road = ModelEntity(
                mesh: .generateBox(size: [roadWidth, roadThickness, length]),
                materials: [material]
            )

            // Orientation and placement
            road.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])

            let anchor = AnchorEntity(world: mid)
            anchor.addChild(road)
            arView.scene.addAnchor(anchor)
            
            // Add to your tracking array to allow for floor-change clearing
            self.instructionArrows.append(anchor)
        }

        // Simple lerp
        func mix(_ a: SIMD3<Float>, _ b: SIMD3<Float>, t: Float) -> SIMD3<Float> {
            return a + (b - a) * t
        }
        
        func localPosition(from point: MXMGeoPoint, baseLat: Float, baseLon: Float) -> SIMD3<Float> {
            let x = (Float(point.longitude) - baseLon) * 100_000
            let z = (baseLat - Float(point.latitude)) * 100_000
            return [x, -5, z]
        }

        private func worldPos(from point: MXMGeoPoint, baseLat: Float, baseLon: Float) -> SIMD3<Float> {
            let x = (Float(point.longitude) - baseLon) * 100_000
            let z = (baseLat - Float(point.latitude)) * 100_000
            return SIMD3<Float>(x, -5, z)
        }

        private func makeRoadSegment(
            width: Float,
            thickness: Float,
            length: Float,
            color: UIColor
        ) -> ModelEntity {

            let material = SimpleMaterial(color: color, roughness: 0.5, isMetallic: false)

            let road = ModelEntity(
                mesh: .generateBox(size: [width, thickness, length]),
                materials: [material]
            )

            road.position = [0, -0.01, 0]
            return road
        }

        private func addDashes(to road: ModelEntity, segmentLength: Float) {

            let dashMaterial = SimpleMaterial(color: .white, isMetallic: false)

            let dashLength: Float = 0.4
            let dashSpacing: Float = 0.5
            let dashThickness: Float = 0.01

            let dashCount = Int(segmentLength / (dashLength + dashSpacing))

            for d in 0..<dashCount {
                let zPos = -segmentLength/2 + Float(d) * (dashLength + dashSpacing)

                let dash = ModelEntity(
                    mesh: .generateBox(size: [0.05, dashThickness, dashLength]),
                    materials: [dashMaterial]
                )

                dash.position = [0, 0.02, zPos]
                road.addChild(dash)
            }
        }
        
        func calculateBearingAndEndPosition(
            from start: SIMD3<Float>,
            to target: SIMD3<Float>,
            length: Float
        ) -> (endPoint: SIMD3<Float>, angle: Float) {
            
            // Calculate vector from start to target in the X-Z plane
            let dx = target.x - start.x
            let dz = target.z - start.z
            
            // Calculate the angle (bearing) from Z-axis (North)
            let angle = atan2(dx, dz)
            
            // Calculate the components of a vector with the desired 'length'
            let endX = start.x + length * sin(angle)
            let endZ = start.z + length * cos(angle)
            
            // The Y-coordinate should match the Z-fighting offset of your roads (-5)
            let endPoint = SIMD3<Float>(endX, -5, endZ)
            
            return (endPoint, angle)
        }
        
        func isDirectionMatch(compass: Float, target: Float, tolerance: Float = 10) -> Bool {
            // Normalize both angles to 0–360
            let c = fmodf((compass + 360), 360)
            let t = fmodf((target + 360), 360)

            let diff = abs(c - t)
            return diff < tolerance || abs(diff - 360) < tolerance
        }
        
        func angleDifference(_ a: Float, _ b: Float) -> Float {
            let diff = fmod((a - b + 540), 360) - 180
            return abs(diff)
        }
        
        func isApproximatelyEqual(_ a: Float, _ b: Float, tolerance: Float = 5) -> Bool {
            return abs(a - b) <= tolerance
        }
        
        func normalizeAngle(_ deg: Float) -> Float {
            let fixed = fmod(deg, 360)
            return fixed < 0 ? fixed + 360 : fixed
        }

        private func getBearing(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
            let fromLat = from.latitude.degreesToRadians
            let fromLon = from.longitude.degreesToRadians
            let toLat = to.latitude.degreesToRadians
            let toLon = to.longitude.degreesToRadians

            let dLon = toLon - fromLon
            let y = sin(dLon) * cos(toLat)
            let x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLon)
            return (atan2(y, x).radiansToDegrees + 360).truncatingRemainder(dividingBy: 360)
        }
        
        func getRelativePosition(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> SIMD3<Float> {
            let fromLocation = CLLocation(latitude: from.latitude, longitude: from.longitude)
            let toLocation = CLLocation(latitude: to.latitude, longitude: to.longitude)
            let distance = fromLocation.distance(from: toLocation)

            let bearing = getBearing(from: from, to: to)

            let x = Float(distance * sin(bearing.degreesToRadians))
            let z = Float(distance * cos(bearing.degreesToRadians))
            return [x, 0, -z]
        }
        
        func interpolatePoints(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D, spacing: Double = 2.0) -> [CLLocationCoordinate2D] {
            let startLocation = CLLocation(latitude: from.latitude, longitude: from.longitude)
            let endLocation = CLLocation(latitude: to.latitude, longitude: to.longitude)
            let distance = startLocation.distance(from: endLocation)

            let bearing = getBearing(from: from, to: to)
            var points: [CLLocationCoordinate2D] = []

            let steps = Int(distance / spacing)
            for i in 0...steps {
                let stepDistance = spacing * Double(i)
                let point = startLocation.coordinate.destinationPoint(bearing: bearing, distanceMeters: stepDistance)
                points.append(point)
            }

            return points
        }
        
        func getDirectionStringInMultipleLanguages(from a: MXMGeoPoint, to b: MXMGeoPoint, languageCode: String) -> String {
            let degrees = compassDegrees(from: a, to: b)
            let directionKey = compassDirectionDetails(degrees: degrees) // e.g., "NE"

            // Define translations
            let translations: [String: [String: String]] = [
                "N":  ["en": "North",      "zh-Hant": "北",   "zh-Hans": "北"],
                "NE": ["en": "North-East", "zh-Hant": "東北", "zh-Hans": "东北"],
                "E":  ["en": "East",       "zh-Hant": "東",   "zh-Hans": "东"],
                "SE": ["en": "South-East", "zh-Hant": "東南", "zh-Hans": "东南"],
                "S":  ["en": "South",      "zh-Hant": "南",   "zh-Hans": "南"],
                "SW": ["en": "South-West", "zh-Hant": "西南", "zh-Hans": "西南"],
                "W":  ["en": "West",       "zh-Hant": "西",   "zh-Hans": "西"],
                "NW": ["en": "North-West", "zh-Hant": "西北", "zh-Hans": "西北"]
            ]

            // Determine the simple language key (mapping zh variants to a common key if desired)
            let lang = languageCode.contains("zh-Han") ? languageCode : "en"

            // Fetch translation with fallbacks
            return translations[directionKey]?[lang] ?? translations[directionKey]?["en"] ?? "Unknown"
        }
        
        func getDirectionString(from a: MXMGeoPoint, to b: MXMGeoPoint) -> String {
            // 1. Calculate the bearing in degrees (0 to 360)
            let degrees = compassDegrees(from: a, to: b)
            
            // 2. Convert degrees to a direction string (e.g., "NE", "S")
            let direction = compassDirectionDetails(degrees: degrees)
            
            // 3. (Optional) Add a descriptive text
            let fullDirection: String
            switch direction {
                case "N": fullDirection = "North"
                case "NE": fullDirection = "North-East"
                case "E": fullDirection = "East"
                case "SE": fullDirection = "South-East"
                case "S": fullDirection = "South"
                case "SW": fullDirection = "South-West"
                case "W": fullDirection = "West"
                case "NW": fullDirection = "North-West"
                default: fullDirection = "Unknown Direction"
            }
            
            return fullDirection
        }
        
        func animateBlinkingRoad(planes: [ModelEntity], arView: ARView) {
            blinkingCancellables.forEach { $0.cancel() }
            blinkingCancellables.removeAll()

            var timeElapsed: Float = 0
            let waveDuration: Float = 2.0 // duration of one wave pass
            let totalPlanes = planes.count

            let cancellable = arView.scene.subscribe(to: SceneEvents.Update.self) { event in
                timeElapsed += Float(event.deltaTime)

                for (index, plane) in planes.enumerated() {
                    let progress = (timeElapsed - Float(index) * 0.1).truncatingRemainder(dividingBy: waveDuration) / waveDuration
                    let sine = sin(progress * 2 * .pi)

                    // Normalize sine wave to 0...1, then map to 0.3...1.0
                    let normalized = max(0, (sine + 1) / 2)
                    let alpha = 0.2 + 0.8 * normalized

                    let material = SimpleMaterial(color: .blue.withAlphaComponent(CGFloat(alpha)), isMetallic: false)
                    plane.model?.materials = [material]
                }
            }

            blinkingCancellables.append(cancellable)
        }
        
        func startInfiniteRotation(for entity: ModelEntity, in arView: ARView) {
            let rotationSpeed: Float = .pi / 4 // 45° per second

            let cancellable = arView.scene.subscribe(to: SceneEvents.Update.self) { event in
                let deltaTime = Float(event.deltaTime)
                let rotation = simd_quatf(angle: rotationSpeed * deltaTime, axis: [0, 1, 0])
                entity.transform.rotation *= rotation
            }

            animationCancellables.append(cancellable)
        }
        
        func startInfiniteRotationForArrowUpAndDown(for entity: ModelEntity, in arView: ARView) {
            let rotationSpeed: Float = .pi / 4 // 90° per second for better visibility

            let cancellable = arView.scene.subscribe(to: SceneEvents.Update.self) { event in
                let deltaTime = Float(event.deltaTime)
                
                // 1. Create the rotation step
                let deltaRotation = simd_quatf(angle: rotationSpeed * deltaTime, axis: [0, 1, 0])
                
                // 2. APPLY ON THE LEFT: This forces the spin to stay horizontal (World Y-axis)
                // regardless of whether the arrow is tilted up or down.
                entity.orientation = deltaRotation * entity.orientation
            }

            animationCancellables.append(cancellable)
        }
        
        func startJumpingAnimation(for entity: ModelEntity, in arView: ARView, height: Float = 0.05, duration: TimeInterval = 1.0) {
            var elapsedTime: Float = 0
            let originalY = entity.position.y

            let cancellable = arView.scene.subscribe(to: SceneEvents.Update.self) { event in
                elapsedTime += Float(event.deltaTime)

                // Calculate normalized time (0 to 1)
                let t = (elapsedTime.truncatingRemainder(dividingBy: Float(duration))) / Float(duration)

                // Use sine wave for smooth up-and-down motion
                let yOffset = sin(t * .pi * 2) * height

                entity.position.y = originalY + yOffset
            }

            jumpCancellables.append(cancellable)
        }
        
        func compassDirection(from a: MXMGeoPoint, to b: MXMGeoPoint) -> String {
            let dLat = b.latitude  - a.latitude   // North/South movement
            let dLon = b.longitude - a.longitude  // East/West movement

            // Threshold to ignore tiny differences
            let threshold = 0.00001

            var vertical = ""
            var horizontal = ""

            // NORTH / SOUTH
            if dLat > threshold {
                vertical = "North"
            } else if dLat < -threshold {
                vertical = "South"
            }

            // EAST / WEST
            if dLon > threshold {
                horizontal = "East"
            } else if dLon < -threshold {
                horizontal = "West"
            }

            // Combine
            if vertical.isEmpty && horizontal.isEmpty {
                return "Same location"
            }
            return vertical + horizontal
        }
        
        func compassDegrees(from a: MXMGeoPoint, to b: MXMGeoPoint) -> Double {
            let lat1 = a.latitude * .pi / 180
            let lat2 = b.latitude * .pi / 180
            let dLon = (b.longitude - a.longitude) * .pi / 180

            let y = sin(dLon) * cos(lat2)
            let x = cos(lat1) * sin(lat2) -
                    sin(lat1) * cos(lat2) * cos(dLon)

            var degrees = atan2(y, x) * 180 / .pi
            if degrees < 0 { degrees += 360 }
            return degrees
        }
        
        func compassDirectionDetails(degrees: Double) -> String {
            switch degrees {
            case 0..<22.5, 337.5...360: return "N"
            case 22.5..<67.5: return "NE"
            case 67.5..<112.5: return "E"
            case 112.5..<157.5: return "SE"
            case 157.5..<202.5: return "S"
            case 202.5..<247.5: return "SW"
            case 247.5..<292.5: return "W"
            case 292.5..<337.5: return "NW"
            default: return "Unknown"
            }
        }

        func normalizedBearingDegrees(from a: MXMGeoPoint, to b: MXMGeoPoint) -> Float {
            let lat1 = a.latitude * .pi / 180
            let lat2 = b.latitude * .pi / 180
            let dLon = (b.longitude - a.longitude) * .pi / 180

            let y = sin(dLon) * cos(lat2)
            let x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

            var bearing = atan2(y, x) * 180 / .pi   // -180 to +180
            if bearing < 0 { bearing += 360 }       // convert → 0–360
            return Float(bearing)
        }
        
        /// Start of: Old one
        /// Show Road path with Floor ID
        func loadCircleDirectionPathOneByOneWithFloorId(from points: [MXMGeoPoint], instruction: [MXMInstruction], currentIndex: Int) {
            guard let arView else { return }
            guard points.indices.contains(currentIndex), points.indices.contains(currentIndex + 1),
                  instruction.indices.contains(currentIndex) else {
                print("❌ Invalid index for path segment")
                return
            }

                Task {
                    let currentFloorId = instruction[currentIndex].floorId ?? "unknown"

                    // Remove previous floor's anchors only if the floor changed
                    if let previousFloorId = currentRenderedFloorId,
                       previousFloorId != currentFloorId,
                       let anchorsToRemove = instructionArrowsByFloor[previousFloorId] {
                        for anchor in anchorsToRemove {
                            arView.scene.removeAnchor(anchor)
                        }
                        instructionArrowsByFloor[previousFloorId] = nil
                        print("🔄 Floor changed: cleared path for previous floor \(previousFloorId)")
                    }

                    // Update current rendered floor
                    currentRenderedFloorId = currentFloorId

                    let baseLat = Float(points[0].latitude)
                    let baseLon = Float(points[0].longitude)

                    let circleRadius: Float = 0.5
                    let circleHeight: Float = 0.01
                    let circleSpacing: Float = 0.8
                    let yOffset: Float = -0.005
                    let anchorHeight: Float = -5.0

                    let start = points[currentIndex]
                    let end = points[currentIndex + 1]

                    let deltaLat = Float(end.latitude - start.latitude)
                    let deltaLon = Float(end.longitude - start.longitude)
                    let segmentDistance = sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111_000

                    let direction = normalize(SIMD3<Float>(x: deltaLon, y: 0, z: deltaLat))
//                    let angle = atan2(direction.x, direction.z)
                    let angle = atan2(deltaLon, deltaLat)

                    let stepDistance = (circleRadius * 2) + circleSpacing
                    let stepCount = max(1, Int((segmentDistance - 0.5) / stepDistance))

                    var newCircles: [ModelEntity] = []
                    var floorAnchors: [AnchorEntity] = []

                    for step in 0..<stepCount {
                        await MainActor.run {
                            let t = (Float(step) + 0.5) / Float(stepCount + 1)
                            let lat = Float(start.latitude) + deltaLat * t
                            let lon = Float(start.longitude) + deltaLon * t

                            let worldX = (lon - baseLon) * 100_000
                            let worldZ = (lat - baseLat) * 100_000

                            let circleMaterial = SimpleMaterial(
                                color: .systemBlue.withAlphaComponent(0.6),
                                isMetallic: false
                            )

                            let circle = ModelEntity(
                                mesh: .generateCylinder(height: circleHeight, radius: circleRadius),
                                materials: [circleMaterial]
                            )
                            circle.position = [0, yOffset, 0]
                            circle.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])

                            let anchor = AnchorEntity(world: [worldX, anchorHeight, worldZ])
                            anchor.addChild(circle)
                            arView.scene.addAnchor(anchor)

                            instructionArrows.append(anchor) // ✅ Still using your original list
                            floorAnchors.append(anchor)      // ✅ Also track per floor
                            newCircles.append(circle)
                        }
                    }

                    // Save anchors grouped by current floor
                    instructionArrowsByFloor[currentFloorId, default: []].append(contentsOf: floorAnchors)

                    animateBlinkingRoad(planes: newCircles, arView: arView)
                    print("✅ Loaded segment on floor \(currentFloorId) from index \(currentIndex) → \(currentIndex + 1)")
                }
        }
        
        /// Works with Compass
        func isShowingCircleDirectionPathOneByOneGemini(
            from points: [MXMGeoPoint],
            instruction: [MXMInstruction],
            currentIndex: Int,
            arWorldRotation: Float // NEW: The rotation angle to align the AR world
        ) {
            guard let arView else { return }
            guard points.indices.contains(currentIndex), points.indices.contains(currentIndex + 1),
                  instruction.indices.contains(currentIndex) else {
                print("❌ Invalid index for path segment")
                return
            }

            Task {
                let currentFloorId = instruction[currentIndex].floorId ?? "unknown"

                // Remove previous floor's anchors only if the floor changed
                if let previousFloorId = currentRenderedFloorId,
                   previousFloorId != currentFloorId,
                   let anchorsToRemove = instructionArrowsByFloor[previousFloorId] {
                    for anchor in anchorsToRemove {
                        arView.scene.removeAnchor(anchor)
                    }
                    instructionArrowsByFloor[previousFloorId] = nil
                    print("🔄 Floor changed: cleared path for previous floor \(previousFloorId)")
                }

                // Update current rendered floor
                currentRenderedFloorId = currentFloorId

                // Base coordinates for local conversion
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)

                // Path constants
                let circleRadius: Float = 0.5
                let circleHeight: Float = 0.01
                let circleSpacing: Float = 0.8
                let yOffset: Float = -0.005
                let anchorHeight: Float = -5.0

                let start = points[currentIndex]
                let end = points[currentIndex + 1]

                let deltaLat = Float(end.latitude - start.latitude)
                let deltaLon = Float(end.longitude - start.longitude)

                // 1. Calculate the local angle for the path segment
                let dx = Float(end.longitude - start.longitude)
                let dz = Float(start.latitude  - end.latitude)  // north becomes negative -> south positive

                let angle = atan2(dx, dz) // Local rotation for the segment

                // 2. Determine circle count for spacing
                let stepDistance = (circleRadius * 2) + circleSpacing
                let segmentDistance = sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111_000 // Approximate distance in meters
                let stepCount = max(1, Int((segmentDistance - 0.5) / stepDistance))

                var newCircles: [ModelEntity] = []
                var floorAnchors: [AnchorEntity] = []
                
                // --- CRITICAL ROTATION SETUP (Do this once outside the loop) ---
                let cosTheta = cos(arWorldRotation)
                let sinTheta = sin(arWorldRotation)
                
                for step in 0..<stepCount {
                    await MainActor.run {
                        // Interpolate position along the segment
                        let t = (Float(step) + 0.5) / Float(stepCount + 1)
                        let lat = Float(start.latitude) + deltaLat * t
                        let lon = Float(start.longitude) + deltaLon * t

                        let worldX = (Float(lon) - baseLon) * 100_000
                        let worldZ = (baseLat - Float(lat)) * 100_000

                        let cosTheta = cos(arWorldRotation)
                        let sinTheta = sin(arWorldRotation)

                        // THIS IS THE CLOCKWISE MATRIX
                        let rotatedX = worldX * cosTheta + worldZ * sinTheta
                        let rotatedZ = -worldX * sinTheta + worldZ * cosTheta
                        
                        // Create visual circle entity
                        let circleMaterial = SimpleMaterial(
                            color: .systemBlue.withAlphaComponent(0.6),
                            isMetallic: false
                        )

                        let circle = ModelEntity(
                            mesh: .generateCylinder(height: circleHeight, radius: circleRadius),
                            materials: [circleMaterial]
                        )
                        
                        // Apply the local segment rotation (which is correct)
                        circle.position = [0, yOffset, 0]
                        circle.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])

                        // 3. Create Anchor Entity at the ROTATED position
                        let finalCirclePosition = SIMD3<Float>(rotatedX, anchorHeight, rotatedZ)
                        let anchor = AnchorEntity(world: finalCirclePosition)
                        
                        // ❗ REMOVED THE INCORRECT LINE:
                        // anchor.orientation = simd_quatf(angle: arWorldRotation, axis: [0, 1, 0])

                        // 4. Add to scene and track lists
                        anchor.addChild(circle)
                        arView.scene.addAnchor(anchor)

                        instructionArrows.append(anchor)
                        floorAnchors.append(anchor)
                        newCircles.append(circle)
                    }
                }

                // Save anchors grouped by current floor
                instructionArrowsByFloor[currentFloorId, default: []].append(contentsOf: floorAnchors)

                animateBlinkingRoad(planes: newCircles, arView: arView)
                
                print("✅ Loaded segment on floor \(currentFloorId) from index \(currentIndex) → \(currentIndex + 1)")
            }
        }
        
        func isShowingArrowAllAtOnce(
            instructions: [MXMInstruction],
            points: [MXMGeoPoint]
        ) {
            guard let arView else { return }

            // -------------------------------------------------------
            // 0. Ensure we actually HAVE points!
            // -------------------------------------------------------
            guard points.count >= 1 else {
                print("⚠️ No points available yet → skip AR")
                return
            }

            // Ensure ARKit frame is ready
            guard arView.session.currentFrame != nil else {
                print("⏳ Waiting for ARKit frame…")
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    self.isShowingArrowAllAtOnce(instructions: instructions, points: points)
                }
                return
            }

            // Clear old anchors
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()

            Task {

                // -------------------------------------------------------
                // Use the FIRST point as base reference safely
                // -------------------------------------------------------
                let baseLat = Float(points.first!.latitude)
                let baseLon = Float(points.first!.longitude)

                var newAnchors: [AnchorEntity] = []

                // Loop through ALL instructions
                for i in 0..<instructions.count {

                    guard points.indices.contains(i) else {
                        print("⚠️ No matching point for instruction \(i)")
                        continue
                    }

                    let current = points[i]
                    let text = instructions[i].text?.lowercased() ?? ""

                    // Filter for instructions that should show arrows
                    let shouldShow =
                        text.contains("straight") ||
                        text.contains("steps ahead") ||
                        text.contains("turn") ||
                        text.contains("keep") ||
                        text.contains("arrive") ||
                        text.contains("take stairs") ||
                        text.contains("take elevator")

                    if !shouldShow { continue }

                    // -------------------------------------------------------
                    // 1. WORLD POSITION
                    // -------------------------------------------------------
                    let worldX = (Float(current.longitude) - baseLon) * 100_000
                    let worldZ = (baseLat - Float(current.latitude)) * 100_000
                    let arrowPos = SIMD3<Float>(worldX, -0.5, worldZ)

                    // -------------------------------------------------------
                    // 2. ROTATION — only if there's a future point
                    // -------------------------------------------------------
                    var angle: Float = 0

                    if i + 1 < points.count {
                        let future = points[i + 1]

                        let dx = Float(future.longitude - current.longitude)
                        let dz = Float(current.latitude - future.latitude)

                        angle = atan2(dx, dz) - .pi / 2
                    }

                    // -------------------------------------------------------
                    // 3. Special arrows
                    // -------------------------------------------------------
                    var arrowName: String? = nil

                    if text.contains("arrive at destination")     { arrowName = "arrive_at_destination" }
                    else if text.contains("take stairs up")       { arrowName = "go_upstair_arrow" }
                    else if text.contains("take stairs down")     { arrowName = "go_downstair_arrow_copy" }
                    else if text.contains("take elevator up")     { arrowName = "go_upstair_arrow" }
                    else if text.contains("take elevator down")   { arrowName = "go_downstair_arrow_copy" }

                    do {
                        let arrow = try await ModelEntity(named: arrowName ?? "direction_arrow_horizontal")

                        await MainActor.run {
                            arrow.scale = (arrowName == "direction_arrow_horizontal") ? [0.1, 0.1, 0.1] : [0.009, 0.009, 0.009]
                            arrow.position = [0, 0.01, 0]
                            arrow.orientation = simd_quatf(angle: angle, axis: [0, 1, 0]) // ← FIXED ANGLE

                            let anchor = AnchorEntity(world: arrowPos)
                            anchor.addChild(arrow)
                            arView.scene.addAnchor(anchor)
                            newAnchors.append(anchor)
                        }

                    } catch {
                        print("❌ Failed to load arrow: \(error)")
                    }
                }

                instructionArrows = newAnchors
                print("📌 Loaded \(newAnchors.count) turn arrows")
            }
        }
        
        func isShowingTurnArrowsOneByOne(
            instructions: [MXMInstruction],
            points: [MXMGeoPoint],
            currentIndex: Int
        ) {
            guard let arView else { return }

            // -----------------------------------------
            // 1. Wait until ARKit actually has a frame
            // -----------------------------------------
            guard let frame = arView.session.currentFrame else {
                print("⏳ Waiting for ARKit frame…")
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    self.isShowingTurnArrowsOneByOne(
                        instructions: instructions,
                        points: points,
                        currentIndex: currentIndex
                    )
                }
                return
            }

            // -----------------------------------------
            // 2. Find next instruction index
            // -----------------------------------------
            var nextIndex = currentIndex
            while nextIndex < instructions.count {
                if let text = instructions[nextIndex].text?.lowercased(),
                   text.contains("straight") ||
                   text.contains("steps ahead") ||
                   text.contains("turn") ||
                   text.contains("keep") ||
                   text.contains("arrive") ||
                   text.contains("take stairs") ||
                   text.contains("take elevator") {
                    print("✅ Next index: \(nextIndex)")
                    break
                }
                nextIndex += 1
            }

            guard instructions.indices.contains(nextIndex),
                  points.indices.contains(nextIndex) else {
                print("❌ No valid next index")
                return
            }

            // -----------------------------------------
            // 3. Clear previous arrows
            // -----------------------------------------
            instructionArrows.forEach { arView.scene.removeAnchor($0) }
            instructionArrows.removeAll()

            Task {
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)

                let current = points[nextIndex]
                let futureIndex = min(nextIndex + 1, points.count - 1)
                let future = points[futureIndex]

                // -----------------------------------------
                // 4. SAME coordinate system as circle path
                // -----------------------------------------
                let worldX = (Float(current.longitude) - baseLon) * 100_000
                let worldZ = (baseLat - Float(current.latitude)) * 100_000
                let arrowPosition = SIMD3<Float>(worldX, -0.5, worldZ)

                // -----------------------------------------
                // 5. SAME angle formula as circle path
                // -----------------------------------------
                let dx = Float(future.longitude - current.longitude)     // East = +X
                let dz = Float(current.latitude - future.latitude)       // South = +Z

                let angle = atan2(dx, dz) - .pi / 2

                // -----------------------------------------
                // HANDLE SPECIAL ARROWS
                // -----------------------------------------
                let text = instructions[nextIndex].text?.lowercased() ?? ""
                var arrowName: String?

                if text.contains("arrive at destination") {
                    arrowName = "arrive_at_destination"
                }
                else if text.contains("take stairs up") {
                    arrowName = "go_upstair_arrow"
                }
                else if text.contains("take stairs down") {
                    arrowName = "go_downstair_arrow_copy"
                }
                else if text.contains("take elevator up") {
                    arrowName = "go_upstair_arrow"
                }
                else if text.contains("take elevator down") {
                    arrowName = "go_downstair_arrow_copy"
                }

                // -----------------------------------------
                // 6. LOAD SPECIAL ARROW IF APPLICABLE
                // -----------------------------------------
                if let arrowName = arrowName {
                    do {
                        let entity = try await ModelEntity(named: arrowName)

                        await MainActor.run {
                            entity.scale = [0.009, 0.009, 0.009]
                            entity.position = [0, 0.01, 0]

                            let anchor = AnchorEntity(world: arrowPosition)
                            anchor.addChild(entity)
                            arView.scene.addAnchor(anchor)

                            instructionArrows = [anchor]
                        }
                    } catch {
                        print("❌ Failed to load special arrow: \(error)")
                    }
                    return
                }

                // -----------------------------------------
                // 7. DEFAULT HORIZONTAL ARROW
                // -----------------------------------------
                do {
                    let arrow = try await ModelEntity(named: "direction_arrow_horizontal")

                    await MainActor.run {
                        arrow.scale = [0.1, 0.1, 0.1]
                        arrow.position = [0, 0.01, 0]
                        arrow.orientation = simd_quatf(angle: angle, axis: [0, 1, 0]) // ← FIXED ANGLE

                        let anchor = AnchorEntity(world: arrowPosition)
                        anchor.addChild(arrow)
                        arView.scene.addAnchor(anchor)
                        instructionArrows = [anchor]
                    }
                } catch {
                    print("❌ Failed to load direction_arrow_horizontal: \(error)")
                }
            }
        }
        
        func isShowingCircleDirectionPathOneByOne(from points: [MXMGeoPoint], instruction: [MXMInstruction], currentIndex: Int) {
            guard let arView else { return }
            guard points.indices.contains(currentIndex), points.indices.contains(currentIndex + 1),
                  instruction.indices.contains(currentIndex) else {
                print("❌ Invalid index for path segment")
                return
            }

                Task {
                    let currentFloorId = instruction[currentIndex].floorId ?? "unknown"

                    // Remove previous floor's anchors only if the floor changed
                    if let previousFloorId = currentRenderedFloorId,
                       previousFloorId != currentFloorId,
                       let anchorsToRemove = instructionArrowsByFloor[previousFloorId] {
                        for anchor in anchorsToRemove {
                            arView.scene.removeAnchor(anchor)
                        }
                        instructionArrowsByFloor[previousFloorId] = nil
                        print("🔄 Floor changed: cleared path for previous floor \(previousFloorId)")
                    }

                    // Update current rendered floor
                    currentRenderedFloorId = currentFloorId

                    let baseLat = Float(points[0].latitude)
                    let baseLon = Float(points[0].longitude)

                    let circleRadius: Float = 0.5
                    let circleHeight: Float = 0.01
                    let circleSpacing: Float = 0.8
                    let yOffset: Float = -0.005
                    let anchorHeight: Float = -5.0

                    let start = points[currentIndex]
                    let end = points[currentIndex + 1]

                    let deltaLat = Float(end.latitude - start.latitude)
                    let deltaLon = Float(end.longitude - start.longitude)

                    // FIX 2 – correct angle for ARKit coordinate system
                    let dx = Float(end.longitude - start.longitude)
                    let dz = Float(start.latitude  - end.latitude)   // north becomes negative -> south positive

                    let angle = atan2(dx, dz)

                    let stepDistance = (circleRadius * 2) + circleSpacing
                    
                    let segmentDistance = sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111_000
                    let stepCount = max(1, Int((segmentDistance - 0.5) / stepDistance))

                    var newCircles: [ModelEntity] = []
                    var floorAnchors: [AnchorEntity] = []

                    for step in 0..<stepCount {
                        await MainActor.run {
                            let t = (Float(step) + 0.5) / Float(stepCount + 1)
                            let lat = Float(start.latitude) + deltaLat * t
                            let lon = Float(start.longitude) + deltaLon * t

                            let worldX = (lon - baseLon) * 100_000
                            let worldZ = (lat - baseLat) * 100_000

                            let circleMaterial = SimpleMaterial(
                                color: .systemBlue.withAlphaComponent(0.6),
                                isMetallic: false
                            )

                            let circle = ModelEntity(
                                mesh: .generateCylinder(height: circleHeight, radius: circleRadius),
                                materials: [circleMaterial]
                            )
                            circle.position = [0, yOffset, 0]
                            circle.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])

                            let anchor = AnchorEntity(world: [worldX, anchorHeight, worldZ])
                            anchor.addChild(circle)
                            arView.scene.addAnchor(anchor)

                            instructionArrows.append(anchor) // ✅ Still using your original list
                            floorAnchors.append(anchor)      // ✅ Also track per floor
                            newCircles.append(circle)
                        }
                    }

                    // Save anchors grouped by current floor
                    instructionArrowsByFloor[currentFloorId, default: []].append(contentsOf: floorAnchors)

                    animateBlinkingRoad(planes: newCircles, arView: arView) /// for blinking animation on the road path
                    
                    print("✅ Loaded segment on floor \(currentFloorId) from index \(currentIndex) → \(currentIndex + 1)")
                }
        }
        
        func isShowingRectangleDirectionPathOneByOneGemini(
            from points: [MXMGeoPoint],
            instruction: [MXMInstruction],
            currentIndex: Int,
            arWorldRotation: Float
        ) {
            guard let arView else { return }
            guard points.indices.contains(currentIndex),
                  points.indices.contains(currentIndex + 1) else { return }

            Task {
                let currentFloorId = instruction[currentIndex].floorId ?? "unknown"

                // 1. Floor management (existing logic)
                if let previousFloorId = currentRenderedFloorId, previousFloorId != currentFloorId {
                    currentRenderedFloorId = currentFloorId
                }

                // 2. Setup Constants
                let baseLat = Float(points[0].latitude)
                let baseLon = Float(points[0].longitude)
                let cosTheta = cos(arWorldRotation)
                let sinTheta = sin(arWorldRotation)
                
                let roadWidth: Float = 1.6
                let roadThickness: Float = 0.02
                let roadColor: UIColor = .systemBlue.withAlphaComponent(0.8)

                // Helper function for rotation
                func getRotatedPosition(for point: MXMGeoPoint) -> SIMD3<Float> {
                    let worldX = (Float(point.longitude) - baseLon) * 100_000
                    let worldZ = (baseLat - Float(point.latitude)) * 100_000
                    
                    let rX = worldX * cosTheta + worldZ * sinTheta
                    let rZ = -worldX * sinTheta + worldZ * cosTheta
                    // Note: Changed -5.0 to -0.5 as usually -5 is too deep for AR floor
                    return SIMD3<Float>(rX, -5.0, rZ)
                }

                let startPos = getRotatedPosition(for: points[currentIndex])
                let endPos = getRotatedPosition(for: points[currentIndex + 1])

                await MainActor.run {
                    // --- 3. UPDATED JUNCTION LOGIC ---
                    
                    // ALWAYS draw the start plate for the very first step
                    // This ensures 2 plates appear (Start & End) on the first segment.
                    if currentIndex == 0 {
                        addJunctionPlate(
                            at: startPos,
                            roadWidth: roadWidth,
                            roadThickness: roadThickness,
                            roadColor: roadColor,
                            to: arView
                        )
                    }

                    // 4. DRAW THE ROAD
                    addStraightRoadForIsShowingOneByOneARNavigation(
                        start: startPos,
                        end: endPos,
                        roadWidth: roadWidth,
                        roadThickness: roadThickness,
                        roadColor: roadColor,
                        to: arView
                    )

                    // 5. DRAW THE END PLATE
                    // This acts as the "second" plate for index 0,
                    // and the "next" plate for all subsequent indices.
                    addJunctionPlate(
                        at: endPos,
                        roadWidth: roadWidth,
                        roadThickness: roadThickness,
                        roadColor: roadColor,
                        to: arView
                    )
                }
            }
        }

    }
}

extension CLLocationCoordinate2D {
    func destinationPoint(bearing: Double, distanceMeters: Double) -> CLLocationCoordinate2D {
        let radius = 6371e3 // Earth radius in meters
        let δ = distanceMeters / radius
        let θ = bearing.degreesToRadians

        let φ1 = self.latitude.degreesToRadians
        let λ1 = self.longitude.degreesToRadians

        let φ2 = asin(sin(φ1) * cos(δ) + cos(φ1) * sin(δ) * cos(θ))
        let λ2 = λ1 + atan2(sin(θ) * sin(δ) * cos(φ1),
                            cos(δ) - sin(φ1) * sin(φ2))

        return CLLocationCoordinate2D(latitude: φ2.radiansToDegrees, longitude: λ2.radiansToDegrees)
    }
}

private extension Double {
    var degreesToRadians: Double { self * .pi / 180 }
    var radiansToDegrees: Double { self * 180 / .pi }
}
