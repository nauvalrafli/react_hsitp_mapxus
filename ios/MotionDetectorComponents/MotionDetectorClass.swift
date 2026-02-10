//
//  MotionDetectorClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 06/06/25.
//

//import CoreMotion
//import Foundation

//class MotionDetectorClass: ObservableObject {
//    private var motionManager = CMMotionManager()
//    @Published var isPhoneRaised = false
//
//    /// For backup from me
////    init() {
////        startMonitoring()
////    }
//
//    func startMonitoring() {
//        guard motionManager.isDeviceMotionAvailable else { return }
//
//        motionManager.deviceMotionUpdateInterval = 0.2
//        motionManager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
//            guard let attitude = motion?.attitude else { return }
//
//            let pitch = attitude.pitch
//
//            // ✅ Phone is vertical (screen facing user) if pitch is near 0
//            // Lying flat is pitch > 1.0 or < -1.0
//            DispatchQueue.main.async {
//                self?.isPhoneRaised = abs(pitch) < 0.5
//                
////                print("iPhone pitch: \(pitch), isPhoneRaised: \(self?.isPhoneRaised ?? false)")
//            }
//        }
//    }
//    
//    func stopMonitoring() {
//        motionManager.stopDeviceMotionUpdates()
//    }
//
//    deinit {
//        motionManager.stopDeviceMotionUpdates()
//    }
//}



