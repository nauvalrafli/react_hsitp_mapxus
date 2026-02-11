//
//  AllCustomComponent.swift
//  mapxus-hsitp-ios
//
//  Created by Boxyguild on 6/6/25.
//

import SwiftUI
import CoreLocation
import Combine
import Foundation
import SDWebImageSwiftUI

struct CustomSlider: View {
    @Binding var progress: CLLocationDistance
    
    var body: some View {
        ZStack(alignment: .leading, content: {
            // 1. Background Track
            Capsule()
                .fill(.ultraThickMaterial)
                .frame(height: 10)
            
            // 2. Interactive Layer
            GeometryReader(content: { geo in
                let range = 50.0 - 5.0
                let progress = CGFloat((progress - 5.0) / range)
                let thumbPosition = progress * geo.size.width
                
                // Colored Progress Line
                Capsule()
                    .fill(Color.mainColor)
                    .frame(width: max(0, thumbPosition), height: 10)
                    .position(x: thumbPosition / 2, y: geo.size.height / 2)
                
                // White Dot (The Draggable Thumb)
                Circle()
                    .fill(Color.white)
                    .frame(width: 24, height: 24)
                    .overlay(alignment: .center, content: {
                        Circle()
                            .stroke(Color.mainColor, lineWidth: 1)
                    })
                    .shadow(color: Color.secondaryMainColor.opacity(0.9), radius: 3)
                    .position(x: thumbPosition, y: geo.size.height / 2)
            })
            .frame(height: 30)
            .contentShape(RoundedRectangle(cornerRadius: 34)) // Makes the whole area tappable
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        // Calculate the new value based on drag position
                        let sliderWidth = UIScreen.main.bounds.width - 40 // Adjust based on your padding
                        let percentage = Double(value.location.x / sliderWidth)
                        let newValue = 5.0 + (percentage * (50.0 - 5.0))
                        
                        // Clamp the value between 5 and 50
                        progress = min(max(5.0, newValue), 50.0)
                    }
            )
        })
        .frame(height: 30)
    }
}

struct RadioButton: View {
    let title: String
    @Binding var isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            HStack(spacing: 8, content: {
                ZStack(content: {
                    // Outer Circle
                    Image("circle", bundle: Bundle(for: BundleFinder.self))
                        .font(.system(size: 18))
                        .foregroundColor(isSelected ? .main : .secondary)
                    
                    // Inner Dot (Only visible when selected)
                    if isSelected {
                        Image("circle.fill", bundle: Bundle(for: BundleFinder.self))
                            .font(.system(size: 10))
                            .foregroundColor(.main)
                            .transition(.scale.combined(with: .opacity))
                    }
                })
                .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isSelected)
                
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(isSelected ? Color.main : Color.primary)
                    .minimumScaleFactor(0.3)
                    .lineLimit(1)
            })
        })
    }
}

struct CustomMainButton: View {
    var label: String
    var action: () -> Void
    var disabled: Bool = false
    var isLoading: Bool = false
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Group(content: {
                if isLoading {
                    ProgressView()
                        .tint(Color.white)
                        .frame(maxWidth: .infinity, minHeight: 50, alignment: .center)
                        .background(disabled ? Color.secondary : Color.mainColor)
                        .overlay(alignment: .center, content: {
                            RoundedRectangle(cornerRadius: 34)
                                .stroke(.ultraThickMaterial, lineWidth: 4)
                        })
                        .cornerRadius(34)
                        .minimumScaleFactor(0.4)
                } else {
                    Text(label)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 16)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color.white)
                        .frame(maxWidth: .infinity, minHeight: 50, alignment: .center)
                        .background(disabled ? Color.secondary : Color.mainColor)
                        .overlay(alignment: .center, content: {
                            RoundedRectangle(cornerRadius: 34)
                                .stroke(.ultraThickMaterial, lineWidth: 4)
                        })
                        .cornerRadius(34)
                        .minimumScaleFactor(0.4)
                }
            })
        })
        .disabled(disabled)
        .transition(.opacity)
    }
}

