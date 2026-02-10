//
//  ARView.swift
//  mapxus-hsitp-ios
//
//  Created by Boxyguild on 6/6/25.
//

import SwiftUI
import MapxusMapSDK

struct ARMainView: View {
    @Binding var arViewHeight: CGFloat
    @Binding var instructionList: [MXMInstruction]
    @Binding var instructionPointList: [MXMGeoPoint]
    @Binding var instructionIndex: Int
    @Binding var compassDegress: Double
    @Binding var compassWarning: String
    @State var languageCode: String
    
    @AppStorage("ARNavigation-App-Enabling-Motion-Sensor") private var isEnablingMotionSensor: Bool = false
    @AppStorage("ARNavigation-App-AR-Visibility") private var isShowingARNavigationAllAtOnceOr: Bool = false
    @AppStorage("ARNavigation-App-Showing-Direction") private var isShowingDirectionDegree: String = ""
    @State private var zeroCompassDegrees: Double = 0.0

    var body: some View {
        ARNavigationArrowPointDirectionViewContainer(instructionList: instructionList, instructionPointList: instructionPointList, currentInstructionIndex: instructionIndex, compassClassDegrees: compassDegress, languageCode: languageCode)
            .frame(maxWidth: .infinity, alignment: .top)
            .frame(height: arViewHeight)
            .ignoresSafeArea(.all)
            .onAppear(perform: {
                for (i, instruction) in instructionList.enumerated() {
                    print("ar navigation between building: \(i) floor id: \(instruction.floorId ?? ""), instruction: \(instruction.text ?? "")")
                }
                
                for (i, instruction) in instructionPointList.enumerated() {
                    print("ar navigation between building: \(i) lat: \(instruction.latitude), lon: \(instruction.longitude), elevation: \(instruction.elevation)")
                }
            })
            .onChange(of: instructionList, { oldValue, newValue in
                guard !newValue.isEmpty else { return }

                // 2. Use a more readable loop
                for (index, instruction) in newValue.enumerated() {
                    let floor = instruction.floorId ?? "Unknown Floor"
                    let text = instruction.text ?? "No Instruction"
                    
                    print("AR Nav [\(index)] | Floor: \(floor) | Task: \(text)")
                }
            })
            .overlay(alignment: .bottomTrailing, content: {
                Text("\(String(format: "%.0f", compassWarning.isEmpty ? compassDegress : compassWarning))°")
                    .font(.system(size: 22, weight: .semibold))
                    .fontWeight(.semibold)
                    .foregroundColor(Color.white)
                    .shadow(color: Color.black, radius: 3)
                    .padding()
            })
            .overlay(alignment: .center, content: {
                if compassWarning.isEmpty {
                    Text(isShowingDirectionDegree)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(Color.white)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.7)
                        .shadow(color: Color.black, radius: 3)
                        .padding()
                } else {
                    Text(compassWarning)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(Color.white)
                        .multilineTextAlignment(.center)
                        .blinking(duration: 0.5)
                        .minimumScaleFactor(0.7)
                        .shadow(color: Color.black, radius: 3)
                        .padding()
                }
            })
        
    }

}

struct BlinkViewModifier: ViewModifier {
    let duration: Double
    @State private var blinking: Bool = false

    func body(content: Content) -> some View {
        content
            .opacity(blinking ? 0 : 1)
            .animation(Animation.linear(duration: duration).repeatForever(autoreverses: true), value: blinking) // Use .linear or .easeInOut
            .onAppear {
                blinking = true
            }
    }
}

extension View {
    func blinking(duration: Double = 0.75) -> some View {
        modifier(BlinkViewModifier(duration: duration))
    }
}
