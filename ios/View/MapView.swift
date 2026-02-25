//
//  MapView.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 08/05/25.
//

import SwiftUI
import Mapbox
import MapxusMapSDK
import CoreLocation
import AVFoundation /// Required for camera checks

private enum ActiveTooltip {
    case none, mapping, arNavigation, destination, start, gps
}

enum SearchField {
    case searchAnything
}

struct MapView : View {
    @Environment(\.presentationMode) private var presentationMode
    @Environment(\.dismiss) private var dismiss // Use this instead of presentationMode
    @State private var selectedPOI: MapPoi?
    @StateObject private var mapxusController: MapxusController = MapxusController(poi: nil)
//    @StateObject private var motionDetector: MotionDetectorClass = MotionDetectorClass()
    @StateObject private var translationClass: TranslationClass = TranslationClass()
    @StateObject private var locationManager: LocationManagerClass = LocationManagerClass()
    @StateObject private var toiletOccupancyClass: TelemetryViewModel = TelemetryViewModel()
    @ObservedObject private var userClass: UserClass = UserClass.shared
    @ObservedObject private var allViewReceiver: AllViewReceiver = AllViewReceiver.shared
    
//    @State private var isShowingARNavigation: Bool = false
//    @State private var isShowingAndClosingTheARNavigation: Bool = false
    @State private var isShowingARButtons: Bool = false
    @AppStorage("ARNavigation-App-Enabling-Motion-Sensor") private var isEnablingMotionSensor: Bool = false
    @AppStorage("ARNavigation-App-Stopping-AR-Navigation") private var isStoppingARNavigation: Bool = false
    @AppStorage("ARNavigation-App-Enabling-TTS") private var isEnablingTTS: Bool = true
    @AppStorage("Mapxus-Map-Off-Route-Threshold") private var offRouteThreshold: CLLocationDistance = 10.0
    
    @State private var isShowingRouteOrNot: Bool = false
    
    @Namespace private var animation
    @Namespace private var searchingAnimation
    
    @State private var isLoading = false
    
//    @State private var arViewHeight: CGFloat = UIScreen.main.bounds.height * 0.5
//    @State private var mapViewHeight: CGFloat = UIScreen.main.bounds.height * 0.5
    @State private var arViewHeight: CGFloat = UIScreen.main.bounds.height * 0.0
    @State private var mapViewHeight: CGFloat = UIScreen.main.bounds.height * 1
    
    @State private var isShowingSheet: Bool = true
    @State private var isDisablingClosingTheSheet: Bool = true
    @State private var isSelectingToiletCategory: String? = nil
    @State private var isGettingBuildingId: String = ""
    @State private var isGettingBuildingNumber: String = ""
    @State private var isGettingBuildingFacilityCategory: String = ""
//    @State private var isGettingDestinationName: String = ""
//    @State private var isGettingDestinationFloorName: String = ""
    
//    @State private var isShowingTheSettingsViewButton: Bool = true
    
    @State private var isUsingCurrentLocation: Bool = false
    @State private var isUsingSearchAnythingFeature: Bool = false
    
    @State private var isShowingMappinTooltip: Bool = true
    @AppStorage("AR-Navigation-Tooltip-Message") private var isShowingARNavigationTooltip: Bool = true
    @AppStorage("GPS-Tooltip-Message") private var isShowingGpsTooltip: Bool = true
    @State private var activeTooltip: ActiveTooltip = .none
    
    @AppStorage("MapxusMap-Route-Options") private var selectedRouteType: String = "Shortest Walk" /// The default value
    
    @State private var selectedStatus: String = "Available" // Track the current selection
//    private let vacantToiletStatuses: [String] = ["Full", "Available", "Almost Full"]
    
    @State private var buildingFacilityCategories: [String] = ["Company", "Shop", "Restaurant", "Washroom", "Transportation", "Utilities"]
    @State private var isShortingTheBuildingFacilities: [String] = ["B1", "BMF", "GF", "MF", "1F", "2F", "3F", "5F", "6F", "7F", "8F", "RF"]
    
    @State private var buildingListCurrentPage: Int? = 0
    
    @State private var isShowingEndMapNavigationAndARNavigationAlertDialog: Bool = false
    
    @AppStorage("MapxusMap-Introduction-Section") private var isShowingMapxusMapIntroductionSection: Bool = true
    @State private var introductionSectionCurrentPage: Int? = 0
    
    @FocusState private var focusedState: SearchField?
    
//    @State private var isShowingCurrentStatusOfMapRotation: Bool = false
    @State private var isRefreshingTheToiletOccupancy: Bool = false
    
    @State private var foundDeviceIds: [String] = [] /// Add this at the top of your View
    @State private var foundDeviceIdsMap: [String: [String]] = [:] /// Add this at the top of your View
    @State private var isRepeatingToiletOccupancy: Bool = false
    
    @State private var isLoadingOnToiletOccupancy: String = ""
    
    @State private var isSearchingAnythingInTheBuilding: String = ""
    @State private var isShowingSearchingOption: Bool = false
    
    var location = CLLocationManager()
    
    var body: some View {
        ZStack(content: {
            MapViewAR()
        })
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .topTrailing, content: {
            VStack(spacing: 8, content: {
                if isShowingARButtons {
                    CustomCircleIconButton(icon: "close_2", iconColor: Color.white, backgroundColor: Color.red, action: {
                        withAnimation(.spring, {
                            isShowingEndMapNavigationAndARNavigationAlertDialog = true
                        })
                    })
                    
                    CustomCircleIconButton(icon: "text_to_speech", iconColor: isEnablingTTS ? Color.mainColor : Color.white, backgroundColor: isEnablingTTS ? Color.white : Color.secondary, action: {
                        isEnablingTTS.toggle()
                        mapxusController.textToSpeech(isActive: isEnablingTTS)
                    })
                    
                    CustomCircleIconButton(
                        icon: "ar-navigation-icon",
                        iconColor: mapxusController.isShowingAndClosingTheARNavigation ? Color.mainColor : Color.white,
                        backgroundColor: mapxusController.isShowingAndClosingTheARNavigation ? Color.white : Color.secondary,
                        action: {
                            accessCameraPermission()
                        }
                    )
                    .onAppear(perform: {
                        if isShowingARNavigationTooltip {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6, execute: {
                                withAnimation(.spring(), {
                                    activeTooltip = .arNavigation
                                })
                            })
                        }
                    })
                    .overlay(alignment: .trailing, content: {
                        if activeTooltip == .arNavigation, let items = arButtonTooltip {
                            Tooltip(items: items, type: .left)
                                .fixedSize()
                                .offset(x: -30)
                                .alignmentGuide(.trailing, computeValue: { d in
                                    d[.trailing] + 10
                                })
                                .transition(.asymmetric(
                                    insertion: .scale(scale: 0.5, anchor: .trailing).combined(with: .opacity),
                                    removal: .opacity.combined(with: .scale(scale: 0.8))
                                ))
                                .onTapGesture(perform: {
                                    if isShowingARNavigationTooltip {
                                        isShowingARNavigationTooltip = false
                                    }
                                    
                                    toggleTooltip(.arNavigation)
                                })
                                .task(id: activeTooltip, {
                                    try? await Task.sleep(nanoseconds: 5_000_000_000)
                                    dismissTooltip()
                                })
                        }
                    })
                }
                
                if mapxusController.isShowingTheSettingsViewButton {
                    CustomCircleIconButton(icon: "gear", iconColor: Color.mainColor, action: {
                        isAdjustingSheetHeight(height: 240, isLarge: true)
                        
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6, execute: {
                            mapxusController.navigationDestinationPath.append("SettingsView")
                        })
                    })
                    .transition(.opacity)
                    .animation(.smooth(duration: 0.3), value: mapxusController.isShowingTheSettingsViewButton)
                }
                
                CustomCircleIconButton(icon: "gps-icon", iconColor: Color.mainColor, action: {
                    if mapxusController.mapState != .navigating {
                        // 1. Force it to false first to "reset" the animation state
                        mapxusController.isShowingCurrentStatusOfMapRotation = false
                        
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05, execute: {
                            withAnimation(.smooth(), {
                                mapxusController.isShowingCurrentStatusOfMapRotation.toggle()
                                mapxusController.showMyLocation()
                            })
                        })
                    }
                })
                .transition(.move(edge: mapxusController.isShowingTheSettingsViewButton ? .top : .bottom))
                .animation(.smooth, value: mapxusController.isShowingTheSettingsViewButton)
                .onAppear(perform: {
                    if isShowingGpsTooltip {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6, execute: {
                            withAnimation(.spring(), {
                                activeTooltip = .gps
                            })
                        })
                    }
                })
                .overlay(alignment: .trailing, content: {
                    if activeTooltip == .gps, let items = gpsTooltip {
                        Tooltip(items: items, type: .left)
                            .fixedSize()
                            .offset(x: -30)
                            .alignmentGuide(.trailing, computeValue: { d in
                                d[.trailing] + 10
                            })
                            .transition(.asymmetric(
                                insertion: .scale(scale: 0.5, anchor: .trailing).combined(with: .opacity),
                                removal: .opacity.combined(with: .scale(scale: 0.8))
                            ))
                            .onTapGesture(perform: {
                                if isShowingGpsTooltip {
                                    isShowingGpsTooltip = false
                                }
                                
                                toggleTooltip(.gps)
                            })
                            .task(id: activeTooltip, {
                                try? await Task.sleep(nanoseconds: 5_000_000_000)
                                dismissTooltip()
                            })
                    }
                })
            })
            .offset(y: 100)
            .padding(.horizontal, 7)
            .transition(.move(edge: mapxusController.isShowingARNavigation ? .bottom : .top))
        })
        .sheet(isPresented: $isShowingSheet, content: {
            NavigationStack(path: $mapxusController.navigationDestinationPath, root: {
                BuildingList()
            })
            .presentationBackgroundInteraction(.enabled)
            .presentationDetents([.height(mapxusController.sheetHeight), .large], selection: $mapxusController.presentationActiveDetent)
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(18)
            .interactiveDismissDisabled(isDisablingClosingTheSheet)
            .customToast(isShown: $allViewReceiver.isShowingACustomToastWashroom, message: allViewReceiver.isShowingACustomToastMessageWashroom, icon: allViewReceiver.isShowingACustomToastIconWashroom, iconColor: allViewReceiver.isShowingACustomToastIconColorWashroom, alignment: allViewReceiver.isShowingACustomToastAlignmentWashroom)
            .customToast(isShown: $allViewReceiver.isShowingACustomToastInternet, message: allViewReceiver.isShowingACustomToastMessageInternet, icon: allViewReceiver.isShowingACustomToastIconInternet, iconColor: allViewReceiver.isShowingACustomToastIconColorInternet, alignment: allViewReceiver.isShowingACustomToastAlignmentInternet)
        })
        .onChange(of: mapxusController.isShowingAndClosingTheARNavigation, { oldValue, newValue in
            withAnimation(.easeInOut(duration: 0.6), {
                if newValue {
                    arViewHeight = UIScreen.main.bounds.height * 0.3
                    mapViewHeight = UIScreen.main.bounds.height * 0.7
                } else {
                    arViewHeight = 0
                    mapViewHeight = UIScreen.main.bounds.height
                }
            })
        })