struct CustomMenuButton<Content: View>: View {
    var label: String
    var icon: String
    var foregroundColor: Color = Color.white
    var iconColor: Color = Color.white
    var content: Content
    
    // Pass label and icon here
    init(label: String, icon: String, @ViewBuilder content: () -> Content) {
        self.label = label
        self.icon = icon
        self.content = content()
        // Note: foregroundColor and iconColor already have default values,
        // so you don't HAVE to put them in the init unless you want to change them.
    }
    
    var body: some View {
        Menu(content: {
            content
        }, label: {
            Label(title: {
                Text(label)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(foregroundColor)
                    .minimumScaleFactor(0.7)
            }, icon: {
                if UIImage(systemName: icon) != nil {
                    Image(systemName: icon)
                        .foregroundColor(iconColor)
                } else {
                    let libraryBundle = Bundle(for: BundleFinder.self)
                    
                    if UIImage(named: icon, in: libraryBundle, with: nil) != nil {
                        Image(icon, bundle: libraryBundle)
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .foregroundColor(iconColor)
                    } else {
                        Image(icon)
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .foregroundColor(iconColor)
                    }
                }
              if UIImage(named: icon, in: Bundle(for: BundleFinder.self), with: nil) != nil {
                    Image(icon, bundle: Bundle(for: BundleFinder.self))
                        .foregroundColor(iconColor)
                } else {
                    Image(icon)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(iconColor)
                }
            })
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 50, maxHeight: 50, alignment: .center)
            .background(Color.mainColor)
            .overlay(alignment: .center, content: {
                RoundedRectangle(cornerRadius: 34)
                    .stroke(.ultraThickMaterial, lineWidth: 4)
            })
            .cornerRadius(34)
        })
    }
}

struct ContentMenuButton: View {
    var title: String
    var icon: String
    var action: () -> ()
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Label(title: {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
            }, icon: {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color.mainColor)
            })
        })
    }
}

struct CustomBorderButton: View {
    var label: String
    var action: () -> Void
    var disabled: Bool = false
    
    var body: some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.7), {
                action()
            })
        }, label: {
            Text(label)
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(disabled ? Color.secondary.opacity(0.3) : Color.mainColor)
                .frame(maxWidth: .infinity, minHeight: 50, alignment: .center)
                .background(.ultraThickMaterial)
                .cornerRadius(34)
                .minimumScaleFactor(0.4)
                .overlay(alignment: .center, content: {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(disabled ? Color.secondary.opacity(0.3) : Color.mainColor, lineWidth: 2)
                })
        })
        .disabled(disabled)
        .transition(.opacity)
    }
}

struct CustomBorderCloseButton: View {
    var label: String
    var action: () -> Void
    var disabled: Bool
    
    var body: some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.7), {
                action()
            })
        }, label: {
            Text(label)
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(disabled ? Color.secondary.opacity(0.3) : Color.red)
                .frame(maxWidth: .infinity, minHeight: 50, alignment: .center)
                .background(.ultraThickMaterial)
                .cornerRadius(34)
                .minimumScaleFactor(0.4)
                .overlay(alignment: .center, content: {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(disabled ? Color.secondary.opacity(0.3) : Color.red, lineWidth: 2)
                })
        })
        .disabled(disabled)
        .transition(.opacity)
    }
}

struct CustomShowRouteButton: View {
    var label: String
    var action: () -> Void
    @Binding var color: Bool
    var disabled: Bool = false
    
    var body: some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.7), {
                action()
            })
        }, label: {
            Text(label)
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(color ? Color.mainColor : disabled ? Color.white : Color.mainColor)
                .frame(maxWidth: .infinity, minHeight: 50, alignment: .center)
                .background(color ? Color(hex: 0xFFe6f1ff) : (disabled ? Color.secondary : Color.secondaryMainColor))
                .overlay(alignment: .center, content: {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(.ultraThickMaterial, lineWidth: 4)
                })
                .cornerRadius(34)
                .minimumScaleFactor(0.3)
                .lineLimit(1)
        })
        .disabled(disabled)
        .transition(.opacity)
    }
}

