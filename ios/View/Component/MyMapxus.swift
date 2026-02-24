//
//  MyMapxus.swift
//  mapxus-hsitp-ios
// 
//  Created by dev01 on 12/05/25.
//

import SwiftUI
import MapxusMapSDK
import MapxusBaseSDK
import MapxusComponentKit
import MapxusVisualSDK
import AVFoundation

class MapxusController: NSObject, ObservableObject, MapxusMapDelegate, MGLMapViewDelegate, MXMRouteSearchDelegate, MXMPoiSearchDelegate, MXMCategorySearchDelegate, MXMBuildingSearchDelegate {
    let translationClass: TranslationClass = TranslationClass()
    
    // Store this at the top of your class to track changes
    private var lastFloorId: String?
    
    @Published var isSelectingLocationByPIN: Bool = false
    
    /// New by me
    @Published var startLocationCoord: String = ""
    @Published var destinationLocationCoord: String = ""
    
    // Inside MapxusController
    @Published var centerPinCoordinate: CLLocationCoordinate2D?
    
    var markerPoints = Array<MXMPointAnnotation>()
    
    @Published var instructionList : Array<MXMInstruction> = []
    @Published var instructionPointList : Array<MXMGeoPoint> = []
    @Published var instructionIndex = 0
    @Published var currentInstructionTitle = ""
    @Published var currentInstructionDistance = ""
    
    /// From me
    @Published var useGPSandWiFi: Bool = false
    @Published var createStartMarkerAndDestinationMarker: String = ""
    @Published var estimatedTimeText: String = ""
    @Published var totalEstimationTimeToGetToTheDestination: UInt = 0
    
    var startMarker: MXMPointAnnotation?
    var endMarker: MXMPointAnnotation?
    var currentRoute: MXMRouteSign?
    var usersGPS = MXMIndoorPoint()
    
    var mapPOI = MXMPOI()
    
    var floorSearcher: MXMFloorInfo?
    
    let speechSynthesizer = AVSpeechSynthesizer()
    @AppStorage("ARNavigation-App-Enabling-TTS") private var isEnablingTTS: Bool = true
    @AppStorage("ARNavigation-App-Guide-Assistant-Language-AVSpeechSynthesisVoiceCode") private var isSelectingAssistantGuideLanguageAVSpeechSynthesisVoiceCode: String = "en-US"
//    @Published var selectedRouteType: String = "Shortest Walk" /// The default value
    @Published var activeRouteType: String = "" /// The default value
    
    @Published var isShowingArrivedAtTheDestinationAlertDialog: Bool = false
    @Published var buildingLists: [BuildingLists] = []
    @Published var buildingFacilities: [BuildingFacilityData] = []
    @Published var allBuildingFacilities: [AllBuildingFacilityData] = []
    @Published var buildingCategories: [MXMCategory] = []
    
//    @Published var currentBuildingIndex: Int? {
//        didSet {
//            // 2. Save the value whenever it changes
//            UserDefaults.standard.set(currentBuildingIndex, forKey: "MapxusMap-Building-Index")
//        }
//    }
    
    
    
    @Published var foundDeviceIds: [String] = []
    private let userClass: UserClass = UserClass.shared
    var washroomOccupancyClass: TelemetryViewModel?
    
    private let allViewReceiver: AllViewReceiver = AllViewReceiver.shared
    
    @Published var currentBuildingIndex: Int? = 0
    @Published var isGettingBuildingId: String = ""
    @Published var isGettingBuildingIdOnMapView: String = ""
    @Published var isGettingLastBuildingIdOnMapView: String = ""
    @Published var isGoingToSpecificBuildingView: Bool = false
    @Published var isGettingDestinationName: String = ""
    @Published var isGettingDestinationFloorName: String = ""
    @Published var isGettingBuildingNumber: String = ""
    @Published var isGettingBuildingFacilityCategory: String = ""
    @Published var isShowingLoadingOnBuildingFacilities: Bool = false
    @Published var isShowingTheSettingsViewButton: Bool = true
    
    @AppStorage("Mapxus-Map-Language") var selectedLanguage: String = "en"
    
    @Published var compassTrueHeading: Double = 0
    @Published var compassTruHeadingWarning: String = ""
    
    @Published var isRotatingTheMapOnGPSButtonClicked: Bool = false
    
    @Published var navigationDestinationPath: NavigationPath = NavigationPath()
    @Published var sheetHeight: CGFloat = 240
    @Published var presentationActiveDetent: PresentationDetent = .height(240)
    
    @Published var isDisablingCreatingDestinationMarker: Bool = false
    @Published var isNavigatingToConfirmingDestinationView: Bool = false
    @Published var isGettingWashroomVacantStatusMessage: String = ""
    @Published var isGettingWashroomVacantStatusColor: Color = Color.mainColor
    @Published var isDisablingGoingToSelectingCurrentLocation: Bool = false
    @Published var isFetchingWashroomOccupancy: Bool = false
    
    @Published var isOffRoute: Bool = false
    @AppStorage("Mapxus-Map-Off-Route-Threshold") private var offRouteThreshold: CLLocationDistance = 10.0 // 20 meters
    
    private var poiRetryCount = 0
    private let maxPoiRetries = 3
    @Published var poiSearch: MXMPoiSearch?
    private var lastPoiRequest: MXMPoiSearchOption? // Store the request to retry it
    
    /// Internet Connection
    @Published var isShowingLossInternetConnectionButton: Bool = false
    @Published var isShowingAnEmptyFacilityCategory: Bool = false
    
    /// AR
    @Published var isShowingARNavigation: Bool = false
    @Published var isShowingAndClosingTheARNavigation: Bool = false
    
    /// Radio Button
    @Published var isSearchingAllFacilitiesOnEveryBuilding: Bool = false
    
    init(poi: MapPoi? = nil) {
        super.init()
        
        /// 3. Load the value when the class starts up
        /// If no value exists, it defaults to 0
//        if UserDefaults.standard.object(forKey: "MapxusMap-Building-Index") != nil {
//            self.currentBuildingIndex = UserDefaults.standard.integer(forKey: "MapxusMap-Building-Index")
//        } else {
//            self.currentBuildingIndex = 0
//        }
        
        Task(operation: { @MainActor in
            self.washroomOccupancyClass = TelemetryViewModel()
        })
        
        if let safePoi = poi {
            self.focusPoi = safePoi
            mapState = .showingNavigationDetails
        }
    }
    
    enum MapState {
        case welcoming
        case initial
        case selectingDestinationLocation
        case selectingCurrentLocation
        case showingRoute
        case showingNavigationDetails
        case navigating
    }
    
    fileprivate var mapView: MGLMapView?
    fileprivate var mapxusMap: MapxusMap?
    fileprivate var buildingSearcher: MXMBuildingSearch? {
        didSet {
            print("✅ Building Searcher initialized and delegate set!")
            buildingSearcher?.delegate = self
        }
    }
    fileprivate var poiSearcher: MXMPoiSearch? {
        didSet {
            print("✅ POI Searcher initialized and delegate set!")
            poiSearcher?.delegate = self
        }
    }
    fileprivate var poiCategorySearcher: MXMCategorySearch? {
        didSet {
            print("✅ POI Category Searcher initialized and delegate set!")
            poiCategorySearcher?.delegate = self
        }
    }
    fileprivate var config: MXMConfiguration?
    fileprivate var routeSearcher: MXMRouteSearch? {
        didSet {
            if (routeSearcher != nil) {
                routeSearcher?.delegate = self
            }
        }
    }
    fileprivate var routePainter: MXMRoutePainter?
    
    @Published var mapState: MapState = .welcoming {
        didSet {
            // Add this line to catch the "culprit"
            print("🔄 mapState changed from \(oldValue) to \(mapState). Triggered by: \(Thread.callStackSymbols[1])")
            
            switch mapState {
            case .welcoming:
                setCenterView(zoomLevel: 10, bottom: 180)
                break
            case .initial:
                clearAllMarkersAlongWithTheInstructionLists()
                isDisablingCreatingDestinationMarker = false
                break
            case .selectingDestinationLocation:
                isFoldingFloorBarSection(fold: false)
                break
            case .selectingCurrentLocation:
                break
            case .showingRoute:
                isFoldingFloorBarSection(fold: true)
                break
            case .showingNavigationDetails:
                isFoldingFloorBarSection(fold: true)
                setCenterView(zoomLevel: 19, bottom: 250)
                break
            case .navigating: break
            }
        }
    }
    
    /// New by me
    var startPoint: MXMWaypoint? = nil {
        didSet {
            guard let point = startPoint else {
                DispatchQueue.main.async {
                    self.startLocationCoord = ""
                }
                return
            }

            let lat = String(format: "%.5f", point.latitude)
            let lng = String(format: "%.5f", point.longitude)
            let floorName = floorList.first(where: { $0.id == point.floorId })?.code ?? point.floorId ?? ""

            DispatchQueue.main.async {
                self.startLocationCoord = "\(self.translationClass.latitude(code: self.selectedLanguage)): \(lat), \(self.translationClass.longitude(code: self.selectedLanguage)): \(lng)"
            }

            print("🔁 startPoint updated: \(String(describing: startPoint))")
        }
    }
    
    /// New by me
    var endPoint: MXMWaypoint? = nil {
        didSet {
            guard let point = endPoint else {
                DispatchQueue.main.async {
                    self.destinationLocationCoord = ""
                }
                return
            }

            let lat = String(format: "%.5f", point.latitude)
            let lng = String(format: "%.5f", point.longitude)

            let floorName = floorList.first(where: { $0.id == point.floorId })?.code ?? point.floorId ?? ""

            DispatchQueue.main.async {
                self.destinationLocationCoord = "\(self.translationClass.latitude(code: self.selectedLanguage)): \(lat), \(self.translationClass.longitude(code: self.selectedLanguage)): \(lng)"
            }
            
            print("🔁 endPoint updated: \(String(describing: endPoint))")
        }
    }
    
    var focusPoi: MapPoi? = nil {
        didSet {
            guard let poi = focusPoi else { return }
            
            // 1. Update State
            mapState = .selectingDestinationLocation
            
            // 2. Clear old destination markers before adding a new one
            clearSelectedMarkers(withTitle: "Destination")
            
            // 3. Create the Marker
            let coord = CLLocationCoordinate2D(latitude: poi.lat, longitude: poi.lng)
            let marker = createMarker(at: coord, floorId: poi.floorId, title: "Destination")
            
            // 4. Update the endpoint for routing
            endPoint = markerToWaypoint(marker: marker)
            endMarker = marker
            
            // 5. Move the map to the POI
            if let mapxusMap = self.mapxusMap {
                mapxusMap.selectFloor(byId: poi.floorId, zoomMode: .animated, edgePadding: .zero)
                mapView?.setTargetCoordinate(coord, animated: true, completionHandler: nil)
            }
            
            print("📍 Focused on POI: id: \(poi.id) at Building Id:  Floor: \(poi.floorName) with floor id: \(poi.floorId), lat: \(poi.lat), lon: \(poi.lng)")
        }
    }

    func map(_ map: MapxusMap, didChangeSelectedFloor floor: MXMFloorProtocol?, inSelectedBuildingId buildingId: String?, atSelectedVenueId venueId: String?) {
        // 1. Ensure we have a valid floor object
        guard let newFloor = floor else {
            print("📍 Map selection cleared (No floor selected)")
            lastFloorId = nil
            return
        }
        
//        newFloor.ordinal?.accessibilityRespondsToUserInteraction = true

        // 2. Only trigger if the floor ID is actually different (ignores repeated calls)
        guard newFloor.floorId != lastFloorId else { return }
        
        // 3. Update the tracking ID
        lastFloorId = newFloor.floorId

        // 4. Perform actions for the new floor
        handleFloorChange(mapxusMap: map, floor: newFloor, venueId: venueId ?? "")
    }
    
    func map(_ map: MapxusMap, didChangeSelectedFloorVisualizationStatus isVisible: Bool, withSelectedFloor floor: MXMFloorProtocol?, selectedBuildingId buildingId: String?, selectedVenueId venueId: String?) {
        
        // 1. Only proceed if the floor is visible and data exists
        guard isVisible,
              let newfloor = floor,
              let venue = venueId,
              let ordinal = floor?.ordinal else {
            print("⚠️ Floor is not visible or data is missing")
            return
        }
        
        let userGPS = map.userLocationFloor?.floorId ?? ""
        
        /// 2. Update the route painter
        routePainter?.change(onVenue: venue, ordinal: ordinal)
        
        print("🏢 Floor changed to: floor name: \(floor?.name ?? "unknown") in venue: \(venue), at: \(ordinal.level as Int)")
        print("User location floor id: \(userGPS)")
    }
    
