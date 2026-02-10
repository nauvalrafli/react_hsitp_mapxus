//
//  AllViewReceiver.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 19/01/26.
//

import SwiftUI
import Combine
import Foundation

class AllViewReceiver: ObservableObject {
    @Published var isEndingTheARNavigationAndTheMapxusMapNavigation: Bool = false
    @Published var isShowingAnAlertDialog: Bool = false
    @Published var isFillingAnAlertDialogTitle: String = ""
    @Published var isFillingAnAlertDialogTextAction: String = ""
    @Published var isFillingAnAlertDialogTextCancel: String = ""
    
    @Published var isShowingACustomToastWashroom: Bool = false
    @Published var isShowingACustomToastMessageWashroom: String = ""
    @Published var isShowingACustomToastIconWashroom: String = ""
    @Published var isShowingACustomToastIconColorWashroom: Color = Color.mainColor
    @Published var isShowingACustomToastAlignmentWashroom: Alignment = Alignment.top
    
    @Published var isShowingACustomToastInternet: Bool = false
    @Published var isShowingACustomToastMessageInternet: String = ""
    @Published var isShowingACustomToastIconInternet: String = ""
    @Published var isShowingACustomToastIconColorInternet: Color = Color.mainColor
    @Published var isShowingACustomToastAlignmentInternet: Alignment = Alignment.top
    
    @Published var isEndingTheARNavigationAndTheMapxusMapNavigationAction: () -> Void = { }
    /// Correct way to create a Singleton
    static let shared = AllViewReceiver()
    
    @MainActor
    func showInternetToast(message: String, icon: String) {
        isShowingACustomToastInternet = true
        isShowingACustomToastMessageInternet = message
        isShowingACustomToastIconInternet = icon
        isShowingACustomToastAlignmentInternet = .bottom
    }
    
    func showWashroomOccupancyToast(message: String, icon: String, show: Bool) {
        isShowingACustomToastWashroom = show
        isShowingACustomToastMessageWashroom = message
        isShowingACustomToastIconWashroom = icon
        isShowingACustomToastAlignmentWashroom = .bottom
    }
    
}