struct CustomCircleIconButton: View {
    var icon: String
    var iconColor: Color
    var backgroundColor: Color = Color.white
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            if UIImage(systemName: icon) != nil {
                Image(systemName: icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 26, height: 26)
                    .foregroundColor(iconColor)
                    .padding(8)
                    .background(backgroundColor)
                    .overlay(alignment: .center, content: {
                        Circle()
                            .stroke(Color.white, lineWidth: 4)
                    })
                    .clipShape(Circle())
                    .shadow(color: Color.gray.opacity(0.3), radius: 3)
            } else {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 26, height: 26)
                    .foregroundColor(iconColor)
                    .padding(8)
                    .background(backgroundColor)
                    .overlay(alignment: .center, content: {
                        Circle()
                            .stroke(Color.white, lineWidth: 4)
                    })
                    .clipShape(Circle())
                    .shadow(color: Color.gray.opacity(0.3), radius: 3)
            }
        })
    }
}

struct CustomBlueCircleIconButton: View {
    var icon: String
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Image(icon)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24, alignment: .center)
                .padding(18)
                .foregroundColor(Color.white)
                .background(Color.mainColor)
                .overlay(alignment: .center, content: {
                    Circle()
                        .stroke(.ultraThickMaterial, lineWidth: 4)
                })
                .clipShape(Circle())
        })
    }
}

struct CustomCircleCloseButton: View {
    var icon: String
    var iconColor: Color
    var backgroundColor: Color = Color.white
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Image(systemName: icon)
                .resizable()
                .scaledToFit()
                .frame(width: 18, height: 18)
                .foregroundColor(iconColor)
                .padding(12)
                .background(backgroundColor)
                .overlay(alignment: .center, content: {
                    Circle()
                        .stroke(Color.white, lineWidth: 4)
                })
                .clipShape(Circle())
                .shadow(color: Color.gray.opacity(0.3), radius: 3)
        })
    }
}

struct PublicCustomBackButton: View {
    var icon: String
    var iconColor: Color = Color.primary
    var backgroundColor: Color = Color.white
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            if UIImage(systemName: icon) != nil {
                Image(systemName: icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
                    .foregroundColor(iconColor)
                    .padding(12)
                    .background(backgroundColor)
                    .overlay(alignment: .center, content: {
                        Circle()
                            .stroke(.ultraThickMaterial, lineWidth: 4)
                    })
                    .clipShape(Circle())
            } else {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
                    .foregroundColor(iconColor)
                    .padding(12)
                    .background(backgroundColor)
                    .overlay(alignment: .center, content: {
                        Circle()
                            .stroke(.ultraThickMaterial, lineWidth: 4)
                    })
                    .clipShape(Circle())
            }
            
        })
    }
}

struct CustomRotationAnimationButton: View {
    var icon: String
    var iconColor: Color = Color.primary
    var backgroundColor: Color = Color.white
    var action: () -> Void
    @Binding var isAnimating: Bool
    
    @State private var rotationDegrees: Double = 0
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            iconView
                .foregroundColor(iconColor)
                .rotationEffect(.degrees(rotationDegrees))
                .padding(12)
                .background(backgroundColor)
                .animation(.smooth, value: isAnimating)
                .overlay(Circle().stroke(.ultraThickMaterial, lineWidth: 4))
                .clipShape(Circle())
        })
        .onChange(of: isAnimating, initial: true) { _, newValue in
            if newValue {
                // Continuous spin while loading
                withAnimation(.linear(duration: 1.0).repeatForever(autoreverses: false)) {
                    rotationDegrees = 360
                }
            } else {
                // Snap back to 0 smoothly when finished
                withAnimation(.smooth()) {
                    rotationDegrees = 0
                }
            }
        }
    }
    
    @ViewBuilder
    private var iconView: some View {
        let image = UIImage(systemName: icon) != nil ? Image(systemName: icon) : Image(icon).renderingMode(.template)
        
        image
            .resizable()
            .scaledToFit()
            .frame(width: 16, height: 16)
    }
}