    private func handleFloorChange(mapxusMap: MapxusMap, floor: MXMFloorProtocol, venueId: String) {
        print("🔔 Processing floor change to: \(floor.name)")

        // 1. ALWAYS update the route painter so the UI stays correct
        if let ordinal = floor.ordinal {
            routePainter?.change(onVenue: venueId, ordinal: ordinal)
        }

        // 2. Check for user location floor
        guard let userFloor = mapxusMap.userLocationFloor else {
            print("⚠️ User positioning not active yet (userLocationFloor is nil)")
            return
        }
        
        print("📍 User detected on floor: \(userFloor.name) (ID: \(userFloor.floorId))")

        // 3. Sync map to user floor if they are different
        if floor.floorId != userFloor.floorId {
            print("🔄 Syncing map to user's actual floor: \(userFloor.name)")
            mapxusMap.selectFloor(byId: userFloor.floorId)
        }
        
        print("✅ Floor sync complete.")
    }
    
    /// Handling tap on blank space
    func map(_ map: MapxusMap, didSingleTapOnBlank coordinate: CLLocationCoordinate2D, at site: MXMSite?) {
        
        // 1. Check if the SDK actually sees a building at this tap coordinate
        guard let tappedBuildingId = site?.building?.identifier else {
            print("📍 Tapped on blank space - No building detected.")
            return
        }

        // 2. Route logic based on mapState
        switch mapState {
        case .welcoming:
            handleBuildingSelection(tappedBuildingId: tappedBuildingId)
            print("📍 Tapped on blank space - No building detected. Welcoming")
        case .initial:
//            handleMarkerCreationOnBlankTapOnMap(coordinate: coordinate, floorId: site?.floor.floorId, tappedBuildingId: tappedBuildingId)
            print("📍 Tapped on blank space - No building detected. Initial")
            break
        default:
            print("⛔ New Tap ignored 1: mapState is \(mapState)")
        }
    }
    
    /// Handling tap not on blank space
    func map(_ map: MapxusMap, didSingleTapOn poi: MXMGeoPOI, at coordinate: CLLocationCoordinate2D, at site: MXMSite?) {
        
        // 1. Identification Check
        guard let tappedBuildingId = site?.building?.identifier else {
            print("📍 Tapped POI - No building detected.")
            return
        }
        
        guard let tappedBuildingNumber = site?.building?.identifier else {
            print("📍 Tapped POI - No building detected.")
            return
        }
        
        let floorId = site?.floor.floorId

        // 2. Logic Branching
        switch mapState {
        case .welcoming:
            handleBuildingSelection(tappedBuildingId: tappedBuildingId)
            
        case .initial, .selectingDestinationLocation:
            handleDestinationPOI(poi: poi, coordinate: coordinate, floorId: floorId, buildingId: tappedBuildingId)
            
        default:
            print("new tap detected: ignored!.")
            break
        }
    }
    
    // MARK: - Private Helpers
    private func handleBuildingSelection(tappedBuildingId: String) {
        if let index = buildingLists.firstIndex(where: { $0.id == tappedBuildingId }) {
            let tappedTheBuilding = buildingLists[index]
            
            DispatchQueue.main.async {
                withAnimation(.smooth()) {
                    self.currentBuildingIndex = index
                    
                    /// Trigger the selection logic
//                    self.selectBuilding(id: tappedTheBuilding.id)
//                    self.isGettingBuildingNumber = tappedTheBuilding.buildingNumber
                }
                print("✅ New Welcoming: Selected Building \(tappedTheBuilding.buildingName)")
            }
        }
    }

    private func handleMarkerCreationOnBlankTapOnMap(coordinate: CLLocationCoordinate2D, floorId: String?, tappedBuildingId: String) {
        // Only proceed if the building is in our recognized list
        if let index = buildingLists.firstIndex(where: { $0.id == tappedBuildingId }) {
            let tappedTheBuilding = buildingLists[index]
            
            // Find the facility (matching the building ID)
            if let facility = buildingFacilities.first(where: { $0.id == tappedTheBuilding.id }) {
                clearSelectedMarkers(withTitle: "Destination")
                
                let marker = createMarker(at: coordinate, floorId: floorId, title: "Destination")
                endPoint = markerToWaypoint(marker: marker)
                endMarker = marker
                
                // Transition State
                mapState = .selectingDestinationLocation
                isGettingDestinationName = facility.facilityName
                isGettingDestinationFloorName = facility.floorName
                isGettingBuildingNumber = tappedTheBuilding.buildingNumber
                
                navigationDestinationPath.append("ConfirmingDestinationView")
                print("✅ New Initial: Created Marker at \(tappedTheBuilding.buildingName)")
            }
        }
    }

    // MARK: - Private Helpers
    private func handleDestinationPOI(poi: MXMGeoPOI, coordinate: CLLocationCoordinate2D, floorId: String?, buildingId: String) {
        if let index = buildingLists.firstIndex(where: { list in
            let allowMarkeringOutsideTheBuilding = list.id == buildingId
            let disableMarkeringOutsideTheBuilding = allowMarkeringOutsideTheBuilding && list.id == isGettingBuildingId
            
            return allowMarkeringOutsideTheBuilding
        }) {
            let tappedTheBuilding = buildingLists[index]
            withAnimation(.spring(), {
                clearSelectedMarkers(withTitle: "Destination")
            })
            let marker = createMarker(at: coordinate, floorId: floorId, title: "Destination")
            
            // Sync focused POI data
            self.focusPoi = MapPoi(
                id: poi.identifier,
                lat: poi.coordinate.latitude,
                lng: poi.coordinate.longitude,
                facilityName: self.getFacilityNameInMultipleLanguagesOnMapTap(from: poi.nameMap, for: self.selectedLanguage),
                floorId: poi.floor?.floorId ?? "",
                floorName: poi.floor?.name ?? ""
            )
            
            self.endPoint = markerToWaypoint(marker: marker)
            self.endMarker = marker
            
            self.currentBuildingIndex = index
            self.isGettingBuildingId = (poi.buildingId ?? "") as String
            self.isGettingBuildingNumber = tappedTheBuilding.buildingNumber
            self.isGettingDestinationName = self.getFacilityNameInMultipleLanguagesOnMapTap(from: poi.nameMap, for: self.selectedLanguage)
            self.isGettingDestinationFloorName = poi.floor?.name ?? ""
            let firstCategory = self.getNormalizedCategory(from: poi.category.first ?? "").first
//            self.isGettingBuildingFacilityCategory = firstCategory ?? ""
            
            /// Occupancy
            self.foundDeviceIds = userClass.getDeviceIds(
                buildingId: poi.buildingId ?? "",
                floorId: poi.floor?.floorId ?? "",
                mapxusId: poi.identifier
            )
            
            if isGettingBuildingIdOnMapView != poi.buildingId {
                isFetchingWashroomOccupancy = false
            }
            
            // Check if the categories contain both required strings
            if poi.category.contains("facility.restroom.female") ||
                poi.category.contains("facility.restroom.disable") ||
                poi.category.contains("facility.restroom.male") {
                
                Task { @MainActor in
                    if !isFetchingWashroomOccupancy {
                        print("🚀 Washroom run for building: \(poi.buildingId ?? "")")
                        isFetchingWashroomOccupancy = true
                        
                        // 1. Await the refresh so the data is actually there before proceeding
                        // Note: Ensure this function in your class is 'async'
                        await washroomOccupancyClass?.getToiletStatusWithoutAutomaticRefresh(buildingId: poi.buildingId)
                        
                        var attempts = 0
                        while washroomOccupancyClass?.isLoading == true && attempts < 50 {
                            try? await Task.sleep(nanoseconds: 100_000_000) // Sleep 0.1s
                            attempts += 1
                        }
                    }
                    
                    let washroomStatus = washroomOccupancyClass?.getRestroomOccupancyStatusAll(poiId: poi.identifier, statuses: vacantToiletStatuses(languageCode: selectedLanguage), languageCode: selectedLanguage, allBuildings: .constant(false))
                    
                    self.isGettingWashroomVacantStatusMessage = washroomStatus?.message ?? "Unavailable"
                    self.isGettingWashroomVacantStatusColor = vacantToiletStatusColor(
                        status: self.isGettingWashroomVacantStatusMessage,
                        languageCode: selectedLanguage
                    )
                }
            } else {
                isGettingWashroomVacantStatusMessage = ""
                isGettingWashroomVacantStatusColor = .clear
                isDisablingGoingToSelectingCurrentLocation = false
            }
            
            print("is disabling going to current location: \(isDisablingGoingToSelectingCurrentLocation)")
            
            // 💡 THE FIX: Only append if it's not already the current view
            if !isDisablingCreatingDestinationMarker {
                
                isDisablingCreatingDestinationMarker = true
                isNavigatingToConfirmingDestinationView = true
                isShowingTheSettingsViewButton = false
                withAnimation(.smooth(), {
                    sheetHeight = 240
                    presentationActiveDetent = .height(240)
                })
                self.navigationDestinationPath.append("ConfirmingDestinationView")
            }
            
            print("🎯 New Destination POI set: \(poi.nameMap.en ?? "")")
            print("building number updated poi id: \(poi.buildingId ?? "" as String)")
            print("building number updated 2: \(isGettingBuildingNumber)")
        }
        
    }

    private func handleStartPOI(coordinate: CLLocationCoordinate2D, floorId: String?) {
        clearSelectedMarkers(withTitle: "Start")
        let marker = createMarker(at: coordinate, floorId: floorId, title: "Start")
        self.startPoint = markerToWaypoint(marker: marker)
        self.startMarker = marker
        print("📍 New Start location set manually.")
    }
    
    func vacantToiletStatuses(languageCode: String) -> [String] {
        switch languageCode {
        case "zh-Hant":
            return ["滿的", "幾乎滿", "可用的"]
        case "zh-Hans":
            return ["满的", "几乎满", "可用的"]
        default:
            return ["Full", "Available", "Almost Full"]
        }
    }
    
    func vacantToiletStatusColor(status: String, languageCode: String) -> Color {
        switch languageCode {
        case "zh-Hant":
            switch status {
            case "滿的":
                return Color.red
            case "幾乎滿":
                return Color.mainColor
            case "可用的":
                return Color.yellow
            default:
                return Color.gray
            }
        case "zh-Hans":
            switch status {
            case "满的":
                return Color.red
            case "几乎满":
                return Color.mainColor
            case "可用的":
                return Color.yellow
            default:
                return Color.gray
            }
        default:
            switch status {
            case "Full":
                return Color.red
            case "Available":
                return Color.mainColor
            case "Almost Full":
                return Color.yellow
            default:
                return Color.gray
            }
        }
    }
    /// End of the new one
    
    // Inside MapxusController
    func mapView(_ mapView: MGLMapView, regionDidChangeAnimated animated: Bool) {
        // 1. Get the center point of the map view's frame
        let centerScreenPoint = CGPoint(x: mapView.bounds.midX, y: mapView.bounds.midY)
        
        // 2. Convert that screen point to a geographical coordinate
        let centerCoord = mapView.convert(centerScreenPoint, toCoordinateFrom: mapView)
        
        // 3. Update your published property
        DispatchQueue.main.async {
            self.centerPinCoordinate = centerCoord
        }
        
        print("📍 Map dragged to: \(centerCoord.latitude), \(centerCoord.longitude)")
        
        // Optional: If you want to automatically update the 'Start' or 'End' waypoint
        // based on the center pin while in a specific state:
        if mapState == .selectingCurrentLocation && isSelectingLocationByPIN {
            let floorId = mapxusMap?.selectedFloor?.floorId
            self.startPoint = MXMWaypoint.createWaypoint(withLatitude: centerCoord.latitude, longitude: centerCoord.longitude, floorId: floorId)
        }
        
    }
    