//        .onChange(of: motionDetector.isPhoneRaised, { oldValue, newValue in
//            withAnimation(.easeInOut(duration: 0.7), {
//                if newValue {
//                    arViewHeight = 0
//                    mapViewHeight = UIScreen.main.bounds.height
//                } else {
//                    arViewHeight = UIScreen.main.bounds.height * 0.5
//                    mapViewHeight = UIScreen.main.bounds.height * 0.5
//                }
//            })
//        })
        .onChange(of: mapxusController.compassTruHeadingWarning, { oldValue, newValue in
            withAnimation(.smooth, {
                mapxusController.compassTruHeadingWarning = newValue
            })
        })
        .onChange(of: mapxusController.isGettingBuildingNumber, { oldValue, newValue in
            print("building number updated: \(newValue)")
        })
        .onChange(of: isShowingMapxusMapIntroductionSection, { oldValue, newValue in
            if newValue {
                withAnimation(.smooth(), {
                    mapxusController.isFoldingFloorBarSection(fold: true)
                    isDisablingClosingTheSheet = false
                    isShowingSheet = false
                })
            } else {
                withAnimation(.smooth(), {
                    mapxusController.isFoldingFloorBarSection(fold: false)
                    isShowingSheet = true
                    isDisablingClosingTheSheet = true
                })
            }
        })
        .onAppear(perform: {
            if isShowingMapxusMapIntroductionSection {
                withAnimation(.smooth(), {
                    mapxusController.isFoldingFloorBarSection(fold: true)
                    isDisablingClosingTheSheet = false
                    isShowingSheet = false
                })
            } else {
                withAnimation(.smooth(), {
                    mapxusController.isFoldingFloorBarSection(fold: false)
                    isShowingSheet = true
                    isDisablingClosingTheSheet = true
                })
            }
        })
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification), perform: { _ in
            if !isShowingMapxusMapIntroductionSection {
                locationManager.locationManagerDidChangeAuthorization(location, introduction: .constant(false))
            }
        })
        .task({
            await userClass.syncSpreadsheet()
        })
        .task(id: mapxusController.selectedLanguage, {
            mapxusController.searchBuildingLists()
        })
        .customAlert(translationClass.endNavigation(code: mapxusController.selectedLanguage), isPresented: $isShowingEndMapNavigationAndARNavigationAlertDialog, actionText: translationClass.end(code: mapxusController.selectedLanguage), closeTextKey: translationClass.cancel(code: mapxusController.selectedLanguage), alertMessage: translationClass.youAreGoingToEndTheNavigation(code: mapxusController.selectedLanguage), image: "", action: {
            withAnimation(.smooth, {
                endNavigation()
            })
        }, closeAction: {
            
        })
        .customAlert(translationClass.kudos(code: mapxusController.selectedLanguage), isPresented: $mapxusController.isShowingArrivedAtTheDestinationAlertDialog, actionText: translationClass.finished(code: mapxusController.selectedLanguage), closeTextKey: translationClass.goPrevious(code: mapxusController.selectedLanguage), alertMessage: translationClass.arrivedAtDestination(code: mapxusController.selectedLanguage), image: "", action: {
            withAnimation(.smooth, {
                endNavigation()
            })
        }, closeAction: {
            mapxusController.previousStep()
        })
        .customAlertWithEmptyContent(isPresented: $isShowingMapxusMapIntroductionSection, content: {
            IntroductionSection(mapxusController: mapxusController, currentPage: $introductionSectionCurrentPage, action: {
                withAnimation(.smooth, {
                    isShowingMapxusMapIntroductionSection = false
                    isShowingSheet = true
                    isDisablingClosingTheSheet = true
                    introductionSectionCurrentPage = 0
                })
            })
        }, action: {
            
        })
        .customToast(isShown: $allViewReceiver.isShowingACustomToastGeneral, message: allViewReceiver.isShowingACustomToastMessageGeneral, icon: allViewReceiver.isShowingACustomToastIconGeneral, iconColor: allViewReceiver.isShowingACustomToastIconColorGeneral, alignment: allViewReceiver.isShowingACustomToastAlignmentGeneral)
        .customToast(isShown: $mapxusController.isShowingCurrentStatusOfMapRotation, message: mapxusController.isRotatingTheMapOnGPSButtonClicked ? translationClass.mapRotationIsActive(code: mapxusController.selectedLanguage) : translationClass.mapRotationIsNotActive(code: mapxusController.selectedLanguage), icon: "globe.asia.australia", iconColor: mapxusController.isRotatingTheMapOnGPSButtonClicked ? Color.mainColor : Color.red)
    }
    
    func accessCameraPermission() {
        // 1. Check the current authorization status
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        
        switch status {
        case .authorized:
            // User already allowed, proceed to AR
            startARNavigation()
            
        case .notDetermined:
            // Haven't asked yet, request permission
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async { // Using Main Thread for UI
                    if granted {
                        self.startARNavigation()
                    } else {
                        // 💡 Call your new UIKit alert for Camera
                        self.showUIKitPermissionAlert(for: "Camera")
                    }
                }
            }
            
        case .denied, .restricted:
            // User said no previously, show the UIKit alert
            self.showUIKitPermissionAlert(for: "Camera")
        @unknown default:
            break
        }
    }
    
    func showUIKitPermissionAlert(for type: String) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { // Reduced delay slightly for better feel
            let alert = UIAlertController(
                title: "\(type) Access Required",
                message: "Please enable \(type) permissions in Settings to continue.",
                preferredStyle: .alert
            )
            
            let settingsAction = UIAlertAction(title: "Settings", style: .default) { _ in
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            
            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { _ in
                // 2. Call the function again to show it immediately
                // We use a small delay so the previous alert has time to fully disappear
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
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
    
    func startARNavigation() {
        // 1. Only run if we aren't currently showing the AR view
        if !mapxusController.isShowingARNavigation {
            print("🚀 AR Button Tapped: Launching Navigation...")
            
            // 2. Trigger the navigation state
                mapxusController.isShowingARNavigation = true
        }
        
        // 3. Toggle your UI state
            mapxusController.isShowingAndClosingTheARNavigation.toggle()
    }
    
    func endNavigation() {
        withAnimation(.smooth, {
            mapxusController.isShowingAndClosingTheARNavigation = false
            isAdjustingSheetHeight(height: 240, isLarge: false)
        })
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            withAnimation(.smooth, {
                isGoingToMainView()
                
                isStoppingARNavigation = true
                    mapxusController.isShowingARNavigation = false
                isShowingARButtons = false
                
                isDisablingClosingTheSheet = true
                    mapxusController.isShowingTheSettingsViewButton = true
                isShowingSheet = true
                
                mapxusController.endNavigation()
                UIApplication.shared.isIdleTimerDisabled = false
            })
        }
    }
    
    var filteredFacilitiesBySearchingValue: [any FacilityProtocol] {
        // 1. Explicitly select the source first
        let source: [any FacilityProtocol]
        if mapxusController.isSearchingAllFacilitiesOnEveryBuilding {
            source = mapxusController.allBuildingFacilities
        } else {
            source = mapxusController.buildingFacilities
        }
        
        // 2. Prepare search variables outside the filter
        let query = isSearchingAnythingInTheBuilding.lowercased()
        let isGlobal = mapxusController.isSearchingAllFacilitiesOnEveryBuilding
        let bId = isGettingBuildingId
        
        // 3. Filter with simple, pre-calculated logic
        return source.filter { facility in
            // Building scope check
            if !isGlobal {
                guard facility.buildingId == bId else { return false }
            }
            
            // Search text check
            if query.isEmpty { return true }
            
            let name = facility.facilityName.lowercased()
            if name.contains(query) { return true }
            
            // Category check
            let hasCategoryMatch = facility.category.contains { cat in
                cat.lowercased().contains(query)
            }
            
            return hasCategoryMatch
        }
    }
    
    private var tooltipMappin: [TooltipModel] {
        [TooltipModel(name: "drag_map", title: translationClass.dragMapTooltip(code: mapxusController.selectedLanguage))]
    }
    
    private var tooltipAR: [TooltipModel] {
        [TooltipModel(name: "ar_button", title: translationClass.arButtonTooltip(code: mapxusController.selectedLanguage))]
    }
    
    private var tooltipDestination: [TooltipModel] {
        [TooltipModel(name: "destination_label", title: translationClass.destinationTooltip(code: mapxusController.selectedLanguage))]
    }

    private var tooltipStart: [TooltipModel] {
        [TooltipModel(name: "start_label", title: translationClass.startTooltip(code: mapxusController.selectedLanguage))]
    }
    
    private var tooltipGps: [TooltipModel] {
        [TooltipModel(name: "gps", title: translationClass.gpsTooltip(code: mapxusController.selectedLanguage))]
    }
    
    private var gpsTooltip: [TooltipModel]? {
        tooltipGps.first(where: { $0.name == "gps" }).map { [$0] }
    }
    
    private var arButtonTooltip: [TooltipModel]? {
        tooltipAR.first(where: { $0.name == "ar_button" }).map { [$0] }
    }
    
    private var destinationLabelTooltip: [TooltipModel]? {
        tooltipDestination.first(where: { $0.name == "destination_label" }).map { [$0] }
    }
    
    private var startLocationLabelTooltip: [TooltipModel]? {
        tooltipStart.first(where: { $0.name == "start_label" }).map { [$0] }
    }
    
    private func buildingFacilityCategoriesInMultipleLanguages(for code: String) -> [String] {
        // 1. Handle empty or default cases immediately
        if code.isEmpty {
            return ["English"]
        }

        // 2. Map codes to their descriptive names
        switch code {
        case "zh-Hant":
            return ["公司", "商店", "餐廳", "洗手間", "交通", "公用事業"]
        case "zh-Hans":
            return ["公司", "商店", "餐厅", "洗手间", "交通", "公用事业"]
        case "en":
            return ["Company", "Shop", "Restaurant", "Washroom", "Transportation", "Utilities"]
        default:
            return ["Unknown Language"]
        }
    }
    
    func buildingFacilityCategoryIcons(for icon: String) -> String {
        switch icon {
        case "Company", "公司":
            return "company"
        case "Shop", "商店":
            return "shop"
        case "Restaurant", "餐廳", "餐厅":
            return "restaurant"
        case "Washroom", "洗手間", "洗手间":
            return "washroom"
        case "Transportation", "交通":
            return "transportation"
        case "Utilities", "公用事業", "公用事业":
            return "utilities"
        default:
            return "default_facility_category" // Best to have a fallback icon rather than an empty string
        }
    }
    
    private func selectedRouteType(code: String) -> String {
        if code.isEmpty {
            return "en"
        }

        // 2. Map codes to their descriptive names
        switch code {
        case "zh-Hant":
            return "最短步行"
        case "zh-Hans":
            return "最短步行"
        case "en":
            return "Shortest Walk"
        default:
            return "Shortest Walk"
        }
    }
    
    private func routeTypeList(code: String) -> [String] {
        if code.isEmpty {
            return ["en"]
        }

        // 2. Map codes to their descriptive names
        switch code {
        case "zh-Hant":
            return ["最短步行", "僅限電梯", "限手扶梯"]
        case "zh-Hans":
            return ["最短步行", "仅限电梯", "仅限自动扶梯"]
        case "en":
            return ["Shortest Walk", "Lift Only", "Escalator Only"]
        default:
            return ["Route Type Not found"]
        }
    }
    
    func getCategoryFacilityInMultipleLanguage(rawCode: String, languageCode: String) -> String {
        let lowerCode = rawCode.lowercased()
        
        switch languageCode {
        case "zh-Hant":
            switch rawCode {
            case _ where lowerCode.contains("female"):
                return "女廁"
            case _ where lowerCode.contains("male"):
                return "男廁"
            case _ where lowerCode.contains("disable") || lowerCode.contains("accessible"):
                return "無障礙廁所"
            case _ where lowerCode.contains("shower"):
                return "淋浴"
                
            case _ where lowerCode.contains("meeting_room"):
                return "會議室"
            case _ where lowerCode.contains("function_room"):
                return "多功能廳"
            case _ where lowerCode.contains("convenience"):
                return "方便"
            case _ where lowerCode.contains("reception_desk"):
                return "資訊"
            case _ where lowerCode.contains("attractions"):
                return "景點"
            case _ where lowerCode.contains("office"):
                return "辦公室"
            case _ where lowerCode.contains("pantry"):
                return "食品儲藏室"
            case _ where lowerCode.contains("lab"):
                return "實驗室"
            case _ where lowerCode.contains("information"):
                return "資訊"
                
            case _ where lowerCode.contains("herbal_tea"):
                return "花草茶"
            case _ where lowerCode.contains("western"):
                return "西"
            case _ where lowerCode.contains("korean"):
                return "韓國人"
            case _ where lowerCode.contains("fast_food"):
                return "速食"
                
            case _ where lowerCode.contains("laundry_services"):
                return "洗衣服務"
            case _ where lowerCode.contains("mothersroom"):
                return "母嬰室"
            case _ where lowerCode.contains("couriers"):
                return "導遊"
            case _ where lowerCode.contains("defibrillator"):
                return "除顫器"
            case _ where lowerCode.contains("tactile_map"):
                return "觸覺地圖"
                
            default:
                // Fallback: extracts the last part of "facility.category.item" and capitalizes it
                return rawCode.components(separatedBy: ".").last?.replacingOccurrences(of: "_", with: " ").capitalized ?? "其他"
            }
        case "zh-Hans":
            switch lowerCode {
            case _ where lowerCode.contains("female"):
                return "女厕所"
            case _ where lowerCode.contains("male"):
                return "男厕所"
            case _ where lowerCode.contains("disable") || lowerCode.contains("accessible"):
                return "无障碍卫生间"
            case _ where lowerCode.contains("shower"):
                return "淋浴"
                
            case _ where lowerCode.contains("meeting_room"):
                return "会议室"
            case _ where lowerCode.contains("function_room"):
                return "多功能厅"
            case _ where lowerCode.contains("convenience"):
                return "方便"
            case _ where lowerCode.contains("reception_desk"):
                return "信息"
            case _ where lowerCode.contains("attractions"):
                return "景点"
            case _ where lowerCode.contains("office"):
                return "办公室"
            case _ where lowerCode.contains("pantry"):
                return "食品储藏室"
            case _ where lowerCode.contains("lab"):
                return "实验室"
            case _ where lowerCode.contains("information"):
                return "信息"
                
            case _ where lowerCode.contains("herbal_tea"):
                return "花草茶"
            case _ where lowerCode.contains("western"):
                return "西"
            case _ where lowerCode.contains("korean"):
                return "韩国人"
            case _ where lowerCode.contains("fast_food"):
                return "快餐"
                
            case _ where lowerCode.contains("laundry_services"):
                return "洗衣服务"
            case _ where lowerCode.contains("mothersroom"):
                return "母婴室"
            case _ where lowerCode.contains("couriers"):
                return "导游"
            case _ where lowerCode.contains("defibrillator"):
                return "除颤器"
            case _ where lowerCode.contains("tactile_map"):
                return "触觉地图"
                
            default:
                // Fallback: extracts the last part of "facility.category.item" and capitalizes it
                return rawCode.components(separatedBy: ".").last?.replacingOccurrences(of: "_", with: " ").capitalized ?? "其他"
            }
        default:
            switch lowerCode {
            case _ where lowerCode.contains("female"):
                return "Female Toilet"
            case _ where lowerCode.contains("male"):
                return "Male Toilet"
            case _ where lowerCode.contains("disable") || lowerCode.contains("accessible"):
                return "Accessible Toilet"
            case _ where lowerCode.contains("shower"):
                return "Shower"
                
            case _ where lowerCode.contains("meeting_room"):
                return "Meeting Room"
            case _ where lowerCode.contains("attractions"):
                return "Attractions"
            case _ where lowerCode.contains("office"):
                return "Office"
            case _ where lowerCode.contains("function_room"):
                return "Function Room"
            case _ where lowerCode.contains("pantry"):
                return "Pantry"
            case _ where lowerCode.contains("reception_desk"):
                return "Information"
            case _ where lowerCode.contains("lab"):
                return "Lab"
                
            case _ where lowerCode.contains("convenience"):
                return "Convenience"
                
            case _ where lowerCode.contains("herbal_tea"):
                return "Herbal Tea"
            case _ where lowerCode.contains("western"):
                return "Western"
            case _ where lowerCode.contains("korean"):
                return "Korean"
            case _ where lowerCode.contains("laundry_services"):
                return "Laundry Services"
            case _ where lowerCode.contains("mothersroom"):
                return "Mothers Room"
            default:
                // Fallback: extracts the last part of "facility.category.item" and capitalizes it
                return rawCode.components(separatedBy: ".").last?.replacingOccurrences(of: "_", with: " ").capitalized ?? "Other"
            }
        }
    }
    
    func getCategoryFacilityDefaultInMultipleLanguage(languangeCode: String) -> String {
        switch languangeCode {
        case "zh-Hant":
            return "全部"
        case "zh-Hans":
            return "全部"
        default:
            return "All"
        }
    }
    
    func isAdjustingSheetHeight(height: Double, isLarge: Bool) {
        if isLarge {
            withAnimation(.smooth(), {
                mapxusController.presentationActiveDetent = .large
            })
        } else {
            withAnimation(.smooth(), {
                mapxusController.sheetHeight = height
                mapxusController.presentationActiveDetent = .height(height)
            })
        }
    }
    
    private func toggleTooltip(_ target: ActiveTooltip) {
        // 1. If tapping the same one, just close it
        if activeTooltip == target {
            dismissTooltip()
            return
        }

        // 2. If another one is open, dismiss it first, then open the new one
        if activeTooltip != .none {
            activeTooltip = .none // Reset instantly to clear the state
            
            // Small delay (0.1s) allows SwiftUI to reset the view hierarchy
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation(.spring()) {
                    activeTooltip = target
                }
            }
        } else {
            // 3. Nothing was open, just open it normally
            withAnimation(.spring()) {
                activeTooltip = target
            }
        }
    }
    
    // MARK: - Helper
    private func dismissTooltip() {
        withAnimation(.spring(duration: 0.3, bounce: 0.6)) {
            activeTooltip = .none
        }
    }
    
    func isGoingToMainView() {
        mapxusController.navigationDestinationPath = NavigationPath()
    }

}