struct CustomRefreshButton: View {
    var icon: String
    var iconColor: Color = Color.primary
    var backgroundColor: Color = Color.white
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            action()
        }, label: {
            Image(icon)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 26, height: 26, alignment: .center)
                .foregroundColor(iconColor)
                .padding(10)
                .background(backgroundColor)
                .overlay(alignment: .center, content: {
                    Circle()
                        .stroke(.ultraThickMaterial, lineWidth: 4)
                })
                .clipShape(Circle())
        })
    }
}

struct CustomPager<Content: View>: View {
    let content: () -> Content
    let isDragable: Bool
    let totalCount: Int
    // 1. Change to optional Int? to match .scrollPosition(id:) requirements
    @Binding var currPage: Int?

    init(
        currPage: Binding<Int?> = .constant(0),
        totalCount: Int,
        isDragable: Bool = true,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self._currPage = currPage
        self.totalCount = totalCount
        self.isDragable = isDragable
        self.content = content
    }

    var body: some View {
        VStack(spacing: 12) {
            ScrollView(.horizontal, showsIndicators: false, content: {
                LazyHStack(spacing: 0, content: {
                    content()
                })
                .scrollTargetLayout()
            })
            .scrollTargetBehavior(.paging)
            .scrollPosition(id: $currPage)
            .scrollDisabled(!isDragable)
            .onChange(of: currPage, { oldValue, newValue in
                handleLooping(newValue)
            })
            
            // 3. Indicator logic
            HStack(spacing: 8, content: {
                ForEach(0..<totalCount, id: \.self, content: { index in
                    Circle()
                        .fill(currPage == index ? Color.mainColor : Color.gray.opacity(0.4))
                        .frame(width: 10, height: 10)
                        .overlay(alignment: .center, content: {
                            Circle()
                                .stroke(.ultraThickMaterial, lineWidth: 4)
                        })
                        .scaleEffect(currPage == index ? 1.2 : 1.0)
                        .animation(.spring(), value: currPage)
                })
            })
        }
    }
    
    // 💡 Reset to first page if we go beyond the last one
    private func handleLooping(_ newValue: Int?) {
        guard let newValue = newValue else { return }
        
        // If we want a 'hidden' extra swipe to trigger the reset:
        if newValue >= totalCount {
            withAnimation(.smooth(duration: 0.3), {
                currPage = 0
            })
        }
    }
}

//struct CustomPager<Content: View>: View {
//    @Binding var currPage: Int? // Keeping the optional Int to match your state
//    let isDragable: Bool
//    let totalCount: Int
//    let content: () -> Content
//
//    var body: some View {
//        VStack(spacing: 12) {
//            // 1. Using TabView for native paging and easy index management
//            TabView(selection: Binding(
//                get: { currPage ?? 0 },
//                set: { currPage = $0 }
//            )) {
//                content()
//            }
//            .tabViewStyle(.page(indexDisplayMode: .never)) // Hides native dots so we can use your custom ones
//            .disabled(!isDragable)
//            .onChange(of: currPage) { oldValue, newValue in
//                // 2. Logic to detect if we've passed the boundaries
//                handleLooping(newValue)
//            }
//            .frame(height: 200) // Adjust based on your UI needs
//
//            // 3. Your Custom Indicators
//            HStack(spacing: 8) {
//                ForEach(0..<totalCount, id: \.self) { index in
//                    Circle()
//                        .fill(currPage == index ? Color.blue : Color.gray.opacity(0.4))
//                        .frame(width: 10, height: 10)
//                        .scaleEffect(currPage == index ? 1.2 : 1.0)
//                        .animation(.spring(), value: currPage)
//                }
//            }
//        }
//    }
//
//    private func handleLooping(_ newValue: Int?) {
//        guard let newValue = newValue else { return }
//        
//        // If the user somehow swipes to an index that doesn't exist,
//        // or if you have a "dummy" invisible page at the end to trigger a jump:
//        if newValue >= totalCount {
//            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
//                withAnimation(.spring()) {
//                    currPage = 0
//                }
//            }
//        }
//    }
//}