    // This function runs automatically every time the GPS updates
    func mapView(_ mapView: MGLMapView, didUpdate userLocation: MGLUserLocation?) {
        // 1. ONLY check for valid GPS data.
        // DO NOT check for 'startPoint == nil' here anymore!
        guard let location = userLocation?.location,
              CLLocationCoordinate2DIsValid(location.coordinate) else {
            return
        }

        let lat = location.coordinate.latitude
        let lon = location.coordinate.longitude
        let userGPS = CLLocation(latitude: lat, longitude: lon)
        
        let gpsTrueHeading = userLocation?.heading?.trueHeading ?? 0 as Double
        let gpsMagneticHeading = userLocation?.heading?.magneticHeading ?? 0 as Double
        let gpsAccuracy = userLocation?.heading?.headingAccuracy ?? -1 as Double
        
        let userFloorLevel = userLocation?.location?.floor?.level ?? 0 as Int
        let userFloorLevel2 = userLocation?.location?.myFloor?.level ?? 0 as Int
        
        // 2. Initial Setup Logic (Runs only once when startPoint is first found)
        if startPoint == nil {
            print("📍 GPS Initialized - Lat: \(lat), Lon: \(lon)")
        }

        // 3. Navigation Proximity Logic (Now this can run even if startPoint exists!)
        if mapState == .navigating {
            print("compass user true heading Map: \(gpsTrueHeading)")
            print("compass user magnetic heading Map: \(gpsMagneticHeading)")
            print("compass user heading accuracy: \(gpsAccuracy)")
            
            if gpsAccuracy > 0 && gpsAccuracy < 15 {
                // 🏆 Trust this data for AR
                self.compassTruHeadingWarning = ""
                self.compassTrueHeading = gpsTrueHeading
                print("🏆 Excellent: Compass accuracy is exellent.")
            } else if gpsAccuracy > 15 {
                // ⚠️ Still use it, but maybe add a "jitter" filter
                self.compassTruHeadingWarning = ""
                self.compassTrueHeading = gpsTrueHeading
                print("⚠️ Warning: Compass accuracy is dipping.")
            } else {
                self.compassTruHeadingWarning = "The Calibration of the Compass is broken. Please try again!."
                self.compassTrueHeading = 0
                // ❌ Negative value: The compass is calibrating or broken
                print("❌ Invalid compass heading data.")
            }
            
            isRotatingTheMapOnGPSButtonClicked = false
            checkUserGPSProximityToNextStep(currentLocation: userGPS)
            rotatingMapxusMapOnNavigating(map: mapView, userLocation: userLocation)
//            disablingTrackingUserGpsOnNavigation(map: mapView, userLocation: userLocation)
        } else {
//            followModeOnlyOnNavigating(map: mapView, userLocation: userLocation)
            if isRotatingTheMapOnGPSButtonClicked {
                rotatingMapxusMapOnNavigating(map: mapView, userLocation: userLocation)
            }
            // This print will now keep appearing even after you set a marker!
            print("DEBUG: User's GPS status: \(lat), \(lon), distance from the target direction: \(location.distance(from: userGPS)), floor level: \(location.floor?.level ?? 0), \(location.myFloor?.level ?? 0). mapState is \(mapState)")
        }
        
        print("user floor level changes: \(userFloorLevel)")
    }
    
    /// For User Tracking Mode
    func mapView(_ mapView: MGLMapView, didChange mode: MGLUserTrackingMode, animated: Bool) {
        // If the user manually drags the map, the system changes mode to .none
        if mode == .none && mapState == .navigating {
            print("📍 User manually moved the map. Auto-follow disabled.")
            // This is a good time to show a 'Recenter' button
        } else if mode == .followWithHeading {
            print("🧭 Map is now auto-rotating with user heading. Auto-follow enabled")
        }
    }
    
    func mapViewDidFinishLoadingMap(_ mapView: MGLMapView) {
        /// Set initial center coordinate so the button works immediately
        self.centerPinCoordinate = mapView.centerCoordinate
    }
    
    /// For customizing start marker
    func mapView(_ mapView: MGLMapView, imageFor annotation: MGLAnnotation) -> MGLAnnotationImage? {
        
        if annotation.title == "Start" {
            var annotationImage = mapView.dequeueReusableAnnotationImage(withIdentifier: "Start")
            
            if annotationImage == nil {
                let originalImage = UIImage(named: "customStartMarkerPin1", in: .main, with: nil)
                
                // 1. Just define the icon size
                let iconSize = CGSize(width: 28, height: 28)
                
                // 2. Simple resize without extra padding
                let renderer = UIGraphicsImageRenderer(size: iconSize)
                let resizedImage = renderer.image { _ in
                  originalImage?.draw(in: CGRect(origin: .zero, size: iconSize))
                }
                
                annotationImage = MGLAnnotationImage(image: resizedImage, reuseIdentifier: "Start")
            }
            
            return annotationImage
        }
        
        return nil
    }
    
    func rotatingMapxusMapOnNavigating(map: MGLMapView, userLocation: MGLUserLocation?) {
        map.allowsRotating = true
        map.userTrackingMode = .followWithHeading
    }
    
    func followModeOnlyOnNavigating(map: MGLMapView, userLocation: MGLUserLocation?) {
        map.userTrackingMode = .follow
    }
    
    func disablingTrackingUserGpsOnNavigation(map: MGLMapView, userLocation: MGLUserLocation?) {
        map.allowsRotating = true
        map.userTrackingMode = .none
    }
    
    /// Old code - For backup from me
//    func checkUserGPSProximityToNextStep(currentLocation: CLLocation) {
//        print("DEBUG: User's GPS checkProximityToNextStep called. Index: \(instructionIndex), Count: \(instructionPointList.count)")
//            
//        guard instructionIndex < instructionPointList.count else {
//            print("DEBUG: User's GPS Exiting because index is out of bounds.")
//            return
//        }
//        
//        let currentStepCoord = instructionPointList[instructionIndex]
//        let stepLocation = CLLocation(latitude: currentStepCoord.latitude,
//                                      longitude: currentStepCoord.longitude)
//        
//        let distance = currentLocation.distance(from: stepLocation)
//        
//        // Check floor ID to prevent triggering on different levels
//        let userFloorId = self.mapxusMap?.userLocationFloor?.floorId
//        let targetFloorId = instructionList[instructionIndex].floorId
//        let isSameFloor = userFloorId == targetFloorId
//
//        // 0.5 meters is roughly 1.6 feet (arm's length)
//        if distance <= 1.0 && isSameFloor {
//            print("🎯 User's GPS Precision Proximity reached (50cm)! Moving to next instruction.")
//            
//            // Haptic feedback is helpful when precision is this high
//            // so the user knows they "hit" the target.
//            let generator = UIImpactFeedbackGenerator(style: .medium)
//            generator.impactOccurred()
//            
//            nextStep()
//        } else {
//            /// Debug print to see how close the user is getting in real-time
//            print("📏 User's GPS Distance: \(String(format: "%.2f", distance))m. Target: 1.0m")
//        }
//    }
    
    func checkUserGPSProximityToNextStep(currentLocation: CLLocation) {
        print("⚠️ OFF-ROUTE: 📍 Proximity Check Called. Index: \(instructionIndex)/\(instructionPointList.count)")

        guard instructionIndex < instructionPointList.count else {
            print("⚠️ OFF-ROUTE: 🛑 Guard failed: Index out of bounds.")
            return
        }
            
        // 1. Current Target Point
        let currentStepCoord = instructionPointList[instructionIndex]
        let stepLocation = CLLocation(latitude: currentStepCoord.latitude,
                                      longitude: currentStepCoord.longitude)
        let distanceToNext = currentLocation.distance(from: stepLocation)

        // 2. Previous Point (If index > 0)
        var distanceToPrevious: CLLocationDistance = .greatestFiniteMagnitude
        if instructionIndex > 0 {
            let prevCoord = instructionPointList[instructionIndex - 1]
            let prevLocation = CLLocation(latitude: prevCoord.latitude, longitude: prevCoord.longitude)
            distanceToPrevious = currentLocation.distance(from: prevLocation)
        }

        let userFloorId = self.mapxusMap?.userLocationFloor?.floorId
        let targetFloorId = instructionList[instructionIndex].floorId
        let isSameFloor = (userFloorId == nil || targetFloorId == nil) ? true : (userFloorId == targetFloorId)

        // ✅ CASE 1: Reached Point
        if distanceToNext <= 1.2 && isSameFloor {
            print("⚠️ OFF-ROUTE: 🎯 Reached Point! Distance: \(distanceToNext)m")
            self.isOffRoute = false
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.impactOccurred()
            nextStep()
        }
        // ✅ CASE 2: Off-Route
        else if (distanceToNext > offRouteThreshold && distanceToPrevious > offRouteThreshold) || (!isSameFloor && distanceToNext > 5.0) {
            // Logic: Trigger ONLY if user is far from the next point AND far from the previous point
            if !isOffRoute {
                print("⚠️ OFF-ROUTE: Distance to Next: \(distanceToNext)m, Prev: \(distanceToPrevious)m, SameFloor: \(isSameFloor)")
                self.isOffRoute = true
                self.showOffRouteAlert()
            }
        }
        // ✅ CASE 3: On-Path (User is near either the next point or the previous point)
        else {
            if isSameFloor && (distanceToNext < offRouteThreshold || distanceToPrevious < offRouteThreshold) {
                if isOffRoute {
                    print("⚠️ OFF-ROUTE: ✅ Back on track!")
                    self.isOffRoute = false
                } else {
                    print("⚠️ OFF-ROUTE: ✅ Back on track!")
                    self.isOffRoute = false
                }
            }
            print("⚠️ OFF-ROUTE: 📏 Path Check: Next: \(String(format: "%.2f", distanceToNext))m, Prev: \(String(format: "%.2f", distanceToPrevious))m, SameFloor: \(isSameFloor)")
        }
    }
    
    func recalculateRouteFromCurrentLocation() {
        // 1. Reset state
        self.isOffRoute = false
        
        withAnimation(.smooth(), {
            self.isShowingAndClosingTheARNavigation = false
            self.isShowingARNavigation = false
        })
        
        self.instructionIndex = 0
        
        // 2. Surgical Cleanup:
        // Remove only the blue line and the current "Start" marker.
        // We do NOT call clearAllMarkers because we want to keep the "Destination".
        removeBlueLineRoute()
        clearSelectedMarkers(withTitle: "Start")
        
        // Clear the old instructions to prevent the UI from showing stale data
        instructionList.removeAll()
        instructionPointList.removeAll()
        
        // 3. Update 'startPoint' using GPS
        // This creates the new "Start" marker and sets the startPoint property.
        createMarkerBasedOnUserGPS()
        
        // 4. Validate and Restart
        guard let ep = endPoint, let sp = startPoint else {
            print("❌ Recalculation failed: Missing start or end point.")
            return
        }
        
        startNavigation()
        print("🔄 Recalculation complete. Path updated from GPS to existing destination.")

//        mapState = .showingRoute
//        let routeType = selectedRouteType(type: activeRouteType, languageCode: selectedLanguage)
//        showRoute(routeOption: routeType)
    }
    
    func showOffRouteAlert() {
        // 1. Ensure we don't present multiple alerts
        guard isOffRoute else { return }
        
        let alert = UIAlertController(
            title: "Off Route",
            message: "You seem to have moved away from the navigation path. Would you like to recalculate?",
            preferredStyle: .alert
        )
        
        // 2. Recalculate Action
        let recalculateAction = UIAlertAction(title: "Recalculate", style: .default) { [weak self] _ in
            self?.recalculateRouteFromCurrentLocation()
        }
        
        // 3. Cancel Action
        let cancelAction = UIAlertAction(title: "Dismiss", style: .cancel) { [weak self] _ in
            // Keep isOffRoute true or handle silence logic here
            
        }
        
        alert.addAction(recalculateAction)
        alert.addAction(cancelAction)
        
        // 4. Present on Main Thread
        // 4. Present on Main Thread using WindowScene
        DispatchQueue.main.async {
            let keyWindow = UIApplication.shared.connectedScenes
                .filter { $0.activationState == .foregroundActive }
                .compactMap { $0 as? UIWindowScene }
                .first?.windows
                .filter { $0.isKeyWindow }.first

            if let topVC = keyWindow?.rootViewController {
                // If the root is a navigation or tab controller, you might need the visible one
                let presenter = self.getVisibleViewController(topVC)
                presenter.present(alert, animated: true)
            }
        }
    }
    
    /// New by me
    func createMarkerBasedOnUserGPS() {
        // 1. Access the location directly from the MapView's userLocation property
        guard let mapxusMap = self.mapxusMap,
              let userLocation = mapView?.userLocation,
              let coordinate = userLocation.location?.coordinate else {
            print("❌ MapView has not found a GPS signal yet")
            return
        }

        // 2. Validate that the coordinate is not 0,0 (common Mapbox/MGL issue before fix)
        guard CLLocationCoordinate2DIsValid(coordinate) && coordinate.latitude != 0 else {
            print("❌ GPS Coordinate is invalid or 0.0")
            return
        }

        // 3. Handle Floor ID Logic
        // In Mapxus, it is best to check userLocationFloor first, then fallback to the map's selected floor
        let selectedFloorId = mapxusMap.userLocationFloor?.floorId ?? mapxusMap.selectedFloor?.floorId ?? ""
        let selectedFloorName = mapxusMap.userLocationFloor?.name ?? ""
        let selectedFloorOrdinal = mapxusMap.userLocationFloor?.ordinal?.level
        let userFloorLevel = userLocation.location?.floor?.level ?? 0 as Int
        
        print("user floor level: \(userFloorLevel), name: \(selectedFloorName), id: \(selectedFloorId)")
        print("📍Using User's GPS: Using mapView.userLocation at \(coordinate.latitude), \(coordinate.longitude) on floorId: \(selectedFloorId), floor name: \(selectedFloorName), ordinal: \(selectedFloorOrdinal ?? 0)")
        
        // 4. Remove previous Start markers using your helper logic
        let startMarkers = markerPoints.filter { $0.title == "Start" }
        mapxusMap.removeMXMPointAnnotaions(startMarkers)
        markerPoints.removeAll { $0.title == "Start" }

        // 5. Create and show new marker
        let marker = createMarker(at: coordinate, floorId: selectedFloorId, title: "Start")
        if let marker = marker {
            startMarker = marker

            // 6. Create Waypoint
            let waypoint = MXMWaypoint()
            waypoint.latitude = coordinate.latitude
            waypoint.longitude = coordinate.longitude
            waypoint.floorId = selectedFloorId

            startPoint = waypoint
            print("✅ Created GPS Start Marker and Waypoint: \(String(describing: startPoint))")

            // 7. Sync the floor view
            if !selectedFloorId.isEmpty {
                mapxusMap.selectFloor(byId: selectedFloorId, zoomMode: .animated, edgePadding: .zero)
            }
        } else {
            print("❌ Failed to create marker at GPS location")
        }
    }
    