extension MapView {
    @ViewBuilder
    func MapViewAR() -> some View {
        let screenHeight = UIScreen.main.bounds.height
      
        VStack(spacing: 0, content: {
            ZStack(content: {
                if mapxusController.isShowingARNavigation {
                    ARMainView(arViewHeight: $arViewHeight, instructionList: $mapxusController.instructionList, instructionPointList: $mapxusController.instructionPointList, instructionIndex: $mapxusController.instructionIndex,  compassDegress: $mapxusController.compassTrueHeading, compassWarning: $mapxusController.compassTruHeadingWarning, languageCode: mapxusController.selectedLanguage)
                        .frame(maxWidth: .infinity, alignment: .top)
                        .background(.ultraThinMaterial)
                        .matchedGeometryEffect(id: "AR-Enabled-Animation", in: animation)
                        .transition(.move(edge: mapxusController.isShowingAndClosingTheARNavigation ? .top : .bottom))
//                        .transition(.move(edge: motionDetector.isPhoneRaised ? .top : .bottom))
                } else {
                    ProgressView("Analyzing AR...")
                        .frame(maxWidth: .infinity)
                        .frame(height: arViewHeight)
                        .matchedGeometryEffect(id: "AR-Enabled-Animation", in: animation)
                        .transition(.move(edge: mapxusController.isShowingAndClosingTheARNavigation ? .top : .bottom))
                }
            })
            .background(.ultraThinMaterial)
            
            ZStack(alignment: .bottom, content: {
                MyMapxus(controller: mapxusController)
                    .frame(maxWidth: .infinity, alignment: .bottom)
                    .frame(height: mapViewHeight)
                    .ignoresSafeArea(.all, edges: .all)
                    .transition(.move(edge: mapxusController.isShowingAndClosingTheARNavigation ? .bottom : .top))
                    .overlay(alignment: .bottom, content: {
                        VStack(content: {
                            NavigationDirectionCard()
                                .matchedGeometryEffect(id: "Mapxus-Map-Animation", in: animation)
                                .transition(.move(edge: isShowingARButtons ? .bottom : .top))
                        })
                        .padding(.bottom, 8)
                    })
                    .overlay(alignment: .center, content: {
                        if mapxusController.isSelectingLocationByPIN {
                            Button(action: {
                                withAnimation(.spring()) {
                                    toggleTooltip(.mapping)
                                }
                            }, label: {
                                Image("custom-start-marker-pin-1", bundle: Bundle.mapxus)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 28, height: 28)
                                    .padding(.bottom, 20) // Keeps the "tip" at the center of the map
                            })
                            .onAppear(perform: {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5, execute: {
                                    withAnimation(.spring(), {
                                        activeTooltip = .mapping
                                    })
                                })
                            })
                            .overlay(alignment: .top, content: {
                                if activeTooltip == .mapping {
                                    if let mapTooltip = tooltipMappin.first(where: { $0.name == "drag_map" }) {
                                        Tooltip(items: [mapTooltip], type: .top)
                                            .fixedSize()
                                            .offset(y: -50)
                                            .transition(.asymmetric(
                                                insertion: .scale(scale: 0.5, anchor: .bottom).combined(with: .opacity),
                                                removal: .opacity.combined(with: .scale(scale: 0.8))
                                            ))
                                            .onTapGesture(perform: {
                                                dismissTooltip()
                                            })
                                            .task(id: activeTooltip, {
                                                // ✅ Task is better: it cancels automatically if the view disappears
                                                // or if isShowingMappinTooltip changes again.
                                                try? await Task.sleep(nanoseconds: 3_500_000_000)
                                                dismissTooltip()
                                            })
                                    }
                                }
                            })
                        }
                    })
            })
        })
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(.ultraThinMaterial)
        .ignoresSafeArea(.all, edges: .all)
    }
    
    @ViewBuilder
    func BuildingList() -> some View {
        GeneralStruct(content: {
            let buildings = mapxusController.buildingLists

            // 2. Use that list in your CustomPager
            CustomPager(currPage: $mapxusController.currentBuildingIndex, totalCount: buildings.count, content: {
                if buildings.isEmpty {
                    BuildingListViewButton(buildingName: translationClass.hsitp(code: mapxusController.selectedLanguage), buildingNumber: "", section: translationClass.lokMaChauLoop(code: mapxusController.selectedLanguage), action: {
                        mapxusController.selectVenue(id: "2506d124f4d049fb8b5019ed9d78c309")
                    })
                    .containerRelativeFrame(.horizontal)
                } else {
                    ForEach(Array(mapxusController.buildingLists.enumerated()), id: \.element.id, content: { (index, building) in
                        BuildingListViewButton(buildingName: building.buildingName, buildingNumber: building.buildingNumber, section: translationClass.lokMaChauLoop(code: mapxusController.selectedLanguage), action: {
                            mapxusController.mapState = .initial
                            mapxusController.selectBuilding(id: building.id)
                            isGettingBuildingId = building.id
                            mapxusController.isGettingBuildingIdOnMapView = building.id
                            mapxusController.isGettingBuildingNumber = building.buildingNumber
                            isGettingBuildingNumber = building.buildingNumber
                            mapxusController.isShowingTheSettingsViewButton = false
                            mapxusController.isFetchingWashroomOccupancy = false
                            
//                            foundDeviceIdsMap = userClass.getDevicesMappingFromBuildingId(buildingId: building.id)
                            
                            mapxusController.navigationDestinationPath.append("SpecificBuildingView")
                            isAdjustingSheetHeight(height: 270, isLarge: false)
                            
//                            for (poiId, deviceIds) in userClass.getDevicesMappingFromBuildingId(buildingId: building.id) {
//                                print("Occupancy POI ID: \(poiId)")
//                                
//                                // 3. Iterate through the device IDs for this specific POI
//                                for deviceId in deviceIds {
//                                    print("Occupancy -> Device ID: \(deviceId)")
//                                }
//                            }
                        })
                        .containerRelativeFrame(.horizontal)
                        .onChange(of: mapxusController.currentBuildingIndex, { oldValue, newValue in
                            guard let index = newValue,
                                  mapxusController.buildingLists.indices.contains(index) else { return }
                            
                            let swipedBuildingId = mapxusController.buildingLists[index].id
                            
                            mapxusController.isShowingSelectedBuildingBasedOnSwiping(id: swipedBuildingId)
                            
//                            if mapxusController.gestureSwitchingBuilding() != swipedBuildingId {
//                                mapxusController.isSwiping = true // 🔒 LOCK
//                                mapxusController.isShowingSelectedBuildingBasedOnSwiping(id: swipedBuildingId)
//                                
//                                // Unlock after a short delay or in a completion handler if available
//                                if swipedBuildingId == mapxusController.currentBuildingId {
//                                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) {
//                                        mapxusController.isSwiping = false // 🔓 UNLOCK
//                                        print("unlock building")
//                                    }
//                                }
//                            }
                        })
//                        .onChange(of: mapxusController.gestureSwitchingBuilding()) { oldValue, newValue in
//                            print("gesture building id: \(newValue), current building id: \(mapxusController.currentBuildingId)")
//                            // 🛑 ONLY WORK if we are NOT currently swiping
//                            guard !mapxusController.isSwiping else { return }
//                            
//                            print("Map Gestured -> Updating UI to: \(newValue). Swiping: \(mapxusController.isSwiping)")
//                            if !mapxusController.isSwiping && newValue != mapxusController.currentBuildingId {
//                                mapxusController.handleBuildingSelection(tappedBuildingId: newValue)
//                            }
//                        }
                        .id(index as Int)
                    })
                }
            })
        })
        .navigationDestination(for: String.self, destination: { destination in
            switch destination {
            case "SpecificBuildingView": SpecificBuildingView(buildingId: isGettingBuildingId, buildingName: isGettingBuildingNumber, sectionName: translationClass.lokMaChauLoop(code: mapxusController.selectedLanguage))
            case "BuildingFacilityView": BuildingFacilityView(category: isGettingBuildingFacilityCategory)
            case "ConfirmingDestinationView":  ConfirmingDestinationView(destinationName: mapxusController.isGettingDestinationName, floorName: mapxusController.isGettingDestinationFloorName, building: mapxusController.isGettingBuildingNumber, category: isGettingBuildingFacilityCategory)
            case "SelectYourLocationView":  SelectYourLocationView()
            case "SetStartLocation":  SetStartLocation()
            case "StartMapxusMapNavigation":  StartMapxusMapNavigation()
            case "ShowRoute":  ShowRoute()
            case "SettingsView":  SettingsView(mapxusController: mapxusController)
            default: EmptyView()
            }
        })
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    Image("map-pin-2", bundle: Bundle.mapxus)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 26, height: 26)
                        .foregroundColor(Color.mainColor)
                    
                    Text(translationClass.exploreByMap(code: mapxusController.selectedLanguage))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color.primary)
                })
                .frame(maxWidth: .infinity, alignment: .leading)
            })
        })
    }
    
    @ViewBuilder
    func SpecificBuildingView(buildingId: String, buildingName: String, sectionName: String) -> some View {
        VStack(content: {
            VStack(alignment: .leading, spacing: 8, content: {
                HStack(spacing: 8, content: {
                    VStack(alignment: .leading, spacing: 6, content: {
                        Text(buildingName)
                            .font(.system(size: 24, weight: .bold))
                        
                        Text(sectionName)
                            .font(.system(size: 14, weight: .light))
                            .foregroundColor(Color.secondary)
                    })
                    
                    Spacer()
                    
                    if isShowingSearchingOption {
                        RadioButton(title: translationClass.allBuildings(code: mapxusController.selectedLanguage), isSelected: $mapxusController.isSearchingAllFacilitiesOnEveryBuilding, action: {
                            mapxusController.isSearchingAllFacilitiesOnEveryBuilding.toggle()
                        })
                        .transition(.fade)
                        .animation(.smooth, value: isShowingSearchingOption)
                        .task(id: mapxusController.isSearchingAllFacilitiesOnEveryBuilding, {
                            mapxusController.findAllIndoorCoordinatesBasedOnBuildingId(buildingId: isGettingBuildingId, keyword: $isSearchingAnythingInTheBuilding, allBuilding: Binding(get: {
                                mapxusController.isSearchingAllFacilitiesOnEveryBuilding
                            }, set: {
                                mapxusController.isSearchingAllFacilitiesOnEveryBuilding = $0
                            }))
                        })
                    }
                })
                .padding(.bottom, 8)
                
                SearchAnythingInTheBuilding(placeholder: translationClass.searchDestination(code: mapxusController.selectedLanguage), value: $isSearchingAnythingInTheBuilding, adjustSheet: $mapxusController.presentationActiveDetent, focusedState: _focusedState, action: {
                    isShowingSearchingOption = false
                    mapxusController.isSearchingAllFacilitiesOnEveryBuilding = false
                })
                .onChange(of: isSearchingAnythingInTheBuilding, { _, newValue in
                    let hasText = !newValue.isEmpty
                    isShowingSearchingOption = hasText
                    
                    if !hasText {
                        mapxusController.isSearchingAllFacilitiesOnEveryBuilding = false
                    }
                    
                    Task(operation: { @MainActor in
                        if hasText {
                            await toiletOccupancyClass.toggleGetWashroomOccupancyBasedOnAllBuildingsOrASpecific(
                                buildingId: isGettingBuildingId,
                                allBuildings: mapxusController.isSearchingAllFacilitiesOnEveryBuilding
                            )
                        }
                    })
                    
                    mapxusController.findAllIndoorCoordinatesBasedOnBuildingId(
                        buildingId: mapxusController.isSearchingAllFacilitiesOnEveryBuilding ? "" : isGettingBuildingId,
                        keyword: .constant(newValue),
                        allBuilding: $mapxusController.isSearchingAllFacilitiesOnEveryBuilding // Much cleaner syntax
                    )
                })
                .onChange(of: mapxusController.isSearchingAllFacilitiesOnEveryBuilding, { _, isSearchingAll in
                    Task(operation: { @MainActor in
                        await toiletOccupancyClass.toggleGetWashroomOccupancyBasedOnAllBuildingsOrASpecific(
                            buildingId: isGettingBuildingId,
                            allBuildings: isSearchingAll
                        )
                    })
                })
            })
            .padding(.top, 8)
            .padding(.horizontal, 16)
            
            ScrollView(.vertical, showsIndicators: false, content: {
                VStack(spacing: 16, content: {
                    if isSearchingAnythingInTheBuilding.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false, content: {
                            HStack(spacing: 16, content: {
                                ForEach(buildingFacilityCategoriesInMultipleLanguages(for: mapxusController.selectedLanguage), id: \.self, content: { destination in
                                    HStack(content: {
                                        SpecificSectionBuildingItem(name: destination, icon: buildingFacilityCategoryIcons(for: destination), action: {
                                            if destination == buildingFacilityCategoriesInMultipleLanguages(for: mapxusController.selectedLanguage)[3] {
                                                Task(operation: {
                                                    await toiletOccupancyClass.getToiletStatusWithoutAutomaticRefresh(buildingId: isGettingBuildingId)
                                                })
                                            }
                                            
                                            focusedState = nil
                                            isUsingSearchAnythingFeature = false
                                            mapxusController.isShowingTheSettingsViewButton = false
                                            mapxusController.isSearchingAllFacilitiesOnEveryBuilding = false
                                            isSearchingAnythingInTheBuilding = ""
                                            isGettingBuildingFacilityCategory = destination
                                            mapxusController.isGettingBuildingFacilityCategory = destination
                                            isAdjustingSheetHeight(height: 240, isLarge: true)
                                            
                                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6, execute: {
                                                mapxusController.navigationDestinationPath.append("BuildingFacilityView")
                                            })
                                        })
                                    })
                                    .frame(width: 80, alignment: .center)
                                })
                            })
                            .padding(.top, 8)
                            .padding(.horizontal, 16)
                        })
                        .transition(.move(edge: .bottom))
                        .matchedGeometryEffect(id: "MapxusMap-Searching-Animation", in: searchingAnimation)
                        .animation(.easeInOut(duration: 0.3), value: isSearchingAnythingInTheBuilding)
                    } else if !isSearchingAnythingInTheBuilding.isEmpty {
                        if mapxusController.isShowingLoadingOnBuildingFacilities {
                            ProgressView()
                                .progressViewStyle(GaugeProgressStyle())
                                .frame(width: 50, height: 50)
                                .contentShape(Circle())
                                .padding()
                                .transition(.move(edge: isShowingSearchingOption ? .top : .bottom))
                                .animation(.smooth, value: isShowingSearchingOption)
                        } else {
                            ScrollView(.vertical, showsIndicators: false, content: {
                                VStack(spacing: 8, content: {
                                    ForEach(filteredFacilitiesBySearchingValue.sorted(by: { a, b in
                                        
                                        // 3. SORTING LOGIC
                                        // We find the index of the floor name in your custom order array
                                        let indexA = isShortingTheBuildingFacilities.firstIndex(of: a.floorName) ?? 999
                                        let indexB = isShortingTheBuildingFacilities.firstIndex(of: b.floorName) ?? 999
                                        
                                        return indexA < indexB
                                        
                                    }), id: \.id, content: { facility in
                                        let status = toiletOccupancyClass.getRestroomOccupancyStatusAll(poiId: facility.id, statuses: mapxusController.vacantToiletStatuses(languageCode: mapxusController.selectedLanguage), languageCode: mapxusController.selectedLanguage, allBuildings: $mapxusController.isSearchingAllFacilitiesOnEveryBuilding)
                                        let newStatusText = status.message
                                        let newStatusColor = mapxusController.vacantToiletStatusColor(status: status.message, languageCode: mapxusController.selectedLanguage)
                                        
                                        BuildingFacilityLists(
                                            toiletName: facility.facilityName,
                                            floorName: facility.floorName,
                                            buildingNumber: facility.buildingNumber,
                                            category: facility.category.first ?? "",
                                            categoryType: facility.categoryCode.values.first?.first ?? "",
                                            status: newStatusText,
                                            statusColor: newStatusColor,
                                            icon: facility.iconName, // Use iconName for the image, label for the text
                                            isLoading: $toiletOccupancyClass.isLoadingOnWashroomOccupancy,
                                            languageCode: mapxusController.selectedLanguage,
                                            action: {
                                                mapxusController.isSearchingAllFacilitiesOnEveryBuilding = false
                                                focusedState = nil
                                                isGettingBuildingFacilityCategory = facility.category.first ?? ""
                                                mapxusController.focusPoi = MapPoi(id: facility.id, lat: facility.lat, lng: facility.lon, facilityName: facility.facilityName, floorId: facility.floorId, floorName: facility.floorName)
                                                mapxusController.isGettingDestinationName = facility.facilityName
                                                mapxusController.isGettingDestinationFloorName = facility.floorName
                                                mapxusController.isGettingBuildingNumber = facility.buildingNumber
                                                
                                                isSearchingAnythingInTheBuilding = ""
                                                withAnimation(.smooth(), {
                                                    isUsingSearchAnythingFeature = true
                                                    mapxusController.isShowingTheSettingsViewButton = false
                                                    mapxusController.isDisablingCreatingDestinationMarker = true
                                                    
                                                    mapxusController.navigationDestinationPath.append("ConfirmingDestinationView")
                                                    
                                                    isAdjustingSheetHeight(height: 240, isLarge: false)
                                                })
                                            }
                                        )
                                        .id(facility.id)
                                    })
                                })
                                .padding(.top, 16)
                                .padding(.horizontal, 16)
                                .transition(.move(edge: .top))
                                .matchedGeometryEffect(id: "MapxusMap-Searching-Animation", in: searchingAnimation)
                                .animation(.easeInOut(duration: 0.3), value: isSearchingAnythingInTheBuilding)
                            })
                        }
                    }
                })
            })
            .scrollDismissesKeyboard(.immediately)
            
        })
        .navigationBarBackButtonHidden(true)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            mapxusController.mapState = .welcoming
                            mapxusController.clearAllMarkersAlongWithTheInstructionLists()
                            isShowingSearchingOption = false
                            mapxusController.isSearchingAllFacilitiesOnEveryBuilding = false
                            mapxusController.isShowingTheSettingsViewButton = true
                            mapxusController.isFetchingWashroomOccupancy = false
                            mapxusController.isGettingLastBuildingIdOnMapView = isGettingBuildingId
                            mapxusController.isGettingBuildingNumber = ""
                            isSearchingAnythingInTheBuilding = ""
                            mapxusController.navigationDestinationPath.removeLast() // This pops the view back to the previous page
                            isAdjustingSheetHeight(height: 240, isLarge: false)
                        }
                    })
                })
            })
            
            /// For testing on TestFlight
