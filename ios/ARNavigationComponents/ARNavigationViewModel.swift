//
//  ARNavigationViewModel.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 11/01/26.
//

import SwiftUI
import RealityKit
import Combine

class ARNavigationViewModel: ObservableObject {
    
    // --- State Properties ---
    @Published var lastRenderedFloorId: String? = nil
    @Published var isWaitingForFloorConfirmation: Bool = false
    @Published var isDrawingStarted: Bool = false
    @Published var isCalibrated: Bool = false
    @Published var isShowingDirectionDegree: String = ""
    @Published var offsetSamples: [Double] = []
    
    @Published var isEndingTheMapxusMapNavigation: Bool = false
    
    // --- AR References ---
    // Note: Use weak if this is owned by a View/Controller to prevent retain cycles
    var arView: ARView?
    var instructionArrows: [AnchorEntity] = []
    
    init() {} // Prevent multiple instances if using .shared

    @MainActor
    func endARNavigation() {
        Task(operation: {
            // 1. Immediate Logic Lock
            // This stops any running loops or frame updates from proceeding
            self.isDrawingStarted = false
            self.isCalibrated = false
            self.isWaitingForFloorConfirmation = false
            self.isShowingDirectionDegree = ""
            
            // 2. Clear Data Buffers
            self.offsetSamples.removeAll()
            self.lastRenderedFloorId = nil
            
            // 3. Complete Scene Purge
            guard let arView = self.arView else {
                print("⚠️ No arView found during cleanup")
                return
            }
            
            // Remove tracked arrows
            for anchor in self.instructionArrows {
                arView.scene.removeAnchor(anchor)
            }
            self.instructionArrows.removeAll()
            
            // NUCLEAR OPTION: Remove absolutely everything else (including roads/plates)
            // This catches any anchors that weren't explicitly saved in an array
            arView.scene.anchors.removeAll()
            
            // 4. Optional: Pause the session to save battery/CPU
            // arView.session.pause()
            
            print("🛑 AR Navigation Ended: All anchors and road segments destroyed.")
        })
    }
}
