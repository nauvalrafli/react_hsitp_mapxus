//
//  SettingsView.swift
//  mapxus-hsitp-ios
//
//  Created by Boxyguild on 6/12/25.
//

import SwiftUI
import CoreLocation

struct SettingsView: View {
    @StateObject var mapxusController: MapxusController
    @StateObject var translationClass: TranslationClass = TranslationClass()
//    @StateObject private var locationManagerClass: LocationManagerClass = LocationManagerClass()
    
    @AppStorage("ARNavigation-App-Enabling-Motion-Sensor") private var isEnablingMotionSensor: Bool = false
    @AppStorage("ARNavigation-App-AR-Visibility") private var isShowingARNavigationAllAtOnceOr: Bool = false
    @AppStorage("ARNavigation-App-AR-Visibility-Label") private var isShowingARNavigationAllAtOnceOrLabel: String = ""
    @AppStorage("ARNavigation-App-Enabling-Dark-Mode") private var isEnablingDarkMode: Bool = false
    @AppStorage("ARNavigation-App-Enabling-GPS-Location") private var isEnablingGPSLocation: Bool = false
    @AppStorage("ARNavigation-App-Enabling-TTS") private var isEnablingTTS: Bool = true
    @AppStorage("ARNavigation-App-Guide-Assistant-Language") private var isSelectingAssistantGuideLanguage: Bool = false
    @AppStorage("ARNavigation-App-Guide-Assistant-Language-Label") private var isSelectingAssistantGuideLanguageLabel: String = "English"
    @AppStorage("ARNavigation-App-Guide-Assistant-Language-AVSpeechSynthesisVoiceCode") private var isSelectingAssistantGuideLanguageAVSpeechSynthesisVoiceCode: String = "en-US"
    @AppStorage("MapxusMap-Introduction-Section") private var isShowingMapxusMapIntroductionSection: Bool = true
    
    @State private var isShowingNavigationPathDialog: Bool = false
    @State private var isShowingLanguageLists: Bool = false
    
    @State private var isLanguageChanges: Bool = false
    