//            ToolbarItem(placement: .topBarTrailing, content: {
//                // Use a 'let' to capture the results of the tuple
//                if let result = mapxusController.userInsideTheBuildingStatus(), result.isInside {
//                    PulseAnimationView(color: result.pulseColor)
//                        .onTapGesture(perform: {
//                            allViewReceiver.showGeneralToast(
//                                message: result.message,
//                                icon: "building.2.crop.circle.fill",
//                                iconColor: Color.mainColor,
//                                show: true
//                            )
//                        })
//                }
//            })
        })
    }
    
    @ViewBuilder
    func BuildingFacilityView(category: String) -> some View {
        VStack(spacing: 8, content: {
            // 1. Category Filter Buttons
            ScrollView(.horizontal, showsIndicators: false, content: {
                HStack(spacing: 8, content: {
                    // "All" Button to reset filter
                    BuildingFacilityListFilterButton(title: getCategoryFacilityDefaultInMultipleLanguage(languangeCode: mapxusController.selectedLanguage), backgroundColor: isSelectingToiletCategory == nil ? Color.mainColor : Color.clear, foregroundColor: isSelectingToiletCategory == nil ? Color.white : Color.primary, action: {
                        isSelectingToiletCategory = nil
                    })
                    .transition(.opacity)
                    .opacity(isSelectingToiletCategory == nil ? 1.0 : 0.6) // Visual feedback for selection
                    
                    let availableSubFilters: [String] = {
                        // 1. Filter using .contains() since $0.category is now a List
                        let currentCategoryFacilities = mapxusController.buildingFacilities.filter({
                            $0.category.contains(category)
                        })
                        
                        // 2. Extract and Flatten
                        let allKeys = currentCategoryFacilities.flatMap({
                            $0.categoryCode.values.joined()
                        }).map({ code in
                            // Clean "facility.toilet.male" -> "Male"
                            getCategoryFacilityInMultipleLanguage(rawCode: code, languageCode: mapxusController.selectedLanguage)
                        })
                        
                        // 3. Remove duplicates using Set
                        let uniqueFilters = Set(allKeys)
                        
                        // 4. Return sorted array
                        return Array(uniqueFilters).sorted()
                    }()
                    
                    ForEach(availableSubFilters, id: \.self, content: { type in
                        BuildingFacilityListFilterButton(
                            title: type,
                            backgroundColor: isSelectingToiletCategory == type ? Color.mainColor : Color.clear,
                            foregroundColor: isSelectingToiletCategory == type ? Color.white : Color.primary,
                            action: {
                                withAnimation(.easeInOut(duration: 0.3)) {
                                    if isSelectingToiletCategory == type {
                                        isSelectingToiletCategory = nil
                                    } else {
                                        isSelectingToiletCategory = type
                                    }
                                }
                            }
                        )
                        .opacity(isSelectingToiletCategory == type ? 1.0 : 0.6)
                    })
                })
                .padding([.leading, .trailing, .top], 16)
            })
            
            // 2. Filtered List of Toilets
            ScrollView(.vertical, showsIndicators: false, content: {
                VStack(alignment: .center, spacing: 8, content: {
                    if mapxusController.isShowingLoadingOnBuildingFacilities {
                        Spacer()
                        ProgressView()
                            .progressViewStyle(GaugeProgressStyle())
                            .frame(width: 50, height: 50)
                            .contentShape(Circle())
                            .padding()
                        Spacer()
                    } else {
                        // 1. Store the filtered and sorted results in a constant
                        let filteredList = mapxusController.buildingFacilities.filter({ list in
                            // 1. Check if the category list contains the target category string
                            let matchesBuildingId = list.buildingId.contains(isGettingBuildingId)
                            let matchesMainCategory = list.category.contains(category)
                            
                            let matchesSubFilter: Bool
                            if let selectedType = isSelectingToiletCategory {
                                // FlatMap the values to ensure you're searching through all codes in the dictionary
                                matchesSubFilter = list.categoryCode.values.flatMap { $0 }.contains(where: {
                                    getCategoryFacilityInMultipleLanguage(rawCode: $0, languageCode: mapxusController.selectedLanguage) == selectedType
                                })
                            } else {
                                matchesSubFilter = true
                            }
                            
                            return matchesBuildingId && matchesMainCategory && matchesSubFilter
                        }).sorted(by: { a, b in
                            let indexA = isShortingTheBuildingFacilities.firstIndex(of: a.floorName) ?? 999
                            let indexB = isShortingTheBuildingFacilities.firstIndex(of: b.floorName) ?? 999
                            return indexA < indexB
                        })

                        // 2. Logic to show either the "Empty State" or the "List"
                        if filteredList.isEmpty {
                            VStack(alignment: .center, spacing: 16, content: {
                                if mapxusController.isShowingLossInternetConnectionButton {
                                    SpecificSectionBuildingItem(name: "Refresh Building Facility", icon: "wifi", action: {
                                        Task(operation: {
                                            await MainActor.run {
                                                mapxusController.isShowingLossInternetConnectionButton = false
                                                mapxusController.isShowingLoadingOnBuildingFacilities = true
                                            }
                                            
                                            mapxusController.findAllIndoorCoordinatesBasedOnBuildingId(buildingId: isGettingBuildingId, keyword: .constant(""), allBuilding: .constant(false))
                                        })
                                    })
                                    .transition(.move(edge: mapxusController.isShowingLossInternetConnectionButton ? .top : .bottom))
                                    .padding()
                                }
                                
                                if mapxusController.isShowingAnEmptyFacilityCategory {
                                    VStack(alignment: .center, spacing: 12, content: {
                                        Image("facilities_not_found", bundle: Bundle.mapxus)
                                            .renderingMode(.template)
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 40, height: 40, alignment: .center)
                                            .foregroundColor(.gray.opacity(0.5))
                                        
                                        Text(translationClass.categoryNotFound(category: category, code: mapxusController.selectedLanguage))
                                            .font(.system(size: 14, weight: .light))
                                            .foregroundColor(Color.secondary)
                                            .multilineTextAlignment(.center)
                                            .padding(.horizontal)
                                    })
                                    .padding()
                                }
                            })
                            .frame(maxWidth: .infinity)
                        } else {
                            // 3. If there are items, run the ForEach
                            ForEach(filteredList, id: \.id, content: { facility in
                                let status = toiletOccupancyClass.getRestroomOccupancyStatusAll(poiId: facility.id, statuses: mapxusController.vacantToiletStatuses(languageCode: mapxusController.selectedLanguage), languageCode: mapxusController.selectedLanguage, allBuildings: .constant(false))
                                let newStatusText = status.message
                                let newStatusColor = mapxusController.vacantToiletStatusColor(status: status.message, languageCode: mapxusController.selectedLanguage)
                                
                                BuildingFacilityLists(
                                    toiletName: facility.facilityName,
                                    floorName: facility.floorName,
                                    buildingNumber: facility.buildingNumber,
                                    category: facility.category.first ?? "",
                                    categoryType: facility.categoryCode.values.first?.first ?? "",
                                    status: newStatusText,
                                    statusColor: newStatusColor,
                                    icon: facility.iconName,
                                    isLoading: .constant(false),
                                    languageCode: mapxusController.selectedLanguage,
                                    action: {
                                        mapxusController.focusPoi = MapPoi(id: facility.id, lat: facility.lat, lng: facility.lon, facilityName: facility.facilityName, floorId: facility.floorId, floorName: facility.floorName)
                                        mapxusController.isGettingDestinationName = facility.facilityName
                                        mapxusController.isGettingDestinationFloorName = facility.floorName
                                        mapxusController.isGettingBuildingNumber = facility.buildingNumber
                                        
                                        mapxusController.navigationDestinationPath.append("ConfirmingDestinationView")
                                        mapxusController.isDisablingCreatingDestinationMarker = true
                                        isRepeatingToiletOccupancy = false
                                        mapxusController.isGettingWashroomVacantStatusMessage = ""
                                        
                                        isAdjustingSheetHeight(height: 240, isLarge: false)
                                        
                                        print("mapxus id: \(facility.id)")
                                    }
                                )
                            })
                        }
                    }
                })
                .padding(16)
            })
        })
        .navigationBarBackButtonHidden(true)
        .navigationTitle(category)
        .navigationBarTitleDisplayMode(.inline)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            mapxusController.navigationDestinationPath.removeLast() // This pops the view back to the previous page
                            mapxusController.isDisablingCreatingDestinationMarker = false
                            isSelectingToiletCategory = nil
                            
                            isAdjustingSheetHeight(height: 270, isLarge: false)
                            
                            mapxusController.clearAllMarkersAlongWithTheInstructionLists()
                        }
                    })
                })
            })
            
            ToolbarItem(placement: .topBarTrailing, content: {
                if category == buildingFacilityCategoriesInMultipleLanguages(for: mapxusController.selectedLanguage)[3] {
                    CustomRotationAnimationButton(icon: "refresh_4", iconColor: Color.mainColor, backgroundColor: Color.secondaryMainColor, action: {
                        Task(operation: { @MainActor in
                            await toiletOccupancyClass.getToiletStatusWithoutAutomaticRefresh(buildingId: isGettingBuildingId)
                        })
                    }, isAnimating: $toiletOccupancyClass.isLoading)
                }
            })
        })
    }
    
    @ViewBuilder
    func ConfirmingDestinationView(destinationName: String, floorName: String, building: String, category: String) -> some View {
        GeneralStruct(content: {
            VStack(spacing: 16, content: {
                VStack(alignment: .leading, spacing: 8, content: {
                    HStack(spacing: 8, content: {
                        VStack(alignment: .leading, spacing: 8, content: {
                            Text(destinationName)
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(Color.primary)
                            
                            Text("\(floorName) ⋅ \(building)")
                                .font(.system(size: 14, weight: .light))
                                .foregroundColor(Color.secondary)
                        })
                        
                        Spacer()
                        
                        if !mapxusController.isGettingWashroomVacantStatusMessage.isEmpty {
                            WashroomStatus(status: mapxusController.isGettingWashroomVacantStatusMessage, statusColor: mapxusController.isGettingWashroomVacantStatusColor, isLoading: .constant(mapxusController.isGettingWashroomVacantStatusMessage), languageCode: mapxusController.selectedLanguage)
                                .padding(.horizontal, 16)
                                .frame(height: 50)
                                .background(Color.secondaryMainColor.opacity(0.3))
                                .overlay(alignment: .center, content: {
                                    RoundedRectangle(cornerRadius: 34)
                                        .stroke(.ultraThickMaterial, lineWidth: 1)
                                })
                                .cornerRadius(34)
                        }
                    })
                    .frame(maxWidth: .infinity, alignment: .leading)
                })
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 8)
                .padding(.horizontal, 16)
                
                CustomMainButton(label: translationClass.direction(code: mapxusController.selectedLanguage), action: {
                    mapxusController.mapState = .selectingCurrentLocation
                    isSelectingToiletCategory = nil
                    
                    isAdjustingSheetHeight(height: 240, isLarge: false)
                    mapxusController.navigationDestinationPath.append("SelectYourLocationView")
                }, disabled: mapxusController.endPoint == nil, isLoading: false)
                .padding(.horizontal, 16)
            })
        })
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                PublicCustomBackButton(icon: "arrow.backward", action: {
                    if !mapxusController.navigationDestinationPath.isEmpty {
                        mapxusController.mapState = .initial
                        mapxusController.clearSelectedMarkers(withTitle: "Destination")
                        mapxusController.clearRoute()
                        mapxusController.clearAllMarkersAlongWithThePoints()
                        mapxusController.isDisablingCreatingDestinationMarker = false
                        mapxusController.isGettingWashroomVacantStatusMessage = ""
                        
                        if mapxusController.isNavigatingToConfirmingDestinationView || isUsingSearchAnythingFeature {
                            mapxusController.isNavigatingToConfirmingDestinationView = false
                            isUsingSearchAnythingFeature = false
                            isAdjustingSheetHeight(height: 270, isLarge: false)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.0, execute: {
                                mapxusController.navigationDestinationPath.removeLast()
                            })
                        } else {
                            isAdjustingSheetHeight(height: 240, isLarge: true)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6, execute: {
                                mapxusController.navigationDestinationPath.removeLast()
                            })
                        }
                    }
                })
            })
        })
    }
    
    @ViewBuilder
    func SelectYourLocationView() -> some View {
        VStack(content: {
            GeneralStruct(content: {
                VStack(spacing: 8, content: {
                    CustomMenuButton(label: translationClass.selectStartPoint(code: mapxusController.selectedLanguage), icon: "user-pin-1", content: {
                        ContentMenuButton(title: translationClass.currentLocation(code: mapxusController.selectedLanguage), icon: "gps-icon", action: {
                            withAnimation(.easeInOut(duration: 0.7), {
                                mapxusController.createMarkerBasedOnUserGPS()
                                
                                mapxusController.isFoldingFloorBarSection(fold: true)
                                isUsingCurrentLocation = true
                                isAdjustingSheetHeight(height: 360, isLarge: false)
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
                                    mapxusController.navigationDestinationPath.append("StartMapxusMapNavigation")
                                })
                            })
                        })
                        ContentMenuButton(title: translationClass.selectLocationFromMap(code: mapxusController.selectedLanguage), icon: "user-pin-1", action: {
                            mapxusController.mapState = .selectingCurrentLocationByPinningOnMap
                            mapxusController.navigationDestinationPath.append("SetStartLocation")
                            mapxusController.isSelectingLocationByPIN = true
                            isUsingCurrentLocation = false
                        })
                    })
                    
                    StartDestinationNavigationLabel(label: "\(mapxusController.isGettingDestinationName), \(mapxusController.isGettingDestinationFloorName), \(mapxusController.isGettingBuildingNumber)", icon: "flag.fill")
                        .onTapGesture(perform: {
                            withAnimation(.spring(), {
                                toggleTooltip(.destination)
                            })
                        })
                        .overlay(alignment: .top, content: {
                            if activeTooltip == .destination, let items = destinationLabelTooltip {
                                Tooltip(items: items, type: .top)
                                    .fixedSize()
                                    .offset(y: -30)
                                    .transition(.asymmetric(
                                        insertion: .scale(scale: 0.5, anchor: .trailing).combined(with: .opacity),
                                        removal: .opacity.combined(with: .scale(scale: 0.8))
                                    ))
                                    .onTapGesture(perform: {
                                        dismissTooltip()
                                    })
                                    .task(id: activeTooltip, {
                                        try? await Task.sleep(nanoseconds: 3_500_000_000)
                                        dismissTooltip()
                                    })
                            }
                        })
                })
                .padding()
            })
        })
        .navigationTitle(translationClass.navigation(code: mapxusController.selectedLanguage))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            mapxusController.navigationDestinationPath.removeLast() // This pops the view back to the previous page
                            isAdjustingSheetHeight(height: 240, isLarge: false)
                            
                            mapxusController.mapState = .selectingDestinationLocation
                            mapxusController.hideRoute()
                            mapxusController.clearSelectedMarkers(withTitle: "Start")
                            
                            isShowingRouteOrNot = false
                        }
                    })
                })
            })
        })
    }
    
    @ViewBuilder
    func SetStartLocation() -> some View {
        VStack(content: {
            GeneralStruct(content: {
                VStack(content: {
                    CustomMainButton(label: translationClass.setStartLocation(code: mapxusController.selectedLanguage), action: {
                        mapxusController.setStartLocationFromCenterPin()
                        mapxusController.isSelectingLocationByPIN = false
                        
                        isAdjustingSheetHeight(height: 360, isLarge: false)
                        
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
                            mapxusController.navigationDestinationPath.append("StartMapxusMapNavigation")
                        })
                    }, disabled: mapxusController.centerPinCoordinate == nil)
                })
                .padding(16)
            })
        })
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            mapxusController.mapState = .selectingCurrentLocation
                            isAdjustingSheetHeight(height: 240, isLarge: false)
                            
                            mapxusController.navigationDestinationPath.removeLast() // This pops the view back to the previous page
                            
                            mapxusController.isFoldingFloorBarSection(fold: false)
                            mapxusController.clearSelectedMarkers(withTitle: "Start")
                            mapxusController.startPoint = nil
                            
                            mapxusController.isSelectingLocationByPIN = false
                        }
                    })
                })
            })
        })
    }
    
    @ViewBuilder
    func StartMapxusMapNavigation() -> some View {
        VStack(content: {
            GeneralStruct(content: {
                VStack(spacing: 8, content: {
                    StartDestinationNavigationLabel(label: mapxusController.startLocationCoord, icon: "user-pin-1", alignment: .leading)
                    .onTapGesture(perform: {
                        withAnimation(.spring(), {
                            toggleTooltip(.start)
                        })
                    })
                    .overlay(alignment: .top, content: {
                        if activeTooltip == .start, let items = startLocationLabelTooltip {
                            Tooltip(items: items, type: .top)
                                .fixedSize()
                                .offset(y: -30)
                                .transition(.asymmetric(
                                    insertion: .scale(scale: 0.5, anchor: .trailing).combined(with: .opacity),
                                    removal: .opacity.combined(with: .scale(scale: 0.8))
                                ))
                                .onTapGesture(perform: {
                                    dismissTooltip()
                                })
                                .task(id: activeTooltip, {
                                    try? await Task.sleep(nanoseconds: 3_500_000_000)
                                    dismissTooltip()
                                })
                                .zIndex(.infinity)
                        }
                    })
                    
                    StartDestinationNavigationLabel(label: "\(mapxusController.isGettingDestinationName), \(mapxusController.isGettingDestinationFloorName), \(mapxusController.isGettingBuildingNumber)", icon: "flag.fill", alignment: .leading)
                    .onTapGesture(perform: {
                        withAnimation(.spring(), {
                            toggleTooltip(.destination)
                        })
                    })
                    .overlay(alignment: .top, content: {
                        if activeTooltip == .destination, let items = destinationLabelTooltip {
                            Tooltip(items: items, type: .bottom)
                                .fixedSize()
                                .offset(y: 30)
                                .transition(.asymmetric(
                                    insertion: .scale(scale: 0.5, anchor: .trailing).combined(with: .opacity),
                                    removal: .opacity.combined(with: .scale(scale: 0.8))
                                ))
                                .onTapGesture(perform: {
                                    dismissTooltip()
                                })
                                .task(id: activeTooltip, {
                                    try? await Task.sleep(nanoseconds: 3_500_000_000)
                                    dismissTooltip()
                                })
                        }
                    })
                })
                .padding(.top, 16)
                .padding(.bottom, 4)
                .padding(.horizontal, 16)
                
                VStack(spacing: 16, content: {
                    VStack(alignment: .leading, spacing: 8, content: {
                        VStack(alignment: .leading, spacing: 4, content: {
                            Text(translationClass.routeType(code: mapxusController.selectedLanguage))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Color.secondary)
                                .padding(.horizontal, 32)
                            
                            ScrollView(.horizontal, showsIndicators: false, content: {
                                HStack(content: {
                                    ForEach(routeTypeList(code: mapxusController.selectedLanguage), id: \.self, content: { routeType in
                                        // Check if this specific button is the one selected
                                        let currentSelection = mapxusController.activeRouteType.isEmpty ?
                                                                   mapxusController.defaultRouteType(code: mapxusController.selectedLanguage) :
                                                                   mapxusController.activeRouteType
                                        
                                        // 2. Compare the button to the current effective selection
                                        let isSelected = currentSelection == routeType
                                        
                                        BuildingFacilityListFilterButton(
                                            title: routeType,
                                            backgroundColor: isSelected ? Color.mainColor : Color(.systemGray6),
                                            foregroundColor: isSelected ? Color.white : Color.mainColor,
                                            action: {
                                                withAnimation(.smooth, {
                                                    mapxusController.activeRouteType = routeType
                                                })
                                            }
                                        )
                                    })
                                })
                                .padding(.horizontal, 16)
                            })
                        })
                        
//                        VStack(alignment: .leading, spacing: 4, content: {
//                            HStack(content: {
//                                Text(translationClass.offRoute(code: mapxusController.selectedLanguage))
//                                    .font(.system(size: 16, weight: .semibold))
//                                    .foregroundColor(Color.secondary)
//                                
//                                Spacer()
//                                
//                                Text("\(Int(offRouteThreshold))\(translationClass.meter(plural: true, code: mapxusController.selectedLanguage))")
//                                    .font(.system(size: 16, weight: .semibold))
//                                    .foregroundColor(Color.mainColor)
//                            })
//                            .padding(.horizontal, 12)
//                            
//                            CustomSlider(progress: $offRouteThreshold)
//                        })
//                        .padding(.horizontal, 20)
                    })
                    
                    VStack(spacing: 8, content: {
                        CustomShowRouteButton(label: translationClass.showRoute(code: mapxusController.selectedLanguage, state: false), action: {
                            withAnimation(.smooth, {
//                                mapxusController.mapState = .showingRoute
                                isAdjustingSheetHeight(height: 240, isLarge: false)
                                mapxusController.navigationDestinationPath.append("ShowRoute")
                                
                                isShowingRouteOrNot = true
                                
                                mapxusController.showRoute(routeOption: mapxusController.selectedRouteType(type: mapxusController.activeRouteType, languageCode: mapxusController.selectedLanguage))
                            })
                        }, color: $isShowingRouteOrNot, disabled: mapxusController.startPoint == nil || mapxusController.endPoint == nil)
                        
                        CustomMainButton(label: translationClass.startNavigation(code: mapxusController.selectedLanguage), action: {
                            withAnimation(.smooth, {
                                isShowingARButtons = true
                                isShowingRouteOrNot = false
                                isDisablingClosingTheSheet = false
                                isShowingSheet = false
                            })
                            
                            // 3. THE HEAVY LIFTING (The "Logic" Phase)
                            // We wait 0.3s for the sheets to actually disappear before hitting the CPU
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.0, execute: {
                                // Prevent phone from sleeping
                                UIApplication.shared.isIdleTimerDisabled = true
                                withAnimation(.smooth(), {
                                    mapxusController.startNavigation()
                                })
                            })
                        }, disabled: mapxusController.startPoint == nil && mapxusController.endPoint == nil)
                    })
                    .padding(.horizontal, 16)
                    
                })
            })
        })
        .navigationTitle(translationClass.navigation(code: mapxusController.selectedLanguage))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            withAnimation(.smooth(), {
                                isAdjustingSheetHeight(height: 240, isLarge: false)
                                
                                if isUsingCurrentLocation {
                                    mapxusController.navigationDestinationPath.removeLast(1)
                                } else {
                                    mapxusController.navigationDestinationPath.removeLast(2)
                                }
                                
                                isShowingRouteOrNot = false
                                mapxusController.isFoldingFloorBarSection(fold: false)
                                mapxusController.hideRoute()
                            })
                            mapxusController.clearSelectedMarkers(withTitle: "Start")
                        }
                    })
                })
            })
        })
    }
    
    @ViewBuilder
    func ShowRoute() -> some View {
        GeneralStruct(content: {
            VStack(spacing: 16, content: {
                VStack(alignment: .leading, spacing: 4, content: {
                    Text(translationClass.routeType(code: mapxusController.selectedLanguage))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color.secondary)
                        .padding(.horizontal, 32)
                        .padding(.top, 16)
                    
                    ScrollView(.horizontal, showsIndicators: false, content: {
                        HStack(content: {
                            ForEach(routeTypeList(code: mapxusController.selectedLanguage), id: \.self, content: { routeType in
                                // Check if this specific button is the one selected
                                let currentSelection = mapxusController.activeRouteType.isEmpty ?
                                                           mapxusController.defaultRouteType(code: mapxusController.selectedLanguage) :
                                                           mapxusController.activeRouteType
                                
                                // 2. Compare the button to the current effective selection
                                let isSelected = currentSelection == routeType
                                
                                BuildingFacilityListFilterButton(
                                    title: routeType,
                                    backgroundColor: isSelected ? Color.mainColor : Color(.systemGray6),
                                    foregroundColor: isSelected ? Color.white : Color.mainColor,
                                    action: {
                                        withAnimation(.smooth, {
                                            isShowingRouteOrNot = true
                                            mapxusController.activeRouteType = routeType
                                        })
                                        
                                        mapxusController.showRoute(routeOption: routeType)
                                        
                                        // Add your logic to change the map route type here
                                        print("Selected Route Type: \(routeType)")
                                    }
                                )
                            })
                        })
                        .padding(.horizontal, 16)
                    })
                })
                
                VStack(alignment: .center, spacing: 8, content: {
                    CustomShowRouteButton(label: translationClass.showRoute(code: mapxusController.selectedLanguage, state: isShowingRouteOrNot), action: {
                        withAnimation(.smooth, {
                            isShowingRouteOrNot.toggle()
                            
                            if isShowingRouteOrNot {
                                mapxusController.showRoute(routeOption: mapxusController.selectedRouteType(type: mapxusController.activeRouteType, languageCode: mapxusController.selectedLanguage))
                            } else {
                                mapxusController.hideRoute()
                            }
                        })
                    }, color: $isShowingRouteOrNot, disabled: mapxusController.startPoint == nil || mapxusController.endPoint == nil)
                    
                    CustomMainButton(label: translationClass.startNavigation(code: mapxusController.selectedLanguage), action: {
                        withAnimation(.smooth, {
                            isShowingARButtons = true
                            isShowingRouteOrNot = false
                            isDisablingClosingTheSheet = false
                            isShowingSheet = false
                        })
                        
                        // 3. THE HEAVY LIFTING (The "Logic" Phase)
                        // We wait 0.3s for the sheets to actually disappear before hitting the CPU
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.0, execute: {
                            // Prevent phone from sleeping
                            UIApplication.shared.isIdleTimerDisabled = true
                            withAnimation(.smooth(), {
                                mapxusController.startNavigation()
                            })
                        })
                    }, disabled: mapxusController.startPoint == nil && mapxusController.endPoint == nil)
                })
                .padding(.horizontal, 16)
            })
        })
        .navigationTitle(translationClass.showRoute(code: mapxusController.selectedLanguage, state: false))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                HStack(spacing: 8, content: {
                    PublicCustomBackButton(icon: "arrow.backward", action: {
                        if !mapxusController.navigationDestinationPath.isEmpty {
                            withAnimation(.smooth(), {
                                mapxusController.mapState = .showingNavigationDetails
                                isShowingRouteOrNot = false
                                mapxusController.hideRoute()
                                isAdjustingSheetHeight(height: 360, isLarge: false)
                            })
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
                                mapxusController.navigationDestinationPath.removeLast()
                            })
                        }
                    })
                })
            })
        })
        
    }
    
    @ViewBuilder
    func NavigationDirectionCard() -> some View {
        if (mapxusController.mapState == .navigating) {
            VStack(content: {
                NavigationCard(
                    mapxusController: mapxusController,
                    direction: mapxusController.currentInstructionTitle,
                    distance: mapxusController.currentInstructionDistance,
                    onPrevious: {
                        mapxusController.previousStep()
                    },
                    onNext: {
                        mapxusController.nextStep()
                    })
            })
        }
    }
}

