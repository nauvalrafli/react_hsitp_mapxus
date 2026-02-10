//
//  LocationManagerClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 15/06/25.
//

import CoreLocation
import SwiftUI

//class LocationManagerClass: NSObject, ObservableObject, CLLocationManagerDelegate {
//    private let manager = CLLocationManager()
//        
//    override init() {
//        super.init()
//        manager.delegate = self
//        
//        // 1. Check existing status immediately
//        // If already denied, show alert. If not determined, request it.
//        handleStatus(manager.authorizationStatus)
//    }
//    
//    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
//        handleStatus(manager.authorizationStatus)
//    }
//
//    private func handleStatus(_ status: CLAuthorizationStatus) {
//        switch status {
//        case .denied, .restricted:
//            self.showPermissionAlert(for: "Location")
//        case .notDetermined:
//            manager.requestWhenInUseAuthorization()
//        default:
//            break
//        }
//    }
//    
//    func showPermissionAlert(for type: String) {
//        // Use a slightly longer delay to ensure the View is ready
//        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//            AllViewReceiver.shared.isFillingAnAlertDialogTitle = "\(type) Access Required"
//            AllViewReceiver.shared.isFillingAnAlertDialogTextAction = "Settings"
//            AllViewReceiver.shared.isFillingAnAlertDialogTextCancel = "Cancel"
//            
//            // This is the "Automatic" trigger
//            AllViewReceiver.shared.isShowingAnAlertDialog = true
//            
//            AllViewReceiver.shared.isEndingTheARNavigationAndTheMapxusMapNavigationAction = {
//                if let url = URL(string: UIApplication.openSettingsURLString) {
//                    UIApplication.shared.open(url)
//                }
//            }
//        }
//    }
//}

class LocationManagerClass: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
        
    // 💡 Add this flag to track if we are already showing an alert
    private var isShowingAlert = false
        
    override init() {
        super.init()
        manager.delegate = self
        handleStatus(manager.authorizationStatus)
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager, introduction: Binding<Bool>) {
        if !introduction.wrappedValue {
            handleStatus(manager.authorizationStatus)
        }
    }

    private func handleStatus(_ status: CLAuthorizationStatus) {
        switch status {
        case .denied, .restricted:
            // 💡 Only call if we aren't already showing one
            if !isShowingAlert {
                self.showUIKitPermissionAlert(for: "Location")
            }
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        default:
            // Reset flag if permission is granted
            isShowingAlert = false
            break
        }
    }
    
    func showUIKitPermissionAlert(for type: String) {
        isShowingAlert = true
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { // Reduced delay slightly for better feel
            let alert = UIAlertController(
                title: "\(type) Access Required",
                message: "Please enable \(type) permissions in Settings to continue.",
                preferredStyle: .alert
            )
            
            let settingsAction = UIAlertAction(title: "Settings", style: .default) { _ in
                self.isShowingAlert = false
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            
            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { _ in
                // 1. Reset the flag so the function allows a new alert
                self.isShowingAlert = false
                
                // 2. Call the function again to show it immediately
                // We use a small delay so the previous alert has time to fully disappear
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    self.showUIKitPermissionAlert(for: type)
                }
            }
            
            alert.addAction(settingsAction)
            alert.addAction(cancelAction)
            
            if let topController = self.getTopViewController() {
                if !(topController is UIAlertController) {
                    topController.present(alert, animated: true)
                }
            }
        }
    }
    
    // Helper function to find the current active View Controller
    private func getTopViewController() -> UIViewController? {
        // Find any active window in the current scene
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        var topController = window?.rootViewController
        while let presented = topController?.presentedViewController {
            topController = presented
        }
        return topController
    }
}
