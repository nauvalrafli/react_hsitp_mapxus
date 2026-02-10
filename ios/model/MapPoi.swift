//
//  MapPoi.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 16/05/25.
//


import Foundation
import CoreLocation

struct MapPoi: Identifiable, Hashable {
    let id: String
    let lat: Double
    let lng: Double
    let facilityName: String
    let floorId: String
    let floorName: String
    var buildingNumber: String = String()
    var category: String = String()

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }
}
