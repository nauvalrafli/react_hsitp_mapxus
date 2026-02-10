//
//  HomeView.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 08/05/25.
//

import SwiftUI

struct HomeView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var allViewReceiver: AllViewReceiver = AllViewReceiver.shared
    
    var body: some View {
        VStack(content: {
            MapView()
        })
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    HomeView()
}
