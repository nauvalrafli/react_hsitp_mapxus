//
//  BuildingFacilityData.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 15/01/26.
//

import SwiftUI
import CoreLocation

struct BuildingLists: Identifiable {
    var id: String
    var venueId: String
    var buildingName: String
    var buildingNumber: String
}

protocol FacilityProtocol {
    var id: String { get }
    var buildingId: String  { get }
    var buildingNumber: String  { get }
    var facilityName: String  { get }
    var floorName: String  { get }
    var floorId: String  { get }
    var category: [String] { get }
    var categoryCode: [String: [String]] { get }
    var lat: Double { get }
    var lon: Double { get }
    var iconName: String { get }
}

struct BuildingFacilityData: Identifiable, Equatable, Hashable {
    var id: String
    var buildingId: String
    var buildingNumber: String
    var facilityName: String
    var floorName: String
    var floorId: String
    var category: [String]
    var categoryCode: [String: [String]]
    var lat: Double
    var lon: Double
    
    var iconName: String {
        let name = facilityName.lowercased()
//        let cat = category.lowercased()
        let rawCodes = categoryCode.values.joined()
        
        // 1. High Priority: Check raw technical codes first (e.g., Lab)
        for code in rawCodes {
            if code.lowercased().contains("restaurants.herbal_tea") { return "herbal_tea_4" }
            if code.lowercased().contains("restaurants.western") { return "western_food_3" }
            if code.lowercased().contains("restaurants.korean") { return "korean_food_3" }
            
            if code.lowercased().contains("laundry_services") { return "laundry_1" }
            if code.lowercased().contains("tactile_map") { return "utilities" }
            if code.lowercased().contains("mothersroom") { return "mothers_room_2" }
            if code.lowercased().contains("defibrillator") { return "defibrillator" }
            if code.lowercased().contains("couriers") { return "couriers" }
            
            if code.lowercased().contains("pantry") { return "cup_of_coffee_4_fill" }
            if code.lowercased().contains("meeting_room") { return "meeting_room_3" }
            if code.lowercased().contains("function_room") { return "function_room_2" }
            if code.lowercased().contains("attractions") { return "attractions_1" }
            if code.lowercased().contains("information") { return "information" }
            if code.lowercased().contains("reception_desk") { return "information" }
            if code.lowercased().contains("workplace.lab") { return "lab_3" }
            if code.lowercased().contains("office") { return "office_1" }
            
            if code.lowercased().contains("restroom.disable") { return "disabled_toilet" }
            if code.lowercased().contains("restroom.male") { return "male-toilet" }
            if code.lowercased().contains("restroom.female") { return "female_toilet_1" }
            if code.lowercased().contains("shower") { return "shower_head_2" }
            
            if code.lowercased().contains("convenience") { return "convenience_1" }
        }
        
        return "default_facility_category"
    }
}

struct AllBuildingFacilityData: Identifiable, Equatable, Hashable {
    var id: String
    var buildingId: String
    var buildingNumber: String
    var facilityName: String
    var floorName: String
    var floorId: String
    var category: [String]
    var categoryCode: [String: [String]]
    var lat: Double
    var lon: Double
    
    var iconName: String {
        let name = facilityName.lowercased()
//        let cat = category.lowercased()
        let rawCodes = categoryCode.values.joined()
        
        // 1. High Priority: Check raw technical codes first (e.g., Lab)
        for code in rawCodes {
            if code.lowercased().contains("restaurants.herbal_tea") { return "herbal_tea_4" }
            if code.lowercased().contains("restaurants.western") { return "western_food_3" }
            if code.lowercased().contains("restaurants.korean") { return "korean_food_3" }
            
            if code.lowercased().contains("laundry_services") { return "laundry_1" }
            if code.lowercased().contains("tactile_map") { return "utilities" }
            if code.lowercased().contains("mothersroom") { return "mothers_room_2" }
            if code.lowercased().contains("defibrillator") { return "defibrillator" }
            if code.lowercased().contains("couriers") { return "couriers" }
            
            if code.lowercased().contains("pantry") { return "cup_of_coffee_4_fill" }
            if code.lowercased().contains("meeting_room") { return "meeting_room_3" }
            if code.lowercased().contains("function_room") { return "function_room_2" }
            if code.lowercased().contains("attractions") { return "attractions_1" }
            if code.lowercased().contains("information") { return "information" }
            if code.lowercased().contains("reception_desk") { return "information" }
            if code.lowercased().contains("workplace.lab") { return "lab_3" }
            if code.lowercased().contains("office") { return "office_1" }
            
            if code.lowercased().contains("restroom.disable") { return "disabled_toilet" }
            if code.lowercased().contains("restroom.male") { return "male-toilet" }
            if code.lowercased().contains("restroom.female") { return "female_toilet_1" }
            if code.lowercased().contains("shower") { return "shower_head_2" }
            
            if code.lowercased().contains("convenience") { return "convenience_1" }
        }
        
        return "default_facility_category"
    }
}

extension BuildingFacilityData: FacilityProtocol {}
extension AllBuildingFacilityData: FacilityProtocol {}