struct GaugeProgressStyle: ProgressViewStyle {
    var strokeColor: Color = Color.mainColor
    var strokeWidth: CGFloat = 25.0
    
    @State var isAnimating: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        let fractionCompleted = configuration.fractionCompleted ?? 0

        return ZStack(content: {
            // Background "Ghost" Ring
            Circle()
                .stroke(Color.clear, lineWidth: 10.0)
            
            // Spinning Foreground
            Circle()
                .trim(from: 0, to: 0.7)
                .stroke(strokeColor, style: StrokeStyle(lineWidth: 10.0, lineCap: .round))
                .rotationEffect(.degrees(isAnimating ? 360 : 0))
        })
        .onAppear(perform: {
            withAnimation(.linear(duration: 1).repeatForever(autoreverses: false)) {
                isAnimating = true
            }
        })
    }
}

/// Start of the Tooltip
enum TooltipDirection {
    case top
    case bottom
    case left
    case right
}

struct TooltipModel: Identifiable {
    let id = UUID().uuidString
    var name: String
    var icon: String? = nil
    let title: String
}

struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY)) // Tip
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY)) // Bottom Right
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY)) // Bottom Left
        path.closeSubpath()
        return path
    }
}

struct Tooltip: View {
    var items: [TooltipModel]
    var type: TooltipDirection
    
    public var body: some View {
        // Use a wrapper to handle the different stack types
        Group(content: {
            if type == .top || type == .bottom {
                VStack(spacing: 2, content: {
                    if type == .bottom { arrow }
                    content
                    if type == .top { arrow }
                })
            } else {
                HStack(spacing: 0, content: {
                    if type == .right { arrow }
                    content
                    if type == .left { arrow }
                })
            }
        })
        .zIndex(.infinity)
    }
    
    private func ActivityItem(item: TooltipModel) -> some View {
        HStack(spacing: 2, content: {
            if let icon = item.icon {
                Image(icon)
                    .resizable()
                    .frame(width: 16, height: 16)
            }
            
            Text(item.title)
                .font(.system(size: 14, weight: .light))
                .lineLimit(.max)
                .foregroundStyle(Color.black)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)
        })
        .padding(8)
        .frame(maxWidth: 280)
    }
    
    private func triangle() -> some View {
        Triangle()
            .fill(Color.white.opacity(1))
            .frame(width: 20, height: 10)
            .overlay(alignment: .center, content: {
                Triangle()
                    .stroke(.ultraThickMaterial, lineWidth: 2)
            })
    }
    
    // The main bubble content
    private var content: some View {
        HStack(spacing: 8, content: {
            ForEach(items, content: { item in
                ActivityItem(item: item)
            })
        })
        .background(Color.white)
        .cornerRadius(8)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(.ultraThickMaterial, lineWidth: 2))
    }
    
    // The rotated triangle based on direction
    private var arrow: some View {
        triangle()
            .rotationEffect(arrowRotation)
            // Small offset to overlap the stroke slightly for a seamless look
            .offset(x: xOffset, y: yOffset)
            .zIndex(1)
    }

    private var arrowRotation: Angle {
        switch type {
        case .top: return .degrees(180) // Points Down
        case .bottom: return .degrees(0) // Points Up
        case .left: return .degrees(90)  // Points Right
        case .right: return .degrees(-90) // Points Left
        }
    }
    
    private var xOffset: CGFloat {
        type == .left ? -1 : (type == .right ? 1 : 0)
    }
    
    private var yOffset: CGFloat {
        type == .top ? -1 : (type == .bottom ? 1 : 0)
    }
}

