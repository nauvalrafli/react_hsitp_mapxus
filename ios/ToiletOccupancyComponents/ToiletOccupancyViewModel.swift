//
//  Untitled.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 29/01/26.
//

import SwiftUI
import Foundation

class UserClass: ObservableObject {
    static let shared = UserClass()
    @Published var values: [[String]]?
    @Published var foundAllBuildingsDeviceIdsMap: [String: [String]] = [:]
    @Published var foundSpecificBuildingDeviceIdsMap: [String: [String]] = [:]
    @Published var foundWashroomNameMap: [String: [String]] = [:]
    
    // 1. Column Mapping for A1:L1000
    private enum Col: Int {
        case key1 = 0, deviceId = 1, building = 2, floor = 3, toilet = 4
        case mapxusId = 5, mapxusType = 6, mapxusBldId = 7, mapxusFloorId = 8
    }
    
    private init() {}
    
    // Example of how to populate it
    @MainActor
    func loadData(newRows: [[String]]) {
        // 1. Assign the data
        self.values = newRows
        
        print("--- Occupancy UserClass Data Load Success ---")
        print("Occupancy Total Rows Loaded: \(newRows.count)")
        
        // Loop through all rows to see what is inside
        for (index, row) in newRows.enumerated() {
//            print("Occupancy Row \(index + 1) Count: \(row.count) columns")
//            print("Occupancy Row \(index + 1) Content: \(row)")
            
            // Check if the row is long enough for your filters (e.g., Index 7)
            if row.count <= 7 {
//                print("⚠️ Warning: Row \(index + 1) is too short for Building/Floor filters!")
            }
        }
    }
    // 1. Get all device IDs for a specific building
    
    func getDevicesFromBuildingId(buildingId: String?) -> [String] {
        guard let rows = values?.dropFirst(), let bId = buildingId else { return [] }
        
        return rows.filter { row in
            isValid(row, at: .mapxusBldId) && row[Col.mapxusBldId.rawValue] == bId
        }.map { $0[Col.deviceId.rawValue] }
    }

    func getDeviceIds(buildingId: String, floorId: String, mapxusId: String) -> [String] {
        guard let rows = values?.dropFirst() else { return [] }
        
        let filtered = rows.filter { row in
            guard row.count > Col.mapxusFloorId.rawValue else { return false }
            return row[Col.mapxusBldId.rawValue] == buildingId &&
                   row[Col.mapxusFloorId.rawValue] == floorId &&
                    row[Col.mapxusId.rawValue] == mapxusId
        }.map { $0[Col.deviceId.rawValue] }
        
        print("Occupancy DEBUG: Found \(filtered.count) device(s): \(filtered)")
        
        return filtered
    }
    
    func getDevicesMappingFromBuildingId(buildingId: String?) -> [String: [String]] {
        guard let rows = values?.dropFirst(), let bId = buildingId else {
            DispatchQueue.main.async { self.foundSpecificBuildingDeviceIdsMap = [:] }
            return [:]
        }
        
        let filteredRows = rows.filter {
            isValid($0, at: .mapxusBldId) && $0[Col.mapxusBldId.rawValue] == bId
        }
        
        let mapping = Dictionary(grouping: filteredRows) { row in
            row.count > Col.mapxusId.rawValue ? row[Col.mapxusId.rawValue] : "Unknown"
        }.mapValues { rows in
            rows.map { $0[Col.deviceId.rawValue] }
        }

        // 3. Update the @Published property on the main thread
        DispatchQueue.main.async {
            self.foundSpecificBuildingDeviceIdsMap = mapping
        }
        
        return mapping
    }
    
