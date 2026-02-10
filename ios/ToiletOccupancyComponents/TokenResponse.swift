//
//  TokenResponse.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 29/01/26.
//

import SwiftUI

struct TokenResponse: Codable {
    let accessToken: String
    let expiresIn: Int
    let refreshExpiresIn: Int?
    let tokenType: String?
    let scope: String?
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case expiresIn = "expires_in"
        case refreshExpiresIn = "refresh_expires_in"
        case tokenType = "token_type"
        case scope
    }
}

// Each key (e.g., "LEC_Color") points to an array of these
struct TimeseriesEntry: Codable {
    let ts: Int64
    let value: String
}

typealias DeviceTimeseriesResponse = [String: [TimeseriesEntry]]