struct MapxusMapIntroduction: Identifiable {
    var id: Int
    var title: String
    var heading: String
    var subHeading: String
    var icon: String
}

private struct IntroductionSection: View {
    @StateObject var mapxusController: MapxusController
    @Binding var currentPage: Int?
    var action: () -> Void
    
    @StateObject private var networkMonitorClass: NetworkMonitorClass = NetworkMonitorClass()
    @StateObject private var translationClass: TranslationClass = TranslationClass()
    
    @State private var getStartedOrAccessPermission: String = ""
    
    private var introductions: [MapxusMapIntroduction] {
        let translation = TranslationClass()
        let code = mapxusController.selectedLanguage
        return [
            MapxusMapIntroduction(
                id: 0,
                title: translation.indoorMaps(code: code),
                heading: translation.indoorMapsHeading(code: code),
                subHeading: translation.indoorMapsSubHeading(code: code),
                icon: "indoor-navigation"
            ),
            MapxusMapIntroduction(
                id: 1,
                title: translation.arNavigation(code: code),
                heading: translation.arNavigationHeading(code: code),
                subHeading: translation.arNavigationSubHeading(code: code),
                icon: "ar-navigation-introduction"
            ),
            MapxusMapIntroduction(
                id: 2,
                title: translation.smartSearch(code: code),
                heading: translation.smartSearchHeading(code: code),
                subHeading: translation.smartSearchSubHeading(code: code),
                icon: "smart-search"
            ),
            MapxusMapIntroduction(
                id: 3,
                title: translation.permissionScreen(code: code),
                heading: translation.permissionScreenHeading(code: code),
                subHeading: translation.permissionScreenSubHeading(code: code),
                icon: "permission"
            ),
            MapxusMapIntroduction(
                id: 4,
                title: translation.wifiIntroduction(code: code),
                heading: translation.wifiIntroductionHeading(code: code),
                subHeading: translation.wifiIntroductionSubHeading(code: code),
                icon: "wi-fi"
            )
        ]
    }
    