    func setStartLocationFromCenterPin() {
        // 1. Ensure we have a coordinate from the center pin
        guard let coord = centerPinCoordinate else {
            print("❌ No center coordinate found")
            return
        }
        
        // 2. Get the current floor ID from the map
        // It's important to use the floor the user is currently looking at
        let floorId = mapxusMap?.selectedFloor?.floorId
        
        // 3. Clear any existing Start marker first
        clearSelectedMarkers(withTitle: "Start")
        
        // 4. Create the visual marker
        let marker = createMarker(at: coord, floorId: floorId, title: "Start")
        
        // 5. Update the routing properties
        self.startPoint = markerToWaypoint(marker: marker)
        self.startMarker = marker
        
//        self.startLocationCoord = "\(translationClass.latitude(code: selectedLanguage)): \(coord.latitude), \(translationClass.latitude(code: selectedLanguage)): \(coord.longitude)"
        
        print("✅ Start Location Set at: \(coord.latitude), \(coord.longitude) on floor: \(floorId ?? "Unknown")")
    }
    
    func createMarker(at coordinate: CLLocationCoordinate2D, floorId: String? = "", title: String = "") -> MXMPointAnnotation? {
        guard let mapxusMap = mapxusMap else { return nil }
        
        let annotation = MXMPointAnnotation()
        annotation.coordinate = coordinate
        annotation.title = title
        annotation.floorId = floorId
        markerPoints.append(annotation)
        mapxusMap.add([annotation])
        return annotation
    }

    func markerToWaypoint(marker : MXMPointAnnotation?) -> MXMWaypoint {
        return MXMWaypoint.createWaypoint(withLatitude: marker?.coordinate.latitude ?? 0, longitude: marker?.coordinate.longitude ?? 0, floorId: marker?.floorId)
    }
    
    func showMyLocation() {
        guard let mapView = mapView else { return }
        
        let userLocationFloor = mapxusMap?.userLocationFloor?.floorId
        let userLocationBuilding = mapxusMap?.userLocationBuilding?.floors
        let userLocationVenue = mapxusMap?.userLocationVenue?.buildingIds
        
        if mapState != .navigating {
            isRotatingTheMapOnGPSButtonClicked.toggle()
        }
        
        // Check if we already have a location (from a previous update)
        if let coord = mapView.userLocation?.coordinate {
            print("📍 Immediate location found: \(coord.latitude), \(coord.longitude)")
            mapView.setCenter(coord, animated: true)
        } else {
            print("🔍 Searching for GPS signal... coordinates will print in delegate.")
        }
        
        print("user location floor: \(String(describing: userLocationFloor)), building: \(String(describing: userLocationBuilding)), venue: \(String(describing: userLocationVenue))")
    }
    
    func navigateBasedOnRouteSearchOptions(to selectedType: String) {
        // 1. Initialize the option object
        let option = MXMRouteSearchOption()
        
        // 2. Map your UI strings to the SDK Pedometer Types
        // This controls whether the route uses Lifts, Escalators, or Stairs.
        switch selectedType {
        case "Walk Only": option.vehicle = .escalator
        case "Stairs Only": option.vehicle = .emergency
        case "Elevator Only": option.vehicle = .wheelchair
        case "Escalator Only": option.vehicle = .escalator
        case "Wheelchair Only": option.vehicle = .wheelchair
        case "Emergency": option.vehicle = .emergency
        default: option.vehicle = .foot
        }

        // 3. Ensure we have valid points before searching
        guard let start = startPoint, let end = endPoint else {
            print("⚠️ Cannot search: Start or End point is missing.")
            return
        }
        
        hideAllMarkerIcons()
        
        // 4. Set the coordinates
        option.points = [start, end]
        
        // 5. Use the existing searcher to find the route
        print("Searching for \(selectedType) route...")
        routeSearcher?.findRoute(with: option)
    }
    
    func getRouteType(type: String, languageCode: String, route: MXMRouteSearchOption) {
        switch languageCode {
        case "zh-Hant":
            switch type {
            case "最短步行": route.vehicle = .foot
            case "僅限電梯": route.vehicle = .wheelchair
            case "限手扶梯": route.vehicle = .escalator
            default:        route.vehicle = .foot
            }
        case "zh-Hans":
            switch type {
            case "最短步行": route.vehicle = .foot
            case "仅限电梯": route.vehicle = .wheelchair
            case "仅限自动扶梯": route.vehicle = .escalator
            default:        route.vehicle = .foot
            }
        default:
            switch type {
            case "Shortest Walk": route.vehicle = .foot
            case "Lift Only":      route.vehicle = .wheelchair
            case "Escalator Only": route.vehicle = .escalator
            default:               route.vehicle = .foot
            }
        }
    }
    
    func showRoute(routeOption type: String) {
        // 1. Check if we actually have the points
        guard let start = startPoint, let end = endPoint else {
            print("❌ Cannot show route: startPoint or endPoint is nil")
            return
        }

        // 2. Prepare the map state
        if mapState != .navigating {
//            mapState = .showingRoute
        }
        
        hideAllMarkerIcons()

        // 3. Configure the search
        let option = MXMRouteSearchOption()
        
        switch type {
        default:
            getRouteType(type: type, languageCode: selectedLanguage, route: option)
        }
        
        option.points = [start, end]

        // 4. Trigger search
        print("🛰️ Requesting route: \(type)")
        routeSearcher?.findRoute(with: option)
    }

    func hideRoute() {
        // 1. Remove the blue route line from the map
        removeBlueLineRoute()
        
        // 2. Restore the markers to the map view
        showAllMarkerIcons()
        
        // 3. Reset UI states
        instructionList.removeAll()
        
        // 🛑 REMOVED: startPoint = nil and endPoint = nil
        // We keep these so the markers still "exist" in data even if hidden
        
        print("🧹 Route hidden. Markers restored: \(markerPoints.count)")
    }
    
    func removeBlueLineRoute() {
        // 1. Remove the blue route line from the map
        routePainter?.cleanRoute()
    }
    
    func startNavigation() {
        guard let sp = startPoint, let ep = endPoint else {
            print("❌ Navigation can't be started: startPoint or endPoint is nil.")
            
            if startPoint == nil && endPoint == nil {
                print("Navigation start point and end point are nill.")
            } else if startPoint == nil {
                print("Navigation start point is nill.")
            } else if endPoint == nil {
                print("Navigation end point is nill.")
            }
            
            return
        }
        
        isFoldingFloorBarSection(fold: true)
        clearAllMarkerIcons()

        mapState = .navigating
        
        if !instructionList.isEmpty {
            instructionIndex = 0
            updateRoute(routeOption: selectedRouteType(type: activeRouteType, languageCode: selectedLanguage))
        } else {
            showRoute(routeOption: selectedRouteType(type: activeRouteType, languageCode: selectedLanguage))
        }
        
        print("🚀 Navigation successfully started. State: \(mapState)")
    }
    