    func languagePlaceholder(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "繁體中文 (香港)"
        case "zh-Hans":
            return "简体中文 (中国)"
        case "en":
            return "English"
        default:
            return "English"
        }
    }
    
    var body: some View {
        VStack(content: {
            List(content: {
//                Section(content: {
//                    ToggleButton(isOn: $isEnablingMotionSensor, label: "Motion Sensor")
//                        .onChange(of: isEnablingMotionSensor, { oldValue, newValue in
//                            isEnablingMotionSensor = newValue
//                        })
//                    SettingsListWithSelectingLabel(label: "Navigation Path", secondLabel: isShowingARNavigationAllAtOnceOrLabel, action: {
//                        isShowingNavigationPathDialog.toggle()
//                    })
//                }, header: {
//                    Text("Augmented Reality")
//                        .font(.system(size: 12, weight: .light))
//                        .textInputAutocapitalization(.words)
//                        .textCase(.none)
//                })
//                
//                Section(content: {
//                    ToggleButton(isOn: $isEnablingTTS, label: "Guide Assistant")
//                        .onChange(of: isEnablingTTS, { oldValue, newValue in
//                            withAnimation(.easeInOut(duration: 0.6), {
//                                isEnablingTTS = newValue
//                            })
//                        })
//                    SettingsListWithSelectingLabel(label: "Guide Language", secondLabel: isSelectingAssistantGuideLanguageLabel, action: {
//                        isSelectingAssistantGuideLanguage.toggle()
//                    })
//                }, header: {
//                    Text("Text to Speech")
//                        .font(.system(size: 12, weight: .light))
//                        .textInputAutocapitalization(.words)
//                        .textCase(.none)
//                })
                
                Section(content: {
//                    ToggleButton(isOn: $isEnablingDarkMode, label: "Dark Mode")
//                        .onChange(of: isEnablingDarkMode, { oldValue, newValue in
//                            withAnimation(.easeInOut(duration: 0.6), {
//                                isEnablingDarkMode = newValue
//                            })
//                        })
                    
                    SettingsListWithSelectingLabel(label: translationClass.language(code: mapxusController.selectedLanguage), secondLabel: languagePlaceholder(code: mapxusController.selectedLanguage), action: {
                        isShowingLanguageLists.toggle()
                    })
                    .sheet(isPresented: $isShowingLanguageLists, content: {
                        VStack(alignment: .center, spacing: 16, content: {
                            Picker("Language", selection: $mapxusController.selectedLanguage, content: {
                                Text("English").tag("en")
                                Text("Traditional Chinese 繁體中文 (香港)").tag("zh-Hant") /// zh-HK
                                Text("Simplified Chinese 简体中文 (中国)").tag("zh-Hans") /// zh_CN
                            })
                            .frame(height: 180, alignment: .center)
                            .pickerStyle(.wheel)
                            .sensoryFeedback(.selection, trigger: mapxusController.selectedLanguage)
                            .onChange(of: mapxusController.selectedLanguage, { oldValue, newValue in
                                if newValue != oldValue {
                                    isLanguageChanges = true
                                }
                                mapxusController.updateMapLanguage(to: newValue)
                            })
                            .padding(.top, 16)
                            
                            CustomBlueCircleIconButton(icon: "checkmark", action: {
                                isShowingLanguageLists = false
                            })
                        })
                        .presentationDetents([.height(240)])
                        .presentationDragIndicator(.hidden)
                        .presentationCornerRadius(18)
                        .padding()
                    })
                    
                }, header: {
                    Text(translationClass.generalSettings(code: mapxusController.selectedLanguage))
                        .font(.system(size: 12, weight: .light))
                        .textInputAutocapitalization(.words)
                        .textCase(.none)
                })
                
//                Section(content: {
//                    SettingsList(label: "Sign Out", action: {
//                        
//                    })
//                }, header: {
//                    Text("Account")
//                        .font(.system(size: 12, weight: .light))
//                        .textInputAutocapitalization(.words)
//                        .textCase(.none)
//                })
            })
        })
        .navigationTitle(translationClass.settings(code: mapxusController.selectedLanguage))
        .navigationBarTitleDisplayMode(.large)
        .navigationBarBackButtonHidden(true)
        .toolbar(content: {
            ToolbarItem(placement: .navigation, content: {
                PublicCustomBackButton(icon: "arrow.backward", action: {
                    if !mapxusController.navigationDestinationPath.isEmpty {
                        if isLanguageChanges {
                            withAnimation(.smooth(), {
                                isShowingMapxusMapIntroductionSection = true
                            })
                        } else {
                            withAnimation(.smooth(), {
                                isShowingMapxusMapIntroductionSection = false
                            })
                        }
                        
                        mapxusController.navigationDestinationPath.removeLast()
                        mapxusController.sheetHeight = 240
                        mapxusController.presentationActiveDetent = .height(240)
                    }
                })
            })
        })
        .confirmationDialog("AR Navigation Path", isPresented: $isShowingNavigationPathDialog, actions: {
            Button(action: {
                isShowingARNavigationAllAtOnceOr = true
                isShowingARNavigationAllAtOnceOrLabel = "One by One"
            }, label: {
                Text("One by One")
                    .font(.system(size: 20, weight: .regular))
            })
            
            Button(action: {
                isShowingARNavigationAllAtOnceOr = false
                isShowingARNavigationAllAtOnceOrLabel = "All at Once"
            }, label: {
                Text("All at Once")
                    .font(.system(size: 20, weight: .regular))
            })
        })
        .confirmationDialog("Please select the language", isPresented: $isSelectingAssistantGuideLanguage, actions: {
            Button(action: {
                isSelectingAssistantGuideLanguageLabel = "English"
                isSelectingAssistantGuideLanguageAVSpeechSynthesisVoiceCode = "en-US"
            }, label: {
                Text("English")
                    .font(.system(size: 20, weight: .regular))
            })
            
            Button(action: {
                isSelectingAssistantGuideLanguageLabel = "Mandarin Chinese"
                isSelectingAssistantGuideLanguageAVSpeechSynthesisVoiceCode = "zh"
            }, label: {
                Text("Mandarin Chinese")
                    .font(.system(size: 20, weight: .regular))
            })
        })
    }
}

extension SettingsView {
    @ViewBuilder
    func ToggleButton(isOn: Binding<Bool>, label: String) -> some View {
        Toggle(isOn: isOn, label: {
            Text(label)
                .font(.system(size: 20, weight: .medium))
                .minimumScaleFactor(0.7)
        })
    }
    
    @ViewBuilder
    func SettingsList(label: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
        }, label: {
            Text(label)
                .font(.system(size: 20, weight: .medium))
                .minimumScaleFactor(0.7)
        })
    }
    
    @ViewBuilder
    func SettingsListWithSelectingLabel(label: String, secondLabel: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
        }, label: {
            HStack(content: {
                Text(label)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(Color.primary)
                    .minimumScaleFactor(0.7)
                Spacer()
                Text(secondLabel)
                    .font(.system(size: 20, weight: .light))
                    .foregroundColor(Color.secondary)
                    .minimumScaleFactor(0.7)
            })
        })
    }
}