    private var isDisabled: Bool {
        // 1. Safely unwrap the optional Int
        guard let page = currentPage else { return true }
        
        // 2. Switch on the unwrapped value
        switch page {
        case 0...2:
            return true
        case 3:
            return isNavigationAuthorized
        case 4:
            if isNavigationAuthorized && networkMonitorClass.isWifi {
                return false // Enabled: Go to Map
            } else if !networkMonitorClass.isWifi {
                return false // Enabled: Click to open Wi-Fi Settings
            }
            return true // Disabled: Still waiting on Permissions
        default:
            return true
        }
    }
    
    // 1. Unified check for authorization status
    var isNavigationAuthorized: Bool {
        let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
        let locationStatus = CLLocationManager().authorizationStatus
        
        let cameraOk = cameraStatus == .authorized
        let locationOk = (locationStatus == .authorizedWhenInUse || locationStatus == .authorizedAlways)
        
        return cameraOk && locationOk
    }

    // 2. Modernized Permission Flow
    func checkPermissions(completion: @escaping (Bool) -> Void) {
        Task {
            // Handle Camera
            let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
            if cameraStatus == .notDetermined {
                _ = await AVCaptureDevice.requestAccess(for: .video)
            }
            
            // Handle Location
            let manager = CLLocationManager()
            if manager.authorizationStatus == .notDetermined {
                manager.requestWhenInUseAuthorization()
                // We return here because location request is non-blocking/delegate-based
                completion(false)
                return
            }
            
            // Final Verification
            await MainActor.run {
                // 1. Check Internet First (since you want this to trigger openWifiSettings)
                if !networkMonitorClass.isWifi {
                    openAppSettings()
                    completion(false)
                    return
                }

                // 2. Then check Permissions
                if isNavigationAuthorized {
//                    updateUI(isAuthorized: true)
                    completion(true)
                } else {
                    // Permissions (Camera/Location) are missing
                    openAppSettings()
                    completion(false)
                }
            }
        }
    }
    