    func endNavigation() {
        print("🛑 Ending navigation and cleaning up...")
        mapView?.setUserTrackingMode(.none, animated: true, completionHandler: {
            self.mapView?.setZoomLevel(18.0, animated: true)
            self.resetNorth()
        })
        
        // Perform all UI and State updates on the Main Thread
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            // 1. Reset Map State first to update UI visibility immediately
            self.mapState = .welcoming
            self.isFoldingFloorBarSection(fold: false)
            self.isSelectingLocationByPIN = false
            self.isOffRoute = false
            
            // 2. Clear visual map elements
            
            // Use your existing helper to ensure both the array and the map view are cleared
            self.clearAllMarkersAlongWithTheInstructionLists()
            
            self.activeRouteType = ""
            
            // 5. Reset Camera (Optional but recommended)
            // If you want the map to zoom out or stop following the user
            // self.mapxusMap?.mapView.setZoomLevel(17, animated: true)

            print("✅ Cleanup complete.")
        }
    }
    
    func updateRoute(routeOption type: String) {
        guard instructionIndex < instructionPointList.count,
              instructionIndex < instructionList.count,
              let end = endPoint else {
            print("❌ updateRoute failed: Missing endPoint or instructionIndex out of bounds.")
            return
        }

        let option = MXMRouteSearchOption()
        
        switch type {
        default:
            getRouteType(type: type, languageCode: selectedLanguage, route: option)
        }
        
        let point = instructionPointList[instructionIndex]
        let instruction = instructionList[instructionIndex]
        let currentStart = MXMWaypoint.createWaypoint(
            withLatitude: point.latitude,
            longitude: point.longitude,
            floorId: instruction.floorId
        )

        option.points = [
            currentStart,
            end
        ]

        currentInstructionTitle = instruction.text ?? ""

        let distance = instruction.distance
        let roundedDistance = ceil(distance * 2) / 2
        currentInstructionDistance = "\(roundedDistance)\(translationClass.meter(plural: false, code: selectedLanguage))"
        
        if isEnablingTTS {
            let rawText = currentInstructionTitle.isEmpty ? (instruction.text ?? "") : currentInstructionTitle
            guard !rawText.isEmpty else { return }

            // Generate the natural sentence
            let textToSpeak = getSpeechText(
                rawInstruction: rawText,
                distance: roundedDistance,
                code: selectedLanguage
            )
            
            let utterance = AVSpeechUtterance(string: textToSpeak)
            utterance.voice = AVSpeechSynthesisVoice(language: selectedLanguage)
            
            // Set appropriate speed for navigation
            utterance.rate = 0.5
            
            if speechSynthesizer.isSpeaking {
                speechSynthesizer.stopSpeaking(at: .immediate)
            }
            
            speechSynthesizer.speak(utterance)
        }

        routeSearcher?.findRoute(with: option)
    }
    
    func getSpeechText(rawInstruction: String, distance: Double, code: String) -> String {
        let roundedDist = Int(distance)
        
        // 1. Get the base translation using your existing function
        let baseAction = getTranslationInstructionListInMultipleLanguages(
            instruction: rawInstruction,
            languageCode: code
        )
        
        // 2. Handle Arrival (No distance needed)
        if rawInstruction.lowercased().contains("arrive") {
            return code.contains("zh") ? "太棒了！\(baseAction)。" : "Kudos! You have arrived at the destination!"
        }

        // 3. Build the distance string based on language
        if code.contains("zh") {
            // Natural Chinese: "右轉，請繼續走 10 公尺。"
            return "\(baseAction)，請繼續走 \(roundedDist) 公尺。"
        } else {
            // Natural English: "Turn right, and follow the steps for 10 meters."
            let meterLabel = roundedDist == 1 ? "meter" : "meters"
            return "\(baseAction), and follow the steps for \(roundedDist) \(meterLabel)."
        }
    }
    
    func getTranslationInstructionListInMultipleLanguages(instruction: String, languageCode: String) -> String {
        let lowerInstruction = instruction.lowercased()
        
        // 1. Handle Simple Directions First
        let directions: [String: [String: String]] = [
            "turn sharp right": ["en": instruction, "zh-Hant": "向右後轉", "zh-Hans": "向右后转"],
            "turn sharp left": ["en": instruction, "zh-Hant": "向左後轉", "zh-Hans": "向左后转"],
            "turn slight right": ["en": instruction, "zh-Hant": "稍微向右轉", "zh-Hans": "稍微向右转"],
            "turn slight left": ["en": instruction, "zh-Hant": "稍微向左轉", "zh-Hans": "稍微向左转"],
            "turn right": ["en": instruction, "zh-Hant": "右轉", "zh-Hans": "右转"],
            "turn left": ["en": instruction, "zh-Hant": "左轉", "zh-Hans": "左转"],
            "keep right": ["en": instruction, "zh-Hant": "靠右", "zh-Hans": "靠右"],
            "keep left": ["en": instruction, "zh-Hant": "靠左", "zh-Hans": "靠左"],
            "straight": ["en": instruction, "zh-Hant": "直行", "zh-Hans": "直行"],
            "steps ahead": ["en": instruction, "zh-Hant": "直行", "zh-Hans": "直行"],
            "arrive at destination": ["en": instruction, "zh-Hant": "到達目的地", "zh-Hans": "到达目的地"]
        ]
        
        if let dict = directions[lowerInstruction], let text = dict[languageCode] ?? dict["en"] {
            return text
        }

        // 2. NEW: Handle Building Transitions
        // Pattern matches: "leave the [placeA] and go into the [placeB]"
        let transitionPattern = "leave the (.+?) and go into the (.+)"

        if let regex = try? NSRegularExpression(pattern: transitionPattern),
           let match = regex.firstMatch(in: lowerInstruction, range: NSRange(lowerInstruction.startIndex..., in: lowerInstruction)) {
            
            let rawPlaceA = (lowerInstruction as NSString).substring(with: match.range(at: 1))
            let rawPlaceB = (lowerInstruction as NSString).substring(with: match.range(at: 2))
            
            // 2. Localize the word "building" within the names
            func localizePlace(_ name: String, lang: String) -> String {
                var localizedName = name.capitalized
                if lang.contains("zh") {
                    let buildingWord = (lang == "zh-Hant") ? "大樓" : "大楼"
                    let complexName = (lang == "zh-Hant") ? "香港深圳創新科技園區" : "香港深圳创新科技园"
                    
                    // 1. First, replace "Building" with "大樓"
                    localizedName = localizedName.replacingOccurrences(of: "Building", with: buildingWord)
                    
                    // 2. Then, check if the specific building/park name is mentioned and translate it
                    // Example: If the name contains "Maling Maling", replace it with the tech park name
                    if localizedName.contains("Hong Kong-Shenzhen Innovation And Technology Park") {
                        localizedName = localizedName.replacingOccurrences(of: "Hong Kong-Shenzhen Innovation And Technology Park", with: complexName)
                    }
                    
                    return localizedName
                }
                return localizedName
            }

            let placeA = localizePlace(rawPlaceA, lang: languageCode)
            let placeB = localizePlace(rawPlaceB, lang: languageCode)
            
            // 3. Construct final sentence
            switch languageCode {
            case "zh-Hant":
                return "離開\(placeA)並進入\(placeB)"
            case "zh-Hans":
                return "离开\(placeA)并进入\(placeB)"
            default:
                return instruction
            }
        }

        // 3. Handle Dynamic Floor Instructions (Stairs/Elevator)
        let floorPattern = "take (stairs|elevator) (up|down) from ([a-z0-9]+) to ([a-z0-9]+)"
        if let regex = try? NSRegularExpression(pattern: floorPattern),
           let match = regex.firstMatch(in: lowerInstruction, range: NSRange(lowerInstruction.startIndex..., in: lowerInstruction)) {
            
            let action = (lowerInstruction as NSString).substring(with: match.range(at: 1))
            let dir = (lowerInstruction as NSString).substring(with: match.range(at: 2))
            let fromFloor = (lowerInstruction as NSString).substring(with: match.range(at: 3)).uppercased()
            let toFloor = (lowerInstruction as NSString).substring(with: match.range(at: 4)).uppercased()
            
            switch languageCode {
            case "zh-Hant":
                let tool = (action == "stairs") ? "樓梯" : "電梯"
                let way = (dir == "up") ? "上樓" : "下樓"
                return "從 \(fromFloor) 乘\(tool)\(way)至 \(toFloor)"
            case "zh-Hans":
                let tool = (action == "stairs") ? "楼梯" : "电梯"
                let way = (dir == "up") ? "上楼" : "下楼"
                return "从 \(fromFloor) 乘\(tool)\(way)至 \(toFloor)"
            default: return instruction.capitalized
            }
        }

        return instruction
    }
    
    func calculateEstimationTimeToArriveBasedOnRouteSearcher() -> String {
        let totalSecondsInt = Int(totalEstimationTimeToGetToTheDestination)
        let code = selectedLanguage
        
        // 1. Return early for 0
        guard totalSecondsInt > 0 else {
            return "0 \(translationClass.second(plural: false, code: code))"
        }

        let hours = totalSecondsInt / 3600
        let minutes = (totalSecondsInt % 3600) / 60
        let seconds = totalSecondsInt % 60

        // 2. Build parts array
        var parts: [String] = []
        
        if hours > 0 {
            parts.append("\(hours)\(translationClass.hour(plural: hours > 1, code: code))")
        }
        if minutes > 0 {
            parts.append("\(minutes)\(translationClass.minute(plural: minutes > 1, code: code))")
        }
        if seconds > 0 || parts.isEmpty {
            parts.append("\(seconds)\(translationClass.second(plural: seconds > 1, code: code))")
        }

        // 3. Join with localized separators
        if code == "en" {
            if parts.count == 3 {
                return "\(parts[0]), \(parts[1]) and \(parts[2])"
            } else {
                return parts.joined(separator: " and ")
            }
        } else {
            // Chinese doesn't typically use spaces or "and" between time units
            return parts.joined()
        }
    }
    
    func clearRoute() {
        removeBlueLineRoute()
        instructionList.removeAll()
        instructionPointList.removeAll()
        print("🗑️ Route cleared")
    }
    
    func clearAllMarkersAlongWithTheInstructionLists() {
        // 1. Reset navigation state
        instructionIndex = 0
        focusPoi = nil
        
        // 2. Clear route visual (polylines/path)
        clearRoute()
        
        // 3. Clear all markers (This already handles mapxusMap.removeMXMPointAnnotaions)
        clearAllMarkersAlongWithThePoints()
        
        print("🧹 Logic and Map cleared: Ready for new navigation.")
    }
    
    func clearAllMarkersAlongWithThePoints() {
        // 1. Exit early if map is missing or there are no markers to remove
        guard let mapxusMap = self.mapxusMap, !markerPoints.isEmpty else { return }
        
        // 2. Clear all annotations from the map in one call
        mapxusMap.removeMXMPointAnnotaions(markerPoints)
        
        print("🗑️ Removed all \(markerPoints.count) markers: \(markerPoints.compactMap { $0.title })")
        
        // 3. Reset all data and references
        markerPoints.removeAll()
        startMarker = nil
        endMarker = nil
        startPoint = nil
        endPoint = nil
    }
    
//    func clearSelectedMarkers(withTitle title: String) {
//        guard let mapxusMap = self.mapxusMap else { return }
//        
//        // Find all markers in your local list that match the title
//        let targets = markerPoints.filter { $0.title == title }
//        
//        if !targets.isEmpty {
//            // Remove from the visual map
//            mapxusMap.removeMXMPointAnnotaions(targets)
//            
//            // Remove from your data array
//            markerPoints.removeAll { $0.title == title }
//            
//            print("🗑️ Removed \(targets.count) markers with title: \(title)")
//        }
//    }
    
    func clearSelectedMarkers(withTitle title: String) {
        guard let mapxusMap = self.mapxusMap else { return }
        
        // 1. Filter targets once
        let targets = markerPoints.filter { $0.title == title }
        guard !targets.isEmpty else { return }
        
        /// 2. Clear from visual map and main data list
        mapxusMap.removeMXMPointAnnotaions(targets)
        markerPoints.removeAll { $0.title == title }
        
        /// 3. Reset specific references based on the title
        if title == "Start" {
            startMarker = nil
            startPoint = nil
        } else if title == "Destination" { // Explicit check for "End"
            endMarker = nil
            endPoint = nil
        }
        
        print("🗑️ Removed \(targets.count) markers with title: \(title)")
    }
    
    func clearAllMarkerIcons() {
        guard let mapxusMap = self.mapxusMap else { return }
        
        if !markerPoints.isEmpty {
            // Remove everything in our list from the map at once
            mapxusMap.removeMXMPointAnnotaions(markerPoints)
            
            print("🗑️ Removed all \(markerPoints.count) markers from map")
            print("Current markers: \(markerPoints.map { $0.title ?? "No Title" })")
            
            // Clear the local array
            markerPoints.removeAll()
        }
    }
    
    func showAllMarkerIcons() {
        guard let mapxusMap = self.mapxusMap else { return }
        if !markerPoints.isEmpty {
            // Use the specific Mapxus method for point annotations
            mapxusMap.add(markerPoints)
            print("✅ Added \(markerPoints.count) markers back to map")
        }
    }
    
    func hideAllMarkerIcons() {
        guard let mapxusMap = self.mapxusMap else { return }
        if !markerPoints.isEmpty {
            // Just remove from map, do NOT call markerPoints.removeAll()
            mapxusMap.removeMXMPointAnnotaions(markerPoints)
        }
    }
    
    func nextStep() {
        // DEBUG: See the numbers in the console
        print("Step Debug: Index is \(instructionIndex), Total instructions: \(instructionList.count)")

        // 1. Check if the list is empty first
        if instructionList.isEmpty {
            print("⚠️ instructionList is empty, cannot proceed.")
            return
        }

        // 2. Check if we are at the end
        // Using >= count - 1 means "if this is the last item OR we somehow went past it"
        if instructionIndex >= instructionList.count - 1 {
            self.mapState = .welcoming
            self.endNavigation()
            
            print("📍 User has arrived at the destination.")
            return
        } else if instructionIndex >= instructionList.count - 2 {
            self.isShowingArrivedAtTheDestinationAlertDialog = true
        }

        // 3. Increment safely
        instructionIndex += 1
        updateRoute(routeOption: selectedRouteType(type: activeRouteType, languageCode: selectedLanguage))
    }
    
    func previousStep() {
        instructionIndex -= 1;
        if instructionIndex >= 0 {
            updateRoute(routeOption: selectedRouteType(type: activeRouteType, languageCode: selectedLanguage))
        } else {
            instructionIndex = 0
        }
    }
    
    func selectVenue(id: String) {
        mapxusMap?.selectVenue(byId: id, zoomMode: .animated, edgePadding: .zero)
    }
    
    func selectBuilding(id: String) {
        let option = MXMPoiSearchOption()
        
        mapxusMap?.selectBuilding(byId: id, zoomMode: .animated, edgePadding: .zero)
        mapxusMap?.autoChangeBuilding = true
        poiSearcher?.searchPois(by: option)
        
//        self.requestCategory(buildingId: id)
        self.findAllIndoorCoordinatesBasedOnBuildingId(buildingId: id, keyword: .constant(""), allBuilding: .constant(false))
//        self.findAllIndoorCoordinatesBasedOnBuildingId(
//            buildingId: id,
//            allBuilding: Binding(
//                get: { self.isSearchingAllFacilitiesOnEveryBuilding },
//                set: { self.isSearchingAllFacilitiesOnEveryBuilding = $0 }
//            )
//        )
    }
    
    func isShowingSelectedBuildingBasedOnSwiping(id: String) {
        if !isRotatingTheMapOnGPSButtonClicked {
            mapxusMap?.selectBuilding(byId: id, zoomMode: .animated, edgePadding: .zero)
            mapxusMap?.autoChangeBuilding = true
        }
    }
    
    func isFoldingFloorBarSection(fold: Bool) {
        guard let map = mapxusMap else {
            print("⚠️ MapxusMap is nil. Cannot fold floor bar.")
            return
        }
        
        // Check if the floor bar is even visible before trying to fold it
        if map.floorBar.isHidden {
            print("ℹ️ Floor bar is currently hidden, folding state might not be visible.")
        }

        DispatchQueue.main.async {
            map.floorBar.isFolded = fold
            print("UI: Floor bar folded set to \(fold)")
        }
    }
    
    func setCenterView(zoomLevel: Double, bottom: CGFloat) {
        guard let poi = focusPoi else { return }
        
        let coord = CLLocationCoordinate2D(latitude: poi.lat, longitude: poi.lng)
        
        // 1. Ensure the map is showing the correct floor for this POI
        if !poi.floorId.isEmpty {
            mapxusMap?.selectFloor(byId: poi.floorId)
        }

        // 2. Set the Camera
        // Zoom 18-19 is usually better for indoor POIs than 16 (which is quite far out)
        mapView?.setCenter(coord, animated: true)
        
        let camera = MGLMapCamera(lookingAtCenter: coord, acrossDistance: 500, pitch: 45, heading: 0)
        
        // Offset the center so the POI appears above the bottom sheet
        mapView?.setContentInset(UIEdgeInsets(top: 0, left: 0, bottom: bottom, right: 0), animated: true, completionHandler: {
            
        })
        
        /// Animate the Map
//        mapView?.setCamera(camera, animated: true)
    }
    
    func routeSearcher(_ routeSearcher: MXMRouteSearch, didReceiveRouteResult searchResult: MXMRouteSearchResult?, error: (any Error)?) {
        guard let searchResult, let firstPath = searchResult.paths.first else {
            print("❌ Failed to receive route result")
            return
        }
        
        // Safely extracting the coordinates from the waypoints
        let coord = CLLocationCoordinate2D(
            latitude: startPoint?.latitude ?? 0.0,
            longitude: startPoint?.longitude ?? 0.0
        )
        
        let start = startPoint.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
        let end = endPoint.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }

        if mapState == .navigating && instructionList.isEmpty {
            instructionPointList = firstPath.points?.coordinates ?? []
            instructionList = firstPath.instructions
            
            updateRoute(routeOption: selectedRouteType(type: activeRouteType, languageCode: selectedLanguage))
        }
        
        if let route = searchResult.paths.first {
            // FIX: Divide by 1000 to convert milliseconds to seconds
            let timeInSeconds = Double(route.time) / 1000.0
            
            // Capture the actual time from the map engine
            self.totalEstimationTimeToGetToTheDestination = UInt(timeInSeconds)
            
            // Debugging: verify the values in your console
            print("Estimation Meters: \(route.distance)") // Should be 270
            print("Estimation Seconds: \(route.time)")     // Should be roughly 210-240
            print("Estimation Converted Time (s): \(timeInSeconds)") // Should now be ~21.3
        }

        // ✅ Draw with routePainter
        routePainter?.routeStyle.isAddStartDash = true
        routePainter?.routeStyle.dashedLineColor = UIColor(Color.mainColor)
        routePainter?.updateFullPath(firstPath, waypoints: searchResult.waypoints)
        routePainter?.drawRoute(with: firstPath)

        // ✅ Optional: focus camera
        // 3. FIX: Only focus the camera if NOT navigating
        if let key = routePainter?.dto?.keys.first,
           let paragraph = routePainter?.dto?.paragraphs[key] {
            // 2. Define the safe area padding (Adjust bottom to push user dot up)
            let navPadding = UIEdgeInsets(top: 120, left: 50, bottom: 120, right: 25)
            
            // 1. Sync Floor and Venue
            if mapState == .navigating {
                mapView?.setCenter(coord, animated: true)
                mapxusMap?.selectFloor(byId: paragraph.floorId, zoomMode: .disable, edgePadding: .zero)
                routePainter?.change(onVenue: paragraph.venueId, ordinal: paragraph.ordinal)
                routePainter?.focus(onKeys: [key], edgePadding: navPadding)
                
                print("route searcher ordinal: \(paragraph.ordinal?.level ?? 0 as Int)")
            } else if mapState == .showingRoute {
                let currentFloorId = ""
//                if currentFloorId != paragraph.floorId {
                    if let start = start, let end = end {
                        let bounds = MGLCoordinateBounds(sw: start, ne: end)
                        mapView?.setVisibleCoordinateBounds(bounds, edgePadding: navPadding, animated: true, completionHandler: nil)
                    }
//                }
                
//                mapView?.setCenter(startCoord, zoomLevel: 19, animated: true)
                mapxusMap?.selectFloor(byId: paragraph.floorId, zoomMode: .disable, edgePadding: navPadding)
                routePainter?.change(onVenue: paragraph.venueId, ordinal: paragraph.ordinal)
            }
        }
    }

    func startRouting(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) {
        guard mapView != nil else { return }
        
        routeSearcher?.delegate = self
    }
    
    func selectFloorIdBasedOnUserLocation() {
        let getUserLocationByFloorId = mapxusMap?.userLocationFloor?.floorId
        let getUserLocationByBuilding = mapxusMap?.userLocationBuilding?.venueId
        let getUserLocationByVenue = mapxusMap?.userLocationVenue?.category
        
        print("user location floor: \(String(describing: getUserLocationByFloorId)), building: \(String(describing: getUserLocationByBuilding)), venue: \(String(describing: getUserLocationByVenue))")
    }
    
    func buildingSearcher(_ buildingSearcher: MXMBuildingSearch, didReceiveBuildingsWith searchResult: MXMBuildingSearchResult?, error: Error?) {
        if let error = error {
            print("❌ Building Search Error: \(error.localizedDescription)")
            return
        }
        
        guard let buildings = searchResult?.buildings, !buildings.isEmpty else {
            print("⚠️ No buildings found.")
            return
        }
        
        // 1. Sort using the SELECTED language, not hardcoded .en
        let sortedRawBuildings = buildings.sorted { (b1, b2) -> Bool in
            let num1 = self.getBuildingNumberInMultipleLanguages(from: b1, for: self.selectedLanguage)
            let num2 = self.getBuildingNumberInMultipleLanguages(from: b2, for: self.selectedLanguage)
            return num1.localizedStandardCompare(num2) == .orderedAscending
        }
        
        // 2. Map the data
        let mappedBuildings = sortedRawBuildings.compactMap { building -> BuildingLists? in
            // Use the localized helpers for both fields
            let localizedName = self.getBuildingNameInMultipleLanguages(from: building, for: self.selectedLanguage)
            let localizedNumber = self.getBuildingNumberInMultipleLanguages(from: building, for: self.selectedLanguage)
            let info = self.extractBuildingNameAndNumberInMultipleLanguages(from: building, for: self.selectedLanguage)
            
            print("building category: \(building.category ?? "" as String), venue name \(building.venueNameMap.description), venue id: \(building.venueId)")
            
            return BuildingLists(
                id: building.buildingId,
                venueId: building.venueId,
                buildingName: info.name,
                buildingNumber: localizedNumber
            )
        }
        
        DispatchQueue.main.async {
            self.buildingLists = mappedBuildings
        }
    }
    
