//
//  CompassClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 03/12/25.
//

import Foundation

import MapxusMapSDK
import MapxusBaseSDK
import MapxusComponentKit
import MapxusVisualSDK
import Combine
import CoreLocation
import SwiftUI

class CompassClass: NSObject, ObservableObject, CLLocationManagerDelegate {
//    var objectWillChange = PassthroughSubject<Void, Never>()
//    
//    @Published var degrees: Double = .zero {
//        didSet {
//            objectWillChange.send()
//        }
//    }
//    
//    private let locationManager: CLLocationManager
//    // Track state to avoid redundant calls
//    private var isActive: Bool = false
//    
//    override init() {
//        self.locationManager = CLLocationManager()
//        super.init()
//        self.locationManager.delegate = self
//        // Only request permission on init, don't start yet
//        self.locationManager.requestWhenInUseAuthorization()
//    }
//    
//    /// Call this when the AR Navigation appears
//    func startUpdatingLocation() {
//        guard !isActive else { return }
//        
//        if CLLocationManager.headingAvailable() {
//            // High accuracy for AR calibration
//            self.locationManager.desiredAccuracy = kCLLocationAccuracyBest
//            self.locationManager.startUpdatingLocation()
//            self.isActive = true
//            print("🧭 Compass Service: Started")
//        }
//    }
//    
//    /// Call this when the AR Navigation is dismissed or finished
//    func stopUpdatingLocation() {
//        guard isActive else { return }
//        
//        self.locationManager.stopUpdatingLocation()
//        self.isActive = false
//        print("🧭 Compass Service: Stopped")
//    }
//    
//    private func startCompass() {
//        locationManager.startUpdatingHeading()
//        print("🧭 Compass Service: Started")
//    }
//
//    private func stopCompass() {
//        locationManager.stopUpdatingHeading()
//        print("🧭 Compass Service: Stopped")
//    }
//    
//    func toggleCompass(to state: Bool) {
//        if state {
//            startCompass()
//        } else {
//            stopCompass()
//        }
//    }
//    
//    func toggleLocation(state: Bool) {
//        if state {
//            self.startUpdatingLocation()
//        } else {
//            self.stopUpdatingLocation()
//        }
//    }
//    
//    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
//        // Use trueHeading if available (better for maps), otherwise magnetic
//        let raw = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
//        let normalized = fmod(raw, 360)
//        self.degrees = normalized
//    }
}