struct CustomAlert<T: Hashable>: View {
    // 1.
    @Binding private var isPresented: Bool
    // 2.
    @State private var titleKey: String
    private var alertMessage: String
    // 3.
    @State private var actionTextKey: String
    @State private var closeTextKey: String
    @Environment(\.colorScheme) private var colorScheme
    private var image: String
    // 7.
    private var action: () -> Void
    
    private var closeAction: () -> Void
    
    @State private var isAnimating = false
    private let animationDuration = 0.5

    // TODO: init()
    init(
        _ titleKey: String,
        _ isPresented: Binding<Bool>,
        presenting data: T?,
        actionTextKey: String,
        closeTextKey: String,
        alertMessage: String,
        image: String,
        action: @escaping () -> Void,
        closeAction: @escaping () -> Void
    ) {
        _titleKey = State(wrappedValue: titleKey)
        _actionTextKey = State(wrappedValue: actionTextKey)
        _closeTextKey = State(wrappedValue: closeTextKey)
        _isPresented = isPresented
      
        self.action = action
        self.closeAction = closeAction
        self.alertMessage = alertMessage
        self.image = image
    }
    
    var CancelButton: some View {
        Button(action: {
            dismiss()
        }, label: {
            Text(closeTextKey)
                .font(.system(size: 16, weight: .semibold))
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .background(colorScheme == .dark ? Material.thin : Material.ultraThinMaterial)
                .background(.gray)
                .clipShape(RoundedRectangle(cornerRadius: 30))
        })
    }
    
    var DoneButton: some View {
        Button(action: {
            dismiss()
            
            action()
        }, label: {
            Text(actionTextKey)
                .font(.system(size: 16, weight: .semibold))
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 50, maxHeight: 50)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 30.0))
        })
    }
    
    func dismiss() {
        if #available(iOS 17.0, *) {
            withAnimation(.smooth(duration: animationDuration)) {
                isAnimating = false
            } completion: {
                isPresented = false
            }
        } else {
            withAnimation(.smooth(duration: animationDuration)) {
                isAnimating = false
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration) {
                isPresented = false
            }
        }
    }
    
    func show() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(.smooth(duration: 0.5), {
                isPresented = true
            })
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            withAnimation(.smooth(duration: animationDuration), {
                isAnimating = true
            })
        }
    }
    
    var body: some View {
        ZStack(content: {
            Color.gray
                .ignoresSafeArea()
                .opacity(isPresented ? 0.6 : 0)
                .zIndex(1)

            if isAnimating {
                VStack(content: {
                    VStack(alignment: .center, spacing: 8, content: {
                        AnimatedImage(name: "arrive-at-destination.gif")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 100, height: 100)
                        
                        VStack(alignment: .center, spacing: 4, content: {
                            /// Title
                            Text(titleKey)
                                .font(.system(size: 34, weight: .semibold))
                                .foregroundColor(Color.mainColor)
                                .multilineTextAlignment(.center)
                                .minimumScaleFactor(0.7)
                                .shadow(color: Color.white, radius: 3)
                                .padding(.horizontal)
                                .padding(.vertical, 8)
                            
                            Text("\(alertMessage)")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(Color.black)
                                .multilineTextAlignment(.center)
                                .minimumScaleFactor(0.7)
                        })
                        .frame(maxWidth: .infinity, alignment: .center)
                        
                        HStack(spacing: 16, content: {
                            Button(action: {
                                dismiss()
                                
                                closeAction()
                            }, label: {
                                Text(closeTextKey)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(Color.primary)
                                    .frame(maxWidth: .infinity, maxHeight: 50, alignment: .center)
                            })
                            
                            Button(action: {
                                dismiss()
                                
                                action()
                            }, label: {
                                Text(actionTextKey)
                                    .font(.system(size: 16, weight: .semibold))
                                    .frame(maxWidth: .infinity, maxHeight: 50, alignment: .center)
                                    .foregroundColor(Color.mainColor)
                            })
                        })
                        .padding(.top, 16)
                        .padding(.horizontal, 16)
                    })
                    .padding(16)
                    .frame(maxWidth: .infinity)
                    .background(Color.white)
                    .overlay(alignment: .center, content: {
                        RoundedRectangle(cornerRadius: 34)
                            .stroke(.ultraThickMaterial, lineWidth: 2)
                    })
                    .cornerRadius(34)
                })
                .padding()
                .transition(.move(edge: .bottom))
                .zIndex(2)
            }
        })
        .ignoresSafeArea()
        .onAppear(perform: {
            withAnimation(.bouncy(duration: 1.0, extraBounce: 0.1), {
                show()
            })
        })

    }

}

