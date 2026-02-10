//
//  HomeMenu.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 08/05/25.
//

import SwiftUI

struct HomeMenu: Identifiable {
    let id = UUID()           // Unique identifier for use in lists
    let destination: AnyView // Use type-erased view for flexibility
    let title: String
    let icon: String // Assume SF Symbol name or image asset name
    let backgroundImage: String
}