//    func poiSearcher(_ poiSearch: MXMPoiSearch, didReceivePoisWith poiResult: MXMPoiSearchResult?, error: Error?) {
//        print("📡 POI Searcher DELEGATE TRIGGERED")
//        
//        // 1. Handle Error Case with Retry Logic
//        Task {
//            if let error = error {
//                let nsError = error as NSError
//                
//                // 1. Check for "No Internet" specifically
//                if nsError.code == NSURLErrorNotConnectedToInternet {
//                    print("❌ No Internet Connection. Stopping retries.")
//                    await MainActor.run {
//                        self.isShowingLoadingOnBuildingFacilities = false
//                        self.isShowingLossInternetConnectionButton = true
//                        self.isShowingAnEmptyFacilityCategory = false
//                        self.allViewReceiver.isShowingACustomToastInternet = true
//                        self.allViewReceiver.isShowingACustomToastMessageInternet = "No Internet Connection"
//                        self.allViewReceiver.isShowingACustomToastIconInternet = "wifi.slash"
//                        self.allViewReceiver.isShowingACustomToastAlignmentInternet = Alignment.bottom
//                    }
//                    
//                    return
//                }
//
//                // 2. Handle other retriable errors (Timeout, Server Error, etc.)
//                await handlePoiRetry(poiSearch)
//                
//                // Inside your poiSearcher delegate success block:
//                if self.poiRetryCount > 0 { // Only show if we actually had to retry
//                    await MainActor.run {
//                        self.allViewReceiver.isShowingACustomToastInternet = true
//                        self.allViewReceiver.isShowingACustomToastMessageInternet = "Internet connection refreshed successfully"
//                        self.allViewReceiver.isShowingACustomToastIconInternet = "wifi"
//                        self.allViewReceiver.isShowingACustomToastAlignmentInternet = .bottom
//                    }
//                }
//                
//                return
//            }
//
//            // 3. Success Case
//            poiRetryCount = 0
//        }
//        
//        // 2. Handle Empty Results Case
//        guard let pois = poiResult?.pois, !pois.isEmpty else {
//            print("⚠️ No POIs found.")
//            DispatchQueue.main.async {
//                self.buildingFacilities = []
//                self.allBuildingFacilities = []
//                self.isShowingLoadingOnBuildingFacilities = false
//            }
//            return
//        }
//        
//        // 3. Map the data (Runs on the current background thread)
//        let mappedFacilities = pois.compactMap { poi -> BuildingFacilityData? in
//            let associatedBuilding = buildingLists.first(where: { $0.id == poi.buildingId })
//            let rawCategory = poi.category.first ?? "general"
//            let category = self.getNormalizedCategory(from: rawCategory.capitalized)
//            let cleanCategory = self.getNormalizedCategory(from: rawCategory.capitalized).first ?? ""
//            let fId = poi.floor?.floorId ?? ""
//            let facilityName = (poi.nameMap.en ?? "Unknown") as String
//            let localizedName = self.getFacilityNameInMultipleLanguages(from: poi, for: self.selectedLanguage)
//            
//            return BuildingFacilityData(
//                id: poi.poiId,
//                buildingId: (poi.buildingId ?? "") as String,
//                buildingName: associatedBuilding?.buildingNumber ?? "Unkown Building",
//                facilityName: localizedName,
//                floorName: (poi.floor?.name ?? "Unknown Floor") as String,
//                floorId: fId,
//                category: category,
//                categoryCode: [cleanCategory: poi.category],
//                lat: poi.location?.latitude ?? 0.0,
//                lon: poi.location?.longitude ?? 0.0
//            )
//        }
//
//        // 4. Update UI - Instant update on the Main Thread
//        DispatchQueue.main.asyncAfter(deadline: .now(), execute: {
//            self.buildingFacilities = mappedFacilities
//            self.isShowingLoadingOnBuildingFacilities = false
//            print("✅ UI Updated with \(self.buildingFacilities.count) facilities.")
//        })
//        
//        if ((self.buildingFacilities.first?.categoryCode.values.isEmpty) == nil) {
//            self.isShowingLossInternetConnectionButton = false
//            self.isShowingAnEmptyFacilityCategory = true
//        }
//    }
    
    func poiSearcher(_ poiSearch: MXMPoiSearch, didReceivePoisWith poiResult: MXMPoiSearchResult?, error: Error?) {
        print("📡 POI Searcher DELEGATE TRIGGERED")
        
        // 1. Handle Error Case (Internet/Retry)
        if let error = error {
            handlePoiError(error: error, searcher: poiSearch, poiResult: poiResult)
            return
        }

        // 2. Success - Reset Retry Count
        self.poiRetryCount = 0

        // 3. Handle Empty Results Case
        guard let pois = poiResult?.pois, !pois.isEmpty else {
            DispatchQueue.main.async {
                self.buildingFacilities = []
                self.allBuildingFacilities = []
                self.isShowingLoadingOnBuildingFacilities = false
            }
            return
        }

        // 4. Map & Update Based on Mode
        if isSearchingAllFacilitiesOnEveryBuilding {
            let mapped = pois.compactMap { self.allBuildingFacilities(poi: $0) }
            
            DispatchQueue.main.asyncAfter(deadline: .now(), execute: {
                self.allBuildingFacilities = mapped
                self.isShowingLoadingOnBuildingFacilities = false
                self.isShowingLossInternetConnectionButton = false
                print("🌍 Global UI Updated: \(mapped.count) facilities.")
            })
        } else {
            let mapped = pois.compactMap { self.specificBuildingFacilities(poi: $0) }
            
            DispatchQueue.main.asyncAfter(deadline: .now(), execute: {
                self.buildingFacilities = mapped
                self.isShowingLoadingOnBuildingFacilities = false
                self.isShowingLossInternetConnectionButton = false
                print("🛰️ Local UI Updated: \(mapped.count) facilities.")
            })
            
            if ((self.buildingFacilities.first?.categoryCode.values.isEmpty) == nil) {
                self.isShowingLossInternetConnectionButton = false
                self.isShowingAnEmptyFacilityCategory = true
                print("specific building facility category is empty \(isShowingAnEmptyFacilityCategory)")
            }
        }
    }

    // Helper to keep the delegate clean
    private func specificBuildingFacilities(poi: MXMPOI) -> BuildingFacilityData {
        let associatedBuilding = buildingLists.first(where: { $0.id == poi.buildingId })
        let rawCategory = poi.category.first ?? "general"
        let category = self.getNormalizedCategory(from: rawCategory.capitalized)
        let cleanCategory = category.first ?? ""
        
        return BuildingFacilityData(
            id: poi.poiId,
            buildingId: poi.buildingId ?? "",
            buildingNumber: associatedBuilding?.buildingNumber ?? "Unknown Building",
            facilityName: self.getFacilityNameInMultipleLanguages(from: poi, for: self.selectedLanguage),
            floorName: poi.floor?.name ?? "Unknown Floor",
            floorId: poi.floor?.floorId ?? "",
            category: category,
            categoryCode: [cleanCategory: poi.category],
            lat: poi.location?.latitude ?? 0.0,
            lon: poi.location?.longitude ?? 0.0
        )
    }

    // Similarly for mapToGlobalData using AllBuildingFacilityData...
    private func allBuildingFacilities(poi: MXMPOI) -> AllBuildingFacilityData {
        // 1. Find the building info for this POI to get the building name
        // We search the buildingLists for a buildingId that matches the POI
        let associatedBuilding = buildingLists.first(where: { $0.id == poi.buildingId })
        
        // 2. Prepare Category Data
        let rawCategory = poi.category.first ?? "general"
        let normalizedCategories = self.getNormalizedCategory(from: rawCategory.capitalized)
        let primaryCategory = normalizedCategories.first ?? ""
        
        // 3. Return the mapped data
        // Note: We return the data even if associatedBuilding is nil,
        // but we use a fallback for the buildingName.
        return AllBuildingFacilityData(
            id: poi.poiId,
            buildingId: poi.buildingId ?? "",
            buildingNumber: associatedBuilding?.buildingNumber ?? "Unknown Building",
            facilityName: self.getFacilityNameInMultipleLanguages(from: poi, for: self.selectedLanguage),
            floorName: poi.floor?.name ?? "Unknown Floor",
            floorId: poi.floor?.floorId ?? "",
            category: normalizedCategories,
            categoryCode: [primaryCategory: poi.category],
            lat: poi.location?.latitude ?? 0.0,
            lon: poi.location?.longitude ?? 0.0
        )
    }
    
    func handlePoiError(error: Error, searcher: MXMPoiSearch, poiResult: MXMPoiSearchResult?) {
        Task(operation: {
            let pois = poiResult?.pois
            let nsError = error as NSError
            
            // 1. Check for "No Internet" specifically
            if nsError.code == NSURLErrorNotConnectedToInternet {
                print("❌ No Internet Connection. Stopping retries.")
                
                await MainActor.run {
                    /// Reset all building facilities
//                    self.allBuildingFacilities = []
                    
                    // 2. Building Match Logic (Only if we have POI data despite the error)
                    if let firstPoiBuildingId = pois?.first?.buildingId,
                       let associatedBuilding = buildingLists.first(where: { $0.id == firstPoiBuildingId }) {
                        
                        if associatedBuilding.id != isGettingLastBuildingIdOnMapView {
                            // IDs don't match: Clear the list as the user moved to a different building
                            self.buildingFacilities = []
                            print("⚠️ Building mismatch: clearing facilities. Map: \(isGettingLastBuildingIdOnMapView), POI: \(associatedBuilding.id)")
                        } else {
                            // IDs match: Try to recover whatever data we did get
                            let mapped = pois?.compactMap { self.specificBuildingFacilities(poi: $0) }
                            self.buildingFacilities = mapped ?? []
                            print("✅ Building match: updated facilities despite error.")
                        }
                    }
                    self.isShowingLossInternetConnectionButton = true
                    self.isShowingLoadingOnBuildingFacilities = false
                    self.isShowingAnEmptyFacilityCategory = false
                    self.allViewReceiver.showInternetToast(message: "No Internet Connection", icon: "wifi.slash")
                }
                
                return
            }

            // 2. Handle other retriable errors (Timeout, Server Error, etc.)
            await handlePoiRetry(searcher)
            
            // Inside your poiSearcher delegate success block:
            if self.poiRetryCount > 0 { // Only show if we actually had to retry
                await MainActor.run {
                    self.allViewReceiver.showInternetToast(message: "Internet connection refreshed successfully", icon: "wifi")
                }
            }
        })
    }
    
    func handlePoiRetry(_ poiSearch: MXMPoiSearch, isManual: Bool = false) async {
        if isManual {
            self.poiRetryCount = 0
            await MainActor.run {
                self.isShowingAnEmptyFacilityCategory = false
                self.isShowingLoadingOnBuildingFacilities = true
            }
        }
        
        if poiRetryCount < maxPoiRetries {
            poiRetryCount += 1
            // Exponential backoff
            let seconds = Double(poiRetryCount) * 2.0
            try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            
            print("🔄 Retrying search... Attempt \(poiRetryCount)")
            if let request = self.lastPoiRequest {
                poiSearch.searchPois(by: request)
            }
        } else {
            await MainActor.run {
                self.isShowingLoadingOnBuildingFacilities = false
                self.poiRetryCount = 0
                // Notify user that retries failed
                self.isShowingAnEmptyFacilityCategory = false
                self.isShowingLoadingOnBuildingFacilities = true
                self.allViewReceiver.showInternetToast(message: "Connection failed after multiple attempts.", icon: "wifi.exclamationmark")
            }
        }
    }
    
    func performPoiSearch(with request: MXMPoiSearchOption) {
        self.lastPoiRequest = request // 👈 Crucial step
        self.poiSearch?.searchPois(by: request)
    }
    
