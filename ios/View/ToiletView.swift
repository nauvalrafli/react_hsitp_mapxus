//
//  ToiletView.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 12/05/25.
//

import SwiftUI
import Flow

struct ToiletView : View {
    @State private var showMap = false
    @State private var selectedPOI: MapPoi?
    
    var body: some View {
        ScrollView(.vertical, showsIndicators: false, content: {
            VStack(content: {
                ToiletListView()
            })
            .padding(.horizontal, 16)
        })
        .navigationTitle("Toilet List")
        .navigationBarTitleDisplayMode(.large)
    }
    
    func getToiletInfo(for type: String) -> (String, String) {
        switch type {
        case "femaleToilet":
            return ("figure.stand.dress", "Female Toilet")
        case "maleToilet":
            return ("figure.stand", "Male Toilet")
        case "accessibleToilet":
            return ("figure.roll", "Accessible Toilet")
        default:
            return ("figure.stand", "Toilet")
        }
    }
}

extension ToiletView {
    @ViewBuilder
    func ToiletListView() -> some View {
        ForEach(buildingFacilityLists, id: \.id, content: { toilet in
            let (iconName, label) = getToiletInfo(for: toilet.facilityName)
//            NavigationLink(destination: MapView(mapxusController: MapxusController(poi: toilet)), label: {
//                VStack(alignment: .leading, spacing: 8, content: {
//                    HStack(spacing: 16, content: {
//                        VStack(alignment: .leading, spacing: 8, content: {
//                            Text(label)
//                                .font(.system(size: 24, weight: .semibold))
//                                .foregroundColor(Color.primary)
//                            
//                            Text(toilet.floorName)
//                                .font(.system(size: 10, weight: .light))
//                                .foregroundColor(Color.secondary)
//                        })
//                        
//                        Spacer()
//                        
//                        VStack(content: {
//                            Image(systemName: iconName)
//                                .resizable()
//                                .scaledToFit()
//                                .frame(height: 60)
//                                .foregroundColor(.primary)
//                                .padding(16)
//                        })
//                        .frame(width: 80, height: 80)
//                        .background(.ultraThinMaterial)
//                        .cornerRadius(18)
//                    })
//                })
//                .padding(16)
//                .frame(minWidth: 0, maxWidth: .infinity, minHeight: 100, alignment: .center)
//                .background(.ultraThinMaterial)
//                .cornerRadius(34)
//            })
        })
    }
}
