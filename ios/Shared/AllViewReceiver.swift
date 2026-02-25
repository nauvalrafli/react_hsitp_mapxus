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
    func showInternetToast(message: String, icon: String, iconColor: Color) {
        self.isShowingACustomToastInternet = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05, execute: {
            withAnimation(.smooth(), {
                self.isShowingACustomToastInternet = true
                self.isShowingACustomToastMessageInternet = message
                self.isShowingACustomToastIconInternet = icon
                self.isShowingACustomToastIconColorInternet = iconColor
                self.isShowingACustomToastAlignmentInternet = .bottom
            })
        })
    }
    
    func showWashroomOccupancyToast(message: String, icon: String, iconColor: Color, show: Bool) {
        self.isShowingACustomToastWashroom = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05, execute: {
            withAnimation(.smooth(), {
                self.isShowingACustomToastWashroom = show
                self.isShowingACustomToastMessageWashroom = message
                self.isShowingACustomToastIconWashroom = icon
                self.isShowingACustomToastIconColorWashroom = iconColor
                self.isShowingACustomToastAlignmentWashroom = .bottom
            })
        })
    }
    
}