//    func findAllIndoorCoordinatesBasedOnBuildingId(buildingId: String) {
//        // 1. Start the loading spinner immediately
//        self.isShowingLoadingOnBuildingFacilities = true
//        
//        let searcher = MXMPoiSearch()
//        let option = MXMPoiSearchOption()
//        
//        option.buildingId = buildingId
//        // ✅ FIX: Use 'offset' to set the number of items per page
//        // Mapxus uses 'offset' as the "Limit" (max 100)
//        option.offset = 100
//        
//        // Start at the first page
//        option.page = 1
//        
//        // Correctly initialize the exclusion array
//        option.excludeCategories = [
//            "facility.connector.elevator",
//            "facility.connector.stairs",
//            "facility.connector.escalator",
//            "facility.steps"
//        ]
//        
//        print("🛰️ Requesting first 100 POIs for building: \(buildingId)")
//        searcher.searchPois(by: option)
//    }
    
    func findAllIndoorCoordinatesBasedOnBuildingId(buildingId: String, keyword: Binding<String>, allBuilding: Binding<Bool>) {
        self.isShowingLoadingOnBuildingFacilities = true
        
        let searcher = MXMPoiSearch()
        searcher.delegate = self
        
        // 1. Initialize the correct Option class based on the toggle
        if allBuilding.wrappedValue {
            // GLOBAL SEARCH: Searches across all buildings
            // Note: You might need to use MXMGlobalSearchOption or
            // a Bound search if you want to limit results to a certain city/area.
            let globalOption = MXMPoiSearchOption()
            // Leaving buildingId nil in some SDK versions triggers a global search,
            // but verify if your version requires MXMPoiBoundSearchOption for 'All'.
            globalOption.venueId = "2506d124f4d049fb8b5019ed9d78c309"
            globalOption.keyword = keyword.wrappedValue
            configureCommonOptions(option: globalOption)
            print("🌍 Requesting global POI search (All Buildings)")
            searcher.searchPois(by: globalOption)
            
        } else {
            // BUILDING-SPECIFIC SEARCH
            let buildingOption = MXMPoiSearchOption()
            buildingOption.buildingId = buildingId
            
            configureCommonOptions(option: buildingOption)
            print("🛰️ Requesting first 100 POIs for building: \(buildingId)")
            searcher.searchPois(by: buildingOption)
        }
    }

    // 2. Helper to keep code DRY (Don't Repeat Yourself)
    private func configureCommonOptions(option: MXMPoiSearchOption) {
        option.offset = 100
        option.page = 1
        option.excludeCategories = [
            "facility.connector.elevator",
            "facility.connector.stairs",
            "facility.connector.escalator",
            "facility.steps"
        ]
    }
    
