//
//  mapxus_hsitp_iosApp.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 05/05/25.
//

import SwiftUI
import MapxusMapSDK
import MapxusBaseSDK
import ObjectiveC

/// hsitp.mapxus-hsitp-ios -> Bundle Identifier
class AppDelegate : NSObject, UIApplicationDelegate {
    func application(
            _ application: UIApplication,
            didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
        ) -> Bool {
            MXMMapServices().register(withApiKey: "66bd33b7409c4895862fbd32008acde0", secret: "4631c87ebe5c473f90463a458e6d642c")
            return true
        }
}

@main
struct mapxus_hsitp_iosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @AppStorage("ARNavigation-App-Enabling-Dark-Mode") private var isEnablingDarkMode: Bool = false
    
    var body: some Scene {
        WindowGroup(makeContent: {
            HomeView()
                .preferredColorScheme(.light)
        })
    }
}

//- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
//  [MXMMapServices.shared registerWithApiKey:@"123123api" secret:@"123123key"];
//  return [super application:application didFinishLaunchingWithOptions:launchOptions];
//}
//
//MapxusHsitpViewManager.mm
//#import <React/RCTViewManager.h>
//#import <React/RCTUIManager.h>
//#import "RCTFabricComponentsPlugins.h"
//#import "mapxus_hsitp_ios-Swift.h" // Replace with your actual ProductModuleName-Swift.h
//
//@interface MapxusHsitpViewManager : RCTViewManager
//@end
//
//@implementation MapxusHsitpViewManager
//RCT_EXPORT_MODULE(MapxusHsitpView)
//
//- (UIView *)view {
//  // This calls the Swift factory to return the Hosted SwiftUI View
//  return [MapxusBridgeFactory createHomeView];
//}
//
//RCT_EXPORT_VIEW_PROPERTY(customLocale, NSString)
//RCT_EXPORT_VIEW_PROPERTY(color, NSString)
//
//@end