    func getDevicesMappingFromAllBuildings() -> [String: [String]] {
        guard let rows = values?.dropFirst() else {
            print("❌ Mapping Error: No rows found in spreadsheet values.")
            return [:]
        }
        
        let mapping = Dictionary(grouping: rows) { row in
            let rawId = row.count > Col.mapxusId.rawValue ? row[Col.mapxusId.rawValue] : "Unknown"
            return rawId.trimmingCharacters(in: .whitespacesAndNewlines)
        }.mapValues { rows in
            rows.map { $0[Col.deviceId.rawValue].trimmingCharacters(in: .whitespacesAndNewlines) }
        }

        // 📝 DEBUG PRINTS
        print("📂 Mapping Complete: Processed \(mapping.count) unique POIs from spreadsheet.")
        
        if let devicesForTarget = mapping["15672319"] {
            print("✅ Mapping Found target POI 15668271 in mapping with \(devicesForTarget.count) devices.")
        } else {
            print("⚠️ POI 15668271 is STILL missing from mapping. Check if column \(Col.mapxusId.rawValue) contains this exact string.")
        }

        DispatchQueue.main.async {
            self.foundAllBuildingsDeviceIdsMap = mapping
        }
        
        return mapping
    }

    // Helper to prevent out-of-bounds crashes
    private func isValid(_ row: [String], at column: Col) -> Bool {
        return row.count > column.rawValue
    }
    
    // MARK: - Sync & Debug
    func syncSpreadsheet() async {
        do {
            let rawData = try await ApiClient.shared.getGoogleSheetData()
            await loadData(newRows: rawData)
        } catch {
            print("❌ Sync error: \(error.localizedDescription)")
        }
    }
}

@MainActor
class TelemetryViewModel: ObservableObject {
    @Published var tokenResponse: TokenResponse?
    @Published var timeseriesData: DeviceTimeseriesResponse?
    @Published var errorMessage: String?
    @Published var isLoading = false
    @Published var isLoadingOnWashroomOccupancy = false
    
    @Published var deviceStatusBatch: [String: [TimeseriesEntry]] = [:]
    
    var accessToken: String?
    private var fetchTask: Task<Void, Never>?
    
    private var lastTokenRefresh: Date?
    
    // 1. Ensure you have an instance of your data
    let userClass = UserClass.shared
    let translationClass: TranslationClass = TranslationClass()
    let allViewReceiver: AllViewReceiver = AllViewReceiver.shared
    
    @AppStorage("Mapxus-Map-Language") private var selectedLanguage: String = "en"
    