    private func refreshPermissionUI() {
        guard let page = currentPage else { return }
        let lang = mapxusController.selectedLanguage
        
        switch page {
        case 0...2:
            // Pages 0-2: Always show "Get Started"
            self.getStartedOrAccessPermission = translationClass.getStarted(code: lang)
            
        case 3:
            // Page 3: Focus on Camera/Location Permissions
            if isNavigationAuthorized {
                self.getStartedOrAccessPermission = translationClass.getStarted(code: lang)
            } else {
                self.getStartedOrAccessPermission = translationClass.openSettings(code: lang)
            }
            
        case 4:
            // Page 4: Focus on Wi-Fi (assuming Permissions are already handled)
            if !networkMonitorClass.isWifi {
                self.getStartedOrAccessPermission = translationClass.enableWiFi(lang: lang)
            } else {
                self.getStartedOrAccessPermission = translationClass.getStarted(code: lang)
            }
            
        default:
            self.getStartedOrAccessPermission = translationClass.getStarted(code: lang)
        }
    }

//    private func refreshPermissionUI() {
//        updateUI(isAuthorized: isNavigationAuthorized)
//    }
//
//    private func updateUI(isAuthorized: Bool) {
//        let lang = mapxusController.selectedLanguage
//        self.getStartedOrAccessPermission = isAuthorized ?
//            translationClass.getStarted(code: lang) :
//            translationClass.openSettings(code: lang)
//    }
//    
//    private func updateInternetLabel(lang: String) {
//        if networkMonitorClass.isWifi {
//            // Connected to Wi-Fi: Best experience
//            getStartedOrAccessPermission = translationClass.getStarted(code: lang)
//            print("Connected via Wi-Fi")
//        } else {
//            // No connection at all
//            getStartedOrAccessPermission = lang == "zh-Hant" ? "開啟 Wi-Fi" : (lang == "zh-Hans" ? "开启 Wi-Fi" : "Enable Wi-Fi")
//        }
//    }
    