private extension CustomAlert where T == Never {
    init(
        _ titleKey: String,
        _ isPresented: Binding<Bool>,
        actionTextKey: String,
        closeTextKey: String,
        alertMessage: String,
        image: String,
        action: @escaping () -> (),
        closeAction: @escaping () -> Void
    ) where T == Never {
        _titleKey = State(wrappedValue: titleKey)
        _actionTextKey = State(wrappedValue: actionTextKey)
        _closeTextKey = State(wrappedValue: closeTextKey)
        _isPresented = isPresented
  
        self.image = image
        self.action = action
        self.closeAction = closeAction
        self.alertMessage = alertMessage
    }
}

extension View {
    func customAlert(
        _ titleKey: String,
        isPresented: Binding<Bool>,
        actionText: String,
        closeTextKey: String,
        alertMessage: String,
        image: String,
        action: @escaping () -> Void,
        closeAction: @escaping () -> Void
    ) -> some View {
        
        fullScreenCover(isPresented: isPresented) {
            
            if #available(iOS 16.4, *) {
                CustomAlert(
                    titleKey,
                    isPresented,
                    actionTextKey: actionText,
                    closeTextKey: closeTextKey,
                    alertMessage: alertMessage,
                    image: image,
                    action: action,
                    closeAction: closeAction
                )
                .presentationBackground(.clear)
            } else {
                CustomAlert(
                    titleKey,
                    isPresented,
                    actionTextKey: actionText,
                    closeTextKey: closeTextKey,
                    alertMessage: alertMessage,
                    image: image,
                    action: action,
                    closeAction: closeAction
                )
            }
                
        }
        
        // TODO: Disable fullScreenCover transition animation.
    }
    /// TODO: customAlert<T>...
}

/// Custom Alert with Empty Content
struct CustomAlertWithEmptyContent<Content: View>: View {
    @Binding var isPresented: Bool
    let content: Content
    let action: () -> Void
    
    @State private var isAnimating = false
    private let animationDuration = 0.3 // Faster duration feels more "iOS-like"

    init(
        isPresented: Binding<Bool>,
        @ViewBuilder content: () -> Content,
        action: @escaping () -> Void
    ) {
        self._isPresented = isPresented
        self.content = content()
        self.action = action
    }
    
    func show() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(.smooth(), {
                isPresented = true
            })
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            withAnimation(.smooth(duration: 0.6), {
                isAnimating = true
            })
        }
    }
    
    func dismiss() {
        withAnimation(.smooth(duration: 0.3)) {
            isAnimating = false
        }
        // Small delay to let the "Scale Down" finish before closing the cover
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isPresented = false
        }
    }
    
    var body: some View {
        ZStack(content: {
            /// Background Dimmer
            Color.white
                .ignoresSafeArea()
                .opacity(isAnimating ? 0.7 : 0)
                .transition(.move(edge: isAnimating ? .bottom : .top))
                .zIndex(1)

            /// The Alert Content
            if isAnimating {
                content
                    .transition(.move(edge: isAnimating ? .bottom : .top))
                    .zIndex(2)
            }
        })
        .ignoresSafeArea()
        .onAppear(perform: {
            withAnimation(.smooth, {
                show()
            })
        })
    }
}

