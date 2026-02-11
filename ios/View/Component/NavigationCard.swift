import SwiftUI

struct NavigationCard: View {
    @StateObject var mapxusController: MapxusController
    
    var direction: String
    var distance: String
    var onPrevious: () -> Void
    var onNext: () -> Void
    
    func getAnIconForEveryDirection(direction: String) -> String {
        let lowerDirection = direction.lowercased()
        
        switch lowerDirection {
        // 1. Specific Turns (Check these BEFORE general right/left)
        case _ where lowerDirection.contains("slight right"):
            return "turn_slight_right"
        case _ where lowerDirection.contains("slight left"):
            return "turn_slight_left"
        case _ where lowerDirection.contains("sharp right"):
            return "turn_sharp_right"
        case _ where lowerDirection.contains("sharp left"):
            return "turn_sharp_left"
            
        // 2. Keep Directions
        case _ where lowerDirection.contains("keep left"):
            return "keep-left"
        case _ where lowerDirection.contains("keep right"):
            return "keep-right"
            
        // 3. General Directions
        case _ where lowerDirection.contains("right"):
            return "turn_right"
        case _ where lowerDirection.contains("left"):
            return "turn_left"
        case _ where lowerDirection.contains("straight"),
             _ where lowerDirection.contains("steps ahead"):
            return "straight"
            
        // 4. Vertical Movement
        case _ where lowerDirection.contains("stairs up"):
            return "upstairs"
        case _ where lowerDirection.contains("stairs down"):
            return "downstairs"
        case _ where lowerDirection.contains("elevator"),
             _ where lowerDirection.contains("lift"):
            return "elevator"
        case _ where lowerDirection.contains("escalator up"):
            return "escalator-up"
        case _ where lowerDirection.contains("escalator down"):
            return "escalator-down"
            
        // 5. Arrival
        case _ where lowerDirection.contains("arrive"),
             _ where lowerDirection.contains("destination"):
            return "arrive_at_destination_flag"
            
        case _ where lowerDirection.contains("leave the building"):
            return "leave-the-building"
        case _ where lowerDirection.contains("go into the building"):
            return "enter-the-building"
            
        // 6. Fallback
        default:
            return "map-pin-2"
        }
    }

    var body: some View {
        VStack(alignment: .center, spacing: 8, content: {
            HStack(spacing: 16, content: {
                Button(action: {
                    onPrevious()
                }, label: {
                    Image(systemName: "chevron.left")
                        .padding(8)
                        .foregroundColor(Color.white)
                        .background(mapxusController.instructionIndex == 0 ? Color.secondary : Color.mainColor)
                        .overlay(alignment: .center, content: {
                            Circle()
                                .stroke(.ultraThickMaterial, lineWidth: 4)
                        })
                        .clipShape(Circle())
                })
                .disabled(mapxusController.instructionIndex == 0)

                HStack(spacing: 8, content: {
                    VStack(alignment: .leading, spacing: 6, content: {
                        Text(mapxusController.getTranslationInstructionListInMultipleLanguages(instruction: direction, languageCode: mapxusController.selectedLanguage))
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(Color.primary)
                            .minimumScaleFactor(0.7)
                        Text("\(mapxusController.calculateEstimationTimeToArriveBasedOnRouteSearcher())")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color.secondary)
                        Text(distance)
                            .font(.system(size: 12, weight: .light))
                            .foregroundColor(Color.secondary)
                    })
                    
                    Spacer()
                    
                    Image(getAnIconForEveryDirection(direction: direction), bundle: Bundle(for: BundleFinder.self))
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 50, height: 50)
                        .foregroundColor(Color.primary)
                })
                
                Button(action: {
                    onNext()
                }, label: {
                    Image(systemName: "chevron.right")
                        .padding(8)
                        .foregroundColor(Color.white)
                        .background(mapxusController.instructionIndex >= mapxusController.instructionList.count - 1 ? Color.secondary : Color.mainColor)
                        .overlay(alignment: .center, content: {
                            Circle()
                                .stroke(.ultraThickMaterial, lineWidth: 4)
                        })
                        .clipShape(Circle())
                })
                .disabled(mapxusController.instructionIndex >= mapxusController.instructionList.count - 1)
            })
            
            ScrollViewReader(content: { proxy in
                // 1. Set the width to fit exactly 4 dots + their spacing
                // (4 dots * 8pt) + (3 gaps * 8pt) + extra room for the 'halo' overlay = ~64-70pt
                ScrollView(.horizontal, showsIndicators: false, content: {
                    HStack(spacing: 8, content: {
                        ForEach(0..<mapxusController.instructionList.count, id: \.self, content: { index in
                            Circle()
                                .fill(mapxusController.instructionIndex == index ? Color.mainColor : Color.gray.opacity(0.3))
                                .frame(width: 8, height: 8)
                                .overlay(
                                    Circle()
                                        .stroke(Color.secondaryMainColor, lineWidth: mapxusController.instructionIndex == index ? 4 : 0)
                                        .scaleEffect(1.4)
                                )
                                .scaleEffect(mapxusController.instructionIndex == index ? 1.2 : 1.0)
                                .id(index)
                        })
                    })
                    .padding(10)
                })
                .frame(width: 84)
                .onChange(of: mapxusController.instructionIndex, { oldValue, newValue in
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        // 3. Scroll to the new index.
                        // 'anchor: .leading' will move the active dot to the far left.
                        // 'anchor: .center' will keep it in the middle of the 4 dots.
                        proxy.scrollTo(newValue, anchor: .center)
                    }
                })
            })
            .frame(maxWidth: .infinity, minHeight: 16, maxHeight: 16, alignment: .center) // Centers the 4-dot window on screen
            
        })
        .padding(16)
        .background(Color.white)
        .cornerRadius(34)
        .overlay(alignment: .center, content: {
            RoundedRectangle(cornerRadius: 34)
                .stroke(.ultraThickMaterial, lineWidth: 2)
        })
        .padding()
    }
}