    func openWifiSettings() {
        // Attempt 1: The most direct path to Wi-Fi
        if let wifiUrl = URL(string: "App-Prefs:root=WIFI") {
            UIApplication.shared.open(wifiUrl)
        } else if let settingsUrl = URL(string: "App-Prefs:root") {
            UIApplication.shared.open(settingsUrl)
        }
    }

    private func openAppSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
    
    var body: some View {
        VStack(alignment: .center, spacing: 16, content: {
            CustomPager(currPage: $currentPage, totalCount: introductions.count, content: {
                ForEach(introductions, id: \.id, content: { section in
                    VStack(alignment: .center, content: {
                        VStack(alignment: .center, spacing: 8, content: {
                            Image(section.icon, bundle: Bundle.mapxus)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 50, height: 50, alignment: .center)
                            
                            Text(section.title)
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(Color.black)
                                .minimumScaleFactor(0.7)
                            
                            Text(section.heading)
                                .font(.system(size: 18, weight: .regular))
                                .foregroundColor(Color.black)
                            
                            Text(section.subHeading)
                                .font(.system(size: 16, weight: .regular))
                                .foregroundColor(Color.black)
                                .multilineTextAlignment(.center)
                                .minimumScaleFactor(0.7)
                        })
                        .padding(8)
                        .frame(minWidth: 330, maxWidth: 330, maxHeight: .infinity, alignment: .center)
                    })
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentTransition(.interpolate)
                    .containerRelativeFrame(.horizontal)
                    .id(section.id)
                })
            })
            
//            CustomMainButton(label: getStartedOrAccessPermission, action: {
//                checkPermissions(completion: { granted in
//                    if granted {
//                        action()
//                    }
//                })
//            }, disabled: currentPage != (introductions.count - 1))
//            .padding(16)
            
            CustomMainButton(label: getStartedOrAccessPermission, action: {
                checkPermissions(completion: { granted in
                    if granted {
                        action()
                    }
                })
            }, disabled: isDisabled)
            .padding(16)
        })
        .frame(maxWidth: .infinity, minHeight: 400, maxHeight: 400)
        .background(Color.white)
        .overlay(alignment: .center, content: {
            RoundedRectangle(cornerRadius: 34)
                .stroke(.ultraThinMaterial, lineWidth: 2)
        })
        .cornerRadius(34)
        .padding(16)
        .task({
            refreshPermissionUI()
        })
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification), perform: { _ in
            refreshPermissionUI()
        })
        .onChange(of: currentPage, { oldValue, newValue in
//            let lang = mapxusController.selectedLanguage
//            
//            switch newValue {
//            case 3:
//                refreshPermissionUI()
//            case 4:
//                updateInternetLabel(lang: lang)
//            default:
//                getStartedOrAccessPermission = translationClass.getStarted(code: lang)
//            }
            refreshPermissionUI()
        })
        .onChange(of: networkMonitorClass.isWifi, { _, _ in
//            if currentPage == 4 {
//                updateInternetLabel(lang: mapxusController.selectedLanguage)
//            }
            refreshPermissionUI()
        })
    }
}

private struct BuildingListViewButton: View {
    var buildingName: String
    var buildingNumber: String
    var section: String
    var action: () -> Void
    var body: some View {
        Button(action: {
            action()
        }, label: {
            VStack(alignment: .leading, spacing: 8, content: {
                Text("\(buildingName)\n\(buildingNumber)")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color.primary)
                    .minimumScaleFactor(0.3)
                    .multilineTextAlignment(.leading)
                
                Text(section)
                    .font(.system(size: 14, weight: .light))
                    .foregroundColor(Color.secondary)
            })
            .padding(16)
            .frame(minWidth: 340, maxWidth: 340, minHeight: 140, maxHeight: 140, alignment: .leading)
            .background(Color.clear)
            .overlay(alignment: .center, content: {
                RoundedRectangle(cornerRadius: 34)
                    .stroke(Color.primary, lineWidth: 1)
            })
            .cornerRadius(34)
        })
    }
}

private struct SpecificSectionBuildingItem: View {
    var name: String
    var icon: String
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            VStack(alignment: .center, spacing: 8, content: {
                Image(icon, bundle: Bundle.mapxus)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24, alignment: .center)
                    .padding(18)
                    .foregroundColor(Color.mainColor)
                    .background(Color.secondaryMainColor)
                    .clipShape(Circle())
                
                Text(name)
                    .font(.system(size: 10))
                    .foregroundColor(Color.primary)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
            })
        })
    }
}

private struct BuildingFacilityListFilterButton: View {
    var title: String
    var backgroundColor: Color
    var foregroundColor: Color
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Text(title)
                .padding(.horizontal, 32)
                .padding(.vertical, 8)
                .font(.system(size: 14, weight: .light))
                .foregroundColor(foregroundColor)
                .background(backgroundColor)
                .overlay(alignment: .center, content: {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(.ultraThickMaterial, lineWidth: 4)
                })
                .cornerRadius(34)
        })
    }
}

private struct BuildingFacilityLists: View {
    var toiletName: String
    var floorName: String
    var buildingNumber: String
    var category: String
    var categoryType: String
    var status: String
    var toiletAvailability: String = ""
    var statusColor: Color
    var icon: String
    @Binding var isLoading: Bool
    var languageCode: String
    var action: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            VStack(alignment: .leading, spacing: 8, content: {
                HStack(spacing: 16, content: {
                    HStack(content: {
                        VStack(content: {
                            if UIImage(systemName: icon) != nil {
                                Image(systemName: icon)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 30, height: 30)
                                    .foregroundColor(.primary)
                                    .padding(16)
                            } else {
                                Image(icon)
                                    .renderingMode(.template)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 30, height: 30)
                                    .foregroundColor(.primary)
                                    .padding(16)
                            }
                        })
                        .frame(width: 44, height: 44)
                        .background(colorScheme == .dark ? Color.black : Color.white)
                        .clipShape(Circle())
                        
                        VStack(alignment: .leading, spacing: 8, content: {
                            Text(toiletName)
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(Color.primary)
                                .multilineTextAlignment(.leading)
                                .minimumScaleFactor(0.7)
                            
                            Text("\(floorName) ⋅ \(buildingNumber)")
                                .font(.system(size: 12, weight: .light))
                                .foregroundColor(Color.secondary)
                        })
                    })
                    .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Spacer()
                    
                    if categoryType.lowercased() != "facility.shower" && category.lowercased() == "washroom" {
                        WashroomStatus(status: status, statusColor: statusColor, isLoading: .constant(status), languageCode: languageCode)
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.white)
                            .cornerRadius(18)
                    }
                })
            })
            .padding(16)
            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 100, alignment: .center)
            .background(.ultraThinMaterial)
            .cornerRadius(34)
        })
    }
}

struct WashroomStatus: View {
    var status: String
    var toiletAvailablity: String = ""
    var statusColor: Color
    @Binding var isLoading: String
    var languageCode: String
    
    @ObservedObject private var translationClass: TranslationClass = TranslationClass()
    
    var body: some View {
        Label(title: {
//            (Text(status) + Text(toiletAvailability))
//                .font(.system(size: 16, weight: .semibold))
//                .foregroundColor(.primary)
//                .minimumScaleFactor(0.3)
//                .lineLimit(1) // Ensures it stays on one line while scaling
            
            Text(status)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.primary)
                .minimumScaleFactor(0.3)
                .lineLimit(1) // Ensures it stays on one line while scaling
        }, icon: {
            if isLoading == translationClass.loading(code: languageCode) {
                ProgressView()
                    .tint(Color.mainColor)
            } else {
                Image(systemName: "dot.circle.fill")
                    .foregroundColor(statusColor)
            }
        })
    }
}

struct SearchAnythingInTheBuilding: View {
    var placeholder: String
    @Binding var value: String
    @Binding var adjustSheet: PresentationDetent
    @FocusState var focusedState: SearchField?
    @State var action: () -> Void
    
    var body: some View {
        TextField(placeholder, text: $value)
            .padding(16)
            .padding(.leading, 28)
            .padding(.trailing, 32)
            .font(.system(size: 18, weight: .semibold))
            .foregroundColor(Color.primary)
            .textInputAutocapitalization(.words)
            .frame(maxWidth: .infinity, minHeight: 50, maxHeight: 50)
            .background(Color.clear)
            .overlay(alignment: .leading, content: {
                Image(systemName: "magnifyingglass")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
                    .foregroundColor(value.isEmpty ? Color.black : Color.mainColor)
                    .padding(.leading, 16)
            })
            .overlay(alignment: .trailing, content: {
                if !value.isEmpty {
                    Button(action: {
                        focusedState = nil
                        value = ""
                        action()
                    }, label: {
                        Image(systemName: "xmark")
                            .padding(8)
                            .foregroundColor(Color.mainColor)
                            .background(.ultraThinMaterial)
                            .overlay(alignment: .center, content: {
                                Circle()
                                    .stroke(.ultraThickMaterial, lineWidth: 1)
                            })
                            .clipShape(Circle())
                    })
                    .padding(.trailing, 8)
                }
            })
            .overlay(alignment: .center, content: {
                if !value.isEmpty {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(Color.mainColor, lineWidth: 1)
                } else {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(Color.black, lineWidth: 1)
                }
            })
            .cornerRadius(34)
            .keyboardType(.default)
            .focused($focusedState, equals: .searchAnything)
            .submitLabel(.done)
            .onSubmit({
                if !value.isEmpty {
                    adjustSheet = .large
                } else {
                    adjustSheet = .height(240)
                }
            })
            .animation(.smooth, value: value)
            .transition(.opacity)
    }
}

private struct StartDestinationNavigationLabel: View {
    var label: String
    var icon: String
    var foregroundColor: Color = Color.secondary
    var iconColor: Color = Color.mainColor
    var alignment: Alignment = Alignment.center
    
    var body: some View {
        Label(title: {
            Text(label)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(foregroundColor)
                .minimumScaleFactor(0.3)
        }, icon: {
            Image(icon, bundle: Bundle.mapxus)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(iconColor)
        })
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 50, maxHeight: 50, alignment: alignment)
        .background(.ultraThickMaterial)
        .cornerRadius(34)
    }
}

private struct TitleTextOverlayCenter: View {
    var title: String
    
    var body: some View {
        Text(title)
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(Color.primary)
            .minimumScaleFactor(0.7)
            .offset(y: -38)
    }
}

struct GeneralStruct<Content: View>: View {
    var content: (Content)
    
    // 2. Use a custom initializer with @ViewBuilder for a clean syntax
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    
    var body: some View {
        VStack(content: {
            ScrollView(.vertical, showsIndicators: false, content: {
                content
            })
        })
        .navigationBarBackButtonHidden(true)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