private extension CustomAlertWithEmptyContent {
    init(
        _ isPresented: Binding<Bool>,
        @ViewBuilder content: @escaping () -> Content,
        action: @escaping () -> ()
    ) {
        _isPresented = isPresented
        self.content = content()
        self.action = action
    }
}

/// MARK: - View Extension
extension View {
    func customAlertWithEmptyContent<AlertContent: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder content: @escaping () -> AlertContent,
        action: @escaping () -> Void
    ) -> some View {
        self.fullScreenCover(isPresented: isPresented) {
            CustomAlertWithEmptyContent(
                isPresented: isPresented,
                content: content,
                action: action
            )
            .presentationBackground(.clear) // Works on iOS 16.4+
        }
    }
}

extension View {
    func customToast(isShown: Binding<Bool>, message: String, icon: String? = nil, iconColor: Color = Color.mainColor, alignment: Alignment = .top) -> some View {
        
        ZStack {
            self
            CustomToast(isShown: isShown, message: message, icon: icon, alignment: alignment)
        }
    }
}

struct CustomToast: View {
    @Binding var isShown: Bool
    var message: String = ""
    var icon: String? = ""
    var iconColor: Color = Color.mainColor
    var alignment: Alignment = .top

    var body: some View {
        VStack(content: {
            if isShown {
                HStack(spacing: 16, content: {
                    if UIImage(systemName: icon ?? "") != nil {
                        Image(systemName: icon ?? "")
                            .foregroundColor(iconColor)
                    } else {
                        Image(icon ?? "")
                            .renderingMode(.template)
                            .foregroundColor(iconColor)
                    }
                    
                    VStack(alignment: .center, spacing: 0, content: {
                        Text(message)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color.black)
                            .multilineTextAlignment(.leading)
                            .minimumScaleFactor(0.3)
                            .lineLimit(1)
                    })
                })
                .padding(.vertical, 8)
                .padding(.horizontal, 32)
                .frame(height: 50, alignment: .center)
                .background(.ultraThinMaterial)
                .overlay(alignment: .center, content: {
                    RoundedRectangle(cornerRadius: 34)
                        .stroke(.ultraThinMaterial, lineWidth: 2)
                })
                .cornerRadius(34)
                .transition(.move(edge: alignmentToEdge(self.alignment)))
            }
        })
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
        .animation(.linear(duration: 0.15), value: isShown)
        .padding(.top, 50)
        .padding(.horizontal, 16)
        .onChange(of: isShown, { oldValue, newValue in
            if newValue {
                DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
                    withAnimation(.spring(duration: 0.3, bounce: 0.7), {
                        isShown = false
                    })
                }
            }
        })
        .onTapGesture(perform: {
            withAnimation(.smooth(), {
                isShown = false
            })
        })
        .zIndex(.infinity)
        
    }
    
    private func alignmentToEdge(_ alignment: Alignment) -> Edge {
        switch alignment {
        case .topLeading, .top, .topTrailing:
            return .top
        case .bottomLeading, .bottom, .bottomTrailing:
            return .bottom
        default:
            return .top
        }
    }
}

private class BundleFinder {}

extension Color {
    static let main = Color("MainColor", bundle: Bundle(for: BundleFinder.self))
    static let mainColor = Color("MainColor", bundle: Bundle(for: BundleFinder.self))
    static let secondaryMainColor = Color("SecondaryColor", bundle: Bundle(for: BundleFinder.self))
    static let secondary = Color("SecondaryColor", bundle: Bundle(for: BundleFinder.self))
    static let whiteOnLightMode = Color("WhiteOnLightMode", bundle: Bundle(for: BundleFinder.self))
    
    init(hex: Int) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1.0)
    }
}

// Usage:
// let myColor = Color(hex: "#FF5733")