//    func onGetPoiResult(_ searcher: MXMPoiSearch!, result: MXMPoiSearchResult!, error: Error!) {
//        self.isShowingLoadingOnBuildingFacilities = false
//        
//        if let results = result?.pois {
//            let newData = results.map { poi in
//                let rawCategory = poi.category.first ?? "general"
//                let category = self.getNormalizedCategory(from: rawCategory.capitalized)
//                let cleanCategory = self.getNormalizedCategory(from: rawCategory.capitalized).first ?? ""
//                let fId = poi.floor?.floorId ?? ""
//                let facilityName = (poi.nameMap.en ?? "Unknown") as String
//                let localizedName = self.getFacilityNameInMultipleLanguages(from: poi, for: self.selectedLanguage)
//                
//                return BuildingFacilityData(
//                    id: poi.poiId,
//                    buildingId: (poi.buildingId ?? "") as String,
//                    facilityName: localizedName,
//                    floorName: (poi.floor?.name ?? "Unknown Floor") as String,
//                    floorId: fId,
//                    category: category,
//                    categoryCode: [cleanCategory: poi.category],
//                    lat: poi.location?.latitude ?? 0.0,
//                    lon: poi.location?.longitude ?? 0.0
//                )
//            }
//            
//            if isSearchingAllFacilitiesOnEveryBuilding {
//                // Replace with global results so the list only shows relevant matches
//                self.buildingFacilities = newData
//            } else {
//                // Standard behavior for single building
//                self.buildingFacilities = newData
//            }
//        }
//    }
    
    func categorySearcher(_ categorySearcher: MXMCategorySearch, didReceivePoiCategoryWith searchResult: MXMPoiCategorySearchResult?, error: Error?) {
        if let error = error {
            print("❌ Category Search Error: \(error.localizedDescription)")
            return
        }
        
        if let categories = searchResult?.categoryResults {
            print("✅ Found \(categories.count) categories in this building")
            
            DispatchQueue.main.async {
                self.buildingCategories = categories
            }
            
            // Debug: Print out the names of categories found
            for cat in categories {
                print("Category Found: Category \(cat.category)")
            }
        }
    }
    
    func getNormalizedCategory(from raw: String) -> [String] {
        // 1. Lowercase for safety to avoid case-sensitivity issues
        let category = raw.lowercased()
        
        // 2. Switch with pattern matching is cleaner than multiple 'if' statements
        switch category {
            
        case _ where category.contains("restaurants"):
            return ["Restaurant", "餐廳", "餐厅"]
            
        case _ where category.contains("workplace") || category.contains("facility.attractions") || category.contains("facility.information") || category.contains("facility.meeting_room") || category.contains("facility.reception_desk") || category.contains("lab"):
            return ["Company", "公司"]
            
        case _ where category.contains("restroom") || category.contains("shower"):
            return ["Washroom", "洗手間", "洗手间"]
            
        // 3. Fix: Use OR (||) instead of AND (&&)
        case _ where category.contains("local_services") ||
                     category.contains("mothersroom") || category.contains("facility.tactile_map") || category.contains("slope") || category.contains("facility.defibrillator"):
            return ["Utilities", "公用事業", "公用事业"]
            
        case _ where category.contains("shop") || category.contains("convenience"):
            return ["Shop", "商店"]
            
        case _ where category.contains("connector") || category.contains("stairs"):
            return ["Transportation", "公用事業", "公用事业"]

        // 4. Default to "All" or an empty string if no match is found
        default:
            return ["All"]
        }
    }
    
    // 6.3.2 Implementation: Searches categories in a building
    func requestCategory(buildingId: String) {
        let searcher = MXMCategorySearch()
        searcher.delegate = self
        
        let option = MXMPoiCategoryBuildingSearchOption()
        option.buildingId = buildingId
        
        print("🛰️ Requesting categories for building: \(buildingId)")
        searcher.searchPoiCategories(byBuilding: option)
    }
    
    func searchBuildingLists() {
        let searcher = MXMBuildingSearch()
        searcher.delegate = self // Ensure your class conforms to MXMBuildingSearchDelegate
        
        let option = MXMBuildingSearchOption()
        
        // Set pagination - same logic as POI search
        option.offset = 100 // Max results per page
        option.page = 1
        
        print("🏢 Searching for buildings...")
        searcher.searchBuildings(by: option)
    }
    
    func textToSpeech(isActive: Bool) {
        let voiceCode = isSelectingAssistantGuideLanguageAVSpeechSynthesisVoiceCode

        // 1. Determine the localized string
        let textToSpeak: String
        
        if isActive {
            textToSpeak = "Text to speech is enabled"
        } else {
            textToSpeak = "Text to speech is disabled"
        }
        
        // 2. Configure Utterance
        let utterance = AVSpeechUtterance(string: textToSpeak)
        utterance.voice = AVSpeechSynthesisVoice(language: voiceCode)
        
        // Slight pitch/rate adjustment for Chinese clarity
        utterance.rate = 0.45
        if voiceCode.contains("zh") {
            utterance.pitchMultiplier = 1.1 // Slightly higher pitch often sounds clearer for Asian languages
        }
        
        // 3. Audio Management
        if speechSynthesizer.isSpeaking {
            speechSynthesizer.stopSpeaking(at: .immediate)
        }
        
        speechSynthesizer.speak(utterance)
    }
    
    func resetMapRotation() {
        guard let mapView = mapView else { return }
        
        // Use the existing altitude so the map doesn't zoom in to the ground level
        let currentAltitude = mapView.camera.altitude
        
        let camera = MGLMapCamera(lookingAtCenter: mapView.userLocation?.coordinate ?? mapView.centerCoordinate, altitude: currentAltitude, pitch: 0, heading: 0)
        
        mapView.fly(to: camera, withDuration: 1.5, completionHandler: nil)
    }
    
    func resetNorth() {
        guard let mapView = mapView else { return }
        
        // 1. Capture the current camera
        let currentCamera = mapView.camera
        
        // 2. Set the target values: North (0) and Flat (0)
        mapView.resetNorth()
        mapView.direction = 0
        
        currentCamera.heading = 0
        currentCamera.pitch = 0
        
        // 3. Apply as a single update to avoid animation conflicts
        mapView.setCamera(currentCamera, withDuration: 0.8, animationTimingFunction: CAMediaTimingFunction(name: .easeInEaseOut), completionHandler: nil)
    }
    
    func resetNavigationPathToThePreviousOne() {
        // To go back to BuildingList
        if !navigationDestinationPath.isEmpty {
            navigationDestinationPath.removeLast()
        }
    }
    
    func resetNavigationPathToRoot() {
        // To go back to BuildingList
        navigationDestinationPath = NavigationPath()
    }
    
    func extractBuildingNameAndNumber(from buildingName: String) -> (name: String, number: String) {
        // This '??' is what actually removes the "Optional()" text
        let nameToProcess = buildingName
        
        if nameToProcess.isEmpty {
            return ("Unknown", "")
        }
        
        // ... rest of your splitting logic ...
        let components = nameToProcess.lowercased().components(separatedBy: "building")
        let cleanName = components.first?.trimmingCharacters(in: .whitespaces).capitalized ?? nameToProcess
        let num = components.last?.trimmingCharacters(in: .whitespaces) ?? ""
        
        return (cleanName, num.isEmpty ? "" : "Building \(num)")
    }
    
    func extractBuildingNameAndNumberInMultipleLanguages(from building: MXMBuilding, for languageCode: String) -> (name: String, number: String) {
        let fullName = self.getBuildingNameInMultipleLanguages(from: building, for: languageCode)
        if fullName.isEmpty { return ("Unknown", "") }

        // Regex pattern: Finds the first digit and everything that follows it
        let pattern = #"\d+.*$"#
        
        if let range = fullName.range(of: pattern, options: .regularExpression) {
            let namePart = fullName[..<range.lowerBound].trimmingCharacters(in: .whitespaces)
            let numberPart = fullName[range.lowerBound...].trimmingCharacters(in: .whitespaces)
            
            let formattedNumber: String
            let formattedName: String

            if languageCode.contains("zh-Hans-TW") {
                formattedName = namePart.replacingOccurrences(of: "building", with: "", options: .caseInsensitive)
                    .trimmingCharacters(in: .whitespaces)
                    .capitalized
                
                formattedNumber = numberPart.lowercased().contains("building")
                    ? numberPart.capitalized
                    : "Building \(numberPart)"
                
            } else if languageCode.contains("zh") {
                // Chinese: Keep parts as found (e.g., "1期" or "A座")
                formattedName = namePart.isEmpty ? fullName : namePart
                formattedNumber = numberPart
            } else {
                // English/Other: Ensure "Building" prefix exists and is capitalized
                // Removes "Building" from namePart if Regex left it there
                formattedName = namePart.replacingOccurrences(of: "building", with: "", options: .caseInsensitive)
                    .trimmingCharacters(in: .whitespaces)
                    .capitalized
                
                formattedNumber = numberPart.lowercased().contains("building")
                    ? numberPart.capitalized
                    : "Building \(numberPart)"
            }
            
            return (formattedName.isEmpty ? "Building" : formattedName, formattedNumber)
        }

        return (fullName, "")
    }
    
    func defaultRouteType(code: String) -> String {
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
    
    func selectedRouteType(type: String, languageCode: String) -> String {
        switch languageCode {
        case "zh-Hant":
            switch type {
            case "最短步行": return "最短步行"
            case "僅限電梯": return "僅限電梯"
            case "限手扶梯": return "限手扶梯"
            default:        return "最短步行"
            }
        case "zh-Hans":
            switch type {
            case "最短步行": return "最短步行"
            case "仅限电梯": return "仅限电梯"
            case "仅限自动扶梯": return "仅限自动扶梯"
            default:        return "最短步行"
            }
        default:
            switch type {
            case "Shortest Walk": return "Shortest Walk"
            case "Lift Only":      return "Lift Only"
            case "Escalator Only": return "Escalator Only"
            default:               return "Shortest Walk"
            }
        }
    }
    
    /// Map Language
//    func mapContext(_ mapContext: MapxusMap, didCreate mapxusMap: MapxusMap) {
//        self.mapxusMap = mapxusMap
//        
//        // Apply your saved language choice
//        mapxusMap.setMapLanguage(self.selectedLanguage)
//        
//        if let mapView = self.mapView {
//            mapView.style?.localizeLabels(into: Locale(identifier: self.selectedLanguage))
//        }
//        
//        print("🚀 Mapxus Engine Created with language: \(self.selectedLanguage)")
//    }
    
    func getBuildingNameInMultipleLanguages(from building: MXMBuilding, for languageCode: String) -> String {
        // Access the nameMap directly from the POI object
        let names = building.nameMap
        
        switch languageCode {
        case "zh-Hant":
            return (names.zh_Hant ?? names.en ?? "") as String
        case "zh-Hans":
            return (names.zh_Hans ?? names.en ?? "") as String
        case "zh-Hant-TW":
            return (names.zh_Hant_TW ?? names.en ?? "") as String
        case "ja":
            return (names.ja ?? names.en ?? "") as String
        case "ko":
            return (names.ko ?? names.en ?? "") as String
        default:
            return (names.en ?? names.en ?? "") as String
        }
    }
    
    func getBuildingNumberInMultipleLanguages(from building: MXMBuilding, for languageCode: String) -> String {
        // Access the nameMap directly from the POI object
        let names = building.buildingNameMap
        
        switch languageCode {
        case "zh-Hant":
            return (names.zh_Hant ?? names.en ?? "") as String
        case "zh-Hans":
            return (names.zh_Hans ?? names.en ?? "") as String
        case "zh-Hant-TW":
            return (names.zh_Hant_TW ?? names.en ?? "") as String
        case "ja":
            return (names.ja ?? names.en ?? "") as String
        case "ko":
            return (names.ko ?? names.en ?? "") as String
        default:
            return (names.en ?? names.en ?? "") as String
        }
    }
    
    func getFacilityNameInMultipleLanguages(from poi: MXMPOI, for languageCode: String) -> String {
        // Access the nameMap directly from the POI object
        let names = poi.nameMap
        
        switch languageCode {
        case "zh-Hant":
            return (names.zh_Hant ?? names.en ?? "") as String
        case "zh-Hans":
            return (names.zh_Hans ?? names.en ?? "") as String
        case "zh-Hant-TW":
            return (names.zh_Hant_TW ?? names.en ?? "") as String
        case "ja":
            return (names.ja ?? names.en ?? "") as String
        case "ko":
            return (names.ko ?? names.en ?? "") as String
        default:
            return (names.en ?? names.en ?? "") as String
        }
    }
    
    func getFacilityNameInMultipleLanguagesOnMapTap(from nameMap: MXMultilingualObject<NSString>, for languageCode: String) -> String {
        switch languageCode {
        case "zh-Hant":
            return (nameMap.zh_Hant ?? nameMap.en ?? "") as String
        case "zh-Hans":
            return (nameMap.zh_Hans ?? nameMap.en ?? "") as String
        case "zh-Hant-TW":
            return (nameMap.zh_Hant_TW ?? nameMap.en ?? "") as String
        case "ja":
            return (nameMap.ja ?? nameMap.en ?? "") as String
        case "ko":
            return (nameMap.ko ?? nameMap.en ?? "") as String
        default:
            return (nameMap.en ?? "") as String
        }
    }
    
    func updateMapLanguage(to languageCode: String) {
        self.selectedLanguage = languageCode
        
        guard let map = self.mapxusMap else {
            print("⏳ Map not ready. Language saved.")
            return
        }

        map.setMapLanguage(languageCode)
        
        // 2. Update Underlying Base Map (e.g., Mapbox Style)
        // Most Mapxus implementations use a Mapbox map underneath
        if let mapView = self.mapView {
            mapView.style?.localizeLabels(into: Locale(identifier: languageCode))
        }
        
        print("🌍 Map language updated live to: \(languageCode)")
    }
    
    func getVisibleViewController(_ rootVC: UIViewController) -> UIViewController {
        if let presented = rootVC.presentedViewController {
            return getVisibleViewController(presented)
        }
        if let nav = rootVC as? UINavigationController {
            return getVisibleViewController(nav.visibleViewController ?? nav)
        }
        if let tab = rootVC as? UITabBarController {
            return getVisibleViewController(tab.selectedViewController ?? tab)
        }
        return rootVC
    }
    
}

/// Old codes
struct MyMapxus: UIViewRepresentable {
    @ObservedObject var controller: MapxusController

    func makeUIView(context: Context) -> MGLMapView {
        let mapView = MGLMapView()
        let config = MXMConfiguration()
        let mapxusMap = MapxusMap.init(mapView: mapView, configuration: config)
        let buildingSearcher = MXMBuildingSearch.init()
        let poiSearchers = MXMPoiSearch.init()
        let poiCategorySearcher = MXMCategorySearch.init()
        let floorSearcher = MXMFloorInfo.init()
        let routePainter = MXMRoutePainter(mapView: mapView)
        
        let getUserLocationByFloorId = mapxusMap.userLocationFloor?.floorId
        let getUserLocationByBuilding = mapxusMap.userLocationBuilding?.venueId
        let getUserLocationByVenue = mapxusMap.userLocationVenue?.category
        
        mapView.delegate = controller
        config.defaultStyle = .MAPXUS
        
        mapView.showsUserLocation = true
        mapView.showsUserHeadingIndicator = true
//        mapView.style?.localizeLabels(into: Locale(identifier: controller.selectedLanguage))
//        mapView.layoutMargins = UIEdgeInsets(top: 0, left: 0, bottom: 180, right: 0)
        mapView.setContentInset(UIEdgeInsets(top: 0, left: 0, bottom: 180, right: 0), animated: true, completionHandler: {
            
        })
        
        mapxusMap.mapxusLogoEnabled = false
        mapxusMap.autoChangeBuilding = true
        mapxusMap.selectFloor(byId: getUserLocationByFloorId == nil ? "11cb3dd6af214a3e9cba6fd4718b145d" : getUserLocationByFloorId)
        mapxusMap.selectBuilding(byId: "996debebfddb4bc2895cdbeb70161d5a")
        mapxusMap.selectVenue(byId: "2506d124f4d049fb8b5019ed9d78c309")
//        mapxusMap.setMapLanguage(controller.selectedLanguage)
        
        // ... (start of Floor Bar setup) ...
        DispatchQueue.main.async {
            let bar = mapxusMap.floorBar
            bar.cornerRadius = 16
            bar.selectBoxColor = UIColor(Color.mainColor)
            bar.translatesAutoresizingMaskIntoConstraints = false
            
            // 1. Clear ALL existing constraints to stop the "Sticking" behavior
            bar.superview?.constraints.forEach { constraint in
                if constraint.firstItem as? UIView == bar {
                    constraint.isActive = false
                }
            }

            // 2. Apply New Constraints
            NSLayoutConstraint.activate([
                bar.topAnchor.constraint(equalTo: mapView.safeAreaLayoutGuide.topAnchor, constant: 16),
                bar.leadingAnchor.constraint(equalTo: mapView.safeAreaLayoutGuide.leadingAnchor, constant: 8),
                bar.widthAnchor.constraint(equalToConstant: 50)
            ])
            
            bar.setNeedsLayout()
            bar.layoutIfNeeded()
            
            if #available(iOS 11.0, *) {
                bar.insetsLayoutMarginsFromSafeArea = true
            }

            bar.layer.shadowOpacity = 1
            bar.layer.shadowColor = UIColor.gray.withAlphaComponent(0.3).cgColor
            bar.layer.shadowOffset = .zero
            bar.layer.shadowRadius = 3
        }
        // ... (rest of Floor Bar setup) ...
        
        // ... (Controller setup remains the same) ...
        controller.mapView = mapView
        controller.mapxusMap = mapxusMap
        controller.mapxusMap?.delegate = controller
        controller.routePainter = routePainter
        controller.routeSearcher = MXMRouteSearch()
        controller.routeSearcher?.delegate = controller
        
        controller.poiSearcher = poiSearchers
        controller.poiSearcher?.delegate = controller
        
        controller.poiCategorySearcher = poiCategorySearcher
        controller.poiCategorySearcher?.delegate = controller
        
        controller.buildingSearcher = buildingSearcher
        controller.buildingSearcher?.delegate = controller
        
        controller.floorSearcher = floorSearcher
        
        mapxusMap.delegate = controller

        return mapView
    }

    func updateUIView(_ uiView: MGLMapView, context: Context) {
        // No changes needed here.
    }
}