    // Remove Task { } and make it async
    func requestAuthToken() async {
        isLoading = true
        do {
            let response = try await ApiClient.shared.getToken(
                grantType: "client_credentials",
                clientId: "graviteeGW",
                clientSecret: "IDLecASjFsi06msaVqX3C3XoKGqtGbfz"
            )
            // Ensure UI updates are on MainActor if this class isn't already isolated
            await MainActor.run {
                self.accessToken = response.accessToken
                self.tokenResponse = response
                self.isLoading = false
                print(" Occupancy token: \(accessToken ?? "nil")")
            }
        } catch {
            await MainActor.run {
                print(" Occupancy token error: \(error.localizedDescription)")
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func getToiletStatus(buildingId: String?, isRepeating: Binding<Bool>) {
        fetchTask?.cancel()
        isLoading = true
        
        fetchTask = Task {
            do {
                // Use a while loop controlled by the binding and cancellation status
                while isRepeating.wrappedValue && !Task.isCancelled {
                    
                    // 1. Token Refresh Logic
                    let needsRefresh = lastTokenRefresh == nil || Date().timeIntervalSince(lastTokenRefresh!) >= 300
                    if (self.accessToken ?? "").isEmpty || needsRefresh {
                        await requestAuthToken()
                        self.lastTokenRefresh = Date()
                    }
                    
                    // 2. Get the Dictionary Mapping [MapxusId: [DeviceId]]
                    let mapping = userClass.getDevicesMappingFromBuildingId(buildingId: buildingId)
                    
                    // 3. FIX: Pass 'mapping' dictionary to fix the type error
                    if let token = self.accessToken, !mapping.isEmpty {
                        let results = try await self.fetchDeviceStatusesBatch(mapping: mapping, token: token)
                        
                        await MainActor.run {
                            self.deviceStatusBatch = results
                            self.isLoading = false
                        }
                    } else {
                        print("⚠️ Occupancy: Skipping fetch - Mapping empty or token missing.")
                    }
                    
                    // 4. Delay for 30 seconds before the next loop iteration
                    print("⏳ Occupancy: Sleeping for 30 seconds...")
                    try await Task.sleep(for: .seconds(30))
                }
            } catch is CancellationError {
                print("🛑 Occupancy: Task was cancelled.")
            } catch {
                print("💥 Occupancy error: \(error.localizedDescription)")
                await MainActor.run { self.isLoading = false }
            }
        }
    }
    
//    func getToiletStatusWithoutAutomaticRefresh(buildingId: String?) async {
//        // 1. Immediately cancel any fetch currently in progress
//        fetchTask?.cancel()
//        
//        isLoading = true
//        
//        // 2. Start a new task
//        fetchTask = Task { @MainActor in
//            do {
//                // Check for cancellation after every major step
//                try Task.checkCancellation()
//
//                // Token refresh logic
//                let needsRefresh = lastTokenRefresh == nil || Date().timeIntervalSince(lastTokenRefresh!) >= 300
//                if (self.accessToken ?? "").isEmpty || needsRefresh {
//                    await requestAuthToken()
//                    self.lastTokenRefresh = Date()
//                }
//                
//                try Task.checkCancellation()
//                
//                let mapping = userClass.getDevicesMappingFromBuildingId(buildingId: buildingId)
//                guard let token = self.accessToken, !mapping.isEmpty else {
//                    self.allViewReceiver.showWashroomOccupancyToast(
//                        message: translationClass.washroomOccupancyCouldntRefresh(code: selectedLanguage),
//                        icon: "xmark.circle.fill",
//                        iconColor: Color.red,
//                        show: true
//                    )
//                    isLoading = false
//                    return
//                }
//                
//                // 3. Perform the fetch
//                let results = try await self.fetchDeviceStatusesBatch(mapping: mapping, token: token)
//                
//                // Final check: if the user clicked again while we were waiting,
//                // don't save this "old" data.
//                try Task.checkCancellation()
//                
//                self.deviceStatusBatch = results
//                self.isLoading = false
//                print("✅ Occupancy: Refresh successful.")
//                
//            } catch is CancellationError {
//                print("🔄 Occupancy: Previous request cancelled by new click.")
//                // Do NOT set isLoading = false here if you want the spinner to stay
//                // active while the NEW task starts.
//            } catch {
//                self.allViewReceiver.showWashroomOccupancyToast(message: translationClass.washroomOccupancyFailedToRefresh(code: selectedLanguage), icon: "xmark.circle.fill", iconColor: Color.red, show: true)
//                print("💥 Occupancy Error: \(error.localizedDescription)")
//                self.isLoading = false
//            }
//        }
//    }
    
    func getToiletStatusWithoutAutomaticRefresh(buildingId: String?) async {
        // 1. Cancel previous if any
        fetchTask?.cancel()
        
        // We create a reference to the current task so it can be cancelled later if needed
        let currentTask: Task<Void, Never> = Task { @MainActor in
            // 1. Debounce: wait 0.3s before doing work
            try? await Task.sleep(nanoseconds: 300_000_000)
            
            // Use guard to check cancellation early
            if Task.isCancelled { return }
            
            isLoading = true
            defer { isLoading = false } // Ensures loading turns off even if code fails or returns early

            do {
                try Task.checkCancellation()

                // Token logic
                let needsRefresh = lastTokenRefresh == nil || Date().timeIntervalSince(lastTokenRefresh!) >= 300
                if (self.accessToken ?? "").isEmpty || needsRefresh {
                    await requestAuthToken()
                    self.lastTokenRefresh = Date()
                }
                
                try Task.checkCancellation()
                
                let mapping = userClass.getDevicesMappingFromBuildingId(buildingId: buildingId)
                guard let token = self.accessToken, !mapping.isEmpty else {
                    self.allViewReceiver.showWashroomOccupancyToast(
                        message: translationClass.washroomOccupancyCouldntRefresh(code: selectedLanguage),
                        icon: "xmark.circle.fill", iconColor: .red, show: true
                    )
                    return
                }
                
                let results = try await self.fetchDeviceStatusesBatch(mapping: mapping, token: token)
                
                try Task.checkCancellation()
                
                self.deviceStatusBatch = results
                print("✅ Occupancy: Refresh successful.")
                
            } catch is CancellationError {
                print("🔄 Occupancy: Task cancelled.")
            } catch {
                // 💡 THE FIX: Check if the error was actually a cancellation
                // that missed the first 'is CancellationError' check.
                if let urlError = error as? URLError, urlError.code == .cancelled {
                    print("🔄 Occupancy: URL Session cancelled.")
                    return
                }
                
                self.allViewReceiver.showWashroomOccupancyToast(
                    message: translationClass.washroomOccupancyFailedToRefresh(code: selectedLanguage),
                    icon: "xmark.circle.fill", iconColor: .red, show: true
                )
                print("💥 Occupancy Error: \(error.localizedDescription)")
            }
        }
        
        self.fetchTask = currentTask
        // 关键 (The Key): Wait for the task we just started to actually finish!
        _ = await currentTask.result
    }
    
//    func getToiletStatusWithoutAutomaticRefreshForAllBuildings() async {
//        // 1. Immediately cancel any fetch currently in progress
//        fetchTask?.cancel()
//        
//        isLoading = true
//        
//        // 2. Start a new task
//        fetchTask = Task { @MainActor in
//            do {
//                try Task.checkCancellation()
//
//                // Token refresh logic
//                let needsRefresh = lastTokenRefresh == nil || Date().timeIntervalSince(lastTokenRefresh!) >= 300
//                if (self.accessToken ?? "").isEmpty || needsRefresh {
//                    await requestAuthToken()
//                    self.lastTokenRefresh = Date()
//                }
//                
//                try Task.checkCancellation()
//                
//                // 🔥 MODIFIED: Get mapping for ALL buildings instead of just one ID
//                let mapping = userClass.getDevicesMappingFromAllBuildings()
//                
//                guard let token = self.accessToken, !mapping.isEmpty else {
//                    isLoading = false
//                    return
//                }
//                
//                // 3. Perform the fetch
//                let results = try await self.fetchDeviceStatusesBatch(mapping: mapping, token: token)
//                
//                try Task.checkCancellation()
//                
//                self.deviceStatusBatch = results
//                self.isLoading = false
//                print("✅ Global Occupancy: Refresh successful for all buildings.")
//                
//            } catch is CancellationError {
//                print("🔄 Occupancy: Previous request cancelled by new click.")
//            } catch {
//                self.allViewReceiver.showWashroomOccupancyToast(
//                    message: translationClass.washroomOccupancyFailedToRefresh(code: selectedLanguage),
//                    icon: "xmark.circle.fill",
//                    iconColor: Color.red,
//                    show: true
//                )
//                print("💥 Global Occupancy Error: \(error.localizedDescription)")
//                self.isLoading = false
//            }
//        }
//    }
    
    func getToiletStatusWithoutAutomaticRefreshForAllBuildings() async {
        // 1. Cancel previous if any
        fetchTask?.cancel()
        
        // 2. Create the task locally first to resolve "No exact matches" error
        let newTask: Task<Void, Never> = Task { @MainActor in
            // Debounce: Wait 0.3s. If the user scrolls/taps again quickly, this task gets cancelled.
            try? await Task.sleep(nanoseconds: 300_000_000)
            if Task.isCancelled { return }
            
            isLoading = true
            defer { isLoading = false } // Guarantees loading stops even on failure
            
            do {
                try Task.checkCancellation()

                // Token refresh logic (5-minute cache)
                let needsRefresh = lastTokenRefresh == nil || Date().timeIntervalSince(lastTokenRefresh!) >= 300
                if (self.accessToken ?? "").isEmpty || needsRefresh {
                    await requestAuthToken()
                    self.lastTokenRefresh = Date()
                }
                
                try Task.checkCancellation()
                
                // Get mapping for ALL buildings
                let mapping = userClass.getDevicesMappingFromAllBuildings()
                
                guard let token = self.accessToken, !mapping.isEmpty else {
                    // We don't show a toast here to avoid spamming the user if mapping is just empty
                    return
                }
                
                // Perform the batch fetch
                let results = try await self.fetchDeviceStatusesBatch(mapping: mapping, token: token)
                
                try Task.checkCancellation()
                
                self.deviceStatusBatch = results
                print("✅ Global Occupancy: Refresh successful for all buildings.")
                
            } catch is CancellationError {
                print("🔄 Global Occupancy: Task cancelled.")
            } catch {
                // THE FIX: Ignore URLError.cancelled so the error toast doesn't show during rapid taps
                if let urlError = error as? URLError, urlError.code == .cancelled {
                    print("🔄 Global Occupancy: URL Session cancelled.")
                    return
                }
                
                self.allViewReceiver.showWashroomOccupancyToast(
                    message: translationClass.washroomOccupancyFailedToRefresh(code: selectedLanguage),
                    icon: "xmark.circle.fill",
                    iconColor: .red,
                    show: true
                )
                print("💥 Global Occupancy Error: \(error.localizedDescription)")
            }
        }
        
        // 3. Assign to the class property and await the result
        self.fetchTask = newTask
        _ = await newTask.result
    }
    
    func toggleGetWashroomOccupancyBasedOnAllBuildingsOrASpecific(buildingId: String, allBuildings: Bool) async {
        if allBuildings {
            await self.getToiletStatusWithoutAutomaticRefreshForAllBuildings()
        } else {
            await self.getToiletStatusWithoutAutomaticRefresh(buildingId: buildingId)
        }
    }
    
    func isAnyDeviceVacant(deviceIds: [String]) -> Bool {
        for id in deviceIds {
            let key = "\(id)_Presence_Seneor_State"
            
            if let entries = deviceStatusBatch[key],
               let latestEntry = entries.first {
                // If we find even one that is vacant, return true immediately
                if latestEntry.value.lowercased() == "vacant" {
                    return false
                } else {
                    return true
                }
            }
        }
        
        // If the loop finishes and none were vacant, return false
        return false
    }
    
    func getSpecificRestroomOccupancyStatus(id: String, statuses: [String], languageCode: String) -> (message: String, isVacant: Bool) {
        // Check if the key contains the specific single ID string
        let presenceKeys = deviceStatusBatch.keys.filter { key in
            key.contains(id) && key.contains("Presence_Seneor_State")
        }
        
        let totalSensors = presenceKeys.count
        guard totalSensors > 0 else {
            return (translationClass.loading(code: languageCode), false)
        }
        
        let vacantCount = presenceKeys.filter { key in
            let val = deviceStatusBatch[key]?.first?.value.lowercased() ?? ""
            return val == "vacant"
        }.count
        
        let occupiedCount = totalSensors - vacantCount
        let occupancyRate = Double(occupiedCount) / Double(totalSensors)
        
        let message: String
        switch occupancyRate {
        case 0.0..<0.5:  message = statuses[2]
        case 0.5..<0.75: message = statuses[1]
        case 0.75...1.0: message = statuses[0]
        default:         message = ""
        }
        
        return (message, vacantCount > 0)
    }
    
//    Occupancy POI ID: 14204
//    Occupancy -> Device ID: 079166e0-11f0-9bdd-15329
//    Occupancy -> Device ID: 062294a0-11f0-9bdd-15329
//    Occupancy -> Device ID: 00c6a9b0-11f0-ac32-f12d4
//    Occupancy -> Device ID: f920cec0-11f0-ac32-f12d44
//    Occupancy -> Device ID: 0918a320-11f0-beac-a73db5
//    Occupancy POI ID: 14200
//    Occupancy -> Device ID: f3424650-11f0-ac32-f12d44
//    Occupancy -> Device ID: fb27e960-11f0-ac32-f12d44
//    Occupancy -> Device ID: 0d327120-11f0-9bdd-15329a
//    Occupancy -> Device ID: 0e3ae070-11f0-beac-a73db5
//    Occupancy -> Device ID: fcd57250-11f0-ac32-f12d44
//    Occupancy POI ID: 15523
//    Occupancy -> Device ID: 0869c580-11f0-9bdd-15329a
//    Occupancy -> Device ID: f7fc4bf0-11f0-beac-a73db5
//    Occupancy -> Device ID: 0f775810-11f0-9bdd-15329a
//    Occupancy -> Device ID: 02fa79a0-11f0-beac-a73db5
//    Occupancy POI ID: 14202
//    Occupancy -> Device ID: f876d320-11f0-beac-a73db5
//    Occupancy -> Device ID: 05a7bf50-11f0-9bdd-15329a
//    Occupancy -> Device ID: f82b7240-11f0-ac32-f12d44
//    Occupancy POI ID: 14204
//    Occupancy -> Device ID: 058b5db0-11f0-9bdd-15329a
//    Occupancy POI ID: 14219
//    Occupancy -> Device ID: 13e1b710-11f0-9bdd-15329a
//    Occupancy POI ID: 14204
//    Occupancy -> Device ID: f86a5000-11f0-ac32-f12d44
//    Occupancy -> Device ID: 02664ff0-11f0-beac-a73db5
//    Occupancy -> Device ID: ea4da440-11f0-ac32-f12d44
    
    func getRestroomOccupancyStatus(poiId: String, mapping: [String: [String]], statuses: [String], languageCode: String) -> (message: String, isVacant: Bool) {
        
        // 1. Look up the specific list of Device IDs for this POI
        guard let deviceIds = mapping[poiId], !deviceIds.isEmpty else {
            print("⚠️ POI \(poiId) not found in Excel mapping.")
            return (translationClass.loading(code: languageCode), false)
        }
        
        // 📝 PRINT POI ID
        print("Occupancy POI ID: \(poiId)")
        
        // 📝 PRINT EACH DEVICE ID
        for deviceId in deviceIds {
            // Find the specific key in the batch that contains this Device ID and the Presence state
            let statusKey = deviceStatusBatch.keys.first { $0.contains(deviceId) && $0.contains("Presence_Seneor_State") }
            
            // Get the value (vacant/occupied), defaulting to "Unknown" if not found
            let rawStatus = deviceStatusBatch[statusKey ?? ""]?.first?.value.lowercased() ?? "unknown"
            
            print("    Occupancy -> Device ID: \(deviceId) | Status: \(rawStatus)")
        }
        
        // 2. Find matching keys
        let presenceKeys = deviceStatusBatch.keys.filter { batchKey in
            deviceIds.contains { dId in batchKey.contains(dId) } &&
            batchKey.contains("Presence_Seneor_State")
        }
        
        let totalSensors = presenceKeys.count
        guard totalSensors > 0 else {
            print("⏳ No sensor data in batch for POI \(poiId). Total sensors found: 0")
            return (translationClass.loading(code: languageCode), false)
        }
        
        // 3. Count vacancy
        let vacantCount = presenceKeys.filter { key in
            let val = deviceStatusBatch[key]?.first?.value.lowercased() ?? ""
            return val == "vacant"
        }.count
        
        // 4. Calculate occupancy rate
        let occupiedCount = totalSensors - vacantCount
        let occupancyRate = Double(occupiedCount) / Double(totalSensors)
        
        print("📊 Rate: \(String(format: "%.2f", occupancyRate * 100))% (Vacant: \(vacantCount)/\(totalSensors))")
        
        let message: String
        switch occupancyRate {
        case 0.0..<0.5:  message = statuses[1] // Available
        case 0.5..<0.75: message = statuses[2] // Almost Full
        case 0.75...1.0: message = statuses[0] // Full
        default:         message = ""
        }
        
        return (message, vacantCount > 0)
    }
    
    // Remove 'mapping' from parameters to ensure we always use the latest global data
    func getRestroomOccupancyStatusAll(poiId: String, statuses: [String], languageCode: String, allBuildings: Binding<Bool>) -> (message: String, isVacant: Bool) {
        // 1. Always grab the freshest mapping from the source of truth
        // 1. Declare the variable outside so it is accessible to the whole function
        var currentMapping: [String: [String]]
        
        if allBuildings.wrappedValue {
            currentMapping = userClass.foundAllBuildingsDeviceIdsMap
        } else {
            currentMapping = userClass.foundSpecificBuildingDeviceIdsMap
        }
        
        // Check if mapping is even loaded yet
        if currentMapping.isEmpty {
            print("⏳ Waiting for mapping to initialize...")
            return (translationClass.loading(code: languageCode), false)
        }
        
        // 2. Look up the POI ID
        guard let deviceIds = currentMapping[poiId], !deviceIds.isEmpty else {
            print("⚠️ POI \(poiId) not found in mapping. Available keys: \(currentMapping.keys.count)")
            return (translationClass.loading(code: languageCode), false)
        }
        
        print("Occupancy POI ID: \(poiId)")
        
        // ... rest of your printing and calculation logic remains the same ...
        // 📝 PRINT EACH DEVICE ID
        for deviceId in deviceIds {
            // Find the specific key in the batch that contains this Device ID and the Presence state
            let statusKey = deviceStatusBatch.keys.first { $0.contains(deviceId) && $0.contains("Presence_Seneor_State") }
            
            // Get the value (vacant/occupied), defaulting to "Unknown" if not found
            let rawStatus = deviceStatusBatch[statusKey ?? ""]?.first?.value.lowercased() ?? "unknown"
            
            print("    Occupancy -> Device ID: \(deviceId) | Status: \(rawStatus)")
        }
        
        // 3. Find matching keys in the latest batch
        let presenceKeys = deviceStatusBatch.keys.filter { batchKey in
            deviceIds.contains { dId in batchKey.contains(dId) } &&
            batchKey.contains("Presence_Seneor_State")
        }
        
        let totalSensors = presenceKeys.count
        guard totalSensors > 0 else {
            print("⏳ No sensor data for \(poiId). Ensure fetchDeviceStatusesBatch finished.")
            return (translationClass.loading(code: languageCode), false)
        }
        
        let vacantCount = presenceKeys.filter { key in
            let val = deviceStatusBatch[key]?.first?.value.lowercased() ?? ""
            return val == "vacant"
        }.count
        
        let occupancyRate = Double(totalSensors - vacantCount) / Double(totalSensors)
        print("📊 Rate: \(String(format: "%.2f", occupancyRate * 100))% (Vacant: \(vacantCount)/\(totalSensors))")
        
        let message: String
        switch occupancyRate {
        case 0.0..<0.5:  message = statuses[1]
        case 0.5..<0.75: message = statuses[2]
        case 0.75...1.0: message = statuses[0]
        default:         message = ""
        }
        
        return (message, vacantCount > 0)
    }
    
    private func fetchDeviceStatusesBatch(mapping: [String: [String]], token: String) async throws -> [String: [TimeseriesEntry]] {
        if mapping.isEmpty { return [:] }
        
        // Pass the mapping to the fetcher
        let mergedResults = try await ApiClient.shared.getDeviceTimeseries(mapping: mapping, token: token)
        return mergedResults
    }
}

class ApiClient {
    private let translationClass: TranslationClass = TranslationClass()
    private let allViewReceiver: AllViewReceiver = AllViewReceiver.shared
    
    @AppStorage("Mapxus-Map-Language") private var selectedLanguage: String = "en"
    
    static let shared = ApiClient()
    private let baseUrl = "http://10.211.0.14" // Direct IP for stability
    private let hostname = "appapi-uat.hsitp.local"

    // Equivalent to getToken @FormUrlEncoded
    func getToken(grantType: String, clientId: String, clientSecret: String) async throws -> TokenResponse {
        let components = URLComponents(string: "\(baseUrl)/auth/realms/hsitp/protocol/openid-connect/token")!
        
        var request = URLRequest(url: components.url!)
        request.httpMethod = "POST"
        request.setValue(hostname, forHTTPHeaderField: "Host") // Essential for internal routing
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        let bodyParams = [
            "grant_type": grantType,
            "client_id": clientId,
            "client_secret": clientSecret
        ]
        
        request.httpBody = bodyParams
            .map { "\($0.key)=\($0.value)" }
            .joined(separator: "&")
            .data(using: .utf8)

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(TokenResponse.self, from: data)
    }
    
    func getDeviceTimeseries(mapping: [String: [String]], token: String) async throws -> [String: [TimeseriesEntry]] {
        // We use a TaskGroup to run all network requests in parallel
        return try await withThrowingTaskGroup(of: [String: [TimeseriesEntry]].self) { group in
            for (mapxusId, deviceIds) in mapping {
                for dId in deviceIds {
                    let cleanId = dId.trimmingCharacters(in: .whitespacesAndNewlines)
                    
                    // Add a child task for each device
                    group.addTask {
                        var localResults: [String: [TimeseriesEntry]] = [:]
                        let urlString = "\(self.baseUrl)/telemetry/plugins/telemetry/DEVICE/\(cleanId)/values/timeseries?useStrictDataTypes=false"
                        
                        guard let url = URL(string: urlString) else { return [:] }
                        var request = URLRequest(url: url)
                        request.setValue(self.hostname, forHTTPHeaderField: "Host")
                        request.addValue("application/json", forHTTPHeaderField: "Accept")
                        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

                        let (data, response) = try await URLSession.shared.data(for: request)
                        
                        if (response as? HTTPURLResponse)?.statusCode == 200 {
                            // 1. Print raw JSON String (Helpful for debugging structure)
                            if let jsonString = String(data: data, encoding: .utf8) {
                                print("📥 Raw Timeseries Data for \(cleanId): \(jsonString)")
                            }
                            
                            let decoded = try JSONDecoder().decode(DeviceTimeseriesResponse.self, from: data)
                            print("📦 Decoded Object: \(decoded)")
                            for (key, entries) in decoded {
                                let compositeKey = "\(mapxusId)_\(cleanId)_\(key)"
                                print("occupancy status: \(compositeKey)")
                                localResults[compositeKey] = entries
                            }
                        } else {
                            
                        }
                        return localResults
                    }
                }
            }

            // Collect and merge results as they finish
            var allResults: [String: [TimeseriesEntry]] = [:]
            for try await partialResult in group {
                allResults.merge(partialResult) { (current, _) in current }
            }
            
            await MainActor.run {
                self.allViewReceiver.showWashroomOccupancyToast(message: translationClass.washroomOccupancyRefreshedSuccessfully(code: selectedLanguage), icon: "checkmark.circle.fill", iconColor: Color.mainColor, show: true)
            }
            
            print("✅ Parallel Fetch Complete: \(allResults.count) entries gathered.")
            return allResults
        }
    }
    
    /// Key: Presence_Seneor_State | Status: None
    /// Key: Presence_Seneor_State | Status: Vacant
    /// Key: Presence_Seneor_State | Status: Occupancy
    
    func getGoogleSheetData() async throws -> [[String]] {
        let spreadsheetId = "1emNilMsRU2qOzm4HDyN9667froUUkKjUbDoespsnNxM"
        let apiKey = "AIzaSyDiIWV4V6OfgdIPrncAnBbjW-yD82SUoqE"
        let range = "Sheet1!A:L"
        
        let urlString = "https://sheets.googleapis.com/v4/spreadsheets/\(spreadsheetId)/values/\(range)?key=\(apiKey)"
        
        guard let url = URL(string: urlString) else { throw URLError(.badURL) }
        
        let (data, response) = try await URLSession.shared.data(for: URLRequest(url: url))

        // 1. Check HTTP Status
        if let httpResponse = response as? HTTPURLResponse {
            print("HTTP Status Code: \(httpResponse.statusCode)")
            // 403 = API Key error, 404 = Spreadsheet ID error
        }

        // 2. Print the raw data count
        print("Bytes received: \(data.count)")

        // 3. Try to print string
        if let jsonString = String(data: data, encoding: .utf8), !jsonString.isEmpty {
            print("RAW GOOGLE RESPONSE: \(jsonString)")
        } else {
            print("❌ RAW GOOGLE RESPONSE is empty or not UTF8")
        }
        
        let decoded = try JSONDecoder().decode(GoogleSheetsResponse.self, from: data)
        return decoded.values ?? []
    }
}

// Helper struct for decoding Google Sheets JSON
struct GoogleSheetsResponse: Codable {
    let values: [[String]]?
}
